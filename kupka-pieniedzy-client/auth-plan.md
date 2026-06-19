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

## 1. Decyzje — PODJĘTE (2026-06-19)

> Domknięte z Sebastianem. Poniżej obowiązujący zakres tracku; reszta dokumentu (§2–§13)
> zaktualizowana pod te decyzje. **Google jest odłożony** — nie wchodzi na start.

1. **Metody logowania (zakres startowy):**
   - **Email = magic-link / OTP** (bez hasła) — `supabase.auth.signInWith(OTP) { email }` → kod/link → weryfikacja. Brak rejestracji z hasłem, brak resetu hasła. Dostępne na **obu** platformach.
   - **Apple = natywnie (flow A)** — tylko na **iOS** (`signInWith(IDToken)` z natywnym Apple ID tokenem). Spełnia wytyczną App Store „Sign in with Apple".
   - **Android:** Apple **ukryte** (brak natywnego Apple na Androidzie; web-fallback świadomie pomijamy) → na Androidzie zostaje **wyłącznie email OTP**.
   - **Google:** **odłożony poza ten track.** Żadnej konfiguracji Google Cloud / Credential Manager / SHA-1 na start. Dokładamy później (flow A, native) jako osobny etap, gdy będzie potrzebny.
   - Konsekwencja: ekran logowania renderuje przyciski warunkowo per platforma (iOS: „Zaloguj przez Apple" + „Kod na email"; Android: „Kod na email").
2. **Strategia danych:** realny user startuje **świeżo** (onboarding go provisionuje) — **bez migracji** danych `…0001/…0002` (to seed/demo, stają się osierocone). Brak skryptu migracyjnego.
3. **Persistencja sesji:** `multiplatform-settings` (russhwolf) jako backend domyślnego `SettingsSessionManager`. Tokeny trzymane **plaintext** (SharedPreferences / NSUserDefaults) — akceptowalne dla MVP jednoosobowego; szyfrowanie (Keychain / EncryptedSharedPreferences) można dołożyć później bez zmiany kontraktu `AuthService`.
4. **Konfigurację dev robi Sebastian** (jedyny z dostępem): Supabase dashboard (Email provider + Apple provider) oraz Apple Developer (capability „Sign in with Apple"). **Bez Google Cloud na start.**

### Co to znaczy dla planu (skrót zmian względem wersji pierwotnej)
- **§2** kurczy się do: Email provider (Supabase, włączony domyślnie) + Apple (Service ID/`.p8` lub natywny App ID) + deep link. **Bez Google Cloud.**
- **§3** deps: `auth-kt` + `multiplatform-settings` + (flow A iOS Apple) `compose-auth`. **Bez** `androidx.credentials` / `googleid` (to było pod Google).
- **§5** `signIn(provider)`: obsługa `AuthProvider.Apple` (iOS, IDToken) + nowa ścieżka **email OTP** (poza obecnym enumem — patrz niżej).
- **Kontrakt `AuthService`:** obecny `signIn(provider: AuthProvider)` nie obejmuje OTP (OTP nie jest „providerem" social). Rozszerzyć o `suspend fun signInWithEmailOtp(email): Outcome<Unit>` + `suspend fun verifyEmailOtp(email, token): Outcome<AuthSession>` (lub magic-link przez deep link). `enum AuthProvider` zostaje `{ Apple }` na start (Google dołożymy później); usuń `Google` albo zostaw zakomentowane jako TODO.
- **§8** Android: brak przycisku Apple, brak Google native; deep link i tak potrzebny (magic-link wraca deep linkiem, jeśli użyjemy linku zamiast kodu OTP).

## 2. Konfiguracja zewnętrzna (poza repo — wymaga Sebastiana)

> Zakres startowy: **Email (OTP/magic-link) + Apple**. Google odłożony — pomiń całą konfigurację Google Cloud.

- **Supabase dashboard → Authentication → Providers:**
  - **Email** — włączony domyślnie; ustaw tryb (OTP code / magic link), szablon maila (PL), `OTP expiry`. Brak hasła → możesz wyłączyć „Confirm password" / signup z hasłem.
  - włącz **Apple** (Service ID, Team ID, Key ID, `.p8` key).
  - **Redirect URLs / deep link:** dodaj schemat aplikacji, np. `com.sd.kupka_pieniedzy_client://login-callback` (Site URL + Additional Redirect URLs). Potrzebny dla magic-linka i dla Apple. (Jeśli email = **kod OTP** zamiast linka, deep link dla emaila nie jest wymagany — ale Apple iOS i tak go potrzebuje.)
- ~~**Google Cloud Console**~~ — **POMINIĘTE na start** (Google odłożony). Gdy wróci: Web + Android (SHA-1 debug/release) + iOS OAuth client IDs.
- **Apple Developer:**
  - **App ID** (`com.pennypile.client`, team `ABJYNW6BYQ`) z capability „Sign in with Apple",
  - przy natywnym flow A na iOS Service ID nie jest konieczne dla samego iOS; **Sign in with Apple Key (`.p8`)** i tak wgraj do Supabase (Supabase weryfikuje token po stronie serwera).
- **Deep link:** NIE wymagany dla wybranych przepływów (OTP kod + natywne Apple). Potrzebny dopiero,
  gdyby włączyć magic-link albo Google web (wtedy: Supabase redirect + AndroidManifest intent-filter
  + iOS URL types). Patrz §8.

## 3. Zależności (`gradle/libs.versions.toml` + `shared/build.gradle.kts`)

- `libs.versions.toml` → dodać biblioteki (wersje z `supabase-bom`):
  - `supabase-auth = { module = "io.github.jan-tennert.supabase:auth-kt" }`
  - (jeśli flow A) `supabase-compose-auth = { module = "io.github.jan-tennert.supabase:compose-auth" }`
  - `multiplatform-settings = { module = "com.russhwolf:multiplatform-settings", version = "1.2.0" }` (+ `multiplatform-settings-no-arg` jeśli użyjesz domyślnego konstruktora — na Androidzie wymaga inicjalizacji Contextem).
- `shared/build.gradle.kts` → `commonMain.dependencies { implementation(libs.supabase.auth); implementation(libs.multiplatform.settings); implementation(libs.supabase.compose.auth) /* Apple iOS native */ }`. Ktor engines (okhttp/darwin) już są.
- **Android:** ~~Credential Manager / `androidx.credentials` / `googleid`~~ — **niepotrzebne na start** (to było pod Google native). Android ma tylko email OTP, bez natywnego social SDK. Dołożymy przy Google.

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

## 8. Platform-specific (natywne)

> **Deep linki NIE są potrzebne** dla wybranych przepływów: e-mail = **kod OTP** (wpisywany ręcznie
> przez `verifyEmailOtp`, bez powrotu linkiem), Apple = **natywne** (AuthenticationServices, bez
> redirectu webowego). `scheme`/`host` w `install(Auth)` zostają jako forward-compat (gdyby kiedyś
> doszedł magic-link lub Google web), ale intent-filter / URL types nie są wymagane teraz.

- **Android** (`androidApp`): **bez zmian** — brak Apple (przycisk ukryty), e-mail OTP nie używa
  deep linku. (Gdy wejdzie Google: Credential Manager + `serverClientId` + SHA-1.)
- **iOS** (`iosApp`):
  - **Sign in with Apple** entitlement — dodane: `iosApp/iosApp/iosApp.entitlements`
    (`com.apple.developer.applesignin`) + `CODE_SIGN_ENTITLEMENTS` w `Config.xcconfig`.
  - Wymaga capability „Sign in with Apple" na App ID `com.pennypile.client` (team `ABJYNW6BYQ`) —
    `deploy-ios.sh` (automatic signing + `-allowProvisioningUpdates`) potrafi to doprovisionować.

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

- [ ] Świeży, realny user loguje się **kodem e-mail (OTP)** na **iOS i Androidzie**; sesja przeżywa restart.
- [ ] **iOS:** natywne „Zaloguj przez Apple" działa; **Android:** przycisk Apple ukryty (tylko e-mail).
- [ ] Po pierwszym logowaniu user ma `user_settings` + konto (trigger), onboarding seeduje kategorie.
- [ ] Returning user („Zaloguj się") wchodzi prosto na Dashboard.
- [ ] Wylogowanie wraca na Welcome.
- [ ] **RLS ON** — user widzi tylko swoje dane; każdy ekran (Dashboard/Wpisy/Kategorie/Trendy/Paragon) działa.
- [ ] `AppConfig.userId` usunięty; `StubAuthService` usunięty lub za flagą dev.
- [ ] E2E przeklikane na Androidzie i iOS.
- [ ] _(odłożone, poza tym trackiem)_ Google login.

## 14. Status implementacji (2026-06-19, branch `feat/auth-supabase`)

### ✅ Zrobione w kodzie (kompiluje się: commonMain metadata + iosSimulatorArm64 + androidDebug)
- Deps: `auth-kt` + `compose-auth` (`libs.versions.toml`, `shared/build.gradle.kts`).
- `SupabaseClientProvider`: `install(Auth)` (PKCE, scheme/host forward-compat) + `install(ComposeAuth){ appleNativeLogin() }` + accessory `auth`/`composeAuth`.
- `AuthService` (kontrakt): `status: StateFlow<AuthStatus>` (Loading/Authenticated/Unauthenticated) + `sendEmailOtp` + `verifyEmailOtp` + `signOut`. `AuthProvider = { Apple }`.
- `SupabaseAuthService` (OTP + sesja z `sessionStatus`), `StubAuthService` (dev, niebindowany), bind w `DataModule`.
- `CurrentUserProvider` (auth.uid() + dev-fallback `AppConfig.userId`); 8 repo używa `currentUser.requireUserId()`.
- Gating w `App.kt`: bramka `AuthStatus` → `AppShell`; spinner na Loading; usunięty `TODO(auth track)`.
- Ekran logowania: krok e-mail → krok kod (OTP); Apple przez expect/actual (`AppleSignInButton`, iOS = compose-auth, Android = no-op). Stringi PL.
- `DomainError.AuthCancelled`.
- iOS: `iosApp.entitlements` (Sign in with Apple) + `CODE_SIGN_ENTITLEMENTS`.
- Migracje: `0013_handle_new_user.sql`, `0014_storage_auth.sql`, `0015_enable_rls.sql`.

### 🔧 Do zrobienia ręcznie przez Sebastiana (poza repo) — ZANIM zadziała logowanie
1. **Supabase dashboard → Authentication → Providers → Email:** włączony; ustaw szablon maila PL z kodem (`{{ .Token }}`), `OTP expiry`. (Magic-link niepotrzebny — używamy kodu.)
2. **Supabase → Providers → Apple:** włącz; wgraj Apple **Sign in with Apple Key (.p8)** + Service ID / Team ID / Key ID.
3. **Apple Developer:** capability „Sign in with Apple" na App ID `com.pennypile.client` (team `ABJYNW6BYQ`). `deploy-ios.sh` (`-allowProvisioningUpdates`) powinien doprovisionować po dodaniu entitlementu.
4. **`app_config.json`:** zostaje (supabaseUrl/anonKey). `userId` na razie zostaje jako dev-fallback.

### 🚦 Kolejność wdrożenia migracji (NA KOŃCU RLS)
- Najpierw `0013` (trigger) + `0014` (storage) — bezpieczne.
- `0015_enable_rls.sql` **dopiero** gdy: (a) realny login działa E2E, (b) usunięty dev-fallback w `CurrentUserProvider` (inaczej insert z `AppConfig.userId` poleci „violates RLS"). Do tego momentu apka działa na starym userId.

### ⏳ Niezweryfikowane (wymaga realnego configu / urządzenia)
- Realny round-trip OTP (wysyłka maila + verify), natywny flow Apple na iOS, persystencja sesji po restarcie, trigger provisioningu, RLS E2E. Kod kompiluje się na 3 targetach, ale logiki end-to-end nie dało się odpalić bez kroków 1–3 powyżej.

---
_Powiązane: PR #17 (onboarding + atrapa auth, branch `feat/onboarding-flow`), `domain/auth/AuthService.kt`, migracje `0013–0015`._
