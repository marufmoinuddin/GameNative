# GameNative Linux Port Architecture Overview

## Objective

Port GameNative from Android to a Linux ARM64 desktop application by reusing domain logic and replacing Android-specific host runtime behavior.

## Current Baseline

- Platform: Android app module in `app/`.
- Core complexity hotspots: `SteamService.kt`, `PrefManager.kt`, Android service lifecycle, and runtime process launching.
- Reuse-ready zones: `data/`, `enums/`, `events/`, `gamefixes/`, and selected utility logic.

## Migration Strategy

1. Extract host-agnostic domain and store logic from Android-bound classes.
2. Introduce Linux host abstractions for config, credentials, networking, notifications, process orchestration, and diagnostics.
3. Rebuild UI and background workflow lifecycle for desktop process ownership.
4. Package and ship on Fedora ARM64 with reproducible CI artifacts.

## Initial Phase Focus

- Phase 0: Architecture mapping, feature inventory, dependency pin audit, and `SteamService.kt` decomposition map.
- Phase 1: `gamenative-linux` scaffolding with compile-ready modules and explicit host interfaces.

## P0 Scope Lock

- Steam-only functional parity.
- One runtime path: Wine + Box64.
- Fedora ARM64 as only officially supported release target.
- Non-Steam stores behind feature flags.
