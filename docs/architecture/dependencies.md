# Linux Port Dependency Pins

## JavaSteam Pin (Locked)

Current baseline pin is defined in `gradle/libs.versions.toml`:

- `javasteam = "1.8.0.1-13-SNAPSHOT"`
- `javasteam-depotdownloader` uses the same version reference.

Resolved coordinates:

- `io.github.joshuatam:javasteam:1.8.0.1-13-SNAPSHOT`
- `io.github.joshuatam:javasteam-depotdownloader:1.8.0.1-13-SNAPSHOT`

## Build Usage Notes

- The Android app currently supports two paths:
- Local jar path for manual JavaSteam builds (`localBuild = true` in `app/build.gradle.kts`).
- Maven dependency path (`localBuild = false`, default).

## Linux Port Guidance

1. Keep JavaSteam pinned to the same snapshot while extracting `SteamSessionManager` to avoid mixed behavior during decomposition.
2. Treat JavaSteam upgrades as explicit change requests with integration test validation for auth and depot download flows.
3. Record version change rationale and regression results in this file.

## Linux Module Consumption

Current Linux port modules consuming JavaSteam:

- `gamenative-linux/core/domain/build.gradle.kts`
- `gamenative-linux/core/store-steam/build.gradle.kts`

When updating JavaSteam, change both module coordinates in the same patch and run:

- `./gradlew -p gamenative-linux :core:domain:compileKotlin :core:store-steam:test`
