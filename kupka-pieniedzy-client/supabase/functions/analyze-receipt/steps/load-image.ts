// Krok 1: żądanie -> obraz paragonu (base64 + media_type).
// Źródło: Storage (imagePath) albo base64 wprost (test).
import type { AnalyzeRequest, ReceiptImage } from "../model.ts";
import { invalidRequest } from "../errors.ts";
import { downloadImageBase64 } from "../services/supabase.ts";

const DEFAULT_BUCKET = Deno.env.get("RECEIPTS_BUCKET") ?? "receipts";

export async function loadImage(req: AnalyzeRequest): Promise<ReceiptImage> {
  const base64 = req.imageBase64
    ? stripDataUrl(req.imageBase64)
    : await fromStorage(req);
  return { base64, mediaType: detectMediaType(base64) };
}

async function fromStorage(req: AnalyzeRequest): Promise<string> {
  if (!req.imagePath) throw invalidRequest("Provide imagePath or imageBase64");
  return downloadImageBase64(req.bucket ?? DEFAULT_BUCKET, req.imagePath);
}

function stripDataUrl(b64: string): string {
  const comma = b64.indexOf(",");
  return b64.startsWith("data:") && comma !== -1 ? b64.slice(comma + 1) : b64;
}

/** media_type po sygnaturze base64 (Claude vision tego wymaga). Default jpeg. */
function detectMediaType(b64: string): string {
  if (b64.startsWith("/9j/")) return "image/jpeg";
  if (b64.startsWith("iVBORw0KGgo")) return "image/png";
  if (b64.startsWith("R0lGOD")) return "image/gif";
  if (b64.startsWith("UklGR")) return "image/webp";
  return "image/jpeg";
}
