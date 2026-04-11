package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MangaRepositoryTest {

    private val context = mockk<Context>()
    private val dataBase = mockk<DataBase>()
    private val mangaDao = mockk<MangaDAO>(relaxed = true)
    private val librariesDao = mockk<LibrariesDAO>(relaxed = true)

    private lateinit var mangaRepository: MangaRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)

        mockkObject(DataBase)
        every { DataBase.getDataBase(any()) } returns dataBase
        every { dataBase.getMangaDao() } returns mangaDao
        every { dataBase.getLibrariesDao() } returns librariesDao
        
        mangaRepository = MangaRepository(context)
    }

    @org.junit.After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `save should update lastAlteration and call DAO save`() {
        val manga = mockk<Manga>(relaxed = true)
        every { manga.id } returns 1L
        every { (mangaDao as DataBaseDAO<Manga>).save(any<Manga>()) } returns 1L
        
        val now = LocalDateTime.now()
        mangaRepository.save(manga, now)
        
        verify { manga.lastAlteration = now }
        verify { mangaDao.save(manga) }
    }

    @Test
    fun `updateLastAccess should update access timestamp and call DAO`() {
        val manga = mockk<Manga>(relaxed = true)
        every { manga.id } returns 10L
        
        mangaRepository.updateLastAccess(manga)
        
        verify { manga.lastAccess = any() }
        verify { manga.lastAlteration = any() }
        verify { mangaDao.update(manga) }
    }

    @Test
    fun `markRead should update bookmark to total pages and update access`() {
        val manga = mockk<Manga>(relaxed = true)
        every { manga.id } returns 5L
        every { manga.pages } returns 30
        
        mangaRepository.markRead(manga)
        
        verify { manga.bookMark = 30 }
        verify { manga.lastAccess = any() }
        verify { mangaDao.update(manga) }
    }

    @Test
    fun `loadLibrary in list should populate library object`() {
        val library = Library(id = 1L)
        val manga = Manga(null, "Title", "", "", "", 0L, br.com.fenix.bilingualreader.model.enums.FileType.UNKNOWN, 1, intArrayOf(), mapOf(), 0, false, false, false, "", "", "", "", "", null, 5L, false, null, null, null, java.util.Date(), null, null)
        val targetLibrary = Library(id = 5L, title = "Target Manga Library")
        
        every { mangaDao.list(1L) } returns listOf(manga)
        every { librariesDao.get(5L) } returns targetLibrary
        
        val results = mangaRepository.list(library)
        
        assertEquals(1, results?.size)
        assertEquals("Target Manga Library", results!![0].library.title)
    }
}
