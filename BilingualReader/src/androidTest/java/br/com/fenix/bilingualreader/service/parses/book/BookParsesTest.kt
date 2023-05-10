package br.com.fenix.bilingualreader.service.parses.book

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.FontUtil
import br.com.fenix.bilingualreader.utils.TestUtils
import junit.framework.TestCase
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class BookParsesTest {

    // Inform a file test here
    private val filePath = "/storage/1D01-1E06/Livros/"
    private val bookEPUB: Book = TestUtils.getBook(ApplicationProvider.getApplicationContext(), filePath + "Russian-Roulette-epub.epub")
    private val bookMOBI: Book = TestUtils.getBook(ApplicationProvider.getApplicationContext(), filePath + "Fundacao_-_Isaac_Asimov.pdf")
    private val bookTXT: Book = TestUtils.getBook(ApplicationProvider.getApplicationContext(), filePath + "Parecido.txt")
    private val bookFB2: Book = TestUtils.getBook(ApplicationProvider.getApplicationContext(), filePath + "Russian-Roulette-fb2.fb2")
    private val bookPDF: Book = TestUtils.getBook(ApplicationProvider.getApplicationContext(), filePath + "Fundacao_-_Isaac_Asimov.pdf")
    private val bookRTF: Book = TestUtils.getBook(ApplicationProvider.getApplicationContext(), filePath + "Russian-Roulette-rtf.rtf")

    private val bookAZW3: Book = TestUtils.getBook(ApplicationProvider.getApplicationContext(), filePath + "Russian-Roulette-azw3.azw3")
    private val bookDJVU: Book = TestUtils.getBook(ApplicationProvider.getApplicationContext(), filePath + "Download.djvu")

    private val bookPDB: Book = TestUtils.getBook(ApplicationProvider.getApplicationContext(), filePath + "Russian-Roulette-pdb.pdb")
    private val bookDOC: Book = TestUtils.getBook(ApplicationProvider.getApplicationContext(), filePath + "9780008117498.doc")
    private val bookDOCX: Book = TestUtils.getBook(ApplicationProvider.getApplicationContext(), filePath + "Teste.docx")
    private val bookODT: Book = TestUtils.getBook(ApplicationProvider.getApplicationContext(), filePath + "9780008117498.odt")

    private val books = arrayListOf(bookEPUB, bookMOBI, bookTXT, bookFB2, bookPDF, bookRTF)

    init {
        DocumentParse.init(ApplicationProvider.getApplicationContext())

        TestCase.assertFalse(
            "Not informed book path, please declare 'filePath' in " + BookParsesTest::class.java.name,
            filePath.isEmpty()
        )

       for (book in books)
           TestCase.assertTrue(
               "Book " + book.name + " (" + book.extension + ") not found, please verify book in " + BookParsesTest::class.java.name,
               book.file.exists()
           )

        //Clear cache
        val cache = File(GeneralConsts.getCacheDir(ApplicationProvider.getApplicationContext()), GeneralConsts.CACHE_FOLDER.BOOKS + '/')
        if (cache.exists() && cache.listFiles() != null)
            for (file in cache.listFiles())
                file.deleteRecursively()
    }


    private val awaitProcessSeconds = 2L
    private val fontSize = GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE_DEFAULT
    private val fontSizeDips = FontUtil.pixelToDips(ApplicationProvider.getApplicationContext(), fontSize)

    @Test
    fun `1_test_book_cover`() {
        val waiter = CountDownLatch(1)

        val parse = ImageParse(ApplicationProvider.getApplicationContext())
        for (book in books) {
            val cover = parse.getCoverPage(book.path)

            TestCase.assertTrue(
                "Book " + book.name + " (" + book.extension + ") not generate cover. Please verify in " + BookParsesTest::class.java.name,
                cover != null
            )
        }

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
    }

    @Test
    fun `2_test_book_parser`() {
        val waiter = CountDownLatch(1)

        for (book in books) {
            val parse = DocumentParse(book.path, book.password, fontSizeDips)

            TestCase.assertTrue(
                "Book " + book.name + " (" + book.extension + ") not loaded. Please verify in " + BookParsesTest::class.java.name,
                parse.isLoaded()
            )

            parse.getPageCount(
                FontUtil.pixelToDips(ApplicationProvider.getApplicationContext(), fontSize)
            )

            TestCase.assertFalse(
                "Book " + book.name + " (" + book.extension + ") not has first page. Please verify in " + BookParsesTest::class.java.name,
                parse.getPage(0).pageHTMLWithImages.isEmpty()
            )
        }

        waiter.await(awaitProcessSeconds, TimeUnit.SECONDS)
    }
}