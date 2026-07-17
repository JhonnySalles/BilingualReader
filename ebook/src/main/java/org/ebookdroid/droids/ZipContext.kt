package org.ebookdroid.droids

import androidx.core.util.Pair
import org.ebookdroid.BookType
import org.ebookdroid.core.codec.CodecDocument
import org.ebookdroid.droids.mupdf.codec.PdfContext
import org.slf4j.LoggerFactory
import java.io.File
import br.com.ebook.foobnix.ext.CacheZipUtils
import br.com.ebook.foobnix.ext.CacheZipUtils.CacheDir

class ZipContext : PdfContext() {
    private val LOGGER = LoggerFactory.getLogger(ZipContext::class.java)

    override fun openDocumentInner(fileName: String, password: String): CodecDocument? {
        val pack: Pair<Boolean, String> = CacheZipUtils.isSingleAndSupportEntryFile(File(fileName))
        if (pack.first == true) {
            val fb2Context = Fb2Context()
            val etryPath = pack.second
            val cacheFileName = fb2Context.getCacheFileName(File(CacheDir.ZipApp.getDir(), etryPath).path)
            if (cacheFileName.exists()) {
                return fb2Context.openDocumentInner(etryPath, password)
            }
        }

        val path = CacheZipUtils.extracIfNeed(fileName, CacheDir.ZipApp).unZipPath ?: return null
        if (path.endsWith("zip")) {
            return null
        }

        val ctx = BookType.getCodecContextByPath(path) ?: return null
        return ctx.openDocument(path, password)
    }
}
