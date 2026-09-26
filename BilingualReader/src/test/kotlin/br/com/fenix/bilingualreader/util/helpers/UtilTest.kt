package br.com.fenix.bilingualreader.util.helpers

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.service.repository.LibrariesDAO
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UtilTest {

    private lateinit var context: Context
    private lateinit var resources: Resources

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        resources = mockk(relaxed = true)
        every { context.resources } returns resources
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testMD5String() {
        val input = "test_string"
        val result = Util.MD5(input)
        assertTrue(result.isNotEmpty())
        assertEquals(32, result.length)
    }

    @Test
    fun testMD5Stream() {
        val input = "test_string".toByteArray()
        val inputStream = ByteArrayInputStream(input)
        val result = Util.MD5(inputStream)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun testGetNameFromPath() {
        assertEquals("file.txt", Util.getNameFromPath("/path/to/file.txt"))
        assertEquals("file.txt", Util.getNameFromPath("C:\\path\\to\\file.txt"))
        assertEquals("file.txt", Util.getNameFromPath("file.txt"))
    }

    @Test
    fun testGetNameWithoutExtensionFromPath() {
        assertEquals("file", Util.getNameWithoutExtensionFromPath("/path/to/file.txt"))
        assertEquals("file.tar", Util.getNameWithoutExtensionFromPath("file.tar.gz"))
        assertEquals("file", Util.getNameWithoutExtensionFromPath("file"))
    }

    @Test
    fun testGetExtensionFromPath() {
        assertEquals("txt", Util.getExtensionFromPath("/path/to/file.txt"))
        assertEquals("gz", Util.getExtensionFromPath("file.tar.gz"))
        assertEquals("file", Util.getExtensionFromPath("file"))
    }

    @Test
    fun testGetNormalizedNameOrdering() {
        assertEquals("Manga 0000000001jpg", Util.getNormalizedNameOrdering("Manga 1.jpg"))
    }

    @Test
    fun testLanguageMappings() {
        val languages = arrayOf("Português", "English", "Japanese", "Português Google")
        every { resources.getStringArray(R.array.languages) } returns languages
        
        val map = Util.getLanguages(context)
        assertEquals(Languages.PORTUGUESE, map["Português"])
        
        assertEquals(Languages.JAPANESE, Util.stringToLanguage(context, "Japanese"))
        assertEquals("Japanese", Util.languageToString(context, Languages.JAPANESE))
    }

    @Test
    fun testMsgUtilValidPermission() {
        assertTrue(MsgUtil.validPermission(intArrayOf(android.content.pm.PackageManager.PERMISSION_GRANTED)))
        assertFalse(MsgUtil.validPermission(intArrayOf(android.content.pm.PackageManager.PERMISSION_DENIED)))
    }

    @Test
    fun testLibraryUtilGetDefault() {
        mockkObject(DataBase.Companion)
        val mockDb = mockk<DataBase>(relaxed = true)
        val mockDao = mockk<LibrariesDAO>(relaxed = true)
        
        every { DataBase.getDataBase(context) } returns mockDb
        every { mockDb.getLibrariesDao() } returns mockDao
        every { mockDao.getDefault(any()) } returns null
        
        every { context.getString(R.string.manga_library_default) } returns "Padrao"
        
        val result = LibraryUtil.getDefault(context, Type.MANGA)
        assertNotNull(result)
        assertEquals("Padrao", result.title)
        assertEquals(Type.MANGA, result.type)
    }

    @Test
    fun testTextUtilReplaceHtmlTTS() {
        val html = "<p>Hello <b>World</b>!</p><br/>Next line."
        val result = TextUtil.replaceHtmlTTS(html)
        assertTrue(result.contains("Hello World! Next line."))
    }

    @Test
    fun testColorUtilConversion() {
        val color = Color.RED
        val hex = ColorUtil.getColor(color)
        assertEquals("#FF0000", hex.uppercase())
        
        val colorBack = ColorUtil.getColor("#FF0000")
        assertEquals(Color.RED, colorBack)
    }

    @Test
    fun testListUtil() {
        val csv = "item1;item2;item3"
        val list = ListUtil.listFromString(csv)
        assertEquals(3, list.size)
        assertEquals("item1", list[0])
    }

    @Test
    fun testImageUtilBase64() {
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        val encoded = ImageUtil.encodeImageBase64(mockBitmap)
        assertNotNull(encoded)
    }

    @Test
    fun testGetNameWithoutVolumeAndChapter() {
        assertEquals("Manga Title", Util.getNameWithoutVolumeAndChapter("Manga Title - Volume 01"))
        assertEquals("Manga Title", Util.getNameWithoutVolumeAndChapter("Manga Title volume 01"))
        assertEquals("Manga Title", Util.getNameWithoutVolumeAndChapter("Manga Title capítulo 01"))
        assertEquals("Plain Name", Util.getNameWithoutVolumeAndChapter("Plain Name"))
    }

    @Test
    fun testGetChapterFromPath() {
        assertEquals(1f, Util.getChapterFromPath("/path/to/Capitulo 1/"))
        assertEquals(2.5f, Util.getChapterFromPath("/path/to/capítulo 2.5\\"))
        assertEquals(-1f, Util.getChapterFromPath("/path/to/invalid/"))
    }

    @Test
    fun testFileUtilFormatSize() {
        assertEquals("500 B", FileUtil.formatSize(500))
        assertEquals("1.0 KB", FileUtil.formatSize(1024))
        assertEquals("1.0 MB", FileUtil.formatSize(1024 * 1024))
    }

    @Test
    fun testGetMangaSeparatorUnreadAndOthers() {
        every { context.getString(R.string.manga_library_separator_favorite) } returns "Favoritos"
        every { context.getString(R.string.library_separator_others) } returns "Outros"
        every { context.getString(R.string.library_separator_unread) } returns "Não lido"

        val manga = mockk<br.com.fenix.bilingualreader.model.entity.Manga>(relaxed = true)
        every { manga.lastAccess } returns null
        every { manga.author } returns "   "
        every { manga.genre } returns ""
        every { manga.series } returns ""
        every { manga.title } returns " "

        // Order.LastAccess with null date should return "Não lido"
        val sepLastAccess = AdapterUtil.AdapterUtils.getMangaSeparator(context, br.com.fenix.bilingualreader.model.enums.Order.LastAccess, manga)
        assertEquals("Não lido", sepLastAccess.title)

        // Order.Author with blank string should return "Outros"
        val sepAuthor = AdapterUtil.AdapterUtils.getMangaSeparator(context, br.com.fenix.bilingualreader.model.enums.Order.Author, manga)
        assertEquals("Outros", sepAuthor.title)

        // Order.Genre with blank string should return "Outros"
        val sepGenre = AdapterUtil.AdapterUtils.getMangaSeparator(context, br.com.fenix.bilingualreader.model.enums.Order.Genre, manga)
        assertEquals("Outros", sepGenre.title)

        // Order.Series with blank string should return "Outros"
        val sepSeries = AdapterUtil.AdapterUtils.getMangaSeparator(context, br.com.fenix.bilingualreader.model.enums.Order.Series, manga)
        assertEquals("Outros", sepSeries.title)

        // Order.Name with blank title should return "Outros"
        val sepName = AdapterUtil.AdapterUtils.getMangaSeparator(context, br.com.fenix.bilingualreader.model.enums.Order.Name, manga)
        assertEquals("Outros", sepName.title)
    }

    @Test
    fun testGetBookSeparatorUnreadAndOthers() {
        every { context.getString(R.string.book_library_separator_favorite) } returns "Favoritos"
        every { context.getString(R.string.library_separator_others) } returns "Outros"
        every { context.getString(R.string.library_separator_unread) } returns "Não lido"

        val book = mockk<br.com.fenix.bilingualreader.model.entity.Book>(relaxed = true)
        every { book.lastAccess } returns null
        every { book.author } returns ""
        every { book.genre } returns "  "
        every { book.series } returns ""

        val sepLastAccess = AdapterUtil.AdapterUtils.getBookSeparator(context, br.com.fenix.bilingualreader.model.enums.Order.LastAccess, book)
        assertEquals("Não lido", sepLastAccess.title)

        val sepAuthor = AdapterUtil.AdapterUtils.getBookSeparator(context, br.com.fenix.bilingualreader.model.enums.Order.Author, book)
        assertEquals("Outros", sepAuthor.title)

        val sepGenre = AdapterUtil.AdapterUtils.getBookSeparator(context, br.com.fenix.bilingualreader.model.enums.Order.Genre, book)
        assertEquals("Outros", sepGenre.title)

        val sepSeries = AdapterUtil.AdapterUtils.getBookSeparator(context, br.com.fenix.bilingualreader.model.enums.Order.Series, book)
        assertEquals("Outros", sepSeries.title)
    }
}
