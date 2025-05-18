package br.com.fenix.bilingualreader.service.parses.manga

import br.com.fenix.bilingualreader.model.entity.ComicInfo
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import org.jsoup.Jsoup
import org.kxml2.io.KXmlParser
import org.kxml2.kdom.Document
import org.kxml2.kdom.Element
import org.kxml2.kdom.Node
import org.slf4j.LoggerFactory
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile


class EpubParse : Parse {

    private val mLOGGER = LoggerFactory.getLogger(EpubParse::class.java)

    private var mZipFile: ZipFile? = null
    private var mEntries = ArrayList<ZipEntry>()
    private var mPages =  ArrayList<ZipEntry>()
    private var mOpf: ZipEntry? = null
    private val mChapters = mutableMapOf<String, Int>()

    override fun parse(file: File?) {
        mZipFile = ZipFile(file?.absolutePath, StandardCharsets.UTF_8)

        mEntries = ArrayList()
        val opf = mZipFile!!.entries()
        while (opf.hasMoreElements()) {
            val ze = opf.nextElement()
            if (ze.isDirectory)
                continue

            mEntries.add(ze)
            if (ze.name.endsWith(".opf", true))
                mOpf = ze
        }

        if (mOpf == null)
            throw Exception("Invalid epub file.")

        val input = mZipFile!!.getInputStream(mOpf)

        try {
            var parser: KXmlParser? = null
            var doc: Document? = null
            val root: Element?
            var kid: Element

            try {
                parser = KXmlParser()

                try {
                    parser.setFeature(KXmlParser.FEATURE_PROCESS_NAMESPACES, true)
                } catch (_: XmlPullParserException) {
                }

                parser.setInput(InputStreamReader(input))

                doc = Document()
                doc.parse(parser)
                parser = null

                root = doc.getRootElement()

                val manifest = mutableMapOf<String, String>()
                var nav : String? = null
                var cover : String? = null

                try {
                    val manifestEl = getElement(root, "manifest")

                    if (manifestEl == null)
                        throw Exception("No manifest tag in OPF")

                    for (i in 0 until manifestEl.childCount) {
                        if (manifestEl.getType(i) !== Node.ELEMENT)
                            continue

                        kid = manifestEl.getElement(i)

                        if (kid.getName().equals("item", ignoreCase = true)) {
                            val id = kid.getAttributeValue(KXmlParser.NO_NAMESPACE, "id")
                            val href = kid.getAttributeValue(KXmlParser.NO_NAMESPACE, "href")

                            if (id != null && href != null) {
                                manifest.put(id, href)

                                val properties = kid.getAttributeValue(KXmlParser.NO_NAMESPACE, "properties")
                                if (properties != null) {
                                    when (properties.lowercase().trim()) {
                                        "cover-image" -> cover = href
                                        "nav" -> nav = href
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    throw Exception("Couldn't parse manifest", e)
                }

                mPages.clear()
                mChapters.clear()
                if (cover != null) {
                    try {
                        val hef = cover
                        val page = mEntries.find { it.name.endsWith(hef, ignoreCase = true) }
                        if (page != null) {
                            if (FileUtil.isHtml(page.name)) {
                                val document = Jsoup.parse(mZipFile!!.getInputStream(page), "UTF-8", "")
                                var images = document.getElementsByTag("image")
                                if (images.isEmpty())
                                    images = document.getElementsByTag("img")

                                if (images.isNotEmpty()) {
                                    val name = getImageRef(images[0])
                                    if (name.isNotEmpty()) {
                                        val page = mEntries.find { it.name.endsWith(name, ignoreCase = true) }
                                        if (page != null)
                                            mPages.add(page)
                                    }
                                }
                            } else if (FileUtil.isImage(page.name))
                                mPages.add(page)
                        }
                    } catch (e: Exception) {
                        cover = null
                        mLOGGER.error("Error to load cover", e)
                    }
                }

                try {
                    val spine = getElement(root, "spine")
                    if (spine == null)
                        throw Exception("No spine tag in OPF")

                    val pages = mutableMapOf<String, Int>()
                    val addImage = { href: String, image: org.jsoup.nodes.Element ->
                        val name = getImageRef(image)
                        if (name.isNotEmpty()) {
                            val page = mEntries.find { it.name.endsWith(name, ignoreCase = true) }
                            if (page != null) {
                                mPages.add(page)
                                pages[href] = mPages.indexOf(page)
                            }
                        }
                    }

                    var isCover = cover != null
                    for (i in 0 until spine.childCount) {
                        if (spine.getType(i) !== Node.ELEMENT)
                            continue

                        kid = spine.getElement(i)

                        if (kid.getName().equals("itemref", ignoreCase = true)) {
                            val idref = kid.getAttributeValue(KXmlParser.NO_NAMESPACE, "idref")
                            if (idref != null) {
                                val href = manifest[idref]
                                if (href != null) {
                                    if (isCover && href.equals(cover, ignoreCase = true)) {
                                        isCover = false
                                        pages[href] = 0
                                        continue
                                    }

                                    val entry = mEntries.find { it.name.endsWith(href, ignoreCase = true) } ?: continue
                                    if (FileUtil.isHtml(entry.name)) {
                                        val document = Jsoup.parse(mZipFile!!.getInputStream(entry), "UTF-8", "")

                                        for (image in document.getElementsByTag("image"))
                                            addImage(href, image)

                                        for (image in document.getElementsByTag("img"))
                                            addImage(href, image)
                                    } else if (FileUtil.isImage(entry.name)) {
                                        mPages.add(entry)
                                        pages[href] = mPages.indexOf(entry)
                                    }
                                }
                            }
                        }
                    }

                    if (mPages.isEmpty())
                        throw Exception("No pages found in opf")

                    if (nav != null)
                        try {
                            val navigation = mEntries.find { it.name.endsWith(nav, ignoreCase = true) }
                            if (navigation != null) {
                                val document = Jsoup.parse(mZipFile!!.getInputStream(navigation), "UTF-8", "")
                                for (element in document.getElementsByAttribute("epub:type")) {
                                    if (element.attr("epub:type").equals("toc", ignoreCase = true)) {
                                        for (item in element.getElementsByTag("li")) {
                                            val href = item.getElementsByTag("a").attr("href")
                                            if (href.isNotEmpty() && pages.containsKey(href))
                                                mChapters.put(item.text(), pages[href]!!)
                                        }
                                        break
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            mLOGGER.error("Error to load chapters from navigation", e)
                        }
                } catch (e: Exception) {
                    throw Exception("Couldn't load chapters", e)
                }
            } catch (e: XmlPullParserException) {
                parser = null
                doc = null
                throw Exception("The opf file is invalid", e)
            }
        } finally {
            input.close()
        }
    }

    private fun getElement(node: Node, name: String): Element? {
        for (i in 0 until node.childCount) {
            if (node.getType(i) !== Node.ELEMENT)
                continue

            val element: Element = node.getElement(i)
            if (name.equals(element.getName(), ignoreCase = true))
                return element
        }

        return null
    }

    private fun getText(kid: Element): String {
        var res: String? = null

        try {
            res = kid.getText(0)
        } catch (_: Exception) {
        }

        if (res == null)
            return ""
        return res
    }

    private fun getImageRef(image: org.jsoup.nodes.Element) : String {
        var name = image.attr("src")
        if (name.isEmpty())
            name = image.attr("xlink:href")

        if (name.isNotEmpty()) {
            if (name.contains("/"))
                name = name.substringAfter("/", name)
            else if (name.contains("\\"))
                name = name.substringAfter("\\", name)
            else if (name.contains("../"))
                name = name.substringAfter("../", name)
            else if (name.contains("..\\"))
                name = name.substringAfter("..\\", name)
        }

        return name
    }

    override fun numPages(): Int {
        return mPages.size
    }

    override fun getSubtitles(): List<String> {
        return arrayListOf<String>()
    }

    override fun hasSubtitles(): Boolean {
        return false
    }

    override fun getSubtitlesNames(): Map<String, Int> {
        return mutableMapOf<String, Int>()
    }

    private fun getName(entry: ZipEntry): String {
        return entry.name
    }

    override fun getPagePath(num: Int): String? {
        if (mPages.size < num)
            return null
        return getName(mPages[num])
    }

    override fun getPagePaths(): Map<String, Int> = mChapters

    override fun getChapters(): IntArray = getPagePaths().filter { it.value != 0 }.map { it.value }.toIntArray()

    override fun isComicInfo(): Boolean = mOpf != null

    override fun getComicInfo(): ComicInfo? {
        return if (isComicInfo()) {
            var comic : ComicInfo? = null
            val input = mZipFile!!.getInputStream(mOpf)
            try {
                var parser: KXmlParser? = null
                var doc: Document? = null
                val root: Element?
                var kid: Element

                try {
                    parser = KXmlParser()

                    try {
                        parser.setFeature(KXmlParser.FEATURE_PROCESS_NAMESPACES, true)
                    } catch (_: XmlPullParserException) {
                    }

                    parser.setInput(InputStreamReader(input))

                    doc = Document()
                    doc.parse(parser)
                    parser = null

                    root = doc.getRootElement()

                    var metadata: Element? = getElement(root, "metadata")
                    val dcmetadata: Element? = getElement(metadata!!, "dc-metadata")

                    if (dcmetadata != null)
                        metadata = dcmetadata

                    if (metadata != null) {
                        comic = ComicInfo()
                        for (i in 0 until metadata.childCount) {
                            if (metadata.getType(i) !== Node.ELEMENT)
                                continue

                            kid = metadata.getElement(i)

                            if (kid.getName().equals("title", ignoreCase = true)) {
                                val text = getText(kid)
                                if (text.isNotEmpty())
                                    comic.title = text
                                continue
                            }

                            if (kid.getName().equals("creator", ignoreCase = true)) {
                                val text = getText(kid)
                                if (text.isNotEmpty())
                                    comic.writer = text
                                continue
                            }

                            if (kid.getName().equals("publisher", ignoreCase = true)) {
                                val text = getText(kid)
                                if (text.isNotEmpty())
                                    comic.publisher = text
                                continue
                            }

                            if (kid.getName().equals("subject", ignoreCase = true)) {
                                val text = getText(kid)
                                if (text.isNotEmpty())
                                    comic.genre = text
                                continue
                            }

                            if (kid.getName().equals("date", ignoreCase = true)) {
                                val text = getText(kid)
                                if (text.isEmpty())
                                    continue

                                try {
                                    val date = if (text.contains("T"))
                                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(text)
                                    else {
                                        try {
                                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(text)
                                        } catch (e: java.lang.Exception) {
                                            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).parse(text)
                                        }
                                    }

                                    if (date != null) {
                                        comic.year = date.year + 1900
                                        comic.month = date.month + 1
                                        comic.day = date.date
                                    }
                                } catch (_: java.lang.Exception) {
                                }
                                continue
                            }

                            if (kid.getName().equals("language", ignoreCase = true)) {
                                var language = getText(kid)
                                if (language.length > 2)
                                    language = language.substring(0, 2)

                                if (language.isNotEmpty())
                                    comic.languageISO = language
                                continue
                            }

                            if (kid.getName().equals("meta", ignoreCase = true)) {
                                val nameAttr = kid.getAttributeValue(null, "name")
                                if ("calibre:series" == nameAttr)
                                    comic.series = kid.getAttributeValue(null, "content")
                                else if ("calibre:series_index" == nameAttr) {
                                    var number = kid.getAttributeValue(null, "content")
                                    if (number != null)
                                        number = number.replace(".0", "")

                                    comic.volume = if (number != null && number.isEmpty()) number.toInt() else null
                                } else {
                                    val propertyAttr = kid.getAttributeValue(null, "property")
                                    if ("group-position" == propertyAttr)
                                        comic.number = if (getText(kid).isNotEmpty()) getText(kid).toFloat() else null
                                }
                            }
                        }
                        comic.pageCount = mPages.size

                        if (comic.volume == null) {
                            comic.volume = comic.title.let {
                                if (it == null)
                                    return@let 0

                                val title = it.lowercase()
                                var volume = ""
                                if (title.contains(" vol.",true))
                                    volume = title.substringAfterLast(" vol.", title.substringAfter(" vol.")).trim()
                                else if (title.contains("volume", true))
                                    volume = title.substringAfterLast("volume", title.substringAfter("volume")).trim()

                                if (volume.isNotEmpty())
                                    volume = volume.substringBefore(" ", volume).trim()

                                if (volume.isNotEmpty())
                                    try {
                                        volume.toInt()
                                    } catch (_: Exception) {
                                        0
                                    }
                                else
                                    0
                            }
                        }
                    }

                } catch (xppe: XmlPullParserException) {
                    parser = null
                    doc = null
                    comic = null
                }
            } finally {
                input.close()
            }
            comic
        } else
            null
    }

    override fun getPage(num: Int): InputStream {
        return mZipFile!!.getInputStream(mPages[num])
    }

    override fun destroy(isClearCache: Boolean) {
        mZipFile?.close()
    }
}