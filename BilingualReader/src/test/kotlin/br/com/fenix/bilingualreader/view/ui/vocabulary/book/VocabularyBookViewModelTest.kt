package br.com.fenix.bilingualreader.view.ui.vocabulary.book

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.paging.PagingSource
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.repository.VocabularyRepository
import br.com.fenix.bilingualreader.view.ui.vocabulary.VocabularyActivity
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VocabularyBookViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: VocabularyBookViewModel
    private lateinit var application: Application
    private val repository: VocabularyRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        application = ApplicationProvider.getApplicationContext()
        
        // Mock static properties of VocabularyActivity before initializing ViewModel
        mockkObject(VocabularyActivity.VocabularyData)
        every { VocabularyActivity.mSortType } returns Order.Description
        every { VocabularyActivity.mSortDesc } returns false
        every { VocabularyActivity.mVocabularySelect } returns ""
        every { VocabularyActivity.mIsFavorite } returns false

        mockkConstructor(VocabularyRepository::class)
        every { anyConstructed<VocabularyRepository>().listBook(any(), any(), any()) } answers { 
            repository.listBook(firstArg(), secondArg(), thirdArg()) 
        }
        every { anyConstructed<VocabularyRepository>().findVocabBookByVocabulary(any(), any()) } answers { 
            repository.findVocabBookByVocabulary(firstArg(), secondArg())
        }

        // Default behavior for repository mock
        every { repository.findVocabBookByVocabulary(any(), any()) } answers { secondArg() }

        viewModel = VocabularyBookViewModel(application)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `setQuery with book and vocabulary should update isQuery LiveData`() {
        viewModel.setQuery("BookName", "Word", false)
        assertEquals(true, viewModel.isQuery.value)
    }

    @Test
    fun `PagingSource should load and call findVocabBookByVocabulary`() = runTest {
        val query = viewModel.Query(book = "BookName")
        val pagingSource = viewModel.PagingSource(repository, query)
        
        val vocab = Vocabulary(null, "word", "meaning", null, null, null, 0, false, false, 0).apply { id = 1L }
        val expectedList = listOf(vocab)
        every { repository.listBook(any(), 0, 10) } returns expectedList

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 10,
                placeholdersEnabled = false
            )
        )

        assertTrue("Result should be Page", result is PagingSource.LoadResult.Page)
        val page = result as PagingSource.LoadResult.Page
        assertEquals(expectedList, page.data)
        
        // Verify cross-reference call
        verify { repository.findVocabBookByVocabulary("BookName", vocab) }
    }

    @Test
    fun `sorted should update order LiveData and query`() {
        viewModel.sorted(Order.Name, isDesc = true)
        assertEquals(Pair(Order.Name, true), viewModel.order.value)
        assertTrue(viewModel.isQuery.value!!)
    }
}
