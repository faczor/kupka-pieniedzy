#!/usr/bin/env bash
#
# Uruchamia aplikację (debug) na emulatorze Androida:
#   1. wykrywa Android SDK i upewnia się, że jest local.properties,
#   2. wybiera działający emulator albo bootuje AVD (pierwszy z listy lub podany argumentem),
#   3. buduje + instaluje :androidApp:installDebug,
#   4. odpala aplikację.
#
# Użycie:
#   scripts/android/run-emulator.sh            # auto: pierwszy AVD / działający emulator
#   scripts/android/run-emulator.sh Pixel_8    # konkretny AVD
#   scripts/android/run-emulator.sh -l         # tylko wylistuj dostępne AVD
#
set -euo pipefail

PKG="com.sd.kupka_pieniedzy_client"
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# --- 1. Android SDK ---
detect_sdk() {
  for p in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
    [ -n "$p" ] && [ -d "$p" ] && { echo "$p"; return; }
  done
  return 1
}
SDK="$(detect_sdk)" || { echo "❌ Nie znaleziono Android SDK (ustaw ANDROID_HOME)."; exit 1; }
ADB="$SDK/platform-tools/adb"
EMU="$SDK/emulator/emulator"
[ -x "$ADB" ] || ADB="$(command -v adb)"
[ -x "$EMU" ] || { echo "❌ Brak emulatora w $SDK/emulator."; exit 1; }

# --- tylko lista AVD ---
if [ "${1:-}" = "-l" ]; then
  echo "Dostępne AVD:"; "$EMU" -list-avds; exit 0
fi

# --- local.properties (gitignorowany, nie ma go w świeżym worktree) ---
LP="$PROJECT_DIR/local.properties"
if [ ! -f "$LP" ]; then
  echo "sdk.dir=$SDK" > "$LP"
  echo "ℹ️  Utworzono local.properties → sdk.dir=$SDK"
fi

# --- ostrzeżenie o configu Supabase (gitignorowany sekret) ---
CFG="$PROJECT_DIR/shared/src/commonMain/composeResources/files/app_config.json"
if [ ! -f "$CFG" ]; then
  echo "⚠️  Brak app_config.json — apka pokaże „Aplikacja nie jest skonfigurowana (Supabase)”."
  echo "    Skopiuj go z głównego checkoutu repo (patrz CLAUDE.md → Worktree)."
fi

# --- 2. emulator ---
running_emulator() { "$ADB" devices | awk '/^emulator-[0-9]+\tdevice$/ {print $1; exit}'; }

DEVICE="$(running_emulator || true)"
if [ -z "$DEVICE" ]; then
  AVD="${1:-$("$EMU" -list-avds | head -1)}"
  [ -n "$AVD" ] || { echo "❌ Brak żadnego AVD. Utwórz go w Android Studio (Device Manager)."; exit 1; }
  echo "▶️  Bootuję emulator: $AVD"
  "$EMU" "@$AVD" -netdelay none -netspeed full >/dev/null 2>&1 &
  echo "⏳ Czekam na start systemu…"
  "$ADB" wait-for-device
  until [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done
  DEVICE="$(running_emulator)"
fi
echo "📱 Urządzenie: $DEVICE"

# --- 3. build + install ---
echo "🔨 :androidApp:installDebug…"
( cd "$PROJECT_DIR" && ./gradlew :androidApp:installDebug --console=plain )

# --- 4. launch ---
"$ADB" -s "$DEVICE" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
echo "✅ Uruchomiono $PKG na $DEVICE"
