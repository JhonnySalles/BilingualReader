package br.com.ebook.core

import org.jsoup.Jsoup
import org.jsoup.parser.ParseSettings
import org.jsoup.parser.Parser
import java.time.LocalDate

data class CopyrightHeuristics(
    val publisher: String = "",
    val year: Int? = null,
    val author: String = ""
)

object MetadataParseUtils {

    /**
     * Limpa entidades e tags HTML de sinopses e descrições, preservando parágrafos legíveis.
     */
    fun cleanHtmlAnnotation(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val unescaped = Parser.unescapeEntities(raw.trim(), false)
        if (!unescaped.contains("<") && !unescaped.contains(">")) {
            return unescaped.trim()
        }

        return try {
            val doc = Jsoup.parse(unescaped)
            doc.outputSettings().prettyPrint(false)
            val body = doc.body()
            if (body != null) {
                body.select("br").forEach { it.replaceWith(org.jsoup.nodes.TextNode("\n")) }
                body.select("p, div, li").forEach { it.prependChild(org.jsoup.nodes.TextNode("\n")) }
                val lines = body.wholeText().replace("\r", "").lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                lines.joinToString("\n\n").trim()
            } else {
                doc.text().trim()
            }
        } catch (e: Exception) {
            unescaped.replace(Regex("<[^>]+>"), " ").trim()
        }
    }

    /**
     * Normaliza nomes de autores separados por &, 'and' ou ponto-e-vírgula em lista separada por vírgula.
     */
    fun normalizeAuthors(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val tokens = raw.split(Regex("""(?i)\s+(?:&|and)\s+|[;&]"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (tokens.size > 1) {
            return tokens.joinToString(", ")
        }
        return raw.trim()
    }

    /**
     * Remove prefixos numéricos de ordenação de arquivos (ex: "01-86--EIGHTY-SIX..." -> "86--EIGHTY-SIX...").
     */
    fun cleanFileTitle(fileNameWithoutExt: String): String {
        if (fileNameWithoutExt.isBlank()) return ""
        var cleaned = fileNameWithoutExt.trim()

        // Remove prefixos como "01-", "01 - ", "001_", etc. (apenas 1 a 3 dígitos e sem duplo traço para preservar títulos como "86--EIGHTY-SIX")
        cleaned = cleaned.replaceFirst(Regex("""^\d{1,3}\s*[-_](?![-_])\s*"""), "")

        // Se contiver padrão Palm de substituição de pontuação por underscore ("_ Vol_ " -> ", Vol. ")
        if (cleaned.contains("_ Vol_ ", ignoreCase = true)) {
            cleaned = cleaned.replace(Regex("""_\s*Vol_\s*""", RegexOption.IGNORE_CASE), ", Vol. ")
        }

        return cleaned.trim()
    }

    /**
     * Infere nome da série e número do volume a partir do título do livro.
     */
    fun inferSeriesAndVolume(title: String): Pair<String, Int> {
        if (title.isBlank()) return Pair("", 0)
        val clean = title.trim()

        // Padrão 1: ", Vol. 01", "- Volume 2", "(Vol 3)", "Vol. 4"
        val volRegex = Regex("""(?i)(?:,\s*)?(?:[-–—]\s*)?(?:\(|\b)(?:vol\.?|volume)\s*(\d+)(?:\))?""")
        val match = volRegex.findAll(clean).lastOrNull()
        if (match != null) {
            val volNum = match.groupValues[1].toIntOrNull() ?: 0
            val seriesName = clean.substring(0, match.range.first)
                .trim()
                .trimEnd(',', '-', '–', '—', '_', ' ')
                .trim()
            if (seriesName.isNotEmpty()) {
                return Pair(seriesName, volNum)
            }
        }

        // Padrão 2: "#01", "- #2"
        val hashRegex = Regex("""(?i)(?:,\s*)?(?:[-–—]\s*)?#(\d+)""")
        val hashMatch = hashRegex.findAll(clean).lastOrNull()
        if (hashMatch != null) {
            val volNum = hashMatch.groupValues[1].toIntOrNull() ?: 0
            val seriesName = clean.substring(0, hashMatch.range.first)
                .trim()
                .trimEnd(',', '-', '–', '—', '_', ' ')
                .trim()
            if (seriesName.isNotEmpty()) {
                return Pair(seriesName, volNum)
            }
        }

        return Pair("", 0)
    }

    /**
     * Parser unificado e case-insensitive para metadados OPF (EPUB, HTMLZ, TXTZ, Calibre metadata.opf).
     */
    fun parseOpfMetadata(opfXml: String): BookMetadata {
        if (opfXml.isBlank()) return BookMetadata("", "")

        return try {
            val parser = Parser.xmlParser().settings(ParseSettings(false, false))
            val doc = Jsoup.parse(opfXml, "", parser)

            val title = doc.select("dc|title, title").firstOrNull()?.text()?.trim() ?: ""

            // Autores
            val creatorElements = doc.select("dc|creator, creator")
            val author = if (creatorElements.isNotEmpty()) {
                creatorElements.map { it.text().trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString(", ")
            } else {
                normalizeAuthors(doc.select("author").firstOrNull()?.text())
            }

            // Série e Índice
            var series = doc.select("meta[property=belongs-to-collection], meta[name=calibre:series]").let { el ->
                el.attr("content").ifEmpty { el.text() }
            }.trim()

            val seriesIndexStr = doc.select("meta[property=group-position], meta[name=calibre:series_index]").let { el ->
                el.attr("content").ifEmpty { el.text() }
            }.trim()

            var seriesIndex = seriesIndexStr.toFloatOrNull()?.toInt() ?: 0

            if (series.isEmpty() && title.isNotEmpty()) {
                val inferred = inferSeriesAndVolume(title)
                if (inferred.first.isNotEmpty()) {
                    series = inferred.first
                    if (seriesIndex == 0) {
                        seriesIndex = inferred.second
                    }
                }
            }

            // Editora
            val publisher = doc.select("dc|publisher, publisher").firstOrNull()?.text()?.trim() ?: ""

            // ISBN
            var isbn = ""
            val idElements = doc.select("dc|identifier, identifier")
            for (el in idElements) {
                val scheme = (el.attr("opf:scheme").ifEmpty { el.attr("scheme") }).trim()
                val idText = el.text().trim()
                if (scheme.equals("isbn", ignoreCase = true)) {
                    isbn = idText.replace(Regex("[^0-9Xx]"), "")
                    break
                }
            }
            if (isbn.isEmpty()) {
                for (el in idElements) {
                    val idText = el.text().trim()
                    if (idText.contains("isbn", ignoreCase = true)) {
                        val digits = idText.replace(Regex("[^0-9Xx]"), "")
                        if (digits.length in listOf(10, 13)) {
                            isbn = digits
                            break
                        }
                    } else {
                        val digits = idText.replace(Regex("[^0-9Xx]"), "")
                        if ((digits.startsWith("978") || digits.startsWith("979")) && digits.length == 13) {
                            isbn = digits
                            break
                        }
                    }
                }
            }

            // Data de lançamento
            val dateStr = doc.select("dc|date, date").firstOrNull()?.text()?.trim()
            val releaseDate = DateParseUtils.parseFlexibleDate(dateStr)

            // Idioma
            val language = doc.select("dc|language, language").firstOrNull()?.text()?.trim() ?: ""

            // Gêneros / Assuntos
            val genre = doc.select("dc|subject, subject").map { it.text().trim() }
                .filter { it.isNotEmpty() }
                .joinToString(", ")

            // Sinopse
            val descEl = doc.select("dc|description, description").firstOrNull()
            val annotation = if (descEl != null) cleanHtmlAnnotation(descEl.html().ifEmpty { descEl.text() }) else ""

            BookMetadata(
                title = title,
                author = author,
                series = series,
                genre = genre,
                language = language,
                isbn = isbn,
                publisher = publisher,
                releaseDate = releaseDate,
                seriesIndex = seriesIndex,
                annotation = annotation
            )
        } catch (e: Exception) {
            BookMetadata("", "")
        }
    }

    /**
     * Extrai heurísticas de direitos autorais de texto bruto (para .pdb, .tcr, .rb).
     */
    fun extractCopyrightHeuristics(sampleText: String): CopyrightHeuristics {
        if (sampleText.isBlank()) return CopyrightHeuristics()

        var publisher = ""
        var year: Int? = null
        var author = ""

        // Busca ano de copyright: © 2019, Copyright ... 2019, "março de 2019", "Yen On, 2019", etc.
        val yearMatch = Regex("""(?:©|copyright|\(c\)|\uFFFD|publicado|edição|edition|press|on)[^\r\n]{0,60}?\b(19\d\d|20\d\d)\b""", RegexOption.IGNORE_CASE).find(sampleText)
            ?: Regex("""\b(19\d\d|20\d\d)\b""").find(sampleText)
        if (yearMatch != null) {
            val yrStr = yearMatch.groupValues.lastOrNull { it.isNotBlank() } ?: yearMatch.value
            val yr = yrStr.filter { it.isDigit() }.takeLast(4).toIntOrNull()
            if (yr != null && yr in 1900..2099) {
                year = yr
            }
        }

        // Busca editora: "publicado por Yen On" ou "published by Yen On"
        val pubMatch = Regex("""(?i)(?:publicado por|published by)\s+([^,\.\n\r]+)""").find(sampleText)
        if (pubMatch != null) {
            publisher = pubMatch.groupValues[1].trim()
        }

        // Busca autor / arte: "Arte da capa de Shirabii" ou "Escrito por Asato"
        val authorMatch = Regex("""(?i)(?:escrito por|written by|arte da capa de|arte de)\s+([^,\.\n\r]+)""").find(sampleText)
        if (authorMatch != null) {
            author = authorMatch.groupValues[1].trim()
        }

        return CopyrightHeuristics(publisher = publisher, year = year, author = author)
    }
}

