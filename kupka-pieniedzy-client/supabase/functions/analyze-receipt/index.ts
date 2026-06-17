// HTTP boundary (cienki): parsuje żądanie, woła pipeline, formatuje odpowiedź/błędy.
// Cała logika domenowa jest w pipeline.ts + steps/. Tu tylko warstwa transportu.
import { corsHeaders } from "../_shared/cors.ts";
import { analyzeReceipt } from "./pipeline.ts";
import { PipelineError } from "./errors.ts";
import type { AnalyzeRequest } from "./model.ts";

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "content-type": "application/json" },
  });
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }
  if (request.method !== "POST") {
    return json({ error: { code: "method_not_allowed", message: "Use POST" } }, 405);
  }

  let body: AnalyzeRequest;
  try {
    body = await request.json();
  } catch {
    return json({ error: { code: "invalid_request", message: "Body must be JSON" } }, 400);
  }

  try {
    return json(await analyzeReceipt(body));
  } catch (e) {
    if (e instanceof PipelineError) {
      return json({ error: { code: e.code, message: e.message } }, e.status);
    }
    console.error("Unhandled error:", e);
    return json({ error: { code: "internal", message: "Unexpected error" } }, 500);
  }
});
