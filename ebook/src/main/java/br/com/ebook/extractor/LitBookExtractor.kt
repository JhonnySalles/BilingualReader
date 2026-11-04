package br.com.ebook.extractor

import br.com.ebook.core.BookContent
import br.com.ebook.core.BookExtractor
import br.com.ebook.core.BookMetadata
import br.com.ebook.core.DateParseUtils
import br.com.ebook.core.MetadataParseUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.RandomAccessFile

object LitBookExtractor : BookExtractor {
    private val LOGGER = LoggerFactory.getLogger(LitBookExtractor::class.java)

    override val supportedFormats: Set<String> = setOf("lit")

    override suspend fun extractMetadata(path: String): Result<BookMetadata> = runCatching {
        val file = File(path)
        var title = MetadataParseUtils.cleanFileTitle(file.nameWithoutExtension)
        var author = ""
        var publisher = ""
        var genre = ""
        var language = ""
        var isbn = ""
        var releaseDate: java.time.LocalDate? = null
        var annotation = ""
        var series = ""
        var seriesIndex = 0

        try {
            RandomAccessFile(file, "r").use { raf ->
                val len = raf.length()
                // O bloco /meta fica nos últimos 1MB do arquivo
                val scanLen = kotlin.math.min(len, 1024 * 1024L).toInt()
                val bytes = ByteArray(scanLen)
                raf.seek(len - scanLen)
                raf.readFully(bytes)

                val content = String(bytes, Charsets.ISO_8859_1)
                val marker = "http://openebook.org/namespaces/oeb-package/1.0/"
                val idx = content.indexOf(marker)
                if (idx >= 0) {
                    val metaSection = content.substring(idx)

                    // Extrai série do Calibre
                    val seriesMatch = Regex("""\{http://calibre\.kovidgoyal\.net/2009/metadata\}series[\x00-\x20]+([^\x00\r\n]{2,100})""").find(metaSection)
                    if (seriesMatch != null) {
                        series = seriesMatch.groupValues[1].trim()
                    }

                    // Extrai série index
                    val seriesIdxMatch = Regex("""\{http://calibre\.kovidgoyal\.net/2009/metadata\}series_index[\x00-\x20]+([^\x00\r\n]{1,10})""").find(metaSection)
                    if (seriesIdxMatch != null) {
                        val raw = seriesIdxMatch.groupValues[1]
                        val digits = raw.filter { it.isDigit() }
                        if (digits.isNotEmpty()) {
                            seriesIndex = digits.toIntOrNull() ?: 0
                        }
                    }

                    // Extrai ISBN
                    val isbnMatch = Regex("""ISBN[\x00-\x20]+(97[89][0-9]{10}|[0-9]{10})""").find(metaSection)
                    if (isbnMatch != null) {
                        isbn = isbnMatch.groupValues[1].trim()
                    }

                    // Extrai data
                    val dateMatch = Regex("""(\d{4}-\d{2}-\d{2}[T\s]\d{2}:\d{2}:\d{2}[^\x00]*)""").find(metaSection)
                    if (dateMatch != null) {
                        releaseDate = DateParseUtils.parseFlexibleDate(dateMatch.groupValues[1])
                    }

                    // Extrai sinopse
                    val descMatch = Regex("""(<div>[\s\S]*?</div>)""").find(metaSection)
                    if (descMatch != null) {
                        annotation = MetadataParseUtils.cleanHtmlAnnotation(descMatch.groupValues[1])
                    }

                    // Extrai autores (Asato, Shirabii)
                    val authorMatches = Regex("""aut\x00([A-Za-z0-9À-ÿ\s'-]{2,50})\x00""").findAll(metaSection).map { it.groupValues[1].trim() }.toList()
                    if (authorMatches.isNotEmpty()) {
                        author = authorMatches.distinct().joinToString(", ")
                    }

                    // Extrai editora
                    val pubMatch = Regex("""Yen On|Yen Press|Publisher[\x00-\x20]+([^\x00\r\n]{2,50})""").find(metaSection)
                    if (pubMatch != null) {
                        publisher = if (pubMatch.value.contains("Yen On")) "Yen On" else pubMatch.groupValues[1].trim()
                    }

                    // Extrai idioma
                    val langMatch = Regex("""\x00(por|eng|jpn|pt|en|ja)\x00""").find(metaSection)
                    if (langMatch != null) {
                        language = langMatch.groupValues[1].trim()
                    }

                    // Extrai assuntos
                    val subjectMatches = Regex("""\x01\x18\x00([A-Za-z0-9À-ÿ\s-]{3,30})\x00""").findAll(metaSection).map { it.groupValues[1].trim() }.toList()
                    if (subjectMatches.isNotEmpty()) {
                        genre = subjectMatches.distinct().joinToString(", ")
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error reading LIT metadata: {}", e.message, e)
        }

        if (series.isEmpty()) {
            val inferred = MetadataParseUtils.inferSeriesAndVolume(title)
            if (inferred.first.isNotEmpty()) {
                series = inferred.first
                seriesIndex = inferred.second
            }
        }

        BookMetadata(
            title = title,
            author = author,
            series = series,
            seriesIndex = seriesIndex,
            genre = genre,
            language = language,
            isbn = isbn,
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
        throw UnsupportedOperationException("Visualização nativa de LIT ainda não suportada.")
    }
}

