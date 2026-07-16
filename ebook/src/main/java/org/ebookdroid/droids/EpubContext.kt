package org.ebookdroid.droids

import br.com.ebook.extractor.EpubBookExtractor
import br.com.ebook.foobnix.ext.CacheZipUtils
import br.com.ebook.pdf.info.ExtUtils
import br.com.ebook.pdf.info.JsonHelper
import br.com.ebook.pdf.info.model.BookCSS
import br.com.ebook.foobnix.sys.TempHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ebookdroid.core.codec.CodecDocument
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument
import org.ebookdroid.droids.mupdf.codec.PdfContext
import org.slf4j.LoggerFactory
import java.io.File

class EpubContext : PdfContext() {
    private val LOGGER = LoggerFactory.getLogger(EpubContext::class.java)
    private var cacheFile: File? = null

    override fun getCacheFileName(fileNameOriginal: String): File {
        val hash = (fileNameOriginal + BookCSS.get().isAutoHypens + BookCSS.get().hypenLang).hashCode()
        val file = File(CacheZipUtils.CACHE_BOOK_DIR, "$hash.epub")
        cacheFile = file
        return file
    }

    override fun openDocumentInner(fileName: String, password: String): CodecDocument? {
        LOGGER.info("EpubContext openDocumentInner: {}", fileName)

        val cache = cacheFile ?: getCacheFileName(fileName)

        if (BookCSS.get().isAutoHypens && !cache.isFile) {
            EpubBookExtractor.processHyphens(fileName, cache.path)
        }

        if (TempHolder.get().loadingCancelled) {
            removeTempFiles()
            return null
        }

        val bookPath = if (BookCSS.get().isAutoHypens) cache.path else fileName
        val muPdfDocument = MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, bookPath, password)

        val jsonFile = File(cache.parentFile, ExtUtils.getFileNameWithoutExt(cache.name) + ".json")
        if (jsonFile.isFile) {
            muPdfDocument.setFootNotes(JsonHelper.fileToMap(jsonFile))
            LOGGER.info("Loaded notes from file: {}", jsonFile)
        }

        CoroutineScope(Dispatchers.IO).launch {
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
