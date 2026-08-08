package org.ebookdroid.droids

import br.com.ebook.extractor.Fb2BookExtractor
import br.com.ebook.foobnix.ext.CacheZipUtils
import br.com.ebook.foobnix.pdf.info.ExtUtils
import br.com.ebook.foobnix.pdf.info.JsonHelper
import br.com.ebook.foobnix.pdf.info.model.BookCSS
import br.com.ebook.foobnix.pdf.info.wrapper.AppState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ebookdroid.core.codec.CodecDocument
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument
import org.ebookdroid.droids.mupdf.codec.PdfContext
import org.slf4j.LoggerFactory
import java.io.File

class Fb2Context : PdfContext() {
    private val LOGGER = LoggerFactory.getLogger(Fb2Context::class.java)

    private var cacheFile: File? = null
    private var cacheFile1: File? = null

    override fun getCacheFileName(fileNameOriginal: String): File {
        val hashName = fileNameOriginal + BookCSS.get().isAutoHypens + BookCSS.get().hypenLang + AppState.get().isDouble
        val hash = hashName.hashCode()
        cacheFile = File(CacheZipUtils.CACHE_BOOK_DIR, "$hash.epub")
        cacheFile1 = File(CacheZipUtils.CACHE_BOOK_DIR, "$hash.epub.fb2")
        return cacheFile!!
    }

    override fun openDocumentInner(fileName: String, password: String): CodecDocument? {
        val cache = cacheFile ?: getCacheFileName(fileName)
        val cache1 = cacheFile1 ?: getCacheFileName(fileName)

        var outName: String? = null
        if (cache.isFile) {
            outName = cache.path
        } else if (cache1.isFile) {
            outName = cache1.path
        }

        if (outName == null) {
            outName = cache.path
            val file = ExtUtils.validNameFileCharacter(fileName, cache.parent ?: "", "fb2_temp_" + fileName.hashCode())
            if (!Fb2BookExtractor.convert(file, outName)) {
                throw RuntimeException("FB2 not converted")
            }
            LOGGER.info("Fb2Context create: {} to {}", fileName, outName)
        }

        LOGGER.info("Fb2Context open: {}", outName)

        var muPdfDocument: MuPdfDocument
        try {
            muPdfDocument = MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, outName, password)
        } catch (e: Exception) {
            LOGGER.error("Error open document inner: {}", e.message, e)
            val c = cacheFile
            if (c != null && c.isFile) {
                c.delete()
            }

            outName = cache1.path
            val file = ExtUtils.validNameFileCharacter(fileName, cache1.parent ?: "", "fb2_temp_" + fileName.hashCode())

            if (!Fb2BookExtractor.convertFB2(file, outName)) {
                throw RuntimeException("FB2 not converted")
            }

            muPdfDocument = MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, outName, password)
            LOGGER.info("Fb2Context create second attempt: {}", outName)
        }

        val jsonFile = File(cache.parentFile, ExtUtils.getFileNameWithoutExt(cache.name) + ".json")
        if (jsonFile.isFile) {
            muPdfDocument.setFootNotes(JsonHelper.fileToMap(jsonFile))
            LOGGER.info("Load notes from file: {}", jsonFile)
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val notesResult = Fb2BookExtractor.extractFooterNotes(fileName)
                    val notes = notesResult.getOrNull()
                    if (notes != null) {
                        muPdfDocument.setFootNotes(notes)
                        JsonHelper.mapToFile(jsonFile, notes)
                        LOGGER.info("Save notes to file: {}", jsonFile)
                    }
                    removeTempFiles()
                } catch (e: Exception) {
                    LOGGER.error("Error parsing Fb2 notes in Coroutine: {}", e.message, e)
                }
            }
        }

        return muPdfDocument
    }
}
