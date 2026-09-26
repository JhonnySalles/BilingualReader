package br.com.fenix.bilingualreader.service.tracker

import br.com.fenix.bilingualreader.model.entity.ComicInfo
import br.com.fenix.bilingualreader.model.entity.Track
import java.util.regex.Pattern

data class ParsedFileInfo(
    val cleanTitle: String,
    val volume: Int?,
    val chapter: Float?
)

object TrackerMatcher {

    // Regex para identificar e extrair volume e capítulo do nome do arquivo
    // Exemplos: "Nome - Vol 01.rar", "Nome - Volume 01.rar", "Nome, Vol. 01.rar", "Nome Vol. 01.rar", "Nome c05.cbz", "Nome - Ch. 12.zip"
    private val VOLUME_REGEX = Regex("(?i)(?:^|[\\s,_-]|vol(?:ume)?\\.?)[\\s._-]*vol(?:ume)?\\.?\\s*(\\d+)")
    private val CHAPTER_REGEX = Regex("(?i)(?:^|[\\s,_-]|ch(?:apter)?\\.?|cap(?:[íi]tulo)?\\.?|c)[\\s._-]*(\\d+(?:\\.\\d+)?)")

    /**
     * Limpa o nome do arquivo removendo extensões, tags entre [], (), volumes e capítulos para obter o título base limpo.
     */
    fun parseFileName(fileName: String): ParsedFileInfo {
        var baseName = fileName

        // Remove extensão se presente
        if (baseName.contains(".")) {
            baseName = baseName.substringBeforeLast(".")
        }

        var extractedVolume: Int? = null
        var extractedChapter: Float? = null

        // Tentar extrair volume
        val volMatch = Regex("(?i)(?:vol(?:ume)?\\.?)[\\s._-]*(\\d+)", RegexOption.IGNORE_CASE).find(baseName)
        if (volMatch != null) {
            extractedVolume = volMatch.groupValues[1].toIntOrNull()
        } else {
            // Tenta achar padrões como "- 01" ou "v01"
            val vMatch = Regex("(?i)\\bv(\\d+)\\b").find(baseName)
            if (vMatch != null) {
                extractedVolume = vMatch.groupValues[1].toIntOrNull()
            }
        }

        // Tentar extrair capítulo
        val chMatch = Regex("(?i)(?:ch(?:apter)?\\.?|cap(?:[íi]tulo)?\\.?|\\bc)[\\s._-]*(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE).find(baseName)
        if (chMatch != null) {
            extractedChapter = chMatch.groupValues[1].toFloatOrNull()
        }

        // Limpeza do título base:
        // 1. Remove tags como [Scan], (Digital), etc.
        var cleanTitle = baseName.replace(Regex("\\[[^\\]]*\\]"), "")
            .replace(Regex("\\([^\\)]*\\)"), "")

        // 2. Remove variações de volume: " - Vol 01", " - Volume 01", ", Vol. 01", " Vol. 01", " Volume 01", " v01"
        cleanTitle = cleanTitle.replace(Regex("(?i)(?:[-_,\\s]+)?\\b(?:vol(?:ume)?\\.?|v)\\s*\\d+", RegexOption.IGNORE_CASE), "")

        // 3. Remove variações de capítulo: " - Ch 01", " - Chapter 01", " c01", " cap 01"
        cleanTitle = cleanTitle.replace(Regex("(?i)(?:[-_,\\s]+)?\\b(?:ch(?:apter)?\\.?|cap(?:[íi]tulo)?\\.?|c)\\s*\\d+(?:\\.\\d+)?", RegexOption.IGNORE_CASE), "")

        // 4. Limpeza final de pontuação residual nas pontas e múltiplos espaços
        cleanTitle = cleanTitle.trim().trim('-', '_', ',', '.').replace(Regex("\\s+"), " ").trim()

        return ParsedFileInfo(
            cleanTitle = cleanTitle,
            volume = extractedVolume,
            chapter = extractedChapter
        )
    }

    /**
     * Tenta extrair o ID do MAL de um ComicInfo através do Web (URL do myanimelist), Notes ou ScanInformation.
     */
    fun extractMalIdFromComicInfo(comicInfo: ComicInfo?): Long? {
        if (comicInfo == null) return null

        // 1. Checar campo Web: "https://myanimelist.net/manga/12345/..."
        comicInfo.web?.let { web ->
            val match = Regex("(?i)myanimelist\\.net/manga/(\\d+)").find(web)
            if (match != null) {
                return match.groupValues[1].toLongOrNull()
            }
        }

        // 2. Checar campo Notes: "mal:12345" ou "myanimelist: 12345"
        comicInfo.notes?.let { notes ->
            val match = Regex("(?i)(?:mal|myanimelist)[\\s:_=-]*(\\d+)").find(notes)
            if (match != null) {
                return match.groupValues[1].toLongOrNull()
            }
        }

        // 3. Checar campo ScanInformation
        comicInfo.scanInformation?.let { scanInfo ->
            val match = Regex("(?i)(?:mal|myanimelist)[\\s:_=-]*(\\d+)").find(scanInfo)
            if (match != null) {
                return match.groupValues[1].toLongOrNull()
            }
        }

        return null
    }

    /**
     * Localiza o Track correspondente seguindo a hierarquia definida:
     * 1. ID do MAL no ComicInfo
     * 2. Título / Séries no ComicInfo comparado com o título ou regex do Track
     * 3. Nome do arquivo limpo comparado com o título ou regex do Track
     */
    fun matchTrack(
        tracks: List<Track>,
        comicInfo: ComicInfo?,
        fileName: String
    ): Track? {
        if (tracks.isEmpty()) return null

        // 1º Nível: MAL ID direto do ComicInfo
        val malId = extractMalIdFromComicInfo(comicInfo)
        if (malId != null) {
            val byMal = tracks.firstOrNull { it.malId == malId }
            if (byMal != null) return byMal
        }

        // 2º Nível: Título ou Series do ComicInfo
        val comicTitle = comicInfo?.series?.trim()?.ifEmpty { null } ?: comicInfo?.title?.trim()?.ifEmpty { null }
        if (!comicTitle.isNullOrEmpty()) {
            val byComicTitle = tracks.firstOrNull { track ->
                matchesTitleOrRegex(track, comicTitle)
            }
            if (byComicTitle != null) return byComicTitle
        }

        // 3º Nível: Nome do arquivo (limpeza por regex)
        val parsedFile = parseFileName(fileName)
        if (parsedFile.cleanTitle.isNotEmpty()) {
            val byFileName = tracks.firstOrNull { track ->
                matchesTitleOrRegex(track, parsedFile.cleanTitle) || matchesTitleOrRegex(track, fileName)
            }
            if (byFileName != null) return byFileName
        }

        return null
    }

    fun matches(titleRegex: String, title: String, testString: String): Boolean {
        if (title.isNotBlank() && title.equals(testString, ignoreCase = true)) {
            return true
        }

        if (titleRegex.isNotBlank()) {
            try {
                val pattern = Pattern.compile(titleRegex, Pattern.CASE_INSENSITIVE)
                if (pattern.matcher(testString).find()) {
                    return true
                }
            } catch (e: Exception) {
                if (testString.contains(titleRegex, ignoreCase = true)) {
                    return true
                }
            }
        }

        if (title.isNotBlank() && testString.contains(title, ignoreCase = true)) {
            return true
        }

        return false
    }

    private fun matchesTitleOrRegex(track: Track, testString: String): Boolean {
        return matches(track.titleRegex, track.title, testString)
    }
}

