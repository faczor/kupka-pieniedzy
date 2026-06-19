-- =============================================================================
-- Storage: prywatny bucket `receipts` na zdjęcia paragonów.
--
-- Po co: trwałe zdjęcie umożliwia (1) ponowną analizę (reanalyze) bez ponownego
-- wskazywania pliku oraz (2) podgląd zdjęcia z poziomu wpisu. Wcześniej zdjęcie
-- szło do Edge Function jako base64 i nie było nigdzie zapisywane (patrz TODO.md).
--
-- Konwencja ścieżki obiektu: `<user_id>/<receipt_id>.jpg` — pierwszy segment to
-- user_id, co pozwoli docelowo zacieśnić policy do `auth.uid()` po wprowadzeniu auth.
-- =============================================================================

-- Bucket prywatny (nie-publiczny). Limit 10 MB, tylko formaty obsługiwane przez Claude vision.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'receipts',
  'receipts',
  false,
  10485760, -- 10 MiB
  array['image/jpeg', 'image/png', 'image/webp']
)
on conflict (id) do nothing;

-- =============================================================================
-- Policies na storage.objects — MVP: dostęp dla roli `anon` (spójnie z tabelami,
-- które też mają RLS wyłączony / hardcoded user, D17). RLS na storage.objects jest
-- włączony przez Supabase i NIE da się go wyłączyć, więc tu policy są realne.
-- Docelowo (po auth) zacieśnić `using` do: (storage.foldername(name))[1] = auth.uid()::text.
-- Funkcja `analyze-receipt` czyta przez service_role (omija RLS) — jej te policy nie dotyczą.
-- =============================================================================
do $$
begin
  create policy receipts_anon_select on storage.objects
    for select to anon using (bucket_id = 'receipts');
  create policy receipts_anon_insert on storage.objects
    for insert to anon with check (bucket_id = 'receipts');
  create policy receipts_anon_update on storage.objects
    for update to anon using (bucket_id = 'receipts') with check (bucket_id = 'receipts');
  create policy receipts_anon_delete on storage.objects
    for delete to anon using (bucket_id = 'receipts');
exception when duplicate_object then
  null; -- policies już istnieją (re-run migracji)
end $$;
