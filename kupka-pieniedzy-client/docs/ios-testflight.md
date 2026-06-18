# iOS → TestFlight (com.pennypile.client)

Deploy iOS na TestFlight. Mechanizm odwzorowany z referencji `shapyfy-mobile`, ale
**bez CocoaPods** (kupka integruje framework przez `embedAndSignAppleFrameworkForXcode`,
więc archiwizujemy bezpośrednio `.xcodeproj`, bez workspace i `pod install`).

## Konfiguracja (już w repo)

| Co | Wartość | Gdzie |
|----|---------|-------|
| Bundle ID | `com.pennypile.client` | `iosApp/Configuration/Config.xcconfig` |
| Team ID | `ABJYNW6BYQ` (Sebastian Druciak) | `Config.xcconfig` + skrypt |
| Nazwa na ekranie | `Kupka Pieniędzy` | `CFBundleDisplayName` w `Info.plist` |
| Nazwa wewn. (.app) | `kupka-pieniedzy-client` | `PRODUCT_NAME` |
| Signing | **Automatic** + `-allowProvisioningUpdates`, cert `Apple Distribution` | `deploy-ios.sh` |
| Klucz API ASC | Key `2AK6L4U6S7`, Issuer `34832680-…` | `scripts/ios/App Store Connect Auth Key.p8` |

Klucz `.p8` jest **kontowy** (ten sam co Shapyfy) i jest w `.gitignore` — nie trafia do repo.
Certyfikat `Apple Distribution: Sebastian Druciak (ABJYNW6BYQ)` jest już w keychain.

**App ID i profil dystrybucyjny powstają automatycznie** podczas pierwszej archiwizacji —
`xcodebuild -allowProvisioningUpdates` tworzy je kluczem API, reużywając wspólnego certu.
Nie trzeba nic klikać w Developer Portal.

## Krok jednorazowy po stronie Apple (wymaga Twojego loginu)

Zostaje **tylko jeden** ręczny krok — rekord aplikacji w App Store Connect. Tego nie zrobi
żaden automat z poziomu Xcode (to tożsamość appki w sklepie):

[appstoreconnect.apple.com/apps](https://appstoreconnect.apple.com/apps) → **+** → New App →
- Platforma: iOS
- Nazwa: `Kupka Pieniędzy` (musi być unikalna w całym App Store — jak zajęta, np. `Kupka Pieniędzy — budżet`)
- Język główny: Polish
- Bundle ID: `com.pennypile.client`
- SKU: dowolny unikalny, np. `kupka-pieniedzy-client`
- Create

## Deploy

Gdy rekord aplikacji w ASC istnieje:

```bash
cd kupka-pieniedzy-client
./scripts/ios/deploy-ios.sh
```

Przy pierwszym uruchomieniu xcodebuild poprosi Apple o utworzenie App ID + profilu
(kluczem API) — to normalne, dzieje się raz.

Skrypt: archiwizuje (Release, Gradle buduje framework w build phase) → eksportuje IPA z
manual signing → wysyła na TestFlight kluczem API. Po sukcesie build pojawia się w
App Store Connect → TestFlight (przetwarzanie kilka–kilkanaście minut).

## Bump wersji

Jedyne źródło prawdy: **`version.properties`** (root projektu) — wspólne dla Android i iOS.

- `VERSION_CODE` (build) — podnieś przy **każdym** uploadzie na TestFlight (musi rosnąć).
- `VERSION_NAME` (wersja) — podnieś przy realnym wydaniu (np. `1.0` → `1.1`).

Android czyta to automatycznie (Gradle). iOS — `deploy-ios.sh` sed-synchronizuje wartości
do `Config.xcconfig` przed archiwizacją (dlatego `Config.xcconfig` może po deployu pokazać
zmianę w gicie — to oczekiwane, wartości są pochodne).

Przykład następnego deployu: zmień w `version.properties` `VERSION_CODE=2`, potem `./scripts/ios/deploy-ios.sh`.

## Migracja na manual signing (przy dodaniu CI)

Automatic jest wygodny lokalnie, ale w CI/CD chcemy reprodukowalności (brak nagłej
regeneracji profilu). Wtedy: utworzyć w portalu profil App Store dla `com.pennypile.client`
(cert `Apple Distribution`, nazwa np. `Kupka Pieniedzy App Store`), zacommitować go zaszyfrowany,
i w `deploy-ios.sh` przywrócić overrides `CODE_SIGN_STYLE=Manual` + `PROVISIONING_PROFILE_SPECIFIER`
oraz `signingStyle=manual` w ExportOptions. Wzór: `shapyfy-mobile/scripts/ios/deploy-ios-dev.sh`.

## Częste problemy

- **„No suitable application records were found"** przy uploadzie → rekord w App Store Connect nie istnieje (zrób krok powyżej).
- **Błąd tworzenia profilu / „account doesn't have permission"** → klucz API musi mieć rolę Admin/App Manager z dostępem do certyfikatów (ten sam klucz tworzy profile dla Shapyfy, więc powinno działać).
- **Build na firmowym VPN** → Gradle nie pobierze zależności (patrz `SETUP.md`). Buduj poza VPN.
