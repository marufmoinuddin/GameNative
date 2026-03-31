# SteamService Decomposition Audit (Phase 0 Draft)

## Source

- Primary file: `app/src/main/java/app/gamenative/service/SteamService.kt`
- Reported size baseline: 167 KB

## Decomposition Objective

Break Android service-centric logic into host-agnostic services with explicit interfaces and no Android imports.

## Proposed Extraction Order

1. `SteamSessionManager`
2. `SteamLibraryService`
3. `SteamDownloadManager`
4. `SteamCloudSaveService`
5. `SteamFriendsService`
6. `SteamAchievementService`

## Initial Boundaries

### 1. SteamSessionManager

- Responsibilities: login, reconnect, auth state, session lifecycle.
- Inputs: credentials, machine auth callbacks, connection events.
- Outputs: session state stream, token/state updates, connection errors.
- Must not depend on Android `Service`, `Context`, or notification APIs.

### 2. SteamLibraryService

- Responsibilities: owned app list, package/license metadata, app info caching.
- Dependencies: `SteamSessionManager` and persistence abstractions.
- Output: normalized library models for UI and install pipeline.

### 3. SteamDownloadManager

- Responsibilities: depot manifests, chunk download/decrypt/write, queue control.
- Dependencies: session, filesystem abstraction, task scheduler, persistence.
- Output: deterministic download/install state machine with resumable state.

### 4. SteamCloudSaveService

- Responsibilities: UFS sync planning, upload/download conflict policy.
- Input source: `SteamAutoCloud` logic migrated to portable I/O.
- Output: sync task set and conflict report.

### 5. SteamFriendsService

- Responsibilities: friend list, presence, message events, social cache.
- Output: social state stream consumed by UI.

### 6. SteamAchievementService

- Responsibilities: achievement polling and publish events.
- Input source: existing `AchievementWatcher.kt` logic.

## Anti-Patterns To Remove During Migration

- Android lifecycle coupling (`Service` callbacks as core workflow entry points).
- Direct `Context` access for file paths and settings.
- Implicit singletons without constructor-injected dependencies.
- UI-side assumptions embedded in service side effects.

## Short-Term Deliverables

1. Annotated section map of `SteamService.kt` with line ranges per responsibility.
2. Dependency graph with Android-bound and portable edges.
3. First extraction PR: `SteamSessionManager` + parity tests.
