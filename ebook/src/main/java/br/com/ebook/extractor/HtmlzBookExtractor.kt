package br.com.ebook.extractor

import br.com.ebook.core.BookContent
import br.com.ebook.core.BookExtractor
import br.com.ebook.core.BookMetadata
import br.com.ebook.core.MetadataParseUtils
import br.com.ebook.util.IOUtils.copyTo
import org.ebookdroid.core.codec.OutlineLink
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

object HtmlzBookExtractor : BookExtractor {
    private val LOGGER = LoggerFactory.getLogger(HtmlzBookExtractor::class.java)

    override val supportedFormats: Set<String> = setOf("htmlz")

    override suspend fun extractMetadata(path: String): Result<BookMetadata> = runCatching {
        var metadata = BookMetadata(
            title = MetadataParseUtils.cleanFileTitle(File(path).nameWithoutExtension),
            author = "",
            unzipPath = path
        )

        try {
            ZipFile(path).use { zip ->
                val entry = zip.getEntry("metadata.opf")
                if (entry != null) {
                    val opfContent = zip.getInputStream(entry).use { it.bufferedReader(Charsets.UTF_8).readText() }
                    val parsed = MetadataParseUtils.parseOpfMetadata(opfContent)
                    metadata = parsed.copy(unzipPath = path)
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error reading HTMLZ metadata: {}", e.message, e)
        }

        if (metadata.title.isBlank()) {
            metadata = metadata.copy(title = MetadataParseUtils.cleanFileTitle(File(path).nameWithoutExtension))
        }

        metadata
    }

    override suspend fun extractCover(path: String): Result<ByteArray?> = runCatching {
        var coverBytes: ByteArray? = null
        try {
            ZipFile(path).use { zip ->
                val entry = zip.getEntry("cover.jpg") ?: zip.getEntry("cover.png") ?: zip.getEntry("cover.jpeg")
                if (entry != null) {
                    zip.getInputStream(entry).use { input ->
                        coverBytes = input.readBytes()
                    }
                } else {
                    val entries = zip.entries().toList()
                    val firstImage = entries.firstOrNull {
                        val name = it.name.lowercase()
                        !it.isDirectory && (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".gif"))
                    }
                    if (firstImage != null) {
                        zip.getInputStream(firstImage).use { input ->
                            coverBytes = input.readBytes()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error extracting HTMLZ cover: {}", e.message, e)
        }
        coverBytes
    }

    override suspend fun extractOverview(path: String): Result<String> = runCatching {
        var description = ""
        try {
            ZipFile(path).use { zip ->
                val entry = zip.getEntry("metadata.opf")
                if (entry != null) {
                    val opfContent = zip.getInputStream(entry).use { it.bufferedReader(Charsets.UTF_8).readText() }
                    description = MetadataParseUtils.parseOpfMetadata(opfContent).annotation
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error extracting HTMLZ overview: {}", e.message, e)
        }
        description.trim()
    }

    override suspend fun extractFooterNotes(path: String): Result<Map<String, String>> = runCatching {
        emptyMap()
    }

    override suspend fun extractContent(path: String, outputDir: String): Result<BookContent> = runCatching {
        val hashCode = path.hashCode().toString()
        val destDir = File(outputDir, "htmlz_temp_$hashCode")
        destDir.mkdirs()

        ZipFile(path).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val outFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(outFile).use { output ->
                            BufferedOutputStream(output).use { bos ->
                                input.copyTo(bos)
                            }
                        }
                    }
                }
            }
        }

        val indexHtml = File(destDir, "index.html")
        if (!indexHtml.exists()) {
            throw java.io.IOException("index.html missing in HTMLZ archive")
        }

        // Package into standard EPUB 2.0
        val epubFile = File(destDir, "book.epub")
        try {
            val meta = extractMetadata(path).getOrNull()
            val title = meta?.title?.ifEmpty { File(path).nameWithoutExtension } ?: File(path).nameWithoutExtension
            val author = meta?.author ?: ""

            val resourceBytes = mutableMapOf<String, ByteArray>()
            destDir.walkTopDown().filter { it.isFile }.forEach { f ->
                val rel = f.relativeTo(destDir).path.replace('\\', '/')
                val n = rel.lowercase()
                if (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".gif") || n.endsWith(".webp") || n.endsWith(".css")) {
                    resourceBytes[rel] = f.readBytes()
                }
            }

            val coverFile = destDir.listFiles { _, name ->
                val n = name.lowercase()
                n.startsWith("cover.") && (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp"))
            }?.firstOrNull()

            val coverBytes = if (coverFile != null && coverFile.exists()) coverFile.readBytes() else null
            val coverFileName = coverFile?.name ?: "cover.jpg"

            val htmlText = indexHtml.readText(Charsets.UTF_8)
            val doc = org.jsoup.Jsoup.parse(htmlText, "UTF-8")

            // Segment content into spine items using SpineSegmenter
            val hasCustomStyle = resourceBytes.keys.any { it.trimStart('/').equals("style.css", ignoreCase = true) }
            val cssLinks = if (hasCustomStyle) listOf("style.css") else emptyList()

            val chapters = br.com.ebook.util.SpineSegmenter.segmentHtmlDocument(
                doc = doc,
                resourceBytes = resourceBytes,
                bookTitle = title,
                coverFileNameToSkip = null,
                defaultCssLinks = cssLinks
            )

            br.com.ebook.util.EpubPackager.createEpub(
                outputFile = epubFile,
                title = title,
                author = author,
                coverBytes = coverBytes,
                coverFileName = coverFileName,
                galleryItems = emptyList(),
                chapters = chapters,
                resourceFiles = resourceBytes
            )
            LOGGER.info("HTMLZ successfully packaged to EPUB: {}", epubFile.absolutePath)
        } catch (e: Exception) {
            LOGGER.error("Error packaging HTMLZ to EPUB: {}", e.message, e)
        }

        if (epubFile.exists()) {
            BookContent.EpubFile(epubFile.absolutePath)
        } else {
            BookContent.HtmlFile(indexHtml.absolutePath)
        }
    }

    fun extractOutline(path: String): List<OutlineLink> {
        val list = mutableListOf<OutlineLink>()
        try {
            val file = File(path)
            if (!file.exists()) return emptyList()

            var htmlContent: String? = null
            if (path.endsWith(".htmlz", ignoreCase = true)) {
                ZipFile(path).use { zip ->
                    val entry = zip.getEntry("index.html") ?: return emptyList()
                    zip.getInputStream(entry).use { stream ->
                        htmlContent = stream.bufferedReader(Charsets.UTF_8).readText()
                    }
                }
            } else if (file.isFile && file.name.equals("index.html", ignoreCase = true)) {
                htmlContent = file.readText(Charsets.UTF_8)
            }

            val content = htmlContent ?: return emptyList()
            if (content.isBlank()) return emptyList()

            val doc = Jsoup.parse(content)

            // 1. Procura por links de TOC dedicados (ex: .toc-chapter a, .toc a, nav a)
            var tocElements = doc.select(".toc-chapter a[href^=#], .toc a[href^=#], nav a[href^=#]")
            if (tocElements.isEmpty()) {
                // Fallback: todos os links com âncora local href="#..."
                tocElements = doc.select("a[href^=#]")
            }

            val seenLinks = mutableSetOf<String>()
            val seenTitles = mutableSetOf<String>()

            for (a in tocElements) {
                val href = a.attr("href").trim()
                val text = a.text().trim()
                if (href.length > 1 && text.isNotEmpty()) {
                    val anchorId = href.removePrefix("#")
                    // Valida se a âncora realmente existe no documento
                    val targetExists = doc.getElementById(anchorId) != null ||
                            doc.select("a[name=$anchorId], section[id=$anchorId], div[id=$anchorId]").isNotEmpty()
                    
                    if (targetExists && !seenLinks.contains(href) && !seenTitles.contains(text.lowercase())) {
                        seenLinks.add(href)
                        seenTitles.add(text.lowercase())
                        list.add(OutlineLink(text, href, 0))
                    }
                }
            }

            // 2. Se nenhum link de TOC foi encontrado, busca cabeçalhos (h1, h2, h3) com id ou âncora interna
            if (list.isEmpty()) {
                val headings = doc.select("h1, h2, h3, h4")
                for (h in headings) {
                    val text = h.text().trim()
                    if (text.isNotEmpty()) {
                        val anchorId = h.id().ifBlank {
                            h.select("a[id]").attr("id").ifBlank {
                                h.select("a[name]").attr("name")
                            }
                        }
                        if (anchorId.isNotBlank()) {
                            val link = "#$anchorId"
                            if (!seenLinks.contains(link)) {
                                seenLinks.add(link)
                                list.add(OutlineLink(text, link, 0))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.warn("Error extracting outline from HTMLZ {}: {}", path, e.message)
        }
        return list
    }
}
