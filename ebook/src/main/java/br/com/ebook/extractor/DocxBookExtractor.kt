package br.com.ebook.extractor

import br.com.ebook.core.*
import br.com.ebook.foobnix.ext.XmlParser
import br.com.ebook.util.IOUtils.copyTo
import org.xmlpull.v1.XmlPullParser
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
        // DOCX files usually don't have standard cover images like EPUBs
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
        ZipFile(path).use { zip ->
            val entry = zip.getEntry("word/document.xml") ?: throw IllegalArgumentException("word/document.xml missing in DOCX")
            zip.getInputStream(entry).use { stream ->
                val xpp = XmlParser.buildPullParser()
                xpp.setInput(stream, "UTF-8")
                
                val htmlBuilder = StringBuilder()
                htmlBuilder.append("<html><head><meta charset=\"UTF-8\"/></head><body>")

                var eventType = xpp.eventType
                var inParagraph = false
                var isHeading = false
                val pTextBuilder = StringBuilder()

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        val name = xpp.name
                        if (name == "p") {
                            inParagraph = true
                            isHeading = false
                            pTextBuilder.setLength(0)
                        } else if (name == "pStyle") {
                            val styleVal = xpp.getAttributeValue(null, "val")
                            if (styleVal != null && styleVal.startsWith("Heading", ignoreCase = true)) {
                                isHeading = true
                            }
                        }
                    } else if (eventType == XmlPullParser.TEXT) {
                        if (inParagraph) {
                            pTextBuilder.append(xpp.text)
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        val name = xpp.name
                        if (name == "p") {
                            inParagraph = false
                            val paragraphText = pTextBuilder.toString().trim()
                            if (paragraphText.isNotEmpty()) {
                                if (isHeading) {
                                    htmlBuilder.append("<h2>").append(paragraphText).append("</h2>")
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
}
