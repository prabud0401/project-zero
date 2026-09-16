# Project Zero implementation backlog

This is the execution index for the six tasks in [SYSTEM_DESIGN.md, section 5](SYSTEM_DESIGN.md#5-cursor--antigravity-implementation-backlog). That section owns the detailed acceptance criteria. Read [IMPLEMENTATION_HANDOFF.md](IMPLEMENTATION_HANDOFF.md) before starting.

Every task also implements its assigned controls in [RELIABILITY_SECURITY.md](RELIABILITY_SECURITY.md). Track verification in [ACCEPTANCE_STATUS.md](ACCEPTANCE_STATUS.md); none of those runtime controls is currently claimed verified.

This sequence supersedes the previous backlog, including its broad package permission, notification-content logging, raw notification upload, and different task numbers. RevenueCat is outside these six tasks and requires a separate product decision.

## Task 1: Android foundation and domain contracts

**Status: partial.** The blank Compose shell builds; the complete SDD foundation is unfinished.

- Scope: preserve the existing Android scaffold and add pure Kotlin domain contracts, validation/result types, JSON schemas/protobuf, architecture checks, dependency locks, and baseline CI.
- Inputs: SDD sections 0 and 2 and the existing Gradle catalog/manifest.
- Outputs: `:app`, `:domain`, `:contracts`, and `:architecture-tests` under `android-client`; fixtures and checks; repository CI configuration. Keep one Android Gradle root at `android-client`.
- Acceptance: all five SDD Task 1 criteria, successful debug APK build, and device/emulator verification of the blank activity and HOME eligibility. Report `NO-SOURCE` as absent tests, not passing coverage.
- Manifest: HOME/DEFAULT; no accessibility service, overlay, broad package visibility, or notification listener. No notification-access flow in Task 1.
- Current checkpoint: finish Task 1 evidence, then proceed through Tasks 2–6 in order under the user's latest authorization. Do not call a task complete or start dependent work before its acceptance evidence passes. External credentials, production deployment, store publication, and release identity still require real owner inputs.

Task 1 contracts must encode H02 event/snapshot/epoch separation, H03 action outcomes, H04 serialization/presence, H05 routing precedence, H06 cache scope, H10 operation/reservation states, H12 bounds, and H14/H19 versioning/build rules. Defining these contracts does not authorize implementing later runtime features.

## Task 2: Local notification ingestion and privacy

**Depends on:** accepted Task 1.

- Inputs/scope: SDD Task 2 and notification contracts; ingestion, sensitivity filtering, clustering, encrypted storage, TTL, and tombstones.
- Outputs: `:notification-ingest`, `:local-ai`, `:data-local`, service declaration, and explicit notification-access onboarding.
- Acceptance: every SDD Task 2 criterion, including denied/revoked access, update/removal ordering, process restoration, retention, and locked-device privacy. No cloud requests or notification-body logging.

## Task 3: Deterministic intent resolution and execution

**Depends on:** accepted Tasks 1–2.

- Inputs/scope: SDD Task 3, capability matrix, typed action contracts, and signed registry fixtures.
- Outputs: `:intent-router`, `:action-resolver`, `:registry`, `:android-executor`, narrow package queries, and preview contracts.
- Acceptance: every SDD Task 3 criterion, including ambiguity, URI validation, registry tampering, missing handlers, and resolution changes. Use documented intents/APIs and target/system confirmation surfaces.

## Task 4: Optional cloud reasoning

**Depends on:** accepted Tasks 1–3 and working local fallbacks.

- Inputs/scope: SDD Task 4, wire contracts, routing/privacy rules, budgets, and owner-supplied cloud configuration.
- Outputs: `:network`, `:cloud-policy`, `services/reasoning`, infrastructure definitions, conformance tests, retention configuration, and aggregate monitoring.
- Acceptance: every SDD Task 4 criterion. Only eligible, redacted, consented content leaves the device. Enforce budgets using current configured prices. Cloud failure preserves local operation.

## Task 5: Compose launcher integration

**Depends on:** accepted Tasks 1–4.

- Inputs/scope: SDD Task 5 and existing use cases; app drawer/search, notification summaries, intent input, previews, settings, and offline/error states.
- Outputs: `:feature-home`, `:feature-intent`, `:feature-settings`, integrated UI, and end-to-end evidence.
- Acceptance: every SDD Task 5 criterion, including external-activity handoff, cancellation, process restoration, privacy, UI semantics, and performance on an agreed device. The blank shell is only the starting point.

## Task 6: Release validation and operations

**Depends on:** accepted Tasks 1–5.

- Inputs/scope: SDD Task 6 and the integrated application/backend.
- Outputs: threat model, data/privacy map, Play evidence, SBOM, release/rollback runbooks, and verification report.
- Acceptance: every SDD Task 6 criterion, backed by actual security, device, load, soak, deletion, and rollback evidence. Record unavailable checks accurately. Publishing and production changes follow user authorization.
