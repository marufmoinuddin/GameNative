# Fedora ARM64 Validation Bootstrap

## Status

- Provisioning from this repository workspace is not directly executable.
- This checklist tracks readiness and validation once the target machine or VM is available.

## Minimum Target Environment

- Fedora 40 or 41 on ARM64 hardware/VM.
- Wayland session with XWayland support.
- Vulkan-capable graphics stack.
- JDK 17 installed.

## P0 Runtime Prerequisites

- `wine` (or `wine-staging`)
- `box64`
- `mesa-vulkan-drivers`
- `libsecret`

## Validation Gates

1. Run capability detection and record output.
2. Launch baseline executable (`notepad.exe`) via Wine + Box64.
3. Verify logs written to XDG state paths.
4. Verify crash marker output for forced abnormal exit.

## Evidence To Store

- Host info (`uname -a`, Fedora release, GPU details).
- Runtime binaries and versions.
- Launch logs for success and failure cases.
- Screenshots of first-run diagnostics screen when available.
