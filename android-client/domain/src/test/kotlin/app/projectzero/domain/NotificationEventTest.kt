package app.projectzero.domain

import app.projectzero.domain.notification.NotificationKind
import app.projectzero.domain.notification.Sensitivity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class NotificationEventTest {
    @Test
    fun validEventIsAccepted() {
        val event = Fixtures.event()
        assertEquals(1, event.schemaVersion)
        assertEquals(NotificationKind.MESSAGE, event.kind)
        assertEquals(Sensitivity.PERSONAL, event.sensitivity)
    }

    @Test
    fun schemaVersionMustBeOne() {
        val ex = assertThrows(DomainInvariantException::class.java) { Fixtures.event(schemaVersion = 2) }
        assertEquals(ReasonCode.UNSUPPORTED_SCHEMA, ex.reasonCode)
        assertThrows(DomainInvariantException::class.java) { Fixtures.event(schemaVersion = 0) }
    }

    @Test
    fun revisionMustBeAtLeastOne() {
        val ex = assertThrows(DomainInvariantException::class.java) { Fixtures.event(revision = 0) }
        assertEquals(ReasonCode.STALE_REVISION, ex.reasonCode)
    }

    @Test
    fun timestampsMustBeNonNegative() {
        assertThrows(DomainInvariantException::class.java) { Fixtures.event(postedAtEpochMs = -1) }
        assertThrows(DomainInvariantException::class.java) { Fixtures.event(observedAtEpochMs = -1) }
        assertThrows(DomainInvariantException::class.java) { Fixtures.event(expiresAtEpochMs = -1) }
        Fixtures.event(postedAtEpochMs = 0, observedAtEpochMs = 0, expiresAtEpochMs = 0)
    }

    @Test
    fun sourcePackageMustBeValid() {
        assertThrows(DomainInvariantException::class.java) { Fixtures.event(sourcePackage = "") }
        assertThrows(DomainInvariantException::class.java) { Fixtures.event(sourcePackage = "bad package") }
        assertThrows(DomainInvariantException::class.java) { Fixtures.event(sourcePackage = "1pkg") }
    }

    @Test
    fun fingerprintFieldsMustBeBase64UrlSha256() {
        assertThrows(DomainInvariantException::class.java) { Fixtures.event(contentFingerprint = "short") }
        assertThrows(DomainInvariantException::class.java) { Fixtures.event(channelIdHash = "+++") }
        Fixtures.event(channelIdHash = null, groupKeyHash = null)
    }

    @Test
    fun titleAndBodyHonorUnicodeCodePointLimits() {
        val titleOk = "a".repeat(DomainBounds.MAX_TITLE_CODE_POINTS)
        val titleOver = "a".repeat(DomainBounds.MAX_TITLE_CODE_POINTS + 1)
        Fixtures.event(title = titleOk, body = null)
        val titleEx = assertThrows(DomainInvariantException::class.java) { Fixtures.event(title = titleOver) }
        assertEquals(ReasonCode.UNICODE_LIMIT, titleEx.reasonCode)

        val bodyOk = "b".repeat(DomainBounds.MAX_BODY_CODE_POINTS)
        val bodyOver = "b".repeat(DomainBounds.MAX_BODY_CODE_POINTS + 1)
        Fixtures.event(body = bodyOk)
        assertThrows(DomainInvariantException::class.java) { Fixtures.event(body = bodyOver) }

        val emojiOk = "😀".repeat(DomainBounds.MAX_TITLE_CODE_POINTS)
        val emojiOver = "😀".repeat(DomainBounds.MAX_TITLE_CODE_POINTS + 1)
        assertEquals(DomainBounds.MAX_TITLE_CODE_POINTS * 2, emojiOk.length)
        assertEquals(DomainBounds.MAX_TITLE_CODE_POINTS, Validation.codePointCount(emojiOk))
        Fixtures.event(title = emojiOk)
        assertThrows(DomainInvariantException::class.java) { Fixtures.event(title = emojiOver) }
    }

    @Test
    fun peopleTokensCappedAtSixteen() {
        val sixteen = (1..16).map { "tok-$it" }.toSet()
        Fixtures.event(peopleTokens = sixteen)
        val ex = assertThrows(DomainInvariantException::class.java) {
            Fixtures.event(peopleTokens = sixteen + "tok-17")
        }
        assertEquals(ReasonCode.BOUNDS_EXCEEDED, ex.reasonCode)
        assertThrows(DomainInvariantException::class.java) {
            Fixtures.event(peopleTokens = setOf(" "))
        }
    }
}
