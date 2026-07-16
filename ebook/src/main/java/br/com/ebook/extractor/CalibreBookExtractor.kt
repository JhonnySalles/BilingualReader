package br.com.ebook.extractor

import br.com.ebook.core.*
import br.com.ebook.util.IOUtils
import br.com.ebook.foobnix.android.utils.TxtUtils
import br.com.ebook.foobnix.pdf.info.wrapper.AppState
import br.com.ebook.foobnix.ext.XmlParser
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import org.slf4j.LoggerFactory
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream
import java.time.LocalDate
import java.util.Locale

object CalibreBookExtractor {
    private val LOGGER = LoggerFactory.getLogger(CalibreBookExtractor::class.java)

    fun isCalibre(path: String): Boolean {
        val rootFolder = File(path).parentFile ?: return false
        val metadata = File(rootFolder, "metadata.opf")
        return metadata.isFile
    }

    fun getBookOverview(path: String): String {
        try {
            val rootFolder = File(path).parentFile ?: return ""
            val metadata = File(rootFolder, "metadata.opf")
            if (!metadata.isFile) return ""

            FileInputStream(metadata).use { fis ->
                val xpp = XmlParser.buildPullParser()
                xpp.setInput(fis, "UTF-8")
                var eventType = xpp.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && xpp.name == "dc:description") {
                        val text = xpp.nextText() ?: ""
                        return Jsoup.clean(text, Safelist.simpleText())
                    }
                    eventType = xpp.next()
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error get Calibre book overview: {}", e.message, e)
        }
        return ""
    }

    fun getBookMetaInformation(path: String): BookMetadata {
        var title = ""
        var author = ""
        var annotation = ""
        var isbn = ""
        var publisher = ""
        var releaseDate: LocalDate? = null
        var sequence = ""
        var sIndex = 0
        var coverImage: ByteArray? = null

        try {
            val rootFolder = File(path).parentFile ?: return BookMetadata("", "")
            val metadata = File(rootFolder, "metadata.opf")
            if (!metadata.isFile) return BookMetadata("", "")

            FileInputStream(metadata).use { fis ->
                val xpp = XmlParser.buildPullParser()
                xpp.setInput(fis, "UTF-8")
                var eventType = xpp.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        when (xpp.name) {
                            "dc:title" -> title = xpp.nextText() ?: ""
                            "dc:creator" -> {
                                var creatorVal = xpp.nextText() ?: ""
                                if (AppState.get().isFirstSurname) {
                                    creatorVal = TxtUtils.replaceLastFirstName(creatorVal) ?: ""
                                }
                                author = if (author.isEmpty()) creatorVal else "$author, $creatorVal"
                            }
                            "dc:description" -> annotation = xpp.nextText() ?: ""
                            "dc:identifier" -> {
                                val content = xpp.nextText() ?: ""
                                if (content.lowercase(Locale.getDefault()).contains("isbn")) {
                                    isbn = content.replace(Regex("\\D"), "")
                                }
                            }
                            "dc:publisher" -> publisher = xpp.nextText() ?: ""
                            "dc:date" -> {
                                val dateStr = xpp.nextText()
                                releaseDate = DateParseUtils.parseFlexibleDate(dateStr)
                            }
                            "meta" -> {
                                val attrName = xpp.getAttributeValue(null, "name")
                                val attrContent = xpp.getAttributeValue(null, "content")
                                val attrProperty = xpp.getAttributeValue(null, "property")

                                if (attrName == "calibre:series" && attrContent != null) {
                                    sequence = attrContent.replace(",", "")
                                }
                                if (attrName == "calibre:series_index" && attrContent != null) {
                                    sIndex = attrContent.toDoubleOrNull()?.toInt() ?: 0
                                }
                                if (attrProperty == "group-position") {
                                    sIndex = xpp.text?.toIntOrNull() ?: 0
                                }
                            }
                            "reference" -> {
                                if (xpp.getAttributeValue(null, "type") == "cover") {
                                    val imgName = xpp.getAttributeValue(null, "href")
                                    if (imgName != null) {
                                        val imgFile = File(rootFolder, imgName)
                                        if (imgFile.isFile) {
                                            try {
                                                FileInputStream(imgFile).use { imgFis ->
                                                    coverImage = IOUtils.run { imgFis.readAllBytes() }
                                                }
                                            } catch (e: Exception) {
                                                LOGGER.error("Error reading Calibre cover image: {}", e.message)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    eventType = xpp.next()
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error get Calibre book meta information: {}", e.message, e)
        }

        return BookMetadata(
            title = title,
            author = author,
            series = sequence,
            genre = "",
            isbn = isbn,
            publisher = publisher,
            releaseDate = releaseDate,
            seriesIndex = sIndex,
            annotation = annotation,
            coverImage = coverImage,
            unzipPath = path
        )
    }
}
