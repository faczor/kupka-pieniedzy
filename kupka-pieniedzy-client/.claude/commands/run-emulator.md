Uruchom aplikację na emulatorze Androida przy pomocy skryptu pomocniczego.

Kroki:
1. Odpal `./scripts/android/run-emulator.sh` (z roota projektu `kupka-pieniedzy-client/`).
   - Bez argumentu: użyje działającego emulatora albo zbootuje pierwszy AVD z listy.
   - Z argumentem (np. `Pixel_8`): zbootuje wskazany AVD.
   - `-l`: tylko wylistuje dostępne AVD.
   Skrypt sam: wykrywa Android SDK, dotworzy `local.properties` jeśli go brak,
   ostrzeże o braku `app_config.json`, zbuduje `:androidApp:installDebug` i odpali apkę.
2. Po starcie zrób zrzut ekranu i pokaż go użytkownikowi:
   `adb shell screencap -p /sdcard/run.png && adb pull /sdcard/run.png /tmp/run.png`
   a następnie odczytaj `/tmp/run.png`.
3. Jeśli apka pokazuje „Aplikacja nie jest skonfigurowana (Supabase)” — brakuje
   `app_config.json` (gitignorowany sekret). Skopiuj go z głównego checkoutu repo
   (patrz CLAUDE.md → sekcja „Worktree”) i uruchom ponownie.

Argumenty użytkownika (opcjonalna nazwa AVD): $ARGUMENTS
Celem uruchomienia jest umozliwienie uzytkownikowi przejscia szybko do testowania manualnego
