package br.com.ebook.extractor

import br.com.ebook.core.*
import br.com.ebook.util.IOUtils
import br.com.ebook.foobnix.hypen.HypenUtils
import br.com.ebook.pdf.info.ExtUtils
import br.com.ebook.foobnix.pdf.info.model.BookCSS
import br.com.ebook.Config
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import org.slf4j.LoggerFactory
import java.io.*
import java.util.Locale

object HtmlBookExtractor : BookExtractor {
    private val LOGGER = LoggerFactory.getLogger(HtmlBookExtractor::class.java)

    const val OUT_FB2_XML = "temp.html"

    override val supportedFormats: Set<String> = setOf("html", "htm", "xhtml", "xhtm", "mht", "mhtml")

    override suspend fun extractMetadata(path: String): Result<BookMetadata> = runCatching {
        // Para arquivos HTML brutos, inferimos o título do arquivo
        val file = File(path)
        BookMetadata(
            title = file.nameWithoutExtension,
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
        val file = File(outputDir, OUT_FB2_XML)
        try {
            val encoding = ExtUtils.determineEncoding(FileInputStream(path))
            val htmlBuilder = StringBuilder()

            BufferedReader(InputStreamReader(FileInputStream(path), encoding)).use { input ->
                if (BookCSS.get().isAutoHypens) {
                    HypenUtils.applyLanguage(BookCSS.get().hypenLang)
                }

                var isBody = false
                var line: String?
                while (input.readLine().also { line = it } != null) {
                    val lineLow = line!!.lowercase(Locale.getDefault())
                    if (lineLow.contains("<body")) {
                        isBody = true
                    }
                    if (isBody) {
                        htmlBuilder.append(line)
                    }
                    if (lineLow.contains("</html>")) {
                        break
                    }
                }
            }

            var cleanHtml = Jsoup.clean(htmlBuilder.toString(), Safelist.relaxed().removeTags("img"))

            if (BookCSS.get().isAutoHypens) {
                cleanHtml = HypenUtils.applyHypnes(cleanHtml)
                cleanHtml = Jsoup.clean(cleanHtml, Safelist.relaxed())
            }

            cleanHtml = "<html><head></head><body style='text-align:justify;'><br/>$cleanHtml</body></html>"
            cleanHtml = cleanHtml.replace("<br>", "<br/>")

            FileOutputStream(file).use { out ->
                out.write(cleanHtml.toByteArray(Charsets.UTF_8))
                out.flush()
            }
        } catch (e: Exception) {
            LOGGER.error("Error to extract HTML: {}", e.message, e)
            throw e
        }

        BookContent.HtmlFile(file.path, null)
    }
}
