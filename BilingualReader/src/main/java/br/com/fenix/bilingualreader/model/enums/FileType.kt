package br.com.fenix.bilingualreader.model.enums

import java.io.File

enum class FileType(var type: Int, var extension: Array<String>, var mimeType: Array<String>, var acronym : String) {
    UNKNOWN(-1, arrayOf(), arrayOf(), ""),

    // Book file
    PDF(1, arrayOf("pdf", "xps"), arrayOf("application/pdf", "application/oxps", "application/vnd.ms-xpsdocument"), "PDF"),
    EPUB(1, arrayOf("epub"), arrayOf("application/epub+zip"), "EPUB"),
    EPUB3(1, arrayOf("epub3"), arrayOf("application/epub3+zip"), "EPUB3"),
    MOBI(
        1,
        arrayOf("mobi", "azw", "azw3", "azw4", "pdb", "prc"),
        arrayOf(
            "application/x-mobipocket-ebook",
            "application/x-palm-database",
            "application/x-mobi8-ebook",
            "application/x-kindle-application",
            "application/vnd.amazon.mobi8-ebook"
        ), "MOBI"
    ),
    DJVU(1, arrayOf("djvu"), arrayOf("image/vnd.djvu", "image/djvu", "image/x-djvu"), "DJVU"),
    FB2(
        1,
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
    TXT(1, arrayOf("txt", "playlist", "log"), arrayOf("text/plain", "text/x-log"), "TXT"),
    RTF(1, arrayOf("rtf"), arrayOf("application/rtf", "application/x-rtf", "text/rtf", "text/richtext"), "RTF"),
    AZW(1, arrayOf("azw"), arrayOf("application/azw", "application/x-azw"), "AZW"),
    AZW3(1, arrayOf("azw3"), arrayOf("application/azw3", "application/x-azw3"), "AZW3"),
    HTML(1, arrayOf("html", "htm", "xhtml", "xhtm", "xml"), arrayOf("text/html", "text/xml"), "HTML"),
    //DOC(1, arrayOf("doc"), arrayOf("application/msword")),
    //DOCX(1, arrayOf("docx"), arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
    OPDS(1, arrayOf("opds"), arrayOf("application/opds", "application/x-opds"), "OPDS"),
    TIFF(1, arrayOf("tiff", "tif"), arrayOf("image/tiff"), "TIFF"),
    //ODT(1, arrayOf("odt"), arrayOf("application/vnd.oasis.opendocument.text")),
    MD(1, arrayOf("md"), arrayOf("text/markdown", "text/x-markdown"), "MD"),
    MHT(1, arrayOf("mht", "mhtml", "shtml"), arrayOf("message/rfc822"), "MHT"),

    // Comic file
    CBZ(0, arrayOf("cbz"), arrayOf("application/cbz", "application/x-cbz", "application/comicbook+zip"), "CBZ("),
    CBR(0, arrayOf("cbr"), arrayOf("application/cbr", "application/x-cbr", "application/comicbook+rar"), "CBR"),
    CB7(0, arrayOf("cb7"), arrayOf("application/cb7", "application/x-cb7", "application/comicbook+7z"), "CB7"),
    CBT(0, arrayOf("cbt"), arrayOf("application/cbt", "application/x-cbt", "application/comicbook+tar"), "CBT"),

    ZIP(
        0,
        arrayOf("zip"),
        arrayOf("application/zip", "application/x-compressed", "application/x-compressed-zip", "application/x-zip-compressed"),
        "ZIP"
    ),
    RAR(0, arrayOf("rar"), arrayOf("application/rar", "application/x-rar", "application/comicbook+rar"), "ZIP"),
    SEVENZ(0, arrayOf("7z"), arrayOf("application/7z", "application/x-7z", "application/comicbook+7z"), "7Z"),
    TAR(0, arrayOf("tar"), arrayOf("application/tar", "application/x-tar", "application/comicbook+tar"), "TAR("),
    DIRECTORY(0, arrayOf(), arrayOf(), "DIR");

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

        fun getManga() = values().filter { it.type == 0 }

        fun getBook() = values().filter { it.type == 1 }

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