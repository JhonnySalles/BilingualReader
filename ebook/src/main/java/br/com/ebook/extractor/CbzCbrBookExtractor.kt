package br.com.ebook.extractor

import br.com.ebook.core.BookContent
import br.com.ebook.core.BookExtractor
import br.com.ebook.core.BookMetadata
import br.com.ebook.foobnix.ext.CacheZipUtils
import br.com.ebook.foobnix.pdf.info.ExtUtils
import org.ebookdroid.BookType
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

object CbzCbrBookExtractor : BookExtractor {
    private val LOGGER = LoggerFactory.getLogger(CbzCbrBookExtractor::class.java)

    override val supportedFormats: Set<String> = setOf("cbz", "cbr", "zip")

    fun isZip(path: String): Boolean {
        try {
            val buffer = ByteArray(2)
            FileInputStream(path).use { fis ->
                fis.read(buffer)
            }
            val archType = String(buffer, StandardCharsets.UTF_8)
            if ("pk".equals(archType, ignoreCase = true)) {
                return true
            }
        } catch (e: Exception) {
            LOGGER.error("Error to detect zip: {}", e.message, e)
        }
        return false
    }

    override suspend fun extractCover(path: String): Result<ByteArray> = runCatching {
        val out = ByteArrayOutputStream()
        if (BookType.CBZ.`is`(path) || isZip(path)) {
            val file = File(path)
            ZipFile(file, StandardCharsets.UTF_8).use { zipFile ->
                val entries = zipFile.entries()
                val names = mutableListOf<String>()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name
                    if (ExtUtils.isImagePath(name)) {
                        names.add(name)
                    }
                }
                if (names.isNotEmpty()) {
                    names.sort()
                    val first = names[0]
                    val targetEntry = zipFile.getEntry(first)
                    if (targetEntry != null) {
                        zipFile.getInputStream(targetEntry).use { inputStream ->
                            CacheZipUtils.writeToStream(inputStream, out)
                        }
                    }
                }
            }
        }
        out.toByteArray()
    }

    override suspend fun extractMetadata(path: String): Result<BookMetadata> = runCatching {
        val file = File(path)
        BookMetadata(title = file.name, author = "")
    }

    override suspend fun extractContent(path: String, outputDir: String): Result<BookContent> {
        return Result.failure(UnsupportedOperationException("CBZ/CBR content extraction not supported"))
    }

    override suspend fun extractOverview(path: String): Result<String> {
        return Result.success("")
    }

    override suspend fun extractFooterNotes(path: String): Result<Map<String, String>> {
        return Result.success(emptyMap())
    }
}
