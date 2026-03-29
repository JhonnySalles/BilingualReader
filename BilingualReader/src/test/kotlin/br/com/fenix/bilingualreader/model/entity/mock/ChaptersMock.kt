package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.Chapters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

object ChaptersMock {

    fun mockEntity(): Chapters = Chapters(
        title = "Chapter 1",
        number = 1,
        page = 1,
        chapter = 1.0f,
        isTitle = true,
        parent = null,
        isSelected = false,
        image = null
    )

    fun mockEntityList(): List<Chapters> = listOf(mockEntity())

    fun asserts(expected: Chapters?, actual: Chapters?) {
        assertNotNull("Actual Chapters should not be null", actual)
        expected?.let {
            assertEquals("Title mismatch", it.title, actual?.title)
            assertEquals("Number mismatch", it.number, actual?.number)
            assertEquals("Chapter float mismatch", it.chapter, actual!!.chapter, 0.01f)
        }
    }
}
