package org.ebookdroid.droids

import br.com.ebook.extractor.EpubBookExtractor
import br.com.ebook.foobnix.ext.CacheZipUtils
import br.com.ebook.foobnix.pdf.info.ExtUtils
import br.com.ebook.foobnix.pdf.info.JsonHelper
import br.com.ebook.foobnix.pdf.info.model.BookCSS
import br.com.ebook.foobnix.sys.TempHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.ebookdroid.core.codec.CodecDocument
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument
import org.ebookdroid.droids.mupdf.codec.PdfContext
import org.slf4j.LoggerFactory
import java.io.File

class EpubContext : PdfContext() {
    private val LOGGER = LoggerFactory.getLogger(EpubContext::class.java)
    private var cacheFile: File? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun freeContext() {
        scope.cancel()
        super.freeContext()
    }

    override fun getCacheFileName(fileNameOriginal: String): File {
        val hash = (fileNameOriginal + BookCSS.get().isAutoHypens + BookCSS.get().hypenLang).hashCode()
        val file = File(CacheZipUtils.CACHE_BOOK_DIR, "$hash.epub")
        cacheFile = file
        return file
    }

    override fun openDocumentInner(fileName: String, password: String): CodecDocument? {
        LOGGER.info("EpubContext openDocumentInner: {}", fileName)

        val cache = cacheFile ?: getCacheFileName(fileName)

        if (!cache.isFile) {
            EpubBookExtractor.preprocessEpub(fileName, cache.path)
        }

        if (TempHolder.get().loadingCancelled) {
            removeTempFiles()
            return null
        }

        val bookPath = cache.path
        val muPdfDocument = MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, bookPath, password)

        val jsonFile = File(cache.parentFile, ExtUtils.getFileNameWithoutExt(cache.name) + ".json")
        if (jsonFile.isFile) {
            muPdfDocument.setFootNotes(JsonHelper.fileToMap(jsonFile))
            LOGGER.info("Loaded notes from file: {}", jsonFile)
        }

        scope.launch {
            try {
                muPdfDocument.setMediaAttachment(EpubBookExtractor.getAttachments(fileName))
                if (!jsonFile.isFile) {
                    val notesResult = EpubBookExtractor.extractFooterNotes(fileName)
                    val notes = notesResult.getOrNull()
                    if (notes != null) {
                        muPdfDocument.setFootNotes(notes)
                        JsonHelper.mapToFile(jsonFile, notes)
                        LOGGER.info("Saved notes to file: {}", jsonFile)
                    }
                }
                removeTempFiles()
            } catch (e: Exception) {
                LOGGER.error("Error loading Epub media/notes in Coroutine: {}", e.message, e)
            }
        }

        return muPdfDocument
    }
}
