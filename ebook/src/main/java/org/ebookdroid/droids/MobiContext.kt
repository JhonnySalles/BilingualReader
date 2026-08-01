package org.ebookdroid.droids

import br.com.ebook.core.BookContent
import br.com.ebook.extractor.EpubBookExtractor
import br.com.ebook.extractor.MobiBookExtractor
import br.com.ebook.foobnix.ext.CacheZipUtils
import br.com.ebook.foobnix.pdf.info.ExtUtils
import br.com.ebook.foobnix.pdf.info.JsonHelper
import br.com.ebook.foobnix.pdf.info.model.BookCSS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.ebookdroid.core.codec.CodecDocument
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument
import org.ebookdroid.droids.mupdf.codec.PdfContext
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

class MobiContext : PdfContext() {
    private val LOGGER = LoggerFactory.getLogger(MobiContext::class.java)

    private var fileNameEpub: String? = null
    var originalHashCode: Int = 0
    private var cacheFile: File? = null

    override fun getCacheFileName(fileName: String): File {
        originalHashCode = (fileName + BookCSS.get().isAutoHypens + BookCSS.get().hypenLang).hashCode()
        val file = File(CacheZipUtils.CACHE_BOOK_DIR, "$originalHashCode$originalHashCode.epub")
        cacheFile = file
        return file
    }

    override fun openDocumentInner(fileName: String, password: String): CodecDocument? {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            throw IllegalStateException("Document extraction must not run on the UI thread.")
        }
        LOGGER.info("Context: MobiContext - {}", fileName)

        val epubCache = cacheFile ?: getCacheFileName(fileName)
        val hashCodeStr = fileName.hashCode().toString()
        val htmlCache = File(CacheZipUtils.CACHE_BOOK_DIR, "$hashCodeStr$hashCodeStr.html")

        if (fileName.endsWith(".pdb", ignoreCase = true) && epubCache.isFile) {
            LOGGER.info("Deleting old corrupted EPUB cache for PDB file: {}", epubCache)
            epubCache.delete()
        }

        val cache = if (htmlCache.isFile) htmlCache else epubCache

        try {
            if (cache.isFile) {
                fileNameEpub = cache.path
            } else {
                try {
                    val outName = if (BookCSS.get().isAutoHypens) "temp".hashCode() else originalHashCode
                    
                    val contentResult = runBlocking(br.com.ebook.core.EbookDispatcher.dispatcher) {
                        MobiBookExtractor.extractContent(fileName, CacheZipUtils.CACHE_BOOK_DIR?.path ?: "")
                    }
                    val content = contentResult.getOrThrow()
                    
                    if (content is BookContent.EpubFile) {
                        fileNameEpub = content.path
                        if (BookCSS.get().isAutoHypens) {
                            EpubBookExtractor.processHyphens(fileNameEpub!!, cache.path)
                            fileNameEpub = cache.path
                        }
                    } else if (content is BookContent.HtmlFile) {
                        fileNameEpub = content.path
                    } else {
                        throw IOException("Invalid content returned from MOBI extractor")
                    }
                    
                    LOGGER.info("new file name: {}", fileNameEpub)
                } catch (e: Exception) {
                    LOGGER.error("Error converting MOBI in Context: {}", e.message, e)
                    throw e
                }
            }

            val epubPath = fileNameEpub ?: throw IOException("Epub file path is null")
            val muPdfDocument = MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, epubPath, password)

            val jsonFile = File(cache.parentFile, ExtUtils.getFileNameWithoutExt(cache.name) + ".json")
            if (jsonFile.isFile) {
                muPdfDocument.setFootNotes(JsonHelper.fileToMap(jsonFile))
                LOGGER.info("Load notes from file: {}", jsonFile)
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (epubPath.endsWith(".epub", ignoreCase = true)) {
                            val notesResult = EpubBookExtractor.extractFooterNotes(epubPath)
                            val notes = notesResult.getOrNull()
                            if (notes != null) {
                                muPdfDocument.setFootNotes(notes)
                                JsonHelper.mapToFile(jsonFile, notes)
                                LOGGER.info("Save notes to file: {}", jsonFile)
                            }
                        }
                        removeTempFiles()
                    } catch (e: Exception) {
                        LOGGER.error("Error loading Mobi/Epub notes in Coroutine: {}", e.message, e)
                    }
                }
            }

            return muPdfDocument
        } catch (e: Exception) {
            LOGGER.warn("Error open document inner: {}", e.message, e)
            val c = cacheFile
            if (c != null && c.exists()) {
                c.delete()
            }
            throw e
        }
    }
}
