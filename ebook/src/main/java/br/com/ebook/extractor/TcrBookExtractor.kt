package br.com.ebook.extractor

import br.com.ebook.core.BookContent
import br.com.ebook.core.BookExtractor
import br.com.ebook.core.BookMetadata
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.IOException

object TcrBookExtractor : BookExtractor {
    override val supportedFormats: Set<String> = setOf("tcr")

    override suspend fun extractMetadata(path: String): Result<BookMetadata> = runCatching {
        BookMetadata(
            title = File(path).nameWithoutExtension,
            author = "",
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
        val file = File(path)
        val data = file.readBytes()
        if (data.size < 9) {
            throw IOException("Arquivo TCR muito pequeno ou corrompido.")
        }
        
        var offset = 9
        val dictionary = Array(256) { ByteArray(0) }
        for (i in 0 until 256) {
            if (offset >= data.size) break
            val len = data[offset++].toInt() and 0xFF
            if (offset + len <= data.size) {
                val valBytes = ByteArray(len)
                System.arraycopy(data, offset, valBytes, 0, len)
                dictionary[i] = valBytes
                offset += len
            } else {
                break
            }
        }
        
        val decompressedStream = java.io.ByteArrayOutputStream(data.size * 2)
        while (offset < data.size) {
            val byteVal = data[offset++].toInt() and 0xFF
            decompressedStream.write(dictionary[byteVal])
        }
        
        val decompressedText = decompressedStream.toString("UTF-8")
        
        val hashCode = path.hashCode().toString()
        val outHtmlFile = File(outputDir, "tcr-converted-$hashCode.html")
        PrintWriter(BufferedWriter(FileWriter(outHtmlFile))).use { writer ->
            writer.println("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>p,p+p{margin:0;}</style></head><body>")
            decompressedText.split('\n').forEach { line ->
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
    }
}
