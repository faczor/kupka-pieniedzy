// Wspólne nagłówki CORS dla Edge Functions. Mobile (Android/iOS) tego nie wymaga,
// ale Compose Web / wywołania z przeglądarki / lokalne testy z curl tak.
export const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};
