package org.ebookdroid.droids

import br.com.ebook.extractor.DocxBookExtractor
import br.com.ebook.core.BookContent
import br.com.ebook.foobnix.ext.CacheZipUtils
import kotlinx.coroutines.runBlocking
import org.ebookdroid.core.codec.CodecDocument
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument
import org.ebookdroid.droids.mupdf.codec.PdfContext
import org.slf4j.LoggerFactory

class DocxContext : PdfContext() {
    private val LOGGER = LoggerFactory.getLogger(DocxContext::class.java)

    override fun openDocumentInner(fileName: String, password: String): CodecDocument? {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            throw IllegalStateException("Document extraction must not run on the UI thread.")
        }
        var finalFileName = fileName
        var notes: Map<String, String>? = null

        try {
            val contentResult = runBlocking(br.com.ebook.core.EbookDispatcher.dispatcher) {
                DocxBookExtractor.extractContent(fileName, CacheZipUtils.CACHE_BOOK_DIR?.path ?: "")
            }
            val content = contentResult.getOrThrow()
            if (content is BookContent.HtmlFile) {
                finalFileName = content.path
                notes = content.notes
            }
            LOGGER.info("docx new file name: {}", finalFileName)
        } catch (e: Exception) {
            LOGGER.error("Error open document inner in DocxContext: {}", e.message, e)
        }

        val muPdfDocument = MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, finalFileName, password)
        muPdfDocument.setFootNotes(notes)
        return muPdfDocument
    }
}
