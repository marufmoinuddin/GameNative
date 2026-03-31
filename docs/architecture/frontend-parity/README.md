# Frontend Parity Artifacts

This directory stores the Android reference artifacts used for Linux frontend parity verification.

## Expected Structure

- `android/` — source captures from Android UI
- `linux/` — Linux desktop captures from matching states
- `diff/` — generated visual diffs for review

## Naming Convention

`<screen>-<state>-<variant>.png`

Examples:

- `library-populated-default.png`
- `account-error-invalid-credentials.png`
- `downloads-loading-empty-queue.png`

## Minimum Required States Per Screen

- `loading`
- `empty`
- `error`
- `populated`
- `dialog`

## Review Rule

A parity PR is not complete unless matching Android and Linux captures and a diff artifact are present for each touched state.
