package br.com.ebook.extractor

import br.com.ebook.core.BookContent
import br.com.ebook.core.BookExtractor
import br.com.ebook.core.BookMetadata
import org.ebookdroid.core.codec.OutlineLink
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Inflater

object RbBookExtractor : BookExtractor {
    private val LOGGER = LoggerFactory.getLogger(RbBookExtractor::class.java)

    override val supportedFormats: Set<String> = setOf("rb")

    private val MAGIC = byteArrayOf(
        0xb0.toByte(), 0x0c.toByte(), 0xb0.toByte(), 0x0c.toByte(),
        0x02, 0x00,
        'N'.code.toByte(), 'U'.code.toByte(), 'V'.code.toByte(), 'O'.code.toByte(),
        0x00, 0x00, 0x00, 0x00
    )

    data class RbItem(
        val name: String,
        val size: Int,
        val offset: Long,
        val flags: Int
    )

    private fun readToc(raf: RandomAccessFile): List<RbItem> {
        raf.seek(0)
        val header = ByteArray(14)
        raf.readFully(header)
        if (!header.contentEquals(MAGIC)) {
            LOGGER.warn("Invalid RocketBook header")
            return emptyList()
        }

        raf.seek(24)
        val tocBuf = ByteArray(4)
        raf.readFully(tocBuf)
        val tocOffset = ByteBuffer.wrap(tocBuf).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL

        raf.seek(tocOffset)
        val countBuf = ByteArray(4)
        raf.readFully(countBuf)
        val count = ByteBuffer.wrap(countBuf).order(ByteOrder.LITTLE_ENDIAN).int

        val items = mutableListOf<RbItem>()
        val entryBuf = ByteArray(44)
        for (i in 0 until count) {
            raf.readFully(entryBuf)
            val nameBytes = entryBuf.copyOfRange(0, 32)
            var len = 0
            while (len < 32 && nameBytes[len] != 0.toByte()) {
                len++
            }
            val name = String(nameBytes, 0, len, Charsets.UTF_8).trim()

            val bb = ByteBuffer.wrap(entryBuf, 32, 12).order(ByteOrder.LITTLE_ENDIAN)
            val size = bb.int
            val offset = bb.int.toLong() and 0xFFFFFFFFL
            val flags = bb.int

            items.add(RbItem(name, size, offset, flags))
        }
        return items
    }

    override suspend fun extractMetadata(path: String): Result<BookMetadata> = runCatching {
        var title = ""
        var author = ""
        try {
            RandomAccessFile(path, "r").use { raf ->
                val items = readToc(raf)
                val infoItem = items.firstOrNull { it.flags == 2 || it.name.equals("info.info", ignoreCase = true) }
                if (infoItem != null) {
                    raf.seek(infoItem.offset)
                    val infoData = ByteArray(infoItem.size)
                    raf.readFully(infoData)
                    val lines = String(infoData, Charsets.UTF_8).split('\n')
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.startsWith("TITLE=", ignoreCase = true)) {
                            title = trimmed.substring(6).trim()
                        } else if (trimmed.startsWith("AUTHOR=", ignoreCase = true)) {
                            author = trimmed.substring(7).trim()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error reading RB metadata from {}: {}", path, e.message, e)
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

    private fun decompressFirstTextChunk(raf: RandomAccessFile, item: RbItem): String {
        return try {
            raf.seek(item.offset)
            val hdr = ByteArray(8)
            raf.readFully(hdr)
            val bbHdr = ByteBuffer.wrap(hdr).order(ByteOrder.LITTLE_ENDIAN)
            val chunkCount = bbHdr.int
            if (chunkCount <= 0) return ""
            val firstChunkSize = ByteBuffer.wrap(ByteArray(4).also { raf.readFully(it) }).order(ByteOrder.LITTLE_ENDIAN).int
            raf.skipBytes((chunkCount - 1) * 4)
            val compData = ByteArray(firstChunkSize)
            raf.readFully(compData)
            val inflater = Inflater()
            inflater.setInput(compData)
            val out = ByteArrayOutputStream()
            val buf = ByteArray(4096)
            while (!inflater.needsInput() && !inflater.finished()) {
                val count = inflater.inflate(buf)
                if (count > 0) out.write(buf, 0, count) else break
            }
            inflater.end()
            out.toString(Charsets.UTF_8.name())
        } catch (e: Exception) {
            LOGGER.warn("Error decompressing first chunk in RB: {}", e.message)
            ""
        }
    }

    override suspend fun extractCover(path: String): Result<ByteArray?> = runCatching {
        var coverBytes: ByteArray? = null
        try {
            RandomAccessFile(path, "r").use { raf ->
                val items = readToc(raf)
                val imageItems = items.filter {
                    val n = it.name.lowercase()
                    n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")
                }

                // 1. Tenta obter a primeira imagem referenciada no index.html (ex: 12.png)
                var targetImgName: String? = null
                val htmlItem = items.firstOrNull { it.name.equals("index.html", ignoreCase = true) }
                if (htmlItem != null && htmlItem.flags == 8) {
                    val firstHtmlChunk = decompressFirstTextChunk(raf, htmlItem)
                    val imgMatch = Regex("""<img[^>]+src=["']?([^"'>\s]+)""", RegexOption.IGNORE_CASE).find(firstHtmlChunk)
                    if (imgMatch != null) {
                        targetImgName = File(imgMatch.groupValues[1]).name.lowercase()
                    }
                }

                val coverItem = if (targetImgName != null) {
                    imageItems.firstOrNull { it.name.equals(targetImgName, ignoreCase = true) }
                } else null ?: imageItems.maxByOrNull { it.size } ?: imageItems.firstOrNull()

                if (coverItem != null) {
                    raf.seek(coverItem.offset)
                    val data = ByteArray(coverItem.size)
                    raf.readFully(data)
                    coverBytes = data
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error extracting RB cover from {}: {}", path, e.message, e)
        }
        coverBytes
    }

    override suspend fun extractOverview(path: String): Result<String> = runCatching {
        ""
    }

    override suspend fun extractFooterNotes(path: String): Result<Map<String, String>> = runCatching {
        emptyMap()
    }

    private fun decodeHtmlBytes(bytes: ByteArray): String {
        return try {
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: Exception) {
            String(bytes, java.nio.charset.Charset.forName("windows-1252"))
        }
    }

    override suspend fun extractContent(path: String, outputDir: String): Result<BookContent> = runCatching {
        val hashCode = path.hashCode().toString()
        val destDir = File(outputDir, "rb_temp_$hashCode")
        destDir.mkdirs()

        RandomAccessFile(path, "r").use { raf ->
            val items = readToc(raf)
            for (item in items) {
                if (item.name.isBlank()) continue
                val outFile = File(destDir, item.name)

                if (item.flags == 8) {
                    // Chunks compactados em Zlib
                    raf.seek(item.offset)
                    val hdr = ByteArray(8)
                    raf.readFully(hdr)
                    val bbHdr = ByteBuffer.wrap(hdr).order(ByteOrder.LITTLE_ENDIAN)
                    val chunkCount = bbHdr.int
                    bbHdr.int // uncompressedSize

                    val chunkSizes = IntArray(chunkCount)
                    val sizesBuf = ByteArray(chunkCount * 4)
                    raf.readFully(sizesBuf)
                    val bbSizes = ByteBuffer.wrap(sizesBuf).order(ByteOrder.LITTLE_ENDIAN)
                    for (i in 0 until chunkCount) {
                        chunkSizes[i] = bbSizes.int
                    }

                    FileOutputStream(outFile).use { fos ->
                        val inflater = Inflater()
                        val uncompBuf = ByteArray(32768)
                        for (cSize in chunkSizes) {
                            val compData = ByteArray(cSize)
                            raf.readFully(compData)
                            inflater.reset()
                            inflater.setInput(compData)
                            while (!inflater.needsInput() && !inflater.finished()) {
                                val count = inflater.inflate(uncompBuf)
                                if (count > 0) {
                                    fos.write(uncompBuf, 0, count)
                                } else {
                                    break
                                }
                            }
                        }
                        inflater.end()
                    }
                } else if (item.flags == 0 && item.size > 0) {
                    // Arquivos não compactados (imagens PNG/JPEG, índice)
                    raf.seek(item.offset)
                    val rawData = ByteArray(item.size)
                    raf.readFully(rawData)
                    FileOutputStream(outFile).use { it.write(rawData) }
                }
            }
        }

        val indexHtml = File(destDir, "index.html")
        if (!indexHtml.exists()) {
            throw java.io.IOException("index.html missing after RB extraction")
        }

        try {
            val rawBytes = indexHtml.readBytes()
            val htmlText = decodeHtmlBytes(rawBytes)
            val doc = Jsoup.parse(htmlText, "UTF-8")

            // Flatten empty <center>, <div>, <p> wrappers around images
            for (img in doc.select("img")) {
                var p = img.parent()
                while (p != null && p.tagName().lowercase() in setOf("center", "div", "p")
                    && p.text().trim().isEmpty() && p.children().size <= 1 && p.tagName().lowercase() != "body"
                ) {
                    val grandParent = p.parent()
                    p.replaceWith(img)
                    p = grandParent
                }
            }

            indexHtml.writeText(doc.outerHtml(), Charsets.UTF_8)

            // Package into standard EPUB 2.0
            val epubFile = File(destDir, "book.epub")
            try {
                val meta = extractMetadata(path).getOrNull()
                val title = meta?.title?.ifEmpty { File(path).nameWithoutExtension } ?: File(path).nameWithoutExtension
                val author = meta?.author ?: ""

                val resourceBytes = mutableMapOf<String, ByteArray>()
                destDir.listFiles { _, name ->
                    val n = name.lowercase()
                    n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")
                }?.forEach { f ->
                    resourceBytes[f.name] = f.readBytes()
                }

                val coverBytes = extractCover(path).getOrNull() ?: resourceBytes["12.png"]
                val coverFileName = "12.png"

                val chapters = br.com.ebook.util.SpineSegmenter.segmentHtmlDocument(
                    doc = doc,
                    resourceBytes = resourceBytes,
                    bookTitle = title,
                    coverFileNameToSkip = coverFileName,
                    minTextChars = 200
                )

                br.com.ebook.util.EpubPackager.createEpub(
                    outputFile = epubFile,
                    title = title,
                    author = author,
                    coverBytes = coverBytes,
                    coverFileName = coverFileName,
                    galleryItems = emptyList(),
                    chapters = chapters,
                    resourceFiles = resourceBytes
                )
                LOGGER.info("RB successfully packaged to EPUB: {}", epubFile.absolutePath)
            } catch (e: Exception) {
                LOGGER.error("Error packaging RB to EPUB: {}", e.message, e)
            }
        } catch (e: Exception) {
            LOGGER.warn("Could not process RB HTML: {}", e.message)
        }

        val epubFile = File(destDir, "book.epub")
        if (epubFile.exists()) {
            BookContent.EpubFile(epubFile.absolutePath)
        } else {
            BookContent.HtmlFile(indexHtml.absolutePath)
        }
    }

    fun extractOutline(path: String): List<OutlineLink> {
        val list = mutableListOf<OutlineLink>()
        try {
            var htmlContent: String? = null
            RandomAccessFile(path, "r").use { raf ->
                val items = readToc(raf)
                val htmlItem = items.firstOrNull { it.name.equals("index.html", ignoreCase = true) }
                if (htmlItem != null && htmlItem.flags == 8) {
                    raf.seek(htmlItem.offset)
                    val hdr = ByteArray(8)
                    raf.readFully(hdr)
                    val bbHdr = ByteBuffer.wrap(hdr).order(ByteOrder.LITTLE_ENDIAN)
                    val chunkCount = bbHdr.int
                    val uncompressedSize = bbHdr.int

                    val chunkSizes = IntArray(chunkCount)
                    val sizesBuf = ByteArray(chunkCount * 4)
                    raf.readFully(sizesBuf)
                    val bbSizes = ByteBuffer.wrap(sizesBuf).order(ByteOrder.LITTLE_ENDIAN)
                    for (i in 0 until chunkCount) {
                        chunkSizes[i] = bbSizes.int
                    }

                    val outStream = java.io.ByteArrayOutputStream(uncompressedSize)
                    val inflater = Inflater()
                    val uncompBuf = ByteArray(32768)
                    for (cSize in chunkSizes) {
                        val compData = ByteArray(cSize)
                        raf.readFully(compData)
                        inflater.reset()
                        inflater.setInput(compData)
                        while (!inflater.needsInput() && !inflater.finished()) {
                            val count = inflater.inflate(uncompBuf)
                            if (count > 0) {
                                outStream.write(uncompBuf, 0, count)
                            } else {
                                break
                            }
                        }
                    }
                    inflater.end()
                    htmlContent = decodeHtmlBytes(outStream.toByteArray())
                }
            }

            val content = htmlContent
            if (content.isNullOrBlank()) return emptyList()

            val doc = Jsoup.parse(content)
            val tocLinks = doc.select("a[href^=#]")
            val seenLinks = mutableSetOf<String>()

            for (a in tocLinks) {
                val href = a.attr("href").trim()
                val text = a.text().trim()
                if (href.length > 1 && text.isNotEmpty() && !seenLinks.contains(href)) {
                    val anchorId = href.removePrefix("#")
                    val exists = doc.getElementById(anchorId) != null || doc.select("a[name=$anchorId]").isNotEmpty()
                    if (exists) {
                        seenLinks.add(href)
                        list.add(OutlineLink(text, href, 0))
                    }
                }
            }

            if (list.isEmpty()) {
                val headings = doc.select("h1, h2, h3, h4")
                for (h in headings) {
                    val text = h.text().trim()
                    if (text.isNotEmpty()) {
                        val anchorId = h.id().ifBlank { h.select("a[name]").attr("name") }
                        if (anchorId.isNotBlank()) {
                            val link = "#$anchorId"
                            if (!seenLinks.contains(link)) {
                                seenLinks.add(link)
                                list.add(OutlineLink(text, link, 0))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.warn("Error extracting outline from RB {}: {}", path, e.message)
        }
        return list
    }
}
