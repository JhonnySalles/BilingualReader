package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.entity.MangaAnnotation
import br.com.fenix.bilingualreader.model.enums.MarkType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AnnotationRepositoriesTest {

    private lateinit var context: Context
    private val bookDao: BookAnnotationDAO = mockk(relaxed = true)
    private val mangaDao: MangaAnnotationDAO = mockk(relaxed = true)
    private val dataBaseMock: DataBase = mockk(relaxed = true)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockkObject(DataBase)
        every { DataBase.getDataBase(any()) } returns dataBaseMock
        every { dataBaseMock.getBookAnnotation() } returns bookDao
        every { dataBaseMock.getMangaAnnotation() } returns mangaDao
    }

    @Test
    fun testBookAnnotationRepository() {
        val repository = BookAnnotationRepository(context)
        val annotation = BookAnnotation(1L, 1, 10, 12f, MarkType.Annotation, 1f, "Ch 1", "Text", intArrayOf(0, 5), "Note")
        
        every { bookDao.save(any<BookAnnotation>()) } returns 123L
        
        val id = repository.save(annotation)
        
        assertEquals(123L, id)
        verify { bookDao.save(any<BookAnnotation>()) }
        
        repository.findAll(1L)
        verify { bookDao.findAllByBook(1L) }
    }

    @Test
    fun testMangaAnnotationRepository() {
        val repository = MangaAnnotationRepository(context)
        val annotation = MangaAnnotation(1L, 1, 10, MarkType.PageMark, "Ch 1", "/path", "Note")
        
        every { mangaDao.save(any<MangaAnnotation>()) } returns 456L
        
        val id = repository.save(annotation)
        
        assertEquals(456L, id)
        verify { mangaDao.save(any<MangaAnnotation>()) }
        
        repository.findByManga(1L)
        verify { mangaDao.findByManga(1L) }
    }
}
