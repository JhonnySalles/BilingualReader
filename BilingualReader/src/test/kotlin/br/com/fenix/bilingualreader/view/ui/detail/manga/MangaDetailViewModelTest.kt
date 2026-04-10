package br.com.fenix.bilingualreader.view.ui.detail.manga

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.service.repository.FileLinkRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.service.repository.VocabularyRepository
import io.mockk.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import br.com.fenix.bilingualreader.util.secrets.Secrets
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MangaDetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: MangaDetailViewModel
    private lateinit var application: Application
    private val mangaRepository: MangaRepository = mockk(relaxed = true)
    private val fileLinkRepository: FileLinkRepository = mockk(relaxed = true)
    private val vocabularyRepository: VocabularyRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        @OptIn(ExperimentalCoroutinesApi::class)
        Dispatchers.setMain(testDispatcher)
        
        application = ApplicationProvider.getApplicationContext()
        mockkConstructor(MangaRepository::class)
        mockkConstructor(FileLinkRepository::class)
        mockkConstructor(VocabularyRepository::class)
        
        mockkObject(Secrets.Instance)
        every { Secrets.getSecrets(any()) } returns mockk(relaxed = true)
        
        every { anyConstructed<MangaRepository>().get(any()) } returns Manga(1L, 1L, File("f.zip"))
        every { anyConstructed<MangaRepository>().update(any()) } just Runs
        every { anyConstructed<MangaRepository>().delete(any()) } just Runs
        every { anyConstructed<MangaRepository>().markRead(any()) } just Runs
        every { anyConstructed<MangaRepository>().clearHistory(any()) } just Runs
        every { anyConstructed<FileLinkRepository>().findAllByManga(any()) } returns mutableListOf()

        viewModel = MangaDetailViewModel(application)
    }

    @After
    fun tearDown() {
        @OptIn(ExperimentalCoroutinesApi::class)
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `setManga should populate liveData and fetch vínculos`() {
        val manga = Manga(1L, 10L, File("op.zip"))
        viewModel.setManga(manga)
        
        assertEquals(manga, viewModel.manga.value)
        verify { anyConstructed<FileLinkRepository>().findAllByManga(10L) }
    }

    @Test
    fun `markRead should call repository and update liveData`() {
        val manga = Manga(1L, 10L, File("op.zip"))
        viewModel.setManga(manga)
        
        viewModel.markRead()
        verify { anyConstructed<MangaRepository>().markRead(any()) }
    }

    @Test
    fun `delete should call repository if manga is set`() {
        val manga = Manga(1L, 10L, File("op.zip"))
        viewModel.setManga(manga)
        
        viewModel.delete()
        verify { anyConstructed<MangaRepository>().delete(any()) }
    }

    @Test
    fun `clearHistory should call repository`() {
        val manga = Manga(1L, 10L, File("op.zip"))
        viewModel.setManga(manga)
        
        viewModel.clearHistory()
        verify { anyConstructed<MangaRepository>().clearHistory(any()) }
    }
}
