package app.projectzero.localai

fun interface EpochClock {
    fun nowMs(): Long

    companion object {
        val System: EpochClock = EpochClock { java.lang.System.currentTimeMillis() }
    }
}
