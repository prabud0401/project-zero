package app.projectzero.actionresolver

import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.StringLength
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class UriSafetyFuzzTest {
    @ParameterizedTest
    @ValueSource(
        strings = [
            "intent://scan/#Intent;scheme=http;end",
            "file:///etc/passwd",
            "javascript:alert(1)",
            "https://user:pass@evil.example/x",
            "https://127.0.0.1/login",
            "https://example.com/../../etc/passwd",
            "https://example.com/%2e%2e/secret",
            "geo:0,0?q=https://evil.example",
            "smsto:555\r\nBcc:bad",
            "https://example.com/${'$'}{jndi}",
            "https://example.com/{{placeholder}}",
        ],
    )
    fun rejectsForbiddenAndTricks(uri: String) {
        assertNotNull(UriSafety.rejectReason(uri), uri)
    }

    @Test
    fun acceptsAllowlisted() {
        assertNull(UriSafety.rejectReason("smsto:5551234?body=hi"))
        assertNull(UriSafety.rejectReason("mailto:a@b.co"))
        assertNull(UriSafety.rejectReason("tel:+15551212"))
        assertNull(UriSafety.rejectReason("geo:0,0?q=Market%20Street"))
        assertNull(UriSafety.rejectReason("https://www.google.com/search?q=a"))
    }

    @Property(tries = 80)
    fun oversizedRejected(@ForAll @StringLength(min = 5000, max = 5200) tail: String) {
        val uri = "https://www.google.com/" + tail
        assertNotNull(UriSafety.rejectReason(uri))
    }
}
