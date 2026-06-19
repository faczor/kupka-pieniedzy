-- 0012_fresh_user.sql
-- Świeży user pod onboarding E2E. Zakłada tylko user_settings + konto domyślne.
-- Kategorie/budżety NIE są seedowane — tworzy je onboarding (provisioning po stronie klienta:
-- 6 startowych + nieusuwalne „inne”). Stary seed (0002) zostaje pod starym userem
-- (00000000-…-0001), osierocony i niewidoczny w aplikacji — historia nie jest kasowana.

insert into user_settings (user_id, default_currency, onboarding_completed)
values ('00000000-0000-0000-0000-000000000002', 'PLN', false)
on conflict (user_id) do nothing;

insert into accounts (id, user_id, name, type, currency) values
  ('00000000-0000-0000-0000-0000000000a2',
   '00000000-0000-0000-0000-000000000002', 'Moje konto', 'checking', 'PLN')
on conflict (id) do nothing;
