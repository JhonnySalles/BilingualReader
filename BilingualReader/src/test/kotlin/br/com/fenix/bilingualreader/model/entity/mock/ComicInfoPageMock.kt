package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.ComicInfoPage
import br.com.fenix.bilingualreader.model.enums.ComicInfoPageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

object ComicInfoPageMock {

    fun mockEntity(): ComicInfoPage = ComicInfoPage(
        bookmark = "Cover",
        image = 1,
        imageHeight = 1000,
        imageWidth = 700,
        imageSize = 500000L,
        type = ComicInfoPageType.FrontCover,
        doublePage = false,
        key = "page001"
    )

    fun mockEntityList(): List<ComicInfoPage> = listOf(mockEntity())

    fun asserts(expected: ComicInfoPage?, actual: ComicInfoPage?) {
        assertNotNull("Actual ComicInfoPage should not be null", actual)
        expected?.let {
            assertEquals("Bookmark mismatch", it.bookmark, actual?.bookmark)
            assertEquals("Image index mismatch", it.image, actual?.image)
            assertEquals("Page type mismatch", it.type, actual?.type)
        }
    }
}
