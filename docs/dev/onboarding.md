# Linux Port Onboarding

## Prerequisites

- JDK 21 installed locally (module bytecode target remains JVM 17)
- Gradle wrapper executable from repository root

## Module Layout

The Linux scaffold lives under `gamenative-linux/` and currently includes:

- `cli`
- `core/domain`
- `core/runtime`
- `core/store-steam`
- `desktop/shell`
- `infra/config`
- `infra/keyring`
- `infra/network`
- `infra/persistence`
- `infra/notifications`

## Useful Commands

From repository root:

- Compile all Linux scaffold modules:
  - `./gradlew -p gamenative-linux :core:domain:compileKotlin :core:runtime:compileKotlin :core:store-steam:compileKotlin :desktop:shell:compileKotlin :infra:config:compileKotlin :infra:keyring:compileKotlin :infra:network:compileKotlin :infra:persistence:compileKotlin :infra:notifications:compileKotlin`

- Android import audit for Linux workspace:
  - `rg -n "^import android\." gamenative-linux | sort`

- Run runtime prototype demo:
  - `./gradlew -p gamenative-linux :core:runtime:runRuntimePrototype`

- Run runtime diagnostics JSON reporter:
  - `./gradlew -p gamenative-linux :core:runtime:runRuntimeDiagnostics`

- Run runtime startup decision report:
  - `./gradlew -p gamenative-linux :core:runtime:runRuntimeStartupDecision`

- Run desktop shell prototype UI:
  - `./gradlew -p gamenative-linux :desktop:shell:runDesktopShell`

- Run GameNative CLI (real Steam network mode):
  - `./gradlew -p gamenative-linux :cli:runCli`

- Show GameNative CLI help:
  - `./gradlew -p gamenative-linux :cli:runCli --args='--help'`

- Run strict Phase 3 runtime proof (Wine+Box64 readiness gate):
  - `./gradlew -p gamenative-linux :core:runtime:runRuntimeProof`

- Build release artifact bundle (tar.gz + SHA256 + SPDX SBOM):
  - `./tools/build_linux_release_artifacts.sh 0.1.0`

- Validate Fedora RPM spec dependencies/parseability:
  - `./tools/validate_rpm_spec.sh`

- Verify generated release artifacts and checksum/SBOM linkage:
  - `./tools/verify_linux_release_artifacts.sh`

- Validate runtime diagnostics schema stability:
  - `./tools/phase6_schema_stability_check.sh`

- Run security log leakage scan:
  - `./tools/phase6_security_log_check.sh`

- Build source RPM from canonical release artifact:
  - `./tools/build_source_rpm.sh 0.1.0`

- Sign release artifacts with imported GPG key:
  - `./tools/sign_linux_release_artifacts.sh`

## Verification Snapshot (2026-04-01)

- Full Linux test suite: passing.
- Release artifact build + verification: passing.
- Runtime schema stability check: passing.
- Security log leakage check: passing.
- Runtime proof: passing on this workstation (architecture-aware validation). ARM64 hosts still require Wine+Box64.

## GameNative CLI (`gamenative-cli`)

- CLI mode is real-only: JavaSteam network flow is always enabled.
- CLI no longer supports non-real mode flags.
- Login, Steam Guard prompts, and owned game list are backed by live Steam responses.
- For packaged installs, the RPM launcher entrypoint is `gamenative-cli`.

Runtime prototype artifacts are written to:

- `gamenative-linux/core/runtime/build/runtime-prototype-state/sessions/`
- `gamenative-linux/core/runtime/build/runtime-prototype-state/logs/`
- `gamenative-linux/core/runtime/build/runtime-prototype-state/logs/*.stderr.log`
- `gamenative-linux/core/runtime/build/runtime-prototype-state/config/profiles.json`
- `gamenative-linux/core/runtime/build/runtime-prototype-state/crashes/*.json`
- `gamenative-linux/core/runtime/build/runtime-prototype-state/recovery/last-termination.json`
- `gamenative-linux/core/runtime/build/runtime-prototype-state/recovery/last-recovery.json` (when interrupted sessions are detected)
- `gamenative-linux/core/runtime/build/runtime-prototype-state/recovery/startup-decision.json`
- `gamenative-linux/core/runtime/build/runtime-prototype-state/recovery/startup-decision-report.txt`
- `gamenative-linux/core/runtime/build/runtime-prototype-state/diagnostics/capability-report.txt`
- `gamenative-linux/core/runtime/build/runtime-prototype-state/diagnostics/runtime-snapshot.txt`
- `gamenative-linux/core/runtime/build/runtime-prototype-state/diagnostics/runtime-snapshot.json`

Runtime proof artifacts are written to:

- `gamenative-linux/core/runtime/build/runtime-proof/runtime-proof-report.txt`
- `gamenative-linux/core/runtime/build/runtime-proof/runtime-proof-success.txt` (only on pass)

`runRuntimePrototype` now routes launch through `RecoveryBackedRuntimeSupervisionGate`, so crash-pattern policy can delay or block launch before process start.

`runtime-snapshot.json` now includes a `crashBundle` object with:

- `profileId`
- `environmentSummary`
- `stdoutTail`
- `stderrTail`
- `exitCode`
- `abnormalExit`
- `terminationMode`

`runtime-snapshot.json` also includes optional `recovery` context when an interrupted session marker is detected on startup.

`runtime-snapshot.json` includes `startupDecision` (`CLEAN_START`, `ATTACH_RUNNING_SESSION`, or `RECOVER_INTERRUPTED_SESSION`) for startup action tracing.

`runtime-snapshot.json` includes `startupRecommendation` with a machine-readable action + recommendation string for UI consumption.

`runtime-snapshot.json` includes `supervisionRecommendation` to guide launch gating (`PROCEED`, `DELAY_RETRY`, `REQUIRE_MANUAL_INTERVENTION`).

Runtime recovery state now persists supervision holds in `recovery/supervision-hold.json` so delayed/manual launch policy survives app restarts.

Runtime recovery state now persists per-session retry attempts in `recovery/retry-attempts.json` (abnormal exits increment, clean exits clear).

Runtime recovery state now persists supervision decision telemetry in `recovery/supervision-events.json`.

Runtime recovery state now persists launch state machine events in `recovery/launch-state-events.json`.

`runtime-snapshot.json` also includes `supervisionHold` with persisted hold metadata (`action`, `reason`, `retryNotBefore`, `remainingBackoffSeconds`, `active`).

`runtime-snapshot.json` includes `retryAttempt` for the latest session (`attempts`, `lastOutcome`, `updatedAt`) to support crash-loop forensics.

`runtime-snapshot.json` includes `supervisionEvents` for recent startup gating decisions and timestamps.

`runtime-snapshot.json` includes `launchStateTimeline` for the latest session (`QUEUED`, `GATED_DELAY`, `LAUNCHED`, `BLOCKED`, `EXITED`).

`runtime-snapshot.json` includes `incidentSummary` (title, summary, severity, recommendedAction, signals) for a single actionable runtime incident card.

`runtime-snapshot.json` includes `schemaVersion` for backwards-compatible parser upgrades.

`startupRecommendation` also includes:

- `code` (`STARTUP_CLEAN`, `STARTUP_ATTACH`, `STARTUP_RECOVER`)
- `severity` (`INFO` or `WARNING`)
- `tags` (classifier list for UI routing/styling)

## Current Migration State

- Data models in `core/domain/src/main` are fully promoted from staging.
- `core/domain/src/staging/kotlin` is now empty.

## Baseline Implementations Added

- `infra/config`: `FileConfigService`
- `infra/config`: typed `AppConfig` schema + `LegacyConfigKeyMap` import (`ConfigService.importLegacyKeyValues`)
- `infra/keyring`: `SecretToolCredentialStore` (libsecret primary), `EncryptedFileCredentialStore` (AES-GCM fallback), `LinuxCredentialStore` (chained primary/fallback)
- `infra/network`: `LinuxNetworkStateService`
- `infra/notifications`: `NotifySendNotificationService` (`notify-send` shell backend with fallback logging)
- `infra/persistence`: `FilePersistenceService` (data/backup directories + health + snapshot backup), `SqliteMigrationPlan`, `SqliteSchemaMigrator`, `DbMigrationValidator`
- `core/runtime`: `ShellCapabilityDetector`
- `core/runtime`: `DefaultRuntimeOrchestrator`, `LocalProcessRunner`, `DefaultEnvironmentComposer`, `InMemoryProfileRepository`
- `core/runtime`: `FileProfileRepository` (JSON-backed runtime profile persistence)
- `core/runtime`: runtime profiles now include `supervisionPolicy` knobs (`lookbackMinutes`, `manualInterventionThreshold`, retry backoff multipliers, incident warning/critical retry thresholds) consumed by supervision gate/advisor/diagnostics.
- `core/store-steam`: `InMemorySteamSessionManager`, `JavaSteamSessionManager`, `InMemorySteamLibraryService`, `JavaSteamLibraryService`, `FixtureSteamLibraryService`, `InMemorySteamDownloadManager`, `JavaSteamDownloadManager`, `FixtureSteamDownloadManager`, `InMemorySteamCloudSaveService`, `JavaSteamCloudSaveService`, `FixtureSteamCloudSaveService`
- `desktop/shell`: tabbed P0 workflow surfaces (Diagnostics, Account, Library, Game Detail, Downloads, Profiles, Session Monitor, Settings)
- `desktop/shell`: `DesktopSettingsStore` (`~/.config/gamenative/desktop-settings.properties`) and `DesktopTaskScheduler` (`~/.local/state/gamenative/desktop-tasks.properties`)
- `desktop/shell`: account/library/download workflows now route via `JavaSteamSessionManager`, `JavaSteamLibraryService`, and `JavaSteamDownloadManager` through prototype gateway adapters
- `desktop/shell`: gateway selection is configurable via `GAMENATIVE_STEAM_GATEWAY_MODE` (`PROTOTYPE` default, `FIXTURE` optional)
- `desktop/shell`: fixture mode root is configurable via `GAMENATIVE_STEAM_FIXTURE_ROOT` (defaults to `~/.local/share/gamenative/fixtures`)
- Fixture library/download/cloud services now delegate to the gateway-backed JavaSteam service layer to keep behavior parity while adapters are swapped.

Fixture mode expects these files under the fixture root:

- `steam-library.json`
- `steam-downloads.json`
- `steam-auth-users.txt` (optional allow-list for usernames)

## Unit Test Commands

- `./gradlew -p gamenative-linux :infra:config:test :core:runtime:test :core:store-steam:test`
- `./gradlew -p gamenative-linux :infra:keyring:test`
- `./gradlew -p gamenative-linux :infra:network:test :infra:notifications:test`
- `./gradlew -p gamenative-linux :infra:persistence:test`
- `./gradlew -p gamenative-linux :core:runtime:test`
- `./gradlew -p gamenative-linux :desktop:shell:test`
- `./gradlew -p gamenative-linux :infra:config:test :infra:keyring:test :infra:persistence:test :core:domain:test :core:store-steam:test :desktop:shell:test`

Phase 6 hardening helper script:

- `./tools/linux_phase6_hardening_check.sh`

`core:runtime:test` includes Phase 3 integration checks for supervision/recovery orchestration in `RuntimePhase3IntegrationTest`.

Runtime profile persistence file (for `FileProfileRepository`) is caller-defined, for example:

- `~/.config/gamenative/profiles.json`
