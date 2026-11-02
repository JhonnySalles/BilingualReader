package br.com.ebook.extractor

import br.com.ebook.core.BookContent
import br.com.ebook.core.BookExtractor
import br.com.ebook.core.BookMetadata
import br.com.ebook.foobnix.ext.XmlParser
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.ebookdroid.core.codec.OutlineLink
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import org.slf4j.LoggerFactory
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Inflater

object SnbBookExtractor : BookExtractor {
    private val LOGGER = LoggerFactory.getLogger(SnbBookExtractor::class.java)

    override val supportedFormats: Set<String> = setOf("snb")

    private val MAGIC = "SNBP000B".toByteArray(Charsets.US_ASCII)

    class SnbFileEntry(
        val attr: Int,
        val fileNameOffset: Int,
        val fileSize: Int,
        var fileName: String = "",
        var fileBody: ByteArray? = null
    )

    class SnbContainer(
        val files: List<SnbFileEntry>
    ) {
        fun getFileStream(name: String): ByteArray? {
            return files.firstOrNull { it.fileName.equals(name, ignoreCase = true) }?.fileBody
        }
    }

    private fun decompressZlib(data: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(data)
        val out = ByteArrayOutputStream(data.size * 2)
        val buffer = ByteArray(16384)
        while (!inflater.needsInput() && !inflater.finished()) {
            val count = inflater.inflate(buffer)
            if (count > 0) {
                out.write(buffer, 0, count)
            } else {
                break
            }
        }
        inflater.end()
        return out.toByteArray()
    }

    private fun parseSnb(raf: RandomAccessFile, metaOnly: Boolean = false): SnbContainer {
        raf.seek(0)
        val vmbr = ByteArray(44)
        raf.readFully(vmbr)

        val bbHdr = ByteBuffer.wrap(vmbr).order(ByteOrder.BIG_ENDIAN)
        val magicBytes = ByteArray(8)
        bbHdr.get(magicBytes)
        if (!magicBytes.contentEquals(MAGIC)) {
            throw IllegalArgumentException("Invalid SNB magic header")
        }

        val rev80 = bbHdr.int
        val revA3 = bbHdr.int
        val revZ1 = bbHdr.int
        val fileCount = bbHdr.int
        val vfatSize = bbHdr.int
        val vfatCompressed = bbHdr.int
        val binStreamSize = bbHdr.int
        val plainStreamSizeUncompressed = bbHdr.int
        val revZ2 = bbHdr.int

        // Ler e descompactar VFAT (Zlib)
        val vfatCompData = ByteArray(vfatCompressed)
        raf.readFully(vfatCompData)
        val vfat = decompressZlib(vfatCompData)

        // Parse VFAT
        val files = mutableListOf<SnbFileEntry>()
        val fileNamesTable = vfat.copyOfRange(fileCount * 12, vfat.size)
        val fileNames = String(fileNamesTable, Charsets.UTF_8).split('\u0000')

        val bbVfat = ByteBuffer.wrap(vfat).order(ByteOrder.BIG_ENDIAN)
        for (i in 0 until fileCount) {
            val attr = bbVfat.int
            val nameOffset = bbVfat.int
            val fileSize = bbVfat.int
            val name = if (i < fileNames.size) fileNames[i] else ""
            files.add(SnbFileEntry(attr, nameOffset, fileSize, name))
        }

        // Ler Tail no final do arquivo
        raf.seek(raf.length() - 16)
        val tailBlock = ByteArray(16)
        raf.readFully(tailBlock)
        val bbTail = ByteBuffer.wrap(tailBlock).order(ByteOrder.BIG_ENDIAN)
        val tailSize = bbTail.int
        val tailOffset = bbTail.int.toLong() and 0xFFFFFFFFL

        raf.seek(tailOffset)
        val tailCompData = ByteArray(tailSize)
        raf.readFully(tailCompData)
        val vTail = decompressZlib(tailCompData)

        val binBlock = (binStreamSize + 0x8000 - 1) / 0x8000
        val plainBlock = (plainStreamSizeUncompressed + 0x8000 - 1) / 0x8000

        val blockOffsets = mutableListOf<Long>()
        val bbVTail = ByteBuffer.wrap(vTail).order(ByteOrder.BIG_ENDIAN)
        for (i in 0 until (binBlock + plainBlock)) {
            val off = bbVTail.int.toLong() and 0xFFFFFFFFL
            blockOffsets.add(off)
        }

        // Se metaOnly for verdadeiro, precisamos apenas carregar book.snbf e imagens
        // Descompactar Plain Stream (blocos BZip2)
        var uncompressedData: ByteArray? = null
        if (plainBlock > 0) {
            val outPlain = ByteArrayOutputStream(plainStreamSizeUncompressed)
            for (i in 0 until plainBlock) {
                val curOff = blockOffsets[binBlock + i]
                val bSize = if (i < plainBlock - 1) {
                    (blockOffsets[binBlock + i + 1] - curOff).toInt()
                } else {
                    (tailOffset - curOff).toInt()
                }

                raf.seek(curOff)
                val blockBytes = ByteArray(bSize)
                raf.readFully(blockBytes)

                if (bSize < 32768) {
                    try {
                        BZip2CompressorInputStream(ByteArrayInputStream(blockBytes)).use { bzIn ->
                            val buf = ByteArray(8192)
                            var read: Int
                            while (bzIn.read(buf).also { read = it } != -1) {
                                outPlain.write(buf, 0, read)
                            }
                        }
                    } catch (e: Exception) {
                        LOGGER.warn("Error decompressing BZip2 block $i in SNB: {}", e.message)
                    }
                } else {
                    outPlain.write(blockBytes)
                }
            }
            uncompressedData = outPlain.toByteArray()
        }

        var binPos = 0L
        var plainPos = 0
        for (f in files) {
            if ((f.attr and 0x41000000) == 0x41000000) {
                // Arquivo comprimido em texto
                if (uncompressedData != null && plainPos + f.fileSize <= uncompressedData.size) {
                    f.fileBody = uncompressedData.copyOfRange(plainPos, plainPos + f.fileSize)
                }
                plainPos += f.fileSize
            } else if ((f.attr and 0x01000000) == 0x01000000) {
                // Arquivo binário (imagem)
                val absPos = 44L + vfatCompressed + binPos
                raf.seek(absPos)
                val buf = ByteArray(f.fileSize)
                raf.readFully(buf)
                f.fileBody = buf
                binPos += f.fileSize
            }
        }

        return SnbContainer(files)
    }

    override suspend fun extractMetadata(path: String): Result<BookMetadata> = runCatching {
        var title = ""
        var author = ""
        var publisher = ""
        var description = ""

        try {
            RandomAccessFile(path, "r").use { raf ->
                val snb = parseSnb(raf, metaOnly = true)
                val metaXml = snb.getFileStream("snbf/book.snbf")
                if (metaXml != null) {
                    val doc = Jsoup.parse(ByteArrayInputStream(metaXml), "UTF-8", "", org.jsoup.parser.Parser.xmlParser())
                    val n = doc.select("name").text().trim()
                    if (n.isNotEmpty()) title = n

                    val a = doc.select("author").text().trim()
                    if (a.isNotEmpty()) author = a

                    val p = doc.select("publisher").text().trim()
                    if (p.isNotEmpty()) publisher = p

                    val d = doc.select("abstract").text().trim()
                    if (d.isNotEmpty()) description = d
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error extracting SNB metadata from {}: {}", path, e.message, e)
        }

        if (title.isBlank()) {
            title = File(path).nameWithoutExtension
        }

        BookMetadata(
            title = title,
            author = author,
            unzipPath = path,
            annotation = description,
            publisher = publisher
        )
    }

    override suspend fun extractCover(path: String): Result<ByteArray?> = runCatching {
        var coverBytes: ByteArray? = null
        try {
            RandomAccessFile(path, "r").use { raf ->
                val snb = parseSnb(raf, metaOnly = true)
                var coverName = ""
                val metaXml = snb.getFileStream("snbf/book.snbf")
                if (metaXml != null) {
                    val doc = Jsoup.parse(ByteArrayInputStream(metaXml), "UTF-8", "", org.jsoup.parser.Parser.xmlParser())
                    coverName = doc.select("cover").text().trim()
                }

                if (coverName.isNotEmpty()) {
                    coverBytes = snb.getFileStream("snbc/images/$coverName")
                }
                if (coverBytes == null) {
                    // Fallback para primeira imagem encontrada
                    val firstImg = snb.files.firstOrNull {
                        val n = it.fileName.lowercase()
                        (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png")) && it.fileBody != null
                    }
                    coverBytes = firstImg?.fileBody
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error extracting SNB cover from {}: {}", path, e.message, e)
        }
        coverBytes
    }

    override suspend fun extractOverview(path: String): Result<String> = runCatching {
        var overview = ""
        try {
            RandomAccessFile(path, "r").use { raf ->
                val snb = parseSnb(raf, metaOnly = true)
                val metaXml = snb.getFileStream("snbf/book.snbf")
                if (metaXml != null) {
                    val doc = Jsoup.parse(ByteArrayInputStream(metaXml), "UTF-8", "", org.jsoup.parser.Parser.xmlParser())
                    val abs = doc.select("abstract").text()
                    if (abs.isNotBlank()) {
                        overview = Jsoup.clean(abs, Safelist.simpleText())
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error extracting SNB overview from {}: {}", path, e.message, e)
        }
        overview.trim()
    }

    override suspend fun extractFooterNotes(path: String): Result<Map<String, String>> = runCatching {
        emptyMap()
    }

    private fun escapeHtml(text: String): String {
        val out = StringBuilder(text.length)
        for (c in text) {
            when (c) {
                '<' -> out.append("&lt;")
                '>' -> out.append("&gt;")
                '&' -> out.append("&amp;")
                '"' -> out.append("&quot;")
                '\'' -> out.append("&#39;")
                else -> out.append(c)
            }
        }
        return out.toString()
    }

    private fun resolveSnbImage(rawName: String, diskImages: Map<String, File>): String? {
        val clean = rawName.trim()
        if (clean.isEmpty()) return null
        val lower = clean.lowercase()
        // 1. Match direto
        diskImages[lower]?.let { return it.name }
        // 2. Normalizar ".._" e "oebps_text_"
        val norm = lower.replace(".._", "").replace("oebps_text_", "oebps_")
        diskImages[norm]?.let { return it.name }
        // 3. Match pelo nome do arquivo simples
        val simple = File(lower).name
        diskImages[simple]?.let { return it.name }
        // 4. Match por sufixo
        val suffix = lower.substringAfterLast(".._")
        for ((k, file) in diskImages) {
            if (k.endsWith(suffix) || suffix.endsWith(k)) {
                return file.name
            }
        }
        return null
    }

    override suspend fun extractContent(path: String, outputDir: String): Result<BookContent> = runCatching {
        val hashCode = path.hashCode().toString()
        val destDir = File(outputDir, "snb_temp_$hashCode")
        val imgDir = File(destDir, "images")
        destDir.mkdirs()
        imgDir.mkdirs()

        var chapters = mutableListOf<Pair<String, String>>() // src to title
        RandomAccessFile(path, "r").use { raf ->
            val snb = parseSnb(raf)

            // 1. Extrair todas as imagens para destDir/images/
            for (f in snb.files) {
                if (f.fileBody != null) {
                    val fName = f.fileName
                    if (fName.startsWith("snbc/images/", ignoreCase = true) || fName.startsWith("images/", ignoreCase = true)) {
                        val simpleName = File(fName).name
                        val imgFile = File(imgDir, simpleName)
                        FileOutputStream(imgFile).use { it.write(f.fileBody!!) }
                    }
                }
            }

            val diskImageFiles = imgDir.listFiles()?.associateBy { it.name.lowercase() } ?: emptyMap()

            // 2. Ler toc.snbf
            val tocXml = snb.getFileStream("snbf/toc.snbf")
            if (tocXml != null) {
                val docToc = Jsoup.parse(ByteArrayInputStream(tocXml), "UTF-8", "", org.jsoup.parser.Parser.xmlParser())
                for (ch in docToc.select("chapter")) {
                    val src = ch.attr("src").trim()
                    val title = ch.text().trim()
                    if (src.isNotEmpty()) {
                        chapters.add(Pair(src, title))
                    }
                }
            }

            // Se toc.snbf não tiver capítulos, fallback para todos os arquivos .snbc
            if (chapters.isEmpty()) {
                for (f in snb.files) {
                    if (f.fileName.endsWith(".snbc", ignoreCase = true) && !f.fileName.contains("snbf/")) {
                        chapters.add(Pair(File(f.fileName).name, File(f.fileName).nameWithoutExtension))
                    }
                }
            }

            // 2.5 Obter imagem de capa de snbf/book.snbf
            var coverName = ""
            val metaXml = snb.getFileStream("snbf/book.snbf")
            if (metaXml != null) {
                val docMeta = Jsoup.parse(ByteArrayInputStream(metaXml), "UTF-8", "", org.jsoup.parser.Parser.xmlParser())
                coverName = docMeta.select("cover").text().trim()
            }
            if (coverName.isEmpty()) {
                val firstImg = snb.files.firstOrNull {
                    val n = it.fileName.lowercase()
                    (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png")) && it.fileBody != null
                }
                if (firstImg != null) {
                    coverName = File(firstImg.fileName).name
                }
            }

            // 3. Montar index.html unificado
            val indexHtml = File(destDir, "index.html")
            val hasCover = coverName.isNotEmpty() && File(imgDir, coverName).exists()
            java.io.PrintWriter(java.io.BufferedWriter(java.io.OutputStreamWriter(FileOutputStream(indexHtml), Charsets.UTF_8))).use { writer ->
                writer.println("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>body{margin:0;padding:0;} p,p+p{margin:0;text-indent:1.5em;line-height:1.4;} h2{text-align:center;margin:1.5em 0;} img{width:100%;height:auto;display:block;margin:0 auto;} .bilingual-reader-cover-page{page-break-after:always;text-align:center;margin:0;padding:0;} .bilingual-reader-gallery-page{page-break-after:always;text-align:center;margin:0;padding:0;} .bilingual-reader-image-inline{text-align:center;margin:1.2em 0;}</style></head><body>")

                if (hasCover) {
                    writer.println("<div class=\"bilingual-reader-cover-page\" style=\"page-break-after: always; text-align: center; margin: 0; padding: 0;\"><img src=\"images/$coverName\" style=\"width: 100%; height: auto; display: block; margin: 0 auto;\" alt=\"Cover\"/></div>")
                }

                var chapIdx = 0
                for (pair in chapters) {
                    chapIdx++
                    val src = pair.first
                    val title = pair.second
                    val chapBytes = snb.getFileStream("snbc/$src") ?: snb.getFileStream(src)

                    if (chapBytes != null) {
                        try {
                            val docChap = Jsoup.parse(ByteArrayInputStream(chapBytes), "UTF-8", "", org.jsoup.parser.Parser.xmlParser())
                            val hasText = docChap.select("text").any { it.text().trim().isNotEmpty() }
                            val isImageOnlyChapter = !hasText

                            writer.println("<section id=\"snb_sec_$chapIdx\"><a id=\"snb_chap_$chapIdx\"></a>")
                            if (title.isNotEmpty() && !isImageOnlyChapter) {
                                writer.println("<h2>${escapeHtml(title)}</h2>")
                            }

                            for (el in docChap.select("text, image, img")) {
                                if (el.tagName().equals("text", ignoreCase = true)) {
                                    val t = el.text().trim()
                                    if (t.isNotEmpty()) {
                                        writer.println("<p>${escapeHtml(t)}</p>")
                                    } else {
                                        writer.println("<br/>")
                                    }
                                } else if (el.tagName().equals("image", ignoreCase = true) || el.tagName().equals("img", ignoreCase = true)) {
                                    val rawSrc = el.attr("src").ifEmpty { el.attr("href") }.ifEmpty { el.text().trim() }
                                    val resolved = resolveSnbImage(rawSrc, diskImageFiles)
                                    if (resolved != null) {
                                        if (isImageOnlyChapter) {
                                            writer.println("<div class=\"bilingual-reader-gallery-page\" style=\"page-break-after: always; text-align: center; margin: 0; padding: 0;\"><img src=\"images/$resolved\" style=\"width: 100%; height: auto; display: block; margin: 0 auto;\"/></div>")
                                        } else {
                                            writer.println("<div class=\"bilingual-reader-image-inline\" style=\"text-align: center; margin: 1.2em 0;\"><img src=\"images/$resolved\" style=\"width: 100%; height: auto; display: block; margin: 0 auto;\"/></div>")
                                        }
                                    }
                                }
                            }
                            writer.println("</section>")
                        } catch (e: Exception) {
                            LOGGER.warn("Error parsing SNB chapter $src: {}", e.message)
                        }
                    }
                }
                writer.println("</body></html>")
            }

            // Package into standard EPUB 2.0
            val epubFile = File(destDir, "book.epub")
            try {
                val meta = extractMetadata(path).getOrNull()
                val title = meta?.title?.ifEmpty { File(path).nameWithoutExtension } ?: File(path).nameWithoutExtension
                val author = meta?.author ?: ""

                val resourceBytes = mutableMapOf<String, ByteArray>()
                imgDir.listFiles { _, name ->
                    val n = name.lowercase()
                    n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp")
                }?.forEach { f ->
                    resourceBytes["images/${f.name}"] = f.readBytes()
                }

                val coverBytes = if (hasCover) File(imgDir, coverName).readBytes() else null

                val galleryItems = mutableListOf<br.com.ebook.util.EpubPackager.GalleryItem>()
                val epubChapters = mutableListOf<br.com.ebook.util.EpubPackager.ChapterItem>()

                var chapCounter = 0
                for (pair in chapters) {
                    chapCounter++
                    val src = pair.first
                    val cTitle = pair.second
                    val chapBytes = snb.getFileStream("snbc/$src") ?: snb.getFileStream(src)
                    if (chapBytes != null) {
                        try {
                            val docChap = Jsoup.parse(ByteArrayInputStream(chapBytes), "UTF-8", "", org.jsoup.parser.Parser.xmlParser())
                            val hasText = docChap.select("text").any { it.text().trim().isNotEmpty() }
                            if (!hasText) {
                                // Image-only chapter (introductory gallery/insert page) in linear reading order
                                val imgEls = docChap.select("image, img")
                                var subIdx = 0
                                for (imgEl in imgEls) {
                                    val rawSrc = imgEl.attr("src").ifEmpty { imgEl.attr("href") }.ifEmpty { imgEl.text().trim() }
                                    val resolved = resolveSnbImage(rawSrc, diskImageFiles)
                                    if (resolved != null && resourceBytes.containsKey("images/$resolved")) {
                                        val itemTitle = if (subIdx == 0 && cTitle.isNotEmpty()) cTitle else (cTitle.ifEmpty { resolved })
                                        val fullPageHtml = """<div class="br-full-page"><img src="images/$resolved" alt="${escapeHtml(itemTitle)}" /></div>"""
                                        epubChapters.add(
                                            br.com.ebook.util.EpubPackager.ChapterItem(
                                                title = itemTitle,
                                                contentHtml = fullPageHtml,
                                                fileName = "img_page_${chapCounter}_$subIdx.xhtml",
                                                anchorId = if (subIdx == 0) "snb_chap_$chapCounter" else null
                                            )
                                        )
                                        subIdx++
                                    }
                                }
                            } else {
                                val chapHtml = StringBuilder()
                                if (cTitle.isNotEmpty()) {
                                    chapHtml.append("<h2>${escapeHtml(cTitle)}</h2>\n")
                                }
                                for (el in docChap.select("text, image, img")) {
                                    if (el.tagName().equals("text", ignoreCase = true)) {
                                        val t = el.text().trim()
                                        if (t.isNotEmpty()) {
                                            chapHtml.append("<p>${escapeHtml(t)}</p>\n")
                                        } else {
                                            chapHtml.append("<br/>\n")
                                        }
                                    } else {
                                        val rawSrc = el.attr("src").ifEmpty { el.attr("href") }.ifEmpty { el.text().trim() }
                                        val resolved = resolveSnbImage(rawSrc, diskImageFiles)
                                        if (resolved != null) {
                                            chapHtml.append("<div style=\"text-align: center; margin: 1.2em 0;\"><img src=\"images/$resolved\" style=\"width: 100%; height: auto; display: block; margin: 0 auto;\"/></div>\n")
                                        }
                                    }
                                }
                                epubChapters.add(
                                    br.com.ebook.util.EpubPackager.ChapterItem(
                                        title = cTitle,
                                        contentHtml = chapHtml.toString(),
                                        fileName = "chap_$chapCounter.xhtml",
                                        anchorId = "snb_chap_$chapCounter"
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            LOGGER.warn("Error processing chapter $src for EPUB: {}", e.message)
                        }
                    }
                }

                br.com.ebook.util.EpubPackager.createEpub(
                    outputFile = epubFile,
                    title = title,
                    author = author,
                    coverBytes = coverBytes,
                    coverFileName = if (coverName.isNotEmpty()) "images/$coverName" else "cover.jpg",
                    galleryItems = galleryItems,
                    chapters = epubChapters,
                    resourceFiles = resourceBytes
                )
                LOGGER.info("SNB successfully packaged to EPUB: {}", epubFile.absolutePath)
            } catch (e: Exception) {
                LOGGER.error("Error packaging SNB to EPUB: {}", e.message, e)
            }
        }

        val outIndex = File(destDir, "index.html")
        val epubFile = File(destDir, "book.epub")
        if (epubFile.exists()) {
            BookContent.EpubFile(epubFile.absolutePath)
        } else {
            BookContent.HtmlFile(outIndex.absolutePath)
        }
    }

    fun extractOutline(path: String): List<OutlineLink> {
        val list = mutableListOf<OutlineLink>()
        try {
            RandomAccessFile(path, "r").use { raf ->
                val snb = parseSnb(raf, metaOnly = true)
                val tocXml = snb.getFileStream("snbf/toc.snbf")
                if (tocXml != null) {
                    val docToc = Jsoup.parse(ByteArrayInputStream(tocXml), "UTF-8", "", org.jsoup.parser.Parser.xmlParser())
                    var idx = 0
                    for (ch in docToc.select("chapter")) {
                        idx++
                        val title = ch.text().trim()
                        if (title.isNotEmpty()) {
                            list.add(OutlineLink(title, "#snb_chap_$idx", 0))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.warn("Error extracting outline from SNB {}: {}", path, e.message)
        }
        return list
    }
}
