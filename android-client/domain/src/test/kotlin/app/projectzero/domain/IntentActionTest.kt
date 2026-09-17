package app.projectzero.domain

import app.projectzero.domain.action.ActionParameter
import app.projectzero.domain.action.AllowlistedAndroidActions
import app.projectzero.domain.action.PackageAffinity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IntentActionTest {
    @Test
    fun validActionIsAcceptedAtFiveMinuteBoundary() {
        val action = Fixtures.action(createdAtEpochMs = 10_000, expiresAtEpochMs = 10_000 + Validation.ACTION_TTL_MS)
        assertFalse(action.isExpiredAt(10_000 + Validation.ACTION_TTL_MS))
        assertTrue(action.isExpiredAt(10_000 + Validation.ACTION_TTL_MS + 1))
    }

    @Test
    fun expiryBeyondFiveMinutesIsRejected() {
        val ex = assertThrows(DomainInvariantException::class.java) {
            Fixtures.action(createdAtEpochMs = 0, expiresAtEpochMs = Validation.ACTION_TTL_MS + 1)
        }
        assertEquals(ReasonCode.EXPIRY_INVALID, ex.reasonCode)
    }

    @Test
    fun expiryBeforeCreatedIsRejected() {
        assertThrows(DomainInvariantException::class.java) {
            Fixtures.action(createdAtEpochMs = 100, expiresAtEpochMs = 99)
        }
    }

    @Test
    fun duplicateParameterKeysAreRejected() {
        val ex = assertThrows(DomainInvariantException::class.java) {
            Fixtures.action(
                parameters = listOf(
                    ActionParameter.Text("body", "a"),
                    ActionParameter.UriValue("body", "smsto:1"),
                ),
            )
        }
        assertEquals(ReasonCode.DUPLICATE_PARAMETER, ex.reasonCode)
    }

    @Test
    fun parameterKeyMustBeNonBlank() {
        assertThrows(DomainInvariantException::class.java) { ActionParameter.Text(" ", "x") }
        ActionParameter.InstantValue("start", 0, "UTC")
        ActionParameter.StringList("ids", listOf("a", "b"))
    }

    @Test
    fun androidActionAndSchemeMustBeAllowlisted() {
        assertTrue("android.intent.action.DIAL" in AllowlistedAndroidActions.VALUES)
        assertThrows(DomainInvariantException::class.java) {
            Fixtures.action(androidAction = "android.intent.action." + "CALL")
        }
        assertThrows(DomainInvariantException::class.java) { Fixtures.action(uriScheme = "intent") }
        assertThrows(DomainInvariantException::class.java) { Fixtures.action(uriScheme = "javascript") }
        assertThrows(DomainInvariantException::class.java) { Fixtures.action(uriScheme = "file") }
        Fixtures.action(uriScheme = null, targetPackage = null)
    }

    @Test
    fun rationaleHonorsUnicodeLimit() {
        Fixtures.action(rationale = "x".repeat(160))
        assertThrows(DomainInvariantException::class.java) { Fixtures.action(rationale = "x".repeat(161)) }
        Fixtures.action(rationale = "😀".repeat(160))
        assertThrows(DomainInvariantException::class.java) { Fixtures.action(rationale = "😀".repeat(161)) }
    }

    @Test
    fun affinityBounds() {
        Fixtures.affinity()
        assertThrows(DomainInvariantException::class.java) {
            Fixtures.affinity(
                rankedPackages = (1..9).map {
                    PackageAffinity("com.example.app$it", false, 0, null, 0.1)
                },
            )
        }
        assertThrows(DomainInvariantException::class.java) {
            PackageAffinity(Fixtures.PACKAGE, false, -1, null, 0.0)
        }
        assertThrows(DomainInvariantException::class.java) {
            PackageAffinity(Fixtures.PACKAGE, false, 0, null, Double.NaN)
        }
    }
}
