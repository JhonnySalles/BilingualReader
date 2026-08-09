package br.com.fenix.bilingualreader.view.components.book

import android.graphics.Rect
import android.graphics.RectF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TextSelectPopupPositionerTest {

    private val display = Rect(0, 0, 1080, 1920)
    private val popupWidth = 200
    private val popupHeight = 100
    private val margin = 8
    private val textLoc = intArrayOf(0, 100)

    @Test
    fun `places popup above selection when there is space`() {
        val selection = RectF(100f, 400f, 300f, 440f)

        val point = TextSelectPopupPositioner.calculate(
            selectionBounds = selection,
            textViewLocationInWindow = textLoc,
            popupWidth = popupWidth,
            popupHeight = popupHeight,
            visibleDisplayFrame = display,
            margin = margin
        )

        // window Y of selection top = 100 + 400 = 500; popup above = 500 - 100 - 8
        assertEquals(392, point.y)
        // centered on selection: centerX = 200, x = 200 - 100 = 100
        assertEquals(100, point.x)
    }

    @Test
    fun `places popup below selection when near top`() {
        // Selection near the top of the text view so spaceAbove < popupHeight + margin
        val selection = RectF(100f, 0f, 300f, 30f)

        val point = TextSelectPopupPositioner.calculate(
            selectionBounds = selection,
            textViewLocationInWindow = textLoc,
            popupWidth = popupWidth,
            popupHeight = popupHeight,
            visibleDisplayFrame = display,
            margin = margin
        )

        // selection bottom in window = 100 + 30 = 130; below = 130 + 8
        assertEquals(138, point.y)
    }

    @Test
    fun `clamps horizontal position to left edge`() {
        val selection = RectF(0f, 400f, 40f, 440f)

        val point = TextSelectPopupPositioner.calculate(
            selectionBounds = selection,
            textViewLocationInWindow = textLoc,
            popupWidth = popupWidth,
            popupHeight = popupHeight,
            visibleDisplayFrame = display,
            margin = margin
        )

        assertEquals(margin, point.x)
    }

    @Test
    fun `clamps horizontal position to right edge`() {
        val selection = RectF(1000f, 400f, 1070f, 440f)

        val point = TextSelectPopupPositioner.calculate(
            selectionBounds = selection,
            textViewLocationInWindow = textLoc,
            popupWidth = popupWidth,
            popupHeight = popupHeight,
            visibleDisplayFrame = display,
            margin = margin
        )

        assertEquals(display.right - popupWidth - margin, point.x)
    }

    @Test
    fun `clamps vertical position within display frame`() {
        val selection = RectF(100f, 1800f, 300f, 1850f)
        val nearBottomDisplay = Rect(0, 0, 1080, 1900)

        val point = TextSelectPopupPositioner.calculate(
            selectionBounds = selection,
            textViewLocationInWindow = intArrayOf(0, 0),
            popupWidth = popupWidth,
            popupHeight = popupHeight,
            visibleDisplayFrame = nearBottomDisplay,
            margin = margin
        )

        assertTrue(point.y >= nearBottomDisplay.top + margin)
        assertTrue(point.y <= nearBottomDisplay.bottom - popupHeight - margin)
    }

    @Test
    fun `accounts for scrolled text view location in window`() {
        val scrolledLoc = intArrayOf(40, -200)
        val selection = RectF(100f, 500f, 300f, 540f)

        val point = TextSelectPopupPositioner.calculate(
            selectionBounds = selection,
            textViewLocationInWindow = scrolledLoc,
            popupWidth = popupWidth,
            popupHeight = popupHeight,
            visibleDisplayFrame = display,
            margin = margin
        )

        // centerX = 40 + 200 = 240; x = 240 - 100 = 140
        assertEquals(140, point.x)
        // selection top in window = -200 + 500 = 300; above = 300 - 100 - 8 = 192
        assertEquals(192, point.y)
    }
}
