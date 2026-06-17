-- =============================================================================
-- 0002_seed.sql — dane startowe MVP jako MIGRACJA (user hardcoded, D17).
-- Świadomie migracja, nie luźny seed: w MVP jest jeden, stały user, więc konto +
-- taksonomia + startowe budżety są częścią schematu aplikacji.
-- Idempotentne (on conflict do nothing, stałe UUID) — bezpieczne przy ponownym db push.
-- Taksonomia: 10 L1 + L2 spożywki (max 2 poziomy). Ikony Material Symbols,
-- kolory z palety design system v1. Budżety w zł.
-- =============================================================================

-- user + ustawienia
insert into user_settings (user_id, default_currency)
values ('00000000-0000-0000-0000-000000000001', 'PLN')
on conflict (user_id) do nothing;

-- konto domyślne (getDefaultAccountId bierze pierwsze konto usera)
insert into accounts (id, user_id, name, type, currency) values
  ('00000000-0000-0000-0000-0000000000a1',
   '00000000-0000-0000-0000-000000000001', 'PKO prywatne', 'checking', 'PLN')
on conflict (id) do nothing;

-- ---------------------------------------------------------------------------
-- Kategorie L1 (level=1, parent_id=null)
-- ---------------------------------------------------------------------------
insert into categories (id, user_id, name, icon, color, parent_id, level, is_default, is_dynamic) values
  ('00000000-0000-0000-0000-0000000000c1', '00000000-0000-0000-0000-000000000001', 'spożywka',       'shopping_cart',   '#7BAE5C', null, 1, false, false),
  ('00000000-0000-0000-0000-0000000000c2', '00000000-0000-0000-0000-000000000001', 'eats',           'restaurant',      '#E8B547', null, 1, false, false),
  ('00000000-0000-0000-0000-0000000000c3', '00000000-0000-0000-0000-000000000001', 'auto',           'directions_car',  '#5FA1A0', null, 1, false, false),
  ('00000000-0000-0000-0000-0000000000c4', '00000000-0000-0000-0000-000000000001', 'transport',      'directions_bus',  '#8AA6E0', null, 1, false, false),
  ('00000000-0000-0000-0000-0000000000c5', '00000000-0000-0000-0000-000000000001', 'zdrowie',        'medical_services','#D85B4A', null, 1, false, false),
  ('00000000-0000-0000-0000-0000000000c6', '00000000-0000-0000-0000-000000000001', 'kosmetyki',      'spa',             '#C77BA0', null, 1, false, false),
  ('00000000-0000-0000-0000-0000000000c7', '00000000-0000-0000-0000-000000000001', 'dom',            'home',            '#9B7FC4', null, 1, false, false),
  ('00000000-0000-0000-0000-0000000000c8', '00000000-0000-0000-0000-000000000001', 'finanse',        'savings',         '#5FA1A0', null, 1, false, false),
  ('00000000-0000-0000-0000-0000000000c9', '00000000-0000-0000-0000-000000000001', 'prezenty/okazje','redeem',          '#E8B547', null, 1, false, false),
  ('00000000-0000-0000-0000-0000000000ca', '00000000-0000-0000-0000-000000000001', 'inne',           'label',           '#9AA3B0', null, 1, true,  false)
on conflict (id) do nothing;

-- ---------------------------------------------------------------------------
-- Sub-kategorie spożywki (level=2, parent_id=spożywka, is_dynamic=true)
-- ---------------------------------------------------------------------------
insert into categories (id, user_id, name, icon, color, parent_id, level, is_default, is_dynamic) values
  ('00000000-0000-0000-0000-0000000000d1', '00000000-0000-0000-0000-000000000001', 'energetyki',         'bolt',               '#D85B4A', '00000000-0000-0000-0000-0000000000c1', 2, false, true),
  ('00000000-0000-0000-0000-0000000000d2', '00000000-0000-0000-0000-000000000001', 'napoje',             'local_drink',        '#5FA1A0', '00000000-0000-0000-0000-0000000000c1', 2, false, true),
  ('00000000-0000-0000-0000-0000000000d3', '00000000-0000-0000-0000-000000000001', 'słodycze',           'cookie',             '#C77BA0', '00000000-0000-0000-0000-0000000000c1', 2, false, true),
  ('00000000-0000-0000-0000-0000000000d4', '00000000-0000-0000-0000-000000000001', 'jedzenie podstawowe','bakery_dining',      '#7BAE5C', '00000000-0000-0000-0000-0000000000c1', 2, false, true),
  ('00000000-0000-0000-0000-0000000000d5', '00000000-0000-0000-0000-000000000001', 'chemia',             'cleaning_services',  '#8AA6E0', '00000000-0000-0000-0000-0000000000c1', 2, false, true),
  ('00000000-0000-0000-0000-0000000000d6', '00000000-0000-0000-0000-000000000001', 'alkohol',            'liquor',             '#9B7FC4', '00000000-0000-0000-0000-0000000000c1', 2, false, true)
on conflict (id) do nothing;

-- ---------------------------------------------------------------------------
-- Budżety na bieżący miesiąc (period = pierwszy..ostatni dzień tego miesiąca)
-- spożywka 600, eats 400, energetyki 250, auto 600 (zł)
-- ---------------------------------------------------------------------------
insert into budgets (id, user_id, category_id, amount, period_start, period_end) values
  ('00000000-0000-0000-0000-0000000000b1', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000c1', 600.00,
     date_trunc('month', current_date)::date,
     (date_trunc('month', current_date) + interval '1 month - 1 day')::date),
  ('00000000-0000-0000-0000-0000000000b2', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000c2', 400.00,
     date_trunc('month', current_date)::date,
     (date_trunc('month', current_date) + interval '1 month - 1 day')::date),
  ('00000000-0000-0000-0000-0000000000b3', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000d1', 250.00,
     date_trunc('month', current_date)::date,
     (date_trunc('month', current_date) + interval '1 month - 1 day')::date),
  ('00000000-0000-0000-0000-0000000000b4', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000c3', 600.00,
     date_trunc('month', current_date)::date,
     (date_trunc('month', current_date) + interval '1 month - 1 day')::date)
on conflict (id) do nothing;
