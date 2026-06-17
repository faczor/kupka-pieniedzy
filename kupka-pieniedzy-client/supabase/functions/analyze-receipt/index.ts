// Edge Function `analyze-receipt`
// Pipeline (wszystko przez Anthropic): Haiku vision (zdjęcie -> struktura)
// -> kategoryzacja (exact-match z pamięci product_categories + LLM few-shot dla reszty).
// Funkcja jest "czysta": liczy wynik i go ZWRACA — nie pisze do `receipts`.
// Czyta: obraz ze Storage, kategorie i pamięć kategoryzacji z DB (service role).
import { encodeBase64 } from "jsr:@std/encoding@1/base64";
import { corsHeaders } from "../_shared/cors.ts";
import { Anthropic, categorizeItems, extractReceipt } from "./anthropic.ts";
import { fetchCategories, fetchMemory, serviceClient } from "./db.ts";
import type { AnalyzeRequest, AnalyzeResponse, CategoryExample } from "./types.ts";

const DEFAULT_BUCKET = Deno.env.get("RECEIPTS_BUCKET") ?? "receipts";
const EXTRACTION_MODEL = Deno.env.get("EXTRACTION_MODEL") ?? "claude-haiku-4-5";
const CATEGORIZATION_MODEL =
  Deno.env.get("CATEGORIZATION_MODEL") ?? "claude-haiku-4-5";
// Catch-all; nieprzypisane pozycje lądują tutaj zamiast jako null (seed: kategoria L1 "inne").
const FALLBACK_CATEGORY = "inne";

class ApiFault extends Error {
  constructor(public code: string, message: string, public status: number) {
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

/** Wykrywa media_type po sygnaturze base64 (Claude vision tego wymaga). Default jpeg. */
function detectMediaType(b64: string): string {
  if (b64.startsWith("/9j/")) return "image/jpeg";
  if (b64.startsWith("iVBORw0KGgo")) return "image/png";
  if (b64.startsWith("R0lGOD")) return "image/gif";
  if (b64.startsWith("UklGR")) return "image/webp";
  return "image/jpeg";
}

function requireEnv(name: string): string {
  const v = Deno.env.get(name);
  if (!v) throw new ApiFault("internal", `Missing env var ${name}`, 500);
  return v;
}

const norm = (s: string) => s.trim().toLowerCase();

async function loadImageBase64(req: AnalyzeRequest, bucket: string): Promise<string> {
  if (req.imageBase64) return stripDataUrl(req.imageBase64);
  if (!req.imagePath) {
    throw new ApiFault("invalid_request", "Provide imagePath or imageBase64", 400);
  }
  const { data, error } = await serviceClient().storage.from(bucket).download(req.imagePath);
  if (error || !data) {
    throw new ApiFault(
      "invalid_request",
      `Cannot download image '${req.imagePath}' from bucket '${bucket}': ${error?.message ?? "not found"}`,
      400,
    );
  }
  return encodeBase64(new Uint8Array(await data.arrayBuffer()));
}

/** Kategorie + pamięć: z DB po userId, albo z override w request (tryb testowy). */
async function resolveContext(
  req: AnalyzeRequest,
): Promise<{ categories: string[]; memory: CategoryExample[] }> {
  if (req.categories) {
    return { categories: req.categories, memory: req.examples ?? [] };
  }
  if (!req.userId) {
    throw new ApiFault("invalid_request", "Provide userId (or a categories override)", 400);
  }
  const supabase = serviceClient();
  try {
    const categories = await fetchCategories(supabase, req.userId);
    const memory = req.examples ?? await fetchMemory(supabase, req.userId);
    return { categories, memory };
  } catch (e) {
    throw new ApiFault("internal", (e as Error).message, 500);
  }
}

/** 0..1: zgodność sumy z paragonem + odsetek skategoryzowanych (nie-fallback) pozycji. */
function computeConfidence(
  parsedTotalMinor: number | null,
  sumMinor: number,
  items: { suggestedCategory: string | null }[],
): number {
  const specific = items.length
    ? items.filter((i) => i.suggestedCategory && i.suggestedCategory !== FALLBACK_CATEGORY)
        .length / items.length
    : 0;
  let c = 0.5 + 0.4 * specific; // 0.5..0.9
  const totalsMatch =
    parsedTotalMinor === null || Math.abs(parsedTotalMinor - sumMinor) <= 2;
  if (!totalsMatch) c -= 0.25;
  return Math.max(0.1, Math.min(0.97, Number(c.toFixed(2))));
}

async function analyze(req: AnalyzeRequest): Promise<AnalyzeResponse> {
  const currency = req.currency ?? "PLN";
  const bucket = req.bucket ?? DEFAULT_BUCKET;

  const imageBase64 = await loadImageBase64(req, bucket);
  const mediaType = detectMediaType(imageBase64);
  const { categories, memory } = await resolveContext(req);

  const client = new Anthropic({ apiKey: requireEnv("ANTHROPIC_API_KEY") });
  const allowed = new Set(categories);

  let store: string;
  let date: string | null;
  let parsedTotal: number | null;
  let names: string[];
  let amountsMinor: number[];
  let cats: (string | null)[];
  try {
    // Etap A (Haiku vision: zdjęcie -> struktura)
    const extracted = await extractReceipt(client, EXTRACTION_MODEL, imageBase64, mediaType);
    store = extracted.store;
    date = extracted.date;
    parsedTotal = extracted.total;
    names = extracted.items.map((i) => i.name);
    amountsMinor = extracted.items.map((i) => Math.round(i.amount * 100));

    // Etap B — exact-match z pamięci (pomija LLM dla znanych pozycji)
    const exact = new Map<string, string>();
    for (const m of memory) {
      const k = norm(m.name);
      if (!exact.has(k) && allowed.has(m.category)) exact.set(k, m.category);
    }
    cats = names.map((n) => exact.get(norm(n)) ?? null);

    // LLM tylko dla nierozpoznanych
    const pending = names.map((_, i) => i).filter((i) => cats[i] === null);
    if (pending.length > 0) {
      const llm = await categorizeItems(
        client,
        CATEGORIZATION_MODEL,
        pending.map((i) => names[i]),
        categories,
        memory,
      );
      pending.forEach((origIdx, k) => (cats[origIdx] = llm[k] ?? null));
    }
  } catch (e) {
    throw new ApiFault("analysis_failed", (e as Error).message, 502);
  }

  // Fallback: nieprzypisane -> "inne", jeśli ta kategoria istnieje u usera.
  if (allowed.has(FALLBACK_CATEGORY)) {
    for (let i = 0; i < cats.length; i++) {
      if (cats[i] === null) cats[i] = FALLBACK_CATEGORY;
    }
  }

  const items = names.map((name, i) => ({
    name,
    amountMinor: amountsMinor[i],
    suggestedCategory: cats[i] ?? null,
  }));

  // Niezmiennik kontraktu: total == suma pozycji.
  const totalMinor = amountsMinor.reduce((a, b) => a + b, 0);
  const parsedTotalMinor = parsedTotal !== null ? Math.round(parsedTotal * 100) : null;

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
