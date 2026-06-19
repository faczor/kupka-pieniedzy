-- =============================================================================
-- 0008_backfill_ready_receipt_transactions.sql
-- Backfill: paragony „przetworzone, lecz niezatwierdzone” (status `ready`) bez transakcji.
--
-- Kontekst: do tej zmiany transakcja paragonu powstawała dopiero przy ZATWIERDZENIU
-- (status `saved`). Paragony, których użytkownik nie zatwierdził na czas (toast „gotowy”
-- auto-acknowledge po 5 s), zostawały jako `ready` + `transaction_id is null` — bez transakcji
-- są NIEWIDOCZNE w feedzie/sumach/budżetach (widoki kluczują po `transaction_id`).
--
-- Od teraz transakcja powstaje już w momencie analizy (`ready`). Ten skrypt „odzyskuje” istniejące
-- osierocone paragony: tworzy im transakcję (kategoria L1 „spożywka” lub domyślna, pierwsze konto
-- usera) i podpina ją. Status zostaje `ready` — wpis pojawi się z badge „do zatwierdzenia”,
-- a użytkownik dokończy przypisanie kategorii pozycji w review.
--
-- Idempotentne: po podpięciu transakcji paragon nie spełnia już warunku (transaction_id not null),
-- więc ponowne uruchomienie nic nie zrobi.
-- =============================================================================

do $$
declare
  r          record;
  v_category uuid;
  v_account  uuid;
  v_tx       uuid;
begin
  for r in
    select id, user_id, date, total, store
    from receipts
    where status = 'ready'
      and transaction_id is null
      and date is not null
      and total is not null
  loop
    -- Kategoria nagłówka: L1 „spożywka” dla usera, w razie braku — domyślna („inne”).
    select id into v_category
    from categories
    where user_id = r.user_id and level = 1 and lower(name) = 'spożywka'
    limit 1;

    if v_category is null then
      select id into v_category
      from categories
      where user_id = r.user_id and is_default
      limit 1;
    end if;

    -- Konto: pierwsze konto usera (konwencja MVP, jak w SupabaseAccountRepository).
    select id into v_account
    from accounts
    where user_id = r.user_id
    order by created_at asc
    limit 1;

    if v_category is null or v_account is null then
      raise notice 'Pomijam paragon % — brak kategorii (%) lub konta (%)', r.id, v_category, v_account;
      continue;
    end if;

    insert into transactions
      (user_id, date, amount, type, account_id, category_id, source_type, merchant)
    values
      (r.user_id, r.date, r.total, 'expense', v_account, v_category, 'receipt', r.store)
    returning id into v_tx;

    update receipts set transaction_id = v_tx where id = r.id;
  end loop;
end $$;
