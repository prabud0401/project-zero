# Google Play Compliance Strategy

## 1. Core Functionality & Permissions
Project Zero acts as a default launcher (`CATEGORY_HOME`/`CATEGORY_DEFAULT`) and requires the following restricted permissions:
- `BIND_NOTIFICATION_LISTENER_SERVICE`: Justified for clustering and summarizing incoming notifications on the home screen.

We explicitly DO NOT request:
- `AccessibilityService` (banned for arbitrary background tasks)
- `SYSTEM_ALERT_WINDOW`
- `QUERY_ALL_PACKAGES` (we use narrow `<queries>`)
- Direct call/SMS permissions.

## 2. Privacy Policy & Data Safety Form
- **Data Collection**: Notification listener reads notification contents. We must declare this in the Data Safety form.
- **Data Sharing**: We send anonymized/redacted notification text to Vertex AI (GCP) for summarization. We must disclose this third-party transmission and declare that it is not used for training.
- **Encryption**: All data in transit uses TLS 1.2+. Data at rest uses encrypted SQLCipher.
- **Deletion**: We provide an in-app mechanism to erase all local data and revoke cloud cached data.

## 3. Prominent Disclosure & Consent
Before requesting `BIND_NOTIFICATION_LISTENER_SERVICE`, the app displays a prominent disclosure explaining:
1. What data is accessed (notifications).
2. How it is used (local and cloud AI summarization).
3. The privacy-preserving redaction applied before any network request.
Consent must be granted explicitly via system settings.

## 4. Policy Violations Avoided
- **Malware/Spyware**: No hidden intents. All external actions use explicit confirmation UIs.
- **Deceptive Behavior**: System intent fallback UI uses standard choosers. No bypassing of target app UI.
- **Child Policy**: Notifications matching the minors policy or managed-profile boundaries are strictly excluded from cloud processing.
