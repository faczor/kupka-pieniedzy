-- =============================================================================
-- 0005_receipt_items.sql
-- Zastępuje `receipt_category_splits` (agregat per kategoria, bez nazw) tabelą
-- `receipt_items` (jedna pozycja paragonu = jeden wiersz: nazwa + kwota + kategoria).
--
-- Cel: klient mobilny czyta ustrukturyzowany model pozycji wprost z tej tabeli,
-- a `receipts.raw_ocr_json` zostaje WYŁĄCZNIE wewnętrznym artefaktem analitycznym
-- (surowy odczyt sprzed kategoryzacji), którego UI już nie dotyka.
--
-- Sumy per (sub)kategoria do budżetów liczą widoki wprost z receipt_items.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- receipt_items (model pozycji paragonu dla klienta)
-- -----------------------------------------------------------------------------
create table if not exists receipt_items (
  id          uuid primary key default gen_random_uuid(),
  receipt_id  uuid not null references receipts(id) on delete cascade,
  user_id     uuid not null,
  position    int  not null default 0,            -- kolejność na paragonie
  name        text not null,                      -- surowa nazwa produktu (np. „Jajka L 10szt")
  amount      numeric(12,2) not null check (amount >= 0),
  category_id uuid references categories(id) on delete set null,  -- null = nieprzypisana
  created_at  timestamptz not null default now()
);
create index if not exists idx_receipt_items_receipt on receipt_items(receipt_id);
create index if not exists idx_receipt_items_user on receipt_items(user_id);
create index if not exists idx_receipt_items_category on receipt_items(category_id);

-- -----------------------------------------------------------------------------
-- Migracja danych: stare splity (bez nazw) -> pozycje (audyt historyczny).
-- -----------------------------------------------------------------------------
insert into receipt_items (receipt_id, user_id, position, name, amount, category_id)
select s.receipt_id, r.user_id, 0, '(zmigrowany split)', s.amount, s.category_id
from receipt_category_splits s
join receipts r on r.id = s.receipt_id;

-- =============================================================================
-- WIDOKI — przeliczane teraz z receipt_items (zamiast receipt_category_splits)
-- =============================================================================

-- recent_entries: receipt_item_count = liczba POZYCJI dopiętego paragonu.
create or replace view recent_entries as
select
  t.id                                          as id,
  t.user_id                                     as user_id,
  coalesce(nullif(t.description, ''), t.merchant, c.name) as title,
  c.name                                        as category_name,
  c.icon                                        as category_icon,
  c.color                                       as category_color,
  t.amount                                      as amount,
  t.type                                        as type,
  t.date                                        as date,
  (select count(*) from receipt_items ri
     join receipts r on r.id = ri.receipt_id
    where r.transaction_id = t.id)              as receipt_item_count
from transactions t
join categories c on c.id = t.category_id
where t.type <> 'transfer';

-- budget_progress: wydane = transakcje (expense - refund) BEZ paragonów rozbitych na pozycje
-- + pozycje paragonów (każda na SWOJEJ (sub)kategorii).
--
-- Model bez double-countingu (D28): paragon rozbity na pozycje liczy się do budżetów WYŁĄCZNIE
-- przez `receipt_items` (per pozycja), a jego transakcja jest WYKLUCZONA ze spent_tx. Kategoria L1
-- transakcji (zwykle spożywka) służy tylko do prezentacji w feedzie — NIE do budżetu. Dzięki temu
-- 30 zł chemii z paragonu spożywczego liczy się w budżecie „chemia", a NIE podwójnie (chemia + L1).
create or replace view budget_progress as
with month as (
  select date_trunc('month', current_date)::date as m_start,
         (date_trunc('month', current_date) + interval '1 month - 1 day')::date as m_end
),
budget_cur as (
  select b.user_id, b.category_id, sum(b.amount) as budget_amount
  from budgets b, month m
  where b.period_start <= m.m_end and b.period_end >= m.m_start
  group by b.user_id, b.category_id
),
spent_tx as (
  select t.user_id, t.category_id,
         sum(case when t.type = 'refund' then -t.amount else t.amount end) as spent
  from transactions t, month m
  where t.type in ('expense','refund')
    and t.date between m.m_start and m.m_end
    -- Transakcje paragonów rozbitych na pozycje liczymy przez receipt_items (niżej), nie tu —
    -- inaczej pełna suma paragonu dublowałaby się z sub-kategoriami.
    and not exists (
      select 1 from receipts r
      join receipt_items ri on ri.receipt_id = r.id
      where r.transaction_id = t.id
    )
  group by t.user_id, t.category_id
),
spent_items as (
  -- Wszystkie pozycje zapisanych paragonów (transaction_id not null), każda na swojej kategorii.
  -- Datę bierzemy z TRANSAKCJI (spójnie z wykluczeniem powyżej — ten sam miesiąc).
  select r.user_id, ri.category_id, sum(ri.amount) as spent
  from receipt_items ri
  join receipts r on r.id = ri.receipt_id
  join transactions tr on tr.id = r.transaction_id
  cross join month m
  where ri.category_id is not null
    and tr.date between m.m_start and m.m_end
  group by r.user_id, ri.category_id
)
select
  c.id                                         as category_id,
  c.user_id                                    as user_id,
  c.name                                       as category_name,
  c.icon                                       as icon,
  c.color                                      as color,
  coalesce(bc.budget_amount, 0)                as budget_amount,
  coalesce(st.spent, 0) + coalesce(si.spent, 0) as spent_amount
from categories c
left join budget_cur bc on bc.category_id = c.id and bc.user_id = c.user_id
left join spent_tx   st on st.category_id = c.id and st.user_id = c.user_id
left join spent_items si on si.category_id = c.id and si.user_id = c.user_id
where coalesce(bc.budget_amount, 0) > 0;

-- =============================================================================
-- Usuń starą tabelę splitów (widoki już jej nie używają). Policy znika z CASCADE.
-- =============================================================================
drop table if exists receipt_category_splits cascade;

-- =============================================================================
-- RLS — policy napisana, ale RLS POZOSTAJE WYŁĄCZONY (MVP hardcoded user, D17),
-- spójnie z resztą tabel z 0001.
-- =============================================================================
do $$
begin
  create policy receipt_items_owner on receipt_items for all
    using (auth.uid() = user_id) with check (auth.uid() = user_id);
exception when duplicate_object then
  null;
end $$;
