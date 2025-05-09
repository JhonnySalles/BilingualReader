package br.com.fenix.bilingualreader.model.enums

import java.io.File


const val TYPE_UNKNOWN = -1
const val TYPE_MANGA = 0
const val TYPE_BOOK = 1
const val TYPE_MANGA_AND_BOOK = 2

enum class FileType(var type: Int, var extension: Array<String>, var mimeType: Array<String>, var acronym : String) {
    UNKNOWN(TYPE_UNKNOWN, arrayOf(), arrayOf(), ""),

    // Manga and Book file
    EPUB(TYPE_MANGA_AND_BOOK, arrayOf("epub"), arrayOf("application/epub+zip"), "EPUB"),
    EPUB3(TYPE_MANGA_AND_BOOK, arrayOf("epub3"), arrayOf("application/epub3+zip"), "EPUB3"),

    // Book file
    PDF(TYPE_BOOK, arrayOf("pdf", "xps"), arrayOf("application/pdf", "application/oxps", "application/vnd.ms-xpsdocument"), "PDF"),
    MOBI(
        TYPE_BOOK,
        arrayOf("mobi", "azw", "azw3", "azw4", "pdb", "prc"),
        arrayOf(
            "application/x-mobipocket-ebook",
            "application/x-palm-database",
            "application/x-mobi8-ebook",
            "application/x-kindle-application",
            "application/vnd.amazon.mobi8-ebook"
        ), "MOBI"
    ),
    DJVU(TYPE_BOOK, arrayOf("djvu"), arrayOf("image/vnd.djvu", "image/djvu", "image/x-djvu"), "DJVU"),
    FB2(
        TYPE_BOOK,
        arrayOf("fb2"),
        arrayOf(
            "application/fb2",
            "application/x-fictionbook",
            "application/x-fictionbook+xml",
            "application/x-fb2",
            "application/fb2+zip",
            "application/fb2.zip",
            "application/x-zip-compressed-fb2"
        ), "FB2"
    ),
    TXT(TYPE_BOOK, arrayOf("txt", "playlist", "log"), arrayOf("text/plain", "text/x-log"), "TXT"),
    RTF(TYPE_BOOK, arrayOf("rtf"), arrayOf("application/rtf", "application/x-rtf", "text/rtf", "text/richtext"), "RTF"),
    AZW(TYPE_BOOK, arrayOf("azw"), arrayOf("application/azw", "application/x-azw"), "AZW"),
    AZW3(TYPE_BOOK, arrayOf("azw3"), arrayOf("application/azw3", "application/x-azw3"), "AZW3"),
    HTML(TYPE_BOOK, arrayOf("html", "htm", "xhtml", "xhtm", "xml"), arrayOf("text/html", "text/xml"), "HTML"),
    //DOC(1, arrayOf("doc"), arrayOf("application/msword")),
    //DOCX(1, arrayOf("docx"), arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
    OPDS(TYPE_BOOK, arrayOf("opds"), arrayOf("application/opds", "application/x-opds"), "OPDS"),
    TIFF(TYPE_BOOK, arrayOf("tiff", "tif"), arrayOf("image/tiff"), "TIFF"),
    //ODT(1, arrayOf("odt"), arrayOf("application/vnd.oasis.opendocument.text")),
    MD(TYPE_BOOK, arrayOf("md"), arrayOf("text/markdown", "text/x-markdown"), "MD"),
    MHT(TYPE_BOOK, arrayOf("mht", "mhtml", "shtml"), arrayOf("message/rfc822"), "MHT"),

    // Comic file
    CBZ(TYPE_MANGA, arrayOf("cbz"), arrayOf("application/cbz", "application/x-cbz", "application/comicbook+zip"), "CBZ"),
    CBR(TYPE_MANGA, arrayOf("cbr"), arrayOf("application/cbr", "application/x-cbr", "application/comicbook+rar"), "CBR"),
    CB7(TYPE_MANGA, arrayOf("cb7"), arrayOf("application/cb7", "application/x-cb7", "application/comicbook+7z"), "CB7"),
    CBT(TYPE_MANGA, arrayOf("cbt"), arrayOf("application/cbt", "application/x-cbt", "application/comicbook+tar"), "CBT"),
    ZIP(TYPE_MANGA, arrayOf("zip"), arrayOf("application/zip", "application/x-compressed", "application/x-compressed-zip", "application/x-zip-compressed"), "ZIP"),
    RAR(TYPE_MANGA, arrayOf("rar"), arrayOf("application/rar", "application/x-rar", "application/comicbook+rar"), "RAR"),
    SEVENZ(TYPE_MANGA, arrayOf("7z"), arrayOf("application/7z", "application/x-7z", "application/comicbook+7z"), "7Z"),
    TAR(TYPE_MANGA, arrayOf("tar"), arrayOf("application/tar", "application/x-tar", "application/comicbook+tar"), "TAR("),
    DIRECTORY(TYPE_MANGA, arrayOf(), arrayOf(), "DIR");

    private var extensions: Array<String> = extension
    private var mimeTypes: Array<String> = mimeType

    fun `is`(path: String): Boolean {
        val name = path.lowercase()
        for (ext in extensions) {
            if (name.endsWith(ext) || name.endsWith(".$ext.zip"))
                return true
        }
        return false
    }

    fun compareExtension(path: String): Boolean {
        val name = path.lowercase()
        for (ext in extensions) {
            if (name.endsWith(ext))
                return true
        }
        return false
    }

    fun getMimeType(): String = mimeTypes.joinToString { "|" }

    fun getMime(): Array<String> = mimeTypes

    fun getExtensions(): Array<String> = extension

    companion object {
        fun isManga(name: String): Boolean {
            for (item in getManga())
                if (item.`is`(name))
                    return true
            return false
        }

        fun isBook(name: String): Boolean {
            for (item in getBook())
                if (item.`is`(name))
                    return true
            return false
        }

        fun getManga() = values().filter { it.type == TYPE_MANGA || it.type == TYPE_MANGA_AND_BOOK }

        fun getBook() = values().filter { it.type == TYPE_BOOK || it.type == TYPE_MANGA_AND_BOOK }

        fun getMimeManga(): ArrayList<String> {
            val array = arrayListOf<String>()
            for (item in getManga())
                array.addAll(item.mimeType)
            return array
        }

        fun getMimeBook(): ArrayList<String> {
            val array = arrayListOf<String>()
            for (item in getBook())
                array.addAll(item.mimeType)
            return array
        }

        fun getMimeTypeManga() = getMimeManga().joinToString { "|" }

        fun getMimeTypeBook() = getMimeBook().joinToString { "|" }

        fun getType(file: File): FileType {
            for (item in values())
                if (item.`is`(file.name))
                    return item

            return UNKNOWN
        }

        fun getType(name: String): FileType {
            for (item in values())
                if (item.`is`(name))
                    return item

            return UNKNOWN
        }
    }
}