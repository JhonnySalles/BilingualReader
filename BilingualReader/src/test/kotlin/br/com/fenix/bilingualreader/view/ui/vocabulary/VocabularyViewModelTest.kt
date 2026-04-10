package br.com.fenix.bilingualreader.view.ui.vocabulary

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.paging.PagingSource
import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.repository.VocabularyRepository
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
class VocabularyViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: VocabularyViewModel
    private val application: Application = mockk(relaxed = true)
    private val repository: VocabularyRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        mockkConstructor(VocabularyRepository::class)
        every { anyConstructed<VocabularyRepository>().list(any(), any(), any()) } answers { 
            repository.list(firstArg(), secondArg(), thirdArg()) 
        }
        every { anyConstructed<VocabularyRepository>().update(any()) } answers { 
            repository.update(firstArg()) 
        }

        viewModel = VocabularyViewModel(application)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `setQuery should update isQuery LiveData`() {
        viewModel.setQuery("test")
        assertEquals(true, viewModel.isQuery.value)
    }

    @Test
    fun `sorted should update order LiveData`() {
        viewModel.sorted(Order.Name, isDesc = true)
        assertEquals(Pair(Order.Name, true), viewModel.order.value)
    }

    @Test
    fun `PagingSource should load data correctly`() = runTest {
        val query = viewModel.Query("test")
        val pagingSource = viewModel.PagingSource(repository, query)
        
        val expectedList = listOf(Vocabulary(null, "word", "meaning", null, null, null, 0, false, false, 0))
        every { repository.list(any(), 0, 10) } returns expectedList

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 10,
                placeholdersEnabled = false
            )
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        val page = result as PagingSource.LoadResult.Page
        assertEquals(expectedList, page.data)
        assertNull(page.prevKey)
        assertEquals(1, page.nextKey)
    }

    @Test
    fun `update should call repository`() {
        val vocab = Vocabulary(null, "word", "meaning", null, null, null, 0, false, false, 0)
        viewModel.update(vocab)
        verify { repository.update(vocab) }
    }
}
