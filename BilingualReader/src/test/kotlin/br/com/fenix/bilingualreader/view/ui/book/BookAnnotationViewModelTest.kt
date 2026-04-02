package br.com.fenix.bilingualreader.view.ui.book

import android.app.Application
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.service.repository.BookAnnotationRepository
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import br.com.fenix.bilingualreader.model.enums.Filter as FilterType

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BookAnnotationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: BookAnnotationViewModel
    private val application: Application = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val repository: BookAnnotationRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { application.applicationContext } returns context
        
        // Mock the constructor of BookAnnotationRepository to return our mocked repository
        mockkConstructor(BookAnnotationRepository::class)
        every { anyConstructed<BookAnnotationRepository>().save(any()) } answers { repository.save(firstArg()) }
        every { anyConstructed<BookAnnotationRepository>().update(any()) } answers { repository.update(firstArg()) }
        every { anyConstructed<BookAnnotationRepository>().delete(any()) } answers { repository.delete(firstArg()) }
        every { anyConstructed<BookAnnotationRepository>().findAll(any()) } answers { repository.findAll(firstArg()) }

        viewModel = BookAnnotationViewModel(application)
    }

    @Test
    fun `search with idBook should populate annotations and create title entries`() {
        val idBook = 1L
        val annotation1 = BookAnnotation(idBook, 1.0f, "Chapter 1", "Text 1", "Note 1").apply { color = Color.Yellow }
        val annotation2 = BookAnnotation(idBook, 1.0f, "Chapter 1", "Text 2", "Note 2").apply { color = Color.Green }
        val annotation3 = BookAnnotation(idBook, 2.0f, "Chapter 2", "Text 3", "Note 3").apply { color = Color.Yellow }
        
        every { repository.findAll(idBook) } returns mutableListOf(annotation1, annotation2, annotation3)

        viewModel.search(idBook)

        val result = viewModel.annotation.value
        assertNotNull(result)
        // Total expected: Title(Ch1), Ann1, Ann2, Title(Ch2), Ann3 = 5
        assertEquals(5, result!!.size)
        
        assertTrue(result[0].isTitle)
        assertEquals("Chapter 1", result[0].chapter)
        assertEquals(annotation1, result[1])
        assertEquals(annotation2, result[2])
        assertTrue(result[3].isTitle)
        assertEquals("Chapter 2", result[3].chapter)
        assertEquals(annotation3, result[4])
    }

    @Test
    fun `save should call repository save or update`() {
        val newAnnotation = BookAnnotation(1, 1.0f, "Chapter 1", "Text", "Note")
        viewModel.save(newAnnotation)
        verify { repository.save(newAnnotation) }

        val existingAnnotation = BookAnnotation(1, 1.0f, "Chapter 1", "Text", "Note").apply { id = 10L }
        viewModel.save(existingAnnotation)
        verify { repository.update(existingAnnotation) }
    }

    @Test
    fun `delete should call repository delete and remove from list`() {
        val idBook = 1L
        val annotation = BookAnnotation(idBook, 1.0f, "Chapter 1", "Text", "Note").apply { id = 10L }
        every { repository.findAll(idBook) } returns mutableListOf(annotation)
        
        viewModel.search(idBook)
        assertEquals(2, viewModel.annotation.value!!.size) // Title + Annotation

        viewModel.delete(annotation)
        
        verify { repository.delete(annotation) }
        // After deleting the only annotation in Chapter 1, the title should also be removed
        assertEquals(0, viewModel.annotation.value!!.size)
    }

    @Test
    fun `filterByType should only show matching annotations`() {
        val idBook = 1L
        val favAnn = BookAnnotation(idBook, 1.0f, "Ch1", "Fav", "").apply { favorite = true }
        val normalAnn = BookAnnotation(idBook, 1.0f, "Ch1", "Normal", "").apply { favorite = false }
        
        every { repository.findAll(idBook) } returns mutableListOf(favAnn, normalAnn)
        viewModel.search(idBook)

        // Filter by Favorite
        viewModel.filterType(FilterType.Favorite)
        
        val filtered = viewModel.annotation.value
        assertNotNull(filtered)
        // Expect Title + favAnn
        assertEquals(2, filtered!!.size)
        assertTrue(filtered.any { it.favorite })
        assertFalse(filtered.any { it.text == "Normal" })
    }

    @Test
    fun `filterByColor should only show matching colors`() {
        val idBook = 1L
        val yellowAnn = BookAnnotation(idBook, 1.0f, "Ch1", "Yellow", "").apply { color = Color.Yellow }
        val greenAnn = BookAnnotation(idBook, 1.0f, "Ch1", "Green", "").apply { color = Color.Green }
        
        every { repository.findAll(idBook) } returns mutableListOf(yellowAnn, greenAnn)
        viewModel.search(idBook)

        viewModel.filterColor(Color.Yellow)
        
        val filtered = viewModel.annotation.value
        assertEquals(2, filtered!!.size) // Title + Yellow
        assertTrue(filtered.any { it.color == Color.Yellow })
        assertFalse(filtered.any { it.color == Color.Green })
    }

    @Test
    fun `search(text) should filter by content`() {
        val idBook = 1L
        val ann1 = BookAnnotation(idBook, 1.0f, "Ch1", "Target word", "")
        val ann2 = BookAnnotation(idBook, 1.0f, "Ch1", "Other thing", "")
        
        every { repository.findAll(idBook) } returns mutableListOf(ann1, ann2)
        viewModel.search(idBook)

        viewModel.search("target")
        
        val filtered = viewModel.annotation.value
        assertEquals(2, filtered!!.size) // Title + ann1
        assertTrue(filtered[1].text.contains("Target"))
    }
}
