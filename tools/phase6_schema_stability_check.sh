#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

snapshot="gamenative-linux/core/runtime/build/runtime-prototype-state/diagnostics/runtime-snapshot.json"

./gradlew -p gamenative-linux :core:runtime:runRuntimeDiagnostics >/dev/null

if [[ ! -f "$snapshot" ]]; then
  echo "FAIL: runtime snapshot missing at $snapshot"
  exit 1
fi

required_keys=(
  '"schemaVersion"'
  '"startupRecommendation"'
  '"supervisionRecommendation"'
  '"supervisionHold"'
  '"retryAttempt"'
  '"supervisionEvents"'
  '"launchStateTimeline"'
  '"incidentSummary"'
)

for key in "${required_keys[@]}"; do
  if ! rg -q "$key" "$snapshot"; then
    echo "FAIL: missing required runtime snapshot key: $key"
    exit 1
  fi
done

echo "Runtime schema stability PASS"
