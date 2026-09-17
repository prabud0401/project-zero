package app.projectzero.localai

import app.projectzero.domain.EventId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Random

class UuidV7Test {
    @Test
    fun generatedValuesAreAcceptedByDomainEventId() {
        repeat(20) {
            val id = UuidV7.generate(1_700_000_000_000L + it, Random(it.toLong()))
            assertEquals(id, EventId.parse(id).value)
        }
    }
}
