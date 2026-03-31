#!/usr/bin/env bash
# GameNative CLI launcher
# Installed by the RPM to /usr/bin/gamenative-cli

set -euo pipefail

app_root="/usr/share/gamenative/gamenative-linux"
cli_jar="$(ls -1 "$app_root"/cli/build/libs/cli-*.jar 2>/dev/null | head -n 1)"
runtime_lib_dir="$app_root/cli/build/packaging/runtime-libs"

if [[ -z "$cli_jar" || ! -f "$cli_jar" ]]; then
    echo "GameNative CLI launcher error: cli jar not found under $app_root" >&2
    exit 1
fi

classpath="$cli_jar"
if [[ -d "$runtime_lib_dir" ]]; then
    while IFS= read -r -d '' jar_file; do
        classpath="$classpath:$jar_file"
    done < <(find "$runtime_lib_dir" -type f -name '*.jar' -print0 | sort -z)
fi

exec java -cp "$classpath" app.gamenative.linux.cli.CliMainKt "$@"
