-- =============================================================================
-- 0006_trends.sql — widoki dla ekranu „Trendy" (wgląd w czasie).
--
-- Dwa widoki z wymiarem MIESIĄCA (date_trunc('month', date)):
--   * month_total_spend     — suma wydatków per miesiąc (cała appka).
--   * category_month_spend   — suma per (kategoria, miesiąc).
--
-- category_month_spend powiela logikę „bez double-countingu" z budget_progress (0005):
-- paragon rozbity na pozycje liczy się WYŁĄCZNIE przez receipt_items (per (sub)kategoria),
-- a jego transakcja jest wykluczona z sumy transakcji — inaczej pełna kwota paragonu
-- dublowałaby sub-kategorie. Konwencja kwot: expense dodaje, refund odejmuje. Transfery
-- i income wykluczone (D12).
--
-- Widoki są read-only; RLS pozostaje wyłączony (MVP, hardcoded user, D17), spójnie z 0001/0005.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- month_total_spend: suma per miesiąc (wszystkie kategorie).
-- Dla SUMY globalnej liczymy kwoty transakcji WPROST — paragon rozbity ma transakcję
-- równą receipt.total (pełna kwota), więc receipt_items NIE dodajemy (to tylko redystrybucja
-- wewnątrz kategorii; dla sumy dałoby double-count).
-- -----------------------------------------------------------------------------
create or replace view month_total_spend as
select
  t.user_id                                                        as user_id,
  date_trunc('month', t.date)::date                                as month_start,
  sum(case when t.type = 'refund' then -t.amount else t.amount end) as spent
from transactions t
where t.type in ('expense', 'refund')
group by t.user_id, date_trunc('month', t.date);

-- -----------------------------------------------------------------------------
-- category_month_spend: suma per (kategoria, miesiąc), bez double-countingu paragonów.
-- spent_tx  — transakcje expense/refund Z WYŁĄCZENIEM tych z paragonem rozbitym na pozycje.
-- spent_items — pozycje paragonów (każda na swojej (sub)kategorii); datę bierzemy z TRANSAKCJI
--               (spójnie z wykluczeniem powyżej — ten sam miesiąc).
-- -----------------------------------------------------------------------------
create or replace view category_month_spend as
with tx as (
  select
    t.user_id,
    t.category_id,
    date_trunc('month', t.date)::date as month_start,
    sum(case when t.type = 'refund' then -t.amount else t.amount end) as spent
  from transactions t
  where t.type in ('expense', 'refund')
    and not exists (
      select 1 from receipts r
      join receipt_items ri on ri.receipt_id = r.id
      where r.transaction_id = t.id
    )
  group by t.user_id, t.category_id, date_trunc('month', t.date)
),
items as (
  select
    r.user_id,
    ri.category_id,
    date_trunc('month', tr.date)::date as month_start,
    sum(ri.amount) as spent
  from receipt_items ri
  join receipts r on r.id = ri.receipt_id
  join transactions tr on tr.id = r.transaction_id
  where ri.category_id is not null
  group by r.user_id, ri.category_id, date_trunc('month', tr.date)
),
combined as (
  select user_id, category_id, month_start, spent from tx
  union all
  select user_id, category_id, month_start, spent from items
)
select
  user_id,
  category_id,
  month_start,
  sum(spent) as spent
from combined
group by user_id, category_id, month_start;
