// Wspólne narzędzia do promptów: podstawianie zmiennych {{KEY}} i tolerancyjny parser JSON
// (model proszony jest o czysty JSON, ale na wszelki wypadek zdejmujemy code-fence i obcinamy
// do pierwszego/ostatniego nawiasu klamrowego).

/** Podstawia {{KEY}} -> value w szablonie promptu. */
export function render(template: string, vars: Record<string, string>): string {
  let out = template;
  for (const [key, value] of Object.entries(vars)) {
    out = out.replaceAll(`{{${key}}}`, value);
  }
  return out;
}

/** Wyciąga i parsuje obiekt JSON z odpowiedzi modelu. Rzuca, gdy nie da się sparsować. */
export function parseJsonObject(text: string): unknown {
  let t = text.trim();

  // zdejmij ```json ... ``` / ``` ... ```
  const fence = t.match(/```(?:json)?\s*([\s\S]*?)```/i);
  if (fence) t = fence[1].trim();

  // obetnij do pierwszego "{" .. ostatniego "}"
  if (!t.startsWith("{")) {
    const start = t.indexOf("{");
    const end = t.lastIndexOf("}");
    if (start !== -1 && end > start) t = t.slice(start, end + 1);
  }

  return JSON.parse(t);
}
