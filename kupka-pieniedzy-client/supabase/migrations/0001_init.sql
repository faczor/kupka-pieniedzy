-- =============================================================================
-- 0001_init.sql — schemat MVP „kupka pieniędzy"
-- Źródło prawdy: docs/schema.md (9 tabel) + rozszerzenia warstwy data:
--   receipts.status / receipts.confidence / receipts.raw_ocr_json (jsonb)
--   categories.icon / categories.color / categories.is_default (UI badge)
--
-- Auth (D17): wszystkie tabele mają user_id. W MVP user jest HARDCODED.
-- RLS policies napisane, ale RLS POZOSTAJE WYŁĄCZONY do czasu realnego auth.
-- Pieniądze: NUMERIC(12,2) w zł (major). Klient konwertuje na grosze (Money.minorUnits).
-- =============================================================================

create extension if not exists "pgcrypto"; -- gen_random_uuid()

-- -----------------------------------------------------------------------------
-- 0. user_settings (1 wiersz / user)
-- -----------------------------------------------------------------------------
create table if not exists user_settings (
  user_id          uuid primary key,
  default_currency char(3) not null default 'PLN',
  created_at       timestamptz not null default now()
);

-- -----------------------------------------------------------------------------
-- 1. accounts (źródła pieniędzy)
-- -----------------------------------------------------------------------------
create table if not exists accounts (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null,
  name       text not null,
  type       text not null default 'checking'
               check (type in ('checking','savings','card','cash','investment')),
  currency   char(3) not null default 'PLN',
  created_at timestamptz not null default now()
);
create index if not exists idx_accounts_user on accounts(user_id);

-- -----------------------------------------------------------------------------
-- 2. categories (2-poziomowe) + kolumny prezentacyjne
-- -----------------------------------------------------------------------------
create table if not exists categories (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null,
  name       text not null,
  icon       text not null default 'label',   -- ligatura Material Symbols
  color      text not null default '#9AA3B0',  -- hex
  parent_id  uuid references categories(id) on delete set null,
  level      int  not null default 1 check (level in (1,2)),
  is_default boolean not null default false,    -- true tylko dla „inne"
  is_dynamic boolean not null default false,    -- true dla L2
  created_at timestamptz not null default now()
);
create index if not exists idx_categories_user on categories(user_id);
create index if not exists idx_categories_parent on categories(parent_id);
-- najwyżej jedna domyślna kategoria na usera
create unique index if not exists uq_categories_one_default
  on categories(user_id) where is_default;

-- -----------------------------------------------------------------------------
-- 3. budgets (budżet kategorii w okresie)
-- -----------------------------------------------------------------------------
create table if not exists budgets (
  id           uuid primary key default gen_random_uuid(),
  user_id      uuid not null,
  category_id  uuid not null references categories(id) on delete cascade,
  amount       numeric(12,2) not null check (amount > 0),
  period_start date not null,
  period_end   date not null,
  created_at   timestamptz not null default now(),
  check (period_end >= period_start)
);
create index if not exists idx_budgets_user on budgets(user_id);
create index if not exists idx_budgets_category on budgets(category_id);
create index if not exists idx_budgets_period on budgets(period_start, period_end);

-- -----------------------------------------------------------------------------
-- 4. recurring_expenses (subskrypcje / leasing / czynsz)
-- -----------------------------------------------------------------------------
create table if not exists recurring_expenses (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null,
  name        text not null,
  amount      numeric(12,2) not null check (amount > 0),
  category_id uuid not null references categories(id) on delete restrict,
  due_day     int  not null check (due_day between 1 and 28),
  active      boolean not null default true,
  created_at  timestamptz not null default now()
);
create index if not exists idx_recurring_user on recurring_expenses(user_id);

-- -----------------------------------------------------------------------------
-- 5. transactions (expense / income / transfer / refund)
-- -----------------------------------------------------------------------------
create table if not exists transactions (
  id                uuid primary key default gen_random_uuid(),
  user_id           uuid not null,
  date              date not null,
  amount            numeric(12,2) not null,
  type              text not null
                      check (type in ('expense','income','transfer','refund')),
  account_id        uuid not null references accounts(id) on delete restrict,
  category_id       uuid not null references categories(id) on delete restrict,
  source_type       text not null default 'manual'
                      check (source_type in ('manual','screenshot','receipt','recurring')),
  source_ref        text,
  merchant          text,
  description       text,
  transfer_group_id uuid,  -- tylko dla type='transfer'; paruje 2 wiersze
  created_at        timestamptz not null default now(),
  -- D12: transfer_group_id NOT NULL ⇔ type='transfer'
  check ((type = 'transfer') = (transfer_group_id is not null))
);
create index if not exists idx_transactions_user on transactions(user_id);
create index if not exists idx_transactions_date on transactions(user_id, date);
create index if not exists idx_transactions_category on transactions(category_id);
create index if not exists idx_transactions_type on transactions(type);

-- -----------------------------------------------------------------------------
-- 6. receipts (mogą istnieć bez transakcji) + status/confidence/raw_ocr_json
-- -----------------------------------------------------------------------------
create table if not exists receipts (
  id             uuid primary key default gen_random_uuid(),
  user_id        uuid not null,
  transaction_id uuid references transactions(id) on delete set null,
  store          text,
  date           date,
  total          numeric(12,2),
  image_path     text,
  status         text not null default 'pending'
                   check (status in ('pending','ready','saved','failed')),
  confidence     real,
  raw_ocr_json   jsonb,                         -- pełny output analizy (audit + retro)
  created_at     timestamptz not null default now()
);
create index if not exists idx_receipts_user on receipts(user_id);
create index if not exists idx_receipts_status on receipts(user_id, status);

-- -----------------------------------------------------------------------------
-- 7. receipt_category_splits (per-sub-suma; SUM(splits)==receipts.total)
-- -----------------------------------------------------------------------------
create table if not exists receipt_category_splits (
  id          uuid primary key default gen_random_uuid(),
  receipt_id  uuid not null references receipts(id) on delete cascade,
  category_id uuid not null references categories(id) on delete restrict,
  amount      numeric(12,2) not null check (amount >= 0)
);
create index if not exists idx_splits_receipt on receipt_category_splits(receipt_id);
create index if not exists idx_splits_category on receipt_category_splits(category_id);

-- -----------------------------------------------------------------------------
-- 8. product_categories (cache produkt → kategoria; Faza 2)
-- -----------------------------------------------------------------------------
create table if not exists product_categories (
  id           uuid primary key default gen_random_uuid(),
  user_id      uuid not null,
  product_name text not null,
  category_id  uuid not null references categories(id) on delete cascade,
  source       text not null default 'manual' check (source in ('manual','llm')),
  confidence   real,
  created_at   timestamptz not null default now()
);
create index if not exists idx_product_categories_user on product_categories(user_id);

-- =============================================================================
-- WIDOKI
-- =============================================================================

-- recent_entries: transakcja zdenormalizowana o kategorię, BEZ transferów.
-- receipt_item_count = liczba splitów paragonu dopiętego do tej transakcji.
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
  (select count(*) from receipt_category_splits s
     join receipts r on r.id = s.receipt_id
    where r.transaction_id = t.id)              as receipt_item_count
from transactions t
join categories c on c.id = t.category_id
where t.type <> 'transfer';

-- budget_progress: kategoria + budżet bieżącego miesiąca + suma wydana.
-- Wydane = transakcje (expense dodaje, refund odejmuje) na tej kategorii
--          + splity paragonów wskazujące tę (sub)kategorię. Splity należą do
--          paragonu, którego transakcja ma category=spożywka (L1), więc dla L2
--          (energetyki itd.) liczą się TYLKO przez splity, a dla L1 przez sumę
--          transakcji (= receipt.total) — bez double-countingu (patrz schema.md).
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
  group by t.user_id, t.category_id
),
spent_splits as (
  -- Splity liczą się dla (sub)kategorii RÓŻNEJ niż kategoria L1 transakcji paragonu
  -- (zwykle spożywka), by nie dublować L1 liczonej już przez sumę transakcji.
  select r.user_id, s.category_id, sum(s.amount) as spent
  from receipt_category_splits s
  join receipts r on r.id = s.receipt_id
  left join transactions tr on tr.id = r.transaction_id
  cross join month m
  where coalesce(r.date, current_date) between m.m_start and m.m_end
    and (tr.category_id is null or s.category_id <> tr.category_id)
  group by r.user_id, s.category_id
)
select
  c.id                                         as category_id,
  c.user_id                                    as user_id,
  c.name                                       as category_name,
  c.icon                                       as icon,
  c.color                                      as color,
  coalesce(bc.budget_amount, 0)                as budget_amount,
  coalesce(st.spent, 0) + coalesce(ss.spent, 0) as spent_amount
from categories c
left join budget_cur   bc on bc.category_id = c.id and bc.user_id = c.user_id
left join spent_tx     st on st.category_id = c.id and st.user_id = c.user_id
left join spent_splits ss on ss.category_id = c.id and ss.user_id = c.user_id
where coalesce(bc.budget_amount, 0) > 0;  -- Dashboard pokazuje tylko kategorie z budżetem

-- =============================================================================
-- RLS — policies napisane, ale RLS WYŁĄCZONY (MVP hardcoded user, D17).
-- Włączyć: alter table <t> enable row level security; po wprowadzeniu auth.
-- =============================================================================
do $$
declare
  tbl text;
  tables text[] := array[
    'user_settings','accounts','categories','budgets','recurring_expenses',
    'transactions','receipts','product_categories'
  ];
begin
  foreach tbl in array tables loop
    execute format(
      'create policy %I_owner on %I for all using (auth.uid() = user_id) with check (auth.uid() = user_id);',
      tbl, tbl
    );
  end loop;
  -- receipt_category_splits: user_id dziedziczone przez FK → policy przez receipts.
  execute $p$
    create policy receipt_category_splits_owner on receipt_category_splits for all
    using (exists (select 1 from receipts r where r.id = receipt_id and r.user_id = auth.uid()))
    with check (exists (select 1 from receipts r where r.id = receipt_id and r.user_id = auth.uid()));
  $p$;
exception when duplicate_object then
  null; -- policies już istnieją (re-run migracji)
end $$;

-- NOTE (MVP): RLS celowo NIE jest włączony — brak auth, user_id hardcoded.
-- alter table accounts enable row level security; -- itd. dopiero po auth.
