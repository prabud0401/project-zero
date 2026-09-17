# Acceptance evidence register

**Snapshot:** 2026-09-17. SDD Task 1 static foundation remains `59875cf` / `2c4ba0f`. Task 2 local ingestion/privacy is implemented with unit/static evidence in this working tree (commit after this register update). Device/emulator runtime remains BLOCKED. Tasks 3–6 are not started.

Status vocabulary: NOT_STARTED, IMPLEMENTED_UNVERIFIED, VERIFIED, BLOCKED. A record must include task owner, requirement, evidence location, build/commit identifier, environment, date, limitations, and any blocking input. Do not infer VERIFIED from documentation, a successful compilation, or another model's unsupported claim.

## Existing scaffold

- HOME/DEFAULT activity: IMPLEMENTED_UNVERIFIED for runtime behavior. `MainActivity` now hosts onboarding plus cluster cards (still no device observation). Source and merged debug/release manifests retain HOME/DEFAULT. `adb devices` on this host returned an empty list (`List of devices attached` with no serials), so blank-screen rendering, HOME selection, locked-emulator privacy, and listener grant/revoke were not observed.
- Build/static gate: VERIFIED for Task 2 at this working tree. Command from `android-client` with `ANDROID_HOME=C:\Users\prabu\AppData\Local\Android\Sdk`, `TEMP`/`TMP`=`D:\tmp`: `.\gradlew.bat assembleDebug check lint test --console=plain`, exit 0, 1m 24s, 293 actionable tasks (27 executed, 266 up-to-date on the final rerun). Environment: Windows, JDK used by the Gradle toolchain targeting 17, CI pin is JDK 21. Lint artifact: `android-client/app/build/reports/lint-results-debug.txt` — 0 errors, 27 warnings (version-newer; SQLCipher pinned at 4.17.0 because 4.18+/4.19.0 require compileSdk 37). Pins were not bumped except sqlcipher 4.17.0.
- Unit-test coverage: VERIFIED for JVM and Robolectric unit tests in this working tree. Unique test methods (debug XML, 0 failures / 0 errors / 0 skipped): `:domain` 39, `:contracts` 24, `:architecture-tests` 8, `:local-ai` 17, `:data-local` 6, `:notification-ingest` 20, `:app` 5 (3 Compose UI tests debug-only + 2 lock-redactor tests). Debug total 119. Release unit tests re-run `:data-local` 6, `:notification-ingest` 20, `:app` 2. jqwik properties are counted as methods; they execute multiple tries internally. `:app` `androidTest` exists but was not executed (no device).
- Complete SDD Task 1: IMPLEMENTED_UNVERIFIED. Static criteria remain as recorded at `59875cf`. Device/emulator HOME eligibility is still BLOCKED.
- SDD Task 2: IMPLEMENTED_UNVERIFIED. Unit/static criteria have evidence below. Device/locked-emulator/instrumentation items are BLOCKED (`adb` empty).
- SDD Tasks 3–6: NOT_STARTED.

## SDD Task 1 criteria

| Criterion | Status | Evidence | Limitations |
|---|---|---|---|
| JVM tests for constructor invariants, enum wire values, sealed outcomes, timestamp/Unicode/duplicate-parameter/action-expiry | VERIFIED (unit) | `:domain` 39 tests in `domain/build/test-results/test` | Does not prove later adapters honor the types |
| Golden JSON/protobuf round-trip v1, reject missing/extra fields, canonicalization, H04 presence | VERIFIED (unit) | `:contracts` 24 tests in `contracts/build/test-results/test`; fixtures under `contracts/src/test/resources/golden/v1` | Default protobuf JSON is not used; explicit mapper only |
| Architecture test fails on Android imports in `:domain` and on dependency cycles | VERIFIED (unit) | `:architecture-tests` 8 tests; ArchUnit plus source import scan, listener permission, no body logging | Enforced on compiled packages `app.projectzero.*` |
| Merged manifest has no accessibility, overlay, broad package query, direct call, SMS-send, contacts, or storage permission | VERIFIED (static scan) | Source `app/src/main/AndroidManifest.xml`; merged debug/release `app/build/intermediates/merged_manifests/*/process*Manifest/AndroidManifest.xml`. HOME/DEFAULT present. Forbidden tokens absent. | Debug merged manifest still includes Compose `PreviewActivity` from tooling. AndroidX adds signature-protected `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. Not a device proof. |
| `./gradlew check lint test` on checkout with dependency locks | VERIFIED at `59875cf` for Task 1; re-verified for Task 2 in this tree | LockMode.STRICT; lockfiles now also `local-ai/gradle.lockfile`, `data-local/gradle.lockfile`, `notification-ingest/gradle.lockfile`. Native protoc configs are not locked (OS-specific). `androidApis`/`androidJdkImage` locking deactivated (empty AGP configs). CI: `.github/workflows/ci.yml` (ubuntu, JDK 21). | GitHub Actions run not yet observed on the Task 2 commit. |

## SDD Task 2 criteria

Owner: Grok-led implementation 2026-09-17. Environment: Windows host, compile/target SDK 36, minSdk 29, Robolectric `@Config(sdk = 29)`, no attached device.

| Criterion | Status | Evidence | Limitations |
|---|---|---|---|
| Robolectric/instrumentation post/update/remove, same-key repost, process death, reboot, listener disconnect, multi-user serials, ongoing, out-of-order | VERIFIED (unit/Robolectric) for listed cases except reboot and instrumentation | `:notification-ingest` `IngestCoordinatorTest` 13 tests; in-memory Room | Reboot as a distinct OS event is not simulated (process-death + reconnect cover persistence). Instrumentation BLOCKED: no device. |
| Property tests: normalization idempotent, unique cluster members, tombstones cannot revive, raw retention <=24h | VERIFIED (unit) | `:local-ai` jqwik `TextNormalizerPropertyTest` / `DeterministicSummaryEngineTest`; `:data-local` `NotificationRepositoryTest`; ingest tombstone/retention tests | jqwik try counts are internal; XML counts methods. Not a device TTL job. |
| Security: RemoteViews, foreign PendingIntent metadata, spans, oversized, OTP, health, finance, managed-profile never enter a cloud-eligible object | VERIFIED (unit) | `:local-ai` `PrivacyClassifierTest` / `CloudEligibleEnvelopeTest`; `:notification-ingest` `SecurityAndNormalizerTest` | Classifier is regex/heuristic, not a proof of anonymity (H05 residual risk). |
| Listener denied/revoked: launcher usable with non-blocking explanation | VERIFIED (Compose unit, debug) | `:app` `LauncherUiTest.deniedListenerShowsNonBlockingExplanationAndHomeRemainsUsable`; revoke advances epoch in `IngestCoordinatorTest.accessRevokeAdvancesEpoch` | System Settings grant/revoke not observed. Instrumentation counterpart present but not run. |
| Locked emulator: secret/personal text absent from Compose semantics/snapshots/logs | VERIFIED (Compose unit) / BLOCKED (device) | `LockScreenRedactor` + `LauncherUiTest.lockScreenRedactsPersonalAndSecretTextFromSemantics`; architecture scan forbids body logging | No locked emulator. Logs scanned statically; not a logcat capture on device. |

Manifest scan (source + merged debug): listener service `android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"`; no `QUERY_ALL_PACKAGES`, `AccessibilityService`, `BIND_ACCESSIBILITY_SERVICE`, `SYSTEM_ALERT_WINDOW`. Debug merged manifest still includes Compose `PreviewActivity` and `ui-test-manifest` `ComponentActivity`.

## Hardening controls

All statuses below concern implementation and verification, not whether the requirement is documented. Their definitions and evidence expectations are in RELIABILITY_SECURITY.md.

- H01 — home isolation/recovery. Tasks 1, 2, 5. IMPLEMENTED_UNVERIFIED for Task 2: `AppGraph` degrades when SQLCipher/key fails; home still renders with `store_unavailable` / access-denied banners. Direct Boot / first-unlock on a real device BLOCKED (`adb` empty).
- H02 — event identity, snapshot revision, data epoch. Tasks 1, 2. Task 2 runtime VERIFIED (unit): same-key update keeps eventId and advances revision; repost after removal gets a new id; duplicates are NO_OP; reconnect does not invent history; revoke advances dataEpoch.
- H03 — at-most-once action attempts. Tasks 1, 3, 5. Task 1 types/tests IMPLEMENTED_UNVERIFIED (`ActionAttemptOutcome` includes `OUTCOME_UNKNOWN` and `HANDED_OFF`). No executor. NOT_STARTED at runtime.
- H04 — canonical wire contracts and validation. Tasks 1, 4. Task 1 JSON Schema + proto + presence mapper VERIFIED by the 24 `:contracts` tests listed above. Network adapters NOT_STARTED.
- H05 — privacy/routing precedence and calibrated thresholds. Tasks 1–4. Task 2 filter/classifier VERIFIED (unit) for OTP/health/finance/managed-profile/hostile extras → LocalOnly and no `CloudEligibleEnvelope`. Calibration NOT_STARTED. No network calls exist yet.
- H06 — cache identity/authority isolation. Tasks 1–4. Task 1 partition/version-binding types IMPLEMENTED_UNVERIFIED. No cache implementation.
- H07 — encryption, backup exclusions, deletion. Tasks 1, 2, 4, 6. IMPLEMENTED_UNVERIFIED: SQLCipher factory refuses plaintext on native-load/key failure; Keystore-wrapped key; DB/salt/key in `noBackupFilesDir`; backup/data-extraction rules exclude database/file/sharedpref and device_* domains; Room schema v2 exported. Encrypted open and backup-restore were not observed on a device. `allowBackup="false"`.
- H08 — hostile notification/model input handling. Tasks 2–5. VERIFIED (unit) that RemoteViews/PendingIntent/spans/oversized/OTP/health/finance/managed-profile never construct a cloud-eligible object; normalizer copies primitives only. Not a device PendingIntent click test.
- H09 — application authentication and IAM boundaries. Tasks 1, 4, 6. NOT_STARTED (token lifetime constants only in `DomainBounds`).
- H10 — durable idempotency and atomic reservations. Tasks 1, 4. Task 1 operation/reservation state types IMPLEMENTED_UNVERIFIED (`OperationState` includes `OUTCOME_UNKNOWN`; join/conflict). No datastore.
- H11 — cost reports include fixed costs and abuse. Tasks 4, 6. NOT_STARTED.
- H12 — bounded queues, allocations, and retries. Tasks 1, 2, 4, 5. IMPLEMENTED_UNVERIFIED: `IngestQueue` capacity `MAX_INGESTION_ITEMS` (256) coalesces by key and prioritizes removals; overflow marks incomplete. Not load-tested at 10x.
- H13 — platform capability/device matrix. Tasks 2, 3, 5, 6. BLOCKED: no physical OEM, emulator, or API matrix run. Unit tests use Robolectric SDK 29 only.
- H14 — migrations and compatible recovery. Tasks 1, 2, 6. Task 2 Room `MIGRATION_1_2` VERIFIED (unit) in `MigrationAndEncryptionTest`. Interrupted/disk-full/key-failure migrations not run on device.
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
