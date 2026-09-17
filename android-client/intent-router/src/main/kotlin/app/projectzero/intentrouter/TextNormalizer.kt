package app.projectzero.intentrouter

import java.text.Normalizer

object IntentTextNormalizer {
    fun view(raw: String): String {
        val nfkc = Normalizer.normalize(raw, Normalizer.Form.NFKC)
        return nfkc
            .replace('\u00A0', ' ')
            .replace(Regex("[\\p{Punct}&&[^:'@.+-]]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun originalPreserved(raw: String): String = raw
}
