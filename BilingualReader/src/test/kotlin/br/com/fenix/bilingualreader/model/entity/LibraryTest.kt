package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LibraryTest {

    @Test
    fun `test library equality is based on path`() {
        val lib1 = Library(1L, "Title 1", "/path/1")
        val lib2 = Library(2L, "Title 2", "/path/1")
        val lib3 = Library(1L, "Title 1", "/path/2")
        
        assertEquals(lib1, lib2)
        assertNotEquals(lib1, lib3)
    }

    @Test
    fun `test merge updates properties`() {
        val lib1 = Library(1L, "Old Title", "path", type = Type.MANGA)
        val lib2 = Library(2L, "New Title", "new path", type = Type.BOOK)
        
        lib1.merge(lib2)
        
        assertEquals("New Title", lib1.title)
        assertEquals("new path", lib1.path)
        assertEquals(Type.BOOK, lib1.type)
        assertEquals(1L, lib1.id) // ID should not be merged
    }

    @Test
    fun `test default values`() {
        val lib = Library(null)
        assertEquals(Libraries.DEFAULT.name, lib.title)
        assertEquals(Type.MANGA, lib.type)
        assertEquals(true, lib.enabled)
    }
}
