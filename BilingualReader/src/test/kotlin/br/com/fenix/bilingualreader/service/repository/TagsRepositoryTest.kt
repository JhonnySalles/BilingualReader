package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Tags
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TagsRepositoryTest {

    private val context = mockk<Context>()
    private val dataBase = mockk<DataBase>()
    private val tagsDao = mockk<TagsDAO>(relaxed = true)
    private val bookDao = mockk<BookDAO>(relaxed = true)

    private lateinit var repository: TagsRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)

        mockkObject(DataBase)
        every { DataBase.getDataBase(any()) } returns dataBase
        every { dataBase.getTagsDao() } returns tagsDao
        every { dataBase.getBookDao() } returns bookDao
        
        repository = TagsRepository(context)
    }

    @org.junit.After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `save book should call bookDao update`() {
        val book = mockk<Book>(relaxed = true)
        repository.save(book)
        verify { bookDao.update(book) }
    }

    @Test
    fun `save tag should call save or update based on id`() {
        val newTag = Tags(null, "New", false)
        every { tagsDao.save(newTag) } returns 1L
        assertEquals(1L, repository.save(newTag))
        verify { tagsDao.save(newTag) }
        
        val existingTag = Tags(2L, "Existing", false)
        assertEquals(2L, repository.save(existingTag))
        verify { tagsDao.update(existingTag) }
    }

    @Test
    fun `valid should return true if DAO returns null`() {
        every { tagsDao.valid("Unique") } returns null
        assertTrue(repository.valid("Unique"))
    }
}
