package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.model.enums.Color
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config()
class BookAnnotationRepositoryTest {

    private val context = mockk<Context>()
    private val dataBase = mockk<DataBase>()
    private val bookAnnotationDao = mockk<BookAnnotationDAO>(relaxed = true)

    private lateinit var repository: BookAnnotationRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)

        mockkObject(DataBase)
        every { DataBase.getDataBase(any()) } returns dataBase
        every { dataBase.getBookAnnotation() } returns bookAnnotationDao
        
        repository = BookAnnotationRepository(context)
    }

    @org.junit.After
    fun tearDown() {
        unmockkAll()
    }

    private fun createAnnotation(id: Long? = null, idBook: Long = 1L) = BookAnnotation(
        id = id,
        id_parent = idBook,
        page = 1,
        pages = 10,
        fontSize = 12f,
        markType = MarkType.Annotation,
        chapterNumber = 1f,
        chapter = "Chapter 1",
        text = "Sample Text",
        range = intArrayOf(0, 5),
        annotation = "My Note",
        favorite = false,
        color = Color.None,
        alteration = LocalDateTime.now(),
        created = LocalDateTime.now()
    )

    @Test
    fun `save should update alteration and return new id`() {
        val annotation = createAnnotation(null)
        every { bookAnnotationDao.save(any<BookAnnotation>()) } returns 123L
        
        val id = repository.save(annotation)
        
        assertEquals(123L, id)
        assertEquals(123L, annotation.id)
        verify { bookAnnotationDao.save(annotation) }
    }

    @Test
    fun `update should update alteration and call DAO`() {
        val annotation = createAnnotation(1L)
        val oldAlteration = annotation.alteration
        Thread.sleep(1)
        repository.update(annotation)
        
        assert(annotation.alteration != oldAlteration)
        verify { bookAnnotationDao.update(annotation) }
    }

    @Test
    fun `delete should call DAO`() {
        val annotation = createAnnotation(123L)
        repository.delete(annotation)
        verify { bookAnnotationDao.delete(annotation) }
    }

    @Test
    fun `findAllByBook should return list from DAO`() {
        val list = listOf(createAnnotation(1L, 10L))
        every { bookAnnotationDao.findAllByBook(10L) } returns list
        
        val result = repository.findAll(10L)
        
        assertEquals(list, result)
        verify { bookAnnotationDao.findAllByBook(10L) }
    }

    @Test
    fun `findAllOrderByBook should return list from DAO`() {
        val list = listOf(createAnnotation(1L))
        every { bookAnnotationDao.findAllOrderByBook() } returns list
        
        val result = repository.findAllOrderByBook()
        
        assertEquals(list, result)
        verify { bookAnnotationDao.findAllOrderByBook() }
    }

    @Test
    fun `findByPage should return list from DAO`() {
        val list = listOf(createAnnotation(1L, 10L))
        every { bookAnnotationDao.findByPage(10L, 1) } returns list
        
        val result = repository.findByPage(10L, 1)
        
        assertEquals(list, result)
        verify { bookAnnotationDao.findByPage(10L, 1) }
    }
}
