# Project Zero: implementation handoff

## Read first

Read applicable `AGENTS.md` files, then `SYSTEM_DESIGN.md`, `RELIABILITY_SECURITY.md`, `BACKLOG.md`, `ACCEPTANCE_STATUS.md`, and this guide. The repository is `C:\Users\prabu\Desktop\Projects\project-zero`; the Android Gradle root is `android-client`.

The SDD defines architecture and detailed acceptance criteria. RELIABILITY_SECURITY.md provides mandatory H01–H20 controls and governs explicit corrections to earlier examples. The backlog follows the same six tasks. This guide and ACCEPTANCE_STATUS.md record actual status. Explicit user instructions control scope; do not silently weaken constraints to satisfy an outdated example. The latest user request authorizes Grok-led implementation toward all six tasks, proceeding sequentially after evidence review.

The global `C:\Users\prabu\.codex\AGENTS.md` was empty at the September 16, 2026 inspection. No repository AGENTS.md was found then. Recheck for newer directives when resuming.

## Current state and evidence

On September 16, 2026, Grok CLI produced a single `:app` module, blank Compose Surface, HOME/DEFAULT and LAUNCHER filters, Gradle wrapper, and version catalog. Antigravity did not complete its run.

Observed pins: minSdk 29; compile/target SDK 36; provisional application ID `app.projectzero.launcher`; Gradle 8.13; AGP 8.13.2; Kotlin 2.3.21; Compose BOM 2026.06.01. These describe the existing project, not the latest available versions.

The prior orchestrator independently ran `assembleDebug check lint`: exit 0, BUILD SUCCESSFUL. The saved lint report had 0 errors and 8 warnings. Unit test tasks reported NO-SOURCE; no unit-test coverage was demonstrated. Source and merged debug/release manifests contained HOME/DEFAULT and no accessibility, overlay, broad package visibility, or notification services.

No Android device or emulator was available for runtime verification. Actual blank-screen rendering and HOME selection remain unverified. The application ID is not approved for release. At handoff the Android client and README changes were uncommitted; inspect git status and preserve existing changes.

**Completion assessment:** the former backlog's Compose scaffold has build/static evidence. SDD Task 1 remains incomplete: domain types, canonical schemas/protobuf, validation tests, architecture checks, dependency locks, and CI are still required. A successful APK build does not complete these requirements.

## Architectural boundaries

- No AccessibilityService, simulated taps, scraping, overlays, hidden APIs, or arbitrary background activity launches.
- Notification content stays local by default. Never log notification bodies or upload raw notifications as a shortcut.
- Follow the SDD prohibition on QUERY_ALL_PACKAGES. Add only needed package/intent queries when supported capabilities are implemented.
- Notification ingestion starts in Task 2, with explicit system notification-access grant. BIND_NOTIFICATION_LISTENER_SERVICE protects the service through its android:permission attribute; it is not a runtime permission request.
- Model output remains typed and non-executable. Validate actions locally, enforce preview/user gesture requirements, resolve again before launch, and leave commits such as Send/Save/Call to the target/system UI.
- Framework imports stay outside the domain module. Tokens, SDK paths, secrets, and signing material stay out of version control.

Implementation references: [notification listener](https://developer.android.com/reference/android/service/notification/NotificationListenerService), [package visibility](https://developer.android.com/training/package-visibility/declaring), [Compose setup](https://developer.android.com/develop/ui/compose/setup). Revalidate platform behavior when changing SDK/dependency versions.

## Contract decisions to encode during Task 1

The hardening review resolves the previous broad ambiguities below. Encode these decisions in canonical contracts and compatibility fixtures before implementing adapters:

1. H04: strict requests/model outputs; response readers may tolerate unknown envelope metadata, never executable/slot extensions. Producer schemas stay strict. Reject duplicate keys, invalid numbers, and oversized payloads.
2. H04/H09: tokens live only in transport metadata; required JSON null redacted fields map to absent optional protobuf values, distinct from empty strings. Explicitly map int64/presence rather than assuming default protobuf JSON equivalence. The SDD slots example now uses typed fields.
3. Complete RegistryService.GetRegistry messages and domain result/reason definitions. Add H02 separate event revision, snapshot revision and data epoch; H03 at-most-once action attempts and OUTCOME_UNKNOWN; H10 durable operation/reservation state types. Do not implement later network/execution features in Task 1.
4. H05: supported-grammar confidence below 0.55 clarifies/rejects; unsupported grammar may use cloud only when local redaction and cloud locale support are independently validated. Complexity never overrides privacy/consent/budget gates.
5. H06/H12/H14/H19: encode cache scope/version bindings, bounds and token-aware batch selection, migration compatibility, and reproducible build gates. Status remains NOT_STARTED until implemented and verified.

The coding model should resolve routine details in documentation and tests and explain the choices. Ask the owner only for an actual product choice or missing external input. Do not treat illustrative snippets as already validated contracts.

## Next coding model's work sequence

1. Inspect existing changes and preserve the Android scaffold. Do not regenerate the project or overwrite unrelated edits.
2. Complete remaining SDD Task 1 requirements. Android module paths are relative to android-client; repository CI configuration stays at the repository root.
3. Preserve the blank Compose shell as an accepted scaffold exception. Functional launcher UI belongs to Task 5 and notification access to Task 2.
4. Encode the specified contract decisions, then implement domain/contracts/checks/CI and required tests. Update ACCEPTANCE_STATUS.md with exact evidence and remaining gaps.
5. Run required checks and inspect merged manifests. If a device/emulator exists, install and launch the debug APK and verify blank rendering and HOME eligibility. Do not silently change the user's primary device's default launcher.
6. Report complete, failed, and unverified criteria separately. After Task 1 acceptance evidence passes, continue sequentially through authorized tasks. Keep missing external inputs and release gates visible.

## Verification and reporting

Configure the SDK through environment variables or ignored local.properties, then from the repository root run:

```powershell
Set-Location android-client
.\gradlew.bat assembleDebug check lint test --console=plain
```

The prior machine used JDK 21 and `C:\Users\prabu\AppData\Local\Android\Sdk`. Check availability rather than embedding the path in tracked files. Satisfy the SDD clean-checkout reproducibility gate after locks and CI exist.

Report scope/files, SDD criteria, exact commands/exit codes, meaningful test counts, warnings, device/API used, unavailable checks, design decisions, and remaining owner inputs. A build cannot prove runtime behavior, privacy, or missing test coverage. Keep evidence free of credentials and notification content. Checkpoint commits to main are now explicitly authorized; production deployment and publishing still require their own validated inputs.

## Copy-paste brief for a coding model

> Continue Project Zero in C:\Users\prabu\Desktop\Projects\project-zero. Read applicable AGENTS.md files and CONTINUE_WITH_GROK.md, docs/SYSTEM_DESIGN.md, docs/RELIABILITY_SECURITY.md, docs/BACKLOG.md, docs/ACCEPTANCE_STATUS.md, and docs/IMPLEMENTATION_HANDOFF.md. Preserve the android-client Compose shell and existing user changes. Use Grok CLI for coding. Complete the remaining SDD Task 1 foundation, including contracts, architecture checks, dependency locks, CI, and acceptance evidence. Encode Task 1 H-controls. Run build/tests/lint, manifest scans, and available device checks; update evidence accurately. Then proceed through Tasks 2–6 sequentially as each task's evidence passes. Keep signing identity and cloud credentials provisional until supplied. Do not publish or deploy production without required inputs and acceptance. Never describe unimplemented or untested protections as complete or bulletproof.
