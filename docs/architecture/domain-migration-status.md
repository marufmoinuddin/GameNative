# Domain Migration Status (Phase 2 Start)

## What Was Migrated

- Copied reusable source packages from Android into `gamenative-linux/core/domain` for extraction work.
- Current staged set includes:
- `data/`
- `enums/`
- `events/`
- `Constants.kt`

First promoted batch from staging into active `src/main`:

- `data/GOGCloudSavesLocation.kt`
- `data/LibraryAssetsInfo.kt`
- `data/LibraryCapsuleInfo.kt`
- `data/LibraryHeroInfo.kt`
- `data/LibraryLogoInfo.kt`
- `data/ManifestInfo.kt`
- `data/OwnedGames.kt`
- `data/UserFilesDownloadResult.kt`
- `data/UserFilesUploadResult.kt`
- `enums/Language.kt` (Timber dependency removed)

## Build-Safe Staging Approach

To keep `:core:domain` buildable while extraction is in progress:

- Active compiled sources: `gamenative-linux/core/domain/src/main/kotlin`
- Migration staging sources: `gamenative-linux/core/domain/src/staging/kotlin`

This allows incremental decoupling without breaking the module on every copied file.

## Portability Audit Results

Snapshot after staging:

- `staged_kotlin_files = 53`
- `android_import_hits = 0`
- `room_import_hits = 37`

Snapshot after first promotion batch:

- `staging_data_remaining = 26`
- `main_data_count = 9`
- `staging_enum_remaining = 13`
- `main_enum_count = 1`

Snapshot after second promotion batch:

- `staging_data_remaining = 23`
- `main_data_count = 12`
- `staging_enum_remaining = 7`
- `main_enum_count = 7`
- `staging_event_remaining = 1`
- `main_event_count = 2`

Snapshot after third promotion batch:

- `staging_data_remaining = 22`
- `main_data_count = 13`
- `staging_enum_remaining = 0`
- `main_enum_count = 14`
- `staging_event_remaining = 1`
- `main_event_count = 2`

Snapshot after fourth promotion batch:

- `staging_data_remaining = 19`
- `main_data_count = 16`
- `staging_event_remaining = 1`
- `main_serializer_count = 2`

Snapshot after fifth promotion wave:

- `staging_data_remaining = 0`
- `main_data_count = 35`
- `staging_enum_remaining = 0`
- `staging_event_remaining = 1`

Snapshot after staging elimination pass:

- `core/domain/src/staging/kotlin` file count: `0`
- all migrated domain/enums/events contracts are now in `src/main`
- full Linux module compile remains successful

Current staging snapshot:

- `core/domain/src/staging/kotlin/app/gamenative/events/AndroidEvent.kt`
- `core/domain/src/staging/kotlin/app/gamenative/Constants.kt`

Second promoted batch:

- `enums/AppTheme.kt`
- `enums/LoginResult.kt`
- `enums/LoginScreen.kt`
- `enums/Marker.kt`
- `enums/SaveLocation.kt`
- `enums/SyncResult.kt`
- `events/EventDispatcher.kt`
- `events/SteamEvent.kt`
- `data/SteamFriend.kt`
- `data/GameProcessInfo.kt`
- `data/PostSyncInfo.kt`

Third promoted batch:

- `enums/AppType.kt`
- `enums/ControllerSupport.kt`
- `enums/OSArch.kt`
- `enums/OS.kt`
- `enums/PathType.kt`
- `enums/ReleaseState.kt`
- `enums/SpecialGameSaveMapping.kt`
- `data/TouchGestureConfig.kt`

Fourth promoted batch:

- `data/LaunchInfo.kt`
- `data/BranchInfo.kt`
- `data/ConfigInfo.kt`
- `db/serializers/DateSerializer.kt`
- `db/serializers/OsEnumSetSerializer.kt`

Fifth promotion wave:

- Room-annotated entities and metadata models:
- `data/AmazonGame.kt`
- `data/AppInfo.kt`
- `data/CachedLicense.kt`
- `data/ChangeNumbers.kt`
- `data/DownloadingAppInfo.kt`
- `data/Emoticon.kt`
- `data/EncryptedAppTicket.kt`
- `data/EpicGame.kt`
- `data/FileChangeLists.kt`
- `data/FriendMessage.kt`
- `data/GOGGame.kt`
- `data/SteamLicense.kt`
- Final decoupled models:
- `data/UserFileInfo.kt`
- `data/SaveFilePattern.kt`
- `data/UFS.kt`
- `data/DownloadInfo.kt`
- `data/DepotInfo.kt`
- `data/LibraryItem.kt`
- `data/SteamApp.kt`
- Build dependency updates:
- `androidx.room:room-common`
- `kotlinx-coroutines-core`
- Added `google()` repository for AndroidX artifact resolution.

Interpretation:

- Android import gate is currently clean in staged sources.
- Room and app-internal dependency coupling is still significant and must be removed/refactored before moving files back into `src/main`.

## Deferred (Not Yet Portable) Items

The following Android-coupled files were removed from `core/domain/src/main` and deferred for dedicated migration tracks:

- `CrashHandler.kt`
- `Crypto.kt`
- `ReleaseTree.kt`
- `service/AchievementWatcher.kt`
- `gamefixes/` package

## Immediate Next Extraction Steps

1. Split `data/` models into portable DTOs vs persistence-annotated entities.
2. Replace Room annotations with SQLDelight schema + mapping layer.
3. Move dependency-light files from `src/staging` back into `src/main` in small batches.
4. Reintroduce `gamefixes/` behind a platform-agnostic `GameFixApplicator` interface.

## Validation

- Main source Android import gate: clean.
- `:core:domain:compileKotlin`: successful after first batch promotion.
- Main source Android import gate remains clean after second batch promotion.
- `:core:domain:compileKotlin` remains successful after second batch promotion.
- Main source Android import gate remains clean after third batch promotion.
- `:core:domain:compileKotlin` remains successful after third batch promotion.
- Main source Android import gate remains clean after fourth batch promotion.
- `:core:domain:compileKotlin` remains successful after fourth batch promotion.
- Main source Android import gate remains clean after fifth promotion wave.
- `:core:domain:compileKotlin` remains successful after fifth promotion wave.

## Remaining Staging Item

- No remaining staged files.

## Platform-Neutral Replacements Added

- `events/AppEvent.kt` replaces Android-only `AndroidEvent.kt` with host-neutral input and lifecycle event payloads.
- `CoreConstants.kt` replaces Android/Compose-bound constants with shared constants (`Ui` now uses primitive `WINDOW_WIDTH_LARGE_DP`).
