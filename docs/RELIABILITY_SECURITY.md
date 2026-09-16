# Reliability, security, and evolution requirements

**Version:** 1.0, design review dated 2026-09-16.
**Status:** mandatory design requirements; implementation and release evidence are pending.

This document supplements SYSTEM_DESIGN.md and is normative for controls H01–H20. Where an older SDD example conflicts with a control here, this document governs that behavior; reconcile the canonical contracts and tests before implementing it. Explicit user instructions govern authorization. The six-task sequence remains unchanged.

No document proves a system secure, infinitely scalable, or future-proof. The objective is bounded behavior, isolated failures, controlled evolution, and independently reviewable evidence. “No limit” means thorough engineering; it does not remove Android restrictions, privacy rules, resource limits, or the existing cost target. Limits prevent one fault from exhausting the system.

## H01 — Home survives optional-feature failure

**Owner tasks: 1, 2, 5.** Home/app launch must not wait for notification storage, cloud authentication, model loading, telemetry, remote configuration, or network calls. Use supervised background work and explicit degraded states. A notification database failure leaves app launch and a route to Android home-app settings usable. Preserve user settings separately from disposable notification/cache data.

Repeated optional-feature initialization failure suppresses that feature for the next session and offers an explicit retry. Do not catch fatal runtime errors indiscriminately or build restart loops. Before first unlock, do not access private notification data. Direct Boot support, if enabled, exposes only a minimal non-sensitive surface. Relocking after first unlock does not itself make credential storage unavailable. [Direct Boot guidance](https://developer.android.com/privacy-and-security/direct-boot).

**Evidence:** unavailable DB, invalid key, model-load failure, stalled network, disk-full, and cancelled child job leave home/app launch usable. Reboot/unlock and process-death tests cover initialization.

## H02 — Notification identity and ordering are distinct

**Owner tasks: 1, 2.** Identify callbacks by local profile/user scope plus framework notification key. Preserve eventId across updates, advance event revision on normalized content change, and assign a new eventId to a repost after removal. Duplicate unchanged callbacks are no-ops.

Maintain a separate persistent snapshotRevision for the personal-profile notification store. Every accepted post/update/removal or relevant policy change advances it transactionally. SummaryBatchRequest.baseRevision refers to that snapshot, not a package counter. V1 batches never cross profiles. Bind pending requestId locally to dataEpoch and snapshotRevision. Apply results only while the request remains pending, both values match, and all members remain live and eligible. Unrelated updates may conservatively discard work.

Removal invalidates membership and pending work before publication. Erase, listener revocation, and store reset advance dataEpoch and invalidate every outstanding request. Reconnect reconciles current OS notifications; it cannot reconstruct missed history. Mark incomplete history instead of inventing it.

**Evidence:** randomized post/update/remove/repost, duplicates, cross-package batches, late results after erase, reconnect, and process death between transaction and publication. Removed content never reappears from stale work.

## H03 — At-most-once action attempts

**Owner tasks: 1, 3, 5.** Ordinary intents cannot guarantee exactly-once completion in another app. Locally allow at most one launch attempt per actionId. Atomically consume confirmation before startActivity. A crash after consumption never triggers automatic replay. Ambiguous dispatch yields OUTCOME_UNKNOWN and requires a new explicit request. HANDED_OFF means Android accepted a launch, not that a message was sent or an event saved.

Executable proposals stay in memory. Bind previews to canonical typed parameters, target identity, registry/policy versions, and fresh confirmation. Recheck foreground/lock state, capability, and resolution at dispatch. Use monotonic elapsed time for five-minute expiry; clock edits cannot extend validity. Process recreation/reboot invalidates proposals. Double taps cannot reuse confirmation.

A chooser owns final target selection; previews must reflect that. Generic attachment sharing cannot guarantee a recipient. Use target-owned recipient selection unless a documented provider contract supports addressing. Resolve contacts through explicit input or a system picker; do not add broad contact access.

**Evidence:** repeated/concurrent dispatch, lock after preview, package update/removal, clock jumps, and process termination around launch. No automatic duplicate or fabricated completion status.

## H04 — One canonical wire contract

**Owner tasks: 1, 4.** Requests and model output use strict validation: reject unknown fields and unsupported versions before side effects. Compatible response readers may ignore unknown non-executable envelope metadata, but nested action/slot objects remain closed and capability-allowlisted. Unknown fields never influence execution. Server output passes its strict schema before transmission. New request fields require negotiated server support.

Authentication belongs only in headers/transport metadata. Required JSON redactedTitle/redactedBody keys may be null; null maps to absent optional protobuf strings, while empty string remains distinct. Reject duplicate JSON keys, invalid UTF-8, non-finite numbers, overflow, invalid enums, and oversized nesting/collections. Enforce compressed/expanded byte limits while streaming.

REST/protobuf adapters produce the same validated domain representation. Default protobuf JSON is not automatically equivalent to the canonical REST encoding: int64 and presence need explicit mapping. Replace the illustrative string-valued slots map with typed optional fields before v1 ships. After release never reuse field numbers/types. Complete registry service messages and closed result/reason types in Task 1.

Define required/allowed slots per capability and clarify contradictory/missing values. Cloud cannot choose packages, phone/email identifiers, or executable URIs; local adapters rehydrate only request-authorized placeholders. Model schema compliance does not establish semantic safety.

**Evidence:** shared JSON/protobuf fixtures, null/empty/missing cases, unknown fields at each boundary, duplicate keys, Unicode/number limits, and forbidden capability-slot combinations.

## H05 — Privacy and consent precede routing

**Owner tasks: 1–4.** Evaluate hard privacy/device-policy gates, cloud consent, supported redaction policy, local rule/cache, confidence/complexity, then network/deadline/budget admission. Notification permission applies to notification processing, not independently entered user intents. Revocation immediately cancels notification work. User input still passes all relevant privacy gates.

For supported grammar: confidence below 0.55 clarifies/rejects; 0.55–0.819 may use cloud after all gates; >=0.82 resolves locally only when required slots are unambiguous. Unsupported local grammar may use cloud without inventing a local score only when the on-device redactor and cloud model are separately validated for that locale. Temporal complexity does not override below-0.55 clarification for supported grammar. Summary complexity is separate.

Self-reported model confidence is not calibrated probability. Thresholds 0.82/0.94/0.98 are initial values requiring held-out calibration by language/capability/model. Unsupported redaction remains local. Regexes cannot prove arbitrary text anonymous. Record residual risk and default uncertain cases to local handling.

**Evidence:** boundaries, unsupported languages, ambiguous contacts/dates/DST, opt-out during upload, and adversarial private text. Prohibited classes make zero cloud calls in the acceptance corpus.

## H06 — Caches never transfer authority or identity

**Owner tasks: 1–4.** Embeddings/fingerprints are sensitive derived data. Partition device caches by profile/dataEpoch and server exact caches by authenticated installation/consent epoch. No cross-installation reuse of user-derived results in v1. Server semantic reuse is limited to curated public templates; user-learned cross-installation semantic caching is disabled.

Keys bind model/prompt/redaction policy/registry/schema versions, locale, capability, and normalized structure. Values contain templates and positional source references, never reusable requestId/eventId/clusterId, executable actions, URI grants, or confirmation tokens. Rebuild metadata/member IDs against each new request, validate placeholders/source membership, and repeat privacy/policy checks. Similarity never authorizes execution or imports old recipient/date/message slots.

Server caches are encrypted, TTL-bound, and deletable by installation/epoch. Salt/pepper changes invalidate entries. Rehydrated local display summaries belong in a separate encrypted store subject to source retention, not the non-PII template cache. Cache-hit goals require measurement under these isolation rules.

**Evidence:** identical redacted inputs across installations cannot leak IDs/placeholders/results. Test erase, profile/version changes, changed negation/recipient/date, and expired source members.

## H07 — Storage, backup, and deletion have explicit behavior

**Owner tasks: 1, 2, 4, 6.** Room alone does not provide database encryption. Select a maintained encryption layer; protect its key using Keystore-backed wrapping where supported. Record actual device capabilities. Key failure never downgrades to plaintext. Preserve settings while quarantining/recreating unreadable disposable notification storage with an explanation. [Android Keystore](https://developer.android.com/privacy-and-security/keystore).

Explicitly exclude notification bodies, summaries, embeddings, salts, tokens, and keys from cloud backup and device transfer using supported-version rules. Test restore/transfer rather than assuming one manifest flag covers every path. [Backup guidance](https://developer.android.com/privacy-and-security/risks/backup-best-practices).

Enforce retention on every read/render/upload even if cleanup is delayed. Raw events become inaccessible at removal or 24 hours. Remove deleted members from summaries immediately; seven days is only an upper bound for still-valid display summaries. Physical cleanup runs when execution is available; do not promise flash overwrites or recall of already submitted inference.

Erase cancels work, advances dataEpoch, deletes local data/keys, revokes the cloud principal, and requests server deletion with a receipt. Opt-out separately closes admission, advances consent epoch, and deletes server user-derived caches. In-flight results cannot be stored/applied after revocation. Offline erase reports remote deletion pending; retain only a protected delete-only credential until acknowledgement/expiry. If all credentials are removed before remote deletion, disclose reliance on server TTL. V1 disallows persistent payload backups; later exceptions require explicit backup-deletion policy.

**Evidence:** invalid key, corruption, lock/reboot, cross-device restore, disk-full, delayed cleanup, offline erase, and deletion/response races. Distinguish logical deletion from physical retention.

Lock-state changes invalidate sensitive action previews and remove personal text from the current UI, semantics, recent-task previews, and applicable capture surfaces. Storage encryption does not provide presentation redaction. Verify lock/unlock and screen-sharing transitions on supported devices rather than relying on one window flag.

## H08 — Notification text and model output are hostile input

**Owner tasks: 2–5.** Copy bounded primitives; never execute RemoteViews, render untrusted HTML, follow text links automatically, or treat notification instructions as policy. The reasoning service has constrained generation only: no model tools or browsing.

Validate generated source references, placeholders, lengths, slot types, and policy after inference. Show summary provenance and access to the original; unsupported claims fall back to local extractive summaries. A cancelled notification PendingIntent fails safely. Use the creator-supplied PendingIntent only on user gesture, without privileged fill-in data. Project-created PendingIntents are explicit and immutable unless a documented contract needs otherwise. [PendingIntent security](https://developer.android.com/privacy-and-security/risks/pending-intent).

**Evidence:** malicious notification fixtures, requests to expose other notifications, forged placeholders/IDs, Unicode controls, and hostile URIs. Content never acquires execution authority.

## H09 — Application authentication differs from Cloud Run IAM

**Owner tasks: 1, 4, 6.** V1 uses application-level installation authentication at the public API entry. Installation tokens are not Google IAM tokens. Validate them before cache/budget/inference access. Service-to-service calls use least-privilege workload identity; no service-account key enters the APK. A gateway/private backend variant must identify each validator and prevent bypass. [Cloud Run end-user authentication](https://docs.cloud.google.com/run/docs/authenticating/end-users).

Registration uses a short-lived single-use server challenge and verified Play Integrity verdict bound to its payload. Check expected package/signing identity, request hash, freshness, and applicable verdicts. Installation tokens have a maximum 15-minute lifetime with issuer/audience/expiry/key-id/scope checks and server-side revocation/consent epoch. Renewal revalidates integrity and is rate-limited; failure disables cloud only. Bearer-token theft remains a residual risk mitigated by short life, TLS, storage protection, revocation, and quotas.

Use standard Integrity requestHash semantics; the HTTP nonce is not a classic Integrity nonce. Do not put raw private text in integrity metadata. [Standard requests](https://developer.android.com/google/play/integrity/standard).

Require platform TLS certificate/hostname validation and deny cleartext production endpoints. Never add trust-all certificate code to recover from a network error. Bind a transport attempt to a bounded-age sent-at timestamp and nonce: initial policy allows at most 120 seconds of clock skew and retains nonce digests for ten minutes. Clock mismatch returns local fallback with a recoverable error; it does not weaken authentication. Logical retries keep requestId/payload but use new attempt metadata.

**Evidence:** forged/expired/revoked/wrong-audience tokens, stale challenges, hash mismatch, signing-key rotation, missing Play services, renewal storms, and backend bypass attempts.

## H10 — Durable idempotency and atomic budget reservations

**Owner tasks: 1, 4.** Use managed transactional persistence for installation state, revocation, operation deduplication, and reservations. Instance memory is only an optimization. Key operations by authenticated installation + endpoint + requestId and bind a canonical payload digest. Same ID/payload joins or returns the operation; different payload fails CONFLICT. Fresh transport nonces are separate from logical operation IDs.

States: RECEIVED, RESERVED, DISPATCHED, SUCCEEDED, FAILED_SAFE, OUTCOME_UNKNOWN. Atomically reserve maximum input/output cost before dispatch, then settle actual usage. Duplicates across instances cannot reserve/dispatch twice. Keep operation records at least ten minutes; clients never automatically retry older requests. If provider dispatch might have occurred, no redispatch without a verified provider idempotency contract. Hold ambiguous reservations for reconciliation; if usage cannot be established, account the reserved maximum. Never refund uncertain timeouts and repeatedly admit new inference.

An unavailable ledger or a restore missing recent reservations closes cloud admission until reconciliation or a safe accounting boundary. Home remains local. TTL cleanup is not an expiry/revocation enforcement mechanism.

**Evidence:** concurrent duplicates, dropped responses, process death around dispatch, partial writes, day rollover, and datastore recovery. Prove conservative accounting and at-most-once local provider dispatch, not exactly-once provider execution.

## H11 — Cost reports include fixed costs and abuse

**Owner tasks: 4, 6.** The $0.002 target is a measured cost objective. Hard admission bounds budgeted inference/estimated attributable platform cost, not the final cloud invoice. Use server UTC and define a user unit as an admitted installation-day; reinstall/fraud does not preserve a human-level quota. Global registration/admission throttles bound exposure.

Installation and fleet budgets reserve before inference. Version SKU, tokenizer, regional rates, currency, and effective date. Unknown/stale price configuration disables inference. Count retries, auth/integrity, idle capacity, datastore, cache, logs, networking, and security infrastructure. Do not retry generation merely to repair malformed JSON. Fixed costs may violate the target at low scale; disclose this and disable cloud when the approved envelope cannot be met.

Combine infrastructure quotas, bounded instance/concurrency settings, admission control, and available billing controls. Cloud Billing spend caps currently have preview status, service/project scope restrictions, delayed enforcement, and in-flight/persistent-cost exceptions. They cannot prove an exact per-user ceiling. [Spend-cap limitations](https://docs.cloud.google.com/billing/docs/how-to/budgets-spend-caps).

**Evidence:** maximum token lengths, misses, low active-user counts, registration abuse, price changes, repeated auth, and sustained errors. Publish mean/p95 and fixed/variable totals with denominator and price version. Cache-hit targets remain hypotheses until measured.

## H12 — Bound queues, allocations, and retries

**Owner tasks: 1, 2, 4, 5.** Initial local bounds: 256 ingestion items, 1,000 active normalized events per personal profile, 50 events per summary batch, one in-flight summary and one user-intent request per installation. Existing cache byte/entry caps remain maxima; enforce source retention too. These are initial policy limits to validate, not measured capacity claims.

Coalesce by notification key; prioritize removals. On overflow, invalidate affected summaries, mark incomplete state, and resync a bounded OS snapshot. Never preserve content known to be removed. Disable enrichment temporarily if normalization/storage cannot keep up; preserve home/app launch. WorkManager data never contains bodies, tokens, or contacts.

The 30-second summary coalescing window is opportunistic, not an exact background scheduling guarantee. Interactive intents use separate deadlines. Bound server decompression, nesting, CPU, provider concurrency, and retries. Overload returns a stable local-fallback result. [Persistent-work guidance](https://developer.android.com/develop/background-work/background-tasks/persistent).

Fifty events is a schema maximum, not a promise that fifty events fit the token budget. Before dispatch, estimate actual serialized prompt overhead, IDs, text, and minimum valid output. Reduce the batch deterministically without hiding excluded events from local UI; if even four related events cannot fit the summary budget, summarize locally. Never truncate JSON, silently clip required slots, or exceed reserved output limits to make a batch succeed. Validate this against the selected tokenizer and worst-case Unicode input.

**Evidence:** 10x measured normal bursts, oversized inputs, low RAM/storage, constrained battery, cancellation, and provider stalls. Record queue/memory/CPU/battery behavior; tune limits through reviewed policy versions.

## H13 — Discover platform capabilities

**Owner tasks: 2, 3, 5, 6.** Test API 29, major permission/visibility boundaries, compile/target SDK, and the latest stable Android available at release. Include two physical OEM families and a constrained reference device alongside emulators. Publish actual tested coverage. Managed/private/secondary profiles, suspended/archived packages, denied HOME role, absent handlers, and disconnected/redacted notifications yield explicit unavailable states.

V1 processes personal-profile content only. Android 15 may redact OTP notification content from untrusted listeners; accept unavailable data without bypass. [Android 15 notification protections](https://developer.android.com/about/versions/15/behavior-changes-all).

Refresh capabilities after package changes and before launch. Unsupported provider links are disabled, never guessed. Attachment URI grants must survive documented target consumption; do not revoke immediately after startActivity or claim a universal fixed timeout is safe. Specify/test lifetime per supported share contract.

**Evidence:** device/API matrix, package-update and absent-handler fixtures, profile denial, redaction, delayed attachment consumption, and verified provider contracts.

## H14 — Migrations and rollback preserve user settings

**Owner tasks: 1, 2, 6.** Version DB, wire/domain contracts, model, prompt, registry, policy, and cache independently. Export schemas and test supported upgrades with populated fixtures, interruption, disk-full, and key failure. Never destructively migrate settings. Disposable caches may be recreated only under documented policy. [Room migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions).

Deploy additive server changes before clients. Support current and previous production protocol generations for at least 90 days after successor release, except emergency security restrictions. Unsupported clients retain local home and receive a clear cloud update requirement. An Android rollback plan uses a newer corrective release with compatible data handling; it does not assume installation of an older APK is possible.

**Evidence:** old/new client/server fixtures, migration matrix, interrupted rollout, forward correction, and rejected registry downgrades.

## H15 — Remote configuration cannot expand authority

**Owner tasks: 1, 3, 4, 6.** Registry/config envelopes carry signed canonical payload, monotonic version, key-id, schema, expiry, and minimum client. Ship a trust root and tested key-rotation overlap; use maintained cryptographic libraries. Reject tampering, rollback, malformed and future-incompatible versions. Prefer structured bounded URI templates over remote regexes. No downloaded executable plugins/scripts.

A cached registry is usable only while valid under installed policy/expiry. Expiry or revocation disables provider routes while built-in documented system routes remain. Kill switches may disable cloud/capabilities but cannot add permissions, broaden upload eligibility, or weaken confirmation. Root-key compromise requires an app update or separately trusted recovery path.

**Evidence:** rotation, revoked key, expired offline registry, rollback, malformed patterns, incompatible client, and total remote-config outage.

## H16 — Model upgrades require evaluation

**Owner tasks: 1, 3, 4, 6.** Isolate provider SDKs behind adapters and keep domain/wire contracts vendor-independent. Choose one default provider for v1; replacements satisfy the same residency, retention, cost, schema, and evaluation rules. Never silently switch region/provider.

Maintain synthetic/consented datasets by locale/capability for privacy, injection, slot fidelity, source grounding, ambiguity, negation, time interpretation, and fallback. Freeze model/prompt/policy/dataset versions. Critical unauthorized-action/privacy fixtures require zero failures; this is a test gate, not a guarantee of zero real-world risk. Document sample counts and coverage gaps. User notifications are not training/evaluation data without separate consent.

Regression retains/restores the approved version; if unavailable, disable cloud. Verify provider data handling in configuration and contract before release. Disabling application logs alone does not prove provider retention controls.

Downloadable on-device models require signed metadata, digest/size verification, compatible format/runtime, bounded installation storage, atomic activation, and a tested previous model or deterministic fallback. Model assets cannot contain executable application plugins. Failed download, low storage, or incompatible hardware must not block home startup.

## H17 — Reliability claims require defined measurements

**Owner tasks: 1, 4–6.** Existing SLOs are targets, not observed results. Cold-home latency runs from activity start to first usable local frame; routing from accepted input to proposal/clarification; cloud from dispatch to validated reply. Report p50/p95/p99, errors/timeouts, sample size, environment, cold/warm state, and window. Include failures in denominators; stale summaries are not fresh responses.

Before beta, run at least 1,000 cold-home launches across the declared matrix, at least 200 on the constrained reference device, every applicable H-control fault, and the SDD 24-hour soak. This is an initial repeatable test floor, not proof of rare-failure rates. Production uses the SDD crash-free target over rolling 28-day sessions, separate ANR/degraded-capability rates, and privacy-safe instrumentation. Insufficient telemetry means insufficient evidence.

Known unauthorized action, cross-installation leakage, private-data exposure, or settings loss blocks release and pauses the affected capability. Reliability regression pauses rollout; aggregates cannot conceal failing device cohorts.

## H18 — Exercise recovery across dependencies

**Owner tasks: 4, 6.** Runbooks cover model/region outage, registry/token-key compromise, corrupt/deleted datastore, runaway spend, bad app update, and leaked credentials. Assign incident/release ownership before production.

Cloud disablement and registry capability revocation have a 15-minute operational recovery target measured from operator action in a drill. Offline clients cannot receive immediate revocation; installed expiry/deny rules bound residual risk. Region failover never violates residency. Uncertain recovered ledgers keep admission closed. Home/app launch remains independent of cloud; preserve settings through compatible migration and treat caches as disposable.

**Evidence:** recorded injected fault, detection/action/restoration times, data/cost impact, and residual failure modes. Untested runbooks do not pass acceptance.

## H19 — Control dependencies and architectural growth

**Owner tasks: 1, 6; maintained thereafter.** Pin toolchains/dependencies, wrapper checksum, CI actions, and container images. Require dependency locks/verification, SBOM, secret/dependency/static scans, isolated least-privilege CI, and protected release signing. Updates run relevant contracts/migration/device tests; newer-version notices alone do not justify a wholesale upgrade.

Review Android/Play changes, provider contracts, security advisories, and model deprecations monthly and before releases. This is a process requirement, not a newly created scheduled automation. Record owner, due date, replacement path, and compatibility evidence.

Begin with a modular client and one stateless reasoning service plus managed transactional persistence. Add services/regions only for measured throughput, latency, isolation, or residency needs. New capabilities enter through typed contracts, local policy, registry tests, and review. Decisions record rationale, alternatives, limits, and measurable revisit triggers.

## H20 — Evidence is the completion criterion

**Owner tasks: all.** Maintain a requirement-to-evidence register for H01–H20 and SDD acceptance items. Fields: owner task, status (NOT_STARTED / IMPLEMENTED_UNVERIFIED / VERIFIED / BLOCKED), evidence path, build/commit identifier, environment, date, and limitations. One green build cannot mark every requirement verified. Security-sensitive contracts/policy require independent review before release.

Task 1 defines epoch/revision/action/operation types, closed slot/reason contracts, canonical encoding, bounds, version/migration rules, and CI gates. Tasks 2–5 implement assigned behavior; Task 6 verifies integration. Absent tests/devices, unknown prices, provisional signing identity, missing reference devices, and operational ownership remain explicit open items.

Release requires applicable SDD/H gates verified, no unresolved critical/high findings, and owner review of residual risks. Current implementation is a blank shell: none of the new runtime protections is claimed implemented or validated.

## Owner and environment inputs

Routine foundation work can proceed without these, but dependent integration/release needs: final application ID/signing identity; GCP project and approved region/residency; model/provider data-handling configuration; fleet spend ceiling; reference devices; supported locales; incident/release owner; and release authorization. Never invent credentials or treat placeholders as approved choices.
