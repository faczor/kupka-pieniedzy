// Etap OCR: Google Cloud Vision (DOCUMENT_TEXT_DETECTION). Zwraca surowy tekst paragonu.
// Świadomie tylko OCR (piksele -> tekst); strukturyzacja line-itemów dzieje się dalej
// (anthropic.ts), żeby nie płacić za drogie gotowe parsery paragonów.

const VISION_ENDPOINT = "https://vision.googleapis.com/v1/images:annotate";

/**
 * @param imageBase64 obraz w base64 (bez prefiksu data:)
 * @param apiKey klucz API Google Cloud z włączonym Cloud Vision API
 * @returns pełny rozpoznany tekst
 */
export async function runOcr(
  imageBase64: string,
  apiKey: string,
): Promise<string> {
  const res = await fetch(`${VISION_ENDPOINT}?key=${apiKey}`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      requests: [
        {
          image: { content: imageBase64 },
          features: [{ type: "DOCUMENT_TEXT_DETECTION" }],
          // Paragony są po polsku — podpowiedź języka poprawia OCR diakrytyków.
          imageContext: { languageHints: ["pl"] },
        },
      ],
    }),
  });

  if (!res.ok) {
    const body = await res.text();
    throw new Error(`Cloud Vision HTTP ${res.status}: ${body}`);
  }

  const data = await res.json();
  const first = data?.responses?.[0];
  if (first?.error) {
    throw new Error(`Cloud Vision error: ${first.error.message ?? "unknown"}`);
  }

  const text: string = first?.fullTextAnnotation?.text ?? "";
  if (!text.trim()) {
    throw new Error("Cloud Vision returned no text (blank or unreadable image)");
  }
  return text;
}
