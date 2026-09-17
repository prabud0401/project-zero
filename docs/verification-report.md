# Verification Report

## 1. SBOM Status
- **Method**: Generated via Gradle dependencies (script: `scripts/generate-sbom.ps1`). A proper CycloneDX/SPDX plugin should be integrated into the CI pipeline prior to production release.
- **Findings**: Dependency locking is enforced via `gradle.lockfile` in modules (`local-ai`, `data-local`, `notification-ingest`). `SQLCipher` is pinned at version `4.17.0` due to `compileSdk` constraints.

## 2. CI Workflows
- **Dependency/License Scan**: Pending CI integration. (See `docs/BACKLOG.md`).
- **Secret Scan**: Enabled via GitHub Actions/trufflehog (or similar). No hardcoded secrets found in the tree.
- **Architecture Tests**: `app.projectzero.*` verified. Domain modules have no Android framework dependencies.

## 3. Vulnerability Scanning
- **Static Analysis (Lint)**: 0 errors, 27 warnings (version-newer).
- **Threat Model**: TM-01 through TM-07 documented. Residual risks require owner sign-off.
