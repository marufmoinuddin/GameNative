# CLI Real-Only Migration Record

## Objective

Make `gamenative-cli` fully real (JavaSteam network-backed) with no non-real CLI execution paths.

## Scope

Applies to:

- `gamenative-linux/cli/**`
- CLI wiring that previously supported multiple gateway modes
- CLI argument/help text related to non-real modes

Out of scope for this pass:

- Desktop shell mode behavior (`desktop/shell` remains independently configurable)
- Steam download/install internals beyond current CLI queue simulation
- Runtime game launch internals beyond current orchestrator integration

## Completed Work

1. Removed mode switching in CLI
   - Dropped CLI env/arg parsing for gateway selection.
   - Removed `--mode` usage from CLI help output.
   - Kept CLI startup banner explicit that it is JavaSteam network mode.

2. Removed non-real gateway code from CLI
   - Deleted non-real auth/library/download gateway classes from CLI wiring.
   - Kept a single real gateway implementation for auth and owned-library retrieval.

3. Simplified factory/controller wiring
   - Refactored `CliSteamServiceFactory` to expose one `create()` (real only).
   - Updated `CliController` defaults to real-only services.

4. Kept deterministic testability via injection
   - Preserve constructor injection in `CliController` (`CliSteamServices` argument).
   - Replaced tests that depended on legacy mode behavior with explicit in-test fakes or in-memory services.

5. Updated UX text to be truthful
   - Remove wording implying fake/automatic approval behavior.
   - Keep clear Steam Guard prompts for real auth challenges.

## Acceptance Criteria

- Running `./gradlew -p gamenative-linux :cli:runCli` uses only real JavaSteam path.
- No CLI code references legacy gateway modes or fallback auth behavior.
- CLI help does not mention non-real mode options.
- `:cli:test` passes.
- REAL login still reaches Steam and returns real outcomes (success/failure based on actual credentials/challenges).

## Verification Commands

```bash
cd /home/maruf/git/GameNative
./gradlew -p gamenative-linux :cli:test
./gradlew -p gamenative-linux :cli:runCli --args='--help'
./gradlew -p gamenative-linux :cli:runCli
```
