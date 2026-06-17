// Edge Function `analyze-receipt`
// Pipeline: Cloud Vision (OCR) -> Claude (strukturyzacja) -> Claude (kategoryzacja).
// Funkcja jest "czysta": liczy wynik analizy i go ZWRACA — nie pisze do tabeli `receipts`
// (zapis/markReady robi klient, tak jak dziś z mockiem). Czyta wyłącznie obraz ze Storage.
import { createClient } from "npm:@supabase/supabase-js@2";
import { encodeBase64 } from "jsr:@std/encoding@1/base64";
import { corsHeaders } from "../_shared/cors.ts";
import { runOcr } from "./vision.ts";
import { Anthropic, categorizeItems, extractReceipt } from "./anthropic.ts";
import type { AnalyzeRequest, AnalyzeResponse } from "./types.ts";

const DEFAULT_BUCKET = Deno.env.get("RECEIPTS_BUCKET") ?? "receipts";
const EXTRACTION_MODEL = Deno.env.get("EXTRACTION_MODEL") ?? "claude-haiku-4-5";
const CATEGORIZATION_MODEL =
  Deno.env.get("CATEGORIZATION_MODEL") ?? "claude-haiku-4-5";

class ApiFault extends Error {
  constructor(
    public code: string,
    message: string,
    public status: number,
  ) {
    super(message);
  }
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "content-type": "application/json" },
  });
}

function stripDataUrl(b64: string): string {
  const comma = b64.indexOf(",");
  return b64.startsWith("data:") && comma !== -1 ? b64.slice(comma + 1) : b64;
}

function requireEnv(name: string): string {
  const v = Deno.env.get(name);
  if (!v) throw new ApiFault("internal", `Missing env var ${name}`, 500);
  return v;
}

async function loadImageBase64(
  req: AnalyzeRequest,
  bucket: string,
): Promise<string> {
  if (req.imageBase64) return stripDataUrl(req.imageBase64);
  if (!req.imagePath) {
    throw new ApiFault("invalid_request", "Provide imagePath or imageBase64", 400);
  }
  const supabase = createClient(
    requireEnv("SUPABASE_URL"),
    requireEnv("SUPABASE_SERVICE_ROLE_KEY"),
  );
  const { data, error } = await supabase.storage.from(bucket).download(req.imagePath);
  if (error || !data) {
    throw new ApiFault(
      "invalid_request",
      `Cannot download image '${req.imagePath}' from bucket '${bucket}': ${error?.message ?? "not found"}`,
      400,
    );
  }
  return encodeBase64(new Uint8Array(await data.arrayBuffer()));
}

/** 0..1: bazuje na zgodności sumy z paragonem i odsetku skategoryzowanych pozycji. */
function computeConfidence(
  parsedTotalMinor: number | null,
  sumMinor: number,
  items: { suggestedCategory: string | null }[],
): number {
  const categorized = items.length
    ? items.filter((i) => i.suggestedCategory !== null).length / items.length
    : 0;
  let c = 0.5 + 0.4 * categorized; // 0.5..0.9
  const totalsMatch =
    parsedTotalMinor === null || Math.abs(parsedTotalMinor - sumMinor) <= 2;
  if (!totalsMatch) c -= 0.25;
  return Math.max(0.1, Math.min(0.97, Number(c.toFixed(2))));
}

async function analyze(req: AnalyzeRequest): Promise<AnalyzeResponse> {
  if (!Array.isArray(req.categories)) {
    throw new ApiFault("invalid_request", "`categories` (string[]) is required", 400);
  }
  const currency = req.currency ?? "PLN";
  const bucket = req.bucket ?? DEFAULT_BUCKET;

  const imageBase64 = await loadImageBase64(req, bucket);

  // Etap OCR
  let ocrText: string;
  try {
    ocrText = await runOcr(imageBase64, requireEnv("GOOGLE_CLOUD_VISION_API_KEY"));
  } catch (e) {
    throw new ApiFault("ocr_failed", (e as Error).message, 502);
  }

  const client = new Anthropic({ apiKey: requireEnv("ANTHROPIC_API_KEY") });

  // Etap A + B
  let store: string;
  let date: string | null;
  let parsedTotal: number | null;
  let amountsMinor: number[];
  let names: string[];
  let cats: (string | null)[];
  try {
    const extracted = await extractReceipt(client, EXTRACTION_MODEL, ocrText);
    store = extracted.store;
    date = extracted.date;
    parsedTotal = extracted.total;
    names = extracted.items.map((i) => i.name);
    amountsMinor = extracted.items.map((i) => Math.round(i.amount * 100));
    cats = await categorizeItems(client, CATEGORIZATION_MODEL, names, req.categories);
  } catch (e) {
    throw new ApiFault("analysis_failed", (e as Error).message, 502);
  }

  const items = names.map((name, i) => ({
    name,
    amountMinor: amountsMinor[i],
    suggestedCategory: cats[i] ?? null,
  }));

  // Niezmiennik kontraktu: total == suma pozycji.
  const totalMinor = amountsMinor.reduce((a, b) => a + b, 0);
  const parsedTotalMinor =
    parsedTotal !== null ? Math.round(parsedTotal * 100) : null;

  return {
    store,
    date,
    currency,
    totalMinor,
    confidence: computeConfidence(parsedTotalMinor, totalMinor, items),
    items,
  };
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }
  if (request.method !== "POST") {
    return json({ error: { code: "method_not_allowed", message: "Use POST" } }, 405);
  }

  let body: AnalyzeRequest;
  try {
    body = await request.json();
  } catch {
    return json({ error: { code: "invalid_request", message: "Body must be JSON" } }, 400);
  }

  try {
    return json(await analyze(body));
  } catch (e) {
    if (e instanceof ApiFault) {
      return json({ error: { code: e.code, message: e.message } }, e.status);
    }
    console.error("Unhandled error:", e);
    return json({ error: { code: "internal", message: "Unexpected error" } }, 500);
  }
});
