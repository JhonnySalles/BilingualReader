package br.com.ebook.extractor

import br.com.ebook.core.BookContent
import br.com.ebook.core.BookExtractor
import br.com.ebook.core.BookMetadata
import br.com.ebook.core.DateParseUtils
import br.com.ebook.core.EbookSettings
import br.com.ebook.foobnix.android.utils.TxtUtils
import br.com.ebook.foobnix.ext.CacheZipUtils.ATTACHMENTS_CACHE_DIR
import br.com.ebook.foobnix.ext.XmlParser
import br.com.ebook.foobnix.pdf.info.ExtUtils
import br.com.ebook.foobnix.pdf.info.model.BookCSS
import br.com.ebook.foobnix.sys.TempHolder
import br.com.ebook.util.IOUtils
import br.com.ebook.util.IOUtils.copyTo
import br.com.ebook.util.IOUtils.getEntryBytes
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.slf4j.LoggerFactory
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object EpubBookExtractor : BookExtractor {
    private val LOGGER = LoggerFactory.getLogger(EpubBookExtractor::class.java)

    override val supportedFormats: Set<String> = setOf("epub", "kepub")

    private fun resolvePath(basePath: String, relativePath: String): String {
        val cleanBase = if (basePath.contains("/")) basePath.substring(0, basePath.lastIndexOf("/") + 1) else ""
        val combined = cleanBase + relativePath
        val parts = combined.split("/")
        val resolvedParts = mutableListOf<String>()
        for (part in parts) {
            if (part == "..") {
                if (resolvedParts.isNotEmpty()) resolvedParts.removeAt(resolvedParts.size - 1)
            } else if (part != "." && part.isNotEmpty()) {
                resolvedParts.add(part)
            }
        }
        return resolvedParts.joinToString("/")
    }

    fun preprocessEpub(inputPath: String, outputPath: String) {
        try {
            LOGGER.info("preprocessEpub: {} || {}", inputPath, outputPath)
            val file = File(inputPath)
            
            var opfPath = ""
            var opfContentModified = ""
            var coverPageEntryName = ""
            var injectCoverPageEntryName = ""
            var injectCoverPageContent = ""
            
            ZipFile(file, StandardCharsets.UTF_8).use { zipFile ->
                val containerEntry = zipFile.getEntry("META-INF/container.xml")
                if (containerEntry != null) {
                    zipFile.getInputStream(containerEntry).use { inputStream ->
                        val containerDoc = Jsoup.parse(inputStream, "UTF-8", "", Parser.xmlParser())
                        val rootfile = containerDoc.select("rootfile[full-path]").firstOrNull()
                        if (rootfile != null) {
                            opfPath = rootfile.attr("full-path")
                        }
                    }
                }
                
                if (opfPath.isNotEmpty()) {
                    val opfEntry = zipFile.getEntry(opfPath)
                    if (opfEntry != null) {
                        zipFile.getInputStream(opfEntry).use { inputStream ->
                            val opfDoc = Jsoup.parse(inputStream, "UTF-8", "", Parser.xmlParser())
                            
                            val ncxItem = opfDoc.select("item[media-type=application/x-dtbncx+xml]").firstOrNull()
                            if (ncxItem != null) {
                                val ncxId = ncxItem.attr("id")
                                val spine = opfDoc.select("spine").firstOrNull()
                                if (spine != null && (!spine.hasAttr("toc") || spine.attr("toc").isEmpty())) {
                                    spine.attr("toc", ncxId)
                                }
                            }
                            
                            val spineItems = opfDoc.select("spine > itemref")
                            var hasCoverPage = false
                            if (spineItems.isNotEmpty()) {
                                val firstItemRef = spineItems.firstOrNull()
                                val firstIdref = firstItemRef?.attr("idref")
                                val firstItem = if (firstIdref != null) opfDoc.select("item[id=$firstIdref]").firstOrNull() else null
                                if (firstItem != null) {
                                    val href = firstItem.attr("href")
                                    val id = firstItem.attr("id")
                                    val hrefLow = href.lowercase(Locale.getDefault())
                                    val idLow = id.lowercase(Locale.getDefault())
                                    if (hrefLow.contains("cover") || hrefLow.contains("titlepage") || hrefLow.contains("title_page") ||
                                        idLow.contains("cover") || idLow.contains("titlepage") || idLow.contains("title_page")) {
                                        hasCoverPage = true
                                        coverPageEntryName = resolvePath(opfPath, href)
                                    }
                                }
                            }
                            
                            if (!hasCoverPage) {
                                val coverImgItem = opfDoc.select("item[properties*=cover-image], item[id=cover], item[id=cover-image]").firstOrNull()
                                if (coverImgItem != null) {
                                    val coverImgHref = coverImgItem.attr("href")
                                    
                                    val manifest = opfDoc.select("manifest").firstOrNull()
                                    if (manifest != null) {
                                        manifest.appendElement("item")
                                            .attr("id", "bilingual-cover-page")
                                            .attr("href", "bilingual-cover-page.xhtml")
                                            .attr("media-type", "application/xhtml+xml")
                                            
                                        val spine = opfDoc.select("spine").firstOrNull()
                                        val firstItemRef = spine?.select("itemref")?.firstOrNull()
                                        if (firstItemRef != null) {
                                            firstItemRef.before("<itemref idref=\"bilingual-cover-page\" />")
                                        } else {
                                            spine?.appendElement("itemref")?.attr("idref", "bilingual-cover-page")
                                        }
                                        
                                        injectCoverPageEntryName = resolvePath(opfPath, "bilingual-cover-page.xhtml")
                                        injectCoverPageContent = """
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
                                                <img src="$coverImgHref" alt="Cover" />
                                              </div>
                                            </body>
                                            </html>
                                        """.trimIndent()
                                    }
                                }
                            }
                            
                            opfContentModified = opfDoc.toString()
                        }
                    }
                }
            }
            
            ZipFile(file, StandardCharsets.UTF_8).use { zipFile ->
                ZipOutputStream(BufferedOutputStream(FileOutputStream(outputPath))).use { zos ->
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        zos.setLevel(0)
                    }

                    val mimetypeEntry = zipFile.getEntry("mimetype")
                    if (mimetypeEntry != null) {
                        zipFile.getInputStream(mimetypeEntry).use { inputStream ->
                            Fb2BookExtractor.writeToZipNoClose(zos, "mimetype", inputStream)
                        }
                    } else {
                        Fb2BookExtractor.writeToZipNoClose(zos, "mimetype", ByteArrayInputStream("application/epub+zip".toByteArray(StandardCharsets.UTF_8)))
                    }

                    if (injectCoverPageEntryName.isNotEmpty() && injectCoverPageContent.isNotEmpty()) {
                        Fb2BookExtractor.writeToZipNoClose(zos, injectCoverPageEntryName, ByteArrayInputStream(injectCoverPageContent.toByteArray(StandardCharsets.UTF_8)))
                    }

                    val entries = zipFile.entries()
                    while (entries.hasMoreElements()) {
                        if (TempHolder.get().loadingCancelled) break
                        val entry = entries.nextElement()
                        val name = entry.name
                        if (name == "mimetype") continue

                        val nameLow = name.lowercase(Locale.getDefault())
                        
                        if (name == opfPath && opfContentModified.isNotEmpty()) {
                            val opfBytes = opfContentModified.toByteArray(StandardCharsets.UTF_8)
                            Fb2BookExtractor.writeToZipNoClose(zos, name, ByteArrayInputStream(opfBytes))
                        } else if (name == coverPageEntryName) {
                            val coverHtml: String = zipFile.getInputStream(entry).use { inputStream ->
                                val coverDoc = Jsoup.parse(inputStream, "UTF-8", "", Parser.xmlParser())
                                val svg = coverDoc.select("svg").firstOrNull()
                                if (svg != null) {
                                    val image = svg.select("image").firstOrNull()
                                    if (image != null) {
                                        val imgHref = if (image.hasAttr("xlink:href")) image.attr("xlink:href") else image.attr("href")
                                        if (imgHref.isNotEmpty()) {
                                            val newBody = """
                                                <div>
                                                  <img src="$imgHref" alt="Cover" />
                                                </div>
                                            """.trimIndent()
                                            coverDoc.body().html(newBody)
                                            val head = coverDoc.head()
                                            head.select("style").remove()
                                            head.append("""
                                                <style type="text/css">
                                                  body { margin: 0; padding: 0; text-align: center; background-color: #ffffff; }
                                                  img { max-width: 100%; max-height: 100%; height: auto; width: auto; margin: 0 auto; display: block; }
                                                </style>
                                            """.trimIndent())
                                        }
                                    }
                                }
                                coverDoc.toString()
                            }
                            
                            if (BookCSS.get().isAutoHypens) {
                                val reader = InputStreamReader(ByteArrayInputStream(coverHtml.toByteArray(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)
                                val hStream = Fb2BookExtractor.generateHyphenFile(reader)
                                Fb2BookExtractor.writeToZipNoClose(zos, name, ByteArrayInputStream(hStream.toByteArray()))
                            } else {
                                Fb2BookExtractor.writeToZipNoClose(zos, name, ByteArrayInputStream(coverHtml.toByteArray(StandardCharsets.UTF_8)))
                            }
                        } else if (!name.endsWith("container.xml") && (nameLow.endsWith("html") || nameLow.endsWith("htm") || nameLow.endsWith("xml"))) {
                            if (BookCSS.get().isAutoHypens) {
                                zipFile.getInputStream(entry).use { inputStream ->
                                    InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
                                        val hStream = Fb2BookExtractor.generateHyphenFile(reader)
                                        Fb2BookExtractor.writeToZipNoClose(zos, name, ByteArrayInputStream(hStream.toByteArray()))
                                    }
                                }
                            } else {
                                zipFile.getInputStream(entry).use { inputStream ->
                                    Fb2BookExtractor.writeToZipNoClose(zos, name, inputStream)
                                }
                            }
                        } else {
                            zipFile.getInputStream(entry).use { inputStream ->
                                Fb2BookExtractor.writeToZipNoClose(zos, name, inputStream)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error preprocessing EPUB: {}", e.message, e)
        }
    }

    fun processHyphens(inputPath: String, outputPath: String) {
        try {
            LOGGER.info("processHyphens: {} || {}", inputPath, outputPath)
            val file = File(inputPath)
            ZipFile(file, StandardCharsets.UTF_8).use { zipFile ->
                ZipOutputStream(BufferedOutputStream(FileOutputStream(outputPath))).use { zos ->
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        zos.setLevel(0)
                    }

                    val mimetypeEntry = zipFile.getEntry("mimetype")
                    if (mimetypeEntry != null) {
                        zipFile.getInputStream(mimetypeEntry).use { inputStream ->
                            Fb2BookExtractor.writeToZipNoClose(zos, "mimetype", inputStream)
                        }
                    }

                    val entries = zipFile.entries()
                    while (entries.hasMoreElements()) {
                        if (TempHolder.get().loadingCancelled) break
                        val entry = entries.nextElement()
                        val name = entry.name
                        if (name == "mimetype") continue
                        val nameLow = name.lowercase(Locale.getDefault())

                        if (!name.endsWith("container.xml") && (nameLow.endsWith("html") || nameLow.endsWith("htm") || nameLow.endsWith("xml"))) {
                            zipFile.getInputStream(entry).use { inputStream ->
                                InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
                                    val hStream = Fb2BookExtractor.generateHyphenFile(reader)
                                    Fb2BookExtractor.writeToZipNoClose(zos, name, ByteArrayInputStream(hStream.toByteArray()))
                                }
                            }
                        } else {
                            zipFile.getInputStream(entry).use { inputStream ->
                                Fb2BookExtractor.writeToZipNoClose(zos, name, inputStream)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error process hyphens: {}", e.message, e)
        }
    }

    override suspend fun extractOverview(path: String): Result<String> = runCatching {
        var info = ""
        ZipFile(File(path), StandardCharsets.UTF_8).use { zipFile ->
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.lowercase(Locale.getDefault()).endsWith(".opf")) {
                    zipFile.getInputStream(entry).use { inputStream ->
                        val xpp = XmlParser.buildPullParser()
                        xpp.setInput(inputStream, "utf-8")
                        var eventType = xpp.eventType
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            if (eventType == XmlPullParser.START_TAG) {
                                val tagName = xpp.name
                                val cleanTagName = if (tagName.contains(":")) tagName.substring(tagName.indexOf(":") + 1) else tagName
                                if (cleanTagName == "description") {
                                    info = xpp.nextText()
                                    break
                                }
                            }
                            if (eventType == XmlPullParser.END_TAG && xpp.name.endsWith("metadata")) {
                                break
                            }
                            eventType = xpp.next()
                        }
                    }
                    if (info.isNotEmpty()) break
                }
            }
        }
        info
    }.onFailure { e ->
        IOUtils.reportException(e, "Error extracting overview from epub: $path")
    }

    override suspend fun extractMetadata(path: String): Result<BookMetadata> = runCatching {
        var title = ""
        var author = ""
        var subject = ""
        var series = ""
        var number = ""
        var lang = ""
        var isbn = ""
        var publisher = ""
        var releaseDate: LocalDate? = null

        ZipFile(File(path), StandardCharsets.UTF_8).use { zipFile ->
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.lowercase(Locale.getDefault()).endsWith(".opf")) {
                    zipFile.getInputStream(entry).use { inputStream ->
                        val xpp = XmlParser.buildPullParser()
                        xpp.setInput(inputStream, "utf-8")
                        var eventType = xpp.eventType
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            if (eventType == XmlPullParser.START_TAG) {
                                val tagName = xpp.name
                                val cleanTagName = if (tagName.contains(":")) tagName.substring(tagName.indexOf(":") + 1) else tagName
                                when (cleanTagName) {
                                    "title" -> title = xpp.nextText() ?: ""
                                    "creator" -> {
                                        val creatorVal = xpp.nextText() ?: ""
                                        author = if (author.isEmpty()) creatorVal else "$author, $creatorVal"
                                    }
                                    "subject" -> {
                                        val subVal = xpp.nextText() ?: ""
                                        subject = if (subject.isEmpty()) subVal else "$subject, $subVal"
                                    }
                                    "language" -> {
                                        if (lang.isEmpty()) lang = xpp.nextText() ?: ""
                                    }
                                    "identifier" -> {
                                        val content = xpp.nextText() ?: ""
                                        if (content.lowercase(Locale.getDefault()).contains("isbn")) {
                                            isbn = content.replace(Regex("\\D"), "")
                                        }
                                    }
                                    "publisher" -> publisher = xpp.nextText() ?: ""
                                    "date" -> {
                                        val dateStr = xpp.nextText()
                                        releaseDate = DateParseUtils.parseFlexibleDate(dateStr)
                                    }
                                    "meta" -> {
                                        val nameAttr = xpp.getAttributeValue(null, "name")
                                        if (nameAttr == "calibre:series") {
                                            series = xpp.getAttributeValue(null, "content") ?: ""
                                        } else if (nameAttr == "calibre:series_index") {
                                            number = xpp.getAttributeValue(null, "content") ?: ""
                                            number = number.replace(".0", "")
                                        } else {
                                            val propertyAttr = xpp.getAttributeValue(null, "property")
                                            if (propertyAttr == "group-position") {
                                                number = xpp.text ?: ""
                                            }
                                        }
                                    }
                                }
                            }
                            if (eventType == XmlPullParser.END_TAG && xpp.name.endsWith("metadata")) {
                                break
                            }
                            eventType = xpp.next()
                        }
                    }
                }
            }
        }

        if (EbookSettings.isFirstSurname) {
            author = TxtUtils.replaceLastFirstName(author) ?: ""
        }

        val sIndex = try {
            if (number.isNotEmpty()) number.toInt() else 0
        } catch (e: Exception) {
            0
        }

        BookMetadata(
            title = title,
            author = author,
            series = series,
            genre = subject.replace(Regex(",$"), ""),
            isbn = isbn,
            publisher = publisher,
            releaseDate = releaseDate,
            seriesIndex = sIndex,
            language = lang,
            unzipPath = path
        )
    }.onFailure { e ->
        IOUtils.reportException(e, "Error extracting metadata from epub: $path")
    }

    override suspend fun extractCover(path: String): Result<ByteArray?> = runCatching {
        var coverBytes: ByteArray? = null
        ZipFile(File(path), StandardCharsets.UTF_8).use { zipFile ->
            var coverName: String? = null
            var coverResource: String? = null

            // Passada 1: Encontrar referencia de capa no OPF
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.lowercase(Locale.getDefault()).endsWith(".opf")) {
                    zipFile.getInputStream(entry).use { inputStream ->
                        val xpp = XmlParser.buildPullParser()
                        xpp.setInput(inputStream, "utf-8")
                        var eventType = xpp.eventType
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            if (eventType == XmlPullParser.START_TAG) {
                                val tagName = xpp.name
                                val cleanTagName = if (tagName.contains(":")) tagName.substring(tagName.indexOf(":") + 1) else tagName
                                if (cleanTagName == "meta" && xpp.getAttributeValue(null, "name") == "cover") {
                                    coverResource = xpp.getAttributeValue(null, "content")
                                }

                                if (coverResource != null && cleanTagName == "item" &&
                                    (coverResource == xpp.getAttributeValue(null, "id") || coverResource == xpp.getAttributeValue(null, "properties"))) {
                                    coverName = xpp.getAttributeValue(null, "href")
                                    if (coverName?.endsWith(".svg") == true) {
                                        coverName = null
                                    }
                                    break
                                }

                                if (coverResource == null && cleanTagName == "item" && xpp.getAttributeValue(null, "properties") != null &&
                                    xpp.getAttributeValue(null, "properties").lowercase(Locale.getDefault()).contains("cover")) {
                                    coverName = xpp.getAttributeValue(null, "href")
                                    if (coverName?.endsWith(".svg") == true) {
                                        coverName = null
                                    }
                                    break
                                }
                            }
                            eventType = xpp.next()
                        }
                    }
                    if (coverName != null) break
                }
            }

            // Se achou pelo OPF, busca a entrada correspondente
            if (coverName != null) {
                val searchName = coverName!!
                val cleanSearchName = if (searchName.contains("/")) searchName.substring(searchName.lastIndexOf("/") + 1) else searchName
                val innerEntries = zipFile.entries()
                while (innerEntries.hasMoreElements()) {
                    val entry = innerEntries.nextElement()
                    if (entry.name.contains(searchName)) {
                        try {
                            coverBytes = zipFile.getEntryBytes(entry)
                        } catch (e: Exception) {
                            LOGGER.error("Error extract attachment: {}", e.message, e)
                        }
                        break
                    }
                }

                if (coverBytes == null) {
                    val innerEntries2 = zipFile.entries()
                    while (innerEntries2.hasMoreElements()) {
                        val entry = innerEntries2.nextElement()
                        if (entry.name.contains(cleanSearchName)) {
                            coverBytes = zipFile.getEntryBytes(entry)
                            break
                        }
                    }
                }
            }

            // Fallback 1: Buscar arquivos que começam com "cover" ou contêm "cover"
            if (coverBytes == null) {
                var coverAux: ByteArray? = null
                val innerEntries = zipFile.entries()
                while (innerEntries.hasMoreElements()) {
                    val entry = innerEntries.nextElement()
                    val name = entry.name.lowercase(Locale.getDefault())
                    val cleanName = when {
                        name.contains("\\") -> name.substring(name.lastIndexOf("\\") + 1)
                        name.contains("/") -> name.substring(name.lastIndexOf("/") + 1)
                        else -> name
                    }

                    if (cleanName.endsWith(".jpeg") || cleanName.endsWith(".jpg") || cleanName.endsWith(".png")) {
                        if (cleanName.startsWith("cover")) {
                            coverBytes = zipFile.getEntryBytes(entry)
                            break
                        }
                        if (cleanName.contains("cover")) {
                            coverAux = zipFile.getEntryBytes(entry)
                        }
                    }
                }
                if (coverBytes == null && coverAux != null) {
                    coverBytes = coverAux
                }
            }

            // Fallback 2: Buscar a primeira imagem do zip
            if (coverBytes == null) {
                val innerEntries = zipFile.entries()
                while (innerEntries.hasMoreElements()) {
                    val entry = innerEntries.nextElement()
                    val name = entry.name.lowercase(Locale.getDefault())
                    if (name.endsWith(".jpeg") || name.endsWith(".jpg") || name.endsWith(".png")) {
                        coverBytes = zipFile.getEntryBytes(entry)
                        break
                    }
                }
            }
        }
        coverBytes
    }.onFailure { e ->
        IOUtils.reportException(e, "Error extracting cover from epub: $path")
    }

    fun extractAttachment(bookPath: File, attachmentName: String): File? {
        LOGGER.info("extractAttachment: {} -- {}", bookPath.path, attachmentName)
        try {
            ZipFile(bookPath).use { zipFile ->
                val entry = zipFile.getEntry(attachmentName) ?: return null
                var cleanName = attachmentName
                if (cleanName.contains("/")) {
                    cleanName = cleanName.substring(cleanName.lastIndexOf("/") + 1)
                }
                val extractMedia = File(ATTACHMENTS_CACHE_DIR, cleanName)
                LOGGER.info("extractAttachment extract: {}", extractMedia.path)

                zipFile.getInputStream(entry).use { inputStream ->
                    FileOutputStream(extractMedia).use { outputStream ->
                        BufferedOutputStream(outputStream).use { bufferedOut ->
                            inputStream.copyTo(bufferedOut)
                        }
                    }
                }
                return extractMedia
            }
        } catch (e: Exception) {
            LOGGER.error("Error extract attachment: {}", e.message, e)
            return null
        }
    }

    fun getAttachments(inputPath: String): List<String> {
        val attachments = mutableListOf<String>()
        try {
            ZipFile(File(inputPath)).use { zipFile ->
                val entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    if (TempHolder.get().loadingCancelled) break
                    val entry = entries.nextElement()
                    val name = entry.name
                    if (ExtUtils.isMediaContent(name)) {
                        val size = if (entry.size > 0) entry.size else if (entry.compressedSize > 0) entry.compressedSize else 0
                        attachments.add("$name,$size")
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error get attachments: {}", e.message, e)
        }
        return attachments
    }

    override suspend fun extractFooterNotes(path: String): Result<Map<String, String>> = runCatching {
        val notes = mutableMapOf<String, String>()
        val textLink = mutableMapOf<String, String>()
        val files = mutableSetOf<String>()
        val zipEntriesByName = mutableMapOf<String, ZipEntry>()

        ZipFile(File(path), StandardCharsets.UTF_8).use { zipFile ->
            br.com.ebook.foobnix.ext.CacheZipUtils.removeFiles(ATTACHMENTS_CACHE_DIR?.listFiles())

            // Passada 1: Coletar links de notas e indexar todas as entradas do ZIP
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                if (TempHolder.get().loadingCancelled) break
                val entry = entries.nextElement()
                val name = entry.name
                val nameLow = name.lowercase(Locale.getDefault())

                val simpleName = if (name.contains("/")) name.substring(name.lastIndexOf("/") + 1) else name
                zipEntriesByName[simpleName] = entry

                if (!nameLow.endsWith("container.xml") && (nameLow.endsWith("html") || nameLow.endsWith("htm") || nameLow.endsWith("xml"))) {
                    zipFile.getInputStream(entry).use { inputStream ->
                        val parse = Jsoup.parse(inputStream, null, "", Parser.xmlParser())
                        val select = parse.select("a[href]")
                        for (item in select) {
                            val text = item.text()
                            val href = item.attr("href")
                            if (href.contains("#")) {
                                var file = href.substring(0, href.indexOf("#"))
                                var attr = href
                                if (attr.startsWith("#")) {
                                    attr = name + attr
                                }
                                if (!TxtUtils.isFooterNote(text)) {
                                    continue
                                }
                                textLink[attr] = text
                                if (TxtUtils.isEmpty(file)) {
                                    file = name
                                }
                                val fileLow = file.lowercase(Locale.getDefault())
                                if (fileLow.endsWith("html") || fileLow.endsWith("htm") || fileLow.endsWith("xml")) {
                                    files.add(file)
                                }
                            }
                        }
                    }
                }
            }

            // Passada 2: Resolver os textos dessas notas de rodapé de forma linear indexada
            for (fileName in files) {
                if (TempHolder.get().loadingCancelled) break
                val simpleFileName = if (fileName.contains("/")) fileName.substring(fileName.lastIndexOf("/") + 1) else fileName
                val entry = zipEntriesByName[simpleFileName] ?: continue
                zipFile.getInputStream(entry).use { inputStream ->
                    val parse = Jsoup.parse(inputStream, null, "", Parser.xmlParser())
                    val ids = parse.select("[id]")
                    for (item in ids) {
                        val id = item.attr("id")
                        var value = item.text()

                        if (value.trim().length < 4) {
                            value = value + " " + parse.select("[id=$id]+*").text()
                        }
                        if (value.trim().length < 4) {
                            value = value + " " + parse.select("[id=$id]+*+*").text()
                        }
                        try {
                            if (value.trim().length < 4) {
                                value = value + " " + parse.select("[id=$id]").parents()[0].text()
                            }
                        } catch (e: Exception) {
                            // Ignora erro
                        }

                        val fileKey = "$fileName#$id"
                        val textKey = textLink[fileKey]
                        if (textKey != null) {
                            notes[textKey] = value
                        }
                    }
                }
            }
        }
        notes
    }.onFailure { e ->
        IOUtils.reportException(e, "Error extracting footer notes from epub: $path")
    }

    override suspend fun extractContent(path: String, outputDir: String): Result<BookContent> = runCatching {
        // Para EPUB, a extração de conteúdo consiste apenas em retornar o próprio arquivo
        // ou descompactar se necessário. No fluxo legado, ele apenas verifica/retorna o caminho.
        BookContent.EpubFile(path)
    }.onFailure { e ->
        IOUtils.reportException(e, "Error extracting content from epub: $path")
    }

    fun extractTocOutline(path: String): List<org.ebookdroid.core.codec.OutlineLink> {
        val list = mutableListOf<org.ebookdroid.core.codec.OutlineLink>()
        try {
            ZipFile(File(path), StandardCharsets.UTF_8).use { zipFile ->
                var ncxEntry: ZipEntry? = null
                var navEntry: ZipEntry? = null
                val entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val nameLow = entry.name.lowercase(Locale.getDefault())
                    if (nameLow.endsWith(".ncx") && ncxEntry == null) {
                        ncxEntry = entry
                    } else if ((nameLow.contains("nav") || nameLow.contains("toc")) && (nameLow.endsWith(".xhtml") || nameLow.endsWith(".html")) && navEntry == null) {
                        navEntry = entry
                    }
                }

                if (ncxEntry != null) {
                    zipFile.getInputStream(ncxEntry).use { inputStream ->
                        val doc = Jsoup.parse(inputStream, "UTF-8", "", Parser.xmlParser())
                        val navPoints = doc.select("navPoint")
                        for (np in navPoints) {
                            val text = np.select("navLabel > text").text()
                            val src = np.select("content").attr("src")
                            if (text.isNotEmpty() && src.isNotEmpty()) {
                                list.add(org.ebookdroid.core.codec.OutlineLink(text, src, 0))
                            }
                        }
                    }
                }

                if (list.isEmpty() && navEntry != null) {
                    zipFile.getInputStream(navEntry).use { inputStream ->
                        val doc = Jsoup.parse(inputStream, "UTF-8", "", Parser.htmlParser())
                        val links = doc.select("nav[epub|type=toc] a[href], nav#toc a[href], body a[href]")
                        for (a in links) {
                            val text = a.text()
                            val href = a.attr("href")
                            if (text.isNotEmpty() && href.isNotEmpty()) {
                                list.add(org.ebookdroid.core.codec.OutlineLink(text, href, 0))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error extracting TOC outline from {}: {}", path, e.message, e)
        }
        return list
    }
}
