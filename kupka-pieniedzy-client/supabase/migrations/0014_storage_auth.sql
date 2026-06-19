-- 0014_storage_auth.sql
-- Storage `receipts`: zamiana policy z roli `anon` (MVP bez auth) na `authenticated` z właścicielem
-- po pierwszym segmencie ścieżki `<user_id>/<receipt_id>.jpg` = auth.uid(). Funkcja analyze-receipt
-- czyta przez service_role (omija RLS) — jej te policy nie dotyczą.

drop policy if exists receipts_anon_select on storage.objects;
drop policy if exists receipts_anon_insert on storage.objects;
drop policy if exists receipts_anon_update on storage.objects;
drop policy if exists receipts_anon_delete on storage.objects;

do $$
begin
  create policy receipts_owner_select on storage.objects
    for select to authenticated
    using (bucket_id = 'receipts' and (storage.foldername(name))[1] = auth.uid()::text);
  create policy receipts_owner_insert on storage.objects
    for insert to authenticated
    with check (bucket_id = 'receipts' and (storage.foldername(name))[1] = auth.uid()::text);
  create policy receipts_owner_update on storage.objects
    for update to authenticated
    using (bucket_id = 'receipts' and (storage.foldername(name))[1] = auth.uid()::text)
    with check (bucket_id = 'receipts' and (storage.foldername(name))[1] = auth.uid()::text);
  create policy receipts_owner_delete on storage.objects
    for delete to authenticated
    using (bucket_id = 'receipts' and (storage.foldername(name))[1] = auth.uid()::text);
exception when duplicate_object then
  null; -- policies już istnieją (re-run migracji)
end $$;
