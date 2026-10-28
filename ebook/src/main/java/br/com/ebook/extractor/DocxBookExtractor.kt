package br.com.ebook.extractor

import br.com.ebook.core.BookContent
import br.com.ebook.core.BookExtractor
import br.com.ebook.core.BookMetadata
import br.com.ebook.foobnix.ext.XmlParser
import br.com.ebook.util.IOUtils.copyTo
import org.slf4j.LoggerFactory
import org.xmlpull.v1.XmlPullParser
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
                        val xpp = XmlParser.buildPullParser()
                        xpp.setInput(stream, "UTF-8")
                        var eventType = xpp.eventType
                        var currentTag = ""
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            if (eventType == XmlPullParser.START_TAG) {
                                currentTag = xpp.name
                            } else if (eventType == XmlPullParser.TEXT) {
                                when (currentTag) {
                                    "dc:title", "title" -> title = xpp.text.trim()
                                    "dc:creator", "creator" -> author = xpp.text.trim()
                                }
                            } else if (eventType == XmlPullParser.END_TAG) {
                                currentTag = ""
                            }
                            eventType = xpp.next()
                        }
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

    override suspend fun extractCover(path: String): Result<ByteArray?> = runCatching {
        null
    }

    override suspend fun extractOverview(path: String): Result<String> = runCatching {
        ""
    }

    override suspend fun extractFooterNotes(path: String): Result<Map<String, String>> = runCatching {
        emptyMap()
    }

    override suspend fun extractContent(path: String, outputDir: String): Result<BookContent> = runCatching {
        val outHtmlFile = File(outputDir, "docx-converted.html")
        val relations = mutableMapOf<String, String>()

        ZipFile(path).use { zip ->
            // 1. Read relationships to map rId to image target paths
            val relsEntry = zip.getEntry("word/_rels/document.xml.rels")
            if (relsEntry != null) {
                try {
                    zip.getInputStream(relsEntry).use { stream ->
                        val xpp = XmlParser.buildPullParser()
                        xpp.setInput(stream, "UTF-8")
                        var eventType = xpp.eventType
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            if (eventType == XmlPullParser.START_TAG && (xpp.name == "Relationship" || xpp.name.endsWith(":Relationship"))) {
                                val id = xpp.getAttributeValue(null, "Id")
                                val target = xpp.getAttributeValue(null, "Target")
                                val type = xpp.getAttributeValue(null, "Type")
                                if (id != null && target != null && type != null && type.contains("image")) {
                                    relations[id] = target
                                }
                            }
                            eventType = xpp.next()
                        }
                    }
                } catch (e: Exception) {
                    LOGGER.error("Error reading DOCX relationships: {}", e.message, e)
                }
            }

            // 2. Extract referenced media files
            relations.values.forEach { target ->
                val zipPath = "word/$target"
                val entry = zip.getEntry(zipPath)
                if (entry != null) {
                    val outFile = File(outputDir, target)
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
                val xpp = XmlParser.buildPullParser()
                xpp.setInput(stream, "UTF-8")
                
                val htmlBuilder = StringBuilder()
                htmlBuilder.append("<html><head><meta charset=\"UTF-8\"/><style>p { margin: 1em 0; line-height: 1.5; } img { max-width: 100%; height: auto; display: block; margin: 1em auto; }</style></head><body>")

                var eventType = xpp.eventType
                var inParagraph = false
                var isHeading = false
                var headingCount = 0
                val pTextBuilder = StringBuilder()

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        val name = xpp.name
                        if (name == "bookmarkStart" || name == "w:bookmarkStart") {
                            val bName = xpp.getAttributeValue(null, "name") ?: xpp.getAttributeValue(null, "w:name")
                            if (!bName.isNullOrBlank() && !bName.startsWith("_")) {
                                htmlBuilder.append("<a id=\"").append(bName).append("\"></a>")
                            }
                        } else if (name == "p" || name == "w:p") {
                            inParagraph = true
                            isHeading = false
                            pTextBuilder.setLength(0)
                        } else if (name == "pStyle" || name == "w:pStyle") {
                            val styleVal = xpp.getAttributeValue(null, "val") ?: xpp.getAttributeValue(null, "w:val")
                            if (styleVal != null && styleVal.startsWith("Heading", ignoreCase = true)) {
                                isHeading = true
                            }
                        } else if (name == "blip" || name == "a:blip") {
                            var embedId: String? = null
                            for (attrIdx in 0 until xpp.attributeCount) {
                                if (xpp.getAttributeName(attrIdx).endsWith("embed")) {
                                    embedId = xpp.getAttributeValue(attrIdx)
                                    break
                                }
                            }
                            if (embedId != null) {
                                val target = relations[embedId]
                                if (target != null) {
                                    htmlBuilder.append("<img src=\"").append(target).append("\"/>")
                                }
                            }
                        }
                    } else if (eventType == XmlPullParser.TEXT) {
                        if (inParagraph) {
                            pTextBuilder.append(xpp.text)
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        val name = xpp.name
                        if (name == "p" || name == "w:p") {
                            inParagraph = false
                            val paragraphText = pTextBuilder.toString().trim()
                            if (paragraphText.isNotEmpty()) {
                                if (isHeading) {
                                    headingCount++
                                    htmlBuilder.append("<h2 id=\"heading_").append(headingCount).append("\">").append(paragraphText).append("</h2>")
                                } else {
                                    htmlBuilder.append("<p>").append(paragraphText).append("</p>")
                                }
                            }
                        }
                    }
                    eventType = xpp.next()
                }

                htmlBuilder.append("</body></html>")
                outHtmlFile.parentFile?.mkdirs()
                outHtmlFile.writeText(htmlBuilder.toString())
            }
        }
        BookContent.HtmlFile(outHtmlFile.absolutePath)
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
                        val valAttr = style?.attr("w:val")?.ifEmpty { style?.attr("val") }
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
