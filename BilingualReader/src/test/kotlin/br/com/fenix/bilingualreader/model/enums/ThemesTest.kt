package br.com.fenix.bilingualreader.model.enums

import br.com.fenix.bilingualreader.R
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemesTest {

    @Test
    fun testGetThemeResource() {
        assertEquals(R.style.Theme_MangaReader, Themes.ORIGINAL.getValue())
        assertEquals(R.style.Theme_MangaReader_Blue, Themes.BLUE.getValue())
        assertEquals(R.style.Theme_MangaReader_Red, Themes.RED.getValue())
    }
}
