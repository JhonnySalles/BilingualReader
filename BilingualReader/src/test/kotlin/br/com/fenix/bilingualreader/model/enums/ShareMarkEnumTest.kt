package br.com.fenix.bilingualreader.model.enums

import org.junit.Assert.assertEquals
import org.junit.Test

class ShareMarkEnumTest {

    @Test
    fun `test companion counters and clear`() {
        ShareMarkType.send = 10
        ShareMarkType.receive = 5
        
        assertEquals(10, ShareMarkType.send)
        assertEquals(5, ShareMarkType.receive)
        
        ShareMarkType.clear()
        
        assertEquals(0, ShareMarkType.send)
        assertEquals(0, ShareMarkType.receive)
    }

    @Test
    fun `test intent property`() {
        val type = ShareMarkType.SUCCESS
        type.intent = null
        assertEquals(null, type.intent)
    }
}
