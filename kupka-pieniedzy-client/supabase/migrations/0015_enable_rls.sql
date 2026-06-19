-- 0015_enable_rls.sql
-- WŁĄCZENIE RLS — wdrażać NA KOŃCU, gdy klient ustawia user_id z auth.uid() i sesja działa.
-- Policy `<t>_owner` (auth.uid() = user_id) są już napisane w 0001 (+ receipt_items w 0005).
-- Tu tylko włączamy egzekwowanie. Patrz auth-plan.md §9.
--
-- UWAGA: po włączeniu każdy INSERT bez poprawnego user_id zostanie odrzucony przez
-- `with check (auth.uid() = user_id)`. Wymaga usuniętego dev-fallbacku w CurrentUserProvider
-- (insert z AppConfig.userId zamiast auth.uid() poleciałby `new row violates row-level security`).
--
-- Robust: `receipt_category_splits` została usunięta w 0005 (model przeszedł na receipt_items),
-- więc włączamy RLS tylko dla tabel, które realnie istnieją — nie wywracamy się na rozbieżnościach.

do $$
declare
  tbl text;
  tables text[] := array[
    'user_settings','accounts','categories','budgets','recurring_expenses',
    'transactions','receipts','receipt_items','product_categories'
  ];
begin
  foreach tbl in array tables loop
    if to_regclass(format('public.%I', tbl)) is not null then
      execute format('alter table %I enable row level security;', tbl);
    else
      raise notice 'Pomijam RLS — tabela % nie istnieje', tbl;
    end if;
  end loop;
end $$;
