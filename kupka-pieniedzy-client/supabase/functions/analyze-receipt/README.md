# Edge Function: `analyze-receipt`

Analiza zdjęcia paragonu. Zastępuje `MockReceiptAnalysisRepository` po stronie klienta —
zwraca ustrukturyzowany wynik gotowy do zbudowania `RawReceiptAnalysis`.

## Pipeline

```
obraz (Storage / base64)
   └─ OCR ────────────► Google Cloud Vision (DOCUMENT_TEXT_DETECTION, languageHints=["pl"])
   └─ Strukturyzacja ─► Claude (Haiku) tekst → { sklep, data, total, items[{nazwa, kwota}] }
   └─ Kategoryzacja ──► Claude (Haiku) items + lista kategorii → kategoria | null
   └─ Walidacja ──────► total = SUM(items); confidence z heurystyki
```

Funkcja jest **czysta** — liczy i **zwraca** wynik, **nie pisze** do tabeli `receipts`.
Zapis (`markReady`) i mapowanie nazw kategorii → `category_id` robi klient, tak jak dziś.
Funkcja czyta jedynie obraz ze Storage (service role).

> **Notka (przyszłe rozszerzenie):** wstępny **OCR on-device** (ML Kit na Androidzie,
> Apple Vision na iOS, przez `expect`/`actual`) można dołożyć **przed** wysyłką do chmury —
> jako darmowa/prywatna „fast path", z fallbackiem na Cloud Vision. Pipeline strukturyzacji
> i kategoryzacji się nie zmienia (źródło tekstu jest wymienne).

## Prompty

Każdy prompt to **osobny plik** w `prompts/` w stylu XML (role / instructions / flow /
json_schema / json-fields / **examples**), z placeholderami `{{...}}` podmienianymi przed
wysyłką (`prompts.ts` → `render`).

| Plik | Zmienne | Rola |
|------|---------|------|
| `prompts/extract-receipt.prompt.ts`   | `{{OCR_DATA}}`              | Etap A: tekst OCR → JSON paragonu |
| `prompts/categorize-items.prompt.ts`  | `{{CATEGORIES}}`, `{{ITEMS}}` | Etap B: pozycje → kategorie |

Treść w środku to czysty prompt; opakowanie `export default \`...\`` służy tylko temu, by
plik na pewno trafił do bundla funkcji (statyczny `.xml` + `Deno.readTextFile` bywa gubiony
w deployu). Few-shot przykłady żyją w plikach promptów — edytuje się je bez ruszania kodu.

## Kontrakt — Request

`POST` z JSON. Podaj **`imagePath`** (obiekt w buckecie Storage) **albo** `imageBase64`.

| Pole          | Typ        | Wymagane | Opis |
|---------------|------------|----------|------|
| `imagePath`   | `string`   | tak\*    | ścieżka obiektu w prywatnym buckecie, np. `"userId/receiptId.jpg"` |
| `imageBase64` | `string`   | tak\*    | alternatywa do testów: base64 (z/bez prefiksu `data:`) |
| `bucket`      | `string`   | nie      | nadpisanie nazwy bucketu (domyślnie env `RECEIPTS_BUCKET` / `receipts`) |
| `categories`  | `string[]` | **tak**  | NAZWY kategorii usera, dokładnie jak w `categories.name` |
| `currency`    | `string`   | nie      | domyślnie `"PLN"` |

\* wymagane jest dokładnie jedno z `imagePath` / `imageBase64`.

```json
{
  "imagePath": "11111111-1111-1111-1111-111111111111/abc.jpg",
  "currency": "PLN",
  "categories": ["jedzenie podstawowe", "energetyki", "chemia", "słodycze", "napoje"]
}
```

## Kontrakt — Response (200)

Kwoty w **groszach** (minor units), spójnie z `Money` i `raw_ocr_json`.

| Pole         | Typ                | Opis |
|--------------|--------------------|------|
| `store`      | `string`           | nazwa sklepu |
| `date`       | `string \| null`   | ISO `yyyy-mm-dd` albo null |
| `currency`   | `string`           | kod waluty |
| `totalMinor` | `number`           | **niezmiennik:** == suma `items[].amountMinor` |
| `confidence` | `number`           | 0..1 (zgodność sumy + odsetek skategoryzowanych) |
| `items[]`    | `AnalyzedItem[]`   | pozycje |
| `items[].name`              | `string`         | nazwa pozycji |
| `items[].amountMinor`       | `number`         | kwota w groszach |
| `items[].suggestedCategory` | `string \| null` | nazwa z `categories` albo null |

```json
{
  "store": "Biedronka",
  "date": "2026-06-17",
  "currency": "PLN",
  "totalMinor": 5684,
  "confidence": 0.92,
  "items": [
    { "name": "Mleko 2% 1l",       "amountMinor": 349,  "suggestedCategory": "jedzenie podstawowe" },
    { "name": "Tiger Energy 0,25", "amountMinor": 449,  "suggestedCategory": "energetyki" },
    { "name": "Papier Velvet 8szt","amountMinor": 1890, "suggestedCategory": "chemia" }
  ]
}
```

### Mapowanie na klienta (`RawReceiptAnalysis`)

```
store              -> RawReceiptAnalysis.store
confidence         -> RawReceiptAnalysis.confidence
totalMinor         -> Money(totalMinor, currency)             // = sum(items)
items[].name       -> RawAnalyzedItem.name
items[].amountMinor-> Money(amountMinor, currency)
items[].suggestedCategory -> RawAnalyzedItem.suggestedCategoryName   // ReceiptService rozwiąże po nazwie
```

## Błędy

Body: `{ "error": { "code": "...", "message": "..." } }`.

| HTTP | `code`              | Kiedy |
|------|---------------------|-------|
| 400  | `invalid_request`   | zły body, brak `categories`, brak/niedostępny obraz |
| 405  | `method_not_allowed`| metoda inna niż POST |
| 502  | `ocr_failed`        | Cloud Vision odmówił / pusty tekst |
| 502  | `analysis_failed`   | błąd modelu (strukturyzacja/kategoryzacja) |
| 500  | `internal`          | brak env / nieoczekiwany błąd |

## Zmienne środowiskowe (sekrety)

| Nazwa                        | Źródło                  | Opis |
|------------------------------|-------------------------|------|
| `ANTHROPIC_API_KEY`          | **ustawiasz**           | klucz Claude API |
| `GOOGLE_CLOUD_VISION_API_KEY`| **ustawiasz**           | klucz Google Cloud z włączonym Cloud Vision API |
| `SUPABASE_URL`               | auto (Supabase)         | wstrzykiwany w deployu |
| `SUPABASE_SERVICE_ROLE_KEY`  | auto (Supabase)         | wstrzykiwany w deployu; pobiera obraz ze Storage |
| `RECEIPTS_BUCKET`            | opcjonalnie             | domyślnie `receipts` |
| `EXTRACTION_MODEL`           | opcjonalnie             | domyślnie `claude-haiku-4-5` |
| `CATEGORIZATION_MODEL`       | opcjonalnie             | domyślnie `claude-haiku-4-5` |

Ustawienie sekretów (cloud):

```bash
supabase secrets set \
  ANTHROPIC_API_KEY=sk-ant-... \
  GOOGLE_CLOUD_VISION_API_KEY=AIza...
# opcjonalnie podbij jakość kosztem ceny:
# supabase secrets set EXTRACTION_MODEL=claude-sonnet-4-6
```

> `SUPABASE_URL` i `SUPABASE_SERVICE_ROLE_KEY` są wstrzykiwane automatycznie na produkcji —
> **nie** ustawiaj ich ręcznie jako sekrety (Supabase to odrzuci). Potrzebne tylko lokalnie
> (patrz `.env.example`).

## Storage — bucket na paragony (raz)

Funkcja czyta obraz z prywatnego bucketu. Utwórz go w Dashboard → Storage → New bucket:

- nazwa: `receipts`
- **Public: OFF** (prywatny — paragony to dane osobowe)
- (zalecane) limit rozmiaru i `allowed_mime_types`: `image/jpeg`, `image/png`

Konwencja ścieżki obiektu: `"<userId>/<receiptId>.jpg"`. Klient wgrywa zdjęcie do Storage,
zapisuje tę ścieżkę w `receipts.image_path`, a potem przekazuje ją jako `imagePath`.

## Deploy

```bash
cd kupka-pieniedzy-client

# wymaga zlinkowanego projektu (patrz supabase/README.md → supabase link)
supabase functions deploy analyze-receipt
```

`verify_jwt = true` (patrz `config.toml`) — funkcja wymaga zalogowanego użytkownika.
Klient (supabase-kt, plugin Functions) dołącza token sesji automatycznie.

## Lokalnie

```bash
cd kupka-pieniedzy-client
cp supabase/functions/.env.example supabase/functions/.env.local
# uzupełnij klucze w .env.local

supabase functions serve analyze-receipt --env-file supabase/functions/.env.local
```

Test (curl, z base64 zamiast Storage):

```bash
curl -i http://127.0.0.1:54321/functions/v1/analyze-receipt \
  -H "Authorization: Bearer <ANON_LUB_USER_JWT>" \
  -H "content-type: application/json" \
  -d '{
    "imageBase64": "'"$(base64 -w0 paragon.jpg)"'",
    "currency": "PLN",
    "categories": ["jedzenie podstawowe","energetyki","chemia","słodycze","napoje"]
  }'
```

## Wywołanie z klienta (supabase-kt, szkic dla integracji)

```kotlin
// w nowym SupabaseReceiptAnalysisRepository : ReceiptAnalysisRepository
val response = supabase.functions.invoke(
    function = "analyze-receipt",
    body = buildJsonObject {
        put("imagePath", imagePath)            // ścieżka obiektu w buckecie 'receipts'
        put("currency", config.defaultCurrency)
        putJsonArray("categories") { categoryNames.forEach { add(it) } }
    },
)
// zdeserializuj do AnalyzeResponse i zbuduj RawReceiptAnalysis (mapowanie wyżej)
```

## Koszt (orientacyjnie, Haiku 4.5)

~**$0.005 / paragon**: Cloud Vision ~$0.0015 (pierwsze 1000/mies. gratis) + strukturyzacja
~$0.002 + kategoryzacja ~$0.002. Podbicie ekstrakcji na Sonnet/Opus (env) ≈ ×3–×5.
