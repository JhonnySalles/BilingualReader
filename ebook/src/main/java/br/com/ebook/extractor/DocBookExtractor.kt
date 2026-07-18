package br.com.ebook.extractor

import br.com.ebook.core.*
import org.slf4j.LoggerFactory
import java.io.File

object DocBookExtractor : BookExtractor {
    private val LOGGER = LoggerFactory.getLogger(DocBookExtractor::class.java)

    override val supportedFormats: Set<String> = setOf("doc")

    override suspend fun extractMetadata(path: String): Result<BookMetadata> = runCatching {
        val file = File(path)
        val title = file.nameWithoutExtension
        BookMetadata(
            title = title,
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
        val outHtmlFile = File(outputDir, "doc-converted.html")
        val file = File(path)
        val bytes = file.readBytes()
        
        // 1. Scan for images (JPEG / PNG) in the binary stream
        val imageFiles = mutableListOf<String>()
        var offset = 0
        var imgCount = 0
        val n = bytes.size
        
        while (offset < n - 4) {
            // JPEG: FF D8 FF
            if (bytes[offset] == 0xFF.toByte() && bytes[offset+1] == 0xD8.toByte() && bytes[offset+2] == 0xFF.toByte()) {
                var end = offset + 3
                while (end < n - 1) {
                    if (bytes[end] == 0xFF.toByte() && bytes[end+1] == 0xD9.toByte()) {
                        break
                    }
                    end++
                }
                if (end < n - 1) {
                    val imgSize = end + 2 - offset
                    if (imgSize > 2048) { // Ignore tiny noise
                        val imgBytes = bytes.copyOfRange(offset, end + 2)
                        val imgName = "image_${imgCount++}.jpg"
                        File(outputDir, imgName).writeBytes(imgBytes)
                        imageFiles.add(imgName)
                        offset = end + 2
                        continue
                    }
                }
            }
            // PNG: 89 50 4E 47
            if (bytes[offset] == 0x89.toByte() && bytes[offset+1] == 0x50.toByte() && bytes[offset+2] == 0x4E.toByte() && bytes[offset+3] == 0x47.toByte()) {
                var end = offset + 4
                while (end < n - 8) {
                    if (bytes[end] == 0x49.toByte() && bytes[end+1] == 0x45.toByte() && bytes[end+2] == 0x4E.toByte() && bytes[end+3] == 0x44.toByte()) {
                        if (bytes[end+4] == 0xAE.toByte() && bytes[end+5] == 0x42.toByte() && bytes[end+6] == 0x60.toByte() && bytes[end+7] == 0x82.toByte()) {
                            break
                        }
                    }
                    end++
                }
                if (end < n - 8) {
                    val imgSize = end + 8 - offset
                    if (imgSize > 2048) {
                        val imgBytes = bytes.copyOfRange(offset, end + 8)
                        val imgName = "image_${imgCount++}.png"
                        File(outputDir, imgName).writeBytes(imgBytes)
                        imageFiles.add(imgName)
                        offset = end + 8
                        continue
                    }
                }
            }
            offset++
        }

        // 2. Parse text and embed images where the character \u0001 (0x01) occurs
        val htmlBuilder = StringBuilder()
        htmlBuilder.append("<html><head><meta charset=\"UTF-8\"/><style>p { margin: 1em 0; line-height: 1.5; } img { max-width: 100%; height: auto; display: block; margin: 1em auto; }</style></head><body>")

        var i = 0
        var embeddedImgCount = 0

        fun insertImageTag() {
            if (embeddedImgCount < imageFiles.size) {
                val imgName = imageFiles[embeddedImgCount++]
                htmlBuilder.append("<img src=\"").append(imgName).append("\"/>")
            }
        }

        while (i < n) {
            // Check for UTF-16LE characters (common in .doc files)
            if (i + 1 < n) {
                val b1 = bytes[i].toInt() and 0xFF
                val b2 = bytes[i + 1].toInt() and 0xFF
                
                if (b2 == 0 && (b1 in 32..126 || b1 == 10 || b1 == 13 || b1 == 9 || b1 == 1)) {
                    val start = i
                    var hasImageChar = false
                    while (i + 1 < n) {
                        val c1 = bytes[i].toInt() and 0xFF
                        val c2 = bytes[i + 1].toInt() and 0xFF
                        if (c2 == 0 && (c1 in 32..126 || c1 == 10 || c1 == 13 || c1 == 9 || c1 == 160 || c1 == 1)) {
                            if (c1 == 1) {
                                hasImageChar = true
                            }
                            i += 2
                        } else {
                            break
                        }
                    }
                    val len = i - start
                    if (len >= 8) {
                        val segment = String(bytes, start, len, Charsets.UTF_16LE).trim()
                        if (segment.isNotEmpty()) {
                            if (hasImageChar) {
                                htmlBuilder.append("<p>")
                                segment.forEach { char ->
                                    if (char == '\u0001') {
                                        htmlBuilder.append("</p>")
                                        insertImageTag()
                                        htmlBuilder.append("<p>")
                                    } else {
                                        htmlBuilder.append(android.text.TextUtils.htmlEncode(char.toString()))
                                    }
                                }
                                htmlBuilder.append("</p>")
                            } else {
                                htmlBuilder.append("<p>").append(android.text.TextUtils.htmlEncode(segment)).append("</p>")
                            }
                        }
                    }
                    continue
                }
            }

            // Check for ASCII/Windows-1252 printable chars
            val b = bytes[i].toInt() and 0xFF
            if (b in 32..126 || b == 10 || b == 13 || b == 9 || b == 1) {
                val start = i
                var hasImageChar = false
                while (i < n) {
                    val c = bytes[i].toInt() and 0xFF
                    if (c in 32..126 || c == 10 || c == 13 || c == 9 || c == 1) {
                        if (c == 1) {
                            hasImageChar = true
                        }
                        i++
                    } else {
                        break
                    }
                }
                val len = i - start
                if (len >= 6) {
                    val segment = String(bytes, start, len, Charsets.US_ASCII).trim()
                    if (segment.isNotEmpty() && !segment.startsWith("Microsoft") && !segment.contains("Word.Document")) {
                        if (hasImageChar) {
                            htmlBuilder.append("<p>")
                            segment.forEach { char ->
                                if (char == '\u0001') {
                                    htmlBuilder.append("</p>")
                                    insertImageTag()
                                    htmlBuilder.append("<p>")
                                } else {
                                    htmlBuilder.append(android.text.TextUtils.htmlEncode(char.toString()))
                                }
                            }
                            htmlBuilder.append("</p>")
                        } else {
                            htmlBuilder.append("<p>").append(android.text.TextUtils.htmlEncode(segment)).append("</p>")
                        }
                    }
                }
                continue
            }

            i++
        }

        // Append any remaining images at the end if they were not placed by \u0001
        while (embeddedImgCount < imageFiles.size) {
            insertImageTag()
        }

        htmlBuilder.append("</body></html>")
        outHtmlFile.parentFile?.mkdirs()
        outHtmlFile.writeText(htmlBuilder.toString())
        
        BookContent.HtmlFile(outHtmlFile.absolutePath)
    }
}
