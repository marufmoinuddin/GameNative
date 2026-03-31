#!/usr/bin/env bash
set -euo pipefail

app_root="/usr/share/gamenative/gamenative-linux"
shell_jar="$(ls -1 "$app_root"/desktop/shell/build/libs/shell-*.jar 2>/dev/null | head -n 1)"
runtime_lib_dir="$app_root/desktop/shell/build/packaging/runtime-libs"

if [[ -z "$shell_jar" || ! -f "$shell_jar" ]]; then
  echo "GameNative launcher error: desktop shell jar not found under $app_root" >&2
  exit 1
fi

classpath="$shell_jar"
if [[ -d "$runtime_lib_dir" ]]; then
  while IFS= read -r -d '' jar_file; do
    classpath="$classpath:$jar_file"
  done < <(find "$runtime_lib_dir" -type f -name '*.jar' -print0 | sort -z)
fi

exec java -cp "$classpath" app.gamenative.linux.desktop.shell.DesktopShellMainKt "$@"
