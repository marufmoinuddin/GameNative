#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

manifest="build/release/linux-arm64/release-manifest.txt"
if [[ ! -f "$manifest" ]]; then
  echo "FAIL: missing release manifest at $manifest"
  exit 1
fi

# shellcheck disable=SC1090
source "$manifest"

if [[ -z "${artifact:-}" || -z "${sha256:-}" || -z "${sbom:-}" || -z "${canonical_artifact:-}" || -z "${canonical_sha256:-}" ]]; then
  echo "FAIL: manifest is missing required keys (artifact, sha256, sbom, canonical_artifact, canonical_sha256)"
  exit 1
fi

if [[ ! -f "$artifact" ]]; then
  echo "FAIL: artifact missing at $artifact"
  exit 1
fi

if [[ ! -f "${artifact}.sha256" ]]; then
  echo "FAIL: checksum file missing at ${artifact}.sha256"
  exit 1
fi

if [[ ! -f "$sbom" ]]; then
  echo "FAIL: sbom file missing at $sbom"
  exit 1
fi

if [[ ! -f "$canonical_artifact" ]]; then
  echo "FAIL: canonical artifact missing at $canonical_artifact"
  exit 1
fi

if [[ ! -f "${canonical_artifact}.sha256" ]]; then
  echo "FAIL: canonical checksum file missing at ${canonical_artifact}.sha256"
  exit 1
fi

echo "[1/3] Verify checksum integrity"
sha256sum -c "${artifact}.sha256"

echo "[2/3] Verify manifest checksum matches file checksum"
actual_sha="$(sha256sum "$artifact" | awk '{print $1}')"
if [[ "$actual_sha" != "$sha256" ]]; then
  echo "FAIL: manifest sha256 does not match artifact checksum"
  exit 1
fi

echo "[3/3] Verify SBOM references artifact checksum"
if ! grep -q "$sha256" "$sbom"; then
  echo "FAIL: sbom does not contain artifact checksum"
  exit 1
fi

canonical_actual_sha="$(sha256sum "$canonical_artifact" | awk '{print $1}')"
if [[ "$canonical_actual_sha" != "$canonical_sha256" ]]; then
  echo "FAIL: canonical artifact checksum mismatch"
  exit 1
fi

echo "Release artifact verification PASS"
