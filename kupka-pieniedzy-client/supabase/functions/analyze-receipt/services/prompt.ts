// Narzędzia do promptów: podstawianie zmiennych {{KEY}} i tolerancyjny parser JSON.

/** Podstawia {{KEY}} -> value w szablonie promptu. */
export function render(template: string, vars: Record<string, string>): string {
  let out = template;
  for (const [key, value] of Object.entries(vars)) {
    out = out.replaceAll(`{{${key}}}`, value);
  }
  return out;
}

/** Wyciąga i parsuje obiekt JSON z odpowiedzi modelu (zdejmuje code-fence, obcina do {…}). */
export function parseJson(text: string): Record<string, unknown> {
  let t = text.trim();

  const fence = t.match(/```(?:json)?\s*([\s\S]*?)```/i);
  if (fence) t = fence[1].trim();

  if (!t.startsWith("{")) {
    const start = t.indexOf("{");
    const end = t.lastIndexOf("}");
    if (start !== -1 && end > start) t = t.slice(start, end + 1);
  }

  const parsed = JSON.parse(t);
  if (typeof parsed !== "object" || parsed === null) {
    throw new Error("Model did not return a JSON object");
  }
  return parsed as Record<string, unknown>;
}
