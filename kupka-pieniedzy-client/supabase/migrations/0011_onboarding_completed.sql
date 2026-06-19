-- 0011_onboarding_completed.sql
-- Flaga zakończenia onboardingu (per user, 1 wiersz w user_settings).
-- Domyślnie false — istniejący zaszyty user również przejdzie onboarding raz po wdrożeniu
-- (świadoma decyzja: chcemy przejść flow E2E na własnym koncie).

alter table user_settings
  add column if not exists onboarding_completed boolean not null default false;
