# Frontend Parity Baseline (Android → Linux Desktop)

## Purpose

This document defines the baseline required to execute and verify frontend parity between Android (`app`) and Linux desktop (`gamenative-linux/desktop/shell`).

## Scope

P0 workflows:

- Diagnostics
- Account sign-in
- Library
- Game detail
- Downloads
- Profiles
- Session monitor
- Settings

## State Capture Matrix

Each screen must provide Android references for the following states:

| Screen | Loading | Empty | Error | Populated | Dialog/Confirm |
|---|---|---|---|---|---|
| Diagnostics | ☐ | ☐ | ☐ | ☐ | ☐ |
| Account | ☐ | ☐ | ☐ | ☐ | ☐ |
| Library | ☐ | ☐ | ☐ | ☐ | ☐ |
| Game Detail | ☐ | ☐ | ☐ | ☐ | ☐ |
| Downloads | ☐ | ☐ | ☐ | ☐ | ☐ |
| Profiles | ☐ | ☐ | ☐ | ☐ | ☐ |
| Session Monitor | ☐ | ☐ | ☐ | ☐ | ☐ |
| Settings | ☐ | ☐ | ☐ | ☐ | ☐ |

## Parity Budget

- Visual variance: <= 2px spacing drift for core layouts.
- Typography variance: same size/weight/line-height or documented fallback.
- Interaction variance: no missing primary actions or flow-order regressions.

## Required Artifacts

- Versioned screenshot corpus in `docs/architecture/frontend-parity/`.
- Component-level parity mapping in `docs/architecture/feature-reuse-matrix.md`.
- PR-level screenshot diff attachments for touched screens/states.

## Initial Ownership

- UI Platform: token contract + theme parity.
- UI Systems: shared component parity.
- Feature Pods: screen-by-screen parity delivery.
- DevEx/CI: screenshot drift gate.
