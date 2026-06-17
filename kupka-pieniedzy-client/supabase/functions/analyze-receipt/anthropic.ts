// Etapy A (strukturyzacja) i B (kategoryzacja) realizowane przez Claude.
// Prompty żyją w osobnych plikach (./prompts/*.prompt.ts) w stylu XML z few-shot
// przykładami; tutaj tylko renderujemy zmienne, wysyłamy i parsujemy JSON.
import Anthropic from "npm:@anthropic-ai/sdk@0.39.0";
import type { CategoryExample, ExtractedReceipt } from "./types.ts";
import extractPromptTemplate from "./prompts/extract-receipt.prompt.ts";
import categorizePromptTemplate from "./prompts/categorize-items.prompt.ts";
import { parseJsonObject, render } from "./prompts.ts";

const JSON_ONLY_SYSTEM =
  "You are a precise extraction engine. Respond with a single JSON object only — " +
  "no prose, no markdown, no code fences.";

// Strukturalny typ zamiast Anthropic.Message — niezależny od wersji SDK.
type ContentBlockLike = { type: string; text?: string };
type MessageLike = { content: ContentBlockLike[] };

function firstText(message: MessageLike): string {
  const block = message.content.find((b) => b.type === "text" && typeof b.text === "string");
  if (!block?.text) throw new Error("Model returned no text content");
  return block.text;
}

/** Etap A (Haiku vision): zdjęcie paragonu -> ustrukturyzowany paragon (kwoty w złotych). */
export async function extractReceipt(
  client: Anthropic,
  model: string,
  imageBase64: string,
  mediaType: string,
): Promise<ExtractedReceipt> {
  const message = await client.messages.create({
    model,
    max_tokens: 4096,
    system: JSON_ONLY_SYSTEM,
    messages: [
      {
        role: "user",
        content: [
          {
            type: "image",
            source: { type: "base64", media_type: mediaType, data: imageBase64 },
          },
          { type: "text", text: extractPromptTemplate },
        ],
      },
    ],
  });

  const parsed = parseJsonObject(firstText(message)) as Record<string, unknown>;
  const rawItems = Array.isArray(parsed.items) ? parsed.items : [];
  const items = rawItems
    .map((it) => {
      const o = it as Record<string, unknown>;
      return { name: String(o.name ?? "").trim(), amount: Number(o.amount) };
    })
    .filter((it) => it.name.length > 0 && Number.isFinite(it.amount));

  return {
    store: String(parsed.store ?? "").trim() || "Nieznany sklep",
    date:
      typeof parsed.date === "string" && parsed.date.trim() ? parsed.date.trim() : null,
    total: Number.isFinite(Number(parsed.total)) ? Number(parsed.total) : null,
    items,
  };
}

/**
 * Etap B (LLM): dla każdej pozycji zwróć nazwę kategorii z `categories` albo null.
 * `history` to spersonalizowane przykłady usera (few-shot). Bez fallbacku — fallback
 * do "inne" robi orkiestracja (index.ts) na całej, scalonej liście.
 */
export async function categorizeItems(
  client: Anthropic,
  model: string,
  itemNames: string[],
  categories: string[],
  history: CategoryExample[],
): Promise<(string | null)[]> {
  if (itemNames.length === 0) return [];

  const allowed = new Set(categories);
  const historyBlock = history.length > 0
    ? history.map((h) => `${h.name} => ${h.category}`).join("\n")
    : "(brak historii — nowy użytkownik)";

  const prompt = render(categorizePromptTemplate, {
    USER_HISTORY: historyBlock,
    CATEGORIES: categories.join("\n"),
    ITEMS: itemNames.map((n, i) => `${i}. ${n}`).join("\n"),
  });

  const message = await client.messages.create({
    model,
    max_tokens: 2048,
    system: JSON_ONLY_SYSTEM,
    messages: [{ role: "user", content: prompt }],
  });

  const parsed = parseJsonObject(firstText(message)) as Record<string, unknown>;
  const assignments = Array.isArray(parsed.assignments) ? parsed.assignments : [];

  const result: (string | null)[] = new Array(itemNames.length).fill(null);
  for (const a of assignments) {
    const o = a as Record<string, unknown>;
    const idx = Number(o.index);
    const cat = o.category;
    if (!Number.isInteger(idx) || idx < 0 || idx >= itemNames.length) continue;
    result[idx] = typeof cat === "string" && allowed.has(cat) ? cat : null;
  }
  return result;
}

export { Anthropic };
