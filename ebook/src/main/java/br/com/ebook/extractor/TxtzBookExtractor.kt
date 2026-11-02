package br.com.ebook.extractor

import br.com.ebook.core.BookContent
import br.com.ebook.core.BookExtractor
import br.com.ebook.core.BookMetadata
import br.com.ebook.foobnix.ext.XmlParser
import br.com.ebook.util.IOUtils.copyTo
import org.ebookdroid.core.codec.OutlineLink
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.jsoup.safety.Safelist
import org.slf4j.LoggerFactory
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern
import java.util.zip.ZipFile

object TxtzBookExtractor : BookExtractor {
    private val LOGGER = LoggerFactory.getLogger(TxtzBookExtractor::class.java)

    override val supportedFormats: Set<String> = setOf("txtz")

    private val CHAPTER_PATTERN = Pattern.compile(
        "^\\s*(cap.?tulo\\s+\\d+.*|chapter\\s+\\d+.*|ep.?logo.*|prologue.*|pr.?logo.*|afterword.*|[0-9IVXLCDM]+\\..*)\\s*$",
        Pattern.CASE_INSENSITIVE
    )

    override suspend fun extractMetadata(path: String): Result<BookMetadata> = runCatching {
        var title = ""
        var author = ""
        var publisher = ""
        var description = ""
        var series = ""
        var seriesIndex = 0
        var isbn = ""

        try {
            ZipFile(path).use { zip ->
                val entry = zip.getEntry("metadata.opf")
                if (entry != null) {
                    zip.getInputStream(entry).use { stream ->
                        val doc = Jsoup.parse(stream, "UTF-8", "", org.jsoup.parser.Parser.xmlParser())
                        val t = doc.select("dc|title, title").text().trim()
                        if (t.isNotEmpty()) title = t

                        val authors = doc.select("dc|creator, creator").map { it.text().trim() }.filter { it.isNotEmpty() }
                        if (authors.isNotEmpty()) {
                            author = authors.joinToString(", ")
                        }

                        val pub = doc.select("dc|publisher, publisher").text().trim()
                        if (pub.isNotEmpty()) publisher = pub

                        val desc = doc.select("dc|description, description").text().trim()
                        if (desc.isNotEmpty()) description = desc

                        val seriesMeta = doc.select("meta[name=calibre:series]").firstOrNull()?.attr("content")?.trim()
                        if (!seriesMeta.isNullOrEmpty()) series = seriesMeta

                        val indexMeta = doc.select("meta[name=calibre:series_index]").firstOrNull()?.attr("content")?.trim()
                        if (!indexMeta.isNullOrEmpty()) seriesIndex = indexMeta.toFloatOrNull()?.toInt() ?: 0

                        val idents = doc.select("dc|identifier, identifier")
                        for (el in idents) {
                            val idVal = el.text().trim()
                            if (idVal.startsWith("978") || idVal.length == 10 || idVal.length == 13) {
                                isbn = idVal
                                break
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error reading TXTZ metadata: {}", e.message, e)
        }

        if (title.isBlank()) {
            title = File(path).nameWithoutExtension
        }

        BookMetadata(
            title = title,
            author = author,
            unzipPath = path,
            annotation = description,
            publisher = publisher,
            series = series,
            seriesIndex = seriesIndex,
            isbn = isbn
        )
    }

    override suspend fun extractCover(path: String): Result<ByteArray?> = runCatching {
        var coverBytes: ByteArray? = null
        try {
            ZipFile(path).use { zip ->
                var coverEntryName: String? = null
                val opfEntry = zip.getEntry("metadata.opf")
                if (opfEntry != null) {
                    try {
                        zip.getInputStream(opfEntry).use { stream ->
                            val doc = Jsoup.parse(stream, "UTF-8", "", Parser.xmlParser())
                            val coverEl = doc.select("cover-relpath-from-base, meta[name=cover-relpath-from-base]").firstOrNull()
                            val coverMeta = if (coverEl?.tagName() == "meta") coverEl.attr("content").trim() else coverEl?.text()?.trim()
                            if (!coverMeta.isNullOrEmpty()) {
                                coverEntryName = coverMeta
                            }
                        }
                    } catch (ignored: Exception) {}
                }

                val entry = (if (coverEntryName != null) zip.getEntry(coverEntryName) else null)
                    ?: zip.getEntry("cover.jpeg") ?: zip.getEntry("cover.jpg") ?: zip.getEntry("cover.png")
                if (entry != null) {
                    zip.getInputStream(entry).use { input ->
                        coverBytes = input.readBytes()
                    }
                } else {
                    val entries = zip.entries().toList()
                    val firstImage = entries.firstOrNull {
                        val name = it.name.lowercase()
                        !it.isDirectory && (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp"))
                    }
                    if (firstImage != null) {
                        zip.getInputStream(firstImage).use { input ->
                            coverBytes = input.readBytes()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error extracting TXTZ cover: {}", e.message, e)
        }
        coverBytes
    }

    override suspend fun extractOverview(path: String): Result<String> = runCatching {
        var description = ""
        try {
            ZipFile(path).use { zip ->
                val entry = zip.getEntry("metadata.opf")
                if (entry != null) {
                    zip.getInputStream(entry).use { stream ->
                        val doc = Jsoup.parse(stream, "UTF-8", "", org.jsoup.parser.Parser.xmlParser())
                        val descEl = doc.select("dc|description, description").firstOrNull()
                        if (descEl != null && descEl.text().isNotBlank()) {
                            description = Jsoup.clean(descEl.text(), Safelist.simpleText())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error extracting TXTZ overview: {}", e.message, e)
        }
        description.trim()
    }

    override suspend fun extractFooterNotes(path: String): Result<Map<String, String>> = runCatching {
        emptyMap()
    }

    private fun decodeTextBytes(bytes: ByteArray): String {
        return try {
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
            decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString()
        } catch (e: Exception) {
            String(bytes, java.nio.charset.Charset.forName("windows-1252"))
        }
    }

    private fun escapeHtml(text: String): String {
        val out = StringBuilder(text.length)
        for (c in text) {
            when (c) {
                '<' -> out.append("&lt;")
                '>' -> out.append("&gt;")
                '&' -> out.append("&amp;")
                '"' -> out.append("&quot;")
                '\'' -> out.append("&#39;")
                else -> out.append(c)
            }
        }
        return out.toString()
    }

    data class TxtzChapterBoundary(
        val lineIndex: Int,
        val title: String,
        val chapterNumber: Int
    )

    fun findChapterBoundaries(lines: List<String>): List<TxtzChapterBoundary> {
        val candidates = mutableListOf<Int>()
        for ((idx, line) in lines.withIndex()) {
            if (CHAPTER_PATTERN.matcher(line).matches()) {
                candidates.add(idx)
            }
        }
        if (candidates.isEmpty()) return emptyList()

        // Cluster detection: if candidate i has neighbor candidate within 10 lines, it's Table of Contents (TOC)
        val isTocCandidate = mutableSetOf<Int>()
        for (i in candidates.indices) {
            val c = candidates[i]
            val prevClose = (i > 0 && (c - candidates[i - 1] <= 10))
            val nextClose = (i + 1 < candidates.size && (candidates[i + 1] - c <= 10))
            if (prevClose || nextClose) {
                isTocCandidate.add(c)
            }
        }

        val realBoundaries = mutableListOf<TxtzChapterBoundary>()
        var chapNum = 1
        for (c in candidates) {
            if (!isTocCandidate.contains(c)) {
                realBoundaries.add(TxtzChapterBoundary(c, lines[c], chapNum++))
            }
        }
        return realBoundaries
    }

    override suspend fun extractContent(path: String, outputDir: String): Result<BookContent> = runCatching {
        val hashCode = path.hashCode().toString()
        val destDir = File(outputDir, "txtz_temp_$hashCode")
        destDir.mkdirs()

        var textContent = ""
        var coverFileName: String? = null
        val resourceBytes = mutableMapOf<String, ByteArray>()

        ZipFile(path).use { zip ->
            // Check metadata.opf for cover-relpath-from-base
            val opfEntry = zip.getEntry("metadata.opf")
            if (opfEntry != null) {
                try {
                    zip.getInputStream(opfEntry).use { stream ->
                        val doc = Jsoup.parse(stream, "UTF-8", "", Parser.xmlParser())
                        val coverEl = doc.select("cover-relpath-from-base, meta[name=cover-relpath-from-base]").firstOrNull()
                        val coverMeta = if (coverEl?.tagName() == "meta") coverEl.attr("content").trim() else coverEl?.text()?.trim()
                        if (!coverMeta.isNullOrEmpty()) {
                            coverFileName = coverMeta
                        }
                    }
                } catch (e: Exception) {
                    LOGGER.warn("Could not read cover from metadata.opf: {}", e.message)
                }
            }

            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val nameLower = entry.name.lowercase()
                if (nameLower.endsWith(".txt") && (nameLower.contains("index") || textContent.isEmpty())) {
                    val bytes = zip.getInputStream(entry).use { it.readBytes() }
                    textContent = decodeTextBytes(bytes)
                } else if (nameLower.endsWith(".jpg") || nameLower.endsWith(".jpeg") || nameLower.endsWith(".png") || nameLower.endsWith(".webp")) {
                    val bytes = zip.getInputStream(entry).use { it.readBytes() }
                    resourceBytes[entry.name] = bytes
                    val outFile = File(destDir, entry.name)
                    outFile.parentFile?.mkdirs()
                    outFile.writeBytes(bytes)

                    if (coverFileName == null && nameLower.startsWith("cover.")) {
                        coverFileName = entry.name
                    }
                }
            }
        }

        if (coverFileName == null) {
            coverFileName = resourceBytes.keys.firstOrNull { it.lowercase().startsWith("cover.") }
                ?: resourceBytes.keys.firstOrNull()
        }

        val coverBytes = if (coverFileName != null) resourceBytes[coverFileName!!] else null
        val hasCover = coverBytes != null && coverBytes.isNotEmpty()

        // Imagens que são diferentes da capa são adicionadas como páginas de galeria ao final do livro.
        // Ignora imagens que sejam a capa primária, duplicatas com o mesmo conteúdo ou nome variantes de cover.
        val galleryItems = mutableListOf<br.com.ebook.util.EpubPackager.GalleryItem>()
        for ((name, bytes) in resourceBytes) {
            val isCoverDuplicate = name.equals(coverFileName, ignoreCase = true) ||
                    File(name).nameWithoutExtension.equals("cover", ignoreCase = true) ||
                    (hasCover && coverBytes.contentEquals(bytes))

            if (!isCoverDuplicate) {
                galleryItems.add(
                    br.com.ebook.util.EpubPackager.GalleryItem(
                        imageFileName = name,
                        title = File(name).nameWithoutExtension
                    )
                )
            }
        }

        val rawLines = textContent.replace("\r", "").split('\n')
        val lines = rawLines.map { it.trim() }
        val boundaries = findChapterBoundaries(lines)
        val boundaryMap = boundaries.associateBy { it.lineIndex }

        val outHtmlFile = File(destDir, "index.html")
        java.io.PrintWriter(java.io.BufferedWriter(java.io.OutputStreamWriter(FileOutputStream(outHtmlFile), Charsets.UTF_8))).use { writer ->
            writer.println("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>body{margin:0;padding:0;} p,p+p{margin:0;text-indent:1.5em;line-height:1.4;} h2{text-align:center;margin:1.5em 0;} img{max-width:100%;height:auto;display:block;margin:0 auto;} .br-gap{margin:1.5em 0;}</style></head><body>")

            if (hasCover && coverFileName != null) {
                writer.println("<div class=\"br-full-page\" style=\"text-align: center; margin: 0; padding: 0;\"><img src=\"$coverFileName\" style=\"max-width: 100%; max-height: 100%; width: auto; height: auto; display: block; margin: 0 auto;\" alt=\"Cover\"/></div>")
            }

            var consecutiveEmpty = 0
            for ((idx, line) in lines.withIndex()) {
                val boundary = boundaryMap[idx]
                if (boundary != null) {
                    writer.println("<h2 id=\"txtz_chap_${boundary.chapterNumber}\" style=\"page-break-before: always;\"><a id=\"txtz_chap_${boundary.chapterNumber}\"></a>${escapeHtml(line)}</h2>")
                    consecutiveEmpty = 0
                } else if (line.isEmpty()) {
                    consecutiveEmpty++
                } else {
                    if (consecutiveEmpty >= 3) {
                        writer.println("<p class=\"br-gap\">&#160;</p>")
                    }
                    consecutiveEmpty = 0
                    writer.println("<p>${escapeHtml(line)}</p>")
                }
            }

            if (galleryItems.isNotEmpty()) {
                writer.println("<div id=\"txtz_gallery\" style=\"page-break-before: always;\"><a id=\"txtz_gallery\"></a></div>")
                for (item in galleryItems) {
                    writer.println("<div class=\"bilingual-reader-gallery-page\" style=\"page-break-after: always; text-align: center; margin: 0; padding: 0;\">")
                    writer.println("<img src=\"${item.imageFileName}\" style=\"width: 100%; height: auto; display: block; margin: 0 auto;\" alt=\"${escapeHtml(item.title)}\"/>")
                    writer.println("</div>")
                }
            }
            writer.println("</body></html>")
        }

        // Package into standard EPUB 2.0
        val epubFile = File(destDir, "book.epub")
        try {
            val meta = extractMetadata(path).getOrNull()
            val title = meta?.title?.ifEmpty { File(path).nameWithoutExtension } ?: File(path).nameWithoutExtension
            val author = meta?.author ?: ""

            val epubChapters = mutableListOf<br.com.ebook.util.EpubPackager.ChapterItem>()
            var currentChapTitle = title
            var currentChapBuilder = StringBuilder()
            var currentChapFileName = "chap_0.xhtml"
            var currentAnchorId: String? = null
            var consecutiveEmpty = 0

            for ((idx, line) in lines.withIndex()) {
                val boundary = boundaryMap[idx]
                if (boundary != null) {
                    if (currentChapBuilder.isNotEmpty()) {
                        epubChapters.add(
                            br.com.ebook.util.EpubPackager.ChapterItem(
                                title = currentChapTitle,
                                contentHtml = currentChapBuilder.toString(),
                                fileName = currentChapFileName,
                                anchorId = currentAnchorId
                            )
                        )
                    }
                    currentChapTitle = boundary.title
                    currentChapFileName = "chap_${boundary.chapterNumber}.xhtml"
                    currentAnchorId = "txtz_chap_${boundary.chapterNumber}"
                    currentChapBuilder = StringBuilder()
                    currentChapBuilder.append("<h2>${escapeHtml(line)}</h2>\n")
                    consecutiveEmpty = 0
                } else if (line.isEmpty()) {
                    consecutiveEmpty++
                } else {
                    if (consecutiveEmpty >= 3) {
                        currentChapBuilder.append("<p class=\"br-gap\">&#160;</p>\n")
                    }
                    consecutiveEmpty = 0
                    currentChapBuilder.append("<p>${escapeHtml(line)}</p>\n")
                }
            }
            if (currentChapBuilder.isNotEmpty()) {
                epubChapters.add(
                    br.com.ebook.util.EpubPackager.ChapterItem(
                        title = currentChapTitle,
                        contentHtml = currentChapBuilder.toString(),
                        fileName = currentChapFileName,
                        anchorId = currentAnchorId
                    )
                )
            }

            br.com.ebook.util.EpubPackager.createEpub(
                outputFile = epubFile,
                title = title,
                author = author,
                coverBytes = coverBytes,
                coverFileName = coverFileName ?: "cover.jpg",
                galleryItems = galleryItems,
                chapters = epubChapters,
                resourceFiles = resourceBytes
            )
            LOGGER.info("TXTZ successfully packaged to EPUB: {}", epubFile.absolutePath)
        } catch (e: Exception) {
            LOGGER.error("Error packaging TXTZ to EPUB: {}", e.message, e)
        }

        if (epubFile.exists()) {
            BookContent.EpubFile(epubFile.absolutePath)
        } else {
            BookContent.HtmlFile(outHtmlFile.absolutePath)
        }
    }

    fun extractOutline(path: String): List<OutlineLink> {
        val list = mutableListOf<OutlineLink>()
        try {
            var textContent: String? = null
            var coverFileName: String? = null
            val imageMap = mutableMapOf<String, ByteArray>()
            if (path.endsWith(".txtz", ignoreCase = true)) {
                ZipFile(path).use { zip ->
                    val opfEntry = zip.getEntry("metadata.opf")
                    if (opfEntry != null) {
                        try {
                            zip.getInputStream(opfEntry).use { stream ->
                                val doc = Jsoup.parse(stream, "UTF-8", "", Parser.xmlParser())
                                val coverEl = doc.select("cover-relpath-from-base, meta[name=cover-relpath-from-base]").firstOrNull()
                                val coverMeta = if (coverEl?.tagName() == "meta") coverEl.attr("content").trim() else coverEl?.text()?.trim()
                                if (!coverMeta.isNullOrEmpty()) {
                                    coverFileName = coverMeta
                                }
                            }
                        } catch (ignored: Exception) {}
                    }

                    val entry = zip.getEntry("index.txt") ?: zip.entries().asSequence().firstOrNull { it.name.endsWith(".txt") }
                    if (entry != null) {
                        val bytes = zip.getInputStream(entry).use { it.readBytes() }
                        textContent = decodeTextBytes(bytes)
                    }

                    for (e in zip.entries().asSequence()) {
                        val n = e.name.lowercase()
                        if (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp")) {
                            imageMap[e.name] = zip.getInputStream(e).use { it.readBytes() }
                        }
                    }
                }
            }

            if (coverFileName == null) {
                coverFileName = imageMap.keys.firstOrNull { it.lowercase().startsWith("cover.") }
                    ?: imageMap.keys.firstOrNull()
            }

            val coverBytes = if (coverFileName != null) imageMap[coverFileName!!] else null
            val hasCover = coverBytes != null && coverBytes.isNotEmpty()

            val galleryImages = imageMap.filter { (name, bytes) ->
                val isCoverDuplicate = name.equals(coverFileName, ignoreCase = true) ||
                        File(name).nameWithoutExtension.equals("cover", ignoreCase = true) ||
                        (hasCover && coverBytes.contentEquals(bytes))
                !isCoverDuplicate
            }.keys.toList()

            if (textContent.isNullOrBlank()) return emptyList()

            val rawLines = textContent!!.replace("\r", "").split('\n')
            val lines = rawLines.map { it.trim() }
            val boundaries = findChapterBoundaries(lines)
            for (b in boundaries) {
                list.add(OutlineLink(b.title, "chap_${b.chapterNumber}.xhtml#txtz_chap_${b.chapterNumber}", 0))
            }
            for ((index, imgName) in galleryImages.withIndex()) {
                val imgTitle = File(imgName).nameWithoutExtension.ifBlank { (index + 1).toString() }
                list.add(OutlineLink("Galeria - $imgTitle", "gallery_$index.xhtml", 0))
            }
        } catch (e: Exception) {
            LOGGER.warn("Error extracting outline from TXTZ {}: {}", path, e.message)
        }
        return list
    }
}
