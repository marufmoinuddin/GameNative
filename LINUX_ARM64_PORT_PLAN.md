# GameNative → Linux ARM64 Port Plan

## Purpose and Scope

This document is the authoritative engineering execution plan for porting GameNative from an Android application to a Linux ARM64 native desktop application. It is grounded in the actual structure of the existing Android codebase (Kotlin + Jetpack Compose, Hilt, Room, JavaSteam, Winlator-derived runtime) and written for engineering execution — not just high-level aspiration.

The target is **not** a recompile. It is a deliberate, staged migration from Android-specific runtime behavior to a Linux-native architecture, preserving the core product value: launcher experience, multi-store account integration (Steam, GOG, Epic, Amazon), game library management, install/download orchestration, and game launch via Wine + Box64/FEX on ARM64 Linux.

The end state is a **Linux desktop application running on Fedora ARM64**, with an officially buildable, CI-generated Fedora ARM64 artifact (RPM + portable tarball) and a reproducible release pipeline.

---

## Existing Codebase Analysis

Before planning the port it is essential to have a factual picture of what exists. The following is a grounded analysis of the current Android project at `app/src/main/java/app/gamenative/`.

### Source Module Map

| Directory / File | Role | Android Coupling |
|---|---|---|
| `MainActivity.kt` (24 KB) | Entry point, Activity lifecycle, navigation host | **High** — Activity, Compose setContent |
| `PluviaApp.kt` (8 KB) | Application class, Hilt entry point, global init | **High** — Application subclass |
| `PrefManager.kt` (43 KB) | All user preferences via DataStore | **Medium** — DataStore is JVM/Android |
| `NetworkMonitor.kt` | Connectivity state via Android ConnectivityManager | **High** — Android API only |
| `CrashHandler.kt` | Uncaught exception handler | **Low** — pure Kotlin |
| `Crypto.kt` | Encryption/decryption helpers | **Low** — pure Kotlin/JVM |
| `Constants.kt` | App-wide compile-time constants | **Low** — pure Kotlin |
| `ReleaseTree.kt` | Timber logging tree | **Low** — pure Kotlin |
| `service/SteamService.kt` (167 KB) | Core Steam connection, library sync, download orchestration, cloud saves | **High** — Android Service lifecycle |
| `service/DownloadService.kt` | Foreground download service | **High** — Android ForegroundService |
| `service/SteamAutoCloud.kt` (44 KB) | UFS cloud save sync logic | **Medium** — some Android IO |
| `service/AchievementWatcher.kt` | Steam achievement polling | **Low** — pure logic |
| `service/NotificationHelper.kt` | Android notifications | **High** — NotificationManager |
| `service/amazon/` | Amazon Games service | **High** — Android Service |
| `service/epic/` | Epic Games service | **High** — Android Service |
| `service/gog/` | GOG Galaxy service | **High** — Android Service |
| `data/` (35 files) | All data models: SteamApp, DownloadInfo, EpicGame, GOGGame, AmazonGame, LibraryItem, TouchGestureConfig, etc. | **Low** — pure Kotlin data classes |
| `db/` | Room database, DAOs, migrations | **Medium** — Room (JVM compatible via SQLDelight swap) |
| `di/` | Hilt dependency injection modules | **Medium** — Hilt (Android-specific) |
| `gamefixes/` | Per-game launch fix registry (24 files: STEAM_*, GOG_*, EPIC_*) | **Low** — pure Kotlin |
| `ui/PluviaMain.kt` (97 KB) | Monolithic Compose navigation root | **High** — Android Compose |
| `ui/screen/` | All workflow screens | **High** — Android Compose |
| `ui/component/` | Reusable UI components | **High** — Android Compose |
| `externaldisplay/` | X server bridge and input routing | **High** — Android-specific display |
| `enums/` | All enumerations | **Low** — pure Kotlin |
| `events/` | Event bus models | **Low** — pure Kotlin |
| `utils/` | Utility classes | **Mixed** |
| `statsgen/` | PostHog analytics integration | **Medium** — Android SDK variant |
| `api/` | Store API clients (REST/protobuf) | **Low–Medium** — OkHttp/Ktor |
| `jniLibs/` | Compiled Winlator native `.so` libraries | **High** — Android ABI packaged |

### Key Dependencies (from `build.gradle.kts`)

| Dependency | Linux Strategy |
|---|---|
| JavaSteam + javasteam-depotdownloader | **Reuse directly** — pure JVM library |
| Jetpack Compose (Android BOM) | **Replace** with Compose Multiplatform Desktop |
| Room | **Replace** with SQLDelight or exposed + SQLite JDBC |
| Hilt | **Replace** with Koin or manual DI (Hilt is Android-specific) |
| DataStore Preferences | **Replace** with JVM-compatible config library (Konf, HOCON, or plain file) |
| OkHttp / Ktor | **Reuse** — JVM-portable |
| Kotlinx Serialization | **Reuse directly** |
| Timber | **Replace** with SLF4J + Logback |
| Coroutines | **Reuse directly** |
| Protobuf Java | **Reuse directly** |
| PostHog Android SDK | **Replace** with PostHog JVM/server SDK |
| Winlator JNI `.so` libraries | **Replace** — build Linux ARM64 equivalents or call Wine/Box64 as external processes |
| Chrome Custom Tabs (GOG OAuth) | **Replace** with desktop browser launch via `xdg-open` |
| Zxing (QR) | **Reuse** — JVM |
| Spongycastle | **Replace** with BouncyCastle JVM |
| Landscapist/Coil image loading | **Replace** with Compose Desktop image loading |

### Reuse Classification Summary

- **Direct reuse (low/no changes):** All files in `data/`, `enums/`, `events/`, `gamefixes/`, `CrashHandler.kt`, `Crypto.kt`, `Constants.kt`, `ReleaseTree.kt`, `AchievementWatcher.kt`. These are pure Kotlin with no Android imports.
- **Refactor (interface extraction needed):** `SteamAutoCloud.kt`, `PrefManager.kt`, store API clients, `db/` schema.
- **Replace (Android APIs too deep):** `SteamService.kt`, `DownloadService.kt`, all UI, `externaldisplay/`, `PluviaApp.kt`, `MainActivity.kt`, `NotificationHelper.kt`, all Android service classes.

---

## Porting Philosophy

The correct strategy is: **extract reusable domain logic, replace platform-dependent host runtime.**

- **Reuse** where logic is platform-agnostic (data models, game fixes, API protocols, crypto, serialization).
- **Refactor** where Android APIs are thin wrappers over portable logic.
- **Replace** where code depends on Android Service lifecycle, Android NDK assumptions, or Android-specific APIs.

This splits work into three streams:

1. Discovery and decomposition of existing functionality.
2. Linux-native host implementation and runtime integration.
3. Product hardening and Fedora ARM64 release engineering.

---

## Success Criteria

The project is successful when all of the following are true:

1. A Linux ARM64 desktop binary installs and runs on Fedora ARM64.
2. The binary supports at least one complete account workflow (Steam first).
3. The binary discovers, installs, and launches at least one test game end-to-end via Wine + Box64.
4. Runtime profile management works (equivalent to existing container/settings model).
5. Basic telemetry/logging and crash diagnostics work on Linux.
6. Build and release are automated, reproducible, and CI-generated.

Optional stretch outcomes:

1. GOG, Epic, and Amazon support parity.
2. Multiple compatibility layer presets (Box64 and FEX).
3. Flatpak or COPR delivery package.
4. Save sync (UFS cloud save equivalent via `SteamAutoCloud` logic reuse).

---

## Program Structure

Work is organized into 7 phases, each with concrete deliverables.

- **Phase 0** — Baseline discovery and architecture mapping.
- **Phase 1** — New Linux workspace and core module scaffolding.
- **Phase 2** — Domain extraction and shared logic migration.
- **Phase 3** — Linux runtime host and process orchestration.
- **Phase 4** — Desktop UI and user workflows.
- **Phase 5** — Packaging, CI/CD, and Fedora ARM64 release path.
- **Phase 6** — Stabilization, compatibility, and production hardening.

## Execution Status (as of 2026-04-01)

This section tracks real implementation progress against the plan.

### Phase Completion Snapshot

- Phase 0: **mostly complete** (architecture docs and audits created; some deep audit outputs still pending)
- Phase 1: **substantially complete** (Linux workspace scaffolded, modules compile, core interfaces and baseline impls added)
- Phase 2: **complete** (domain migration, steam service decomposition boundaries, typed config migration, keyring fallback chain, and persistence migration validator implemented)
- Phase 3: **complete** (runtime orchestration, supervision/recovery persistence, diagnostics pipeline, and integration checks implemented)
- Phase 4: **workflow-complete, visual-parity pending** (desktop shell P0 workflow surfaces implemented, but Android-grade 1:1 frontend parity has not yet been delivered)
- Phase 5: **complete** (release-grade packaging and CI pipeline implemented with artifact generation, integrity verification, source RPM build, signing gates, and release workflows)
- Phase 6: **complete** (hardening automation gates now pass end-to-end: audit, compile/test, schema stability, security scan, and architecture-aware runtime proof)

### Deep Verification Snapshot (2026-04-01)

- Full Linux module test suite is passing (`core/*`, `infra/*`, `desktop/shell`).
- Release artifact pipeline is passing locally (`build_linux_release_artifacts.sh` + `verify_linux_release_artifacts.sh`).
- Runtime diagnostics schema stability gate is passing (`phase6_schema_stability_check.sh`).
- Security log leakage scan is passing (`phase6_security_log_check.sh`).
- Integrated hardening gate is passing all checks end-to-end on this host.
- Runtime proof is now architecture-aware: ARM64 hosts require Wine+Box64; non-ARM64 hosts require Wine capability + smoke validation.

### Completed So Far

- `gamenative-linux` workspace scaffold created and compiling.
- Modules added: `core/domain`, `core/runtime`, `core/store-steam`, `infra/config`, `infra/keyring`, `infra/network`, `infra/persistence`, `infra/notifications`.
- Desktop module added: `desktop/shell` (Linux desktop workflow prototype).
- Shared-domain migration completed for data models/enums/events in `core/domain/src/main`.
- Android-coupled staging files removed from `core/domain/src/staging` (staging now empty).
- Core contracts created for runtime, steam store, domain services, and infra services.
- Baseline implementations added:
  - `FileConfigService`
  - `AppConfig` + `LegacyConfigKeyMap` migration support
  - `ConfigService.importLegacyKeyValues(...)`
  - `EncryptedFileCredentialStore`
  - `SecretToolCredentialStore` (libsecret CLI-backed primary store)
  - `LinuxCredentialStore` (primary libsecret + encrypted-file fallback)
  - `LinuxNetworkStateService`
  - `NotifySendNotificationService`
  - `FilePersistenceService`
  - `SqliteMigrationPlan`
  - `SqliteSchemaMigrator`
  - `DbMigrationValidator`
  - `ShellCapabilityDetector`
  - `DefaultRuntimeOrchestrator`
  - `LocalProcessRunner`
  - `DefaultEnvironmentComposer`
  - `InMemorySteamSessionManager`
  - `JavaSteamSessionManager` (gateway-backed boundary for incremental JavaSteam wiring)
  - `InMemorySteamLibraryService`
  - `JavaSteamLibraryService` (gateway-backed metadata boundary)
  - `FixtureSteamLibraryService`
  - `InMemorySteamDownloadManager`
  - `JavaSteamDownloadManager` (gateway-backed queue coordinator boundary)
  - `FixtureSteamDownloadManager`
  - `InMemorySteamCloudSaveService`
  - `JavaSteamCloudSaveService` (gateway-backed cloud sync boundary)
  - `FixtureSteamCloudSaveService`
  - `FileProfileRepository`
  - `FileRuntimeDiagnosticsService`
  - `RuntimeDiagnosticsReporter`
- Unit tests added and passing for implemented services.
- Linux workspace Android-import gate is currently clean.
- Runtime prototype and diagnostics tasks produce structured JSON diagnostics artifacts.
- Runtime diagnostics now include abnormal-exit metadata (`exitCode`, `abnormalExit`) and archived crash bundles with rotation.
- Runtime process stop path now records `terminationMode` (`graceful`, `force`, `graceful-timeout-force`, `already-exited`) with process-tree-aware shutdown behavior.
- Runtime recovery hooks now persist active-session markers and termination records, and can emit `previous-session-interrupted` recovery reasons on restart.
- Runtime startup resolver now persists structured startup decisions (`CLEAN_START`, `ATTACH_RUNNING_SESSION`, `RECOVER_INTERRUPTED_SESSION`) for restart diagnostics.
- Runtime tooling now emits a startup decision recommendation report artifact for upcoming diagnostics panel integration.
- Runtime diagnostics snapshot now contains a machine-readable `startupRecommendation` block for direct UI binding.
- `startupRecommendation` now includes `severity` and `tags` to support deterministic UI styling and filtering.
- `startupRecommendation` now includes stable recommendation `code` values (`STARTUP_CLEAN`, `STARTUP_ATTACH`, `STARTUP_RECOVER`) for robust UI/business logic routing.
- Runtime recovery now persists bounded recent termination history for supervision-policy decisions.
- Runtime diagnostics now include a machine-readable `supervisionRecommendation` (`PROCEED`, `DELAY_RETRY`, `REQUIRE_MANUAL_INTERVENTION`) derived from startup action plus recent crash patterns.
- Runtime orchestrator launch flow now enforces supervision gating (optional delay backoff and manual-intervention block) before process start.
- Runtime supervision gate now persists retry/manual holds in recovery state so launch policy survives app restarts.
- Runtime diagnostics now expose persisted supervision-hold metadata (`retryNotBefore`, active/manual state) for UI/runtime visibility.
- Runtime supervision policy is now profile-configurable (lookback window, crash-loop threshold, retry backoff factors) and applied in gate/advisor decisions.
- Runtime recovery now tracks per-session retry attempts for abnormal exits and surfaces them in diagnostics for crash-loop forensics.
- Runtime orchestrator now emits persisted supervision telemetry events (`PROCEED`, `DELAY_RETRY`, `REQUIRE_MANUAL_INTERVENTION`) for startup diagnostics.
- Runtime launch attempts now persist a per-session state timeline (`QUEUED`, `GATED_DELAY`, `LAUNCHED`, `BLOCKED`, `EXITED`) for diagnostics UI timelines.
- Runtime diagnostics now include an `incidentSummary` rollup payload that merges crash/retry/supervision/timeline signals into one actionable card model.
- Incident summary severity is now profile-policy aware (warning/critical retry thresholds) and emits remediation hints for common failure signatures (segfault, graphics init, sync-layer, force-timeout exits).
- Runtime integration checks now validate supervision/recovery orchestration behavior end-to-end (delay-hold launch, manual-hold block, and persisted telemetry/timeline events).
- Runtime module now exposes a strict `runRuntimeProof` verification task that fails unless Wine+Box64 capability checks and smoke command execution pass.
- Desktop shell now includes tabbed P0 workflow surfaces for Diagnostics, Account sign-in, Library, Game Detail, Downloads, Profiles, Session Monitor, and Settings using runtime/store service boundaries.
- Desktop shell controller now routes account/library/download workflows through `JavaSteam*` service boundaries via prototype gateways instead of direct in-memory service implementations.
- Desktop shell Steam gateway wiring is now mode-configurable (`PROTOTYPE` or `FIXTURE`) using `GAMENATIVE_STEAM_GATEWAY_MODE`, with fixture-root override via `GAMENATIVE_STEAM_FIXTURE_ROOT`.
- Desktop shell now persists app-level settings in `~/.config/gamenative/desktop-settings.properties` and tracks background queue tasks in `~/.local/state/gamenative/desktop-tasks.properties`.
- Desktop shell controller and persistence components now include workflow tests for profile save/reload, game detail selection, download task state tracking, settings persistence, and task resume behavior.
- Packaging scaffold added under `packaging/fedora/` (RPM spec, desktop entry, AppStream metadata, dependency list, post-install checks).
- Linux port CI workflow scaffold added at `.github/workflows/linux-port-ci.yml` to compile/test Linux modules and run audits.
- Blocking Fedora ARM64 runtime gate workflow added at `.github/workflows/linux-port-fedora-arm64.yml` (self-hosted ARM64 runner) for runtime proof, hardening checks, and release artifact verification.
- Linux tagged release workflow added at `.github/workflows/linux-port-release.yml` with compile/test, artifact build, artifact integrity checks, RPM spec validation, source RPM generation, and GPG signing gates.
- Release artifact tooling now emits both traceable build-id tarballs and canonical RPM source tarballs with SHA256 + SPDX SBOM + manifest metadata.
- Release integrity/signing helpers added (`build_linux_release_artifacts.sh`, `verify_linux_release_artifacts.sh`, `build_source_rpm.sh`, `sign_linux_release_artifacts.sh`).
- Phase 6 automation now includes explicit runtime schema stability and security log leakage checks (`phase6_schema_stability_check.sh`, `phase6_security_log_check.sh`) integrated into hardening gates.
- Phase 6 helper automation added as `tools/linux_phase6_hardening_check.sh` to run import audits, compile/tests, and runtime proof status checks.
- Fixture-backed Steam library/download/cloud services now route through the same gateway-backed service layer used by JavaSteam decomposition paths.
- Infra config now supports a typed `AppConfig` schema and legacy key migration import map.
- Infra keyring now supports libsecret-first credential writes/reads/deletes with encrypted file fallback.
- Infra persistence now includes SQLite schema migrations and a migration validator test for fresh DB bootstrap.

### Remaining High-Level Work

- Replace in-memory desktop workflow adapters with production Steam auth/library/download integrations and real runtime session streaming.
- Execute a dedicated frontend parity program to make Linux UI/UX a 1:1 replica of the Android experience (visual design, interactions, motion, and screen behavior).

### Suggested Next Milestone

- Start Phase 4A frontend parity baseline (token extraction + component inventory + visual diff tooling) while finalizing production store/runtime integration.

---

## Phase 0: Baseline Discovery and Architecture Mapping

**Goal:** establish a factual, up-to-date model of what exists, what can be reused, and what must be rewritten.

### 0.1 Feature Inventory

Create a feature matrix with these columns:

- Feature name
- User-visible behavior
- Source modules/files (actual file names)
- Android dependency type (Service / Activity / NDK / API)
- Reuse decision (reuse / refactor / replace)
- Priority (P0 / P1 / P2)

Initial feature categories (mapped to known source files):

| Feature | Key Source Files | Reuse Decision | Priority |
|---|---|---|---|
| Steam auth and session | `SteamService.kt`, `api/` | Refactor | P0 |
| Steam library sync | `SteamService.kt`, `data/SteamApp.kt` | Refactor | P0 |
| Steam download/install | `SteamService.kt`, `service/DownloadService.kt`, `data/DownloadInfo.kt` | Refactor | P0 |
| Steam cloud saves | `service/SteamAutoCloud.kt` | Refactor | P1 |
| Game launch orchestration | `SteamService.kt`, `gamefixes/GameFixesRegistry.kt` | Refactor | P0 |
| Game fixes registry | `gamefixes/` (24 files) | **Reuse directly** | P0 |
| GOG integration | `service/gog/`, `data/GOGGame.kt` | Replace service, reuse model | P1 |
| Epic integration | `service/epic/`, `data/EpicGame.kt` | Replace service, reuse model | P1 |
| Amazon integration | `service/amazon/`, `data/AmazonGame.kt` | Replace service, reuse model | P2 |
| Preferences/settings | `PrefManager.kt` (43 KB) | Refactor | P0 |
| Database/persistence | `db/` (Room) | Refactor (swap to SQLDelight) | P0 |
| UI shell and navigation | `ui/PluviaMain.kt` | Replace | P0 |
| Runtime profile management | `PrefManager.kt`, runtime sections | Refactor | P0 |
| X server / display bridge | `externaldisplay/` | Replace entirely | P0 |
| Notifications | `service/NotificationHelper.kt` | Replace | P1 |
| Achievements | `service/AchievementWatcher.kt` | **Reuse directly** | P2 |
| Analytics | `statsgen/` (PostHog Android) | Replace with JVM SDK | P1 |
| Crypto utilities | `Crypto.kt`, Spongycastle | Reuse, swap to BouncyCastle | P0 |
| Network monitoring | `NetworkMonitor.kt` | Replace (Linux netlink or polling) | P1 |

### 0.2 Dependency Mapping

Create diagrams for:

1. Android lifecycle dependencies (Activity → Service → BroadcastReceiver chains).
2. `SteamService.kt` internal dependency graph — this single 167 KB file contains the majority of Steam protocol, download orchestration, and library sync logic and must be carefully decomposed.
3. JNI/Winlator native library chain (what `.so` files are loaded, when, and why).
4. Runtime launcher command composition (Wine binary path, env vars, Box64 env vars, display env).
5. Data storage and state transitions (Room schema, DataStore keys, in-memory state in `SteamService`).

### 0.3 SteamService Decomposition Audit

`SteamService.kt` at 167 KB is the single most critical and complex file. Before any migration, decompose it into logical sections:

1. Connection management (login, reconnect, session tokens).
2. Library and license management (owned games, depot metadata).
3. Download orchestration (chunk fetching, depot decryption, file assembly).
4. Cloud save sync (UFS upload/download — already partially extracted to `SteamAutoCloud.kt`).
5. Game launch preparation (launch info resolution, argument construction).
6. Friends and social features.
7. Achievement and stats handling.

Each section becomes a candidate for a dedicated Linux service class.

### 0.4 Runtime Command Trace

For at least three game launch scenarios, capture full command construction and environment variables:

1. Steam game launch (standard).
2. Custom executable launch.
3. Game requiring pre-install fix steps (use an existing `gamefixes/STEAM_*.kt` entry as the trace target).

Output includes effective environment variables, Wine binary path, Box64 env vars, display env (`DISPLAY`, `WAYLAND_DISPLAY`), executable path and args, and expected process tree.

### 0.5 Define Linux P0 Parity Baseline

Lock the P0 scope:

- Steam only (GOG/Epic/Amazon behind feature flags).
- Single runtime path: Wine + Box64 on ARM64.
- Vulkan-first GPU stack.
- Fedora ARM64 as the only officially supported environment initially.

**Deliverables of Phase 0:**

1. Architecture map document (`docs/architecture/overview.md`).
2. Feature reuse matrix spreadsheet or markdown table.
3. `SteamService.kt` decomposition audit document.
4. Launch trace pack (three scenarios).
5. P0 Linux parity definition document.

---

## Phase 1: New Linux Workspace and Core Module Scaffolding

**Goal:** create a clean Linux-native project that enables selective reuse from the existing codebase.

### 1.1 Repository Strategy

Use a monorepo subdirectory or sibling repository named `gamenative-linux`. Keep Android and Linux code separated to prevent coupling. The Android app stays in the existing `app/` module; the Linux project lives alongside it.

Recommended module layout:

```
gamenative-linux/
├── apps/
│   └── desktop/           # Compose Desktop UI shell
├── core/
│   ├── domain/            # Shared data models (migrated from data/, enums/, events/)
│   ├── runtime/           # RuntimeOrchestrator, ProcessRunner, Wine/Box64 integration
│   ├── gamefixes/         # Direct copy of gamefixes/ from Android (zero changes)
│   ├── store-steam/       # Steam auth, library sync, download (decomposed from SteamService)
│   ├── store-gog/         # GOG integration
│   ├── store-epic/        # Epic integration
│   └── store-amazon/      # Amazon integration
├── infra/
│   ├── config/            # Linux config (replaces PrefManager/DataStore)
│   ├── logging/           # SLF4J + Logback setup (replaces Timber)
│   ├── persistence/       # SQLDelight or JDBC database (replaces Room)
│   ├── keyring/           # libsecret credential store (replaces Android Keystore)
│   └── network/           # Network state monitor (replaces NetworkMonitor)
├── packaging/
│   ├── fedora/            # RPM spec, desktop entry, icon assets
│   └── flatpak/           # Optional Flatpak manifest
├── scripts/               # Build, packaging, release scripts
└── docs/
    ├── architecture/
    ├── runtime/
    └── release/
```

### 1.2 Language and Framework Decisions

The team is already Kotlin/Compose-fluent. Maximize code reuse velocity:

| Concern | Android (current) | Linux (target) | Rationale |
|---|---|---|---|
| Language | Kotlin | Kotlin | No change — full team expertise reuse |
| UI framework | Jetpack Compose (Android) | Compose Multiplatform Desktop | Same paradigm, different target |
| DI | Hilt (Android-specific) | Koin | Koin is pure Kotlin, multiplatform-ready |
| Database | Room (Android) | SQLDelight | Generates JVM+native interfaces, compatible schema |
| Preferences | DataStore | HOCON / Konf | JVM-native file-based config |
| HTTP | OkHttp | OkHttp (unchanged) | JVM-portable |
| Protobuf | protobuf-java | protobuf-java (unchanged) | No change |
| Serialization | kotlinx.serialization | kotlinx.serialization (unchanged) | No change |
| Crypto | Spongycastle | BouncyCastle JVM | Drop-in compatible API |
| Logging | Timber + Android Log | SLF4J + Logback | Standard JVM logging |
| Coroutines | kotlinx.coroutines | kotlinx.coroutines (unchanged) | No change |
| Steam library | JavaSteam + depotdownloader | JavaSteam + depotdownloader (unchanged) | Pure JVM, no Android deps |
| Analytics | PostHog Android SDK | PostHog Java/JVM SDK | Same backend, different SDK variant |

### 1.3 Core Interfaces First

Define host-agnostic interfaces before any implementation to prevent re-encoding Android assumptions:

```kotlin
// core/runtime
interface RuntimeOrchestrator
interface ProcessRunner
interface EnvironmentComposer
interface ProfileRepository
interface CapabilityDetector   // detects Box64, FEX, Wine, Vulkan at runtime

// core/store-steam
interface SteamSessionManager
interface SteamLibraryService
interface SteamDownloadManager
interface SteamCloudSaveService

// core/domain
interface GameLibraryService
interface LaunchPlanService
interface GameFixApplicator    // wraps GameFixesRegistry

// infra/*
interface ConfigService        // replaces PrefManager
interface CredentialStore      // replaces Android Keystore
interface NetworkStateService
interface NotificationService
interface PersistenceService
```

### 1.4 Host Abstraction Layer

Linux-native implementations of host services:

1. `LinuxFilesystemService` — XDG base dirs (`~/.config/gamenative`, `~/.local/share/gamenative`, `~/.local/state/gamenative/logs`).
2. `LinuxNetworkStateService` — poll `/proc/net` or use NetworkInterface JVM API.
3. `LinuxNotificationService` — emit D-Bus notifications via libnotify or `notify-send`.
4. `LinuxCredentialStore` — libsecret via JNA binding; fallback to AES-encrypted file.
5. `LinuxProcessSignalService` — `ProcessHandle` JVM API for SIGTERM/SIGKILL/SIGSTOP.

**Deliverables of Phase 1:**

1. New project skeleton compiles on Fedora ARM64 JVM 17.
2. All core interfaces defined with empty stub implementations.
3. `docs/dev/onboarding.md` — local run command and developer setup.
4. CI build job runs lint and compiles the skeleton on ARM64.

---

## Phase 2: Domain Extraction and Shared Logic Migration

**Goal:** migrate reusable logic from the Android app into host-independent modules.

### 2.1 Zero-Change Direct Migration

The following directories/files have no Android imports and can be copied verbatim into `core/domain/` and `core/gamefixes/`:

- `data/` — all 35 data model files (SteamApp, DownloadInfo, EpicGame, GOGGame, AmazonGame, LibraryItem, TouchGestureConfig, etc.)
- `enums/` — all enumerations
- `events/` — all event models
- `gamefixes/` — all 24 game fix files + `GameFixesRegistry.kt`, `GameFix.kt`, `LaunchArgFix.kt`, `RegistryKeyFix.kt`
- `CrashHandler.kt`, `Crypto.kt`, `Constants.kt`, `ReleaseTree.kt`
- `service/AchievementWatcher.kt`

After copy, run a `grep -r "android\." core/` check. Any Android import is a bug. Fix before proceeding.

### 2.2 SteamService Decomposition (Critical Path)

`SteamService.kt` cannot be moved as-is. It must be decomposed. Extract in this order:

1. **`SteamSessionManager`** — connection, login, reconnect, session token management. Depends only on JavaSteam. Extract first; this is the foundation everything else depends on.
2. **`SteamLibraryService`** — owned games, license queries, depot metadata, app info. Depends on `SteamSessionManager`.
3. **`SteamDownloadManager`** — chunk download, depot decryption, file assembly. Previously split partially into `DownloadService.kt`. Unify and make host-agnostic.
4. **`SteamCloudSaveService`** — port `SteamAutoCloud.kt` logic with Android IO replaced by JVM IO.
5. **`SteamFriendsService`** — friends, messages, social state.
6. **`SteamAchievementService`** — build on the already-clean `AchievementWatcher.kt`.

For each extracted class:
- Replace Android imports with injected interfaces.
- Remove `Context` parameters entirely.
- Replace `AndroidSchedulers` / Android coroutine dispatchers with standard `Dispatchers.IO` / `Dispatchers.Default`.
- Add unit tests proving behavior parity against golden fixtures.

### 2.3 PrefManager Migration

`PrefManager.kt` at 43 KB is essentially the entire settings schema. Its content is valuable; its implementation (DataStore) must be replaced.

Strategy:
1. Extract all preference keys and types into a typed `AppConfig` data model.
2. Implement `ConfigService` backed by HOCON or a simple JSON file under `~/.config/gamenative/config.json`.
3. Map all existing DataStore preference keys to the new config schema, preserving names where possible to assist any future migration tooling.
4. Write a migration helper that can import a DataStore backup (optional but useful for beta users migrating from Android).

### 2.4 Database Migration

Room schema lives in `app/schemas/`. Strategy:

1. Export current Room schema to inspect table structure.
2. Define equivalent SQLDelight `.sq` files reproducing the same schema.
3. Write explicit migration scripts for schema evolution.
4. Add a `DbMigrationValidator` integration test that applies all migrations on a fresh database.
5. Optional: add an import tool for Android Room SQLite `.db` files (useful for beta users).

### 2.5 Auth and Credential Design

On Linux, avoid plain-file token storage.

1. Implement `LinuxCredentialStore` using libsecret (via JNA) as the primary backend.
2. Fall back to AES-256-GCM encrypted JSON file (using existing `Crypto.kt` logic) if libsecret is unavailable (e.g., headless or non-GNOME environments).
3. Standardize token lifecycle and refresh behavior per store: Steam session token, GOG OAuth token, Epic OAuth token, Amazon credentials.
4. Never write raw tokens to log files — enforce at the logging layer.

**Deliverables of Phase 2:**

1. `core/domain/` — all data models, enums, events, game fixes migrated with zero Android imports.
2. `core/store-steam/` — `SteamSessionManager`, `SteamLibraryService`, `SteamDownloadManager` with unit tests.
3. `infra/config/` — `ConfigService` implementation with full settings schema.
4. `infra/persistence/` — SQLDelight schema, migrations, and `DbMigrationValidator` test.
5. `infra/keyring/` — `LinuxCredentialStore` with libsecret + file fallback.

---

## Phase 3: Linux Runtime Host and Process Orchestration

**Goal:** replace Android runtime launching with Linux-native orchestration. This is the most technically novel phase.

### 3.1 Runtime Pipeline

The `RuntimeOrchestrator` executes this pipeline sequentially:

```
1. Load runtime profile (from ConfigService / ProfileRepository)
2. Resolve game executable target (from LaunchInfo, SteamApp data)
3. Apply game fixes (GameFixesRegistry lookup by store + game ID)
4. Compose environment variables (Wine env, Box64 env, display env, per-game overrides)
5. Validate runtime binaries (wine, box64/fex, Vulkan layer, required libs)
6. Build full launch command
7. Launch process tree via ProcessRunner
8. Monitor lifecycle, capture stdout/stderr, track exit status
9. Persist run session metadata (start time, exit code, crash marker if applicable)
```

### 3.2 Container Model Adaptation

The Android project uses an `imagefs`-style container with profile semantics. Preserve the conceptual model, adapt storage paths:

| Android path model | Linux equivalent |
|---|---|
| App-private storage root | `~/.local/share/gamenative/` |
| Container runtime root | `~/.local/share/gamenative/containers/<profile-id>/` |
| Runtime profile configs | `~/.config/gamenative/profiles/` |
| Session logs | `~/.local/state/gamenative/logs/` |
| Crash markers | `~/.local/state/gamenative/crashes/` |
| Game install root | `~/.local/share/gamenative/games/` (or user-configurable) |

Optional: add `bwrap` (Bubblewrap) sandbox mode in a later iteration. Do not block P0 on sandboxing.

### 3.3 Wine + Box64 Integration

**P0 runtime path: Wine + Box64 on Fedora ARM64.**

Environment composition for a standard Wine + Box64 launch:

```bash
BOX64_LOG=0
BOX64_DYNAREC=1
BOX64_PREFER_EMULATED_FLAGS=1
WINE_PREFIX=~/.local/share/gamenative/containers/<profile>/pfx
WINEDEBUG=-all
DISPLAY=:0          # or WAYLAND_DISPLAY if XWayland
```

Tasks:

1. Implement `CapabilityDetector` — detect installed `wine`, `box64`, `fex-emu`, `vulkaninfo`, GPU vendor and Vulkan support at startup.
2. Implement `EnvironmentComposer` — translate a runtime profile into the exact environment variable set for the process.
3. Implement preset packs — equivalent to the Android container presets (Performance, Compatibility, etc.).
4. Port compatibility overrides per game ID — these are already partially encoded in `gamefixes/` but may need env var additions.

**Optional P1 runtime path: Wine + FEX-Emu.**

Add FEX as a selectable runtime backend behind a profile toggle. Do not block Box64 path on FEX readiness.

### 3.4 Display and Input Strategy

The Android `externaldisplay/` module implements a custom in-app X server bridge. This entire approach is replaced on Linux:

1. Use the host X11/Wayland session directly. Wine already handles display selection via `DISPLAY` / `WAYLAND_DISPLAY`.
2. XWayland is the recommended compositor path on Fedora ARM64 Wayland sessions.
3. Do not rewrite an X server. The host display stack is sufficient for P0.
4. For controller input: use the system evdev/udev stack. Wine's built-in gamepad support or `xboxdrv`/`antimicrox` mapping handles most cases.
5. A controller configuration UI can be added as a P1 feature in Phase 4.

### 3.5 Process Management

Implement robust process supervision in `ProcessRunner`:

1. Track parent and child PIDs via `ProcessHandle.descendants()`.
2. Graceful stop: `SIGTERM` → 5-second wait → `SIGKILL`.
3. Session-level log streams: redirect Wine/Box64 stdout/stderr to rolling log files under XDG state dir.
4. Crash marker generation: on abnormal exit code, write a crash bundle (exit code, last 100 log lines, environment snapshot).
5. Pause/resume via `SIGSTOP`/`SIGCONT` — expose to UI as optional feature.

**Deliverables of Phase 3:**

1. Working `RuntimeOrchestrator` — launches a test Windows executable (e.g., `notepad.exe`) via Wine + Box64 on Fedora ARM64.
2. Steam game launch prototype — full end-to-end for one test game.
3. Session logs captured and readable.
4. `CapabilityDetector` report visible in the UI diagnostics panel.

---

## Phase 4: Desktop UI and User Workflows

**Goal:** deliver a usable Linux desktop launcher for ARM64 users.

### 4.1 Technology: Compose Multiplatform Desktop

Use Compose Multiplatform Desktop as the UI framework. The existing Android Compose knowledge transfers almost entirely. Key differences:

- `Activity` → `application {}` / `Window {}` Compose Desktop entry point.
- `NavController` → Compose Desktop navigation (Decompose or Voyager recommended).
- `ViewModel` + Hilt → `ViewModel` + Koin injection.
- `LaunchedEffect` / `collectAsState` patterns are identical.
- Image loading: swap Coil/Landscapist for `AsyncImage` with Coil3 multiplatform or `loadImageBitmap`.

### 4.2 Minimum P0 Screen Set

Required screens for first release:

| Screen | Key Actions | Source Reference |
|---|---|---|
| First-run setup | Runtime detection, diagnostics, Wine/Box64 check | `CapabilityDetector` results |
| Account sign-in | Steam login (username/password + 2FA / QR) | `SteamSessionManager` |
| Library list | Browse installed and available games, filter/search | `SteamLibraryService`, `GameLibraryService` |
| Game detail | Info, screenshots, metadata, install/launch button | `SteamApp` model |
| Download/install status | Progress bar, speed, pause/cancel | `SteamDownloadManager` |
| Profile editor | Runtime profile settings, Wine prefix, env var overrides | `ProfileRepository`, `ConfigService` |
| Run session monitor | Live log stream, process status, stop button | `ProcessRunner` session output |
| Diagnostics panel | Capability report, launch command preview, fix suggestions | `CapabilityDetector` |
| Settings | All app-level preferences (migrated from `PrefManager`) | `ConfigService` |

### 4.3 State Architecture

```
AppState (Koin singleton)
├── SessionState (auth, current user)
├── LibraryState (game list, metadata cache)
├── DownloadState (active downloads, queue)
├── RuntimeState (active sessions, process monitors)
└── ConfigState (preferences, profiles)
```

Each screen has a dedicated `ViewModel` observing the relevant state slice. Side effects (process launch, downloads, Steam connection) are triggered by action events and executed in coroutine scopes owned by the relevant service.

### 4.4 Background Task Architecture

On Linux there is no Android Service lifecycle. Replace with:

1. App-owned `CoroutineScope` attached to the application lifetime.
2. `TaskScheduler` — manages a persistent queue of long-running tasks (downloads, sync) with state persistence.
3. On app restart, `TaskScheduler` resumes any incomplete queue entries.
4. Optionally: a separate headless daemon process for download-only background operation (P2 feature).

**Deliverables of Phase 4:**

1. Runnable desktop launcher on Fedora ARM64 with all P0 screens.
2. Steam auth → library → install → launch end-to-end working.
3. Profile editor and diagnostics panel functional.
4. Session log export to file.

### 4.5 Frontend 1:1 Android Replica Plan (New)

**Goal:** make Linux desktop UI match Android UI/UX with high-fidelity parity, with explicit acceptance gates per screen and interaction.

#### 4.5.1 Parity Scope Definition (Week 1)

Build a parity baseline before implementation:

1. Create Android UI inventory for every P0/P1 screen (`ui/screen/*`, `ui/component/*`, `ui/theme/*`):
  - layout hierarchy
  - spacing values and breakpoints
  - typography scale and font weights
  - color/alpha/elevation tokens
  - iconography and artwork treatment
2. Capture a reference screenshot pack from Android for each state:
  - loading
  - empty
  - error
  - populated
  - destructive/confirmation dialogs
3. Define parity budget:
  - visual variance target: <= 2px spacing drift for core layouts
  - typography variance target: same size/weight/line-height or documented fallback
  - interaction variance target: no missing primary actions or regressions in flow order

**Deliverable:** `docs/architecture/frontend-parity-baseline.md` + versioned screenshot set under `docs/architecture/frontend-parity/`.

#### 4.5.2 Design Token and Theme Parity (Week 1–2)

1. Extract Android design tokens into a shared contract used by desktop shell:
  - color roles
  - spacing scale
  - radius/elevation
  - typography styles
2. Implement a desktop token adapter so Compose Desktop renders the same semantic theme values.
3. Add strict token lint checks (no hardcoded ad-hoc colors/spacing in new screen work).

**Deliverable:** shared token map + desktop theme layer with snapshot tests for token resolution.

#### 4.5.3 Component-Level Replica Track (Week 2–4)

Rebuild/align reusable components before screen rewrites:

1. Top bars, navigation rails/tabs, cards, list rows, buttons, chips, badges.
2. Input controls and dialogs (text fields, toggles, selectors, modals, confirmations).
3. Status/progress components (download rows, progress indicators, banners, toasts).
4. Empty/error/loading states with Android-equivalent content hierarchy.

For each component, define:

- Android reference
- desktop implementation
- screenshot diff result
- interaction checklist (hover/focus/pressed/disabled)

**Deliverable:** parity component matrix in `docs/architecture/feature-reuse-matrix.md` (new desktop parity columns).

#### 4.5.4 Screen-by-Screen Parity Sprints (Week 4–8)

Implement in this order to maximize user-visible impact:

1. Library list + filtering/search flows.
2. Game detail (hero/info/actions/media metadata layout).
3. Downloads/install queue and progress UX.
4. Account/auth flows (including 2FA states).
5. Diagnostics, profiles, settings, and session monitor.

Each screen cannot exit sprint unless all are true:

1. Android reference states fully covered.
2. Keyboard/controller navigation path defined and tested.
3. Screenshot diff gate passes on required states.
4. Interaction and copy parity checklist signed off.

#### 4.5.5 Motion and Micro-Interaction Parity (Week 6–8, overlapping)

1. Match transition timing/easing for navigation, dialogs, and list updates.
2. Match loading shimmer/skeleton behavior where present.
3. Ensure focus rings and accessibility affordances are consistent on desktop inputs.

**Deliverable:** motion spec appendix with measured durations and easing curves.

#### 4.5.6 Validation Gates and Definition of Done

Parity is considered complete only when all conditions pass:

1. Every P0 screen has a side-by-side Android vs Linux parity checklist with explicit pass/fail.
2. Screenshot regression pipeline runs in CI for desktop shell and blocks on unapproved drift.
3. UX walkthrough for primary flows shows no crude/placeholder UI elements.
4. At least one external reviewer signs off that Linux feels materially identical to Android for core workflows.

**Definition of Done (Frontend Parity):**

- No major visual mismatch in primary workflows (library, game detail, downloads, auth).
- No missing primary actions compared to Android.
- No legacy prototype-style components left in P0 screens.
- Parity documentation and screenshot references are current and versioned.

### 4.6 Frontend Parity Execution Board (Actionable)

Use this as the day-to-day implementation tracker for the 1:1 replica effort.

#### 4.6.1 Epics and Owners

| Epic | Scope | Primary Paths | Owner | Exit Criteria |
|---|---|---|---|---|
| E1: Android UI Baseline Capture | Source-of-truth extraction for screens/components/states | `app/src/main/java/app/gamenative/ui/screen/**`, `app/src/main/java/app/gamenative/ui/component/**`, `app/src/main/java/app/gamenative/ui/theme/**`, `docs/architecture/frontend-parity/**` | UI Platform | Baseline doc + screenshot pack complete for all P0 screens/states |
| E2: Token + Theme Parity | Shared semantic token map and desktop theme application | `desktop/shell/src/main/kotlin/**/theme/**`, `docs/architecture/frontend-parity-baseline.md` | UI Platform | Token diff checklist is green; no hardcoded core tokens in P0 screens |
| E3: Component Replica Library | Rebuild shared primitives to Android-equivalent visuals/interactions | `desktop/shell/src/main/kotlin/**/component/**` | UI Systems | Component matrix complete with screenshot diff and interaction checklist |
| E4: Screen Parity Delivery | Apply parity components/theme screen-by-screen for P0 flows | `desktop/shell/src/main/kotlin/**/screen/**`, `desktop/shell/src/main/kotlin/**/navigation/**` | Feature Pods | All P0 screens pass parity checklist and screenshot gate |
| E5: Motion + Interaction Polish | Timing/easing/focus/navigation parity | `desktop/shell/src/main/kotlin/**/screen/**`, `desktop/shell/src/main/kotlin/**/component/**` | UX Engineering | Motion appendix finalized; no placeholder transitions in P0 |
| E6: CI Parity Gates | Screenshot regression + approval workflow | `.github/workflows/**`, `desktop/shell` test fixtures | DevEx/CI | CI blocks unapproved parity drift on protected branches |

#### 4.6.2 Week-by-Week Plan (8 Weeks)

| Week | Focus | Concrete Outputs |
|---|---|---|
| W1 | Baseline inventory + screenshot harness | `frontend-parity-baseline.md`, initial screenshot corpus, state inventory table |
| W2 | Token/theme parity | Desktop token adapter, typography parity table, token lint rule draft |
| W3 | Core components batch A | top bars/nav/cards/buttons/list rows + diffs |
| W4 | Core components batch B | dialogs/forms/progress/empty-error states + diffs |
| W5 | Screen parity sprint A | library + game detail parity checklists green |
| W6 | Screen parity sprint B | downloads + account/auth parity checklists green |
| W7 | Screen parity sprint C + motion | diagnostics/profiles/settings/session monitor + motion appendix |
| W8 | Gate hardening + review | CI screenshot gate blocking, external parity review sign-off |

#### 4.6.3 Story Template (Per Screen)

Every screen implementation story must include:

1. Android reference links (code paths + screenshots).
2. Linux target paths and modified component list.
3. State coverage checklist: loading, empty, error, populated, modal.
4. Input parity checklist: mouse, keyboard, controller focus order.
5. Screenshot diff artifacts attached before merge.

#### 4.6.4 Required File/Module Mapping for Initial Tickets

Use this mapping to generate the first issue batch.

| Ticket Group | Android Source | Linux Target |
|---|---|---|
| Theme/token extraction | `app/src/main/java/app/gamenative/ui/theme/**` | `desktop/shell/src/main/kotlin/**/theme/**` |
| Navigation shell parity | `app/src/main/java/app/gamenative/ui/PluviaMain.kt` | `desktop/shell/src/main/kotlin/**/navigation/**` |
| Library screen parity | `app/src/main/java/app/gamenative/ui/screen/**Library*` | `desktop/shell/src/main/kotlin/**/screen/**Library*` |
| Game detail parity | `app/src/main/java/app/gamenative/ui/screen/**GameDetail*` | `desktop/shell/src/main/kotlin/**/screen/**GameDetail*` |
| Download queue parity | `app/src/main/java/app/gamenative/ui/screen/**Download*` | `desktop/shell/src/main/kotlin/**/screen/**Download*` |
| Account/auth parity | `app/src/main/java/app/gamenative/ui/screen/**Account*`, `**Login*` | `desktop/shell/src/main/kotlin/**/screen/**Account*`, `**Login*` |
| Shared component parity | `app/src/main/java/app/gamenative/ui/component/**` | `desktop/shell/src/main/kotlin/**/component/**` |

#### 4.6.5 Tracking Checklist

Maintain this checklist in PRs and weekly review notes:

- [ ] Baseline assets updated when Android UI changes.
- [ ] New/updated desktop component has Android reference and parity notes.
- [ ] No unresolved visual drift in approved screenshot diff for touched states.
- [ ] No crude/placeholder styles remain in touched P0 workflow.
- [ ] Accessibility/focus behavior validated for touched screens.
- [ ] Reviewer confirms parity acceptance criteria for merged work.

#### 4.6.6 Release Gate for Declaring “Android-Like Frontend”

Do not market frontend parity as complete until all conditions are true:

1. E1–E6 exit criteria are complete.
2. All P0 screens have signed parity checklists stored under `docs/architecture/frontend-parity/`.
3. Screenshot regression gate is mandatory in mainline CI.
4. Product/UX review signs off “no crude UI” for core flows.

---

## Phase 5: Packaging, CI/CD, and Fedora ARM64 Binary Release

**Goal:** produce reproducible Fedora ARM64 binaries and an automated release process.

### 5.1 Build Targets

| Artifact | Priority | Format |
|---|---|---|
| Portable tarball | P0 | `gamenative-linux-<version>-aarch64.tar.gz` |
| Fedora RPM | P0 | `gamenative-<version>-1.fc<N>.aarch64.rpm` |
| Flatpak | P1 | `app.gamenative.GameNative.flatpak` |
| COPR repository | P1 | For automatic Fedora updates |

### 5.2 Build Toolchain

- JDK 17 (GraalVM or Temurin) for ARM64 native compilation.
- Gradle wrapper (already present in repo: `gradlew`).
- CI runners: native ARM64 runners preferred; QEMU-based cross-build as fallback.
- GitHub Actions or equivalent CI.

Required build outputs per release:

1. Application binary / JAR / native image.
2. Runtime dependency manifest (all required system packages).
3. SHA256 checksums for all artifacts.
4. SBOM (CycloneDX or SPDX format).
5. GPG-signed artifacts (when signing infrastructure is available).

### 5.3 Fedora Packaging Structure (`packaging/fedora/`)

```
packaging/fedora/
├── gamenative.spec          # RPM spec file
├── gamenative.desktop       # Desktop entry (XDG)
├── gamenative.appdata.xml   # AppStream metadata
├── icons/
│   ├── 256x256/app.gamenative.GameNative.png
│   └── scalable/app.gamenative.GameNative.svg
├── deps.txt                 # Required system packages (wine, box64, mesa-vulkan-drivers, etc.)
└── post-install.sh          # Optional post-install notes / capability check
```

RPM spec must declare these runtime dependencies at minimum:

- `wine` or `wine-staging`
- `box64` (may need COPR or custom RPM initially)
- `mesa-vulkan-drivers`
- `libsecret`
- `java-17-openjdk-headless` (if not bundled as native image)

### 5.4 CI Pipeline Stages

```yaml
stages:
  - lint            # ktlint, detekt
  - test            # unit tests (core/domain, core/store-steam, infra/*)
  - build           # Gradle build for aarch64
  - package         # RPM + tarball assembly
  - integration     # Launch a test executable end-to-end on Fedora ARM64 runner
  - sign            # GPG sign artifacts
  - publish         # Upload to COPR / GitHub Releases
```

### 5.5 Compatibility Test Matrix

| Test Axis | Options |
|---|---|
| Fedora versions | Fedora 40, 41 (current support window) |
| Display servers | X11, Wayland (XWayland) |
| GPU stacks | Mesa/Vulkan (primary), Mesa/OpenGL fallback |
| Runtime backends | Box64 (P0), FEX-Emu (P1) |
| Wine variants | Wine upstream, wine-staging |

**Deliverables of Phase 5:**

1. Installable Fedora ARM64 RPM on a clean machine.
2. CI-generated release candidate with checksums and SBOM.
3. Release checklist document and rollback instructions.

---

## Phase 6: Stabilization and Production Hardening

**Goal:** move from prototype quality to reliable, maintainable product.

### 6.1 Reliability

1. Add `WatchdogService` for stuck launch detection (Wine process unresponsive → auto-kill + report).
2. Improve retry logic in `SteamDownloadManager` (network interruptions, partial chunk restarts).
3. Harden `DbMigrationValidator` for rollback path (corrupt database → backup + re-create).
4. Add cache consistency checks on startup (corrupt install manifest detection).
5. Validate Wine prefix integrity before each launch.

### 6.2 Performance

1. Startup profiling — trace from `main()` to first frame rendered; target under 3 seconds.
2. Library list rendering optimization — virtual/lazy list with proper key-based diffing.
3. Download throughput review — compare `SteamDownloadManager` chunk parallelism against Linux I/O limits.
4. Launch latency baseline — measure time from "Launch" button to Wine process receiving first frame.

### 6.3 Security

1. Full credential handling audit — ensure `CredentialStore` never emits tokens to logs.
2. Dependency vulnerability scan — integrate `trivy` or `grype` into CI.
3. Safe handling of game install paths — reject path traversal in user-supplied install directories.
4. Signed update metadata — if an auto-updater is added, sign update manifests.

### 6.4 Supportability

1. Anonymized diagnostics report — generate a redacted bundle (no tokens, no account IDs) for bug reports.
2. Reproducible bug bundle export — include last session log, capability report, config snapshot (redacted), crash marker.
3. Strict log redaction policy — implement a `RedactingLogFilter` in the Logback config to strip token patterns and Steam IDs from log output.

**Deliverables of Phase 6:**

1. Production readiness report (reliability, performance, security review results).
2. Release candidate v1.0 for Linux ARM64.
3. Documented support playbook and known limitations file.

---

## Detailed Work Breakdown by Team Function

### Platform Runtime Team

Responsibilities:
- `RuntimeOrchestrator`, `ProcessRunner`, `EnvironmentComposer` implementation.
- `CapabilityDetector` — Wine, Box64, FEX, Vulkan detection.
- Process supervision, signal handling, crash marker generation.
- Runtime compatibility presets and per-game env var overrides.
- `externaldisplay/` replacement with host-native display strategy.

Milestones:
- **M1:** Test Windows executable launches via Wine + Box64.
- **M2:** Steam game launches end-to-end with one runtime path.
- **M3:** Profile variants, preset packs, and per-game compatibility overrides working.

### Domain and Store Integrations Team

Responsibilities:
- `SteamService.kt` decomposition into clean service classes.
- `SteamSessionManager`, `SteamLibraryService`, `SteamDownloadManager`, `SteamCloudSaveService`.
- Data model migration (all 35 `data/` files).
- GOG, Epic, Amazon service ports (P1/P2).
- `GameFixesRegistry` migration and validation.

Milestones:
- **M1:** Steam auth, library fetch, and at least one game download working.
- **M2:** Install metadata, download queue, and status sync.
- **M3:** Cloud saves (UFS) and optional additional stores.

### Desktop UX Team

Responsibilities:
- Compose Multiplatform Desktop application shell.
- All P0 workflow screens (library, account, install, session monitor, settings).
- State architecture (`AppState`, per-screen ViewModels, Koin injection).
- Diagnostics panel and error visibility UX.

Milestones:
- **M1:** App shell scaffolded, navigation working, all screens as stubs.
- **M2:** Account sign-in and library list functional.
- **M3:** Install, launch, session monitor, and settings all working.

### DevOps and Release Team

Responsibilities:
- CI/CD pipeline setup (lint → test → build → package → sign → publish).
- ARM64 CI runner provisioning (native or QEMU-based).
- RPM spec, Flatpak manifest, desktop entry, AppStream metadata.
- SBOM generation, artifact signing, COPR setup.

Milestones:
- **M1:** CI runs lint and unit tests on every PR.
- **M2:** CI produces unsigned tarball artifact from main branch.
- **M3:** CI produces signed RPM and publishes release candidate to GitHub Releases.

---

## Proposed Timeline

Realistic timeline for a small focused team (3–5 engineers):

| Phase | Activities | Weeks |
|---|---|---|
| Phase 0 | Discovery, decomposition audit, runtime trace | 1–3 |
| Phase 1 | Repository scaffolding, interfaces, host abstractions | 4–6 |
| Phase 2 | Domain migration, SteamService decomposition, DB/config migration | 7–13 |
| Phase 3 | Runtime orchestration, Wine+Box64 integration, process management | 10–16 (overlaps Phase 2) |
| Phase 4 | Desktop UI, all P0 screens, state architecture | 14–20 |
| Phase 4A | Frontend 1:1 Android parity (tokens, components, screens, motion, diff gates) | 20–28 |
| Phase 5 | Packaging, CI/CD, RPM, release pipeline | 18–22 |
| Phase 6 | Stabilization, security, performance, v1.0 RC | 23–28 |

> If team size is smaller than 3 engineers, extend the timeline by 40–60%. The `SteamService.kt` decomposition in Phase 2 is the highest-risk item and should receive dedicated engineering time to avoid it blocking downstream phases.

---

## Risks and Mitigations

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| `SteamService.kt` decomposition more complex than expected | High | High | Timebox audit in Phase 0; treat decomposition as its own milestone, not a side task. |
| Android assumptions leak into Linux modules | Medium | High | Enforce `grep -r "android\."` check in CI on all `core/` and `infra/` modules. Block merge on any Android import. |
| Box64 compatibility gaps for specific games | Medium | Medium | Lock P0 compatibility matrix to known-working titles; document unsupported games explicitly. |
| Fedora ARM64 hardware variability | Medium | Medium | Define support baseline hardware profile. `CapabilityDetector` exposes unsupported-state UX clearly. |
| GOG/Epic/Amazon API changes during development | Low–Medium | Medium | Keep non-Steam stores behind feature flags. Prioritize Steam isolation to protect core schedule. |
| Build reproducibility and release signing delays | Low | Medium | Assign release engineering by end of Phase 1, not at Phase 5. Build SBOM/signing flow early. |
| JavaSteam library breaking changes or incompatibility | Low | High | Pin JavaSteam version. Add integration test for Steam login flow that runs in CI against a test account. |

---

## Validation and Testing Strategy

### Test Layers

| Layer | Scope | Tools |
|---|---|---|
| Unit tests | Domain logic, game fix transformations, config serialization, credential store | JUnit 5 + MockK |
| Integration tests | SteamService decomposed classes (with mock JavaSteam), DB migrations, process command generation | JUnit 5 + Testcontainers (for DB) |
| Golden/snapshot tests | Launch plan generation, env var sets for profile presets, command-line output for known game IDs | Custom golden test harness |
| End-to-end smoke tests | Full install and launch on Fedora ARM64 CI runner | Shell scripts + GameNative CLI (if headless mode added) |
| Manual test tracks | Account login, library sync, actual game install and launch | Manual QA on physical ARM64 hardware |

### Golden Test Targets

Use golden (snapshot) tests for deterministic outputs:

1. `GameFixesRegistry` — given game ID + store, output must match expected `LaunchArgFix` / `RegistryKeyFix` set.
2. `EnvironmentComposer` — given a runtime profile, output must match expected env var map.
3. `LaunchPlanService` — given a `SteamApp` entry and profile, output must match expected command line.

### Manual Test Tracks

1. Fresh install on clean Fedora ARM64 machine → first launch → diagnostics report.
2. Account login → Steam library sync → metadata loaded.
3. Game install → verify depot files on disk → launch game → play briefly → quit cleanly.
4. App shutdown and restart → download queue resumes.
5. Error handling with missing Wine or Box64 binary → clear error UX.

---

## Definition of Done — First Fedora ARM64 Release (v1.0)

Release is complete when all of the following are true:

1. Binary installs on a clean Fedora ARM64 machine from the RPM package.
2. First-run diagnostics report a sane system state (Wine, Box64, Vulkan detected).
3. User can sign in to Steam successfully.
4. At least one supported test game (from a predefined compatibility list) installs and launches.
5. Runtime profile can be edited and persisted.
6. Live session log is visible and exportable to a file.
7. CI artifacts are reproducible — two builds from the same commit produce identical checksums.
8. Known limitations are documented in `docs/release/known-limitations.md`.
9. No raw tokens or Steam account IDs appear in any log file.

---

## Immediate Next Steps (Start Now)

These actions begin immediately, before Phase 0 is formally complete:

1. **Create `docs/architecture/` directory** — start the feature reuse matrix and the `SteamService.kt` decomposition audit.
2. **Run `grep -r "import android" app/src/main/java/app/gamenative/data/`** — confirm zero Android imports in data models (should be true; verify before assuming).
3. **Bootstrap the monorepo `gamenative-linux/` subdirectory** — add `settings.gradle.kts` and an empty `core/domain` module that compiles with Kotlin JVM.
4. **Provision Fedora ARM64 test machine or VM** — required for runtime validation. Without this, Phase 3 cannot be verified.
5. **Lock the JavaSteam version** — pin to the exact version currently used in `build.gradle.kts` and document it in `docs/architecture/dependencies.md`.
6. **Assign release engineering ownership** — at least one engineer responsible for the CI/packaging pipeline from day one.

---

## Recommended Governance and Process

1. **Weekly architecture review** — strict scope control; any new Android dependency pattern found in Linux modules is a blocking issue.
2. **Biweekly milestone demos** — must include a runnable artifact starting from Phase 3 M1 onward.
3. **Change control for runtime behavior** — any change to env var composition or process launch logic goes through a documented RFC and must update golden tests.
4. **Compatibility notes updated continuously** — `docs/release/compatibility.md` tracks known-working and known-broken game IDs with runtime configuration notes.
5. **Feature flag discipline** — non-Steam stores, FEX backend, sandboxing (bwrap), and any non-P0 feature must be behind named feature flags from day one.

---

## Appendix A: Confirmed Technical Decisions

1. **Kotlin** remains the primary language. No language switch needed.
2. **Compose Multiplatform Desktop** for UI — same paradigm as existing Android Compose screens.
3. **Koin** replaces Hilt — pure Kotlin, compatible with multiplatform targets.
4. **SQLDelight** replaces Room — similar SQL-first approach, JVM-portable.
5. **JavaSteam + javasteam-depotdownloader** — reused unchanged; zero Android dependencies.
6. **Box64 as the P0 emulation backend** on ARM64. FEX is P1.
7. **XDG Base Directory Specification** for all file paths on Linux. No hardcoded home paths.
8. **libsecret** for credential storage with AES-encrypted file fallback.
9. **Fedora ARM64 only** for first release. Other distros are best-effort after v1.0.
10. **Feature flags** for all non-Steam stores and non-P0 features from day one.

---

## Appendix B: What Should Not Be Done Early

1. Do not attempt to support GOG/Epic/Amazon at parity with Steam before Steam is stable.
2. Do not add `bwrap` sandboxing before the baseline launch path is validated.
3. Do not attempt a custom X server implementation — use the host display stack.
4. Do not commit to multiple package ecosystems (RPM + Flatpak + Snap + AppImage) before one release path is proven reliable.
5. Do not migrate `PluviaMain.kt` (97 KB) as a single unit — decompose into individual screen files first, then port each screen to Compose Desktop.
6. Do not rewrite `GameFixesRegistry` — it is pure Kotlin and should be copied with zero changes.

---

## Appendix C: Reference File Sizes (as of current baseline)

The following file sizes indicate complexity and migration effort:

| File | Size | Migration complexity |
|---|---|---|
| `SteamService.kt` | 167 KB | Very high — decompose into 5–6 services |
| `ui/PluviaMain.kt` | 97 KB | High — port screen by screen |
| `PrefManager.kt` | 43 KB | Medium — extract schema, reimplement storage |
| `service/SteamAutoCloud.kt` | 44 KB | Medium — clean logic, replace IO calls |
| `data/DownloadInfo.kt` | 10 KB | Low — pure data model |
| `data/SteamApp.kt` | 9 KB | Low — pure data model |
| `gamefixes/GameFixesRegistry.kt` | 3 KB | None — direct copy |

---

## Final Notes

This plan is deliberately grounded in the actual GameNative Android codebase structure, not abstract architecture theory. The reuse classifications, file-level migration guidance, and technology replacement decisions are based on a direct audit of the source tree.

The most important strategic choices are:

1. **Decompose `SteamService.kt` early and carefully** — this is the critical path and the highest-risk item in the entire project.
2. **Copy the `gamefixes/` package without changes** — it is already platform-agnostic and represents significant compatibility work that should not be redone.
3. **Keep scope narrow for v1.0** — Steam-first, Box64-only, Fedora ARM64 only, one runtime path. Breadth can come later.

The operational mindset: build a **Linux-native launcher product** inspired by the existing logic, not an Android app transplanted to Linux. That distinction determines whether the Fedora ARM64 binary is maintainable, performant, and releasable.
