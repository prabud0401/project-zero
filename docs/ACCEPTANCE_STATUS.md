# Acceptance evidence register

**Snapshot:** 2026-09-16. Documentation review only; no runtime hardening implementation in this update.

Status vocabulary: NOT_STARTED, IMPLEMENTED_UNVERIFIED, VERIFIED, BLOCKED. A record must include task owner, requirement, evidence location, build/commit identifier, environment, date, limitations, and any blocking input. Do not infer VERIFIED from documentation, a successful compilation, or another model's unsupported claim.

## Existing scaffold

- Blank Compose shell and HOME/DEFAULT activity: IMPLEMENTED_UNVERIFIED for runtime behavior. Existing implementation is under `android-client`; source and merged debug/release manifests were inspected in the prior implementation session. No device/emulator evidence is available.
- Build/static gate: VERIFIED for the earlier scaffold only. Prior independent command: `gradlew.bat assembleDebug check lint --console=plain`, exit 0, 23 seconds, 72 actionable tasks. Working tree was uncommitted; no immutable build identifier was recorded. Evidence artifact: `android-client/app/build/reports/lint-results-debug.txt` (0 errors, 8 warnings). This is historical evidence, not a new build in this documentation update.
- Unit-test coverage: NOT_STARTED; previous unit tasks reported NO-SOURCE.
- Complete SDD Task 1: NOT_STARTED for missing domain/contracts/architecture-test/CI/lock work; scaffold is partial progress only.
- SDD Tasks 2–6: NOT_STARTED. The user has now authorized Grok-led work toward all six tasks, sequentially after each dependency's acceptance evidence passes.

## Hardening controls

All statuses below concern implementation and verification, not whether the requirement is documented. Their definitions and evidence expectations are in RELIABILITY_SECURITY.md.

- H01 — home isolation/recovery. Tasks 1, 2, 5. NOT_STARTED.
- H02 — event identity, snapshot revision, data epoch. Tasks 1, 2. NOT_STARTED.
- H03 — at-most-once action attempts. Tasks 1, 3, 5. NOT_STARTED.
- H04 — canonical wire contracts and validation. Tasks 1, 4. NOT_STARTED.
- H05 — privacy/routing precedence and calibrated thresholds. Tasks 1–4. NOT_STARTED.
- H06 — cache identity/authority isolation. Tasks 1–4. NOT_STARTED.
- H07 — encryption, backup exclusions, deletion. Tasks 1, 2, 4, 6. NOT_STARTED.
- H08 — hostile notification/model input handling. Tasks 2–5. NOT_STARTED.
- H09 — application authentication and IAM boundaries. Tasks 1, 4, 6. NOT_STARTED.
- H10 — durable idempotency and atomic reservations. Tasks 1, 4. NOT_STARTED.
- H11 — cost/abuse envelope and measured accounting. Tasks 4, 6. NOT_STARTED.
- H12 — bounded queues and token-aware batching. Tasks 1, 2, 4, 5. NOT_STARTED.
- H13 — platform capability/device matrix. Tasks 2, 3, 5, 6. NOT_STARTED.
- H14 — migrations and compatible recovery. Tasks 1, 2, 6. NOT_STARTED.
- H15 — signed configuration and authority limits. Tasks 1, 3, 4, 6. NOT_STARTED.
- H16 — model evaluation and upgrade/fallback. Tasks 1, 3, 4, 6. NOT_STARTED.
- H17 — SLO instrumentation and release measurements. Tasks 1, 4–6. NOT_STARTED.
- H18 — operational recovery drills. Tasks 4, 6. NOT_STARTED.
- H19 — dependency controls and maintenance process. Tasks 1, 6. NOT_STARTED; existing version pins alone do not satisfy the full control.
- H20 — complete evidence and independent release review. All tasks. IMPLEMENTED_UNVERIFIED for this initial register only; release review and runtime evidence are absent.

For every control above, implementation evidence/build identifier/environment/date are pending unless explicitly recorded. Add per-test records as work proceeds; never replace all statuses with VERIFIED as one batch operation.

## Open release inputs and residual risks

Final app/signing identity, approved GCP region, provider data-handling settings, fleet budget, supported locales, physical reference devices, and incident/release ownership are not confirmed. They block their dependent integration/release gates, not routine local Task 1 work.

Residual risks requiring evidence and owner review include compromised OS/device, malicious target apps, notification information unavailable from Android, classifier/model mistakes, unsupported OEM behavior, offline revocation delay, stolen bearer tokens, delayed provider/billing accounting, interrupted migrations, and external API changes. The design limits these risks; it does not eliminate them.

**Release decision:** NOT READY. All six SDD tasks and applicable H01–H20 acceptance controls must pass before production readiness can be asserted.
