# Setup — kupka-pieniedzy-client

KMP (iOS + Android, Compose Multiplatform) + Supabase. Cała logika i UI w module `shared`.

## 0. Wymagania
- JDK 17+ (jest OpenJDK 23).
- Android SDK (ścieżka w `local.properties`).
- iOS: Xcode (tylko do uruchomienia na iOS).

## 1. Pierwszy build — uwaga na VPN
Na **firmowym VPN** Gradle/JVM nie pobiera nowych zależności (koin / supabase / ktor / plugin serialization), mimo że `curl` działa. Przyczyna: VPN podstawia własny root CA, którego JVM (`cacerts`) nie zna → handshake TLS pada. Objaw: `Plugin ... was not found` / `could not resolve plugin artifact` w 1–3 s.

Obejście (jedno z):
1. **Zbuduj poza VPN** (najprościej — pobierze zależności do cache, potem działa offline).
2. Zaimportuj firmowy root CA do truststore JVM:
   ```
   keytool -importcert -alias firma -file firma-ca.pem \
     -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit
   ```
3. Skonfiguruj proxy w `~/.gradle/gradle.properties`:
   ```
   systemProp.https.proxyHost=...
   systemProp.https.proxyPort=...
   ```

Sanity check kompilacji wspólnego kodu:
```
./gradlew :shared:compileCommonMainKotlinMetadata
```
Android debug:
```
./gradlew :androidApp:assembleDebug
```
iOS: otwórz `iosApp/` w Xcode i uruchom.

## 2. Supabase
1. Załóż projekt w Supabase.
2. Uruchom migracje (SQL editor po kolei, lub `supabase db push`):
   - `supabase/migrations/0001_init.sql` — 9 tabel + widoki `budget_progress`, `recent_entries` (RLS napisane, wyłączone w MVP — hardcoded user).
   - `supabase/migrations/0002_seed.sql` — dane startowe jako migracja (jeden hardcoded user w MVP): konto domyślne + 10 kategorii L1 + sub-kategorie spożywki + startowe budżety + `user_settings` (user_id `00000000-0000-0000-0000-000000000001`). Idempotentna.
3. Skopiuj template i uzupełnij danymi z Supabase (Settings → API):
   ```
   cp shared/src/commonMain/composeResources/files/app_config.example.json \
      shared/src/commonMain/composeResources/files/app_config.json
   ```
   ```json
   { "supabaseUrl": "https://xxxx.supabase.co", "supabaseAnonKey": "<anon key>",
     "userId": "00000000-0000-0000-0000-000000000001", "defaultCurrency": "PLN", "language": "pl" }
   ```
   ⚠️ `app_config.json` jest w `.gitignore` — **nie commituj go**. Do repo trafia tylko `*.example.json`.
   Tylko **anon key** trafia do klienta (jest publiczny). **Nigdy nie wpisuj tu hasła do bazy** —
   klient to artefakt publiczny (APK/IPA da się rozpakować).

Bez skonfigurowanego Supabase apka się uruchomi, ale repozytoria zwrócą `DomainError.Configuration` (ekrany pokażą stan błędu z retry).

## 3. Architektura (gdzie co jest)
`shared/src/commonMain/kotlin/com/sd/kupka_pieniedzy_client/`
- `core/` — `result/Outcome`, `error/DomainError`, `money/`, `time/`, `config/`, `presentation/ScreenState`.
- `localization/` — `Strings` + `PlStrings` (PL), `LocalStrings`. Zero literałów w UI.
- `designsystem/` — `theme/` (tokeny kolorów/typografii/spacingu/kształtów), `icon/` (MaterialSymbol + palety), `component/` (atomy/molekuły: `AppText`+`TextVariant`, Button, Card, ProgressBar, CategoryBadge, BudgetRow, TransactionRow, ReceiptItemRow, Sheet, TopBar, BottomNav, banery, StateContainer).
- `domain/` — `model/`, `repository/` (interfejsy), `service/` (interfejs + Default impl).
- `data/` — `supabase/` (klient + mapowanie błędów), `dto/`, `mapper/`, `repository/` (Supabase* + `MockReceiptAnalysisRepository`), `di/DataModule`.
- `feature/` — `dashboard/`, `addexpense/`, `receipt/`, `categories/`, `placeholder/` (Screen + ViewModel + UiState).
- `navigation/` — `Route`, `Navigator`, `AppNavHost`, `AppBottomBar`.
- `di/Modules.kt` — `appModule(config)`, `domainModule`, `presentationModule`. `App.kt` — bootstrap (config → Koin → theme → nav).

Przepływ: `Screen → ViewModel → Service → Repository → Supabase*Repository`. Błędy łapane TYLKO w warstwie data (`runCatchingDomain`) i mapowane na `DomainError`; wyżej operujemy na `Outcome`.

## 4. Jedyny mock
`data/repository/MockReceiptAnalysisRepository` — analiza zdjęcia paragonu zwraca stały obiekt „Biedronka" po `delay(2500)`. To placeholder pod docelową Edge Function z Claude vision. Reszta flow (zapis paragonu, splits per-sub-suma, transakcje, budżety, kategorie) jest realna na Supabase.

## 5. Fonty
W `shared/src/commonMain/composeResources/font/`: Manrope (400/500/600/700), Commit Mono (400/500), Material Symbols Rounded (ikony renderowane przez ligatury w `MaterialSymbol`).
