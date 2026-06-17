// Infra: klient Anthropic + helper „wyślij content, odbierz obiekt JSON".
// Wspólny dla Etapu A (vision) i B (kategoryzacja). Modele konfigurowalne env-em.
import Anthropic from "npm:@anthropic-ai/sdk@0.39.0";
import { parseJson } from "./prompt.ts";
import { analysisFailed, internal } from "../errors.ts";

const JSON_ONLY_SYSTEM =
  "You are a precise extraction engine. Respond with a single JSON object only — " +
  "no prose, no markdown, no code fences.";

export const EXTRACTION_MODEL = Deno.env.get("EXTRACTION_MODEL") ?? "claude-haiku-4-5";
export const CATEGORIZATION_MODEL =
  Deno.env.get("CATEGORIZATION_MODEL") ?? "claude-haiku-4-5";

type TextBlock = { type: "text"; text: string };
type ImageBlock = {
  type: "image";
  source: { type: "base64"; media_type: string; data: string };
};
export type PromptContent = string | Array<TextBlock | ImageBlock>;

let client: Anthropic | null = null;
function anthropic(): Anthropic {
  if (client) return client;
  const key = Deno.env.get("ANTHROPIC_API_KEY");
  if (!key) throw internal("Missing ANTHROPIC_API_KEY");
  client = new Anthropic({ apiKey: key });
  return client;
}

// Strukturalny typ zamiast Anthropic.Message — niezależny od wersji SDK.
type MessageLike = { content: Array<{ type: string; text?: string }> };

function firstText(message: MessageLike): string {
  const block = message.content.find((b) => b.type === "text" && typeof b.text === "string");
  if (!block?.text) throw new Error("Model returned no text content");
  return block.text;
}

/** Wyślij prompt (tekst lub bloki) i zwróć sparsowany obiekt JSON. Błędy → analysis_failed. */
export async function askForJson(
  model: string,
  content: PromptContent,
  maxTokens = 4096,
): Promise<Record<string, unknown>> {
  try {
    const message = await anthropic().messages.create({
      model,
      max_tokens: maxTokens,
      system: JSON_ONLY_SYSTEM,
      messages: [{ role: "user", content }],
    });
    return parseJson(firstText(message));
  } catch (e) {
    throw analysisFailed((e as Error).message);
  }
}
