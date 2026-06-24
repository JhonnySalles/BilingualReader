package br.com.fenix.bilingualreader.view.ui.reader.manga

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.MangaAnnotation
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.service.repository.HistoryRepository
import br.com.fenix.bilingualreader.service.repository.MangaAnnotationRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class MangaReaderViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: MangaReaderViewModel
    private lateinit var application: Application
    private val mangaRepository: MangaRepository = mockk(relaxed = true)
    private val historyRepository: HistoryRepository = mockk(relaxed = true)
    private val annotationRepository: MangaAnnotationRepository = mockk(relaxed = true)

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        
        mockkConstructor(MangaRepository::class)
        mockkConstructor(HistoryRepository::class)
        mockkConstructor(MangaAnnotationRepository::class)
        
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher
        every { Dispatchers.Default } returns testDispatcher
        
        mockkStatic("com.google.firebase.crashlytics.ktx.FirebaseCrashlyticsKt")
        val mockCrashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { Firebase.crashlytics } returns mockCrashlytics
        every { mockCrashlytics.recordException(any()) } just Runs
        every { mockCrashlytics.log(any()) } just Runs
        every { mockCrashlytics.setCustomKey(any(), any<String>()) } just Runs
        
        every { anyConstructed<MangaRepository>().update(any()) } just Runs
        every { anyConstructed<MangaAnnotationRepository>().save(any()) } returns 1L
        every { anyConstructed<MangaAnnotationRepository>().update(any()) } just Runs
        every { anyConstructed<MangaAnnotationRepository>().delete(any()) } just Runs
        every { anyConstructed<MangaAnnotationRepository>().findByManga(any()) } returns mutableListOf()

        viewModel = MangaReaderViewModel(application)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `changeCustomFilter should update liveData and generate filters`() {
        viewModel.changeCustomFilter(true)
        assertEquals(true, viewModel.customFilter.value)
        assertNotNull(viewModel.filters.value)
        assertTrue(viewModel.filters.value!!.isNotEmpty())
    }

    @Test
    fun `changeGrayScale should update liveData`() {
        viewModel.changeGrayScale(true)
        assertEquals(true, viewModel.grayScale.value)
    }

    @Test
    fun `addOcrItem should append items and notify observers`() {
        viewModel.clearOcrItem()
        viewModel.addOcrItem("Text 1")
        viewModel.addOcrItem("Text 2")
        
        val list = viewModel.ocrItem.value
        assertNotNull(list)
        assertEquals(2, list?.size)
        assertTrue(list!!.contains("Text 1"))
        assertTrue(list.contains("Text 2"))
    }

    @Test
    fun `save annotation should refresh list with titles`() {
        val annotation = MangaAnnotation(id_manga = 1L, page = 5, pages = 10, type = MarkType.PageMark, chapter = "Chapter 1", folder = "f", annotation = "text")
        
        viewModel.save(annotation as MangaAnnotation)
        
        val list = viewModel.annotation.value
        assertNotNull(list)
        // Title + Item
        assertTrue(list!!.any { it.isTitle && it.chapter == "Chapter 1" })
        assertTrue(list.any { !it.isTitle && it.page == 5 })
    }

    @Test
    fun `delete annotation should update list`() {
        val annotation = MangaAnnotation(id_manga = 1L, page = 5, pages = 10, type = MarkType.PageMark, chapter = "Chapter 1", folder = "f", annotation = "text").apply { id = 10L }
        viewModel.save(annotation as MangaAnnotation)
        
        viewModel.delete(annotation)
        
        val list = viewModel.annotation.value
        assertTrue(list.isNullOrEmpty() || list.none { it.id == 10L })
    }
}
