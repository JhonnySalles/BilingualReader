package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.ShareAnnotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import java.util.Date

object ShareAnnotationMock {

    fun mockEntity(): ShareAnnotation = ShareAnnotation(
        page = 1,
        pages = 10,
        fontSize = 12f,
        type = "Annotation",
        chapterNumber = 1.0f,
        chapter = "Chapter 1",
        text = "Mock shared text",
        range = "0-10",
        annotation = "Mock shared annotation",
        favorite = false,
        color = "Yellow",
        created = Date()
    )

    fun mockEntityList(): List<ShareAnnotation> = listOf(mockEntity())

    fun asserts(expected: ShareAnnotation?, actual: ShareAnnotation?) {
        assertNotNull("Actual shared annotation should not be null", actual)
        expected?.let {
            assertEquals("Page mismatch", it.page, actual?.page)
            assertEquals("Link mismatch", it.text, actual?.text)
        }
    }
}
