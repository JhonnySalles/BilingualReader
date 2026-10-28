package br.com.ebook.extractor

import br.com.ebook.core.BookContent
import br.com.ebook.core.BookExtractor
import br.com.ebook.core.BookMetadata
import br.com.ebook.foobnix.ext.XmlParser
import br.com.ebook.util.IOUtils.copyTo
import org.ebookdroid.core.codec.OutlineLink
import org.jsoup.Jsoup
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
        "^\\s*(cap[ií]tulo\\s+\\d+.*|chapter\\s+\\d+.*|ep[ií]logo.*|prologue.*|pr[oó]logo.*|afterword.*|[0-9IVXLCDM]+\\..*)\\s*$",
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
                val entry = zip.getEntry("cover.jpeg") ?: zip.getEntry("cover.jpg") ?: zip.getEntry("cover.png")
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

    override suspend fun extractContent(path: String, outputDir: String): Result<BookContent> = runCatching {
        val hashCode = path.hashCode().toString()
        val destDir = File(outputDir, "txtz_temp_$hashCode")
        destDir.mkdirs()

        var textContent = ""
        var coverFileName: String? = null

        ZipFile(path).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val nameLower = entry.name.lowercase()
                if (nameLower.endsWith(".txt") && (nameLower.contains("index") || textContent.isEmpty())) {
                    zip.getInputStream(entry).use { input ->
                        textContent = input.bufferedReader(Charsets.UTF_8).readText()
                    }
                } else if (nameLower.endsWith(".jpg") || nameLower.endsWith(".jpeg") || nameLower.endsWith(".png")) {
                    val outFile = File(destDir, entry.name)
                    outFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(outFile).use { output ->
                            BufferedOutputStream(output).use { bos ->
                                input.copyTo(bos)
                            }
                        }
                    }
                    if (nameLower.startsWith("cover.")) {
                        coverFileName = entry.name
                    }
                }
            }
        }

        val outHtmlFile = File(destDir, "index.html")
        java.io.PrintWriter(java.io.BufferedWriter(java.io.OutputStreamWriter(FileOutputStream(outHtmlFile), Charsets.UTF_8))).use { writer ->
            writer.println("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>p,p+p{margin:0;text-indent:1.5em;line-height:1.4;} h2{text-align:center;margin:1.5em 0;}</style></head><body>")

            if (coverFileName != null && File(destDir, coverFileName!!).exists()) {
                writer.println("<div style=\"text-align:center;\"><img src=\"$coverFileName\" style=\"max-width:100%;height:auto;\"/></div><br/>")
            }

            var chapterIndex = 0
            textContent.split('\n').forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty()) {
                    writer.println("<br/>")
                } else if (CHAPTER_PATTERN.matcher(line).matches()) {
                    chapterIndex++
                    writer.println("<h2 id=\"txtz_chap_$chapterIndex\"><a id=\"txtz_chap_$chapterIndex\"></a>${escapeHtml(line)}</h2>")
                } else {
                    writer.println("<p>${escapeHtml(line)}</p>")
                }
            }
            writer.println("</body></html>")
        }

        BookContent.HtmlFile(outHtmlFile.absolutePath)
    }

    fun extractOutline(path: String): List<OutlineLink> {
        val list = mutableListOf<OutlineLink>()
        try {
            var textContent: String? = null
            if (path.endsWith(".txtz", ignoreCase = true)) {
                ZipFile(path).use { zip ->
                    val entry = zip.getEntry("index.txt") ?: zip.entries().asSequence().firstOrNull { it.name.endsWith(".txt") }
                    if (entry != null) {
                        zip.getInputStream(entry).use { stream ->
                            textContent = stream.bufferedReader(Charsets.UTF_8).readText()
                        }
                    }
                }
            }

            if (textContent.isNullOrBlank()) return emptyList()

            var chapterIndex = 0
            textContent!!.split('\n').forEach { rawLine ->
                val line = rawLine.trim()
                if (CHAPTER_PATTERN.matcher(line).matches()) {
                    chapterIndex++
                    list.add(OutlineLink(line, "#txtz_chap_$chapterIndex", 0))
                }
            }
        } catch (e: Exception) {
            LOGGER.warn("Error extracting outline from TXTZ {}: {}", path, e.message)
        }
        return list
    }
}
