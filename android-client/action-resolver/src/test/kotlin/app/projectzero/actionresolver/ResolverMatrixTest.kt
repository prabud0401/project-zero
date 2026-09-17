package app.projectzero.actionresolver

import app.projectzero.domain.action.ActionCapability
import app.projectzero.domain.action.AppAffinityContext
import app.projectzero.domain.action.FallbackResolution
import app.projectzero.domain.action.PackageAffinity
import app.projectzero.domain.routing.RouteResult
import app.projectzero.intentrouter.LocalGrammarRouter
import app.projectzero.registry.ShippedRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResolverMatrixTest {
    private val router = LocalGrammarRouter()
    private val emptyContext = AppAffinityContext(
        schemaVersion = 1,
        capability = ActionCapability.WEB_SEARCH,
        rankedPackages = emptyList(),
        updatedAtEpochMs = 1_700_000_000_000,
    )

    @Test
    fun messageBuildsSmsto() = runBlocking {
        val parsed = router.parse("Message Maya 'late by 10'")
        val resolver = DeterministicActionResolver(ShippedRegistry.lastKnownGood(), clockMs = { 1_700_000_000_000 })
        val result = resolver.resolve(parsed, emptyContext.copy(capability = ActionCapability.MESSAGE))
        val proposed = result as RouteResult.Proposed
        assertEquals("android.intent.action.SENDTO", proposed.action.androidAction)
        assertEquals("smsto", proposed.action.uriScheme)
        val data = proposed.action.parameters.filterIsInstance<app.projectzero.domain.action.ActionParameter.UriValue>().first().value
        assertTrue(data.startsWith("smsto:"))
        assertTrue(data.contains("late"))
    }

    @Test
    fun navigateBuildsGeo() = runBlocking {
        val parsed = router.parse("Navigate to 21 Market Street")
        val resolver = DeterministicActionResolver(ShippedRegistry.lastKnownGood())
        val result = resolver.resolve(parsed, emptyContext.copy(capability = ActionCapability.NAVIGATE)) as RouteResult.Proposed
        assertEquals("android.intent.action.VIEW", result.action.androidAction)
        assertEquals("geo", result.action.uriScheme)
    }

    @Test
    fun dialNeverUsesActionCall() = runBlocking {
        val parsed = router.parse("Call Maya")
        val resolver = DeterministicActionResolver(ShippedRegistry.lastKnownGood())
        val result = resolver.resolve(parsed, emptyContext.copy(capability = ActionCapability.DIAL)) as RouteResult.Proposed
        assertEquals("android.intent.action.DIAL", result.action.androidAction)
        assertTrue(result.action.androidAction != "android.intent.action.CALL")
    }

    @Test
    fun emailBuildsMailto() = runBlocking {
        val parsed = router.parse("Email Lee about the report")
        val resolver = DeterministicActionResolver(ShippedRegistry.lastKnownGood())
        val result = resolver.resolve(parsed, emptyContext.copy(capability = ActionCapability.EMAIL)) as RouteResult.Proposed
        assertEquals("mailto", result.action.uriScheme)
    }

    @Test
    fun searchBuildsHttps() = runBlocking {
        val parsed = router.parse("Search the web for kotlin sealed classes")
        val resolver = DeterministicActionResolver(ShippedRegistry.lastKnownGood())
        val result = resolver.resolve(parsed, emptyContext.copy(capability = ActionCapability.WEB_SEARCH)) as RouteResult.Proposed
        assertEquals("https", result.action.uriScheme)
        val data = result.action.parameters.filterIsInstance<app.projectzero.domain.action.ActionParameter.UriValue>().first().value
        assertTrue(data.startsWith("https://www.google.com/search"))
    }

    @Test
    fun calendarUsesInsertNotProviderWrite() = runBlocking {
        val parsed = router.parse("Add dentist tomorrow at 3 for an hour")
        val resolver = DeterministicActionResolver(ShippedRegistry.lastKnownGood())
        val result = resolver.resolve(parsed, emptyContext.copy(capability = ActionCapability.CREATE_CALENDAR_EVENT))
        assertTrue(result is RouteResult.Proposed || result is RouteResult.Clarification)
        if (result is RouteResult.Proposed) {
            assertEquals("android.intent.action.INSERT", result.action.androidAction)
        }
    }

    @Test
    fun openAppUsesMain() = runBlocking {
        val parsed = router.parse("Open Spotify")
        val resolver = DeterministicActionResolver(ShippedRegistry.lastKnownGood())
        val result = resolver.resolve(parsed, emptyContext.copy(capability = ActionCapability.OPEN_APP)) as RouteResult.Proposed
        assertEquals("android.intent.action.MAIN", result.action.androidAction)
    }

    @Test
    fun absentHandlerUsesChooser() = runBlocking {
        val parsed = router.parse("Open Spotify")
        val resolver = DeterministicActionResolver(ShippedRegistry.lastKnownGood(), handlersFor = { _, _ -> emptyList() })
        val result = resolver.resolve(parsed, emptyContext.copy(capability = ActionCapability.OPEN_APP)) as RouteResult.Proposed
        assertEquals(FallbackResolution.ANDROID_CHOOSER, result.action.fallbackResolution)
    }

    @Test
    fun tiedAffinitiesUseChooser() = runBlocking {
        val parsed = router.parse("Open Spotify")
        val context = emptyContext.copy(
            capability = ActionCapability.OPEN_APP,
            rankedPackages = listOf(
                PackageAffinity("com.spotify.music", false, 1, 1L, 0.50),
                PackageAffinity("com.spotify.lite", false, 1, 1L, 0.50),
            ),
        )
        val resolver = DeterministicActionResolver(
            ShippedRegistry.lastKnownGood(),
            handlersFor = { _, _ ->
                listOf(
                    HandlerCandidate("com.spotify.music", true, true, affinity = 0.50),
                    HandlerCandidate("com.spotify.lite", true, true, affinity = 0.50),
                )
            },
        )
        val result = resolver.resolve(parsed, context) as RouteResult.Proposed
        assertEquals(FallbackResolution.ANDROID_CHOOSER, result.action.fallbackResolution)
    }

    @Test
    fun expiredActionDenied() = runBlocking {
        val parsed = router.parse("Navigate to 21 Market Street")
        val resolver = DeterministicActionResolver(ShippedRegistry.lastKnownGood(), elapsedMs = { 10L })
        val result = resolver.resolve(parsed, emptyContext.copy(capability = ActionCapability.NAVIGATE)) as RouteResult.Proposed
        val denied = DefaultActionPolicyGate { 10L + app.projectzero.domain.Validation.ACTION_TTL_MS + 1 }.validate(result.action)
        assertTrue(denied is app.projectzero.domain.routing.PolicyDecision.Deny)
    }
}
