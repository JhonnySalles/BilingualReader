package br.com.ebook.extractor

import br.com.ebook.core.BookContent
import br.com.ebook.core.BookExtractor
import br.com.ebook.core.BookMetadata
import br.com.ebook.util.IOUtils.copyTo
import org.slf4j.LoggerFactory
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

object PmlzBookExtractor : BookExtractor {
    private val LOGGER = LoggerFactory.getLogger(PmlzBookExtractor::class.java)

    override val supportedFormats: Set<String> = setOf("pmlz")

    override suspend fun extractMetadata(path: String): Result<BookMetadata> = runCatching {
        BookMetadata(
            title = File(path).nameWithoutExtension,
            author = "",
            unzipPath = path
        )
    }

    override suspend fun extractCover(path: String): Result<ByteArray?> = runCatching {
        var coverBytes: ByteArray? = null
        try {
            ZipFile(path).use { zip ->
                val entries = zip.entries()
                var coverEntryName: String? = null
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name.lowercase()
                    if (name.endsWith("cover.png") || name.endsWith("cover.jpg") || name.endsWith("cover.jpeg")) {
                        coverEntryName = entry.name
                        break
                    }
                }
                if (coverEntryName != null) {
                    val entry = zip.getEntry(coverEntryName)
                    zip.getInputStream(entry).use { input ->
                        coverBytes = input.readBytes()
                    }
                } else {
                    val entriesList = zip.entries().toList()
                    val firstImage = entriesList.firstOrNull {
                        val name = it.name.lowercase()
                        !it.isDirectory && (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".gif"))
                    }
                    if (firstImage != null) {
                        zip.getInputStream(firstImage).use { input ->
                            coverBytes = input.readBytes()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error extracting PMLZ cover: {}", e.message, e)
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
        val destDir = File(outputDir, "pmlz_temp_$hashCode")
        destDir.mkdirs()

        var pmlFileName: String? = null

        ZipFile(path).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val outFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(outFile).use { output ->
                            BufferedOutputStream(output).use { bos ->
                                input.copyTo(bos)
                            }
                        }
                    }
                    if (entry.name.lowercase().endsWith(".pml")) {
                        pmlFileName = entry.name
                    }
                }
            }
        }

        if (pmlFileName == null) {
            throw java.io.IOException(".pml file missing in PMLZ archive")
        }

        val pmlFile = File(destDir, pmlFileName!!)
        val pmlText = pmlFile.readText(charset("cp1252"))
        val htmlContent = pmlToHtml(pmlText, pmlFile.nameWithoutExtension)

        val outHtmlFile = File(destDir, "index.html")
        outHtmlFile.writeText(htmlContent, Charsets.UTF_8)

        BookContent.HtmlFile(outHtmlFile.absolutePath)
    }

    private fun pmlToHtml(pmlText: String, pmlBaseName: String): String {
        var html = pmlText

        // 1. Handle character entities like \a225 -> char(225)
        val charRegex = Regex("\\\\a(\\d{3})")
        html = html.replace(charRegex) { m ->
            val code = m.groupValues[1].toInt()
            String(byteArrayOf(code.toByte()), charset("cp1252"))
        }

        // 2. Handle images \m="filename.png"
        html = html.replace(Regex("\\\\m=\"(.+?)\"")) { m ->
            val imgFile = m.groupValues[1]
            "<img src=\"${pmlBaseName}_img/$imgFile\" style=\"max-width:100%;\" /><br/>"
        }

        // 3. Handle bold toggle \B
        var boldOpen = false
        html = html.replace(Regex("\\\\B")) {
            boldOpen = !boldOpen
            if (boldOpen) "<b>" else "</b>"
        }

        // 4. Handle italic toggle \x
        var italicOpen = false
        html = html.replace(Regex("\\\\x")) {
            italicOpen = !italicOpen
            if (italicOpen) "<i>" else "</i>"
        }

        // 5. Handle center toggle \c
        var centerOpen = false
        html = html.replace(Regex("\\\\c")) {
            centerOpen = !centerOpen
            if (centerOpen) "<div style=\"text-align:center;\">" else "</div>"
        }

        // 6. Handle anchors \Q="anchor"
        html = html.replace(Regex("\\\\Q=\"(.+?)\"")) { m ->
            "<a name=\"${m.groupValues[1]}\"></a>"
        }

        // 7. Handle links \q="#link" and closing \q
        html = html.replace(Regex("\\\\q=\"(.+?)\"")) { m ->
            "<a href=\"${m.groupValues[1]}\">"
        }
        html = html.replace(Regex("\\\\q\\b")) {
            "</a>"
        }

        // Remove remaining backslash commands we don't care about, e.g. \T="xxx", \fn="xxx"
        html = html.replace(Regex("\\\\[a-zA-Z]+(?:=\".*?\")?")) { "" }

        // Replace newlines with <br/>
        html = html.replace("\n", "<br/>")

        return "<html><head><meta charset='UTF-8'/></head><body>$html</body></html>"
    }
}
