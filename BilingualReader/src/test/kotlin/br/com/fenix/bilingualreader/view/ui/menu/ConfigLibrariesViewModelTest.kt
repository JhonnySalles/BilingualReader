package br.com.fenix.bilingualreader.view.ui.menu

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.LibraryRepository
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkAll
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfigLibrariesViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val application = mockk<Application>(relaxed = true)
    private val repository = mockk<LibraryRepository>(relaxed = true)

    private lateinit var viewModel: ConfigLibrariesViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkConstructor(LibraryRepository::class)
        every { anyConstructed<LibraryRepository>().context = any() } just runs
        every { anyConstructed<LibraryRepository>().delete(any()) } just runs
        every { anyConstructed<LibraryRepository>().deleteAllByPath(any(), any(), any()) } just runs
        
        viewModel = ConfigLibrariesViewModel(application)
    }

    @org.junit.After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `newLibrary should not add if empty title or path`() {
        val library = Library(null, "", "", Libraries.JAPANESE, Type.MANGA, true, false)
        viewModel.newLibrary(library)
        assertEquals(0, viewModel.libraries.value?.size)
    }

    @Test
    fun `addLibrary should add to list`() {
        val library = Library(null, "Lib", "path", Libraries.JAPANESE, Type.MANGA, true, false)
        viewModel.addLibrary(library)
        assertEquals(1, viewModel.libraries.value?.size)
        assertEquals(library, viewModel.libraries.value?.first())
    }

    @Test
    fun `deleteLibrary should remove from list`() {
        val library = Library(1L, "Lib", "path", Libraries.JAPANESE, Type.MANGA, true, false)
        viewModel.addLibrary(library)
        assertEquals(1, viewModel.libraries.value?.size)
        
        viewModel.deleteLibrary(library)
        assertEquals(0, viewModel.libraries.value?.size)
        assertTrue(viewModel.mClearLibraries.contains(1L))
    }
}
