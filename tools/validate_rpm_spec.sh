#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

spec_file="packaging/fedora/gamenative.spec"

if [[ ! -f "$spec_file" ]]; then
  echo "FAIL: missing $spec_file"
  exit 1
fi

if ! command -v rpmspec >/dev/null 2>&1; then
  echo "FAIL: rpmspec is required (install rpm tooling)."
  exit 1
fi

echo "[1/2] Parse RPM spec"
rpmspec -P "$spec_file" >/dev/null

echo "[2/2] Validate required runtime deps"
requires_output="$(rpmspec -q --requires "$spec_file")"
for dep in wine box64 mesa-vulkan-drivers libsecret java-17-openjdk-headless; do
  if ! grep -q "^${dep}" <<<"$requires_output"; then
    echo "FAIL: missing runtime dependency '$dep' in spec requires"
    exit 1
  fi
done

echo "RPM spec validation PASS"
