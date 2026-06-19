# Plan: pełna autoryzacja (Apple + Google) — track #6

> Dokument dla kolejnego agenta. Opisuje, jak zamienić **atrapę logowania** (`StubAuthService`),
> która powstała w PR #17 (onboarding, branch `feat/onboarding-flow`), na **prawdziwy Supabase Auth**
> (GoTrue) z Apple/Google, sesją, RLS i provisioningiem usera. Zachowuje warstwy projektu
> (Screen → ViewModel → Service → Repository → Impl, `Outcome<T>`, 0 hardcode w UI, teksty z `PlStrings`).

## 0. Stan wyjściowy (co już jest po PR #17)

- **Kontrakt gotowy:** `domain/auth/AuthService.kt` — `interface AuthService { val session: StateFlow<AuthSession?>; suspend fun signIn(provider: AuthProvider): Outcome<AuthSession>; suspend fun signOut(): Outcome<Unit> }`, `AuthSession(userId, provider)`, `enum AuthProvider { Google, Apple }`.
- **Atrapa:** `data/auth/StubAuthService.kt` — „loguje" jako hardcoded user z `AppConfig.userId`, sesja tylko w pamięci. Zbindowana w `data/di/DataModule.kt` (`single<AuthService> { StubAuthService(...) }`).
- **Gating:** `App.kt` → `AppRoot()` woła `OnboardingService.isCompleted()` i wybiera start (onboarding vs Dashboard). **Jest tam `TODO(auth track)`**: start ma zależeć też od sesji.
- **Ekran logowania:** `feature/onboarding/OnboardingLoginScreen.kt` + `OnboardingLoginViewModel.kt` — wołają `authService.signIn(provider)`; po sukcesie nawigują (returning → Dashboard, else → Categories).
- **userId:** wszędzie z `AppConfig.userId` (hardcoded `…0002`). Każde repo filtruje `eq("user_id", config.userId)`. To trzeba przestawić na `auth.uid()`.
- **Supabase client:** `data/supabase/SupabaseClientProvider.kt` instaluje **tylko `Postgrest` + `Storage`** — brak `Auth`/GoTrue.
- **RLS:** policy `auth.uid() = user_id` są napisane w `supabase/migrations/0001_init.sql`, ale **WYŁĄCZONE**. Storage ma policy dla roli `anon` (`0007_receipts_storage.sql`).
- **Brak lokalnej persistencji** (multiplatform-settings/DataStore) — potrzebna do trzymania sesji.
- **Provisioning usera:** onboarding tworzy kategorie (`CategoryRepository.provisionInitial`), ale `user_settings` + konto domyślne dla `…0001/…0002` są **seedowane migracją** (`0002`, `0012`) — realny nowy user musi je dostać inaczej (trigger, patrz §6).

## 1. Decyzje do podjęcia PRZED kodem (zapytaj Sebastiana)

1. **Sposób logowania (provider flow):**
   - **(A) `compose-auth` — natywne** (rekomendowane UX): Google przez Credential Manager (Android) / natywnie (iOS), Apple natywnie na iOS, web-fallback Apple na Androidzie. Więcej konfiguracji (serverClientId), lepszy UX, zgodne z wytycznymi Apple/Google.
   - **(B) OAuth przez przeglądarkę + deep link** (prościej): `supabase.auth.signInWith(Google/Apple) { ... }` otwiera browser/ASWebAuthenticationSession, redirect wraca deep linkiem. Jeden kod na obie platformy, słabszy UX, ale szybciej.
   - Rekomendacja: **A dla Google, A (iOS) + B (Android) dla Apple** (Apple na Androidzie jest tylko web).
2. **Strategia danych:** realny user startuje **świeżo** (onboarding go provisionuje) — **bez migracji** danych `…0001/…0002`. Potwierdź, że nie migrujemy istniejących paragonów/transakcji do pierwszego realnego konta. (Jeśli trzeba migrować — osobny, ręczny skrypt SQL mapujący `user_id`.)
3. **Persistencja sesji:** `multiplatform-settings` (russhwolf) jako backend `SettingsSessionManager` supabase-kt. Potwierdź dodanie zależności.
4. **Kto robi konfigurację dev** (konta): Sebastian ma App Store/Play + Anthropic; Google Cloud + Apple Developer + Supabase dashboard wymagają jego dostępu (patrz §3).

## 2. Konfiguracja zewnętrzna (poza repo — wymaga Sebastiana)

- **Supabase dashboard → Authentication → Providers:**
  - włącz **Google** (Client ID + Secret z Google Cloud — „Web client"),
  - włącz **Apple** (Service ID, Team ID, Key ID, `.p8` key),
  - **Redirect URLs / deep link:** dodaj schemat aplikacji, np. `com.sd.kupka_pieniedzy_client://login-callback` (Site URL + Additional Redirect URLs).
- **Google Cloud Console → Credentials:** OAuth client IDs:
  - **Web** (dla Supabase + `serverClientId` compose-auth),
  - **Android** (package `com.sd.kupka_pieniedzy_client` + SHA-1 debug i release),
  - **iOS** (bundle `com.pennypile.client` — patrz deploy iOS).
- **Apple Developer:**
  - **App ID** z capability „Sign in with Apple",
  - **Service ID** (dla web/Android flow) + return URL = Supabase callback,
  - **Sign in with Apple Key** (`.p8`) → do Supabase.
- **Deep link scheme** spójny w: Supabase redirect, AndroidManifest intent-filter, iOS Info.plist URL types.

## 3. Zależności (`gradle/libs.versions.toml` + `shared/build.gradle.kts`)

- `libs.versions.toml` → dodać biblioteki (wersje z `supabase-bom`):
  - `supabase-auth = { module = "io.github.jan-tennert.supabase:auth-kt" }`
  - (jeśli flow A) `supabase-compose-auth = { module = "io.github.jan-tennert.supabase:compose-auth" }`
  - `multiplatform-settings = { module = "com.russhwolf:multiplatform-settings", version = "1.2.0" }` (+ `multiplatform-settings-no-arg` jeśli użyjesz domyślnego konstruktora — na Androidzie wymaga inicjalizacji Contextem).
- `shared/build.gradle.kts` → `commonMain.dependencies { implementation(libs.supabase.auth); implementation(libs.multiplatform.settings); /* opc. compose-auth */ }`. Ktor engines (okhttp/darwin) już są.
- **Android:** Credential Manager (flow A) → `androidx.credentials:credentials` + `androidx.credentials:credentials-play-services-auth` + `com.google.android.libraries.identity.googleid:googleid` w `androidApp` (lub androidMain).

## 4. Warstwa data — klient + sesja + userId

### 4.1 `SupabaseClientProvider.kt`
- `install(Auth) { /* sessionManager = SettingsSessionManager(Settings()); flowType = FlowType.PKCE; scheme/host dla deep linku */ }`.
- Wystaw `val auth get() = client.auth`.
- Sesja persystowana automatycznie przez `SettingsSessionManager` (Android: SharedPreferences, iOS: NSUserDefaults — przez multiplatform-settings).

### 4.2 Źródło userId — **kluczowy refaktor**
- Wprowadź `data/auth/CurrentUserProvider.kt` (lub rozszerz `AuthService`): `fun currentUserId(): String?` / `fun requireUserId(): String` czytające `supabase.auth.currentUserOrNull()?.id`.
- **Zamień `config.userId` na `currentUserProvider.requireUserId()`** w repozytoriach (8 plików): `SupabaseAccountRepository`, `SupabaseCategoryRepository`, `SupabaseTransactionRepository`, `SupabaseBudgetRepository`, `SupabaseReceiptRepository`, `SupabaseTrendsRepository`, `SupabaseOnboardingRepository`, (+ `SupabaseFunctionReceiptAnalysisRepository` jeśli używa userId).
- Przy RLS włączonym `SELECT`-y i tak są filtrowane przez `auth.uid()`; filtry `eq("user_id", …)` mogą zostać (defensywne), ale **INSERT-y muszą ustawiać `user_id = currentUserId`**.
- `AppConfig.userId` → usuń z modelu (lub zostaw jako opcjonalny dev-fallback za flagą). `FallbackConfig` w `App.kt` zaktualizuj.

### 4.3 Storage
- Po włączeniu RLS na storage: w `0007`-następcy zamień policy `anon` na `authenticated` z właścicielem `auth.uid()` w ścieżce/owner (bucket `receipts`).

## 5. Warstwa domain — prawdziwy `AuthService`

- Nowy `data/auth/SupabaseAuthService.kt : AuthService`:
  - `session: StateFlow<AuthSession?>` mapowany z `supabase.auth.sessionStatus` (`SessionStatus.Authenticated` → `AuthSession(userId, provider)`; inne → null).
  - `signIn(provider)`:
    - flow A: `composeAuth.rememberSignInWith…` (UI-side) **albo** serwisowo `supabase.auth.signInWith(IDToken) { idToken = …; provider = Google/Apple }` po pobraniu tokenu natywnie.
    - flow B: `supabase.auth.signInWith(Google/Apple) { /* redirectUrl deep link */ }` → otwiera browser; wynik wraca przez `handleDeeplinks`.
    - Mapuj wyjątki na `Outcome.Failure(DomainError…)` (rozszerz `toDomainError` o anulowanie logowania = nowy `DomainError.AuthCancelled` lub `Unauthorized`).
  - `signOut()`: `supabase.auth.signOut()`.
- Podmień bind w `DataModule.kt`: `single<AuthService> { SupabaseAuthService(...) }` (usuń `StubAuthService` lub zostaw za flagą dev).
- `OnboardingLoginViewModel` zostaje bez zmian (zna tylko `AuthService`). **Uwaga:** przy flow A z UI-side `compose-auth` może być potrzebny launcher w `OnboardingLoginScreen` (composable) — wtedy VM dostaje już gotowy idToken/wynik.

## 6. Provisioning realnego usera (zamiast seeda)

Realny nowy user nie ma `user_settings` ani konta. Dwie opcje:
- **(A — rekomendowane) Trigger w Supabase:** `create function handle_new_user()` + `trigger on auth.users after insert` → wstawia `user_settings(user_id, 'PLN', onboarding_completed=false)` + domyślne `accounts` (1 konto). Nowa migracja `00NN_handle_new_user.sql`. Wtedy onboarding (kategorie) działa od razu.
- **(B) Klient po pierwszym logowaniu:** `OnboardingService.ensureProvisioned()` woła repo, które idempotentnie tworzy `user_settings` + konto, jeśli brak. Mniej „magii", ale więcej kodu klienta.
- Kategorie startowe nadal robi `CategoryRepository.provisionInitial` w kroku „Wybór kategorii" (bez zmian).

## 7. Nawigacja / gating (`App.kt`) — świadomy sesji

Zastąp obecny gating (tylko flaga) trójstanowym:
- `supabase.auth.sessionStatus`: **Loading** → spinner; **NotAuthenticated** → `Route.OnboardingWelcome`; **Authenticated** → sprawdź `OnboardingService.isCompleted()`:
  - `true` → `Route.Dashboard`,
  - `false` → kontynuuj onboarding (Welcome/Categories — do decyzji, czy wracać do wyboru kategorii czy od Welcome).
- Usuń `TODO(auth track)` i `if (false)`-owy/override-owy kod (override był tylko do demo — w PR #17 już cofnięty).
- **Returning user** („Zaloguj się") działa naturalnie: po `signIn` `sessionStatus` → Authenticated → onboarding done → Dashboard.
- **Wylogowanie** (gdzieś w ustawieniach/profilu — nowy ekran lub przycisk): `authService.signOut()` → `sessionStatus` → NotAuthenticated → Welcome.

## 8. Platform-specific (deep link / natywne)

- **Android** (`androidApp`):
  - `AndroidManifest.xml`: `intent-filter` na `MainActivity` z `BROWSABLE` + scheme/host redirectu.
  - `MainActivity`: `supabase.handleDeeplinks(intent)` w `onCreate`/`onNewIntent`.
  - Google natywnie: Credential Manager + `serverClientId` (Web OAuth client).
  - SHA-1 (debug/release) w Google Cloud + Play Console.
- **iOS** (`iosApp`):
  - `Info.plist`: `CFBundleURLTypes` z URL scheme.
  - `onOpenURL` / `SceneDelegate` → `supabase.handleDeeplinks(url)`.
  - **Sign in with Apple** capability + entitlement (`scripts/ios/deploy-ios.sh` używa automatic signing; trzeba dodać capability do App ID `com.pennypile.client`, team `ABJYNW6BYQ`).
  - Google iOS: reversed client ID URL scheme w Info.plist.

## 9. Migracje Supabase (nowe, forward-only)

- `00NN_enable_rls.sql`: `alter table <t> enable row level security;` dla: `user_settings, accounts, categories, budgets, recurring_expenses, transactions, receipts, product_categories, receipt_items, receipt_category_splits`. Policy już istnieją (0001) — tylko włączyć. Zweryfikuj, że **każdy** `insert` z klienta ustawia `user_id` (inaczej `with check (auth.uid() = user_id)` odrzuci).
- `00NN_storage_auth.sql`: zamień policy `receipts_anon_*` na `authenticated` z właścicielem.
- `00NN_handle_new_user.sql`: trigger provisioningu (§6A).
- **Kolejność wdrożenia:** RLS włączamy **na końcu**, po tym jak klient ustawia `user_id` z `auth.uid()` i sesja działa — inaczej cała apka zwróci puste/`Unauthorized`.

## 10. Etapy (proponowana kolejność PR-ów/commitów)

1. **Deps + klient:** dodaj `auth-kt` (+ `multiplatform-settings`), `install(Auth)`, `SupabaseAuthService` (signOut + session), bind w Koin. (RLS jeszcze OFF — działa na starym userId jako fallback.)
2. **Provider config + login:** konfiguracja Google/Apple (dashboard + cloud), podpięcie `signIn(provider)` (flow A/B), deep link na obu platformach. Test: realne zalogowanie → `sessionStatus` Authenticated.
3. **userId z sesji:** `CurrentUserProvider`, zamiana `config.userId` w repo, gating świadomy sesji w `App.kt`, wylogowanie.
4. **Provisioning:** trigger `handle_new_user` (lub klient), test świeżego usera E2E (onboarding → kategorie → wpis).
5. **RLS ON:** włącz RLS + storage auth policy. Pełny regres E2E (każdy ekran) na realnym userze, na Androidzie i iOS.

## 11. Pliki — dokładna lista (create = C, modify = M)

| Plik | C/M | Cel |
|---|---|---|
| `gradle/libs.versions.toml` | M | wersje: auth-kt, (compose-auth), multiplatform-settings, credentials |
| `shared/build.gradle.kts` | M | zależności auth + settings |
| `shared/.../data/supabase/SupabaseClientProvider.kt` | M | `install(Auth)` + sessionManager + `auth` accessor |
| `shared/.../data/auth/SupabaseAuthService.kt` | C | prawdziwy `AuthService` (session/signIn/signOut) |
| `shared/.../data/auth/CurrentUserProvider.kt` | C | `currentUserId()/requireUserId()` z sesji |
| `shared/.../data/auth/StubAuthService.kt` | M/usuń | zostaw za flagą dev albo usuń |
| `shared/.../data/di/DataModule.kt` | M | bind real `AuthService` + `CurrentUserProvider`; przekaż do repo |
| `shared/.../data/repository/Supabase*Repository.kt` (×8) | M | `config.userId` → `currentUserProvider.requireUserId()` |
| `shared/.../core/config/AppConfig.kt` | M | usuń/zdeprecjonuj `userId` |
| `shared/.../App.kt` | M | gating na `sessionStatus`; usuń `TODO`; deep link hook (iOS strona host) |
| `shared/.../core/error/DomainError.kt` + `SupabaseErrorMapper.kt` | M | obsługa anulowania logowania / błędów auth |
| `shared/.../feature/onboarding/OnboardingLoginScreen.kt` | M (flow A) | launcher natywny (compose-auth) jeśli wybrany flow A |
| `shared/.../feature/settings/…` (ewent.) | C | ekran/akcja „Wyloguj" |
| `androidApp/src/main/AndroidManifest.xml` | M | intent-filter redirect; (credentials meta) |
| `androidApp/.../MainActivity.kt` | M | `handleDeeplinks(intent)` |
| `iosApp/.../Info.plist` | M | URL scheme(y) |
| `iosApp/.../iOSApp.swift` / SceneDelegate | M | `onOpenURL` → handleDeeplinks; capability Apple |
| `supabase/migrations/00NN_handle_new_user.sql` | C | trigger provisioningu |
| `supabase/migrations/00NN_enable_rls.sql` | C | RLS ON |
| `supabase/migrations/00NN_storage_auth.sql` | C | storage policy authenticated |

## 12. Pułapki / ryzyka

- **Worktree:** pliki gitignorowane (`app_config.json`, `local.properties`) nie przechodzą do nowego worktree — skopiuj (patrz `CLAUDE.md`). Nowe sekrety (Google serverClientId itp.) trzymaj poza repo / w gitignorowanym configu.
- **RLS = wszystko albo nic:** włącz dopiero, gdy każdy insert ustawia `user_id` i sesja działa; inaczej apka zwróci puste listy / `Unauthorized` (DomainError już to mapuje na ekran błędu).
- **Apple na Androidzie** = tylko web OAuth (brak natywnego). Zaplanuj fallback.
- **Apple wymaga „Sign in with Apple"** jeśli oferujesz inne social logins (wytyczna App Store) — Apple musi być obecne na iOS.
- **Sesja/refresh:** GoTrue sam odświeża token; upewnij się, że `sessionManager` persystuje (test: kill + restart apki = nadal zalogowany).
- **Gating loading state:** na starcie `sessionStatus` jest `Loading…/Initializing` — pokaż spinner, nie „przeskakuj" na Welcome.
- **Dane `…0001/…0002`** stają się nieistotne (osierocone). Stary seed (`0002`, `0012`) i `AppConfig.userId` do usunięcia/zignorowania.
- **Build pod VPN:** Gradle nie pobierze nowych zależności na firmowym VPN (TLS interception) — buduj poza VPN albo dodaj firmowy CA.
- **Deep link testing:** na emulatorze testuj `adb shell am start -a android.intent.action.VIEW -d "com.sd.kupka_pieniedzy_client://login-callback?..."`.

## 13. Definicja ukończenia (akceptacja)

- [ ] Świeży, realny user: Google **i** Apple logują (Android + iOS), sesja przeżywa restart.
- [ ] Po pierwszym logowaniu user ma `user_settings` + konto (trigger), onboarding seeduje kategorie.
- [ ] Returning user („Zaloguj się") wchodzi prosto na Dashboard.
- [ ] Wylogowanie wraca na Welcome.
- [ ] **RLS ON** — user widzi tylko swoje dane; każdy ekran (Dashboard/Wpisy/Kategorie/Trendy/Paragon) działa.
- [ ] `AppConfig.userId` usunięty; `StubAuthService` usunięty lub za flagą dev.
- [ ] E2E przeklikane na Androidzie i iOS.

---
_Powiązane: PR #17 (onboarding + atrapa auth, branch `feat/onboarding-flow`), `App.kt` `TODO(auth track)`, `domain/auth/AuthService.kt`._
