#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

version="${1:-0.1.0}"
short_sha="$(git rev-parse --short HEAD 2>/dev/null || echo dev)"
build_stamp="$(date -u +%Y%m%dT%H%M%SZ)"
release_id="${version}-${short_sha}-${build_stamp}"

out_dir="build/release/linux-arm64"
stage_dir="$out_dir/stage"
mkdir -p "$stage_dir"

# Stage core deliverables.
cp -r gamenative-linux "$stage_dir/"
cp -r packaging "$stage_dir/"
cp LINUX_ARM64_PORT_PLAN.md "$stage_dir/"
cp README.md "$stage_dir/"

artifact_name="gamenative-linux-${release_id}-aarch64.tar.gz"
artifact_path="$out_dir/$artifact_name"
rm -f "$artifact_path"
tar -C "$stage_dir" -czf "$artifact_path" .

# Canonical artifact name used by RPM Source0.
canonical_artifact_path="$out_dir/gamenative-${version}-aarch64.tar.gz"
cp "$artifact_path" "$canonical_artifact_path"

sha_file="$artifact_path.sha256"
sha256sum "$artifact_path" > "$sha_file"
artifact_sha="$(awk '{print $1}' "$sha_file")"

canonical_sha_file="$canonical_artifact_path.sha256"
sha256sum "$canonical_artifact_path" > "$canonical_sha_file"
canonical_sha="$(awk '{print $1}' "$canonical_sha_file")"

sbom_file="$out_dir/gamenative-linux-${release_id}-sbom.spdx.json"
cat > "$sbom_file" <<EOF
{
  "spdxVersion": "SPDX-2.3",
  "dataLicense": "CC0-1.0",
  "SPDXID": "SPDXRef-DOCUMENT",
  "name": "gamenative-linux-arm64-release-${release_id}",
  "documentNamespace": "https://gamenative.app/spdx/${release_id}",
  "creationInfo": {
    "created": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "creators": ["Tool: build_linux_release_artifacts.sh"]
  },
  "packages": [
    {
      "name": "gamenative-linux",
      "SPDXID": "SPDXRef-Package-GameNativeLinux",
      "versionInfo": "${version}",
      "downloadLocation": "NOASSERTION",
      "licenseConcluded": "NOASSERTION",
      "licenseDeclared": "Apache-2.0",
      "filesAnalyzed": false,
      "checksums": [
        {
          "algorithm": "SHA256",
          "checksumValue": "${artifact_sha}"
        }
      ]
    }
  ]
}
EOF

manifest_file="$out_dir/release-manifest.txt"
cat > "$manifest_file" <<EOF
release_id=${release_id}
artifact=${artifact_path}
sha256=${artifact_sha}
sbom=${sbom_file}
canonical_artifact=${canonical_artifact_path}
canonical_sha256=${canonical_sha}
EOF

echo "Release artifact: $artifact_path"
echo "Canonical artifact: $canonical_artifact_path"
echo "SHA256 file: $sha_file"
echo "SBOM file: $sbom_file"
echo "Manifest: $manifest_file"
