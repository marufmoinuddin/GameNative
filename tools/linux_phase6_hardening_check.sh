#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

echo "[1/6] Android import audit"
android_imports="$(rg -n '^import android\.' gamenative-linux | wc -l || true)"
echo "android_imports=$android_imports"
if [[ "$android_imports" -ne 0 ]]; then
  echo "FAIL: Android imports detected in gamenative-linux"
  exit 1
fi

echo "[2/6] Linux compile validation"
./gradlew -p gamenative-linux :core:domain:compileKotlin :core:runtime:compileKotlin :core:store-steam:compileKotlin :desktop:shell:compileKotlin :infra:config:compileKotlin :infra:keyring:compileKotlin :infra:network:compileKotlin :infra:persistence:compileKotlin :infra:notifications:compileKotlin

echo "[3/6] Linux test validation"
./gradlew -p gamenative-linux :infra:config:test :infra:keyring:test :infra:network:test :infra:persistence:test :infra:notifications:test :core:domain:test :core:store-steam:test :core:runtime:test :desktop:shell:test

echo "[4/6] Runtime snapshot schema stability"
./tools/phase6_schema_stability_check.sh

echo "[5/6] Security log leakage scan"
./tools/phase6_security_log_check.sh

echo "[6/6] Runtime proof status"
if ./gradlew -p gamenative-linux :core:runtime:runRuntimeProof; then
  echo "runtime_proof=PASS"
else
  echo "runtime_proof=FAIL (requires Wine+Box64 on target host)"
fi

echo "HARDENING CHECK COMPLETE"
