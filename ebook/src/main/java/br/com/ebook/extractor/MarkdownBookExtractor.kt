package br.com.ebook.extractor

import br.com.ebook.core.*
import org.slf4j.LoggerFactory
import java.io.File

object MarkdownBookExtractor : BookExtractor {
    private val LOGGER = LoggerFactory.getLogger(MarkdownBookExtractor::class.java)

    override val supportedFormats: Set<String> = setOf("md", "markdown")

    override suspend fun extractMetadata(path: String): Result<BookMetadata> = runCatching {
        var title = ""
        var author = ""
        try {
            val file = File(path)
            if (file.exists()) {
                val lines = file.readLines()
                var inFrontmatter = false
                var count = 0
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed == "---") {
                        count++
                        inFrontmatter = count == 1
                        if (count > 2) break
                        continue
                    }
                    if (inFrontmatter) {
                        val parts = trimmed.split(":", limit = 2)
                        if (parts.size == 2) {
                            val key = parts[0].trim().lowercase()
                            val value = parts[1].trim().removeSurrounding("\"").removeSurrounding("'")
                            if (key == "title") {
                                title = value
                            } else if (key == "author") {
                                author = value
                            }
                        }
                    } else if (count >= 2 || count == 0) {
                        // Fora do frontmatter, se achar um heading #, usa como título fallback
                        if (title.isBlank() && trimmed.startsWith("# ")) {
                            title = trimmed.removePrefix("# ").trim()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error reading Markdown metadata: {}", e.message, e)
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
        null
    }

    override suspend fun extractOverview(path: String): Result<String> = runCatching {
        ""
    }

    override suspend fun extractFooterNotes(path: String): Result<Map<String, String>> = runCatching {
        emptyMap()
    }

    override suspend fun extractContent(path: String, outputDir: String): Result<BookContent> = runCatching {
        val outHtmlFile = File(outputDir, "markdown-converted.html")
        val file = File(path)
        val lines = file.readLines()

        val htmlBuilder = StringBuilder()
        htmlBuilder.append("<html><head><meta charset=\"UTF-8\"/></head><body>")

        var inFrontmatter = false
        var frontmatterCount = 0
        var inList = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed == "---" && frontmatterCount < 2) {
                frontmatterCount++
                inFrontmatter = frontmatterCount == 1
                continue
            }
            if (inFrontmatter) {
                continue
            }

            if (trimmed.isEmpty()) {
                if (inList) {
                    htmlBuilder.append("</ul>")
                    inList = false
                }
                continue
            }

            // List item check
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
                if (!inList) {
                    htmlBuilder.append("<ul>")
                    inList = true
                }
                val rawText = trimmed.substring(2).trim()
                htmlBuilder.append("<li>").append(formatInline(rawText)).append("</li>")
                continue
            }

            if (inList) {
                htmlBuilder.append("</ul>")
                inList = false
            }

            // Headings check
            if (trimmed.startsWith("# ")) {
                htmlBuilder.append("<h1>").append(formatInline(trimmed.substring(2).trim())).append("</h1>")
            } else if (trimmed.startsWith("## ")) {
                htmlBuilder.append("<h2>").append(formatInline(trimmed.substring(3).trim())).append("</h2>")
            } else if (trimmed.startsWith("### ")) {
                htmlBuilder.append("<h3>").append(formatInline(trimmed.substring(4).trim())).append("</h3>")
            } else if (trimmed.startsWith("> ")) {
                htmlBuilder.append("<blockquote>").append(formatInline(trimmed.substring(2).trim())).append("</blockquote>")
            } else {
                htmlBuilder.append("<p>").append(formatInline(trimmed)).append("</p>")
            }
        }

        if (inList) {
            htmlBuilder.append("</ul>")
        }

        htmlBuilder.append("</body></html>")
        outHtmlFile.parentFile?.mkdirs()
        outHtmlFile.writeText(htmlBuilder.toString())

        BookContent.HtmlFile(outHtmlFile.absolutePath)
    }

    private fun formatInline(text: String): String {
        var formatted = text
        // Negrito: **text**
        formatted = formatted.replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")
        // Itálico: *text*
        formatted = formatted.replace(Regex("\\*(.*?)\\*"), "<i>$1</i>")
        // Itálico: _text_
        formatted = formatted.replace(Regex("_(.*?)_"), "<i>$1</i>")
        return formatted
    }
}
