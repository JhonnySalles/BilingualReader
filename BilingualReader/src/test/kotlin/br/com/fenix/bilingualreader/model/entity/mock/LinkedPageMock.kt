package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.LinkedPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

object LinkedPageMock : Mock<Long, LinkedPage> {

    override fun mockEntity(): LinkedPage = mockEntity(1L)

    override fun mockEntityList(): List<LinkedPage> = listOf(
        mockEntity(1L),
        mockEntity(2L)
    )

    override fun mockEntity(id: Long): LinkedPage = LinkedPage(
        id = id,
        idFile = 100L,
        mangaPage = 1,
        mangaPages = 100,
        mangaPageName = "page001.jpg",
        mangaPagePath = "/manga/page001.jpg",
        fileLinkLeftPage = 1,
        fileLinkLeftPages = 100,
        fileLinkLeftPageName = "page001.jpg",
        fileLinkLeftPagePath = "/file/page001.jpg",
        isNotLinked = false,
        isDualImage = false,
        isMangaDualPage = false,
        isFileLeftDualPage = false,
        isFileRightDualPage = false
    )

    override fun asserts(expected: LinkedPage?, actual: LinkedPage?) {
        assertNotNull("Actual LinkedPage should not be null", actual)
        expected?.let {
            assertEquals("ID mismatch", it.id, actual?.id)
            assertEquals("File ID mismatch", it.idFile, actual?.idFile)
            assertEquals("Manga page name mismatch", it.mangaPageName, actual?.mangaPageName)
            assertEquals("Linked left page mismatch", it.fileLinkLeftPage, actual?.fileLinkLeftPage)
        }
    }
}
