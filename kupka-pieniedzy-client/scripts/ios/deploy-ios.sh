#!/bin/bash

# iOS Deployment Script — kupka-pieniedzy-client (PennyPile)
# Buduje i wysyła com.pennypile.client na TestFlight.
#
# Różnice względem referencji (shapyfy-mobile):
#   - BRAK CocoaPods — kupka integruje framework przez embedAndSignAppleFrameworkForXcode
#     (Xcode build phase odpala Gradle automatycznie podczas archive), więc nie ma
#     kroków `pod install` ani workspace. Archiwizujemy bezpośrednio -project.
#   - signing: AUTOMATIC + -allowProvisioningUpdates — xcodebuild sam tworzy App ID i
#     profil dla com.pennypile.client kluczem API (reużywa wspólny cert Apple Distribution).
#     (Przy dodaniu CI przełączymy na manual z konkretnym profilem — patrz docs.)
#
# Wymagania (jednorazowo, po stronie Apple — patrz docs/ios-testflight.md):
#   - Rekord aplikacji w App Store Connect (bundle com.pennypile.client). App ID i profil
#     powstają automatycznie podczas pierwszej archiwizacji.

set -euo pipefail

# --- Konfiguracja ---
SCHEME="iosApp"
CONFIGURATION="Release"
PROJECT="iosApp/iosApp.xcodeproj"
ARCHIVE_PATH="iosApp/build/iosApp.xcarchive"
EXPORT_PATH="iosApp/build"

# --- Signing ---
TEAM_ID="ABJYNW6BYQ"
BUNDLE_ID="com.pennypile.client"

# --- App Store Connect API (klucz kontowy — wspólny z Shapyfy) ---
API_KEY_ID="2AK6L4U6S7"
API_ISSUER_ID="34832680-e506-4482-ab8d-7f94703d884e"

# --- Kolory ---
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/../.."
cd "$PROJECT_ROOT"

API_KEY_PATH="$SCRIPT_DIR/App Store Connect Auth Key.p8"

echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}  iOS → TestFlight (com.pennypile.client)${NC}"
echo -e "${GREEN}======================================${NC}"
echo ""

if [ ! -f "$API_KEY_PATH" ]; then
    echo -e "${RED}Błąd: brak klucza API w: $API_KEY_PATH${NC}"
    exit 1
fi

# --- [0/3] Synchronizacja wersji z version.properties (jedyne źródło prawdy) ---
echo -e "${YELLOW}[0/3] Synchronizacja wersji...${NC}"
if [ ! -f "version.properties" ]; then
    echo -e "${RED}Błąd: brak version.properties${NC}"
    exit 1
fi
VERSION_NAME=$(grep "^VERSION_NAME=" version.properties | cut -d'=' -f2 | tr -d '\n\r ')
VERSION_CODE=$(grep "^VERSION_CODE=" version.properties | cut -d'=' -f2 | tr -d '\n\r ')
XCCONFIG="iosApp/Configuration/Config.xcconfig"
sed -i '' "s/^MARKETING_VERSION=.*/MARKETING_VERSION=$VERSION_NAME/" "$XCCONFIG"
sed -i '' "s/^CURRENT_PROJECT_VERSION=.*/CURRENT_PROJECT_VERSION=$VERSION_CODE/" "$XCCONFIG"
echo -e "  Wersja: ${GREEN}$VERSION_NAME${NC} (Build: ${GREEN}$VERSION_CODE${NC})"
echo ""

# --- [1/3] Archiwizacja (build phase odpali Gradle: :shared:embedAndSignAppleFrameworkForXcode) ---
echo -e "${YELLOW}[1/3] Archiwizacja...${NC}"
mkdir -p "$EXPORT_PATH"

xcodebuild clean archive \
    -project "$PROJECT" \
    -scheme "$SCHEME" \
    -configuration "$CONFIGURATION" \
    -archivePath "$ARCHIVE_PATH" \
    -destination "generic/platform=iOS" \
    -authenticationKeyPath "$API_KEY_PATH" \
    -authenticationKeyID "$API_KEY_ID" \
    -authenticationKeyIssuerID "$API_ISSUER_ID" \
    -allowProvisioningUpdates \
    DEVELOPMENT_TEAM="$TEAM_ID" \
    CODE_SIGN_STYLE=Automatic

# --- [2/3] Eksport IPA ---
echo -e "${YELLOW}[2/3] Eksport IPA...${NC}"

cat > "$EXPORT_PATH/ExportOptions.plist" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>method</key>
    <string>app-store-connect</string>
    <key>teamID</key>
    <string>$TEAM_ID</string>
    <key>signingStyle</key>
    <string>automatic</string>
    <key>uploadSymbols</key>
    <true/>
    <key>destination</key>
    <string>upload</string>
</dict>
</plist>
EOF

xcodebuild -exportArchive \
    -archivePath "$ARCHIVE_PATH" \
    -exportOptionsPlist "$EXPORT_PATH/ExportOptions.plist" \
    -exportPath "$EXPORT_PATH" \
    -authenticationKeyPath "$API_KEY_PATH" \
    -authenticationKeyID "$API_KEY_ID" \
    -authenticationKeyIssuerID "$API_ISSUER_ID" \
    -allowProvisioningUpdates

echo ""
echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}  Gotowe — build wysłany na TestFlight${NC}"
echo -e "${GREEN}======================================${NC}"
echo ""
echo "Sprawdź status przetwarzania w App Store Connect → TestFlight."
