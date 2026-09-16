# Acceptance evidence register

**Snapshot:** 2026-09-16. SDD Task 1 static foundation committed at `59875cf`. Runtime hardening for Tasks 2–6 is still absent.

Status vocabulary: NOT_STARTED, IMPLEMENTED_UNVERIFIED, VERIFIED, BLOCKED. A record must include task owner, requirement, evidence location, build/commit identifier, environment, date, limitations, and any blocking input. Do not infer VERIFIED from documentation, a successful compilation, or another model's unsupported claim.

## Existing scaffold

- Blank Compose shell and HOME/DEFAULT activity: IMPLEMENTED_UNVERIFIED for runtime behavior. `MainActivity` still renders an empty Compose `Surface`. Source and merged debug/release manifests retain HOME/DEFAULT. `adb devices` on this host returned an empty list, so blank-screen rendering and HOME selection were not observed.
- Build/static gate: VERIFIED at commit `59875cf`. Command from `android-client` with `ANDROID_HOME=C:\Users\prabu\AppData\Local\Android\Sdk`: `.\gradlew.bat assembleDebug check lint test --console=plain`, exit 0, 14 seconds, 91 actionable tasks (15 executed, 76 up-to-date on the orchestrator rerun after commit). Environment: Windows, JDK used by the Gradle toolchain targeting 17, CI pin is JDK 21. Lint artifact: `android-client/app/build/reports/lint-results-debug.txt` — 0 errors, 18 warnings (version-newer and ObsoleteSdkInt). Pins were not bumped.
- Unit-test coverage: VERIFIED for JVM modules in this working tree. JUnit XML totals: `:domain` 39 tests, `:contracts` 24 tests, `:architecture-tests` 5 tests (68 tests, 0 failures, 0 errors, 0 skipped). `:app` `testDebugUnitTest` / `testReleaseUnitTest` remain NO-SOURCE (honest absence, not passing coverage).
- Complete SDD Task 1: IMPLEMENTED_UNVERIFIED. The five SDD static criteria have command/test/manifest evidence below. BACKLOG also requires device/emulator verification of the blank activity and HOME eligibility; that item is still open (`adb` attached zero devices). Do not treat Task 1 as accepted for Task 2 start.
- SDD Tasks 2–6: NOT_STARTED.

## SDD Task 1 criteria

| Criterion | Status | Evidence | Limitations |
|---|---|---|---|
| JVM tests for constructor invariants, enum wire values, sealed outcomes, timestamp/Unicode/duplicate-parameter/action-expiry | VERIFIED (unit) | `:domain` 39 tests in `domain/build/test-results/test` | Does not prove later adapters honor the types |
| Golden JSON/protobuf round-trip v1, reject missing/extra fields, canonicalization, H04 presence | VERIFIED (unit) | `:contracts` 24 tests in `contracts/build/test-results/test`; fixtures under `contracts/src/test/resources/golden/v1` | Default protobuf JSON is not used; explicit mapper only |
| Architecture test fails on Android imports in `:domain` and on dependency cycles | VERIFIED (unit) | `:architecture-tests` 5 tests; ArchUnit plus source import scan | Enforced on compiled packages `app.projectzero.*` |
| Merged manifest has no accessibility, overlay, broad package query, direct call, SMS-send, contacts, or storage permission | VERIFIED (static scan) | Source `app/src/main/AndroidManifest.xml`; merged debug/release `app/build/intermediates/merged_manifests/*/process*Manifest/AndroidManifest.xml`. HOME/DEFAULT present. Forbidden tokens absent. | Debug merged manifest still includes Compose `PreviewActivity` from tooling. AndroidX adds signature-protected `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. Not a device proof. |
| `./gradlew check lint test` on checkout with dependency locks | VERIFIED at `59875cf` | LockMode.STRICT; lockfiles `app/gradle.lockfile`, `domain/gradle.lockfile`, `contracts/gradle.lockfile`, `architecture-tests/gradle.lockfile`, `settings-gradle.lockfile`. Native protoc configs are not locked (OS-specific). CI: `.github/workflows/ci.yml` (ubuntu, JDK 21). | GitHub Actions run not yet observed on this commit. |

## Hardening controls

All statuses below concern implementation and verification, not whether the requirement is documented. Their definitions and evidence expectations are in RELIABILITY_SECURITY.md.

- H01 — home isolation/recovery. Tasks 1, 2, 5. NOT_STARTED for runtime. No degraded-home feature flags or Direct Boot surface.
- H02 — event identity, snapshot revision, data epoch. Tasks 1, 2. Task 1 types/tests IMPLEMENTED_UNVERIFIED (`EventRevision` vs `SnapshotRevision`/`DataEpoch`, `SnapshotApplyPolicy`). Runtime ingest/reconcile NOT_STARTED.
- H03 — at-most-once action attempts. Tasks 1, 3, 5. Task 1 types/tests IMPLEMENTED_UNVERIFIED (`ActionAttemptOutcome` includes `OUTCOME_UNKNOWN` and `HANDED_OFF`). No executor. NOT_STARTED at runtime.
- H04 — canonical wire contracts and validation. Tasks 1, 4. Task 1 JSON Schema + proto + presence mapper VERIFIED by the 24 `:contracts` tests listed above. Network adapters NOT_STARTED.
- H05 — privacy/routing precedence and calibrated thresholds. Tasks 1–4. Task 1 threshold/precedence types IMPLEMENTED_UNVERIFIED (`RoutingPolicy`, 0.55/0.82). No filter/router runtime. Calibration NOT_STARTED.
- H06 — cache identity/authority isolation. Tasks 1–4. Task 1 partition/version-binding types IMPLEMENTED_UNVERIFIED. No cache implementation.
- H07 — encryption, backup exclusions, deletion. Tasks 1, 2, 4, 6. NOT_STARTED.
- H08 — hostile notification/model input handling. Tasks 2–5. NOT_STARTED.
- H09 — application authentication and IAM boundaries. Tasks 1, 4, 6. NOT_STARTED (token lifetime constants only in `DomainBounds`).
- H10 — durable idempotency and atomic reservations. Tasks 1, 4. Task 1 operation/reservation state types IMPLEMENTED_UNVERIFIED (`OperationState` includes `OUTCOME_UNKNOWN`; join/conflict). No datastore.
- H11 — cost reports include fixed costs and abuse. Tasks 4, 6. NOT_STARTED.
- H12 — bounded queues, allocations, and retries. Tasks 1, 2, 4, 5. Task 1 bound constants IMPLEMENTED_UNVERIFIED (`DomainBounds`). No queues.
- H13 — platform capability/device matrix. Tasks 2, 3, 5, 6. NOT_STARTED.
- H14 — migrations and compatible recovery. Tasks 1, 2, 6. Task 1 independent version fields IMPLEMENTED_UNVERIFIED (`CompatibilityVersions`). No Room/wire migrations.
- H15 — signed configuration and authority limits. Tasks 1, 3, 4, 6. Registry proto messages exist; signing/runtime NOT_STARTED.
- H16 — model evaluation and upgrade/fallback. Tasks 1, 3, 4, 6. NOT_STARTED.
- H17 — SLO instrumentation and release measurements. Tasks 1, 4–6. NOT_STARTED.
- H18 — operational recovery drills. Tasks 4, 6. NOT_STARTED.
- H19 — dependency controls and architectural growth. Tasks 1, 6. Task 1 slice IMPLEMENTED_UNVERIFIED: version catalog pins, STRICT Gradle lockfiles, GitHub Actions pins (`actions/checkout@v4.2.2`, `actions/setup-java@v4.7.1`, `android-actions/setup-android@v3.2.2`). Not yet run on GitHub. SBOM, secret/dependency scans, isolated least-privilege CI, and protected release signing remain NOT_STARTED (Task 6).
- H20 — complete evidence and independent release review. All tasks. IMPLEMENTED_UNVERIFIED for this register update only.

For every control above, implementation evidence/build identifier/environment/date are pending unless explicitly recorded. Add per-test records as work proceeds; never replace all statuses with VERIFIED as one batch operation.

## Open release inputs and residual risks

Final app/signing identity, approved GCP region, provider data-handling settings, fleet budget, supported locales, physical reference devices, and incident/release ownership are not confirmed. They block their dependent integration/release gates, not routine local Task 1 work.

Residual risks requiring evidence and owner review include compromised OS/device, malicious target apps, notification information unavailable from Android, classifier/model mistakes, unsupported OEM behavior, offline revocation delay, stolen bearer tokens, delayed provider/billing accounting, interrupted migrations, and external API changes. The design limits these risks; it does not eliminate them.

**Release decision:** NOT READY. All six SDD tasks and applicable H01–H20 acceptance controls must pass before production readiness can be asserted.
