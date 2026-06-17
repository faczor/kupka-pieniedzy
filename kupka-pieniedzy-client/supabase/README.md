# Supabase — jak wypchnąć migracje

Migracje siedzą w `migrations/` (np. `0001_init.sql`), seed danych w `seed.sql`.
CLI: `supabase` (sprawdź `supabase --version`).

## Stan startowy (do zrobienia raz)

- [ ] `config.toml` — **brak**, trzeba `supabase init`
- [ ] projekt zlinkowany — **nie**, trzeba `supabase link`
- [ ] `supabaseUrl` / `supabaseAnonKey` w configu apki — **puste**, uzupełnić po utworzeniu projektu

---

## A. Cloud (hostowany projekt na supabase.com) — docelowo apka łączy się z tym

```bash
cd kupka-pieniedzy-client

# 1. (raz) zainicjuj config.toml — NIE nadpisuje istniejących migracji
supabase init

# 2. zaloguj CLI (otwiera przeglądarkę)
supabase login

# 3. zlinkuj z projektem
#    PROJECT_REF znajdziesz w URL dashboardu: app.supabase.com/project/<REF>
supabase link --project-ref <PROJECT_REF>

# 4. wypchnij migracje na zdalną bazę
supabase db push

# 5. seed — przy cloud NIE leci automatycznie, trzeba ręcznie
#    connection string: Dashboard > Project Settings > Database > Connection string
psql "<connection-string>" -f supabase/seed.sql
```

`supabase db push` aplikuje tylko te pliki z `migrations/`, których nie ma jeszcze
w tabeli `supabase_migrations.schema_migrations` na zdalnej bazie.

Po tym: uzupełnij `supabaseUrl` + `supabaseAnonKey` w configu apki
(`shared/.../core/config/AppConfig.kt` ładuje z resource; teraz puste w `App.kt`).
Klucze: Dashboard > Project Settings > API.

---

## B. Lokalnie (dev, wymaga Dockera)

```bash
cd kupka-pieniedzy-client

# 1. (raz) config.toml
supabase init

# 2. podnosi lokalny stack; aplikuje migracje + seed.sql AUTOMATYCZNIE
supabase start

# reset lokalnej bazy (ponownie aplikuje migracje + seed)
supabase db reset

# zatrzymanie
supabase stop
```

Lokalnie `start` / `db reset` ładują `seed.sql` same z siebie (w przeciwieństwie do cloud).

---

## Częste operacje

```bash
# nowa migracja (tworzy pusty plik z timestampem w migrations/)
supabase migration new <nazwa>

# status migracji lokalnie vs zdalnie
supabase migration list

# wygeneruj migrację z różnicy schematu (po zmianach w Studio)
supabase db diff -f <nazwa>
```

## Pułapki

- **Bez `supabase init` (brak `config.toml`)** CLI nie zna ścieżki seeda → przy cloud
  seed trzeba puszczać ręcznie przez `psql`.
- `supabase db push` idzie na **zdalną** bazę — upewnij się, że linkujesz właściwy projekt.
- Nie commituj connection stringów ani service_role keya. Anon key może być w configu apki.