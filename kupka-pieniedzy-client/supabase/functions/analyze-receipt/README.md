# Edge Function: `analyze-receipt`

Analiza zdjęcia paragonu. Zastępuje `MockReceiptAnalysisRepository` po stronie klienta —
zwraca ustrukturyzowany wynik gotowy do zbudowania `RawReceiptAnalysis`.

## Pipeline

```
obraz (Storage / base64)
   └─ Ekstrakcja ─────► Claude Haiku (vision) zdjęcie → { sklep, data, total, items[{nazwa, kwota}] }
   └─ Kategoryzacja ──► exact-match z pamięci (product_categories) → reszta: Claude (Haiku)
                        z few-shot z historii usera → fallback "inne"
   └─ Walidacja ──────► total = SUM(items); confidence z heurystyki
```

Wszystko idzie przez Anthropic — **dwa calle do Haiku** (vision-ekstrakcja + kategoryzacja),
bez zewnętrznego OCR. Funkcja jest **czysta** — liczy i **zwraca** wynik, **nie pisze** do
tabeli `receipts`. Zapis (`markReady`) i mapowanie nazw kategorii → `category_id` robi klient.
Funkcja czyta (service role): obraz ze Storage, kategorie usera i pamięć kategoryzacji z DB.

> **Notka (przyszłe rozszerzenie):** wstępny **OCR on-device** (ML Kit na Androidzie,
> Apple Vision na iOS, przez `expect`/`actual`) można dołożyć **przed** wysyłką do chmury —
> jako darmowa/prywatna „fast path". Wtedy Etap A dostawałby tekst zamiast obrazu; reszta
> pipeline'u się nie zmienia.

## Prompty

Każdy prompt to **osobny plik** w `prompts/` w stylu XML (role / instructions / flow /
json_schema / json-fields / **examples**), z placeholderami `{{...}}` podmienianymi przed
wysyłką (`prompts.ts` → `render`).

| Plik | Zmienne | Rola |
|------|---------|------|
| `prompts/extract-receipt.prompt.ts`   | (brak — wejściem jest obraz) | Etap A: zdjęcie (vision) → JSON paragonu |
| `prompts/categorize-items.prompt.ts`  | `{{USER_HISTORY}}`, `{{CATEGORIES}}`, `{{ITEMS}}` | Etap B: pozycje → kategorie |

Treść w środku to czysty prompt; opakowanie `export default \`...\`` służy tylko temu, by
plik na pewno trafił do bundla funkcji (statyczny `.xml` + `Deno.readTextFile` bywa gubiony
w deployu). Few-shot przykłady żyją w plikach promptów — edytuje się je bez ruszania kodu.

## Kontrakt — Request

`POST` z JSON. Podaj obraz (`imagePath` lub `imageBase64`) i `userId`.

| Pole          | Typ        | Wymagane | Opis |
|---------------|------------|----------|------|
| `imagePath`   | `string`   | tak\*    | ścieżka obiektu w prywatnym buckecie, np. `"userId/receiptId.jpg"` |
| `imageBase64` | `string`   | tak\*    | alternatywa do testów: base64 (z/bez prefiksu `data:`) |
| `userId`      | `string`   | tak\*\*  | id usera — funkcja po nim czyta kategorie i pamięć (`product_categories`) |
| `bucket`      | `string`   | nie      | nadpisanie nazwy bucketu (domyślnie env `RECEIPTS_BUCKET` / `receipts`) |
| `currency`    | `string`   | nie      | domyślnie `"PLN"` |
| `categories`  | `string[]` | nie      | override listy kategorii — **pomija** odczyt z DB (test/curl) |
| `examples`    | `{name,category}[]` | nie | override historii few-shot — pomija odczyt `product_categories` (test/curl) |

\* dokładnie jedno z `imagePath` / `imageBase64`.
\*\* `userId` wymagane, **chyba że** podasz `categories` (tryb testowy bez DB).

```json
{
  "imagePath": "11111111-1111-1111-1111-111111111111/abc.jpg",
  "userId": "11111111-1111-1111-1111-111111111111",
  "currency": "PLN"
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

## Kategoryzacja i uczenie (personalizacja)

Etap B działa trójstopniowo:

1. **exact-match z pamięci** — pozycje, których znormalizowana nazwa (lower/trim) pasuje
   dokładnie do wpisu w `product_categories`, dostają kategorię **bez pytania LLM**
   (natychmiast, koszt 0). Jeśli wszystkie pozycje trafią — drugiego calla nie ma.
2. **LLM few-shot** — reszta idzie do Claude z `{{USER_HISTORY}}` (≤50 unikalnych par
   nazwa→kategoria tego usera, najświeższe pierwsze) + pełną listą kategorii. Tak model
   „uczy się" kategorii usera i jego sposobu przypisywania.
3. **fallback** — nieprzypisane → `inne` (jeśli istnieje u usera).

Pamięć to istniejąca tabela `product_categories` (`product_name`, `category_id`,
`source 'manual'|'llm'`, `created_at`). Funkcja **tylko czyta**.

### Pętla uczenia — zadanie po stronie klienta

Żeby model uczył się z korekt, **klient musi zapisywać** finalne kategorie pozycji do
`product_categories` przy zapisie/edycji paragonu (dziś to „Faza 2" — nikt tam nie pisze):

- przy zaakceptowaniu paragonu: `upsert` każdej pozycji `(product_name → category_id)`,
  `source='llm'` gdy zgodne z podpowiedzią, `source='manual'` gdy user zmienił,
- przy edycji kategorii line-itema: `upsert` z `source='manual'` (korekta nadpisuje).

Zalecana zmiana w DB pod czysty upsert (korekta = ostatni zapis wygrywa):

```sql
alter table product_categories add column if not exists updated_at timestamptz not null default now();
create unique index if not exists uq_product_categories_user_name
  on product_categories (user_id, lower(product_name));
-- upsert:
-- insert into product_categories (user_id, product_name, category_id, source, updated_at)
-- values (:u, :name, :cat, :src, now())
-- on conflict (user_id, lower(product_name))
-- do update set category_id = excluded.category_id, source = excluded.source, updated_at = now();
```

Dopóki tabela jest pusta (nowy user), Etap B działa na samych generycznych przykładach
z pliku promptu — bez błędów, po prostu bez personalizacji.

## Błędy

Body: `{ "error": { "code": "...", "message": "..." } }`.

| HTTP | `code`              | Kiedy |
|------|---------------------|-------|
| 400  | `invalid_request`   | zły body, brak `categories`, brak/niedostępny obraz |
| 405  | `method_not_allowed`| metoda inna niż POST |
| 502  | `analysis_failed`   | błąd modelu (vision-ekstrakcja lub kategoryzacja) |
| 500  | `internal`          | brak env / nieoczekiwany błąd |

## Zmienne środowiskowe (sekrety)

| Nazwa                        | Źródło                  | Opis |
|------------------------------|-------------------------|------|
| `ANTHROPIC_API_KEY`          | **ustawiasz**           | klucz Claude API (vision + tekst) |
| `SUPABASE_URL`               | auto (Supabase)         | wstrzykiwany w deployu |
| `SUPABASE_SERVICE_ROLE_KEY`  | auto (Supabase)         | wstrzykiwany w deployu; pobiera obraz ze Storage |
| `RECEIPTS_BUCKET`            | opcjonalnie             | domyślnie `receipts` |
| `EXTRACTION_MODEL`           | opcjonalnie             | domyślnie `claude-haiku-4-5` |
| `CATEGORIZATION_MODEL`       | opcjonalnie             | domyślnie `claude-haiku-4-5` |

Ustawienie sekretów (cloud):

```bash
supabase secrets set ANTHROPIC_API_KEY=sk-ant-...
# opcjonalnie podbij jakość ekstrakcji kosztem ceny (trudne paragony termiczne):
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
    "categories": ["jedzenie podstawowe","energetyki","chemia","słodycze","napoje","inne"],
    "examples": [{"name":"Mleko 2% 1l","category":"jedzenie podstawowe"}]
  }'
```

> W teście podajemy `categories` (i opcjonalnie `examples`) wprost — funkcja pomija wtedy DB.
> W produkcji wysyłasz `userId`, a funkcja sama czyta kategorie i pamięć.

```bash
# produkcyjny kształt żądania:
#   { "imagePath": "<userId>/<receiptId>.jpg", "userId": "<userId>", "currency": "PLN" }
```

## Wywołanie z klienta (supabase-kt, szkic dla integracji)

```kotlin
// w nowym SupabaseReceiptAnalysisRepository : ReceiptAnalysisRepository
val response = supabase.functions.invoke(
    function = "analyze-receipt",
    body = buildJsonObject {
        put("imagePath", imagePath)            // ścieżka obiektu w buckecie 'receipts'
        put("userId", config.userId)           // funkcja sama czyta kategorie + pamięć
        put("currency", config.defaultCurrency)
    },
)
// zdeserializuj do AnalyzeResponse i zbuduj RawReceiptAnalysis (mapowanie wyżej)
```

## Koszt (orientacyjnie, Haiku 4.5)

~**$0.005 / paragon** (Haiku): vision-ekstrakcja (obraz ~1500 tok. + output) ~$0.003 +
kategoryzacja ~$0.002. **Exact-match z pamięci** zbija koszt kategoryzacji — gdy wszystkie
pozycje są znane, drugiego calla nie ma (0 zł). Podbicie `EXTRACTION_MODEL` na Sonnet/Opus
(env) ≈ ×3–×5.
