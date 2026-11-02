package br.com.ebook.extractor

import br.com.ebook.core.BookContent
import br.com.ebook.core.BookExtractor
import br.com.ebook.core.BookMetadata
import br.com.ebook.util.IOUtils.copyTo
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

object DocxBookExtractor : BookExtractor {
    private val LOGGER = LoggerFactory.getLogger(DocxBookExtractor::class.java)

    override val supportedFormats: Set<String> = setOf("docx")

    override suspend fun extractMetadata(path: String): Result<BookMetadata> = runCatching {
        var title = ""
        var author = ""
        try {
            ZipFile(path).use { zip ->
                val entry = zip.getEntry("docProps/core.xml")
                if (entry != null) {
                    zip.getInputStream(entry).use { stream ->
                        val doc = Jsoup.parse(stream, "UTF-8", "", Parser.xmlParser())
                        val t = doc.select("dc|title, title").text().trim()
                        if (t.isNotEmpty()) title = t
                        val a = doc.select("dc|creator, creator").text().trim()
                        if (a.isNotEmpty()) author = a
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error reading DOCX metadata: {}", e.message, e)
        }

        if (title.isBlank()) {
            title = File(path).nameWithoutExtension
        }

        BookMetadata(
            title = title,
            author = author,
            unzipPath = path
        )
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

    override suspend fun extractCover(path: String): Result<ByteArray?> = runCatching {
        var coverBytes: ByteArray? = null
        try {
            ZipFile(path).use { zip ->
                // 1. Ler relacionamentos de imagem do document.xml.rels
                val relations = mutableMapOf<String, String>()
                val relsEntry = zip.getEntry("word/_rels/document.xml.rels")
                if (relsEntry != null) {
                    zip.getInputStream(relsEntry).use { stream ->
                        val docRels = org.jsoup.Jsoup.parse(stream, "UTF-8", "", org.jsoup.parser.Parser.xmlParser())
                        for (rel in docRels.select("Relationship")) {
                            val id = rel.attr("Id")
                            val target = rel.attr("Target")
                            val type = rel.attr("Type")
                            if (id.isNotEmpty() && target.isNotEmpty() && type.contains("image", ignoreCase = true)) {
                                relations[id] = target
                            }
                        }
                    }
                }

                // 2. Encontrar primeiro relacionamento de imagem no word/document.xml
                var firstEmbedId: String? = null
                val docEntry = zip.getEntry("word/document.xml")
                if (docEntry != null) {
                    zip.getInputStream(docEntry).use { stream ->
                        val docXml = org.jsoup.Jsoup.parse(stream, "UTF-8", "", org.jsoup.parser.Parser.xmlParser())
                        val blip = docXml.select("blip, a|blip").firstOrNull()
                        if (blip != null) {
                            firstEmbedId = blip.attr("r:embed").ifEmpty { blip.attr("embed") }
                        }
                        if (firstEmbedId.isNullOrEmpty()) {
                            val imgData = docXml.select("imagedata, v|imagedata").firstOrNull()
                            if (imgData != null) {
                                firstEmbedId = imgData.attr("r:id").ifEmpty { imgData.attr("id") }
                            }
                        }
                    }
                }

                if (!firstEmbedId.isNullOrEmpty()) {
                    val target = relations[firstEmbedId]
                    if (target != null) {
                        val imgEntry = zip.getEntry("word/$target") ?: zip.getEntry(target)
                        if (imgEntry != null) {
                            zip.getInputStream(imgEntry).use { input ->
                                coverBytes = input.readBytes()
                            }
                        }
                    }
                }

                // Fallback 1: docProps/thumbnail.jpeg
                if (coverBytes == null) {
                    val thumbEntry = zip.getEntry("docProps/thumbnail.jpeg") ?: zip.getEntry("docProps/thumbnail.png")
                    if (thumbEntry != null) {
                        zip.getInputStream(thumbEntry).use { input ->
                            coverBytes = input.readBytes()
                        }
                    }
                }

                // Fallback 2: imagem com nome "cover" em word/media/
                if (coverBytes == null) {
                    val coverEntry = zip.entries().asSequence().firstOrNull {
                        val n = it.name.lowercase()
                        n.startsWith("word/media/cover.") || n.contains("/cover.")
                    }
                    if (coverEntry != null) {
                        zip.getInputStream(coverEntry).use { input ->
                            coverBytes = input.readBytes()
                        }
                    }
                }

                // Fallback 3: primeira imagem qualquer em word/media/
                if (coverBytes == null) {
                    val firstMedia = zip.entries().asSequence().firstOrNull {
                        val n = it.name.lowercase()
                        n.startsWith("word/media/") && (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png"))
                    }
                    if (firstMedia != null) {
                        zip.getInputStream(firstMedia).use { input ->
                            coverBytes = input.readBytes()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error extracting DOCX cover: {}", e.message, e)
        }
        coverBytes
    }

    override suspend fun extractOverview(path: String): Result<String> = runCatching {
        ""
    }

    override suspend fun extractFooterNotes(path: String): Result<Map<String, String>> = runCatching {
        emptyMap()
    }

    override suspend fun extractContent(path: String, outputDir: String): Result<BookContent> = runCatching {
        val hashCode = path.hashCode().toString()
        val destDir = File(outputDir, "docx_temp_$hashCode")
        destDir.mkdirs()
        val outHtmlFile = File(destDir, "index.html")
        val relations = mutableMapOf<String, String>()

        ZipFile(path).use { zip ->
            // 1. Read relationships to map rId to image target paths
            val relsEntry = zip.getEntry("word/_rels/document.xml.rels")
            if (relsEntry != null) {
                try {
                    zip.getInputStream(relsEntry).use { stream ->
                        val docRels = Jsoup.parse(stream, "UTF-8", "", Parser.xmlParser())
                        for (rel in docRels.select("Relationship")) {
                            val id = rel.attr("Id")
                            val target = rel.attr("Target")
                            val type = rel.attr("Type")
                            if (id.isNotEmpty() && target.isNotEmpty() && type.contains("image", ignoreCase = true)) {
                                relations[id] = target
                            }
                        }
                    }
                } catch (e: Exception) {
                    LOGGER.error("Error reading DOCX relationships: {}", e.message, e)
                }
            }

            // 2. Extract referenced media files
            relations.values.forEach { target ->
                val zipPath = "word/$target"
                val entry = zip.getEntry(zipPath) ?: zip.getEntry(target)
                if (entry != null) {
                    val outFile = File(destDir, target)
                    outFile.parentFile?.mkdirs()
                    try {
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(outFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (e: Exception) {
                        LOGGER.error("Error extracting media file $zipPath: {}", e.message, e)
                    }
                }
            }

            // 3. Extract XML content and render paragraphs/images
            val entry = zip.getEntry("word/document.xml") ?: throw IllegalArgumentException("word/document.xml missing in DOCX")
            zip.getInputStream(entry).use { stream ->
                val doc = Jsoup.parse(stream, "UTF-8", "", Parser.xmlParser())
                val htmlBuilder = StringBuilder()
                htmlBuilder.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>body{margin:0;padding:0;} p{margin:1em 0;line-height:1.5;} h2{text-align:center;margin:1.5em 0;} img{width:100%;height:auto;display:block;margin:0 auto;} .bilingual-reader-cover-page{page-break-after:always;text-align:center;margin:0;padding:0;} .bilingual-reader-gallery-page{page-break-after:always;text-align:center;margin:0;padding:0;} .bilingual-reader-image-inline{text-align:center;margin:1.2em 0;}</style></head><body>")

                // Localiza a primeira imagem do documento para servir de capa dedicada na Página 1
                var coverTarget: String? = null
                val firstBlip = doc.select("blip, a|blip").firstOrNull()
                if (firstBlip != null) {
                    val rEmbed = firstBlip.attr("r:embed").ifEmpty { firstBlip.attr("embed") }
                    coverTarget = relations[rEmbed]
                }
                if (coverTarget == null) {
                    val firstImgData = doc.select("imagedata, v|imagedata").firstOrNull()
                    if (firstImgData != null) {
                        val rId = firstImgData.attr("r:id").ifEmpty { firstImgData.attr("id") }
                        coverTarget = relations[rId]
                    }
                }

                val hasCover = !coverTarget.isNullOrEmpty() && File(destDir, coverTarget).exists()
                if (hasCover) {
                    htmlBuilder.append("<div class=\"bilingual-reader-cover-page\" style=\"page-break-after: always; text-align: center; margin: 0; padding: 0;\">")
                    htmlBuilder.append("<img src=\"").append(coverTarget).append("\" style=\"width: 100%; height: auto; display: block; margin: 0 auto;\" alt=\"Cover\"/>")
                    htmlBuilder.append("</div>\n")
                }
                var headingCount = 0
                var coverAlreadySkipped = false
                val blocks = mutableListOf<br.com.ebook.util.SpineSegmenter.Block>()

                val resourceBytes = mutableMapOf<String, ByteArray>()
                relations.values.forEach { target ->
                    val f = File(destDir, target)
                    if (f.exists()) {
                        resourceBytes[target] = f.readBytes()
                    }
                }

                val coverBytes = if (hasCover && coverTarget != null) {
                    resourceBytes[coverTarget]
                } else null

                val paragraphs = doc.select("w|p, p")
                for (p in paragraphs) {
                    val hasBreak = p.select("w|pageBreakBefore, pageBreakBefore, w|br[w|type=page]").isNotEmpty()
                    if (hasBreak) {
                        blocks.add(br.com.ebook.util.SpineSegmenter.Block.Boundary())
                    }

                    // Bookmarks
                    for (bm in p.select("w|bookmarkStart, bookmarkStart")) {
                        val bName = bm.attr("w:name").ifEmpty { bm.attr("name") }
                        if (bName.isNotBlank() && !bName.startsWith("_")) {
                            blocks.add(br.com.ebook.util.SpineSegmenter.Block.HtmlText("<a id=\"$bName\"></a>", textLength = 0, anchorId = bName))
                        }
                    }

                    // Images: <a:blip r:embed="..."/> or <v:imagedata r:id="..."/>
                    val blips = p.select("blip, a|blip, imagedata, v|imagedata")
                    for (el in blips) {
                        val embedId = el.attr("r:embed").ifEmpty { el.attr("embed").ifEmpty { el.attr("r:id").ifEmpty { el.attr("id") } } }
                        if (embedId.isNotEmpty()) {
                            val target = relations[embedId]
                            if (target != null) {
                                if (hasCover && target == coverTarget && !coverAlreadySkipped) {
                                    coverAlreadySkipped = true
                                } else {
                                    val bytes = resourceBytes[target]
                                    val role = br.com.ebook.util.ImageSizeUtils.classify(bytes)
                                    blocks.add(br.com.ebook.util.SpineSegmenter.Block.Image(src = target, role = role))
                                }
                            }
                        }
                    }

                    // Paragraph text
                    val text = p.select("w|t, t").joinToString("") { it.text() }.trim()
                    val styleVal = p.select("w|pStyle, pStyle").firstOrNull()?.let {
                        it.attr("w:val").ifEmpty { it.attr("val") }
                    }
                    val isHeading = styleVal?.startsWith("Heading", ignoreCase = true) == true

                    if (text.isNotEmpty()) {
                        if (isHeading) {
                            headingCount++
                            blocks.add(br.com.ebook.util.SpineSegmenter.Block.HtmlText("<h2 id=\"heading_$headingCount\">${escapeHtml(text)}</h2>", textLength = text.length, anchorId = "heading_$headingCount"))
                        } else {
                            blocks.add(br.com.ebook.util.SpineSegmenter.Block.HtmlText("<p>${escapeHtml(text)}</p>", textLength = text.length))
                        }
                    }
                }

                // Package into standard EPUB 2.0 so MuPDF renders all images natively
                val epubFile = File(destDir, "book.epub")
                try {
                    val meta = extractMetadata(path).getOrNull()
                    val title = meta?.title?.ifEmpty { File(path).nameWithoutExtension } ?: File(path).nameWithoutExtension
                    val author = meta?.author ?: ""

                    val chapters = br.com.ebook.util.SpineSegmenter.segment(
                        blocks = blocks,
                        bookTitle = title,
                        minTextChars = 200
                    )

                    br.com.ebook.util.EpubPackager.createEpub(
                        outputFile = epubFile,
                        title = title,
                        author = author,
                        coverBytes = coverBytes,
                        coverFileName = coverTarget ?: "cover.jpeg",
                        galleryItems = emptyList(),
                        chapters = chapters,
                        resourceFiles = resourceBytes
                    )
                    LOGGER.info("DOCX successfully packaged to EPUB: {}", epubFile.absolutePath)
                } catch (e: Exception) {
                    LOGGER.error("Error packaging DOCX to EPUB: {}", e.message, e)
                }
            }
        }

        val epubFile = File(destDir, "book.epub")
        if (epubFile.exists()) {
            BookContent.EpubFile(epubFile.absolutePath)
        } else {
            BookContent.HtmlFile(outHtmlFile.absolutePath)
        }
    }

    fun extractOutline(path: String): List<org.ebookdroid.core.codec.OutlineLink> {
        val list = mutableListOf<org.ebookdroid.core.codec.OutlineLink>()
        try {
            ZipFile(path).use { zip ->
                val entry = zip.getEntry("word/document.xml") ?: return emptyList()
                zip.getInputStream(entry).use { stream ->
                    val doc = org.jsoup.Jsoup.parse(stream, "UTF-8", "", org.jsoup.parser.Parser.xmlParser())
                    val bookmarks = doc.select("w|bookmarkStart, bookmarkStart").mapNotNull {
                        val n = it.attr("w:name").ifEmpty { it.attr("name") }
                        if (n.isNotBlank()) n else null
                    }.toSet()

                    val hyperlinks = mutableListOf<Pair<String, String>>()
                    for (el in doc.select("w|hyperlink, hyperlink")) {
                        val anchor = el.attr("w:anchor").ifEmpty { el.attr("anchor") }
                        val text = el.select("w|t, t").text().trim()
                        if (anchor.isNotEmpty() && text.isNotEmpty()) {
                            hyperlinks.add(Pair(anchor, text))
                        }
                    }

                    val headings = mutableListOf<String>()
                    for (p in doc.select("w|p, p")) {
                        val style = p.select("w|pStyle, pStyle").firstOrNull()
                        val valAttr = style?.attr("w:val")?.ifEmpty { style.attr("val") }
                        if (valAttr != null && valAttr.startsWith("Heading", ignoreCase = true)) {
                            val text = p.select("w|t, t").text().trim()
                            if (text.isNotEmpty()) {
                                headings.add(text)
                            }
                        }
                    }

                    // 1. Estratégia de Hyperlinks de TOC apontando para Bookmarks
                    val seenAnchors = mutableSetOf<String>()
                    val hasTopAnchors = hyperlinks.any { it.first.startsWith("Top_of_") }
                    for (pair in hyperlinks) {
                        val anchor = pair.first
                        val text = pair.second
                        if (bookmarks.contains(anchor) && !seenAnchors.contains(anchor)) {
                            if (hasTopAnchors && !anchor.startsWith("Top_of_")) {
                                continue
                            }
                            val lower = text.lowercase(java.util.Locale.getDefault())
                            if (!lower.startsWith("http") && !lower.startsWith("www") &&
                                !lower.contains("yenpress") && !lower.contains("facebook") && !lower.contains("twitter")
                            ) {
                                seenAnchors.add(anchor)
                                list.add(org.ebookdroid.core.codec.OutlineLink(text, "#$anchor", 0))
                            }
                        }
                    }

                    // 2. Estratégia de Fallback: Headings de parágrafo caso não haja links de TOC
                    if (list.isEmpty() && headings.isNotEmpty()) {
                        var hIndex = 1
                        for (hText in headings) {
                            list.add(org.ebookdroid.core.codec.OutlineLink(hText, "#heading_$hIndex", 0))
                            hIndex++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error extracting DOCX outline: {}", e.message, e)
        }
        return list
    }
}
