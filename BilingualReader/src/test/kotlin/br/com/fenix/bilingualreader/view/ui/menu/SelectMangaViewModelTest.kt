package br.com.fenix.bilingualreader.view.ui.menu

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.LibraryRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.util.helpers.LibraryUtil
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SelectMangaViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val application = mockk<Application>(relaxed = true)
    private val mangaRepository = mockk<MangaRepository>(relaxed = true)
    private val libraryRepository = mockk<LibraryRepository>(relaxed = true)

    private lateinit var viewModel: SelectMangaViewModel

    private val defaultLibrary = Library(1L, "Default", "path", Libraries.JAPANESE, Type.MANGA, true, false)

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        mockkStatic(FirebaseApp::class)
        every { FirebaseApp.getInstance() } returns mockk(relaxed = true)
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns mockk(relaxed = true)

        mockkObject(LibraryUtil)
        every { LibraryUtil.getDefault(any(), Type.MANGA) } returns defaultLibrary
        
        mockkConstructor(MangaRepository::class)
        mockkConstructor(LibraryRepository::class)
    }

    @org.junit.After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `changeLibrary should update current library and reload list`() {
        val newLib = Library(2L, "New", "new_path", Libraries.JAPANESE, Type.MANGA, true, false)
        val mangaList = listOf(Manga(null, 100L, java.io.File("path")))
        
        // This is tricky because repositories are created in constructor.
        // We need to ensure the mocked repository is used or its methods are mocked on any instance.
        every { anyConstructed<MangaRepository>().list(newLib) } returns mangaList
        
        viewModel = SelectMangaViewModel(application)
        viewModel.changeLibrary(newLib)
        
        assertEquals(newLib, viewModel.getLibrary())
        assertEquals(1, viewModel.listMangas.value?.size)
        assertEquals(100L, viewModel.listMangas.value?.first()?.id)
    }

    @Test
    fun `clearMangaSelected should reset id and manga string`() {
        viewModel = SelectMangaViewModel(application)
        viewModel.id = 50L
        viewModel.manga = "Some Manga"
        
        viewModel.clearMangaSelected()
        
        assertEquals(-1L, viewModel.id)
        assertEquals("", viewModel.manga)
    }
}
