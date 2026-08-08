package br.com.ebook.extractor

import br.com.ebook.core.BookContent
import br.com.ebook.core.BookExtractor
import br.com.ebook.core.BookMetadata
import br.com.ebook.core.DateParseUtils
import br.com.ebook.core.EbookSettings
import br.com.ebook.foobnix.android.utils.TxtUtils
import br.com.ebook.foobnix.mobi.parser.MobiParser
import br.com.ebook.foobnix.pdf.info.ExtUtils
import com.foobnix.libmobi.LibMobi
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

object MobiBookExtractor : BookExtractor {
    private val LOGGER = LoggerFactory.getLogger(MobiBookExtractor::class.java)

    override val supportedFormats: Set<String> = setOf("mobi", "azw", "azw3", "azw4", "pdb", "prc")

    override suspend fun extractOverview(path: String): Result<String> = runCatching {
        var info = ""
        val file = File(path)
        try {
            val raw = getMappedByteBuffer(file)
            val parser = MobiParser(raw)
            info = parser.description ?: ""
        } catch (e: Throwable) {
            LOGGER.error("Error to get book overview: {}", e.message, e)
        }
        info
    }

    override suspend fun extractMetadata(path: String): Result<BookMetadata> = runCatching {
        val file = File(path)
        val raw = getMappedByteBuffer(file)
        val parser = MobiParser(raw)
        
        var title = parser.title
        var author = parser.author
        val subject = parser.subject ?: ""
        val lang = parser.language ?: ""
        val isbn = parser.isbn ?: ""
        val publisher = parser.publisher ?: ""
        val release = parser.release

        if (title.isNullOrBlank()) {
            title = file.name
        }

        if (EbookSettings.isFirstSurname) {
            author = TxtUtils.replaceLastFirstName(author)
        }

        val releaseDate = DateParseUtils.parseFlexibleDate(release)

        BookMetadata(
            title = title,
            author = author ?: "",
            genre = subject,
            language = lang,
            isbn = isbn,
            publisher = publisher,
            releaseDate = releaseDate,
            unzipPath = path
        )
    }.onFailure { e ->
        br.com.ebook.util.IOUtils.reportException(e, "Error extracting metadata from mobi: $path")
    }

    override suspend fun extractCover(path: String): Result<ByteArray?> = runCatching {
        var coverBytes: ByteArray? = null
        val file = File(path)
        try {
            val raw = getMappedByteBuffer(file)
            val parser = MobiParser(raw)
            coverBytes = parser.getCoverOrThumb()
        } catch (e: Throwable) {
            LOGGER.error("Error to get book cover: {}", e.message, e)
        }
        coverBytes
    }

    private fun getMappedByteBuffer(file: File): java.nio.ByteBuffer {
        val raf = java.io.RandomAccessFile(file, "r")
        return raf.use { r ->
            r.channel.use { ch ->
                ch.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, ch.size())
            }
        }
    }

    override suspend fun extractFooterNotes(path: String): Result<Map<String, String>> = runCatching {
        // Notas de rodapé não são extraídas diretamente do arquivo MOBI bruto, 
        // mas sim após a conversão para EPUB intermediário.
        emptyMap()
    }

    override suspend fun extractContent(path: String, outputDir: String): Result<BookContent> = runCatching {
        // Converte o arquivo MOBI para EPUB usando a biblioteca nativa LibMobi
        val hashCode = path.hashCode().toString()
        val tempFile = ExtUtils.validNameFileCharacter(path, outputDir, "mobi_temp_$hashCode")
        val destPath = File(outputDir, hashCode).path
        
        try {
            if (path.endsWith(".pdb", ignoreCase = true)) {
                throw IOException("PDB format is not supported by LibMobi EPUB conversion, using fallback")
            }
            val success = LibMobi.convertToEpub(tempFile, destPath)
            if (success > 0) {
                throw IOException("O formato PDB/MOBI não é suportado pelo LibMobi (código: $success)")
            }
            
            val result = File(outputDir, "$hashCode$hashCode.epub")
            if (result.exists()) {
                val coverBytes = extractCover(path).getOrNull()
                if (coverBytes != null && coverBytes.isNotEmpty()) {
                    br.com.ebook.util.EpubCoverInjector.injectCover(result.path, coverBytes)
                }
                BookContent.EpubFile(result.path)
            } else {
                throw IOException("Converted EPUB file not found")
            }
        } catch (e: Exception) {
            LOGGER.warn("LibMobi failed, trying PalmDOC fallback decompressor for: {}", path, e)
            try {
                val decompressedBytes = br.com.ebook.util.PalmDocDecompressor.decompress(File(path))
                val textContent = decompressedBytes.toString(charset("cp1252"))
                val outHtmlFile = File(outputDir, "$hashCode$hashCode.html")
                java.io.PrintWriter(java.io.BufferedWriter(java.io.FileWriter(outHtmlFile))).use { writer ->
                    writer.println("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>p,p+p{margin:0;}</style></head><body>")
                    textContent.split('\n').forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isEmpty()) {
                            writer.println("<br/>")
                        } else {
                            writer.println("<p>${android.text.TextUtils.htmlEncode(trimmed)}</p>")
                        }
                    }
                    writer.println("</body></html>")
                }
                BookContent.HtmlFile(outHtmlFile.path)
            } catch (fallbackEx: Exception) {
                LOGGER.error("PalmDOC fallback decompressor also failed for: {}", path, fallbackEx)
                throw e
            }
        }
    }.onFailure { e ->
        br.com.ebook.util.IOUtils.reportException(e, "Error extracting content from mobi: $path")
    }
}
