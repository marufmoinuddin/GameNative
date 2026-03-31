# Linux Runtime Architecture (Skeleton)

## Purpose

Document the target architecture for Linux ARM64 runtime hosting, Steam integration boundaries, and diagnostics/recovery flows.

## Scope

- `gamenative-linux/core/runtime`
- `gamenative-linux/core/store-steam`
- `gamenative-linux/infra/*` modules used by runtime launch paths

## High-Level Component Map

- Runtime orchestration
- Process launch/stop supervision
- Profile/config persistence
- Steam service boundaries
- Diagnostics/reporting pipeline

## Data and Control Flows

### Launch Flow

1. Profile lookup and environment composition.
2. Supervision gate policy evaluation.
3. Process launch and session persistence.
4. Launch-state timeline and telemetry emission.

### Recovery Flow

1. Startup interrupted-session detection.
2. Startup decision persistence (`CLEAN_START`, `ATTACH_RUNNING_SESSION`, `RECOVER_INTERRUPTED_SESSION`).
3. Supervision hold application (`PROCEED`, `DELAY_RETRY`, `REQUIRE_MANUAL_INTERVENTION`).

### Diagnostics Flow

1. Capability snapshot collection.
2. Session/crash bundle assembly.
3. Incident summary generation (policy-tuned severity + remediation hints).
4. JSON reporter emission for desktop UI consumption.

## Module Contracts (To Be Detailed)

### `core/runtime`

- Orchestrator API (`RuntimeOrchestrator`)
- Process boundary (`ProcessRunner`)
- Recovery state service
- Diagnostics service

### `core/store-steam`

- Session manager boundary
- Library metadata boundary
- Download queue boundary
- Cloud save boundary

### `infra` modules

- Config/keyring/network/persistence/notifications responsibilities
- Runtime dependencies and ownership rules

## Failure Model (To Be Detailed)

- Abnormal exits and retry accounting
- Supervision recommendation derivation
- Launch block/delay behavior
- Incident severity policy thresholds

## Security and Credentials (To Be Detailed)

- Credential storage chain (libsecret primary, encrypted fallback)
- Config and state file permissions
- Sensitive diagnostics redaction

## Open Questions

- Final desktop-shell ownership of runtime diagnostics polling cadence.
- Cross-module schema versioning strategy for runtime diagnostics payloads.
- Linux integration test environment matrix for Phase 3 closeout.

## Phase 3 Verification Criteria

- `DefaultRuntimeOrchestrator` launch path enforces supervision gates (`PROCEED`, `DELAY_RETRY`, `REQUIRE_MANUAL_INTERVENTION`) with deterministic launch-state emission.
- `FileRuntimeRecoveryService` persists active-session, startup decision, supervision hold, retry attempts, supervision events, and launch timelines across process restarts.
- `FileRuntimeDiagnosticsService` emits a schemaed snapshot with startup, supervision, retry, timeline, and incident summary payloads.
- `RuntimeIncidentSummaryGenerator` applies profile policy thresholds and signature-based remediation hints.

Verification command used during Phase 3 closeout:

- `./gradlew -p gamenative-linux :core:runtime:test`
