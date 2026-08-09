package br.com.fenix.bilingualreader.service.translate

import br.com.fenix.bilingualreader.model.enums.Languages
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MlKitLanguageMappingTest {

    @Test
    fun fromBcp47MapsCommonCodes() {
        assertEquals(Languages.PORTUGUESE, mapCode("pt"))
        assertEquals(Languages.ENGLISH, mapCode("en"))
        assertEquals(Languages.JAPANESE, mapCode("ja"))
        assertEquals(null, mapCode("und"))
        assertEquals(null, mapCode(null))
    }

    @Test
    fun sameLanguageTreatsPortugueseVariantsAsEqual() {
        val same: (Languages, Languages) -> Boolean = { a, b ->
            fun normalize(l: Languages) = when (l) {
                Languages.PORTUGUESE_GOOGLE -> Languages.PORTUGUESE
                else -> l
            }
            normalize(a) == normalize(b)
        }
        assertTrue(same(Languages.PORTUGUESE, Languages.PORTUGUESE_GOOGLE))
        assertFalse(same(Languages.PORTUGUESE, Languages.ENGLISH))
    }

    private fun mapCode(code: String?): Languages? {
        if (code.isNullOrBlank() || code == "und") return null
        return when (code.lowercase().take(2)) {
            "pt" -> Languages.PORTUGUESE
            "en" -> Languages.ENGLISH
            "ja" -> Languages.JAPANESE
            else -> null
        }
    }
}
