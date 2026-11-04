package br.com.ebook.core

import br.com.ebook.extractor.CalibreBookExtractor
import br.com.ebook.extractor.CbzCbrBookExtractor
import br.com.ebook.extractor.DjvuBookExtractor
import br.com.ebook.extractor.DocBookExtractor
import br.com.ebook.extractor.DocxBookExtractor
import br.com.ebook.extractor.EpubBookExtractor
import br.com.ebook.extractor.Fb2BookExtractor
import br.com.ebook.extractor.HtmlBookExtractor
import br.com.ebook.extractor.HtmlzBookExtractor
import br.com.ebook.extractor.LitBookExtractor
import br.com.ebook.extractor.LrfBookExtractor
import br.com.ebook.extractor.MarkdownBookExtractor
import br.com.ebook.extractor.MobiBookExtractor
import br.com.ebook.extractor.OdtBookExtractor
import br.com.ebook.extractor.PdfBookExtractor
import br.com.ebook.extractor.PmlzBookExtractor
import br.com.ebook.extractor.RbBookExtractor
import br.com.ebook.extractor.RtfBookExtractor
import br.com.ebook.extractor.SnbBookExtractor
import br.com.ebook.extractor.TcrBookExtractor
import br.com.ebook.extractor.TxtBookExtractor
import br.com.ebook.extractor.TxtzBookExtractor
import br.com.ebook.foobnix.android.utils.TxtUtils
import br.com.ebook.foobnix.ext.CacheZipUtils
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Locale

object BookExtractorFactory {
    private val LOGGER = LoggerFactory.getLogger(BookExtractorFactory::class.java)

    fun getExtractor(path: String): BookExtractor? {
        val ext = getFileExtension(path)
        return when (ext) {
            "epub", "kepub" -> EpubBookExtractor
            "fb2" -> Fb2BookExtractor
            "mobi", "azw", "azw3", "azw4", "pdb", "prc" -> MobiBookExtractor
            "txt" -> TxtBookExtractor
            "tcr" -> TcrBookExtractor
            "html", "htm", "xhtml", "xhtm", "mht", "mhtml" -> HtmlBookExtractor
            "htmlz" -> HtmlzBookExtractor
            "txtz" -> TxtzBookExtractor
            "rb" -> RbBookExtractor
            "snb" -> SnbBookExtractor
            "pmlz" -> PmlzBookExtractor
            "rtf" -> RtfBookExtractor
            "pdf", "xps" -> PdfBookExtractor
            "cbz", "cbr" -> CbzCbrBookExtractor
            "doc" -> DocBookExtractor
            "docx" -> DocxBookExtractor
            "odt" -> OdtBookExtractor
            "md", "markdown" -> MarkdownBookExtractor
            "djvu" -> DjvuBookExtractor
            "lrf" -> LrfBookExtractor
            "lit" -> LitBookExtractor
            else -> null
        }
    }

    suspend fun getMetadata(path: String, withPDF: Boolean = true): BookMetadata {
        var metadata = BookMetadata("", "")
        try {
            val file = File(path)
            if (!file.exists()) return metadata

            // Se for arquivo compactado em ZIP, descompacta temporariamente
            val targetPath = if (path.lowercase(Locale.getDefault()).endsWith(".zip")) {
                CacheZipUtils.cacheLock.lock()
                try {
                    val res = CacheZipUtils.extracIfNeed(path, CacheZipUtils.CacheDir.ZipApp)
                    res.unZipPath ?: path
                } finally {
                    CacheZipUtils.cacheLock.unlock()
                }
            } else {
                path
            }

            // 1. Tenta extrair usando o formato Calibre se disponível
            if (CalibreBookExtractor.isCalibre(targetPath)) {
                metadata = CalibreBookExtractor.getBookMetaInformation(targetPath)
                LOGGER.info("Calibre metadata found for: {}", targetPath)
            } else {
                // 2. Tenta obter o extrator correto
                val extractor = getExtractor(targetPath)
                if (extractor != null) {
                    if (extractor != PdfBookExtractor || withPDF) {
                        metadata = extractor.extractMetadata(targetPath).getOrDefault(metadata)
                    }
                }
            }

            // 3. Fallback: Se o título veio em branco, infere a partir do nome do arquivo original
            if (metadata.title.isBlank()) {
                val cleanTitle = MetadataParseUtils.cleanFileTitle(File(path).nameWithoutExtension)
                val pair = TxtUtils.getTitleAuthorByPath(File(path).name)
                metadata = metadata.copy(
                    title = cleanTitle.ifBlank { pair.first ?: "" },
                    author = if (metadata.author.isBlank()) pair.second ?: "" else metadata.author
                )
            }

            // 4. Fallback: Tenta inferir a série e volume se não estiverem setados
            if (metadata.series.isBlank() || metadata.seriesIndex == 0) {
                val inferred = MetadataParseUtils.inferSeriesAndVolume(metadata.title.ifEmpty { File(path).nameWithoutExtension })
                if (metadata.series.isBlank() && inferred.first.isNotBlank()) {
                    metadata = metadata.copy(series = inferred.first)
                }
                if (metadata.seriesIndex == 0 && inferred.second > 0) {
                    metadata = metadata.copy(seriesIndex = inferred.second)
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error to get metadata in factory: {}", e.message, e)
        }
        return metadata
    }

    suspend fun getBookOverview(path: String): String {
        var info = ""
        try {
            val file = File(path)
            if (!file.exists()) return ""

            // Tenta obter do calibre
            if (CalibreBookExtractor.isCalibre(path)) {
                return CalibreBookExtractor.getBookOverview(path)
            }

            val targetPath = if (path.lowercase(Locale.getDefault()).endsWith(".zip")) {
                CacheZipUtils.cacheLock.lock()
                try {
                    val res = CacheZipUtils.extracIfNeed(path, CacheZipUtils.CacheDir.ZipApp)
                    res.unZipPath ?: path
                } finally {
                    CacheZipUtils.cacheLock.unlock()
                }
            } else {
                path
            }

            val extractor = getExtractor(targetPath)
            if (extractor != null) {
                info = extractor.extractOverview(targetPath).getOrDefault("")
            }

            if (info.isNotBlank()) {
                info = Jsoup.clean(info, Safelist.none())
                info = info.replace("&amp;nbsp;", " ").replace("&nbsp;", " ")
            }
        } catch (e: Exception) {
            LOGGER.error("Error to get book overview in factory: {}", e.message, e)
        }
        return info.trim()
    }

    private fun getFileExtension(path: String): String {
        val file = File(path)
        val name = file.name
        val lastDot = name.lastIndexOf('.')
        return if (lastDot > 0 && lastDot < name.length - 1) {
            name.substring(lastDot + 1).lowercase(Locale.getDefault())
        } else {
            ""
        }
    }
}
