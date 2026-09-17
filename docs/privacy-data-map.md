# Privacy and Data Map

## 1. Data Categories
- **Notification Data**: Titles, bodies, source packages, timestamps.
- **Intent Data**: Transcribed speech, typed text for action dispatch.
- **Affinity Data**: App usage ranking (local only).
- **Authentication**: Short-lived installation tokens, Play Integrity metadata.

## 2. Data Flow & Residency

| Data Element | Local Storage | Cloud Transmission | Cloud Storage/Retention |
|--------------|---------------|--------------------|-------------------------|
| Notification Body (Public) | Encrypted Room DB, TTL 24h/7d | Redacted structure sent via HTTPS | TTL max 24h, cache partitioned by install |
| Notification Body (Secret/PII)| Encrypted Room DB, TTL 24h/7d | **NEVER** transmitted | N/A |
| User Intent (Text) | In-memory only (ephemeral) | Transmitted if cloud-eligible | Not logged; cache template only |
| Contact IDs/URIs | In-memory (transient) | **NEVER** transmitted (Redacted) | N/A |
| Device/Installation ID | App Data (SharedPreferences) | Included in Auth Header | Used for rate limiting/cache partitioning |

## 3. Privacy Controls (H05, H07)
- **Retention**: Local raw events deleted on removal or after 24h. Summaries expire in 7d. Cloud caches are TTL-bound to 24h.
- **Redaction**: PII, contacts, addresses, OTPs are replaced with deterministic placeholders before cloud transmission.
- **Encryption**: Room database uses SQLCipher. Cloud backup is explicitly disabled (`allowBackup="false"`).
- **User Controls**: Explicit notification listener consent. Erase/revoke advances data epoch, dropping all associated data and revoking cloud principal.

## 4. Third-Party Sharing
- Inference requires GCP/Vertex AI. No data is used for model training.
- External actions hand off to user-chosen third-party apps via standard Android Intents.
