package app.projectzero.actionresolver

import app.projectzero.domain.action.ActionCapability
import app.projectzero.domain.routing.TypedSlots
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object CapabilityUriBuilder {
    fun build(capability: ActionCapability, slots: TypedSlots): BuiltUri {
        return when (capability) {
            ActionCapability.MESSAGE -> {
                val recipient = requireNotNull(slots.recipientToken)
                val body = slots.message.orEmpty()
                val uri = "smsto:" + encodePath(recipient) + "?body=" + encodeQuery(body)
                BuiltUri("android.intent.action.SENDTO", "smsto", UriSafety.requireSafe(uri), null)
            }
            ActionCapability.NAVIGATE -> {
                val dest = requireNotNull(slots.destination)
                val uri = "geo:0,0?q=" + encodeQuery(dest)
                BuiltUri("android.intent.action.VIEW", "geo", UriSafety.requireSafe(uri), null)
            }
            ActionCapability.CREATE_CALENDAR_EVENT ->
                BuiltUri(
                    androidAction = "android.intent.action.INSERT",
                    scheme = null,
                    uri = "content://com.android.calendar/events",
                    mimeType = "vnd.android.cursor.item/event",
                )
            ActionCapability.DIAL -> {
                val number = requireNotNull(slots.recipientToken)
                val uri = "tel:" + encodePath(number.filter { it.isDigit() || it == '+' })
                BuiltUri("android.intent.action.DIAL", "tel", UriSafety.requireSafe(uri), null)
            }
            ActionCapability.EMAIL -> {
                val address = requireNotNull(slots.recipientToken)
                val uri = buildString {
                    append("mailto:")
                    append(encodePath(address))
                    val query = mutableListOf<String>()
                    slots.title?.let { query += "subject=" + encodeQuery(it) }
                    slots.message?.let { query += "body=" + encodeQuery(it) }
                    if (query.isNotEmpty()) {
                        append('?')
                        append(query.joinToString("&"))
                    }
                }
                BuiltUri("android.intent.action.SENDTO", "mailto", UriSafety.requireSafe(uri), null)
            }
            ActionCapability.WEB_SEARCH -> {
                val query = requireNotNull(slots.query)
                val uri = "https://www.google.com/search?q=" + encodeQuery(query)
                BuiltUri("android.intent.action.VIEW", "https", UriSafety.requireSafe(uri), null)
            }
            ActionCapability.OPEN_APP ->
                BuiltUri("android.intent.action.MAIN", null, null, null)
        }
    }

    private fun encodeQuery(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

    private fun encodePath(value: String): String = encodeQuery(value)
}

data class BuiltUri(
    val androidAction: String,
    val scheme: String?,
    val uri: String?,
    val mimeType: String?,
)
