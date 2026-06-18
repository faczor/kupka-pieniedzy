1. Import this Claude Design project using the Claude Design connector: https://claude.ai/design/p/3de7b074-9f34-4804-b341-265b8166123e
2. Remember to fetch chat's context! Its important
3. Wait for user to specify on what area we will be working on
4. After receiving user input with requirements
5. Create a branch (name it based on requirements) based on the origin master branch and run on new worktree
6. **Skopiuj gitignorowane pliki konfiguracyjne do nowego worktree** — nie przechodzą one z głównego
   checkoutu, a bez nich apka pokaże „Aplikacja nie jest skonfigurowana (Supabase)” / build nie
   znajdzie SDK:
   - `shared/src/commonMain/composeResources/files/app_config.json` (sekret Supabase) — skopiuj z głównego checkoutu repo,
   - `local.properties` (`sdk.dir=…`) — skopiuj lub pozwól, żeby `scripts/android/run-emulator.sh` dotworzył go sam.
   Szczegóły: patrz `CLAUDE.md` → sekcja „Worktree”.
