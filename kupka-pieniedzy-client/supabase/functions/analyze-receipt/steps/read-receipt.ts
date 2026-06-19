// Krok 3 (Etap A): zdjęcie -> ustrukturyzowany paragon (Claude/Haiku vision).
// Prompt żyje w prompts/extract-receipt.prompt.ts; obraz idzie jako blok content.
import type { ReadReceipt, ReceiptImage, ReceiptLine } from "../model.ts";
import { askForJson, EXTRACTION_MODEL } from "../services/claude.ts";
import extractPrompt from "../prompts/extract-receipt.prompt.ts";

export async function readReceipt(image: ReceiptImage): Promise<ReadReceipt> {
  const json = await askForJson(EXTRACTION_MODEL, [
    {
      type: "image",
      source: { type: "base64", media_type: image.mediaType, data: image.base64 },
    },
    { type: "text", text: extractPrompt },
  ]);
  return toReadReceipt(json);
}

function toReadReceipt(json: Record<string, unknown>): ReadReceipt {
  const rawLines = Array.isArray(json.items) ? json.items : [];
  const lines: ReceiptLine[] = rawLines
    .map((it) => {
      const o = it as Record<string, unknown>;
      return { name: String(o.name ?? "").trim(), amount: Number(o.amount) };
    })
    .filter((l) => l.name.length > 0 && Number.isFinite(l.amount));

  const error =
    typeof json.error === "string" && json.error.trim() ? json.error.trim() : null;

  return {
    store: String(json.store ?? "").trim() || "Nieznany sklep",
    date: typeof json.date === "string" && json.date.trim() ? json.date.trim() : null,
    printedTotal: Number.isFinite(Number(json.total)) ? Number(json.total) : null,
    lines,
    error,
  };
}
