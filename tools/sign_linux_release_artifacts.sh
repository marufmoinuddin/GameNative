#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

manifest="build/release/linux-arm64/release-manifest.txt"
if [[ ! -f "$manifest" ]]; then
  echo "FAIL: missing $manifest"
  exit 1
fi

if ! command -v gpg >/dev/null 2>&1; then
  echo "FAIL: gpg is required"
  exit 1
fi

# shellcheck disable=SC1090
source "$manifest"

files=("$artifact" "${artifact}.sha256" "$canonical_artifact" "${canonical_artifact}.sha256" "$sbom" "$manifest")

for file in "${files[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "FAIL: missing file to sign: $file"
    exit 1
  fi
  gpg --batch --yes --armor --detach-sign "$file"
  gpg --verify "${file}.asc" "$file" >/dev/null 2>&1
  echo "SIGNED: $file"
done

echo "Artifact signing PASS"
