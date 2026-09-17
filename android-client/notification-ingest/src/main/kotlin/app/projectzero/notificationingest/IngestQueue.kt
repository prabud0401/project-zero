package app.projectzero.notificationingest

import app.projectzero.domain.DomainBounds

sealed interface IngestWork {
    val frameworkKey: String
    val removal: Boolean

    data class Posted(val capture: NormalizedCapture) : IngestWork {
        override val frameworkKey: String get() = capture.frameworkKey
        override val removal: Boolean get() = false
    }

    data class Removed(override val frameworkKey: String) : IngestWork {
        override val removal: Boolean get() = true
    }
}

class IngestQueue(private val capacity: Int = DomainBounds.MAX_INGESTION_ITEMS) {
    private val items = LinkedHashMap<String, IngestWork>()
    var overflowed: Boolean = false
        private set

    @Synchronized
    fun offer(work: IngestWork): Boolean {
        val existing = items[work.frameworkKey]
        if (work.removal) {
            items[work.frameworkKey] = work
            return true
        }
        if (existing is IngestWork.Removed) {
            items[work.frameworkKey] = work
            return true
        }
        if (items.size >= capacity && existing == null) {
            overflowed = true
            val firstNonRemoval = items.entries.firstOrNull { !it.value.removal }?.key
            if (firstNonRemoval != null) {
                items.remove(firstNonRemoval)
            } else {
                return false
            }
        }
        items[work.frameworkKey] = work
        return true
    }

    @Synchronized
    fun drain(): List<IngestWork> {
        val drained = items.values.toList().sortedByDescending { it.removal }
        items.clear()
        val overflow = overflowed
        overflowed = false
        return if (overflow) drained else drained
    }

    @Synchronized
    fun size(): Int = items.size
}
