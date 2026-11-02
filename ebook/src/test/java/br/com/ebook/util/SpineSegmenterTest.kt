package br.com.ebook.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SpineSegmenterTest {

    @Test
    fun testSegmentingWithFullPageAndInlineImages() {
        val blocks = listOf(
            SpineSegmenter.Block.HtmlText("<p>Short paragraph 1</p>", textLength = 20),
            // Full page illustration
            SpineSegmenter.Block.Image(
                src = "images/insert1.jpg",
                alt = "Insert 1",
                role = ImageSizeUtils.ImageRole.FULL_PAGE
            ),
            // Text after full page
            SpineSegmenter.Block.HtmlText("<p>A longer paragraph with enough text content to read comfortably.</p>", textLength = 65),
            // Inline ornament
            SpineSegmenter.Block.Image(
                src = "images/ornament.png",
                alt = "Ornament",
                role = ImageSizeUtils.ImageRole.INLINE
            ),
            SpineSegmenter.Block.HtmlText("<p>More continuous text continuing the story.</p>", textLength = 45),
            // Boundary with little text -> should NOT split yet (< 200 chars total)
            SpineSegmenter.Block.Boundary(title = "Sub-title", anchorId = "sub1"),
            SpineSegmenter.Block.HtmlText("<p>More text that still doesn't cross 200 chars.</p>", textLength = 48)
        )

        val chapters = SpineSegmenter.segment(blocks, "Book Title", minTextChars = 200)

        // Chapter 0: Short paragraph 1 (flushed before full page)
        // Chapter 1: Isolated full page illustration
        // Chapter 2: The rest of the text merged together (since < 200 chars boundary didn't split)
        assertEquals(3, chapters.size)
        assertTrue(chapters[1].contentHtml.contains("br-full-page"))
        assertTrue(chapters[1].contentHtml.contains("images/insert1.jpg"))
        assertTrue(chapters[2].contentHtml.contains("br-inline"))
        assertTrue(chapters[2].contentHtml.contains("images/ornament.png"))
    }

    @Test
    fun testBoundarySplitsOnlyWhenAboveThreshold() {
        val blocks = listOf(
            SpineSegmenter.Block.Boundary(title = "TOC Line 1"),
            SpineSegmenter.Block.HtmlText("<p>Line 1</p>", textLength = 10),
            SpineSegmenter.Block.Boundary(title = "TOC Line 2"),
            SpineSegmenter.Block.HtmlText("<p>Line 2</p>", textLength = 10),
            SpineSegmenter.Block.Boundary(title = "TOC Line 3"),
            SpineSegmenter.Block.HtmlText("<p>Line 3</p>", textLength = 10),
            SpineSegmenter.Block.Boundary(title = "Chapter 1", forceBreak = true),
            SpineSegmenter.Block.HtmlText("<p>" + "A".repeat(250) + "</p>", textLength = 250),
            SpineSegmenter.Block.Boundary(title = "Chapter 2"),
            SpineSegmenter.Block.HtmlText("<p>" + "B".repeat(250) + "</p>", textLength = 250)
        )

        val chapters = SpineSegmenter.segment(blocks, "Book Title", minTextChars = 200)

        // TOC Lines 1, 2, 3 have only 10 chars each -> accumulated into 1 chapter
        // Then Chapter 1 has 250 chars -> Chapter 2 boundary splits!
        // Expecting:
        // chap 0: TOC lines 1, 2, 3
        // chap 1: Chapter 1
        // chap 2: Chapter 2
        assertEquals(3, chapters.size)
    }
}
