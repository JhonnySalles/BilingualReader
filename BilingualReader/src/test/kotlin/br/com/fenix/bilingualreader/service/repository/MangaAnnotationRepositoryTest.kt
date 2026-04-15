package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.MangaAnnotation
import br.com.fenix.bilingualreader.model.enums.MarkType
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MangaAnnotationRepositoryTest {

    private val context = mockk<Context>()
    private val dataBase = mockk<DataBase>()
    private val mangaAnnotationDao = mockk<MangaAnnotationDAO>(relaxed = true)

    private lateinit var repository: MangaAnnotationRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)

        mockkObject(DataBase)
        every { DataBase.getDataBase(any()) } returns dataBase
        every { dataBase.getMangaAnnotation() } returns mangaAnnotationDao
        
        repository = MangaAnnotationRepository(context)
    }

    @org.junit.After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `save should update alteration and return new id`() {
        // Constructor: id_manga, page, pages, type, chapter, folder, annotation
        val annotation = MangaAnnotation(1L, 1, 1, MarkType.PageMark, "Chapter", "Folder", "Text")
        every { mangaAnnotationDao.save(any<MangaAnnotation>()) } returns 789L
        
        val id = repository.save(annotation)
        
        assertEquals(789L, id)
        assertEquals(789L, annotation.id)
        assert(annotation.alteration != null)
        verify { mangaAnnotationDao.save(annotation) }
    }

    @Test
    fun `findAllByManga should return list from DAO`() {
        val list = listOf(MangaAnnotation(10L, 1, 1, MarkType.PageMark, "Chapter", "Folder", "Text"))
        every { mangaAnnotationDao.findAllByManga(10L) } returns list
        
        val result = repository.findAll(10L)
        
        assertEquals(list, result)
        verify { mangaAnnotationDao.findAllByManga(10L) }
    }
}
