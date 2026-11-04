package br.com.ebook.extractor

import br.com.ebook.core.BookContent
import br.com.ebook.core.BookExtractor
import br.com.ebook.core.BookMetadata
import br.com.ebook.core.DateParseUtils
import br.com.ebook.core.MetadataParseUtils
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Inflater

object LrfBookExtractor : BookExtractor {
    private val LOGGER = LoggerFactory.getLogger(LrfBookExtractor::class.java)

    override val supportedFormats: Set<String> = setOf("lrf")

    override suspend fun extractMetadata(path: String): Result<BookMetadata> = runCatching {
        val file = File(path)
        var title = MetadataParseUtils.cleanFileTitle(file.nameWithoutExtension)
        var author = ""
        var publisher = ""
        var genre = ""
        var language = ""
        var releaseDate: java.time.LocalDate? = null
        var annotation = ""
        var series = ""
        var seriesIndex = 0

        try {
            RandomAccessFile(file, "r").use { raf ->
                if (raf.length() > 0x60) {
                    raf.seek(0)
                    val magic = ByteArray(8)
                    raf.readFully(magic)
                    // Magic: "LRF\0" (UTF-16LE: 'L', 0, 'R', 0, 'F', 0, 0, 0)
                    if (magic[0] == 'L'.code.toByte() && magic[2] == 'R'.code.toByte() && magic[4] == 'F'.code.toByte()) {
                        // Offset 88 (0x58): início do bloco compactado Zlib do Info XML
                        raf.seek(0x58)
                        val maxRead = kotlin.math.min(raf.length() - 0x58, 64 * 1024L).toInt()
                        val compBytes = ByteArray(maxRead)
                        raf.readFully(compBytes)

                        val inflater = Inflater()
                        inflater.setInput(compBytes)
                        val out = ByteArrayOutputStream(64 * 1024)
                        val buf = ByteArray(4096)
                        while (!inflater.needsInput() && !inflater.finished()) {
                            val count = inflater.inflate(buf)
                            if (count > 0) out.write(buf, 0, count) else break
                        }
                        inflater.end()

                        val xmlStr = out.toString(Charsets.UTF_8.name())
                        if (xmlStr.isNotBlank()) {
                            val doc = Jsoup.parse(xmlStr, "", Parser.xmlParser())

                            val t = doc.select("Title").text().trim()
                            if (t.isNotEmpty()) title = t

                            val a = doc.select("Author").text().trim()
                            if (a.isNotEmpty()) author = MetadataParseUtils.normalizeAuthors(a)

                            val p = doc.select("Publisher").text().trim()
                            if (p.isNotEmpty()) publisher = p

                            val c = doc.select("Category").text().trim()
                            if (c.isNotEmpty()) genre = c

                            val l = doc.select("Language").text().trim()
                            if (l.isNotEmpty()) language = l

                            val d = doc.select("FreeText").text().trim()
                            if (d.isNotEmpty()) annotation = MetadataParseUtils.cleanHtmlAnnotation(d)

                            val dateStr = doc.select("CreationDate").text().trim()
                            if (dateStr.isNotEmpty()) releaseDate = DateParseUtils.parseFlexibleDate(dateStr)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error extracting LRF metadata from {}: {}", path, e.message, e)
        }

        val inferred = MetadataParseUtils.inferSeriesAndVolume(title)
        if (inferred.first.isNotEmpty()) {
            series = inferred.first
            seriesIndex = inferred.second
        }

        BookMetadata(
            title = title,
            author = author,
            series = series,
            seriesIndex = seriesIndex,
            genre = genre,
            language = language,
            publisher = publisher,
            releaseDate = releaseDate,
            annotation = annotation,
            unzipPath = path
        )
    }

    override suspend fun extractCover(path: String): Result<ByteArray?> = runCatching {
        null
    }

    override suspend fun extractOverview(path: String): Result<String> = runCatching {
        extractMetadata(path).getOrNull()?.annotation ?: ""
    }

    override suspend fun extractFooterNotes(path: String): Result<Map<String, String>> = runCatching {
        emptyMap()
    }

    override suspend fun extractContent(path: String, outputDir: String): Result<BookContent> = runCatching {
        throw UnsupportedOperationException("Visualização nativa de LRF ainda não suportada.")
    }
}

