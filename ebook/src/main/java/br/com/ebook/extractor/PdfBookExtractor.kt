package br.com.ebook.extractor

import br.com.ebook.core.BookContent
import br.com.ebook.core.BookExtractor
import br.com.ebook.core.BookMetadata
import br.com.ebook.core.DateParseUtils
import br.com.ebook.core.MetadataParseUtils
import org.ebookdroid.droids.mupdf.codec.PdfContext
import org.jsoup.Jsoup
import org.jsoup.parser.ParseSettings
import org.jsoup.parser.Parser
import org.slf4j.LoggerFactory
import java.io.File
import java.io.RandomAccessFile

object PdfBookExtractor : BookExtractor {
    private val LOGGER = LoggerFactory.getLogger(PdfBookExtractor::class.java)

    override val supportedFormats: Set<String> = setOf("pdf", "xps")

    override suspend fun extractMetadata(path: String): Result<BookMetadata> = runCatching {
        val file = File(path)
        var metadata = BookMetadata(
            title = MetadataParseUtils.cleanFileTitle(file.nameWithoutExtension),
            author = "",
            unzipPath = path
        )

        // 1. Tenta extrair pacote XMP RDF (<x:xmpmeta>...</x:xmpmeta>) de alto desempenho sem abrir o MuPDF
        val xmpXml = extractXmpFromPdf(file)
        if (!xmpXml.isNullOrBlank()) {
            val parsedXmp = parseXmpMetadata(xmpXml)
            if (parsedXmp.title.isNotBlank()) {
                metadata = parsedXmp.copy(unzipPath = path)
            }
        }

        // 2. Se título ou autor ainda estiverem vazios, tenta extrair do dicionário /Info
        if (metadata.title.isBlank() || metadata.author.isBlank()) {
            val infoMeta = extractInfoDictFromPdf(file)
            metadata = metadata.copy(
                title = if (metadata.title.isBlank()) infoMeta.title else metadata.title,
                author = if (metadata.author.isBlank()) infoMeta.author else metadata.author,
                genre = if (metadata.genre.isBlank()) infoMeta.genre else metadata.genre,
                releaseDate = metadata.releaseDate ?: infoMeta.releaseDate
            )
        }

        // 3. Fallback final: MuPDF Nativo
        if (metadata.title.isBlank() || metadata.author.isBlank()) {
            try {
                val codecContext = PdfContext()
                val openDocument = codecContext.openDocument(path, "")
                if (openDocument != null) {
                    val muTitle = openDocument.bookTitle ?: ""
                    val muAuthor = openDocument.bookAuthor ?: ""
                    openDocument.recycle()
                    metadata = metadata.copy(
                        title = if (metadata.title.isBlank() && muTitle.isNotBlank()) muTitle else metadata.title,
                        author = if (metadata.author.isBlank() && muAuthor.isNotBlank()) muAuthor else metadata.author
                    )
                }
            } catch (e: Exception) {
                LOGGER.warn("MuPDF native metadata fallback failed for {}: {}", path, e.message)
            }
        }

        if (metadata.title.isBlank()) {
            metadata = metadata.copy(title = MetadataParseUtils.cleanFileTitle(file.nameWithoutExtension))
        }

        if (metadata.series.isBlank()) {
            val inferred = MetadataParseUtils.inferSeriesAndVolume(metadata.title)
            if (inferred.first.isNotEmpty()) {
                metadata = metadata.copy(
                    series = inferred.first,
                    seriesIndex = if (metadata.seriesIndex == 0) inferred.second else metadata.seriesIndex
                )
            }
        }

        metadata
    }

    override suspend fun extractCover(path: String): Result<ByteArray?> = runCatching {
        // PDF não tem extração de imagem de capa direta como array de bytes bruto do arquivo
        // A capa do PDF é renderizada dinamicamente como imagem a partir da página 0.
        null
    }

    override suspend fun extractOverview(path: String): Result<String> = runCatching {
        val file = File(path)
        val xmpXml = extractXmpFromPdf(file)
        if (!xmpXml.isNullOrBlank()) {
            parseXmpMetadata(xmpXml).annotation
        } else {
            ""
        }
    }

    override suspend fun extractFooterNotes(path: String): Result<Map<String, String>> = runCatching {
        emptyMap()
    }

    override suspend fun extractContent(path: String, outputDir: String): Result<BookContent> = runCatching {
        BookContent.EpubFile(path)
    }

    private fun extractXmpFromPdf(file: File): String? {
        if (!file.exists()) return null
        val fileSize = file.length()
        if (fileSize == 0L) return null

        return try {
            RandomAccessFile(file, "r").use { raf ->
                val bufSize = 512 * 1024
                val overlap = 1024
                var offset = 0L
                val buf = ByteArray(bufSize)
                var fallbackCandidate: String? = null

                while (offset < fileSize) {
                    raf.seek(offset)
                    val readBytes = raf.read(buf)
                    if (readBytes <= 0) break
                    val chunkStr = String(buf, 0, readBytes, Charsets.UTF_8)
                    var searchPos = 0
                    while (true) {
                        val xmpStart = chunkStr.indexOf("<x:xmpmeta", searchPos)
                        if (xmpStart < 0) break
                        val xmpEnd = chunkStr.indexOf("</x:xmpmeta>", xmpStart)
                        if (xmpEnd >= 0) {
                            val candidate = chunkStr.substring(xmpStart, xmpEnd + 12)
                            if (candidate.contains("calibre", ignoreCase = true) ||
                                candidate.contains("dc:publisher", ignoreCase = true) ||
                                candidate.contains("isbn", ignoreCase = true)) {
                                return@use candidate
                            }
                            if (fallbackCandidate == null) {
                                fallbackCandidate = candidate
                            }
                            searchPos = xmpEnd + 12
                        } else {
                            break
                        }
                    }
                    if (offset + readBytes >= fileSize) break
                    offset += readBytes - overlap
                }
                fallbackCandidate
            }
        } catch (e: Exception) {
            LOGGER.warn("Error scanning PDF XMP: {}", e.message)
            null
        }
    }

    private fun parseXmpMetadata(xmpXml: String): BookMetadata {
        return try {
            val parser = Parser.xmlParser().settings(ParseSettings(false, false))
            val doc = Jsoup.parse(xmpXml, "", parser)

            val titleEl = doc.select("dc|title li, dc|title, title").firstOrNull()
            val title = titleEl?.text()?.trim() ?: ""

            val creators = doc.select("dc|creator li, dc|creator").map { it.text().trim() }.filter { it.isNotEmpty() }
            val author = if (creators.isNotEmpty()) {
                creators.joinToString(", ")
            } else {
                MetadataParseUtils.normalizeAuthors(doc.select("author").firstOrNull()?.text())
            }

            val seriesEl = doc.select("calibre|series, series").firstOrNull()
            var series = ""
            if (seriesEl != null) {
                val rVal = seriesEl.select("rdf|value, value").firstOrNull()?.text()?.trim()
                series = if (!rVal.isNullOrEmpty()) rVal else seriesEl.ownText().trim()
            }
            if (series.isBlank()) {
                series = doc.select("calibre|series li").text().trim()
            }
            val seriesIndexStr = doc.select("calibreSI|series_index, series_index").text().trim()
            var seriesIndex = seriesIndexStr.toFloatOrNull()?.toInt() ?: 0

            val publisher = doc.select("dc|publisher, publisher").firstOrNull()?.text()?.trim() ?: ""

            var isbn = ""
            val idEls = doc.select("xmp|identifier li, identifier li, dc|identifier li, dc|identifier")
            for (el in idEls) {
                val scheme = (el.attr("xmpidq:scheme").ifEmpty { el.attr("scheme") }).trim()
                if (scheme.equals("isbn", ignoreCase = true)) {
                    isbn = el.text().trim().replace(Regex("[^0-9Xx]"), "")
                    break
                }
            }
            if (isbn.isEmpty()) {
                for (el in doc.select("xmp|Identifier li, Identifier li")) {
                    val scheme = el.select("xmpidq|Scheme, Scheme").text().trim()
                    if (scheme.equals("isbn", ignoreCase = true)) {
                        val v = el.select("rdf|value, value").text().trim()
                        if (v.isNotEmpty()) {
                            isbn = v.replace(Regex("[^0-9Xx]"), "")
                            break
                        }
                    }
                }
            }
            if (isbn.isEmpty()) {
                val pdfxIsbn = doc.select("pdfx|isbn, prism|isbn").firstOrNull()?.text()?.trim()
                if (!pdfxIsbn.isNullOrEmpty()) {
                    isbn = pdfxIsbn.replace(Regex("[^0-9Xx]"), "")
                }
            }
            if (isbn.isEmpty()) {
                for (el in idEls) {
                    val digits = el.text().trim().replace(Regex("[^0-9Xx]"), "")
                    if ((digits.startsWith("978") || digits.startsWith("979")) && digits.length == 13) {
                        isbn = digits
                        break
                    }
                }
            }

            val dateStr = doc.select("dc|date, date").firstOrNull()?.text()?.trim()
            val releaseDate = DateParseUtils.parseFlexibleDate(dateStr)

            val language = doc.select("dc|language, language").firstOrNull()?.text()?.trim() ?: ""

            val subjects = doc.select("dc|subject li, dc|subject").map { it.text().trim() }.filter { it.isNotEmpty() }
            val genre = subjects.joinToString(", ")

            val descEl = doc.select("dc|description li, dc|description").firstOrNull()
            val annotation = if (descEl != null) MetadataParseUtils.cleanHtmlAnnotation(descEl.html().ifEmpty { descEl.text() }) else ""

            if (series.isEmpty() && title.isNotEmpty()) {
                val inferred = MetadataParseUtils.inferSeriesAndVolume(title)
                if (inferred.first.isNotEmpty()) {
                    series = inferred.first
                    if (seriesIndex == 0) seriesIndex = inferred.second
                }
            }

            BookMetadata(
                title = title,
                author = author,
                series = series,
                genre = genre,
                language = language,
                isbn = isbn,
                publisher = publisher,
                releaseDate = releaseDate,
                seriesIndex = seriesIndex,
                annotation = annotation
            )
        } catch (e: Exception) {
            BookMetadata("", "")
        }
    }

    private fun extractInfoDictFromPdf(file: File): BookMetadata {
        var title = ""
        var author = ""
        var genre = ""
        var releaseDate: java.time.LocalDate? = null

        try {
            val length = file.length()
            val tailLen = kotlin.math.min(length, 128 * 1024L).toInt()
            val tailBytes = ByteArray(tailLen)
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(length - tailLen)
                raf.readFully(tailBytes)
            }
            val tailStr = String(tailBytes, Charsets.ISO_8859_1)

            val titleMatch = Regex("""/Title\s*\(([^)]+)\)""").find(tailStr)
            if (titleMatch != null) title = titleMatch.groupValues[1].trim()

            val authorMatch = Regex("""/Author\s*\(([^)]+)\)""").find(tailStr)
            if (authorMatch != null) author = MetadataParseUtils.normalizeAuthors(authorMatch.groupValues[1])

            val kwMatch = Regex("""/Keywords\s*\(([^)]+)\)""").find(tailStr)
            if (kwMatch != null) genre = kwMatch.groupValues[1].trim()

            val dateMatch = Regex("""/CreationDate\s*\(([^)]+)\)""").find(tailStr)
            if (dateMatch != null) releaseDate = DateParseUtils.parseFlexibleDate(dateMatch.groupValues[1])
        } catch (e: Exception) {
            // Silencioso
        }

        return BookMetadata(
            title = title,
            author = author,
            genre = genre,
            releaseDate = releaseDate
        )
    }
}
