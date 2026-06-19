-- 0013_handle_new_user.sql
-- Provisioning realnego usera po pierwszym logowaniu (Apple / kod e-mail). Zamiast seeda (0012):
-- trigger na auth.users zakłada user_settings + domyślne konto. Kategorie startowe nadal tworzy
-- onboarding po stronie klienta (CategoryRepository.provisionInitial). Patrz auth-plan.md §6A.

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.user_settings (user_id, default_currency, onboarding_completed)
  values (new.id, 'PLN', false)
  on conflict (user_id) do nothing;

  -- Jedno domyślne konto (MVP). id z gen_random_uuid() (default kolumny). Idempotentnie — bez
  -- unique na (user_id,name), więc guard `where not exists`, żeby nie dublować przy re-runie.
  insert into public.accounts (user_id, name, type, currency)
  select new.id, 'Moje konto', 'checking', 'PLN'
  where not exists (select 1 from public.accounts where user_id = new.id);

  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row
  execute function public.handle_new_user();
