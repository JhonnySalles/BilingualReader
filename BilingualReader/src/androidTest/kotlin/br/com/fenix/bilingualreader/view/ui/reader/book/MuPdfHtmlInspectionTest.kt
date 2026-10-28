package br.com.fenix.bilingualreader.view.ui.reader.book

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.ebook.foobnix.sys.ImageExtractor
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.util.helpers.TextUtil
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class MuPdfHtmlInspectionTest {

    @Test
    fun inspectMuPdfRawHtmlOutput() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val testContext = InstrumentationRegistry.getInstrumentation().context

        DocumentParse.init(appContext)

        val bookFile = File(appContext.cacheDir, "inspect_book.epub")
        testContext.assets.open("livro.epub").use { input ->
            FileOutputStream(bookFile).use { output ->
                input.copyTo(output)
            }
        }

        val codecDoc = ImageExtractor.getNewCodecContext(bookFile.absolutePath, "", 1080, 1920, 20)
        if (codecDoc == null) {
            Log.e("MUPDF_INSPECT", "Failed to open document codec")
            return
        }

        val pageCount = codecDoc.getPageCount(1080, 1920, 20)
        Log.i("MUPDF_INSPECT", "Total pages: $pageCount")

        for (i in 0 until minOf(5, pageCount)) {
            val page = codecDoc.getPage(i)
            val rawHtml = page.pageHTML
            val rawHtmlWithImages = page.pageHTMLWithImages
            val formatted = TextUtil.formatHtml(rawHtmlWithImages)

            Log.i("MUPDF_INSPECT", "=== PAGE $i ===")
            Log.i("MUPDF_INSPECT", "--- RAW pageHTML ---:\n$rawHtml")
            Log.i("MUPDF_INSPECT", "--- RAW pageHTMLWithImages ---:\n$rawHtmlWithImages")
            Log.i("MUPDF_INSPECT", "--- Formatted by TextUtil.formatHtml ---:\n$formatted")
            println("=== PAGE $i ===")
            println("--- RAW pageHTML ---:\n$rawHtml")
            println("--- RAW pageHTMLWithImages ---:\n$rawHtmlWithImages")
            println("--- Formatted by TextUtil.formatHtml ---:\n$formatted")
            page.recycle()
        }

        codecDoc.recycle()
    }
}
