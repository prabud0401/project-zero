package app.projectzero.localai

import app.projectzero.domain.DomainBounds
import app.projectzero.domain.Validation

data class NormalizedText(
    val value: String?,
    val hadSpans: Boolean,
    val wasOversized: Boolean,
)

object TextNormalizer {
    fun normalize(
        raw: CharSequence?,
        maxCodePoints: Int,
        hadSpansHint: Boolean = false,
    ): NormalizedText {
        if (raw == null) {
            return NormalizedText(value = null, hadSpans = hadSpansHint, wasOversized = false)
        }
        val hadSpans = hadSpansHint || raw.javaClass.name.contains("Spanned") ||
            raw.javaClass.name.contains("Spannable")
        val stripped = stripControls(raw.toString())
        val count = Validation.codePointCount(stripped)
        val wasOversized = count > maxCodePoints
        val truncated = if (wasOversized) {
            truncateCodePoints(stripped, maxCodePoints)
        } else {
            stripped
        }
        val value = truncated.takeIf { it.isNotEmpty() }
        return NormalizedText(value = value, hadSpans = hadSpans, wasOversized = wasOversized)
    }

    fun normalizeTitle(raw: CharSequence?, hadSpansHint: Boolean = false): NormalizedText =
        normalize(raw, DomainBounds.MAX_TITLE_CODE_POINTS, hadSpansHint)

    fun normalizeBody(raw: CharSequence?, hadSpansHint: Boolean = false): NormalizedText =
        normalize(raw, DomainBounds.MAX_BODY_CODE_POINTS, hadSpansHint)

    /**
     * Idempotent: applying twice yields the same value and flags other than hadSpans
     * (span metadata is gone after the first toString).
     */
    fun stripControls(input: String): String {
        val builder = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val cp = input.codePointAt(i)
            i += Character.charCount(cp)
            if (cp == '\n'.code || cp == '\t'.code || cp == '\r'.code) {
                builder.appendCodePoint(cp)
                continue
            }
            val type = Character.getType(cp)
            val control = type == Character.CONTROL.toInt() ||
                type == Character.FORMAT.toInt() ||
                type == Character.PRIVATE_USE.toInt() ||
                type == Character.SURROGATE.toInt() ||
                type == Character.UNASSIGNED.toInt() ||
                cp == 0x200B || cp == 0x200C || cp == 0x200D || cp == 0x2066 ||
                cp == 0x2067 || cp == 0x2068 || cp == 0x2069 || cp == 0x202A ||
                cp == 0x202B || cp == 0x202C || cp == 0x202D || cp == 0x202E ||
                cp == 0x061C
            if (!control) {
                builder.appendCodePoint(cp)
            }
        }
        return builder.toString().trim()
    }

    private fun truncateCodePoints(value: String, max: Int): String {
        if (Validation.codePointCount(value) <= max) return value
        var end = 0
        var seen = 0
        while (end < value.length && seen < max) {
            val cp = value.codePointAt(end)
            end += Character.charCount(cp)
            seen++
        }
        return value.substring(0, end)
    }
}
