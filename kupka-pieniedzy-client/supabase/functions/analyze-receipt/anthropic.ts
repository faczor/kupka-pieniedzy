// Etapy A (strukturyzacja) i B (kategoryzacja) realizowane przez Claude.
// Oba używają wymuszonego wywołania narzędzia (tool_choice) zamiast luźnego JSON —
// dzięki temu wynik jest ustrukturyzowany i odporny na różnice wersji SDK.
import Anthropic from "npm:@anthropic-ai/sdk@0.39.0";
import type { ExtractedReceipt } from "./types.ts";

const EXTRACT_SYSTEM = `You parse raw OCR text from Polish store receipts (paragony).
Identify the store name, the purchase date, and every purchased product line with its
final price in major currency units (złote, e.g. 3.49).

Rules:
- Use the FINAL price actually charged for each line (after any rabat/discount), not the unit price.
- Exclude non-product lines: subtotals, SUMA / RAZEM, PTU / VAT summaries, payment lines
  (GOTÓWKA / KARTA / BLIK), change (RESZTA), loyalty points, NIP, addresses, headers and footers.
- A quantity line like "2 x 3,49" belongs to the item above it; record that line's total amount.
- Polish receipts use a comma as the decimal separator; output numbers with a dot.
- If the date is missing or unreadable, set date to null.
Record everything via the record_receipt tool.`;

const CATEGORIZE_SYSTEM = `You assign each receipt line item to exactly one budget category
from the provided list, or null when none is a clear fit. Categories are Polish budget
categories. Match by what the product actually is (an energy drink -> "energetyki",
milk or bread -> "jedzenie podstawowe"). Use only categories from the given list, verbatim.
When unsure, return null rather than guessing. Report results via the assign_categories tool.`;

// Strukturalny typ zamiast Anthropic.Message — niezależny od ścieżki typu w danej wersji SDK.
type ToolUseLike = { type: string; input?: unknown };
type MessageLike = { content: ToolUseLike[] };

function firstToolInput(message: MessageLike): Record<string, unknown> {
  const block = message.content.find((b) => b.type === "tool_use");
  if (!block) {
    throw new Error("Model did not return the expected tool call");
  }
  return (block.input ?? {}) as Record<string, unknown>;
}

/** Etap A: surowy tekst OCR -> ustrukturyzowany paragon (kwoty w złotych). */
export async function extractReceipt(
  client: Anthropic,
  model: string,
  ocrText: string,
): Promise<ExtractedReceipt> {
  const message = await client.messages.create({
    model,
    max_tokens: 4096,
    system: EXTRACT_SYSTEM,
    tools: [
      {
        name: "record_receipt",
        description: "Record the structured contents of a Polish store receipt.",
        input_schema: {
          type: "object",
          properties: {
            store: {
              type: "string",
              description: "Merchant / store name, e.g. Biedronka, Lidl, Żabka.",
            },
            date: {
              type: ["string", "null"],
              description: "Purchase date as ISO yyyy-mm-dd, or null if absent.",
            },
            total: {
              type: ["number", "null"],
              description:
                "Receipt grand total in major units (e.g. 56.84), or null if not found.",
            },
            items: {
              type: "array",
              items: {
                type: "object",
                properties: {
                  name: { type: "string", description: "Product name as printed." },
                  amount: {
                    type: "number",
                    description: "Final line price in major units (after discounts).",
                  },
                },
                required: ["name", "amount"],
              },
            },
          },
          required: ["store", "items"],
        },
      },
    ],
    tool_choice: { type: "tool", name: "record_receipt" },
    messages: [
      { role: "user", content: `OCR text of the receipt:\n\n${ocrText}` },
    ],
  });

  const input = firstToolInput(message);
  const rawItems = Array.isArray(input.items) ? input.items : [];
  const items = rawItems
    .map((it) => {
      const o = it as Record<string, unknown>;
      return { name: String(o.name ?? "").trim(), amount: Number(o.amount) };
    })
    .filter((it) => it.name.length > 0 && Number.isFinite(it.amount));

  return {
    store: String(input.store ?? "").trim() || "Nieznany sklep",
    date: typeof input.date === "string" && input.date.trim() ? input.date.trim() : null,
    total: Number.isFinite(Number(input.total)) ? Number(input.total) : null,
    items,
  };
}

/**
 * Etap B: dla każdej pozycji zwróć nazwę kategorii z `categories` albo null.
 * Wynik jest indeksowany; nieznane/halucynowane kategorie mapujemy na null.
 */
export async function categorizeItems(
  client: Anthropic,
  model: string,
  itemNames: string[],
  categories: string[],
): Promise<(string | null)[]> {
  if (itemNames.length === 0) return [];

  const allowed = new Set(categories);
  const numbered = itemNames.map((n, i) => `${i}. ${n}`).join("\n");

  const message = await client.messages.create({
    model,
    max_tokens: 2048,
    system: CATEGORIZE_SYSTEM,
    tools: [
      {
        name: "assign_categories",
        description: "Assign a category (or null) to every receipt item by index.",
        input_schema: {
          type: "object",
          properties: {
            assignments: {
              type: "array",
              items: {
                type: "object",
                properties: {
                  index: {
                    type: "integer",
                    description: "0-based index of the item in the input list.",
                  },
                  category: {
                    type: ["string", "null"],
                    description:
                      "One of the allowed category names, verbatim, or null if none fits.",
                  },
                },
                required: ["index", "category"],
              },
            },
          },
          required: ["assignments"],
        },
      },
    ],
    tool_choice: { type: "tool", name: "assign_categories" },
    messages: [
      {
        role: "user",
        content:
          `Allowed categories (use verbatim or null):\n${categories.join("\n")}\n\n` +
          `Items:\n${numbered}`,
      },
    ],
  });

  const input = firstToolInput(message);
  const assignments = Array.isArray(input.assignments) ? input.assignments : [];

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
