package br.com.ebook.extractor

import br.com.ebook.core.BookContent
import br.com.ebook.core.BookExtractor
import br.com.ebook.core.BookMetadata
import org.ebookdroid.core.codec.OutlineLink
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
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

    override suspend fun extractCover(path: String): Result<ByteArray?> = runCatching {
        var coverBytes: ByteArray? = null
        try {
            RandomAccessFile(path, "r").use { raf ->
                val items = readToc(raf)
                val imageItems = items.filter {
                    val n = it.name.lowercase()
                    n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")
                }
                val coverItem = imageItems.firstOrNull { it.name.startsWith("0.") } ?: imageItems.firstOrNull()
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
                    val uncompressedSize = bbHdr.int

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

        BookContent.HtmlFile(indexHtml.absolutePath)
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
                    htmlContent = outStream.toString("UTF-8")
                }
            }

            if (htmlContent.isNullOrBlank()) return emptyList()

            val doc = Jsoup.parse(htmlContent)
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
