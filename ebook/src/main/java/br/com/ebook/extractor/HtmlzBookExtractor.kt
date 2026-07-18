package br.com.ebook.extractor

import br.com.ebook.core.BookContent
import br.com.ebook.core.BookExtractor
import br.com.ebook.core.BookMetadata
import br.com.ebook.foobnix.android.utils.TxtUtils
import br.com.ebook.foobnix.ext.XmlParser
import br.com.ebook.util.IOUtils.copyTo
import org.xmlpull.v1.XmlPullParser
import org.slf4j.LoggerFactory
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

object HtmlzBookExtractor : BookExtractor {
    private val LOGGER = LoggerFactory.getLogger(HtmlzBookExtractor::class.java)

    override val supportedFormats: Set<String> = setOf("htmlz")

    override suspend fun extractMetadata(path: String): Result<BookMetadata> = runCatching {
        var title = ""
        var author = ""
        try {
            ZipFile(path).use { zip ->
                val entry = zip.getEntry("metadata.opf")
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
            LOGGER.error("Error reading HTMLZ metadata: {}", e.message, e)
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
        ""
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

        BookContent.HtmlFile(indexHtml.absolutePath)
    }
}
