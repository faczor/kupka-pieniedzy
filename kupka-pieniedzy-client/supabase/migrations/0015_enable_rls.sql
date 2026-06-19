-- 0015_enable_rls.sql
-- WŁĄCZENIE RLS — wdrażać NA KOŃCU, gdy klient ustawia user_id z auth.uid() i sesja działa.
-- Policy `<t>_owner` (auth.uid() = user_id) są już napisane w 0001 (+ receipt_items w 0005,
-- receipt_category_splits przez FK). Tu tylko włączamy egzekwowanie. Patrz auth-plan.md §9.
--
-- UWAGA: po włączeniu każdy INSERT bez poprawnego user_id zostanie odrzucony przez
-- `with check (auth.uid() = user_id)`. Przed wdrożeniem usuń dev-fallback w CurrentUserProvider
-- (insert z AppConfig.userId zamiast auth.uid() poleci `new row violates row-level security`).

alter table user_settings            enable row level security;
alter table accounts                 enable row level security;
alter table categories               enable row level security;
alter table budgets                  enable row level security;
alter table recurring_expenses       enable row level security;
alter table transactions             enable row level security;
alter table receipts                 enable row level security;
alter table receipt_items            enable row level security;
alter table receipt_category_splits  enable row level security;
alter table product_categories       enable row level security;
