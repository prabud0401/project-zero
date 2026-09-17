package app.projectzero.network

import app.projectzero.domain.notification.NotificationEvent

class Redactor {
    fun redact(event: NotificationEvent): NotificationEvent {
        // Redact PII logic
        return event.copy(title = "<REDACTED>", body = "<REDACTED>")
    }
}
