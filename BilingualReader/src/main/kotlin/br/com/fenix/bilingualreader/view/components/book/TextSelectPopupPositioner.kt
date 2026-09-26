package br.com.fenix.bilingualreader.view.components.book

import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pure math for placing the custom text-selection popup in window coordinates.
 */
object TextSelectPopupPositioner {

    fun calculate(
        selectionBounds: RectF,
        textViewLocationInWindow: IntArray,
        popupWidth: Int,
        popupHeight: Int,
        visibleDisplayFrame: Rect,
        margin: Int = 0
    ): Point {
        val selLeft = textViewLocationInWindow[0] + selectionBounds.left
        val selTop = textViewLocationInWindow[1] + selectionBounds.top
        val selRight = textViewLocationInWindow[0] + selectionBounds.right
        val selBottom = textViewLocationInWindow[1] + selectionBounds.bottom
        val selCenterX = (selLeft + selRight) / 2f

        val rawX = (selCenterX - popupWidth / 2f).roundToInt()
        val spaceAbove = selTop - visibleDisplayFrame.top
        val rawY = if (spaceAbove >= popupHeight + margin) {
            (selTop - popupHeight - margin).roundToInt()
        } else {
            (selBottom + margin).roundToInt()
        }

        val minX = visibleDisplayFrame.left + margin
        val maxX = visibleDisplayFrame.right - popupWidth - margin
        val minY = visibleDisplayFrame.top + margin
        val maxY = visibleDisplayFrame.bottom - popupHeight - margin

        val point = Point()
        point.x = clamp(rawX, minX, max(minX, maxX))
        point.y = clamp(rawY, minY, max(minY, maxY))
        return point
    }

    private fun clamp(value: Int, minValue: Int, maxValue: Int): Int =
        max(minValue, min(value, maxValue))
}
