package org.ebookdroid.droids

import br.com.ebook.extractor.TxtBookExtractor
import br.com.ebook.core.BookContent
import br.com.ebook.foobnix.ext.CacheZipUtils
import kotlinx.coroutines.runBlocking
import org.ebookdroid.core.codec.CodecDocument
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument
import org.ebookdroid.droids.mupdf.codec.PdfContext
import org.slf4j.LoggerFactory
import java.io.IOException

class TxtContext : PdfContext() {
    private val LOGGER = LoggerFactory.getLogger(TxtContext::class.java)

    override fun openDocumentInner(fileName: String, password: String): CodecDocument? {
        var finalFileName = fileName
        var notes: Map<String, String>? = null

        try {
            val contentResult = runBlocking {
                TxtBookExtractor.extractContent(fileName, CacheZipUtils.CACHE_BOOK_DIR.path)
            }
            val content = contentResult.getOrThrow()
            if (content is BookContent.HtmlFile) {
                finalFileName = content.path
                notes = content.notes
            }
            LOGGER.info("new file name: {}", finalFileName)
        } catch (e: Exception) {
            LOGGER.error("Error open document inner in TxtContext: {}", e.message, e)
        }

        val muPdfDocument = MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, finalFileName, password)
        muPdfDocument.setFootNotes(notes)
        return muPdfDocument
    }
}
