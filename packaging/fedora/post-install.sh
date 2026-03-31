#!/usr/bin/env bash
set -euo pipefail

echo "GameNative post-install capability check"
for cmd in wine box64 vulkaninfo; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "WARN: missing $cmd"
  else
    echo "OK: $cmd"
  fi
done
