package br.com.fenix.bilingualreader.model.enums

import org.junit.Assert.assertEquals
import org.junit.Test

class EnumsTest {

    @Test
    fun testEnumsExist() {
        // Enums simples apenas para garantir cobertura e integridade das constantes
        assertEquals("PREPARE", AudioStatus.PREPARE.name)
        assertEquals("PLAY", AudioStatus.PLAY.name)
        
        assertEquals("Justify", AlignmentLayoutType.Justify.name)
        assertEquals("Center", AlignmentLayoutType.Center.name)
        
        assertEquals("MANGA", Type.MANGA.name)
        assertEquals("BOOK", Type.BOOK.name)
        
        assertEquals("Name", Order.Name.name)
        assertEquals("Date", Order.Date.name)
        
        assertEquals("Default", PaginationType.Default.name)
        assertEquals("Stack", PaginationType.Stack.name)
        
        assertEquals("ASPECT_FILL", ReaderMode.ASPECT_FILL.name)
        assertEquals("ASPECT_FIT", ReaderMode.ASPECT_FIT.name)
    }
    
    @Test
    fun testMarkType() {
        assertEquals("PageMark", MarkType.PageMark.name)
        assertEquals("BookMark", MarkType.BookMark.name)
    }
}
