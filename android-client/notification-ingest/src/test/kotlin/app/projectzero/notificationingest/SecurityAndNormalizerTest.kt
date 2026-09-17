package app.projectzero.notificationingest

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.UserHandle
import android.service.notification.StatusBarNotification
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider
import app.projectzero.datalocal.NotificationDatabaseFactory
import app.projectzero.datalocal.NotificationRepository
import app.projectzero.localai.ContentFingerprinter
import app.projectzero.localai.DeterministicSummaryEngine
import app.projectzero.localai.EpochClock
import app.projectzero.localai.LocalPrivacyFilter
import app.projectzero.localai.TextNormalizer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class SecurityAndNormalizerTest {
    private lateinit var coordinator: IngestCoordinator
    private lateinit var normalizer: NotificationNormalizer
    private lateinit var context: Application
    private val now = 1_700_000_000_000L
    private val salt = ByteArray(32) { 9 }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val db = NotificationDatabaseFactory.inMemory(context)
        val repo = NotificationRepository(db, clock = { now })
        val fingerprinter = ContentFingerprinter(salt)
        normalizer = NotificationNormalizer(null, fingerprinter, EpochClock { now })
        coordinator = IngestCoordinator(
            repository = repo,
            normalizer = normalizer,
            fingerprinter = fingerprinter,
            filter = LocalPrivacyFilter(),
            summaryEngine = DeterministicSummaryEngine(clock = EpochClock { now }, fingerprinter = fingerprinter),
            clock = EpochClock { now },
        )
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("ch", "ch", NotificationManager.IMPORTANCE_DEFAULT))
    }

    @Test
    fun remoteViewsNeverCloudEligible() = runBlocking {
        val notification = baseBuilder().build()
        notification.contentView = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        val capture = normalizer.normalize(sbn(notification))
        assertTrue(capture.hadRemoteViews)
        coordinator.processPosted(capture)
        assertNull(coordinator.cloudEnvelopeFor(coordinatorEventId("0|com.example.mail|1")))
        assertTrue(coordinator.lastEnvelopes.isEmpty())
    }

    @Test
    fun pendingIntentMetadataNeverCloudEligible() = runBlocking {
        val pi = PendingIntent.getActivity(
            context,
            0,
            Intent(Intent.ACTION_VIEW),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = baseBuilder().setContentIntent(pi).build()
        val capture = normalizer.normalize(sbn(notification))
        assertTrue(capture.hadPendingIntent)
        coordinator.processPosted(capture)
        assertTrue(coordinator.lastEnvelopes.isEmpty())
    }

    @Test
    fun spansAreStrippedAndNeverCloudEligible() = runBlocking {
        val title = SpannableString("Hello")
        title.setSpan(ForegroundColorSpan(0xFFFF0000.toInt()), 0, 5, 0)
        val notification = baseBuilder().setContentTitle(title).build()
        val capture = normalizer.normalize(sbn(notification))
        assertEquals("Hello", capture.title)
        assertTrue(capture.hadSpans)
        coordinator.processPosted(capture)
        assertTrue(coordinator.lastEnvelopes.isEmpty())
    }

    @Test
    fun oversizedTextNeverCloudEligible() = runBlocking {
        val body = "x".repeat(2_500)
        coordinator.processPosted(
            NormalizedCapture(
                frameworkKey = "big",
                sourcePackage = "com.example.mail",
                sourceUserSerial = 0,
                channelId = "ch",
                groupKey = null,
                title = "t",
                body = TextNormalizer.normalizeBody(body).value,
                peopleTokens = emptySet(),
                isOngoing = false,
                isClearable = true,
                postedAtEpochMs = now,
                observedAtEpochMs = now,
                category = "msg",
                hadRemoteViews = false,
                hadPendingIntent = false,
                hadSpans = false,
                wasOversized = true,
            ),
        )
        assertTrue(coordinator.lastEnvelopes.isEmpty())
    }

    @Test
    fun otpHealthFinanceManagedProfileNeverCloudEligible() = runBlocking {
        val cases = listOf(
            capture("otp", body = "Your verification code is 424242"),
            capture("health", body = "Lab results ready at the clinic"),
            capture("fin", body = "Account ending 1111 available balance $20"),
            capture("work", body = "Hello from work", userSerial = 10),
        )
        cases.forEach { item ->
            coordinator.processPosted(item)
            assertTrue(item.frameworkKey, coordinator.lastEnvelopes.isEmpty())
            assertNull(coordinator.cloudEnvelopeFor("unused"))
        }
    }

    @Test
    fun normalizationIsIdempotentOnRandomInputs() {
        val random = Random(42)
        repeat(120) {
            val raw = buildString {
                repeat(random.nextInt(0, 80)) {
                    append(Char(random.nextInt(0, 512)))
                }
                append('\u202E')
            }
            val first = TextNormalizer.normalizeTitle(raw)
            val second = TextNormalizer.normalizeTitle(first.value)
            assertEquals(first.value, second.value)
        }
    }

    @Test
    fun normalizerDoesNotKeepRemoteViewsOrPendingIntentObjects() {
        val pi = PendingIntent.getActivity(context, 1, Intent(Intent.ACTION_VIEW), PendingIntent.FLAG_IMMUTABLE)
        val notification = baseBuilder().setContentIntent(pi).build()
        notification.contentView = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        val capture = normalizer.normalize(sbn(notification))
        assertTrue(capture.hadRemoteViews)
        assertTrue(capture.hadPendingIntent)
        val hostileTypes = NormalizedCapture::class.java.declaredFields.map { it.type.name }
        assertFalse(hostileTypes.any { it.contains("RemoteViews") || it.contains("PendingIntent") })
    }

    private fun coordinatorEventId(key: String): String = "missing"

    private fun capture(
        key: String,
        body: String,
        userSerial: Long = 0,
    ) = NormalizedCapture(
        frameworkKey = key,
        sourcePackage = "com.example.mail",
        sourceUserSerial = userSerial,
        channelId = "ch",
        groupKey = null,
        title = "Notice",
        body = body,
        peopleTokens = emptySet(),
        isOngoing = false,
        isClearable = true,
        postedAtEpochMs = now,
        observedAtEpochMs = now,
        category = "msg",
        hadRemoteViews = false,
        hadPendingIntent = false,
        hadSpans = false,
        wasOversized = false,
    )

    private fun baseBuilder(): Notification.Builder =
        Notification.Builder(context, "ch")
            .setContentTitle("Hello")
            .setContentText("World")
            .setSmallIcon(android.R.drawable.ic_dialog_info)

    private fun sbn(notification: Notification, pkg: String = "com.example.mail", id: Int = 1): StatusBarNotification =
        StatusBarNotification(
            pkg,
            pkg,
            id,
            null,
            1000,
            0,
            0,
            notification,
            UserHandle.getUserHandleForUid(1000),
            now,
        )
}
