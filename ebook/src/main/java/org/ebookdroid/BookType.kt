package org.ebookdroid

import android.content.Intent
import org.ebookdroid.core.codec.CodecContext
import org.ebookdroid.droids.*
import org.ebookdroid.droids.djvu.codec.DjvuContext
import org.ebookdroid.droids.mupdf.codec.PdfContext
import org.slf4j.LoggerFactory
import java.util.Locale

enum class BookType(
    @JvmField val contextClass: Class<out CodecContext>,
    @JvmField val extensions: List<String>,
    @JvmField val mimeTypes: List<String>
) {
    PDF(PdfContext::class.java, listOf("pdf", "xps"), listOf("application/pdf")),
    TIFF(PdfContext::class.java, listOf("tiff", "tif"), listOf("image/tiff")),
    CBZ(PdfContext::class.java, listOf("cbz"), listOf("application/x-cbz")),
    EPUB(EpubContext::class.java, listOf("epub"), listOf("application/epub+zip")),
    FB2(Fb2Context::class.java, listOf("fb2"), listOf(
        "application/fb2", "application/x-fictionbook", "application/x-fictionbook+xml", 
        "application/x-fb2", "application/fb2+zip", "application/fb2.zip", "application/x-zip-compressed-fb2"
    )),
    MOBI(MobiContext::class.java, listOf("mobi", "azw", "azw3", "azw4", "pdb", "prc"), listOf("application/x-mobipocket-ebook", "application/x-palm-database")),
    TXT(TxtContext::class.java, listOf("txt"), listOf("text/plain")),
    HTML(HtmlContext::class.java, listOf("html", "htm", "xhtml", "xhtm", "mht", "mhtml"), listOf("text/html", "text/xml")),
    RTF(RtfContext::class.java, listOf("rtf"), listOf("application/rtf", "application/x-rtf", "text/rtf", "text/richtext")),
    DJVU(DjvuContext::class.java, listOf("djvu"), listOf("image/vnd.djvu", "image/djvu", "image/x-djvu")),
    ZIP(ZipContext::class.java, listOf("zip"), listOf("application/zip", "application/x-compressed", "application/x-compressed-zip", "application/x-zip-compressed")),
    DOCX(DocxContext::class.java, listOf("docx"), listOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
    ODT(OdtContext::class.java, listOf("odt"), listOf("application/vnd.oasis.opendocument.text")),
    MD(MarkdownContext::class.java, listOf("md", "markdown"), listOf("text/markdown", "text/x-markdown"));

    fun `is`(path: String?): Boolean {
        if (path == null) return false
        val pathLow = path.lowercase(Locale.getDefault())
        for (ext in extensions) {
            if (pathLow.endsWith(ext) || pathLow.endsWith("$ext.zip")) {
                return true
            }
        }
        return false
    }

    fun getExt(): String {
        return extensions[0]
    }

    fun `is`(intent: Intent): Boolean {
        return try {
            `is`(intent.data?.path)
        } catch (e: Exception) {
            false
        }
    }

    val firstMimeType: String
        get() = mimeTypes[0]

    companion object {
        private val LOGGER = LoggerFactory.getLogger(BookType::class.java)

        private val extensionToActivity = HashMap<String, BookType>()
        private val mimeTypesToActivity = HashMap<String, BookType>()

        init {
            for (a in values()) {
                for (ext in a.extensions) {
                    extensionToActivity[ext.lowercase(Locale.getDefault())] = a
                }
                for (type in a.mimeTypes) {
                    mimeTypesToActivity[type.lowercase(Locale.getDefault())] = a
                }
            }
        }

        @JvmStatic
        fun getCodecContextByType(activityType: BookType): CodecContext? {
            return try {
                activityType.contextClass.getDeclaredConstructor().newInstance()
            } catch (e: Exception) {
                LOGGER.error("Error get codec context by type: {}", e.message, e)
                null
            }
        }

        @JvmStatic
        fun getCodecContextByPath(path: String): CodecContext? {
            return try {
                val bookType = getByUri(path) ?: return null
                bookType.contextClass.getDeclaredConstructor().newInstance()
            } catch (e: Exception) {
                LOGGER.error("Error get codec context by path: {}", e.message, e)
                null
            }
        }

        @JvmStatic
        fun getAllSupportedExtensions(): List<String> {
            val list = ArrayList<String>()
            for (a in values()) {
                list.addAll(a.extensions)
            }
            return list
        }

        @JvmStatic
        fun isSupportedExtByPath(path: String?): Boolean {
            if (path == null) return false
            val pathLow = path.lowercase(Locale.getDefault())
            for (a in values()) {
                for (ext in a.extensions) {
                    if (pathLow.endsWith(ext)) {
                        return true
                    }
                }
            }
            return false
        }

        @JvmStatic
        fun getByUri(uri: String?): BookType? {
            if (uri == null) return null
            val uriLow = uri.lowercase(Locale.getDefault())
            for (ext in extensionToActivity.keys) {
                if (uriLow.endsWith(".$ext")) {
                    return extensionToActivity[ext]
                }
            }
            return null
        }

        @JvmStatic
        fun getByMimeType(type: String?): BookType? {
            if (type == null) return null
            return mimeTypesToActivity[type.lowercase(Locale.getDefault())]
        }
    }
}
