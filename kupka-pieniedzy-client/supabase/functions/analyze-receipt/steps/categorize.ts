// Krok 4 (Etap B): pozycje paragonu -> kategorie, w 3 fazach:
//   1. exact-match z pamięci usera (bez LLM),
//   2. LLM few-shot (z historią usera) dla nierozpoznanych,
//   3. fallback "inne".
// Zwraca kategorię (lub null) dla każdej pozycji, w kolejności receipt.lines.
import { FALLBACK_CATEGORY, type CategoryExample, type ReadReceipt, type UserContext } from "../model.ts";
import { askForJson, CATEGORIZATION_MODEL } from "../services/claude.ts";
import { render } from "../services/prompt.ts";
import categorizePrompt from "../prompts/categorize-items.prompt.ts";

const norm = (s: string) => s.trim().toLowerCase();

export async function categorize(
  receipt: ReadReceipt,
  ctx: UserContext,
): Promise<(string | null)[]> {
  const names = receipt.lines.map((l) => l.name);
  const allowed = new Set(ctx.categories);

  // 1. exact-match z pamięci
  const memory = memoryIndex(ctx.history, allowed);
  const result = names.map((n) => memory.get(norm(n)) ?? null);

  // 2. LLM few-shot dla nierozpoznanych
  const pending = names.map((_, i) => i).filter((i) => result[i] === null);
  if (pending.length > 0) {
    const suggested = await suggestViaLlm(pending.map((i) => names[i]), ctx, allowed);
    pending.forEach((idx, k) => (result[idx] = suggested[k]));
  }

  // 3. fallback "inne"
  if (allowed.has(FALLBACK_CATEGORY)) {
    for (let i = 0; i < result.length; i++) {
      if (result[i] === null) result[i] = FALLBACK_CATEGORY;
    }
  }
  return result;
}

/** Mapa znormalizowana-nazwa -> kategoria (tylko kategorie wciąż istniejące u usera). */
function memoryIndex(history: CategoryExample[], allowed: Set<string>): Map<string, string> {
  const map = new Map<string, string>();
  for (const h of history) {
    const key = norm(h.name);
    if (!map.has(key) && allowed.has(h.category)) map.set(key, h.category);
  }
  return map;
}

async function suggestViaLlm(
  names: string[],
  ctx: UserContext,
  allowed: Set<string>,
): Promise<(string | null)[]> {
  const historyBlock = ctx.history.length > 0
    ? ctx.history.map((h) => `${h.name} => ${h.category}`).join("\n")
    : "(brak historii — nowy użytkownik)";

  const prompt = render(categorizePrompt, {
    USER_HISTORY: historyBlock,
    CATEGORIES: ctx.categories.join("\n"),
    ITEMS: names.map((n, i) => `${i}. ${n}`).join("\n"),
  });

  const json = await askForJson(CATEGORIZATION_MODEL, prompt, 2048);
  const assignments = Array.isArray(json.assignments) ? json.assignments : [];

  const out: (string | null)[] = new Array(names.length).fill(null);
  for (const a of assignments) {
    const o = a as Record<string, unknown>;
    const idx = Number(o.index);
    const cat = o.category;
    if (!Number.isInteger(idx) || idx < 0 || idx >= names.length) continue;
    out[idx] = typeof cat === "string" && allowed.has(cat) ? cat : null;
  }
  return out;
}
