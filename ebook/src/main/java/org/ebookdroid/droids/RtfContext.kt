package org.ebookdroid.droids

import br.com.ebook.extractor.RtfBookExtractor
import br.com.ebook.core.BookContent
import br.com.ebook.foobnix.ext.CacheZipUtils
import br.com.ebook.foobnix.pdf.info.model.BookCSS
import kotlinx.coroutines.runBlocking
import org.ebookdroid.core.codec.CodecDocument
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument
import org.ebookdroid.droids.mupdf.codec.PdfContext
import org.slf4j.LoggerFactory
import java.io.File

class RtfContext : PdfContext() {
    private val LOGGER = LoggerFactory.getLogger(RtfContext::class.java)
    private var cacheFile: File? = null

    override fun getCacheFileName(fileNameOriginal: String): File {
        val hashName = fileNameOriginal + BookCSS.get().isAutoHypens + BookCSS.get().hypenLang
        val hash = hashName.hashCode()
        val file = File(CacheZipUtils.CACHE_BOOK_DIR, "$hash.html")
        cacheFile = file
        return file
    }

    override fun openDocumentInner(fileName: String, password: String): CodecDocument? {
        val cache = cacheFile ?: getCacheFileName(fileName)
        var finalPath = cache.path

        if (!cache.isFile) {
            try {
                val result = runBlocking {
                    RtfBookExtractor.extractContent(fileName, CacheZipUtils.CACHE_BOOK_DIR.path)
                }
                val content = result.getOrThrow()
                if (content is BookContent.HtmlFile) {
                    val generatedFile = File(content.path)
                    if (generatedFile.isFile) {
                        generatedFile.copyTo(cache, overwrite = true)
                        generatedFile.delete()
                    }
                }
            } catch (e: Exception) {
                LOGGER.error("Error open document inner in RtfContext: {}", e.message, e)
                finalPath = fileName
            }
        }

        return MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, finalPath, password)
    }
}
