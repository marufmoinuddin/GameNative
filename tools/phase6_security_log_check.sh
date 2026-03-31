#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

# Detect likely accidental logging of sensitive credential values.
pattern='(println|print|appendLine|logger\.(info|debug|warn|error))\([^\n]*(token|password|secret|oauth)'

# Exclude tests and docs to focus on runtime code paths.
if rg -n --pcre2 "$pattern" gamenative-linux --glob '!**/test/**' --glob '!**/*.md'; then
  echo "FAIL: potential sensitive logging pattern detected"
  exit 1
fi

echo "Security log review PASS"
