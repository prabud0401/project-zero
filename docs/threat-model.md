# Project Zero Threat Model

## 1. System Overview and Boundaries
Project Zero is an Android application that uses a NotificationListenerService to read local notifications, applies a privacy filter, and optionally uses a cloud service to generate notification summaries. It also processes natural-language intents and dispatches explicit intents to external apps.

Trust Zones:
1. Android private zone: Room database, DataStore, local cache, local embeddings.
2. Android IPC boundary: Untrusted inbound StatusBarNotification, Intents.
3. Network boundary: TLS 1.3/1.2 minimum, Cloud Run ingress.
4. Cloud project: Policy service, Vertex AI model endpoint, TTL cache.
5. External app boundary: Target-owned/system confirmation UI.

## 2. Identified Threats and Mitigations

| Threat ID | Threat Description | Attack Vector | Mitigation |
|-----------|--------------------|---------------|------------|
| TM-01 | Hostile Notification Content | Malicious app posts crafted text/RemoteViews/PendingIntents to exploit parsing or inference. | H08: Only copy primitives. Reject RemoteViews. Validate model output. Never execute unauthorized intents. |
| TM-02 | PII Leakage to Cloud | Sensitive data (OTP, health, finance) uploaded to cloud model. | H05, H12: Local privacy classifier (regex/heuristic) blocks sensitive categories. Redaction of PII before network request. |
| TM-03 | Unauthorized Action Execution | Attacker triggers sensitive action (e.g. sending SMS) silently. | H03: At-most-once execution. Explicit user confirmation (ActionPolicyGate) before any dispatch. |
| TM-04 | Cache Poisoning/Cross-user Leakage | Cloud cache or local cache leaks data across users/installations. | H06: Cache partitioned by profile/dataEpoch (local) and installation/consent epoch (server). Validated templates. |
| TM-05 | Intent Spoofing | Malicious app sends crafted intent to bypass policy. | H04, H15: Validate all inbound fields. Use signed registry for capability resolution. |
| TM-06 | Token Theft | Bearer token intercepted to abuse cloud quota. | H09: Short-lived tokens (15m), TLS 1.3/1.2, revocation. |
| TM-07 | Cloud Cost Exhaustion | Malicious app spams notifications to exhaust project budget. | H10, H11: Local coalescing, server-side quota (4000 input/600 output tokens per day), atomic ledger reservations. |

## 3. Residual Risks
- Device compromise (rooted device) bypasses Android sandboxing.
- Heuristic privacy redaction (TM-02) is imperfect; zero-day PII patterns may leak.
- Phishing via crafted notification text that bypasses classification.
