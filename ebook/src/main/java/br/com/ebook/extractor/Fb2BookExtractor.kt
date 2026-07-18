package br.com.ebook.extractor

import android.util.Base64
import br.com.ebook.core.*
import br.com.ebook.util.IOUtils
import br.com.ebook.util.IOUtils.copyTo
import br.com.ebook.util.Fb2Templates
import br.com.ebook.core.EbookSettings
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
import java.time.LocalDate
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

        var author = if (EbookSettings.isFirstSurname) {
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
            val binaries = mutableMapOf<String, ByteArray>()
            val contentTypes = mutableMapOf<String, String>()
            var coverId: String? = null

            val encoding = findHeaderEncoding(inputFile)

            // Step 1: Scan FB2 for binary tags
            try {
                FileInputStream(inputFile).use { fis ->
                    val xpp = XmlParser.buildPullParser()
                    xpp.setInput(fis, encoding)
                    var eventType = xpp.eventType
                    var firstImageId: String? = null
                    var coverImageId: String? = null

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            if (xpp.name == "image") {
                                val href = xpp.getAttributeValue(0)
                                if (!href.isNullOrEmpty()) {
                                    val cleanHref = href.replace("#", "")
                                    if (firstImageId == null) firstImageId = cleanHref
                                    if (cleanHref.lowercase(Locale.getDefault()).contains("cover")) {
                                        coverImageId = cleanHref
                                    }
                                }
                            } else if (xpp.name == "binary") {
                                val id = xpp.getAttributeValue(null, "id")
                                val contentType = xpp.getAttributeValue(null, "content-type") ?: "image/jpeg"
                                val text = xpp.nextText()
                                if (!id.isNullOrEmpty() && !text.isNullOrEmpty()) {
                                    try {
                                        val bytes = Base64.decode(text, Base64.DEFAULT)
                                        binaries[id] = bytes
                                        contentTypes[id] = contentType
                                    } catch (e: Exception) {
                                        LOGGER.error("Failed to decode binary base64: {}", id, e)
                                    }
                                }
                            }
                        }
                        eventType = xpp.next()
                    }
                    coverId = coverImageId ?: firstImageId ?: binaries.keys.firstOrNull()
                }
            } catch (e: Exception) {
                LOGGER.error("Error scanning binaries: {}", e.message, e)
            }

            ZipOutputStream(BufferedOutputStream(FileOutputStream(File(toName)))).use { zos ->
                zos.setLevel(0)
                writeToZip(zos, "mimetype", "application/epub+zip")
                writeToZip(zos, "META-INF/container.xml", Fb2Templates.container_xml)

                // Step 2: Write binaries to ZIP and prepare manifest/spine
                val manifestItems = StringBuilder()
                val spineItems = StringBuilder()

                // Cover image injection
                val coverBytes = coverId?.let { binaries[it] }
                if (coverBytes != null && coverBytes.isNotEmpty()) {
                    val coverType = contentTypes[coverId] ?: "image/jpeg"
                    writeToZip(zos, "OEBPS/bilingual-cover-image.jpg", ByteArrayInputStream(coverBytes))
                    manifestItems.append("  <item id=\"bilingual-cover-image\" href=\"bilingual-cover-image.jpg\" media-type=\"$coverType\" />\n")
                    
                    val xhtml = """
                        <?xml version="1.0" encoding="utf-8"?>
                        <!DOCTYPE html>
                        <html xmlns="http://www.w3.org/1999/xhtml">
                        <head>
                          <title>Cover</title>
                          <style type="text/css">
                            body { margin: 0; padding: 0; text-align: center; background-color: #ffffff; }
                            img { max-width: 100%; max-height: 100%; height: auto; width: auto; margin: 0 auto; display: block; }
                          </style>
                        </head>
                        <body>
                          <div>
                            <img src="bilingual-cover-image.jpg" alt="Cover" />
                          </div>
                        </body>
                        </html>
                    """.trimIndent()
                    writeToZip(zos, "OEBPS/bilingual-cover-page.xhtml", xhtml)
                    manifestItems.append("  <item id=\"bilingual-cover-page\" href=\"bilingual-cover-page.xhtml\" media-type=\"application/xhtml+xml\" />\n")
                    spineItems.append("  <itemref idref=\"bilingual-cover-page\" />\n")
                }

                // Other images
                for ((id, bytes) in binaries) {
                    val type = contentTypes[id] ?: "image/jpeg"
                    writeToZip(zos, "OEBPS/$id", ByteArrayInputStream(bytes))
                    manifestItems.append("  <item id=\"$id\" href=\"$id\" media-type=\"$type\" />\n")
                }

                var title = "FB2 Book"
                var author = "Unknown"
                try {
                    kotlinx.coroutines.runBlocking {
                        val metaResult = extractMetadata(inputFile)
                        val meta = metaResult.getOrNull()
                        if (meta != null) {
                            if (meta.title.isNotEmpty()) title = meta.title
                            if (meta.author.isNotEmpty()) author = meta.author
                        }
                    }
                } catch (e: Exception) {
                    LOGGER.error("Error extracting metadata for OPF: {}", e.message, e)
                }

                val opf = Fb2Templates.content_opf
                    .replace("%manifest%", manifestItems.toString())
                    .replace("%spine%", spineItems.toString())
                    .replace("%title%", title)
                    .replace("%creator%", author)

                writeToZip(zos, "OEBPS/content.opf", opf)

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
                writeToZip(zos, "META-INF/container.xml", Fb2Templates.container_xml)

                var meta = Fb2Templates.content_opf.replace("fb2.fb2", "temp" + ExtUtils.REFLOW_HTML)
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
        
        if (EbookSettings.isAutoHypens) {
            HypenUtils.applyLanguage(EbookSettings.hypenLang)
        }

        var count = 0
        var isEncoding = false
        var isFindBodyEnd = false
        var titleBegin = false
        var insideBinary = false

        BufferedReader(InputStreamReader(FileInputStream(fb2), encoding)).use { input ->
            var line: String?
            while (input.readLine().also { line = it } != null) {
                if (TempHolder.get().loadingCancelled) break
                var curLine = line!!

                if (curLine.contains("<binary", ignoreCase = true)) {
                    insideBinary = true
                }
                if (insideBinary) {
                    if (curLine.contains("</binary>", ignoreCase = true)) {
                        insideBinary = false
                    }
                    continue
                }

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

                    if (EbookSettings.isAutoHypens && sub.contains("<title")) {
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

                    if (!isFindBodyEnd && (EbookSettings.isDouble || !titleBegin) && EbookSettings.isAutoHypens) {
                        sub = HypenUtils.applyHypnesOld(sub)
                    }

                    sub = sub.replace(Regex("<image\\s+[^>]*href=\"#([^\"]+)\"[^>]*>"), "<img src=\"$1\" style=\"max-width: 100%;\" />")
                    sub = sub.replace(Regex("<image\\s+[^>]*href=\"#([^\"]+)\"\\s*/>"), "<img src=\"$1\" style=\"max-width: 100%;\" />")

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
        return Fb2Templates.NCX.replace("%nav%", navs.toString())
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
        return Fb2Templates.NCX.replace("%nav%", navs.toString())
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



    fun createNavPoint(id: Int, text: String): String {
        return "<navPoint id=\"toc-$id\" playOrder=\"$id\">\n" +
                "<navLabel>\n" +
                "<text>$text</text>\n" +
                "</navLabel>\n" +
                "<content src=\"fb2.fb2#$id\"/>\n" +
                "</navPoint>"
    }

    fun generateHyphenFile(input: InputStreamReader): ByteArrayOutputStream {
        val out = ByteArrayOutputStream()
        val writer = PrintWriter(out)
        val reader = BufferedReader(input)
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            if (TempHolder.get().loadingCancelled) {
                break
            }
            var currentLine = line ?: ""
            if (!currentLine.endsWith(" ")) {
                currentLine = "$currentLine "
            }

            val subLine = currentLine.split("</")

            for (i in subLine.indices) {
                var processedLine = if (i == 0) {
                    subLine[i]
                } else {
                    "</${subLine[i]}"
                }

                processedLine = HypenUtils.applyHypnes(processedLine)
                writer.print(processedLine)
            }
        }
        writer.close()
        return out
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
