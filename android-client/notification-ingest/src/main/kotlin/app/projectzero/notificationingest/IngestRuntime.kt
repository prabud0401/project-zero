package app.projectzero.notificationingest

import java.util.concurrent.atomic.AtomicReference

object IngestRuntime {
    private val coordinatorRef = AtomicReference<IngestCoordinator?>()

    fun attach(coordinator: IngestCoordinator?) {
        coordinatorRef.set(coordinator)
    }

    fun coordinator(): IngestCoordinator? = coordinatorRef.get()
}
