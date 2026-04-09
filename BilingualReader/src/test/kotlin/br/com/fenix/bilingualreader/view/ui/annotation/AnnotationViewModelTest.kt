package br.com.fenix.bilingualreader.view.ui.annotation

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.entity.MangaAnnotation
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.model.interfaces.Annotation
import br.com.fenix.bilingualreader.service.repository.BookAnnotationRepository
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.MangaAnnotationRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import io.mockk.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AnnotationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: AnnotationViewModel
    private lateinit var application: Application
    private val bookAnnotationRepository: BookAnnotationRepository = mockk(relaxed = true)
    private val mangaAnnotationRepository: MangaAnnotationRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        mockkConstructor(BookAnnotationRepository::class)
        mockkConstructor(BookRepository::class)
        mockkConstructor(MangaAnnotationRepository::class)
        mockkConstructor(MangaRepository::class)
        
        every { anyConstructed<BookAnnotationRepository>().findAllOrderByBook() } returns mutableListOf()
        every { anyConstructed<MangaAnnotationRepository>().findAllOrderByManga() } returns mutableListOf()
        every { anyConstructed<BookAnnotationRepository>().save(any<BookAnnotation>()) } returns 1L
        every { anyConstructed<BookAnnotationRepository>().update(any<BookAnnotation>()) } just Runs
        every { anyConstructed<MangaAnnotationRepository>().save(any<MangaAnnotation>()) } returns 1L
        every { anyConstructed<MangaAnnotationRepository>().update(any<MangaAnnotation>()) } just Runs
        
        viewModel = AnnotationViewModel(application)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `filterType should update liveData and trigger list refresh`() {
        viewModel.filterType(Type.BOOK)
        assertEquals(Type.BOOK, viewModel.type.value)
        
        viewModel.filterType(null)
        assertNull(viewModel.type.value)
    }

    @Test
    fun `save BookAnnotation should call repository`() {
        val annotation = BookAnnotation(1L, 1, 10, 12f, MarkType.Annotation, 1.0f, "Chapter 1", "f", intArrayOf(), "text")
        viewModel.save(annotation as Annotation)
        verify { anyConstructed<BookAnnotationRepository>().save(any<BookAnnotation>()) }
        
        annotation.id = 5L
        viewModel.save(annotation as Annotation)
        verify { anyConstructed<BookAnnotationRepository>().update(any<BookAnnotation>()) }
    }

    @Test
    fun `save MangaAnnotation should call repository`() {
        val annotation = MangaAnnotation(1L, 5, 10, MarkType.PageMark, "Chapter 1", "f", "text")
        viewModel.save(annotation as Annotation)
        verify { anyConstructed<MangaAnnotationRepository>().save(any<MangaAnnotation>()) }
        
        annotation.id = 5L
        viewModel.save(annotation as Annotation)
        verify { anyConstructed<MangaAnnotationRepository>().update(any<MangaAnnotation>()) }
    }

    @Test
    fun `search should filter results`() {
        viewModel.search("query")
        // Word filter updated internally
        assertNotNull(viewModel.annotation.value)
    }

    @Test
    fun `clearFilterType should reset the set of filters`() {
        viewModel.clearFilterType()
        assertTrue(viewModel.typeFilter.value!!.isEmpty())
    }
}
