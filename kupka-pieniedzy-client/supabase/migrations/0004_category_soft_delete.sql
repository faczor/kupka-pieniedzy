-- =============================================================================
-- 0004 — Miękkie usuwanie kategorii (soft-delete / dezaktywacja)
-- -----------------------------------------------------------------------------
-- Zarządzanie kategoriami (edycja + usuwanie) z designu „Kategorie".
-- Decyzja: usuwanie kategorii to pod spodem DEZAKTYWACJA, nie fizyczny DELETE —
-- wpisy historyczne (transactions / receipt_category_splits) zachowują swoją
-- etykietę nawet po „usunięciu" kategorii (opcja „Zostaw bez zmian"). Kategoria
-- z `active=false` znika tylko z listy i z wyboru przy nowych wydatkach.
--
-- Idempotentna — można puszczać wielokrotnie.
-- =============================================================================

alter table categories
  add column if not exists active boolean not null default true;

-- Lista i pickery operują na aktywnych kategoriach — indeks pod ten filtr.
create index if not exists idx_categories_active
  on categories(user_id) where active;
