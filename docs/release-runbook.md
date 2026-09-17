# Release Runbook

## 1. Pre-Release Checklist
- [ ] Ensure all CI checks (lint, test, assembleDebug) pass.
- [ ] Run SBOM generation and vulnerability scans.
- [ ] Verify GCP cost simulation (p95 < $0.004 / active-user day).
- [ ] Confirm no unresolved critical/high findings in the Threat Model.
- [ ] Execute 1,000 cold-home launches and 24-hour soak tests on reference devices.
- [ ] Verify Play Integrity bindings and signed registry manifest versions.

## 2. Artifact Generation
1. Check out the release branch.
2. Increment `versionCode` and `versionName` in `build.gradle.kts`.
3. Assemble release bundle: `./gradlew bundleRelease`.
4. Sign the bundle with the production keystore.
5. Generate the SBOM: `./gradlew spdxSbomsForRelease`.

## 3. Staged Rollout
1. Upload AAB to Play Console (Internal App Sharing).
2. Validate installation on 3 separate physical OEM devices.
3. Promote to Closed Testing (Alpha). Wait 48 hours for crash analytics.
4. Promote to Production with a 10% staged rollout.
5. Monitor logs, crash-free sessions (target >= 99.8%), and cloud budgets for 24 hours.
6. Ramp up rollout to 100%.

## 4. Post-Release
- Publish release notes.
- Tag the commit in git: `git tag v1.X.X`.
- Update backend supported client versions if needed.
