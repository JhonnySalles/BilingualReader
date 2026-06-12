package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.Type
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class ShareMarkEntityTest {

    @Test
    fun `test sharemark construction and properties`() {
        val now = Date()
        val items = mutableSetOf<ShareItem>()
        val shareMark = ShareMark(
            origin = "Source",
            lastAlteration = now,
            type = Type.BOOK,
            marks = items
        )

        assertEquals("Source", shareMark.origin)
        assertEquals(now, shareMark.lastAlteration)
        assertEquals(Type.BOOK, shareMark.type)
        assertEquals(items, shareMark.marks)
    }

    @Test
    fun `test sharemark secondary constructor`() {
        val shareMark = ShareMark(Type.MANGA)
        
        assertEquals("", shareMark.origin)
        assertEquals(null, shareMark.lastAlteration)
        assertEquals(Type.MANGA, shareMark.type)
        assertTrue(shareMark.marks?.isEmpty() ?: false)
    }
}
