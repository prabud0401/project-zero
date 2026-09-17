package app.projectzero.actionresolver

import app.projectzero.domain.DomainBounds
import app.projectzero.domain.action.AllowlistedUriSchemes
import java.net.URI

object UriSafety {
    private val crlf = Regex("[\r\n]")
    private val credentials = Regex("://[^/]*:[^/]*@")
    private val ipLiteral = Regex("://\\[?\\d{1,3}(?:\\.\\d{1,3}){3}")
    private val nestedScheme = Regex("(?i)(intent|file|javascript|https?):")

    fun rejectReason(raw: String): String? {
        if (raw.codePointCount(0, raw.length) > DomainBounds.MAX_URI_CODE_POINTS) return "oversized"
        if (crlf.containsMatchIn(raw)) return "crlf"
        val lower = raw.lowercase()
        val scheme = lower.substringBefore(':', missingDelimiterValue = "").ifBlank { return "missing-scheme" }
        if (scheme in AllowlistedUriSchemes.FORBIDDEN) return "forbidden-scheme"
        if (scheme !in AllowlistedUriSchemes.VALUES) return "scheme-not-allowlisted"
        if (credentials.containsMatchIn(raw)) return "embedded-credentials"
        if (ipLiteral.containsMatchIn(raw)) return "ip-literal"
        if (".." in raw || "%2e%2e" in lower || "%2E%2E" in raw) return "path-traversal"
        if ("${'$'}{" in raw || "{{" in raw) return "template-placeholder"
        val rest = raw.substringAfter(':', "")
        if (scheme in setOf("https", "geo") && nestedScheme.containsMatchIn(rest) &&
            !rest.startsWith("//")
        ) {
            return "nested-uri"
        }
        if (scheme == "https") {
            val uri = runCatching { URI(raw) }.getOrNull() ?: return "malformed"
            val host = uri.host ?: return "missing-host"
            if (host.startsWith(".") || host.contains("..")) return "host"
        }
        return null
    }

    fun requireSafe(raw: String): String {
        val reason = rejectReason(raw)
        require(reason == null) { "unsafe uri: $reason" }
        return raw
    }
}
