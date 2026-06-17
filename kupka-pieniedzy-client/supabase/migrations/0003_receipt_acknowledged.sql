-- =============================================================================
-- 0003_receipt_acknowledged.sql — „odhaczenie” toasta gotowego paragonu.
-- Toast „paragon gotowy” na Dashboardzie był pochodną statusu `ready`, więc wracał
-- po każdym powrocie na ekran (status zmienia się dopiero przy zapisie/usunięciu).
-- Flaga `acknowledged` pozwala schować notyfikację po pierwszym kliknięciu, trwale
-- (przeżywa restart aplikacji i jest spójna między urządzeniami).
-- Idempotentne (add column if not exists) — bezpieczne przy ponownym db push.
-- =============================================================================

alter table receipts
  add column if not exists acknowledged boolean not null default false;
