package br.com.fenix.bilingualreader.view.ui.history

import android.app.Application
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.HistoryGroup
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.Separator
import br.com.fenix.bilingualreader.model.enums.HistoryType
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.LibraryRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.service.repository.TagsRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
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
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.Executors

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class HistoryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: HistoryViewModel
    private lateinit var application: Application
    private val context: Context by lazy { application.applicationContext }

    private val mangaRepository: MangaRepository = mockk(relaxed = true)
    private val bookRepository: BookRepository = mockk(relaxed = true)
    private val libraryRepository: LibraryRepository = mockk(relaxed = true)
    private val tagsRepository: TagsRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher
        every { Dispatchers.Default } returns testDispatcher

        mockkStatic("com.google.firebase.crashlytics.ktx.FirebaseCrashlyticsKt")
        try {
            mockkStatic("kotlinx.coroutines.ThreadPoolDispatcherKt")
            val realDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
            every { newSingleThreadContext(any()) } returns realDispatcher
        } catch (_: Throwable) {}

        val mockCrashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { Firebase.crashlytics } returns mockCrashlytics
        every { mockCrashlytics.recordException(any()) } just Runs
        every { mockCrashlytics.log(any()) } just Runs
        every { mockCrashlytics.setCustomKey(any(), any<String>()) } just Runs

        application = ApplicationProvider.getApplicationContext()

        mockkConstructor(MangaRepository::class)
        every { anyConstructed<MangaRepository>().listHistory() } answers { mangaRepository.listHistory() }
        every { anyConstructed<MangaRepository>().save(any<Manga>(), any()) } answers { mangaRepository.save(firstArg<Manga>(), secondArg()) }
        every { anyConstructed<MangaRepository>().update(any<Manga>(), any()) } answers { mangaRepository.update(firstArg<Manga>(), secondArg()) }
        every { anyConstructed<MangaRepository>().delete(any<Manga>()) } answers { mangaRepository.delete(firstArg<Manga>()) }

        mockkConstructor(BookRepository::class)
        every { anyConstructed<BookRepository>().listHistory() } answers { bookRepository.listHistory() }
        every { anyConstructed<BookRepository>().save(any<Book>(), any()) } answers { bookRepository.save(firstArg<Book>(), secondArg()) }
        every { anyConstructed<BookRepository>().update(any<Book>(), any()) } answers { bookRepository.update(firstArg<Book>(), secondArg()) }
        every { anyConstructed<BookRepository>().delete(any<Book>()) } answers { bookRepository.delete(firstArg<Book>()) }

        mockkConstructor(LibraryRepository::class)
        mockkConstructor(TagsRepository::class)
        every { anyConstructed<TagsRepository>().list() } returns arrayListOf()

        GeneralConsts.getSharedPreferences(application).edit()
            .putString(GeneralConsts.KEYS.LIBRARY.HISTORY_TYPE, HistoryType.SEPARATOR_LINE.toString())
            .commit()

        viewModel = HistoryViewModel(application)
    }

    @After
    fun tearDown() {
        try {
            unmockkAll()
        } catch (_: Throwable) {}
        Dispatchers.resetMain()
    }

    @Test
    fun `list should fetch from both repositories and sort by last access`() = runTest {
        val now = LocalDateTime.now()
        val book = Book(null, null, File("/path/Book1")).apply {
            id = 1L
            lastAccess = now.minusHours(1)
        }
        val manga = Manga(null, null, File("/path/Manga1")).apply {
            id = 2L
            lastAccess = now
        }

        every { bookRepository.listHistory() } returns mutableListOf(book)
        every { mangaRepository.listHistory() } returns mutableListOf(manga)

        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher

        viewModel.list()

        val history = viewModel.history.value
        assertNotNull(history)
        val content = history!!.filterIsInstance<br.com.fenix.bilingualreader.model.interfaces.History>()
        assertEquals(2, content.size)
        assertEquals("Manga1", content[0].name)
        assertEquals("Book1", content[1].name)
        assertTrue(history.any { it is Separator })
    }

    @Test
    fun `filterType should filter history by manga or book`() {
        val book = Book(1L, 1L, File("/path/Book1")).apply { lastAccess = LocalDateTime.now() }
        val manga = Manga(1L, 1L, File("/path/Manga1")).apply { lastAccess = LocalDateTime.now() }

        viewModel.update(mutableListOf(book, manga))

        viewModel.filterType(Type.MANGA)
        val mangaList = viewModel.history.value!!.filterIsInstance<br.com.fenix.bilingualreader.model.interfaces.History>()
        assertEquals(1, mangaList.size)
        assertTrue(mangaList[0] is Manga)

        viewModel.filterType(Type.BOOK)
        val bookList = viewModel.history.value!!.filterIsInstance<br.com.fenix.bilingualreader.model.interfaces.History>()
        assertEquals(1, bookList.size)
        assertTrue(bookList[0] is Book)
    }

    @Test
    fun `save should call appropriate repository based on entity type`() {
        val book = Book(null, null, File("/path/Book"))
        val manga = Manga(null, null, File("/path/Manga"))

        viewModel.save(book)
        verify { bookRepository.save(book, any()) }

        viewModel.save(manga)
        verify { mangaRepository.save(manga, any()) }
    }

    @Test
    fun `changeHistoryType should cycle layout types`() {
        assertEquals(HistoryType.SEPARATOR_LINE, viewModel.historyType.value)

        viewModel.changeHistoryType()
        assertEquals(HistoryType.SEPARATOR_CAROUSEL, viewModel.historyType.value)

        viewModel.changeHistoryType()
        assertEquals(HistoryType.SEPARATOR_BIG, viewModel.historyType.value)

        viewModel.changeHistoryType()
        assertEquals(HistoryType.SEPARATOR_MEDIUM, viewModel.historyType.value)

        viewModel.changeHistoryType()
        assertEquals(HistoryType.LINE, viewModel.historyType.value)

        viewModel.changeHistoryType()
        assertEquals(HistoryType.SEPARATOR_LINE, viewModel.historyType.value)
    }

    @Test
    fun `legacy prefs LINE_DATE migrates to SEPARATOR_LINE`() {
        GeneralConsts.getSharedPreferences(application).edit()
            .putString(GeneralConsts.KEYS.LIBRARY.HISTORY_TYPE, "LINE_DATE")
            .commit()

        val migrated = HistoryViewModel(application)
        assertEquals(HistoryType.SEPARATOR_LINE, migrated.historyType.value)
    }

    @Test
    fun `series order should group items by series`() {
        val now = LocalDateTime.now()
        val manga1 = Manga(1L, 1L, File("/path/Manga1")).apply {
            lastAccess = now
            series = "One Piece"
        }
        val manga2 = Manga(2L, 1L, File("/path/Manga2")).apply {
            lastAccess = now.minusHours(1)
            series = "One Piece"
        }
        val book = Book(3L, 1L, File("/path/Book1")).apply {
            lastAccess = now.minusHours(2)
            series = "Lord of the Rings"
        }

        viewModel.update(mutableListOf(manga1, manga2, book))
        viewModel.setHistoryType(HistoryType.SEPARATOR_LINE)
        viewModel.sorted(Order.Series, false)

        val history = viewModel.history.value!!
        assertTrue(history.any { it is Separator && it.title == "One Piece" })
        assertTrue(history.any { it is Separator && it.title == "Lord of the Rings" })
        assertEquals(2, history.filterIsInstance<br.com.fenix.bilingualreader.model.interfaces.History>()
            .count { it.series == "One Piece" })
    }

    @Test
    fun `author order should group items by author`() {
        val now = LocalDateTime.now()
        val manga1 = Manga(1L, 1L, File("/path/Manga1")).apply {
            lastAccess = now
            author = "Oda"
        }
        val manga2 = Manga(2L, 1L, File("/path/Manga2")).apply {
            lastAccess = now.minusHours(1)
            author = "Oda"
        }
        val book = Book(3L, 1L, File("/path/Book1")).apply {
            lastAccess = now.minusHours(2)
            author = "Tolkien"
        }

        viewModel.update(mutableListOf(manga1, manga2, book))
        viewModel.setHistoryType(HistoryType.SEPARATOR_LINE)
        viewModel.sorted(Order.Author, false)

        val history = viewModel.history.value!!
        assertTrue(history.any { it is Separator && it.title == "oda" })
        assertTrue(history.any { it is Separator && it.title == "tolkien" })
    }

    @Test
    fun `LINE type should not include separators`() {
        val manga = Manga(1L, 1L, File("/path/Manga1")).apply { lastAccess = LocalDateTime.now() }
        viewModel.update(mutableListOf(manga))
        viewModel.setHistoryType(HistoryType.LINE)

        val history = viewModel.history.value!!
        assertFalse(history.any { it is Separator })
        assertEquals(1, history.filterIsInstance<br.com.fenix.bilingualreader.model.interfaces.History>().size)
    }

    @Test
    fun `carousel type should include HistoryGroup`() {
        val manga = Manga(1L, 1L, File("/path/Manga1")).apply {
            lastAccess = LocalDateTime.now()
            series = "One Piece"
            volume = "1"
        }
        viewModel.update(mutableListOf(manga))
        viewModel.setHistoryType(HistoryType.SEPARATOR_CAROUSEL)
        viewModel.sorted(Order.Series, false)

        val history = viewModel.history.value!!
        assertTrue(history.any { it is Separator })
        assertTrue(history.any { it is HistoryGroup })
    }

    @Test
    fun `SEPARATOR_BIG should group like SEPARATOR_LINE without HistoryGroup`() {
        val manga = Manga(1L, 1L, File("/path/Manga1")).apply {
            lastAccess = LocalDateTime.now()
            series = "One Piece"
        }
        viewModel.update(mutableListOf(manga))
        viewModel.setHistoryType(HistoryType.SEPARATOR_BIG)
        viewModel.sorted(Order.Series, false)

        val history = viewModel.history.value!!
        assertTrue(history.any { it is Separator })
        assertFalse(history.any { it is HistoryGroup })
        assertEquals(1, history.filterIsInstance<br.com.fenix.bilingualreader.model.interfaces.History>().size)
    }

    @Test
    fun `SEPARATOR_MEDIUM should group like SEPARATOR_LINE without HistoryGroup`() {
        val manga = Manga(1L, 1L, File("/path/Manga1")).apply {
            lastAccess = LocalDateTime.now()
            author = "Oda"
        }
        viewModel.update(mutableListOf(manga))
        viewModel.setHistoryType(HistoryType.SEPARATOR_MEDIUM)
        viewModel.sorted(Order.Author, false)

        val history = viewModel.history.value!!
        assertTrue(history.any { it is Separator && it.title == "oda" })
        assertFalse(history.any { it is HistoryGroup })
    }

    @Test
    fun `filterYears should filter by lastAccess year`() {
        val manga2024 = Manga(1L, 1L, File("/path/Manga1")).apply {
            lastAccess = LocalDateTime.of(2024, 5, 1, 10, 0)
        }
        val manga2025 = Manga(2L, 1L, File("/path/Manga2")).apply {
            lastAccess = LocalDateTime.of(2025, 3, 1, 10, 0)
        }
        viewModel.update(mutableListOf(manga2024, manga2025))
        viewModel.setHistoryType(HistoryType.LINE)

        viewModel.filterYears(setOf(2025))
        val filtered = viewModel.history.value!!.filterIsInstance<br.com.fenix.bilingualreader.model.interfaces.History>()
        assertEquals(1, filtered.size)
        assertEquals(2025, filtered[0].lastAccess!!.year)

        assertTrue(viewModel.availableYears.value!!.contains(2024))
        assertTrue(viewModel.availableYears.value!!.contains(2025))
    }

    @Test
    fun `filterContentTypes should support multi select`() {
        val book = Book(1L, 1L, File("/path/Book1")).apply { lastAccess = LocalDateTime.now() }
        val manga = Manga(2L, 1L, File("/path/Manga1")).apply { lastAccess = LocalDateTime.now() }
        viewModel.update(mutableListOf(book, manga))
        viewModel.setHistoryType(HistoryType.LINE)

        viewModel.filterContentTypes(setOf(Type.MANGA, Type.BOOK))
        assertEquals(2, viewModel.history.value!!.filterIsInstance<br.com.fenix.bilingualreader.model.interfaces.History>().size)
        assertTrue(viewModel.contentTypes.value!!.isEmpty())

        viewModel.filterContentTypes(setOf(Type.BOOK))
        val books = viewModel.history.value!!.filterIsInstance<br.com.fenix.bilingualreader.model.interfaces.History>()
        assertEquals(1, books.size)
        assertTrue(books[0] is Book)
    }

    @Test
    fun `filterLibraries should support multi select`() {
        val libA = br.com.fenix.bilingualreader.model.entity.Library(10L, "Lib A", "/a")
        val libB = br.com.fenix.bilingualreader.model.entity.Library(20L, "Lib B", "/b")
        val mangaA = Manga(10L, 1L, File("/path/Manga1")).apply {
            lastAccess = LocalDateTime.now()
        }
        val mangaB = Manga(20L, 2L, File("/path/Manga2")).apply {
            lastAccess = LocalDateTime.now().minusHours(1)
        }
        val mangaC = Manga(30L, 3L, File("/path/Manga3")).apply {
            lastAccess = LocalDateTime.now().minusHours(2)
        }
        viewModel.update(mutableListOf(mangaA, mangaB, mangaC))
        viewModel.setHistoryType(HistoryType.LINE)

        viewModel.filterLibraries(setOf(libA, libB))
        val filtered = viewModel.history.value!!.filterIsInstance<br.com.fenix.bilingualreader.model.interfaces.History>()
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.fkLibrary == 10L || it.fkLibrary == 20L })
    }
}
