package app.projectzero.localai

import app.projectzero.domain.DomainBounds
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.StringLength
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TextNormalizerPropertyTest {
    @Property(tries = 200)
    fun normalizationIsIdempotent(@ForAll @StringLength(max = 4000) raw: String) {
        val first = TextNormalizer.normalize(raw, DomainBounds.MAX_BODY_CODE_POINTS)
        val second = TextNormalizer.normalize(first.value, DomainBounds.MAX_BODY_CODE_POINTS)
        assertEquals(first.value, second.value)
        assertEquals(false, second.wasOversized)
    }

    @Property(tries = 100)
    fun truncatedResultsStayWithinBounds(@ForAll @StringLength(min = 0, max = 8000) raw: String) {
        val result = TextNormalizer.normalizeTitle(raw)
        val value = result.value
        if (value != null) {
            assertTrue(value.codePointCount(0, value.length) <= DomainBounds.MAX_TITLE_CODE_POINTS)
        }
    }

    @Test
    fun stripsBidiAndNullControls() {
        val dirty = "\u202Esecret\u0000code"
        val normalized = TextNormalizer.normalizeTitle(dirty).value
        assertEquals("secretcode", normalized)
        assertEquals(normalized, TextNormalizer.normalizeTitle(normalized).value)
    }
}
