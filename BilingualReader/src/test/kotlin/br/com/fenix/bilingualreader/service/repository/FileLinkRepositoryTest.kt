package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.LinkedFile
import br.com.fenix.bilingualreader.model.entity.LinkedPage
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.service.repository.DataBaseDAO.*
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FileLinkRepositoryTest {

    private lateinit var context: Context
    private val fileDao: FileLinkDAO = mockk(relaxed = true)
    private val pageDao: PageLinkDAO = mockk(relaxed = true)
    private val dataBaseMock: DataBase = mockk(relaxed = true)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockkObject(DataBase)
        every { DataBase.getDataBase(any()) } returns dataBaseMock
        every { dataBaseMock.getFileLinkDao() } returns fileDao
        every { dataBaseMock.getPageLinkDao() } returns pageDao
    }

    @Test
    fun testSaveAndRecursivePages() {
        val repository = FileLinkRepository(context)
        val manga = Manga(1L, 1L, File("/path/manga.cbz"))
        val fileLink = LinkedFile(1L, 1L, 10, "/path", "File", "cbz", "Folder", Languages.JAPANESE).apply {
            this.manga = manga
            this.pagesLink = listOf(LinkedPage(null, 1L, 0, 10, "P0", "/p0"))
            this.pagesNotLink = listOf(LinkedPage(null, 1L, 1, 10, "P1", "/p1"))
        }

        every { fileDao.save(any<LinkedFile>()) } returns 123L
        every { pageDao.save(any<LinkedPage>()) } returns 999L

        val id = repository.save(fileLink)

        assertEquals(123L, id)
        verify { fileDao.save(any<LinkedFile>()) }
        verify(exactly = 2) { pageDao.save(any<LinkedPage>()) }
        verify { pageDao.deleteAll(123L) }
    }

    @Test
    fun testGetWithPages() {
        val repository = FileLinkRepository(context)
        val manga = Manga(1L, 1L, File("/path/manga.cbz"))
        val fileLink = LinkedFile(123L, 1L, 10, "/path", "File", "cbz", "Folder", Languages.JAPANESE)

        every { fileDao.getLastAccess(1L) } returns fileLink
        every { pageDao.getPageLink(123L) } returns listOf(mockk())
        every { pageDao.getPageNotLink(123L) } returns listOf(mockk())

        val result = repository.get(manga)

        assertEquals(fileLink, result)
        assertEquals(manga, result?.manga)
        assertEquals(1, result?.pagesLink?.size)
        assertEquals(1, result?.pagesNotLink?.size)
    }
}
