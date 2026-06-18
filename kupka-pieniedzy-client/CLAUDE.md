# CLAUDE.md — kupka-pieniedzy-client

Kotlin Multiplatform (iOS + Android) + Compose Multiplatform + Supabase. Cała logika i UI w module
`shared`. Aplikacja do śledzenia wydatków i budżetów. Język: **polski** (dokumenty, teksty UI,
komentarze). Kod po angielsku.

> Repo nadrzędne (`../`) trzyma dokumentację produktową, design i `Fundusze.xlsx` — patrz `../CLAUDE.md`.
> Ten plik dotyczy **kodu klienta** (`kupka-pieniedzy-client/`).

## Układ repo

```
kupka-pieniedzy-client/
├── shared/        # ✦ cały kod (commonMain) — KMP + Compose
├── androidApp/    # cienki host Androida (MainActivity)
├── iosApp/        # host iOS (Xcode)
├── supabase/      # migracje SQL (0001_init, 0002_seed) — 9 tabel + widoki
├── scripts/       # android/run-emulator.sh, ios/deploy-ios.sh
└── SETUP.md       # build, VPN, Supabase, migracje — przeczytaj przy pierwszym buildzie
```

Pakiet bazowy: `com.sd.kupka_pieniedzy_client`. applicationId/namespace: identyczny.

## Architektura (warstwy)

Przepływ jednokierunkowy:
**Screen → ViewModel → Service → Repository (interface) → Impl (Supabase)**

Błędy nie lecą wyjątkami przez warstwy — wszędzie `Outcome<T>` (`Success`/`Failure(DomainError)`).
UI używa `ScreenState<T>` (`Loading`/`Content`/`Error`) renderowanego przez `StateContainer`.

Gdzie co jest (`shared/src/commonMain/kotlin/com/sd/kupka_pieniedzy_client/`):

| Ścieżka | Zawartość |
|---|---|
| `feature/<nazwa>/` | `<Nazwa>Screen.kt` + `<Nazwa>ViewModel.kt` (+ sheety). Np. `dashboard`, `entries`, `categories`, `receipt`, `addexpense` |
| `domain/model/` | Modele domenowe + read-modele per widok (`DashboardSnapshot`, `EntriesSnapshot`, `RecentEntry`, `EntryListItem`…). Wspólny kontrakt wiersza: `EntryRowData` |
| `domain/service/` | Logika: `DashboardService`, `EntriesService`, `CategoryService`, `ReceiptService`, `ExpenseService` (interface + `Default…` impl) |
| `domain/repository/` | `Repositories.kt` — **wszystkie** interfejsy repozytoriów w jednym pliku |
| `data/repository/` | Implementacje `Supabase…Repository` (+ `MockReceiptAnalysisRepository`) |
| `data/dto/`, `data/mapper/` | DTO `@Serializable` (snake_case) + mappery DTO↔domena |
| `data/di/DataModule.kt`, `di/Modules.kt` | Koin: repo (data) / service (domain) / viewModel (presentation). **Nowy feature = wpis tutaj** |
| `designsystem/component/` | Atomy/komponenty: `AppText`, `EntryRow`/`EntryAmount`/`ExpandableEntryRow`, `KupkaCard`, `KupkaButton`, `CategoryBadge`, `StateContainer`, `Banners`… |
| `designsystem/theme/` | Tokeny: `KupkaColors`, `KupkaSpacing`, `KupkaShapes`, `KupkaTypography` + akcesor `KupkaTheme.{colors,spacing,shapes,typography}` |
| `designsystem/icon/` | `AppIcons` (stałe ligatur Material Symbols) + `MaterialSymbol` (render) |
| `navigation/` | `Route.kt` (sealed), `AppNavHost.kt` (Crossfade po `Route`), `AppBottomBar.kt`, `Navigator.kt` (stos, bez biblioteki) |
| `localization/` | `Strings` (interface) + `PlStrings` (impl PL) + `Plurals` (`plChoosePlural`). Dostęp przez `LocalStrings.current` |
| `core/` | `money/` (`Money` w **groszach** + `MoneyFormatter`), `time/` (`DateProvider`, `monthRange`, `LocalToday`), `result/Outcome`, `presentation/ScreenState`, `config/AppConfig` |

## Konwencje (twarde — 0 hardcode w UI)

- **Kolory** wyłącznie z `KupkaTheme.colors` (nigdy surowe hexy). Status budżetu → `colors.budgetFill/Track(status)`.
- **Typografia** wyłącznie przez `AppText(variant = TextVariant.…)` (żadnego surowego `Text` ani `KupkaTheme.typography.*` w ekranach).
- **Teksty** wyłącznie z `LocalStrings.current` (PL). Liczebniki → `Strings.fun(...)` z `plChoosePlural`.
- **Ikony** UI z `AppIcons` (ligatury Material Symbols); ikony kategorii pochodzą z danych (`category.icon`).
- **Spacing** z `KupkaTheme.spacing` (`screenH` = margines ekranu); drobne paddingi w komponentach mogą być surowe `.dp` (jak w istniejących `Rows.kt`).
- **Kwoty** zawsze `Money` w jednostkach podrzędnych (grosze); formatowanie przez `MoneyFormatter`.
- **Reuse wiersza listy:** model implementuje `EntryRowData`, render przez wspólny `EntryRow`/`ExpandableEntryRow` (chevron rozwijania tylko gdy `expandable == true`). Nie duplikuj logiki kwoty — jest w `EntryAmount`.

## Konfiguracja i sekrety (gitignorowane!)

- `shared/src/commonMain/composeResources/files/app_config.json` — ładowany przez `AppConfigLoader`
  (`supabaseUrl`, `supabaseAnonKey`, `userId` hardcoded w MVP). **Gitignorowany.** W repo jest tylko
  `app_config.example.json`. Brak pliku → apka pokazuje „Aplikacja nie jest skonfigurowana (Supabase)”.
- `local.properties` — `sdk.dir=…` do Androida. **Gitignorowany** (skrypt run-emulator dotworzy go sam).

## Worktree — ważny haczyk

Pliki gitignorowane **nie przechodzą** do świeżego `git worktree`. Po `git worktree add` skopiuj z
głównego checkoutu:
```
cp <main>/shared/src/commonMain/composeResources/files/app_config.json <wt>/.../files/
# local.properties dotworzy run-emulator.sh, albo: echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
```
Bez configu apka się uruchomi, ale każdy ekran pokaże błąd „nie skonfigurowana”.

## Build / Run

- Pierwszy build, VPN, Supabase, migracje → **`SETUP.md`** (na firmowym VPN Gradle nie pobierze
  zależności — buduj poza VPN albo dodaj firmowy CA do `cacerts`).
- Szybki sanity-check wspólnego kodu (najtańszy gate na błędy commonMain + Compose):
  ```
  ./gradlew :shared:compileCommonMainKotlinMetadata
  ```
- Android debug + instalacja: `./gradlew :androidApp:installDebug`
- **Emulator end-to-end:** `./scripts/android/run-emulator.sh` (lub komenda `/run-emulator`) —
  wybiera/bootuje AVD, buduje, instaluje, odpala.
- iOS / TestFlight: `scripts/ios/deploy-ios.sh` (patrz `../CLAUDE.md`).

## Design (Faza 2) — `/refresh-design`

Designy żyją w projekcie **Claude Design** (board HTML), nie w repo. Komenda `/refresh-design`
importuje board przez konektor DesignSync, pobiera kontekst, czeka na obszar i zakłada branch +
worktree. Implementacja = przepisanie boardu na realny KMP + Compose + Supabase zgodnie z warstwami
i konwencjami powyżej.

## Niuanse danych (z których łatwo się potknąć)

- Widok `recent_entries` jest **zdenormalizowany o kategorię** (ikona/kolor/nazwa), ale **nie zwraca
  `category_id`** — filtrowanie/dopasowanie budżetu robimy po **nazwie** kategorii.
- `transactions` trzyma tylko **datę** (bez godziny) — nie pokazujemy czasu wpisu.
- Transfery (`type='transfer'`) są wykluczone z list/raportów; zwroty to `type='refund'` (UI: zielony „+”).
- Paragony: status `pending` (w analizie) → `ready` → `saved`. Pozycje w `receipt_items`
  (`getAnalyzed(receiptId)`); powiązanie z transakcją przez `receipts.transaction_id`.
