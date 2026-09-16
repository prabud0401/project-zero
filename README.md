# Project Zero

Project Zero is an intent-driven, privacy-first Android launcher. Its design uses
only documented Android intents, verified App Links, deep links, and system APIs;
it never performs UI automation or uses `AccessibilityService`.

The implementation-ready architecture, wire contracts, deterministic action
resolution rules, AI routing policy, and six-stage agent backlog are defined in
[the System Design Document](docs/SYSTEM_DESIGN.md).

Coding agents should start with [the implementation handoff](docs/IMPLEMENTATION_HANDOFF.md)
and [the aligned six-task backlog](docs/BACKLOG.md). The Compose shell is built;
the SDD Task 1 domain/contracts/testing/CI foundation is still incomplete.

For an IDE changeover, start with [CONTINUE_WITH_GROK.md](CONTINUE_WITH_GROK.md).
The user has authorized Grok-led work through all six tasks, advancing only after
each task's evidence passes. This repository checkpoint saves the current scaffold
and design; it does not certify production readiness.

[Reliability and security requirements](docs/RELIABILITY_SECURITY.md) define mandatory
failure behavior, privacy, cost admission, upgrade, and recovery controls.
[Acceptance status](docs/ACCEPTANCE_STATUS.md) distinguishes requirements from
verified implementation. No production-readiness guarantee is implied by this scaffold.

## Android client

The initial scaffold lives in [`android-client`](android-client): a Jetpack Compose
home-app shell with a blank UI. The Gradle project is self-contained in that directory.

Build with JDK 17+ and the Android SDK. Do not commit `local.properties`; point
Gradle at the SDK with `ANDROID_HOME` (this machine: `C:\Users\prabu\AppData\Local\Android\Sdk`).

```powershell
$env:ANDROID_HOME = "C:\Users\prabu\AppData\Local\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
cd android-client
.\gradlew.bat assembleDebug check lint test
```

`applicationId` is the provisional value `app.projectzero.launcher` because none
was supplied out-of-band. Signing secrets are not part of this repository.
