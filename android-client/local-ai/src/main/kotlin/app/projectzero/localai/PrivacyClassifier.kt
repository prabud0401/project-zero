package app.projectzero.localai

import app.projectzero.domain.DomainBounds
import app.projectzero.domain.ReasonCode
import app.projectzero.domain.notification.NotificationKind
import app.projectzero.domain.notification.Sensitivity

data class CaptureSignals(
    val hadRemoteViews: Boolean = false,
    val hadPendingIntent: Boolean = false,
    val hadSpans: Boolean = false,
    val wasOversized: Boolean = false,
    val sourceUserSerial: Long = DomainBounds.PERSONAL_USER_SERIAL,
    val category: String? = null,
    val packageName: String = "",
)

data class ClassificationResult(
    val sensitivity: Sensitivity,
    val kind: NotificationKind,
    val reasons: Set<ReasonCode>,
    val cloudProhibited: Boolean,
) {
    val primaryReason: ReasonCode
        get() = when {
            ReasonCode.OTP in reasons -> ReasonCode.OTP
            ReasonCode.PASSWORD in reasons -> ReasonCode.PASSWORD
            ReasonCode.HEALTH in reasons -> ReasonCode.HEALTH
            ReasonCode.FINANCIAL in reasons -> ReasonCode.FINANCIAL
            ReasonCode.PRECISE_LOCATION in reasons -> ReasonCode.PRECISE_LOCATION
            ReasonCode.MANAGED_PROFILE in reasons -> ReasonCode.MANAGED_PROFILE
            ReasonCode.MINORS_POLICY in reasons -> ReasonCode.MINORS_POLICY
            ReasonCode.SECRET in reasons -> ReasonCode.SECRET
            ReasonCode.OVERSIZED in reasons -> ReasonCode.OVERSIZED
            else -> reasons.firstOrNull() ?: ReasonCode.CLOUD_ELIGIBLE
        }
}

object PrivacyClassifier {
    private val otpKeyword = Regex(
        "(?i)\\b(otp|one[\\s-]?time(?:\\s+pass(?:word|code))?|verification code|auth(?:entication)? code|security code|login code)\\b",
    )
    private val shortCode = Regex("\\b\\d{4,8}\\b")
    private val onlyCode = Regex("^\\s*\\d{4,8}\\s*$")
    private val password = Regex("(?i)\\b(password|passwd|secret key|recovery (?:code|key)|private key|passcode)\\b")
    private val health = Regex(
        "(?i)\\b(diagnos(?:is|ed)|prescription|lab results?|blood test|a1c|medical record|patient|clinic|dosage|hipaa|hemoglobin)\\b",
    )
    private val finance = Regex(
        "(?i)\\b(account ending|routing number|cvv|iban|swift|wire transfer|card ending|available balance|payment of|acct\\.?\\s*#)\\b",
    )
    private val location = Regex(
        "(?i)\\b(current location|live location|sharing location)\\b|\\b-?\\d{1,2}\\.\\d{4,},\\s*-?\\d{1,3}\\.\\d{4,}\\b",
    )
    private val minors = Regex("(?i)\\b(child account|under 13|coppa)\\b")
    private val delivery = Regex("(?i)\\b(delivered|out for delivery|package|tracking|shipped)\\b")
    private val media = Regex("(?i)\\b(now playing|paused|spotify|youtube music)\\b")

    fun classify(title: String?, body: String?, signals: CaptureSignals): ClassificationResult {
        val text = listOfNotNull(title, body, signals.category).joinToString("\n")
        val reasons = linkedSetOf<ReasonCode>()

        if (signals.sourceUserSerial != DomainBounds.PERSONAL_USER_SERIAL) {
            reasons += ReasonCode.MANAGED_PROFILE
        }
        if (otpKeyword.containsMatchIn(text) && shortCode.containsMatchIn(text) || onlyCode.containsMatchIn(text)) {
            reasons += ReasonCode.OTP
        }
        if (password.containsMatchIn(text)) {
            reasons += ReasonCode.PASSWORD
        }
        if (health.containsMatchIn(text)) {
            reasons += ReasonCode.HEALTH
        }
        if (finance.containsMatchIn(text)) {
            reasons += ReasonCode.FINANCIAL
        }
        if (location.containsMatchIn(text)) {
            reasons += ReasonCode.PRECISE_LOCATION
        }
        if (minors.containsMatchIn(text)) {
            reasons += ReasonCode.MINORS_POLICY
        }
        if (signals.wasOversized) {
            reasons += ReasonCode.OVERSIZED
        }

        val secretReasons = setOf(
            ReasonCode.OTP,
            ReasonCode.PASSWORD,
            ReasonCode.HEALTH,
            ReasonCode.FINANCIAL,
            ReasonCode.PRECISE_LOCATION,
            ReasonCode.MINORS_POLICY,
        )
        if (reasons.any { it in secretReasons }) {
            reasons += ReasonCode.SECRET
        }

        val cloudProhibited = reasons.isNotEmpty() ||
            signals.hadRemoteViews ||
            signals.hadPendingIntent ||
            signals.hadSpans ||
            signals.wasOversized

        val sensitivity = when {
            ReasonCode.SECRET in reasons || ReasonCode.OTP in reasons || ReasonCode.PASSWORD in reasons ->
                Sensitivity.SECRET
            ReasonCode.MANAGED_PROFILE in reasons -> Sensitivity.SECRET
            reasons.isNotEmpty() -> Sensitivity.PERSONAL
            signals.category.equals("sys", ignoreCase = true) -> Sensitivity.PUBLIC
            else -> Sensitivity.PERSONAL
        }

        val kind = kindOf(signals.category, text)
        return ClassificationResult(
            sensitivity = sensitivity,
            kind = kind,
            reasons = reasons,
            cloudProhibited = cloudProhibited,
        )
    }

    private fun kindOf(category: String?, text: String): NotificationKind {
        val cat = category?.lowercase().orEmpty()
        return when {
            cat == "msg" || cat == "message" || cat.contains("msg") -> NotificationKind.MESSAGE
            cat == "social" || cat.contains("social") -> NotificationKind.SOCIAL
            cat == "event" || cat.contains("event") || cat.contains("alarm") -> NotificationKind.CALENDAR
            delivery.containsMatchIn(text) || cat.contains("transport") && delivery.containsMatchIn(text) ->
                NotificationKind.DELIVERY
            media.containsMatchIn(text) || cat.contains("transport") && media.containsMatchIn(text) ->
                NotificationKind.MEDIA
            cat == "sys" || cat == "service" || cat == "error" || cat == "progress" || cat == "call" ->
                NotificationKind.SYSTEM
            cat.contains("status") -> NotificationKind.SYSTEM
            else -> NotificationKind.OTHER
        }
    }
}
