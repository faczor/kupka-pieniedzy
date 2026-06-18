# TODO — kupka pieniędzy (OCR paragonów)

Świadomie odłożone / niedokończone rzeczy. Aktualizować przy zmianach.

## Wymagane do pełnego działania OCR

- [ ] **Zapis do `product_categories` (pętla uczenia).** Edge Function `analyze-receipt`
      CZYTA pamięć kategoryzacji, ale klient jeszcze tam NIE PISZE. Przy zapisie/edycji
      kategorii line-itema zrobić `upsert` (`product_name` → `category_id`,
      `source 'manual'|'llm'`). Bez tego personalizacja i exact-match nie mają danych.
      Zalecany SQL (unikalny indeks + `updated_at`) jest w
      `supabase/functions/analyze-receipt/README.md`.

- [ ] **Ponowna analiza paragonu (`reanalyze`) — WYŁĄCZONA.**
      `ReceiptViewModel.reanalyze` pokazuje tylko toast. Powód: w wariancie base64 nie
      przechowujemy zdjęcia, więc nie ma czego ponownie wysłać. Przywrócić po wdrożeniu
      Storage albo po dodaniu flow „re-pick" na ekranie review.

## Decyzje do podjęcia

- [ ] **Zdjęcie: base64 vs Storage.** Teraz wysyłamy base64 inline (proste, bez bucketu
      i bez Auth). Storage daje trwałe zdjęcie (audyt, `image_path`, reanalyze), ale
      wymaga prywatnego bucketu `receipts` + polityki RLS dla anon. Rozstrzygnąć przy
      wdrażaniu realnego logowania.

- [ ] **Auth.** MVP używa anon key (przechodzi `verify_jwt` funkcji) i hardcoded
      `userId`. Docelowo realne logowanie + włączenie RLS na tabelach.

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
