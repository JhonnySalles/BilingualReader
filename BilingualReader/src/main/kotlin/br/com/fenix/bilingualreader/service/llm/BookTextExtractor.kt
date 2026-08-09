package br.com.fenix.bilingualreader.service.llm

import android.text.Html
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import org.jsoup.Jsoup

object BookTextExtractor {

    data class ChapterRange(
        val title: String,
        val startPage: Int,
        val endPageExclusive: Int
    )

    fun htmlToPlainText(html: String): String {
        if (html.isBlank()) return ""
        return try {
            Jsoup.parse(html).text().trim()
        } catch (_: Exception) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().trim()
        }
    }

    /**
     * Builds chapter ranges from parse.getChapters() map (title -> 1-based page).
     */
    fun buildChapterRanges(parse: DocumentParse): List<ChapterRange> {
        val chapters = parse.getChapters()
            .map { it.key to it.value }
            .sortedBy { it.second }
        if (chapters.isEmpty()) return emptyList()

        val pageCount = parse.pageCount
        val ranges = mutableListOf<ChapterRange>()
        for (i in chapters.indices) {
            val start = chapters[i].second.coerceAtLeast(1)
            val endExclusive = if (i + 1 < chapters.size) chapters[i + 1].second else pageCount + 1
            ranges.add(ChapterRange(chapters[i].first, start, endExclusive.coerceAtMost(pageCount + 1)))
        }
        return ranges
    }

    fun findCurrentChapterIndex(ranges: List<ChapterRange>, currentPage1Based: Int): Int {
        if (ranges.isEmpty()) return -1
        val idx = ranges.indexOfLast { currentPage1Based >= it.startPage }
        return idx.coerceAtLeast(0)
    }

    /**
     * Returns up to [count] chapters immediately before the current one (not including current),
     * falling back to including current when at the beginning.
     */
    fun selectLastChapters(
        ranges: List<ChapterRange>,
        currentPage1Based: Int,
        count: Int = 3
    ): List<ChapterRange> {
        if (ranges.isEmpty() || count <= 0) return emptyList()
        val currentIndex = findCurrentChapterIndex(ranges, currentPage1Based)
        if (currentIndex < 0) return emptyList()

        val before = ranges.subList(0, currentIndex)
        return if (before.isNotEmpty()) {
            before.takeLast(count)
        } else {
            ranges.take((currentIndex + 1).coerceAtMost(count))
        }
    }

    fun extractChaptersText(
        parse: DocumentParse,
        chapters: List<ChapterRange>,
        maxCharsPerChapter: Int = 4000
    ): String {
        val builder = StringBuilder()
        for (chapter in chapters) {
            val chapterBuilder = StringBuilder()
            for (page in chapter.startPage until chapter.endPageExclusive) {
                val index0 = page - 1
                if (index0 < 0 || index0 >= parse.pageCount) continue
                val html = parse.getPage(index0)?.pageHTMLWithImages.orEmpty()
                val plain = htmlToPlainText(html)
                if (plain.isNotBlank()) {
                    if (chapterBuilder.isNotEmpty()) chapterBuilder.append('\n')
                    chapterBuilder.append(plain)
                }
                if (chapterBuilder.length >= maxCharsPerChapter) break
            }
            val text = LlmPromptBuilder.truncate(chapterBuilder.toString(), maxCharsPerChapter)
            if (text.isNotBlank()) {
                if (builder.isNotEmpty()) builder.append("\n\n")
                builder.append("### ").append(chapter.title).append('\n').append(text)
            }
        }
        return builder.toString()
    }

    fun extractPageText(parse: DocumentParse, page0Based: Int): String {
        if (page0Based < 0 || page0Based >= parse.pageCount) return ""
        return htmlToPlainText(parse.getPage(page0Based)?.pageHTMLWithImages.orEmpty())
    }
}
