package br.com.ebook.extractor

import br.com.ebook.core.BookContent
import br.com.ebook.core.BookExtractor
import br.com.ebook.core.BookMetadata
import org.ebookdroid.droids.djvu.codec.DjvuContext
import org.slf4j.LoggerFactory
import java.io.File

object DjvuBookExtractor : BookExtractor {
    private val LOGGER = LoggerFactory.getLogger(DjvuBookExtractor::class.java)

    override val supportedFormats: Set<String> = setOf("djvu")

    override suspend fun extractMetadata(path: String): Result<BookMetadata> = runCatching {
        val codecContext = DjvuContext()
        var title = ""
        var author = ""
        try {
            val openDocument = codecContext.openDocument(path, "")
            if (openDocument != null) {
                title = openDocument.bookTitle ?: ""
                author = openDocument.bookAuthor ?: ""
                openDocument.recycle()
            }
        } catch (e: Exception) {
            LOGGER.error("Error get DJVU metadata information: {}", e.message, e)
        } finally {
            codecContext.recycle()
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
        BookContent.EpubFile(path) // Retorna como arquivo original
    }
}
