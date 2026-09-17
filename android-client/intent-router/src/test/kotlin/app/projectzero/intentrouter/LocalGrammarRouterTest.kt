package app.projectzero.intentrouter

import app.projectzero.domain.action.ActionCapability
import app.projectzero.domain.routing.ParsedIntent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class LocalGrammarRouterTest {
    private val router = LocalGrammarRouter()

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "Message Maya 'late by 10'|MESSAGE",
            "Navigate to 21 Market Street|NAVIGATE",
            "Add dentist tomorrow at 3 for an hour|CREATE_CALENDAR_EVENT",
            "Call Maya|DIAL",
            "Email Lee about the report|EMAIL",
            "Search the web for kotlin sealed classes|WEB_SEARCH",
            "Open Spotify|OPEN_APP",
        ],
    )
    fun matrixExactMatch(utterance: String, capability: String) {
        val parsed = router.parse(utterance)
        assertTrue(parsed is ParsedIntent.Complete, utterance + " -> " + parsed)
        assertEquals(ActionCapability.valueOf(capability), (parsed as ParsedIntent.Complete).capability)
        assertTrue(parsed.confidence >= 0.82)
    }

    @Test
    fun photoShareClarifiesRecipientContract() {
        val parsed = router.parse("Text this photo to Maya")
        assertTrue(parsed is ParsedIntent.ClarificationRequired || parsed is ParsedIntent.Complete)
    }

    @Test
    fun missingHandlerSlotsAskForClarification() {
        val parsed = router.parse("Message someone")
        assertTrue(parsed is ParsedIntent.ClarificationRequired)
    }

    @Test
    fun ambiguousContactClarifies() {
        val parsed = router.parse("Call Maya or Lee")
        assertTrue(parsed is ParsedIntent.ClarificationRequired)
    }

    @Test
    fun ambiguousDstClarifies() {
        val parsed = router.parse("Add dentist tomorrow at 2 am or 2 pm")
        assertTrue(parsed is ParsedIntent.ClarificationRequired)
    }

    @Test
    fun capabilityTieClarifies() {
        val parsed = router.parse("search open maps")
        assertTrue(parsed is ParsedIntent.ClarificationRequired || parsed is ParsedIntent.Complete)
    }

    @Test
    fun unsupportedGrammarRejected() {
        val parsed = router.parse("asdf qwer zxcv")
        assertTrue(parsed is ParsedIntent.Rejected)
    }
}
