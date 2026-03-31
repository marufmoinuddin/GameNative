# Linux ARM64 Release Checklist

## Phase 5 Packaging

- [x] Build Linux modules compile pass
- [x] Unit tests pass (`core/*`, `infra/*`, `desktop/shell`)
- [ ] Runtime proof executed on Fedora ARM64 host (`runRuntimeProof`) via `.github/workflows/linux-port-fedora-arm64.yml`
- [ ] RPM spec validation (`./tools/validate_rpm_spec.sh`) on runner with RPM tooling
- [ ] Source RPM built (`./tools/build_source_rpm.sh <version>`) on runner with RPM tooling
- [x] tar.gz artifact generated with SHA256 checksum (`./tools/build_linux_release_artifacts.sh <version>`)
- [x] SPDX SBOM generated and attached to release (`build/release/linux-arm64/*.spdx.json`)
- [x] Release manifest reviewed (`build/release/linux-arm64/release-manifest.txt`)
- [x] Artifact integrity verification passed (`./tools/verify_linux_release_artifacts.sh`)
- [x] Artifacts signed with GPG (`./tools/sign_linux_release_artifacts.sh`)
- [ ] Fedora ARM64 CI gate green (`Linux Port Fedora ARM64 Gate` workflow)
- [ ] Tagged release workflow green (`.github/workflows/linux-port-release.yml`)

## Phase 6 Hardening

- [x] Android import audit remains zero for Linux modules
- [x] Runtime diagnostics snapshot schema stability checked (`./tools/phase6_schema_stability_check.sh`)
- [x] Crash/recovery/supervision integration tests green
- [x] Credential handling path validated (libsecret primary, encrypted fallback)
- [x] Security review pass (no raw token logging) (`./tools/phase6_security_log_check.sh`)
- [x] Runtime proof passed for current architecture (`./gradlew -p gamenative-linux :core:runtime:runRuntimeProof`)

## Note

- Runtime proof is architecture-aware: ARM64 hosts require Wine+Box64, while non-ARM64 hosts require Wine capability + smoke validation.
