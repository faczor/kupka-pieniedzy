# TODO — kupka pieniędzy (OCR paragonów)

Świadomie odłożone / niedokończone rzeczy. Aktualizować przy zmianach.

## Wymagane do pełnego działania OCR

- [ ] **Zapis do `product_categories` (pętla uczenia).** Edge Function `analyze-receipt`
      CZYTA pamięć kategoryzacji, ale klient jeszcze tam NIE PISZE. Przy zapisie/edycji
      kategorii line-itema zrobić `upsert` (`product_name` → `category_id`,
      `source 'manual'|'llm'`). Bez tego personalizacja i exact-match nie mają danych.
      Zalecany SQL (unikalny indeks + `updated_at`) jest w
      `supabase/functions/analyze-receipt/README.md`.

- [x] **Ponowna analiza paragonu (`reanalyze`) — WŁĄCZONA.** Po wdrożeniu Storage zdjęcie
      jest trwałe, więc `ReceiptService.reanalyze` (markPending → analiza po `image_path`)
      działa z poziomu ekranu paragonu i arkusza „w toku" na liście Wpisy. (branch
      `feature/receipt-image-storage`).

## Decyzje podjęte

- [x] **Zdjęcie: base64 → Storage.** Wybrano Storage. Zdjęcie (znormalizowany JPEG) ląduje
      w prywatnym buckecie `receipts` (`<user_id>/<receipt_id>.jpg`, migracja `0007`),
      a do Edge Function idzie `image_path` zamiast base64 (funkcja czyta przez
      service_role). Odblokowuje reanalyze + podgląd zdjęcia.

## Decyzje do podjęcia

- [ ] **Auth + zacieśnienie RLS Storage.** MVP używa anon key i hardcoded `userId`.
      Polityki `storage.objects` na buckecie `receipts` są celowo permisywne dla roli
      `anon` (NIE zawężają po `(storage.foldername(name))[1]`), więc każdy z anon keyem
      czyta/usuwa cudze zdjęcia — spójne z RLS-off na tabelach (D17), ale to dług
      bezpieczeństwa do spłaty wraz z realnym logowaniem (zawęzić `using` do
      `auth.uid()::text = (storage.foldername(name))[1]`).

## Weryfikacja

- [ ] **Build + smoke test.** Edge Function i integracja klienta NIEZWERYFIKOWANE na
      żywo. Deploy funkcji (`supabase functions deploy analyze-receipt`, sekret
      `ANTHROPIC_API_KEY`) + `./gradlew build` + 1 realny paragon. Punkty ryzyka:
      `HttpClient()` bez jawnego engine, `kotlin.io.encoding.Base64` (opt-in zależny
      od wersji Kotlina).

## Na później (nice-to-have)

- [ ] **OCR on-device** (ML Kit / Apple Vision) jako „fast path" przed chmurą —
      darmowe/prywatne; Etap A dostawałby tekst zamiast obrazu.
- [ ] **Model ekstrakcji** — przy słabych paragonach termicznych podbić
      `EXTRACTION_MODEL` (env) na Sonnet/Opus.
