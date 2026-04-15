package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Date

class ShareHistoryTest {

    @Before
    fun setUp() {
        mockkObject(GeneralConsts)
        every { GeneralConsts.dateTimeToDate(any()) } returns Date()
    }

    @After
    fun tearDown() {
        unmockkObject(GeneralConsts)
    }

    @Test
    fun `test share history from history entity`() {
        val history = History(
            fkLibrary = 1L, fkReference = 2L, type = Type.BOOK, pageStart = 1, pages = 100, volume = "1"
        )
        // History sets end and secondsRead when end is set. 
        // We'll trust the mapping here.

        val sh = ShareHistory(history)

        assertEquals(1, sh.pageStart)
        assertEquals(100, sh.pages)
        assertEquals("1", sh.volume)
    }

    @Test
    fun `test share history equality`() {
        val dateStart = Date(1000)
        val dateEnd = Date(2000)
        val sh1 = ShareHistory(1, 50, 100, false, "1", 0, dateStart, dateEnd, 60, 1, false)
        val sh2 = ShareHistory(1, 50, 100, true, "1", 1, dateStart, dateEnd, 70, 2, true) // many diffs but same start/end/pages

        assertEquals("Equality should based on pageStart, pageEnd, pages, start, end", sh1, sh2)
    }
}
