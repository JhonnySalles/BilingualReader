package br.com.ebook.extractor

import android.text.TextUtils
import br.com.ebook.core.*
import br.com.ebook.core.EbookSettings
import br.com.ebook.util.IOUtils
import br.com.ebook.foobnix.android.utils.TxtUtils
import br.com.ebook.foobnix.hypen.HypenUtils
import br.com.ebook.foobnix.pdf.info.ExtUtils
import br.com.ebook.foobnix.pdf.info.model.BookCSS
import br.com.ebook.foobnix.pdf.info.wrapper.AppState
import org.slf4j.LoggerFactory
import java.io.*

object TxtBookExtractor : BookExtractor {
    private val LOGGER = LoggerFactory.getLogger(TxtBookExtractor::class.java)

    const val OUT_FB2_XML = "txt.html"
    private val END_CHARS = charArrayOf('.', '!', '?', ';')

    override val supportedFormats: Set<String> = setOf("txt")

    fun formatUB(line: String?): String? {
        var formatted = line
        if (formatted != null && formatted.trim().startsWith("(*)") && TxtUtils.isLastCharEq(formatted, END_CHARS)) {
            formatted = "<b><u>$formatted</u></b>"
        }
        return formatted
    }

    override suspend fun extractMetadata(path: String): Result<BookMetadata> = runCatching {
        // Arquivos TXT brutos não possuem metadados embutidos. 
        // Retornamos metadados básicos inferidos a partir do nome do arquivo.
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
        val prePrefix = if (EbookSettings.isPreText) "pre_" else ""
        val file = File(outputDir, prePrefix + OUT_FB2_XML)
        val encoding = ExtUtils.determineEncoding(FileInputStream(path))

        BufferedReader(InputStreamReader(FileInputStream(path), encoding)).use { input ->
            PrintWriter(BufferedWriter(FileWriter(file))).use { writer ->
                writer.println("<!DOCTYPE html>")
                writer.println("<html>")
                
                if (EbookSettings.isPreText) {
                    writer.println("<head><style>@page{margin:0px 0.5em} pre{margin:0px} {body:margin:0px;}</style></head>")
                } else {
                    writer.println("<head><style>p,p+p{margin:0;}</style></head>")
                }

                writer.println("<body>")

                if (EbookSettings.isPreText) {
                    writer.println("<pre>")
                }

                if (EbookSettings.isLineBreaksText) {
                    writer.println("<p>")
                }

                if (EbookSettings.isAutoHypens) {
                    HypenUtils.applyLanguage(EbookSettings.hypenLang)
                }

                var line: String?
                while (input.readLine().also { line = it } != null) {
                    var outLn: String? = null
                    if (EbookSettings.isPreText) {
                        outLn = retab(line!!, 8)
                        outLn = TextUtils.htmlEncode(outLn)
                        if (TxtUtils.isLineStartEndUpperCase(outLn)) {
                            outLn = "<b>$outLn</b>"
                        }
                    } else {
                        val trimmedLine = line!!.trim()
                        if (EbookSettings.isLineBreaksText) {
                            outLn = if (trimmedLine.isEmpty()) "<br/>" else format(line!!)
                        } else {
                            outLn = when {
                                trimmedLine.isEmpty() -> "<br/>"
                                TxtUtils.isLineStartEndUpperCase(line) -> "<b>${format(line!!)}</b>"
                                line!!.contains("Title:") -> "<b>${format(line!!)}</b>"
                                else -> "<p>${format(line!!)}</p>"
                            }
                        }
                    }
                    writer.println(outLn)
                }

                if (EbookSettings.isLineBreaksText) {
                    writer.println("</p>")
                }

                if (EbookSettings.isPreText) {
                    writer.println("</pre>")
                }

                writer.println("</body></html>")
            }
        }

        BookContent.HtmlFile(file.path, null)
    }

    fun retab(text: String, tabstop: Int): String {
        val input = text.toCharArray()
        val sb = java.lang.StringBuilder()
        var linepos = 0
        for (i in input.indices) {
            val ch = input[i]
            if (ch == '\t') {
                do {
                    sb.append(' ')
                    linepos++
                } while (linepos % tabstop != 0)
            } else {
                sb.append(ch)
                linepos++
            }
        }
        return sb.toString()
    }

    fun format(line: String): String {
        var formatted = line
        try {
            formatted = formatted.replace("\n", "").replace("\r", "")
            formatted = TextUtils.htmlEncode(formatted)
            if (EbookSettings.isAutoHypens) {
                formatted = HypenUtils.applyHypnes(formatted)
            }
            formatted = formatted.trim()
        } catch (e: Exception) {
            LOGGER.error("Error format: {}", e.message, e)
        }
        return formatted
    }
}
