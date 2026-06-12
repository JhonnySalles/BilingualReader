package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Util
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Date

class ShareAnnotationTest {

    @Before
    fun setUp() {
        mockkObject(GeneralConsts)
        mockkObject(Util)
        every { GeneralConsts.dateTimeToDate(any()) } returns Date()
        every { Util.intArrayToString(any()) } returns "1,2,3"
    }

    @After
    fun tearDown() {
        unmockkObject(GeneralConsts)
        unmockkObject(Util)
    }

    @Test
    fun `test share annotation from book annotation`() {
        val ba = BookAnnotation(
            id_book = 1L, page = 5, pages = 10, fontSize = 12f, type = MarkType.Annotation,
            chapterNumber = 1f, chapter = "C1", text = "T", range = intArrayOf(1, 2, 3), annotation = "A"
        )

        val sa = ShareAnnotation(ba)

        assertEquals(5, sa.page)
        assertEquals(12f, sa.fontSize)
        assertEquals("Annotation", sa.type)
        assertEquals("1,2,3", sa.range)
    }

    @Test
    fun `test share annotation equality`() {
        val date = Date()
        val sa1 = ShareAnnotation(5, 10, 12f, "T1", 1f, "C1", "Txt", "1,2", "Ann", false, "#FFF", date)
        val sa2 = ShareAnnotation(5, 10, 12f, "T1", 1f, "C1", "Txt", "1,2", "Ann", true, "#FFF", date) // favorite diff

        assertEquals("Favorite shouldn't affect equality based on implementation", sa1, sa2)
    }
}
