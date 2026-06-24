package br.com.fenix.bilingualreader.model.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class SubTitleTextTest {

    @Test
    fun `test subtitle text construction`() {
        val st = SubTitleText(
            text = "Subtitle line",
            sequence = 1,
            x1 = 10,
            y1 = 20,
            x2 = 100,
            y2 = 50
        )

        assertEquals("Subtitle line", st.text)
        assertEquals(1, st.sequence)
        assertEquals(10, st.x1)
        assertEquals(20, st.y1)
        assertEquals(100, st.x2)
        assertEquals(50, st.y2)
    }
}
