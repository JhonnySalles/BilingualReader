package br.com.ebook.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities

object SpineSegmenter {

    const val DEFAULT_MIN_TEXT_CHARS = 200

    sealed class Block {
        data class Boundary(
            val title: String? = null,
            val anchorId: String? = null,
            val forceBreak: Boolean = false
        ) : Block()

        data class HtmlText(
            val html: String,
            val textLength: Int = -1,
            val anchorId: String? = null
        ) : Block()

        data class Image(
            val src: String,
            val alt: String = "",
            val role: ImageSizeUtils.ImageRole? = null,
            val bytes: ByteArray? = null,
            val dims: Pair<Int, Int>? = null
        ) : Block()
    }

    /**
     * Converts raw body HTML into strict, valid XHTML with self-closing tags (<br />, <img />).
     */
    fun toValidXhtml(bodyHtml: String): String {
        if (bodyHtml.isBlank()) return ""
        val doc = Jsoup.parseBodyFragment(bodyHtml)
        doc.outputSettings().apply {
            syntax(Document.OutputSettings.Syntax.xml)
            escapeMode(Entities.EscapeMode.xhtml)
            charset(Charsets.UTF_8)
            prettyPrint(false)
        }
        return doc.body().html()
    }

    /**
     * Segments an ordered sequence of blocks into a List of EpubPackager.ChapterItem.
     * Full-page images become isolated single-page spine items.
     * Inline images remain in the flow.
     * Boundaries close segments only if accumulated text length >= minTextChars.
     */
    fun segment(
        blocks: List<Block>,
        bookTitle: String,
        minTextChars: Int = DEFAULT_MIN_TEXT_CHARS,
        screenW: Int = ImageSizeUtils.DEFAULT_SCREEN_WIDTH,
        screenH: Int = ImageSizeUtils.DEFAULT_SCREEN_HEIGHT,
        minSide: Int = ImageSizeUtils.DEFAULT_MIN_SIDE,
        defaultCssLinks: List<String> = emptyList()
    ): List<EpubPackager.ChapterItem> {
        val chapters = mutableListOf<EpubPackager.ChapterItem>()

        var currentHtml = StringBuilder()
        var currentTextChars = 0
        var currentTitle = ""
        var currentAnchorId: String? = null
        var chapterIndex = 0

        fun flushCurrentChapter() {
            val html = currentHtml.toString().trim()
            if (html.isNotEmpty()) {
                val docFragment = Jsoup.parseBodyFragment(html)
                val hasVisibleText = docFragment.text().trim().isNotEmpty()
                val hasImages = docFragment.select("img, image").isNotEmpty()

                if (hasVisibleText || hasImages) {
                    val cleanXhtml = toValidXhtml(html)
                    if (cleanXhtml.isNotBlank()) {
                        val chapTitle = currentTitle.ifBlank { bookTitle }
                        val chapFile = "chap_${chapterIndex++}.xhtml"
                        chapters.add(
                            EpubPackager.ChapterItem(
                                title = chapTitle,
                                contentHtml = cleanXhtml,
                                fileName = chapFile,
                                anchorId = currentAnchorId,
                                cssLinks = defaultCssLinks
                            )
                        )
                        currentHtml = StringBuilder()
                        currentTextChars = 0
                        currentTitle = ""
                        currentAnchorId = null
                    }
                } else {
                    // Buffer has no visible content (e.g. only <a id="..."></a> or empty tags)
                    // Preserve any anchor IDs for the subsequent chapter or image
                    val anchors = docFragment.select("a[id], a[name]")
                    for (a in anchors) {
                        val id = a.id().ifBlank { a.attr("name") }
                        if (id.isNotBlank() && currentAnchorId == null) {
                            currentAnchorId = id
                        }
                    }
                    currentHtml = StringBuilder()
                    currentTextChars = 0
                }
            } else {
                currentHtml = StringBuilder()
                currentTextChars = 0
            }
        }

        for (block in blocks) {
            when (block) {
                is Block.Boundary -> {
                    if (block.forceBreak || currentTextChars >= minTextChars) {
                        flushCurrentChapter()
                        currentTitle = block.title ?: ""
                        if (!block.anchorId.isNullOrBlank()) {
                            currentAnchorId = block.anchorId
                        }
                    } else {
                        // Accumulate boundary without splitting if under minTextChars
                        if (currentTitle.isBlank() && !block.title.isNullOrBlank()) {
                            currentTitle = block.title
                        }
                        if (currentAnchorId == null && !block.anchorId.isNullOrBlank()) {
                            currentAnchorId = block.anchorId
                        }
                    }
                }

                is Block.HtmlText -> {
                    if (block.html.isNotBlank()) {
                        currentHtml.append(block.html).append("\n")
                        val length = if (block.textLength >= 0) {
                            block.textLength
                        } else {
                            Jsoup.parseBodyFragment(block.html).text().trim().length
                        }
                        currentTextChars += length
                        if (currentAnchorId == null && !block.anchorId.isNullOrBlank()) {
                            currentAnchorId = block.anchorId
                        }
                    }
                }

                is Block.Image -> {
                    val role = block.role ?: when {
                        block.bytes != null -> ImageSizeUtils.classify(block.bytes, screenW, screenH, minSide)
                        block.dims != null -> ImageSizeUtils.classify(block.dims, screenW, screenH, minSide)
                        else -> ImageSizeUtils.ImageRole.INLINE
                    }

                    if (role == ImageSizeUtils.ImageRole.FULL_PAGE) {
                        // Flush any pending text before full-page illustration
                        flushCurrentChapter()

                        // Create isolated full-page illustration spine item
                        val fullPageHtml = """<div class="br-full-page"><img src="${escapeXml(block.src)}" alt="${escapeXml(block.alt)}" /></div>"""
                        val imgFile = "img_page_${chapterIndex++}.xhtml"
                        val imgTitle = block.alt.ifBlank { "Illustration" }
                        chapters.add(
                            EpubPackager.ChapterItem(
                                title = imgTitle,
                                contentHtml = fullPageHtml,
                                fileName = imgFile,
                                anchorId = currentAnchorId,
                                cssLinks = defaultCssLinks
                            )
                        )
                        currentAnchorId = null
                    } else {
                        // Inline image stays within current text segment
                        currentHtml.append("<div class=\"br-inline-wrapper\" style=\"text-align: center; margin: 0.8em 0;\">")
                            .append("<img class=\"br-inline\" src=\"")
                            .append(escapeXml(block.src))
                            .append("\" alt=\"")
                            .append(escapeXml(block.alt))
                            .append("\" /></div>\n")
                    }
                }
            }
        }

        flushCurrentChapter()

        return chapters
    }

    fun findResourceBytes(src: String, resourceBytes: Map<String, ByteArray>): ByteArray? {
        val clean = src.trim().trimStart('/')
        resourceBytes[clean]?.let { return it }
        val name = java.io.File(clean).name
        resourceBytes[name]?.let { return it }
        return resourceBytes.entries.firstOrNull {
            it.key.equals(clean, ignoreCase = true) || java.io.File(it.key).name.equals(name, ignoreCase = true)
        }?.value
    }

    /**
     * Segments an HTML Document (such as from HTMLZ or RB) into ChapterItems.
     */
    fun segmentHtmlDocument(
        doc: Document,
        resourceBytes: Map<String, ByteArray>,
        bookTitle: String,
        coverFileNameToSkip: String? = null,
        minTextChars: Int = DEFAULT_MIN_TEXT_CHARS,
        screenW: Int = ImageSizeUtils.DEFAULT_SCREEN_WIDTH,
        screenH: Int = ImageSizeUtils.DEFAULT_SCREEN_HEIGHT,
        minSide: Int = ImageSizeUtils.DEFAULT_MIN_SIDE,
        defaultCssLinks: List<String> = emptyList()
    ): List<EpubPackager.ChapterItem> {
        val calibreDivs = doc.select("div.calibre")
        if (calibreDivs.isNotEmpty()) {
            val blocks = mutableListOf<Block>()
            for (div in calibreDivs) {
                val anchorId = div.id().ifBlank { div.attr("id") }
                blocks.add(Block.Boundary(anchorId = anchorId.ifBlank { null }))

                val imgs = div.select("img")
                val fullPageImgs = imgs.filter { img ->
                    val src = img.attr("src").ifEmpty { img.attr("SRC") }
                    val bytes = findResourceBytes(src, resourceBytes)
                    ImageSizeUtils.classify(bytes, screenW, screenH, minSide) == ImageSizeUtils.ImageRole.FULL_PAGE
                }

                if (fullPageImgs.isEmpty()) {
                    for (img in imgs) {
                        img.addClass("br-inline")
                    }
                    blocks.add(Block.HtmlText(div.outerHtml()))
                } else if (div.text().trim().isEmpty() && fullPageImgs.size == 1 && imgs.size == 1) {
                    val img = fullPageImgs.first()
                    val src = img.attr("src").ifEmpty { img.attr("SRC") }
                    blocks.add(Block.Image(src = src, alt = img.attr("alt"), role = ImageSizeUtils.ImageRole.FULL_PAGE))
                } else {
                    for (child in div.children()) {
                        val childFullImgs = child.select("img").filter { img ->
                            val src = img.attr("src").ifEmpty { img.attr("SRC") }
                            val bytes = findResourceBytes(src, resourceBytes)
                            ImageSizeUtils.classify(bytes, screenW, screenH, minSide) == ImageSizeUtils.ImageRole.FULL_PAGE
                        }
                        if (childFullImgs.isNotEmpty() && child.text().trim().isEmpty()) {
                            for (img in childFullImgs) {
                                val src = img.attr("src").ifEmpty { img.attr("SRC") }
                                blocks.add(Block.Image(src = src, alt = img.attr("alt"), role = ImageSizeUtils.ImageRole.FULL_PAGE))
                            }
                        } else {
                            blocks.add(Block.HtmlText(child.outerHtml()))
                        }
                    }
                }
            }
            return segment(blocks, bookTitle, minTextChars, screenW, screenH, minSide, defaultCssLinks)
        }

        // Flat HTML structure (like RB)
        val blocks = mutableListOf<Block>()
        var coverSkipped = false
        val coverNameOnly = coverFileNameToSkip?.let { java.io.File(it).name.lowercase() }

        for (child in doc.body().children()) {
            val tagName = child.tagName().lowercase()
            val isHeading = tagName in listOf("h1", "h2", "h3")
            val imgs = child.select("img")

            if (isHeading) {
                val anchorId = child.id().ifBlank { child.select("a[name], a[id]").attr("name") }
                blocks.add(Block.Boundary(title = child.text().trim(), anchorId = anchorId.ifBlank { null }))
                blocks.add(Block.HtmlText(child.outerHtml()))
            } else if (imgs.isNotEmpty()) {
                var hasFullPage = false
                for (img in imgs) {
                    val src = img.attr("src").ifEmpty { img.attr("SRC") }
                    val cleanSrc = java.io.File(src).name.lowercase()

                    if (!coverSkipped && coverNameOnly != null && cleanSrc == coverNameOnly) {
                        coverSkipped = true
                        continue
                    }

                    val bytes = findResourceBytes(src, resourceBytes)
                    val role = ImageSizeUtils.classify(bytes, screenW, screenH, minSide)
                    if (role == ImageSizeUtils.ImageRole.FULL_PAGE) {
                        hasFullPage = true
                        blocks.add(Block.Image(src = src, alt = img.attr("alt"), role = ImageSizeUtils.ImageRole.FULL_PAGE))
                    }
                }

                if (!hasFullPage) {
                    for (img in imgs) {
                        img.addClass("br-inline")
                    }
                    blocks.add(Block.HtmlText(child.outerHtml()))
                } else if (child.text().trim().isNotEmpty()) {
                    val clone = child.clone()
                    for (img in clone.select("img")) {
                        val src = img.attr("src").ifEmpty { img.attr("SRC") }
                        val bytes = findResourceBytes(src, resourceBytes)
                        if (ImageSizeUtils.classify(bytes, screenW, screenH, minSide) == ImageSizeUtils.ImageRole.FULL_PAGE) {
                            img.remove()
                        }
                    }
                    if (clone.text().trim().isNotEmpty()) {
                        blocks.add(Block.HtmlText(clone.outerHtml()))
                    }
                }
            } else {
                blocks.add(Block.HtmlText(child.outerHtml()))
            }
        }

        return segment(blocks, bookTitle, minTextChars, screenW, screenH, minSide, defaultCssLinks)
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
