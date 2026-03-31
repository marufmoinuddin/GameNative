#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

version="${1:-0.1.0}"

if ! command -v rpmbuild >/dev/null 2>&1; then
  echo "FAIL: rpmbuild is required"
  exit 1
fi

./tools/build_linux_release_artifacts.sh "$version"

out_dir="build/release/linux-arm64"
srpm_out="$out_dir/srpm"
mkdir -p "$srpm_out"

rpmbuild -bs packaging/fedora/gamenative.spec \
  --define "app_version $version" \
  --define "_sourcedir $out_dir" \
  --define "_srcrpmdir $srpm_out"

echo "Source RPM directory: $srpm_out"
ls -1 "$srpm_out"/*.src.rpm
