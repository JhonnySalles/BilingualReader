package br.com.fenix.bilingualreader.view.ui.library.manga

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.LibraryMangaType
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MangaLibraryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: MangaLibraryViewModel
    private lateinit var application: Application
    private val mangaRepository: MangaRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        mockkConstructor(MangaRepository::class)
        every { anyConstructed<MangaRepository>().list(any()) } returns mutableListOf()
        every { anyConstructed<MangaRepository>().save(any()) } returns 1L
        every { anyConstructed<MangaRepository>().update(any()) } just Runs
        every { anyConstructed<MangaRepository>().delete(any()) } just Runs

        viewModel = MangaLibraryViewModel(application)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `setLibrary should clear list if library ID changes`() {
        val lib1 = Library(id = 1L, title = "Lib 1")
        val lib2 = Library(id = 2L, title = "Lib 2")
        
        viewModel.setLibrary(lib1)
        val manga = Manga(1L, 1L, File("f1.zip"))
        viewModel.add(manga)
        
        viewModel.setLibrary(lib2)
        assertTrue(viewModel.listMangas.value!!.isEmpty())
        assertEquals(lib2, viewModel.getLibrary())
    }

    @Test
    fun `add should update liveData`() {
        val manga = Manga(1L, 1L, File("f1.zip"))
        viewModel.add(manga)
        
        assertEquals(1, viewModel.listMangas.value!!.size)
        assertEquals(manga, viewModel.listMangas.value!![0])
    }

    @Test
    fun `sorted should reorder items by name`() {
        val mangaB = Manga(1L, 1L, File("b.zip"))
        val mangaA = Manga(1L, 2L, File("a.zip"))
        viewModel.setList(arrayListOf(mangaB, mangaA))
        
        viewModel.sorted(Order.Name, isDesc = false)
        assertEquals("a", viewModel.listMangas.value!![0].fileName)
        assertEquals("b", viewModel.listMangas.value!![1].fileName)
    }

    @Test
    fun `changeLibraryType should cycle through types`() {
        viewModel.setLibraryType(LibraryMangaType.LINE)
        viewModel.changeLibraryType()
        assertEquals(LibraryMangaType.GRID_BIG, viewModel.libraryType.value)
    }

    @Test
    fun `stack library operations should preserve and restore state`() {
        val lib1 = Library(id = 1L, title = "Lib 1")
        viewModel.setLibrary(lib1)
        val manga = Manga(1L, 1L, File("f1.zip"))
        viewModel.add(manga)
        
        viewModel.addStackLibrary("stack1", lib1)
        
        val lib2 = Library(id = 2L, title = "Lib 2")
        viewModel.setLibrary(lib2)
        assertTrue(viewModel.listMangas.value!!.isEmpty())
        
        viewModel.restoreLastStackLibrary("stack1")
        assertEquals(1, viewModel.listMangas.value!!.size)
        assertEquals(lib1, viewModel.getLibrary())
    }
}
