package br.com.ebook.extractor

import android.util.Base64
import br.com.ebook.core.*
import br.com.ebook.util.IOUtils
import br.com.ebook.util.IOUtils.copyTo
import br.com.ebook.foobnix.android.utils.StreamUtils
import br.com.ebook.foobnix.android.utils.TxtUtils
import br.com.ebook.foobnix.hypen.HypenUtils
import br.com.ebook.foobnix.pdf.info.ExtUtils
import br.com.ebook.foobnix.pdf.info.model.BookCSS
import br.com.ebook.foobnix.pdf.info.model.OutlineLinkWrapper
import br.com.ebook.foobnix.pdf.info.wrapper.AppState
import br.com.ebook.foobnix.sys.TempHolder
import br.com.ebook.foobnix.ext.XmlParser
import org.ebookdroid.core.codec.OutlineLink
import org.slf4j.LoggerFactory
import org.xmlpull.v1.XmlPullParser
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object Fb2BookExtractor : BookExtractor {
    private val LOGGER = LoggerFactory.getLogger(Fb2BookExtractor::class.java)

    const val FOOTER_NOTES_SIGN = "***"
    const val FOOTER_AFTRER_BOODY = "[!]"
    const val DIVIDER = "~@~"

    override val supportedFormats: Set<String> = setOf("fb2")

    override suspend fun extractOverview(path: String): Result<String> = runCatching {
        val info = StringBuilder()
        val encoding = findHeaderEncoding(path)
        
        FileInputStream(path).use { fis ->
            val xpp = XmlParser.buildPullParser()
            xpp.setInput(fis, encoding)
            var eventType = xpp.eventType
            var findAnnotation = false
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.name == "annotation") {
                        findAnnotation = true
                    }
                    if (xpp.name == "body") {
                        break
                    }
                }
                if (eventType == XmlPullParser.TEXT && findAnnotation) {
                    info.append(" ").append(xpp.text)
                }
                if (eventType == XmlPullParser.END_TAG && xpp.name == "annotation") {
                    break
                }
                eventType = xpp.next()
            }
        }
        info.toString().trim()
    }

    override suspend fun extractMetadata(path: String): Result<BookMetadata> = runCatching {
        val encoding = findHeaderEncoding(path)
        var bookTitle = ""
        var firstName = ""
        var lastName = ""
        var genre = ""
        var sequence = ""
        var lang = ""
        var number = ""
        var isbn = ""
        var publisher = ""
        var releaseDate: LocalDate? = null
        var titleInfo = false

        FileInputStream(path).use { fis ->
            val xpp = XmlParser.buildPullParser()
            xpp.setInput(fis, encoding)
            var eventType = xpp.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.name == "title-info") {
                        titleInfo = true
                    }
                    if (titleInfo) {
                        when (xpp.name) {
                            "book-title" -> bookTitle = xpp.nextText() ?: ""
                            "lang" -> lang = xpp.nextText() ?: ""
                            "first-name" -> if (firstName.isEmpty()) firstName = xpp.nextText() ?: ""
                            "last-name" -> if (lastName.isEmpty()) lastName = xpp.nextText() ?: ""
                            "genre" -> {
                                val gen = xpp.nextText() ?: ""
                                genre = if (genre.isEmpty()) gen else "$genre,$gen"
                            }
                            "sequence" -> {
                                sequence = xpp.getAttributeValue(null, "name") ?: ""
                                val current = xpp.getAttributeValue(null, "number") ?: ""
                                if (current.isNotEmpty() && current != "0" && current != "00") {
                                    number = current
                                }
                            }
                            "isbn" -> isbn = xpp.nextText() ?: ""
                            "publisher" -> publisher = xpp.nextText() ?: ""
                            "date" -> {
                                val dateStr = xpp.nextText()
                                releaseDate = DateParseUtils.parseFlexibleDate(dateStr)
                            }
                        }
                    }
                    if (xpp.name == "body") {
                        break
                    }
                }
                if (eventType == XmlPullParser.END_TAG && xpp.name == "title-info") {
                    titleInfo = false
                }
                eventType = xpp.next()
            }
        }

        lastName = lastName.trim()
        firstName = firstName.trim()

        var author = if (AppState.get().isFirstSurname) {
            if (firstName.isNotEmpty() && lastName.isNotEmpty()) "$lastName $firstName" else lastName + firstName
        } else {
            if (firstName.isNotEmpty() && lastName.isNotEmpty()) "$firstName $lastName" else firstName + lastName
        }

        val sIndex = try {
            if (number.isNotEmpty()) number.replace(".0", "").toInt() else 0
        } catch (e: Exception) {
            0
        }

        BookMetadata(
            title = bookTitle,
            author = author,
            series = sequence,
            genre = genre.replace(Regex(",$"), ""),
            isbn = isbn,
            publisher = publisher,
            releaseDate = releaseDate,
            seriesIndex = sIndex,
            language = lang,
            unzipPath = path
        )
    }

    override suspend fun extractCover(path: String): Result<ByteArray?> = runCatching {
        var decode: ByteArray? = null
        val encoding = findHeaderEncoding(path)
        FileInputStream(path).use { fis ->
            val xpp = XmlParser.buildPullParser()
            xpp.setInput(fis, encoding)
            var eventType = xpp.eventType
            var imageID: String? = null
            var imageCover: String? = null
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.name == "image") {
                        if (imageID == null) {
                            imageID = xpp.getAttributeValue(0)
                            if (!imageID.isNullOrEmpty()) imageID = imageID.replace("#", "")
                        }
                        if (imageCover == null) {
                            val candidate = xpp.getAttributeValue(0)
                            if (!candidate.isNullOrEmpty() && candidate.lowercase(Locale.getDefault()).contains("cover")) {
                                imageCover = candidate.replace("#", "")
                                imageID = imageCover
                            }
                        }
                    }
                    if (imageID != null && xpp.name == "binary" && imageID == xpp.getAttributeValue(null, "id")) {
                        val text = xpp.nextText()
                        decode = Base64.decode(text, Base64.DEFAULT)
                        break
                    }
                }
                eventType = xpp.next()
            }
        }
        decode
    }

    fun getBookCover(inputStream: InputStream): ByteArray? {
        var decode: ByteArray? = null
        try {
            val xpp = XmlParser.buildPullParser()
            xpp.setInput(inputStream, "UTF-8")
            var eventType = xpp.eventType
            var imageID: String? = null
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (imageID == null && xpp.name == "image") {
                        imageID = xpp.getAttributeValue(0)
                        if (!imageID.isNullOrEmpty()) imageID = imageID.replace("#", "")
                    }
                    if (imageID != null && xpp.name == "binary" && imageID == xpp.getAttributeValue(null, "id")) {
                        decode = Base64.decode(xpp.nextText(), Base64.DEFAULT)
                        break
                    }
                }
                eventType = xpp.next()
            }
        } catch (e: Exception) {
            LOGGER.error("Error get book cover: {}", e.message, e)
        }
        return decode
    }

    override suspend fun extractFooterNotes(path: String): Result<Map<String, String>> = runCatching {
        val map = mutableMapOf<String, String>()
        val encoding = findHeaderEncoding(path)
        FileInputStream(path).use { fis ->
            val xpp = XmlParser.buildPullParser()
            xpp.setInput(fis, encoding)
            var eventType = xpp.eventType
            var sectionId: String? = null
            var text: StringBuilder? = null
            var isLink = false
            var link: String? = null
            val key = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (TempHolder.get().loadingCancelled) break
                if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.name == "a") {
                        val type = xpp.getAttributeValue(null, "type")
                        if (type == "note") {
                            isLink = true
                            link = xpp.getAttributeValue(null, "l:href")
                                ?: xpp.getAttributeValue(null, "xlink:href")
                        }
                    } else if (xpp.name == "section") {
                        sectionId = xpp.getAttributeValue(null, "id")
                        text = StringBuilder()
                    }
                } else if (eventType == XmlPullParser.TEXT) {
                    if (sectionId != null) {
                        val trim = xpp.text.trim()
                        if (trim.isNotEmpty()) {
                            text?.append(trim)?.append(" ")
                        }
                    }
                    if (isLink) {
                        key.append(" ").append(xpp.text)
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (sectionId != null && xpp.name == "section") {
                        val keyEnd = StreamUtils.getKeyByValue(map, sectionId) ?: ""
                        map[keyEnd] = text.toString().trim()
                        sectionId = null
                        text = null
                    } else if (xpp.name == "a") {
                        if (isLink && link != null) {
                            var cleanKey = key.toString().trim()
                            if (!TxtUtils.isFooterNote(cleanKey)) {
                                cleanKey = "[$link]"
                            }
                            val cleanLink = link.replace("#", "")
                            map[cleanKey] = cleanLink
                            isLink = false
                            key.setLength(0)
                        }
                    }
                }
                eventType = xpp.next()
            }
        }
        map
    }

    fun convertFB2(inputFile: String, toName: String): Boolean {
        try {
            val encoding = findHeaderEncoding(inputFile)
            generateFb2File(inputFile, encoding, true).use { generateFb2File ->
                FileOutputStream(toName).use { out ->
                    out.write(generateFb2File.toByteArray())
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error convert FB2: {}", e.message, e)
            return false
        }
        return true
    }

    fun convert(inputFile: String, toName: String): Boolean {
        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(File(toName)))).use { zos ->
                zos.setLevel(0)
                writeToZip(zos, "mimetype", "application/epub+zip")
                writeToZip(zos, "META-INF/container.xml", container_xml)
                writeToZip(zos, "OEBPS/content.opf", content_opf)

                val encoding = findHeaderEncoding(inputFile)
                val titles = getFb2Titles(inputFile, encoding)
                val ncx = generateNCX(titles)
                writeToZip(zos, "OEBPS/fb2.ncx", ncx)

                generateFb2File(inputFile, encoding, false).use { generateFb2File ->
                    writeToZip(zos, "OEBPS/fb2.fb2", ByteArrayInputStream(generateFb2File.toByteArray()))
                }
                return true
            }
        } catch (e: Exception) {
            LOGGER.error("Error convert fb2: {}", e.message, e)
        }
        return false
    }

    fun convertFolderToEpub(inputFolder: File, outputFile: File, author: String, title: String, outline: List<OutlineLink>): Boolean {
        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zos ->
                zos.setLevel(0)
                writeToZip(zos, "mimetype", "application/epub+zip")
                writeToZip(zos, "META-INF/container.xml", container_xml)

                var meta = content_opf.replace("fb2.fb2", "temp" + ExtUtils.REFLOW_HTML)
                meta = meta.replace("%title%", title)
                meta = meta.replace("%creator%", author)

                writeToZip(zos, "OEBPS/content.opf", meta)
                if (!outline.isNullOrEmpty()) {
                    writeToZip(zos, "OEBPS/fb2.ncx", generateNCXbyOutline(outline))
                }

                inputFolder.listFiles()?.forEach { file ->
                    FileInputStream(file).use { fis ->
                        writeToZip(zos, "OEBPS/${file.name}", fis)
                    }
                }
                return true
            }
        } catch (e: Exception) {
            LOGGER.error("Error convert folder to epub: {}", e.message, e)
        }
        return false
    }

    fun generateFb2File(fb2: String, encoding: String, fixXML: Boolean): ByteArrayOutputStream {
        val out = ByteArrayOutputStream()
        val writer = PrintWriter(out)
        
        if (BookCSS.get().isAutoHypens) {
            HypenUtils.applyLanguage(BookCSS.get().hypenLang)
        }

        var count = 0
        var isEncoding = false
        var isFindBodyEnd = false
        var titleBegin = false

        BufferedReader(InputStreamReader(FileInputStream(fb2), encoding)).use { input ->
            var line: String?
            while (input.readLine().also { line = it } != null) {
                if (TempHolder.get().loadingCancelled) break
                var curLine = line!!

                if (!isEncoding && curLine.contains("windows-1251", ignoreCase = true)) {
                    curLine = curLine.replace("windows-1251", "utf-8", ignoreCase = true)
                    isEncoding = true
                } else if (!isEncoding && curLine.contains("windows-1252", ignoreCase = true)) {
                    curLine = curLine.replace("windows-1252", "utf-8", ignoreCase = true)
                    isEncoding = true
                }

                if (fixXML) {
                    curLine = curLine.replace("l:href==", "l:href=")
                }

                val subLine = curLine.split("</")
                for (i in subLine.indices) {
                    var sub = if (i == 0) subLine[i] else "</" + subLine[i]

                    if (BookCSS.get().isAutoHypens && sub.contains("<title")) {
                        titleBegin = true
                    }

                    if (sub.contains("</title>")) {
                        titleBegin = false
                        count++
                        sub = sub.replace("</title>", "<a id=\"$count\"></a></title>")
                    }

                    if (!isFindBodyEnd && sub.contains("</body>")) {
                        isFindBodyEnd = true
                    }

                    if (!isFindBodyEnd && (AppState.get().isDouble || !titleBegin) && BookCSS.get().isAutoHypens) {
                        sub = HypenUtils.applyHypnesOld(sub)
                    }
                    writer.println(sub)
                }
            }
        }
        writer.close()
        return out
    }

    fun generateNCX(titles: List<String>): String {
        val navs = StringBuilder()
        for (i in titles.indices) {
            navs.append(createNavPoint(i + 1, titles[i]))
        }
        return NCX.replace("%nav%", navs.toString())
    }

    fun generateNCXbyOutline(titles: List<OutlineLink>): String {
        val navs = StringBuilder()
        for (i in titles.indices) {
            val link = titles[i]
            val titleTxt = link.title
            if (!titleTxt.isNullOrEmpty()) {
                var createNav = createNavPoint(OutlineLinkWrapper.getPageNumber(link.link), "${link.level}$DIVIDER$titleTxt")
                createNav = createNav.replace("fb2.fb2", "temp-reflow.html")
                navs.append(createNav)
            }
        }
        return NCX.replace("%nav%", navs.toString())
    }

    fun getFb2Titles(fb2: String, encoding: String): List<String> {
        val titles = mutableListOf<String>()
        var section = 0
        val dividerSection = -1
        val dividerLine: String? = null

        FileInputStream(fb2).use { fis ->
            val xpp = XmlParser.buildPullParser()
            xpp.setInput(fis, encoding)
            var eventType = xpp.eventType
            var isTitle = false
            val title = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (TempHolder.get().loadingCancelled) break
                if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.name == "section") section++
                    if (xpp.name == "title") isTitle = true
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (xpp.name == "title") {
                        isTitle = false
                        val cleanTitle = "[${xpp.name}]${title.toString().trim()}"
                        titles.add("$section$DIVIDER$cleanTitle")
                        title.setLength(0)
                        if (section == dividerSection && dividerLine != null) {
                            titles.remove(dividerLine)
                        }
                    }
                    if (xpp.name == "section") section--
                    if (xpp.name == "body") break
                } else if (eventType == XmlPullParser.TEXT) {
                    if (isTitle) {
                        title.append(" ").append(xpp.text.trim())
                    }
                }
                eventType = xpp.next()
            }
        }

        if (titles.isNotEmpty() && titles.last().endsWith(DIVIDER)) {
            titles.removeAt(titles.size - 1)
        }
        if (titles.isNotEmpty() && titles.last().endsWith(FOOTER_NOTES_SIGN)) {
            titles.removeAt(titles.size - 1)
        }
        return titles
    }

    fun findHeaderEncoding(fb2: String): String {
        var encoding = "UTF-8"
        try {
            FileInputStream(fb2).use { fis ->
                val header = ByteArray(80)
                fis.read(header)
                val headerStr = String(header).lowercase(Locale.getDefault())
                if (headerStr.contains("windows-1251")) {
                    encoding = "cp1251"
                } else if (headerStr.contains("windows-1252")) {
                    encoding = "cp1252"
                }
            }
        } catch (e: Exception) {
            // Silently fallback to UTF-8
        }
        return encoding
    }

    const val container_xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n" +
            "  <rootfiles>\n" +
            "    <rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>\n" +
            "  </rootfiles>\n" +
            "</container>"

    const val content_opf = "<?xml version=\"1.0\"?>\n" +
            "<package version=\"2.0\" unique-identifier=\"uid\" xmlns=\"http://www.idpf.org/2007/opf\">\n" +
            " <metadata xmlns:opf=\"http://www.idpf.org/2007/opf\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n" +
            "  <dc:title>%title%</dc:title>\n" +
            "  <dc:creator>%creator%</dc:creator>\n" +
            "<meta name=\"cover\" content=\"cover.jpg\" />\n" +
            " </metadata>\n" +
            "\n<manifest>\n" +
            "  <item id=\"idBookFb2\" href=\"fb2.fb2\" media-type=\"application/xhtml+xml\"/>\n" +
            "  <item id=\"idResourceFb2\" href=\"fb2.ncx\" media-type=\"application/x-dtbncx+xml\"/>\n" +
            " </manifest>\n" +
            " \n<spine toc=\"idResourceFb2\">\n" +
            "  <itemref idref=\"idBookFb2\"/>\n" +
            "</spine>\n" +
            "</package>"

    const val NCX = "<?xml version=\"1.0\"?>\n" +
            "<ncx version=\"2005-1\" xml:lang=\"en\" xmlns=\"http://www.daisy.org/z3986/2005/ncx/\">\n" +
            " <head>\n" +
            " </head>\n" +
            " <docTitle>\n" +
            "  <text>title</text>\n" +
            " </docTitle>\n" +
            " <navMap>\n  \n%nav% \n   \n </navMap>\n" +
            "</ncx>"

    fun createNavPoint(id: Int, text: String): String {
        return "<navPoint id=\"toc-$id\" playOrder=\"$id\">\n" +
                "<navLabel>\n" +
                "<text>$text</text>\n" +
                "</navLabel>\n" +
                "<content src=\"fb2.fb2#$id\"/>\n" +
                "</navPoint>"
    }

    fun writeToZip(zos: ZipOutputStream, name: String, stream: InputStream) {
        zos.putNextEntry(ZipEntry(name))
        stream.use { input ->
            input.copyTo(zos)
        }
    }

    fun writeToZipNoClose(zos: ZipOutputStream, name: String, stream: InputStream) {
        zos.putNextEntry(ZipEntry(name))
        stream.copyTo(zos)
    }

    fun writeToZip(zos: ZipOutputStream, name: String, content: String) {
        writeToZip(zos, name, ByteArrayInputStream(content.toByteArray(StandardCharsets.UTF_8)))
    }

    override suspend fun extractContent(path: String, outputDir: String): Result<BookContent> = runCatching {
        // O FB2 é convertido para um arquivo EPUB intermediário que depois é aberto pelo visualizador
        val tempEpubPath = File(outputDir, "${path.hashCode()}.epub").path
        if (convert(path, tempEpubPath)) {
            BookContent.EpubFile(tempEpubPath)
        } else {
            throw RuntimeException("Falha ao converter FB2 para EPUB intermediário")
        }
    }
}
