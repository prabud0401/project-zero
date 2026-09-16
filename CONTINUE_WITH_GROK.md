# Resume Project Zero in another IDE agent

## Copy-paste prompt

> You are the engineering orchestrator for Project Zero. The user authorized Grok CLI to implement all six tasks and H01–H20, one task at a time, with real verification before advancing. Work in this repository. First read applicable AGENTS.md files, this file, README.md, docs/SYSTEM_DESIGN.md, docs/RELIABILITY_SECURITY.md, docs/BACKLOG.md, docs/ACCEPTANCE_STATUS.md, and docs/IMPLEMENTATION_HANDOFF.md. Read any Markdown chat export in the repository root for historical context; if it conflicts with a later explicit user instruction or the current canonical docs, follow the later instruction and record the decision. Inspect git status and the latest commit before changing files. Use installed Grok CLI (`grok`) for implementation; orchestrate, review, run checks, correct Grok errors, and update evidence. Start with the unfinished SDD Task 1 foundation. Do not claim the earlier Compose scaffold satisfies Task 1. Advance to Tasks 2–6 only after the preceding task's acceptance evidence passes. Never use AccessibilityService, simulated taps, QUERY_ALL_PACKAGES, overlays, hidden APIs, raw notification uploads, or silent data-changing actions. Keep local functionality working when cloud is unavailable. Record test commands, device/SDK coverage, warnings, and unverified criteria honestly. Commit and push reviewable checkpoints to `main` as authorized. Do not publish to Play or deploy production infrastructure without verified credentials, owner inputs, and release gates. Preserve existing work and avoid inventing approvals, test results, costs, or security guarantees.

## Exact checkpoint at this handoff

- Repository: `C:\Users\prabu\Desktop\Projects\project-zero`; branch `main`; remote `origin` is `https://github.com/prabud0401/project-zero.git`. Check the current Git state rather than assuming it remains unchanged.
- The `android-client` Compose shell exists with HOME/DEFAULT activity, Gradle wrapper, minSdk 29, compile/target SDK 36, and provisional application ID `app.projectzero.launcher`.
- Historical build: `assembleDebug check lint` passed; lint reported 0 errors and 8 warnings. Unit test tasks were NO-SOURCE. No device/emulator runtime verification happened. Re-run checks after changes.
- Docs align the six tasks and define H01–H20 controls. `docs/ACCEPTANCE_STATUS.md` is the evidence register. Runtime controls remain unverified.
- A Grok CLI attempt for Task 1 was started in this session but stopped to create a clean Git handoff. It had read project files and had not changed the working tree at interruption. Inspect again to confirm.
- A checkpoint commit/push of the scaffold and documents is being created in this handoff. Use `git log -1` and `git status` for its exact commit. A commit is a saved state, not SDD Task 1 acceptance.
- The owner plans to download this chat as Markdown into the repository root. Its filename/content is not available here. Once it exists, review for secrets, then commit/push it separately if desired. Do not fabricate a transcript.

## Execution order

1. **Task 1:** pure domain contracts, strict/versioned JSON and protobuf, reason codes, invariants, architecture tests, dependency locks/verification, CI, and meaningful tests under the existing `android-client` root. Resolve wire/epoch/action/idempotency contracts. No listener, cloud client, or functional launcher UI yet.
2. **Task 2:** notification service after system grant, local privacy filter, clustering, encrypted storage, retention/tombstones, denied/locked states. Never log/upload raw notification text.
3. **Task 3:** deterministic router, signed registry, narrow package visibility, safe intent builders, immutable preview, and at-most-once launch attempt. Target/system app owns Send/Save/Call.
4. **Task 4:** optional authenticated cloud reasoning, redaction, residency/retention, durable idempotency, atomic installation/fleet budget admission, isolated caches, fault fallback, measured cost. External GCP/model inputs are pending; never claim production integration verified without them.
5. **Task 5:** Compose home/app drawer/search/summaries/intent previews/settings, accessibility semantics, privacy/lock behavior, end-to-end tests and measured performance.
6. **Task 6:** compliance/security/recovery drills, device matrix, soak, supply-chain evidence, price replay, runbooks, and release report. Keep release NOT READY until applicable gates have evidence and owner inputs.

At each stage, require exact SDD criteria and assigned H-controls. Inspect merged manifests/diffs, run relevant tests, update `docs/ACCEPTANCE_STATUS.md`, then commit a coherent checkpoint. Missing devices, signing identity, GCP region, model terms, fleet ceiling, or release owner block only dependent criteria; continue independent work. Never mark them VERIFIED without evidence.

## Grok CLI invocation

Use one scoped prompt per task. On this Windows host the installed CLI accepted `grok --always-approve --no-subagents --output-format plain -p $prompt` from PowerShell. It emitted unrelated MCP connector authentication warnings during startup; those did not stop the earlier scaffold implementation. Verify CLI authentication and wait for completion before reviewing/dispatching the next task.

The repository documentation defines architecture, while user instructions and platform rules take priority. Build/release claims need actual commands, device observations, and cost/security measurements. A green build or Grok summary is not full acceptance.
