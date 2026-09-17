package app.projectzero.launcher

import android.app.Application
import android.os.UserManager
import app.projectzero.datalocal.DatabaseKeyException
import app.projectzero.datalocal.DatabaseKeyManager
import app.projectzero.datalocal.InstallSaltStore
import app.projectzero.datalocal.NotificationDatabaseFactory
import app.projectzero.datalocal.NotificationRepository
import app.projectzero.localai.ContentFingerprinter
import app.projectzero.localai.DeterministicSummaryEngine
import app.projectzero.localai.LocalPrivacyFilter
import app.projectzero.notificationingest.IngestCoordinator
import app.projectzero.notificationingest.NotificationNormalizer

class AppGraph(
    val coordinator: IngestCoordinator?,
    val repository: NotificationRepository?,
    val notificationStoreAvailable: Boolean,
    val storeFailureReason: String?,
) {
    companion object {
        fun create(application: Application): AppGraph {
            return try {
                val keyManager = DatabaseKeyManager(application)
                val db = NotificationDatabaseFactory.encrypted(application, keyManager)
                val repository = NotificationRepository(db)
                val salt = InstallSaltStore(application).getOrCreate()
                val fingerprinter = ContentFingerprinter(salt)
                val userManager = application.getSystemService(UserManager::class.java)
                val normalizer = NotificationNormalizer(userManager, fingerprinter)
                val engine = DeterministicSummaryEngine(fingerprinter = fingerprinter)
                val coordinator = IngestCoordinator(
                    repository = repository,
                    normalizer = normalizer,
                    fingerprinter = fingerprinter,
                    filter = LocalPrivacyFilter(),
                    summaryEngine = engine,
                )
                AppGraph(
                    coordinator = coordinator,
                    repository = repository,
                    notificationStoreAvailable = true,
                    storeFailureReason = null,
                )
            } catch (e: DatabaseKeyException) {
                AppGraph(null, null, false, "notification-store-unavailable")
            } catch (e: Exception) {
                AppGraph(null, null, false, "notification-store-unavailable")
            }
        }

        fun degraded(): AppGraph =
            AppGraph(null, null, false, "notification-store-unavailable")
    }
}
