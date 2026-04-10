package br.com.fenix.bilingualreader.view.ui.pages_link

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.LinkedFile
import br.com.fenix.bilingualreader.model.entity.LinkedPage
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.PageLinkType
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.service.repository.FileLinkRepository
import br.com.fenix.bilingualreader.service.controller.SubTitleController
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PagesLinkViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: PagesLinkViewModel
    private lateinit var application: Application
    private val repository: FileLinkRepository = mockk(relaxed = true)
    private val manga: Manga = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()

        mockkConstructor(FileLinkRepository::class)
        every { repository.get(any()) } returns null
        every { anyConstructed<FileLinkRepository>().get(any<Manga>()) } answers { repository.get(firstArg<Manga>()) }
        every { anyConstructed<FileLinkRepository>().save(any<LinkedFile>()) } answers { repository.save(firstArg<LinkedFile>()) }
        every { anyConstructed<FileLinkRepository>().update(any<LinkedFile>()) } answers { repository.update(firstArg<LinkedFile>()) }
        every { anyConstructed<FileLinkRepository>().delete(any<LinkedFile>()) } answers { repository.delete(firstArg<LinkedFile>()) }
        every { anyConstructed<FileLinkRepository>().delete(any<Manga>()) } answers { repository.delete(firstArg<Manga>()) }

        mockkObject(SubTitleController.Companion)
        val subTitleController: SubTitleController = mockk<SubTitleController>(relaxed = true)
        every { SubTitleController.getInstance(any()) } returns subTitleController
        every { subTitleController.getFileLink() } returns null

        every { manga.id } returns 1L
        every { manga.fileName } returns "manga_test"
        every { manga.file } returns File("manga_test.zip")
        every { manga.path } returns "manga_test.zip"
        every { manga.pages } returns 2

        viewModel = PagesLinkViewModel(application)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `loadManga should initialize pagesLink with manga pages`() {
        val parse: Parse = mockk(relaxed = true)
        every { parse.numPages() } returns 2
        every { parse.getPagePath(0) } returns "page1.jpg"
        every { parse.getPagePath(1) } returns "page2.jpg"

        mockkObject(br.com.fenix.bilingualreader.service.parses.manga.ParseFactory)
        every { br.com.fenix.bilingualreader.service.parses.manga.ParseFactory.create(any<File>()) } returns parse

        val refresh: (Int?, PageLinkType) -> Unit = mockk(relaxed = true)
        
        viewModel.loadManga(manga, refresh)

        assertEquals(2, viewModel.pagesLink.value!!.size)
        assertEquals("page1.jpg", viewModel.pagesLink.value!![0].mangaPageName)
        verify { refresh(null, PageLinkType.MANGA) }
    }

    @Test
    fun `save should call repository save or update`() {
        val linkedFile = LinkedFile(manga)
        linkedFile.id = null
        viewModel.save(linkedFile)
        verify { repository.save(linkedFile) }

        linkedFile.id = 5L
        viewModel.save(linkedFile)
        verify { repository.update(linkedFile) }
    }

    @Test
    fun `setLanguage should update liveData`() {
        viewModel.setLanguage(Languages.JAPANESE)
        assertEquals(Languages.JAPANESE, viewModel.language.value)

        viewModel.setLanguage(isClear = true)
        assertEquals(Languages.PORTUGUESE, viewModel.language.value)
    }

    @Test
    fun `onMove should correctly shift pages left or right`() {
        val parse: Parse = mockk(relaxed = true)
        every { parse.numPages() } returns 3
        every { parse.getPagePath(any()) } returns "img.jpg"
        mockkObject(br.com.fenix.bilingualreader.service.parses.manga.ParseFactory)
        every { br.com.fenix.bilingualreader.service.parses.manga.ParseFactory.create(any<File>()) } returns parse
        
        viewModel.loadManga(manga, { _, _ -> })
        
        val p1 = viewModel.getPageLink(0)!!
        val p2 = viewModel.getPageLink(1)!!
        
        viewModel.onMove(p1, p2)
        
        assertNotNull(viewModel.pagesLink.value)
    }
}
