package app.projectzero.notificationingest

import android.util.Log
import app.projectzero.domain.ReasonCode

/**
 * Logs only identifiers and reason codes. Never pass notification title/body/extras here.
 */
object SafeLog {
    private const val TAG = "PzIngest"

    fun d(eventId: String?, reason: ReasonCode?, message: String) {
        Log.d(TAG, format(eventId, reason, message))
    }

    fun w(eventId: String?, reason: ReasonCode?, message: String) {
        Log.w(TAG, format(eventId, reason, message))
    }

    fun e(eventId: String?, reason: ReasonCode?, message: String, error: Throwable? = null) {
        if (error != null) {
            Log.e(TAG, format(eventId, reason, message), error)
        } else {
            Log.e(TAG, format(eventId, reason, message))
        }
    }

    private fun format(eventId: String?, reason: ReasonCode?, message: String): String =
        buildString {
            append(message)
            if (eventId != null) {
                append(" eventId=")
                append(eventId)
            }
            if (reason != null) {
                append(" reason=")
                append(reason.wireValue)
            }
        }
}
