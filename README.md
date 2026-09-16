# Project Zero

Project Zero is an intent-driven, privacy-first Android launcher. Its design uses
only documented Android intents, verified App Links, deep links, and system APIs;
it never performs UI automation or uses `AccessibilityService`.

The implementation-ready architecture, wire contracts, deterministic action
resolution rules, AI routing policy, and six-stage agent backlog are defined in
[the System Design Document](docs/SYSTEM_DESIGN.md).

Coding agents should start with [the implementation handoff](docs/IMPLEMENTATION_HANDOFF.md)
and [the aligned six-task backlog](docs/BACKLOG.md). The Compose shell and the
SDD Task 1 domain/contracts/architecture-test/CI foundation are in tree; see
[acceptance status](docs/ACCEPTANCE_STATUS.md) for what is and is not verified.
Do not start Task 2 until Task 1 acceptance evidence is accepted.

For an IDE changeover, start with [CONTINUE_WITH_GROK.md](CONTINUE_WITH_GROK.md).
The user has authorized Grok-led work through all six tasks, advancing only after
each task's evidence passes. This repository checkpoint saves the current scaffold
and design; it does not certify production readiness.

The [chat transcript](CHAT_TRANSCRIPT.md) preserves the earlier discussion for
context. Current instructions and the linked architecture documents define the
active implementation requirements.

[Reliability and security requirements](docs/RELIABILITY_SECURITY.md) define mandatory
failure behavior, privacy, cost admission, upgrade, and recovery controls.
[Acceptance status](docs/ACCEPTANCE_STATUS.md) distinguishes requirements from
verified implementation. No production-readiness guarantee is implied by this scaffold.

## Android client

The Gradle project lives in [`android-client`](android-client). `:app` is the blank
Jetpack Compose HOME/DEFAULT shell. Task 1 adds:

- `:domain` — pure Kotlin JVM types and invariants (no Android/framework imports)
- `:contracts` — v1 JSON Schemas, `reasoning.proto`, and JSON/protobuf presence mapping
- `:architecture-tests` — ArchUnit plus source/manifest guards

`:app` depends on `:domain` only, for later feature modules. Do not add notification
listeners, overlays, accessibility, or network clients in this foundation.

Build with JDK 17+ (CI uses JDK 21) and the Android SDK. Do not commit
`local.properties`; point Gradle at the SDK with `ANDROID_HOME`
(this machine: `C:\Users\prabu\AppData\Local\Android\Sdk`).

```powershell
$env:ANDROID_HOME = "C:\Users\prabu\AppData\Local\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
cd android-client
.\gradlew.bat assembleDebug check lint test --console=plain
```

Versions are pinned in `android-client/gradle/libs.versions.toml` and locked in
per-module `gradle.lockfile` files. Modules use Gradle `LockMode.STRICT`, so a
clean checkout fails if a lockfile is missing or stale. Refresh locks after an
intentional catalog change:

```powershell
cd android-client
.\gradlew.bat dependencies --write-locks --console=plain
```

Native `protoc` binaries are OS-specific and are excluded from `:contracts`
locking. Protobuf Java and schema artifacts remain locked.

`applicationId` is the provisional value `app.projectzero.launcher` because none
was supplied out-of-band. Signing secrets are not part of this repository.
