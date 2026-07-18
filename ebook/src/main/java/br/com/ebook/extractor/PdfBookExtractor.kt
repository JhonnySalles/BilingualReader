package br.com.ebook.extractor

import br.com.ebook.core.BookContent
import br.com.ebook.core.BookExtractor
import br.com.ebook.core.BookMetadata
import org.ebookdroid.droids.mupdf.codec.PdfContext
import org.slf4j.LoggerFactory
import java.io.File

object PdfBookExtractor : BookExtractor {
    private val LOGGER = LoggerFactory.getLogger(PdfBookExtractor::class.java)

    override val supportedFormats: Set<String> = setOf("pdf", "xps")

    override suspend fun extractMetadata(path: String): Result<BookMetadata> = runCatching {
        val codecContext = PdfContext()
        var title = ""
        var author = ""
        try {
            val openDocument = codecContext.openDocument(path, "")
            if (openDocument != null) {
                title = openDocument.bookTitle ?: ""
                author = openDocument.bookAuthor ?: ""
                openDocument.recycle()
            }
        } catch (e: RuntimeException) {
            LOGGER.error("Error get PDF metadata information: {}", e.message, e)
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
        // PDF não tem extração de imagem de capa direta como array de bytes bruto do arquivo
        // A capa do PDF é renderizada dinamicamente como imagem a partir da página 0.
        null
    }

    override suspend fun extractOverview(path: String): Result<String> = runCatching {
        ""
    }

    override suspend fun extractFooterNotes(path: String): Result<Map<String, String>> = runCatching {
        emptyMap()
    }

    override suspend fun extractContent(path: String, outputDir: String): Result<BookContent> = runCatching {
        // Arquivo PDF não é convertido para HTML intermediário
        // Ele é aberto e renderizado como documento PDF puro.
        BookContent.EpubFile(path) // Retorna como arquivo original
    }
}
