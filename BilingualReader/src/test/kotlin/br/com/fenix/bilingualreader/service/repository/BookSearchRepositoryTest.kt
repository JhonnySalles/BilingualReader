package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.BookSearch
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BookSearchRepositoryTest {

    private val context = mockk<Context>()
    private val dataBase = mockk<DataBase>()
    private val bookSearchDao = mockk<BookSearchDAO>(relaxed = true)

    private lateinit var repository: BookSearchRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkObject(DataBase)
        every { DataBase.getDataBase(any()) } returns dataBase
        every { dataBase.getBookSearch() } returns bookSearchDao
        
        repository = BookSearchRepository(context)
    }

    @org.junit.After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `save should call DAO save`() {
        val search = BookSearch(null, 1L, "Query", LocalDateTime.now())
        every { bookSearchDao.save(search) } returns 456L
        
        val id = repository.save(search)
        
        assertEquals(456L, id)
        verify { bookSearchDao.save(search) }
    }

    @Test
    fun `delete by idBook should fetch all and delete each`() {
        val list = listOf(
            BookSearch(1L, 10L, "Q1", LocalDateTime.now()),
            BookSearch(2L, 10L, "Q2", LocalDateTime.now())
        )
        every { bookSearchDao.findAllByBook(10L) } returns list
        
        repository.delete(10L)
        
        verify { bookSearchDao.delete(list[0]) }
        verify { bookSearchDao.delete(list[1]) }
    }

    @Test
    fun `findAll should return list from DAO`() {
        val list = listOf(BookSearch(1L, 10L, "Q", LocalDateTime.now()))
        every { bookSearchDao.findAllByBook(10L) } returns list
        
        val result = repository.findAll(10L)
        
        assertEquals(list, result)
        verify { bookSearchDao.findAllByBook(10L) }
    }
}
