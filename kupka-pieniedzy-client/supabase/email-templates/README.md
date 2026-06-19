# Szablony e-mail (Supabase Auth)

Supabase nie wczytuje tych plików automatycznie — to **źródło prawdy w repo**, które
wklejasz ręcznie w dashboardzie.

## Logowanie kodem (OTP) — `magic-link.html`

1. **Authentication → Emails → „Magic Link"**.
2. **Subject (temat):** `Twój kod logowania — Kupka Pieniędzy`
3. **Message body:** przełącz na widok **HTML / source** i wklej całą zawartość `magic-link.html`.
4. Zapisz.

> Wymaga: **„Confirm email" = OFF** (Authentication → Providers → Email), żeby nowy user
> dostawał ten szablon (logowanie), a nie „Confirm signup" (rejestracja z linkiem).
> Jeśli kiedyś zostawisz „Confirm email" ON — wklej ten sam HTML także do „Confirm signup".

## Zmienne użyte w szablonie
- `{{ .Token }}` — 6-cyfrowy kod (główny element, duży, mono).
- `{{ .Email }}` — adres odbiorcy (stopka).
- Celowo **bez** `{{ .ConfirmationURL }}` — logujemy się kodem, nie linkiem (brak deep linku).

## Styl
Design system „kupka pieniędzy": teal `#5FA1A0`, Manrope + mono, cool-neutral surfaces.
Layout tabelowy + inline style (kompatybilność z klientami pocztowymi); web-fonty łapie
głównie Apple Mail, reszta spada na fallbacki systemowe/mono.
