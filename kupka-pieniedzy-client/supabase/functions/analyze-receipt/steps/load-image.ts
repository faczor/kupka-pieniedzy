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

/**
 * media_type po sygnaturze base64 (Claude vision wymaga JPEG/PNG/GIF/WebP).
 * HEIC/HEIF (domyślny format aparatu telefonu) jest NIEOBSŁUGIWANY — zamiast cicho podawać
 * `image/jpeg` (co kończyło się błędem Anthropic „Could not process image"), zwracamy czytelny
 * błąd. Klient powinien transkodować zdjęcie do JPEG przed wysłaniem.
 */
function detectMediaType(b64: string): string {
  if (b64.startsWith("/9j/")) return "image/jpeg";
  if (b64.startsWith("iVBORw0KGgo")) return "image/png";
  if (b64.startsWith("R0lGOD")) return "image/gif";
  if (b64.startsWith("UklGR")) return "image/webp";
  if (isHeif(b64)) {
    throw invalidRequest(
      "Unsupported image format (HEIC/HEIF). Wyślij JPEG/PNG/WebP — zdjęcie należy " +
        "transkodować na urządzeniu przed wysłaniem do funkcji.",
    );
  }
  return "image/jpeg";
}

/** HEIF/HEIC: po nagłówku ISO-BMFF `....ftyp<brand>` (brand w zbiorze marek HEIF). */
function isHeif(b64: string): boolean {
  try {
    const head = atob(b64.slice(0, 32)); // ~24 bajty wystarczą na box `ftyp`
    if (head.length < 12 || head.slice(4, 8) !== "ftyp") return false;
    const brand = head.slice(8, 12);
    return [
      "heic", "heix", "hevc", "hevx", "heim", "heis", "hevm", "hevs",
      "mif1", "msf1", "heif",
    ].includes(brand);
  } catch {
    return false;
  }
}
