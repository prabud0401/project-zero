# Rollback Runbook

## 1. Client-Side Rollback (Android)
*Note: Android does not support downgrading an installed APK. Rollback requires a new corrective release.*
1. Identify the regression (e.g., severe crash, privacy leak).
2. Halt the staged rollout in the Google Play Console immediately.
3. Check out the previous stable release branch/tag.
4. Cherry-pick any mandatory security/data-migration fixes.
5. Increment `versionCode` to be higher than the faulty release.
6. Build, sign, and upload the new AAB.
7. Push to 100% rollout to overwrite the faulty version.

## 2. Server-Side Rollback / Kill Switches
If the regression is caused by a backend change or remote configuration:
1. **Registry/Config Switch**: Publish a new signed registry manifest disabling the faulty capability. The app will fetch this within 15 minutes or on next cold start.
2. **Cloud Inference Disablement**: If Vertex AI or GCP is unstable or leaking data, operator toggles the remote kill switch to disable cloud admission. The app will gracefully fall back to local-only behavior.
3. **Backend Service Rollback**: Deploy the previous stable Cloud Run container image. Ensure the older image still satisfies the v1 wire contracts and schema compatibility.
