/*
 * Copyright (C) 2020 Nicolas Centa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.com.fenix.bilingualreader.service.japanese

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Spanned
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.MetricAffectingSpan
import android.text.style.ReplacementSpan
import androidx.annotation.IntDef
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.util.LinkedList

open class SuperReplacementSpan @JvmOverloads constructor(@field:Alignment val mAlignment: Int = Alignment.CENTER) : ReplacementSpan() {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(*[Alignment.BEGIN, Alignment.END, Alignment.CENTER, Alignment.JUSTIFIED, Alignment.JIS])
    annotation class Alignment {
        companion object {
            const val BEGIN = 0
            const val END = 1
            const val CENTER = 2
            const val JUSTIFIED = 3
            const val JIS = 4
        }
    }

    class TextSizeInformation(
        val fontMetricsInt: Paint.FontMetricsInt,
        val size: Float,
        val charSequenceSizedElements: List<CharSequenceSizedElement>
    )

    class CharSequenceElement(
        val start: Int,
        val end: Int,
        val replacementSpans: List<ReplacementSpan>?,
        val metricAffectingSpans: List<MetricAffectingSpan>?,
        val characterStyles: List<CharacterStyle>?
    )

    class CharSequenceSizedElement(
        val charSequenceElement: CharSequenceElement,
        val size: Float,
        val textPaint: TextPaint,
        val fontMetricsInt: Paint.FontMetricsInt
    ) {
        var spaceBefore = 0f
        var spaceAfter = 0f
    }

    private fun getCharSequenceElements(text: CharSequence, start: Int, end: Int): List<CharSequenceElement> {
        val textSpanned = if (text is Spanned) text else null
        val replacementSpans = if (textSpanned != null) getSpans(textSpanned, start, end, ReplacementSpan::class.java) else null
        val metricAffectingSpans = if (textSpanned != null) getSpans(textSpanned, start, end, MetricAffectingSpan::class.java) else null
        val characterStyles = if (textSpanned != null) getSpans(textSpanned, start, end, CharacterStyle::class.java) else null
        val textString = text.toString()
        val charSequenceElements = LinkedList<CharSequenceElement>()
        var cursor = start
        while (cursor < end) {
            var nextCursor = textString.offsetByCodePoints(cursor, 1)
            var metricAffectingSpansSub: LinkedList<MetricAffectingSpan>? = null
            var characterStylesSub: LinkedList<CharacterStyle>? = null
            if (textSpanned != null) {
                var replacementSpansSub: LinkedList<ReplacementSpan>? = null
                for (replacementSpan in replacementSpans!!) {
                    val spanStart = textSpanned.getSpanStart(replacementSpan)
                    val spanEnd = textSpanned.getSpanEnd(replacementSpan)
                    if (spanStart >= cursor) {
                        if (replacementSpansSub == null) {
                            replacementSpansSub = LinkedList()
                        }
                        replacementSpansSub.add(replacementSpan)
                        if (spanEnd > nextCursor) {
                            nextCursor = spanEnd
                        }
                    }
                }
                if (replacementSpansSub != null) {
                    charSequenceElements.add(
                        CharSequenceElement(
                            cursor, nextCursor,
                            replacementSpansSub, null, null
                        )
                    )
                    cursor = nextCursor
                    continue
                }
                for (metricAffectingSpan in metricAffectingSpans!!) {
                    val spanStart = textSpanned.getSpanStart(metricAffectingSpan)
                    val spanEnd = textSpanned.getSpanEnd(metricAffectingSpan)
                    if (spanStart <= cursor && spanEnd >= nextCursor) {
                        if (metricAffectingSpansSub == null) {
                            metricAffectingSpansSub = LinkedList()
                        }
                        metricAffectingSpansSub.add(metricAffectingSpan)
                    }
                }
                for (characterStyle in characterStyles!!) {
                    val spanStart = textSpanned.getSpanStart(characterStyle)
                    val spanEnd = textSpanned.getSpanEnd(characterStyle)
                    if (spanStart <= cursor && spanEnd >= nextCursor) {
                        if (characterStylesSub == null) {
                            characterStylesSub = LinkedList()
                        }
                        characterStylesSub.add(characterStyle)
                    }
                }
            }
            charSequenceElements.add(
                CharSequenceElement(
                    cursor, nextCursor,
                    null, metricAffectingSpansSub, characterStylesSub
                )
            )
            cursor = nextCursor
        }
        return charSequenceElements
    }

    fun getTextSize(paint: Paint, text: CharSequence, start: Int, end: Int): TextSizeInformation {
        val charSequenceElements = getCharSequenceElements(text, start, end)
        val fm = Paint.FontMetricsInt()
        val charSequenceSizedElements = LinkedList<CharSequenceSizedElement>()
        var size = 0
        for (charSequenceElement in charSequenceElements) {
            if (charSequenceElement.replacementSpans != null) {
                val replacementSpan = charSequenceElement.replacementSpans[charSequenceElement.replacementSpans.size - 1]
                val fontMetricsInt = Paint.FontMetricsInt()
                val elementSize = replacementSpan.getSize(paint, text, charSequenceElement.start, charSequenceElement.end, fontMetricsInt).toFloat()
                charSequenceSizedElements.add(
                    CharSequenceSizedElement(
                        charSequenceElement,
                        elementSize, TextPaint(paint), fontMetricsInt
                    )
                )
                size += elementSize.toInt()
                mergeFontMetricsInt(fm, fontMetricsInt)
            } else {
                val textPaint = TextPaint(paint)
                if (charSequenceElement.metricAffectingSpans != null) {
                    for (metricAffectingSpan in charSequenceElement.metricAffectingSpans) {
                        metricAffectingSpan.updateMeasureState(textPaint)
                    }
                }
                if (charSequenceElement.characterStyles != null) {
                    for (characterStyle in charSequenceElement.characterStyles) {
                        characterStyle.updateDrawState(textPaint)
                    }
                }
                val fontMetricsInt = textPaint.fontMetricsInt
                val elementSize = textPaint.measureText(text, charSequenceElement.start, charSequenceElement.end)
                charSequenceSizedElements.add(
                    CharSequenceSizedElement(
                        charSequenceElement,
                        elementSize, textPaint, fontMetricsInt
                    )
                )
                size += elementSize.toInt()
                mergeFontMetricsInt(fm, fontMetricsInt)
            }
        }
        return TextSizeInformation(fm, size.toFloat(), charSequenceSizedElements)
    }

    private fun <T> getSpans(text: Spanned, start: Int, end: Int, type: Class<T>): List<T> {
        val list = LinkedList<T>()
        for (span in text.getSpans<T>(start, end, type)) {
            if (span !== this) {
                list.add(span)
            } else if (type == SuperReplacementSpan::class.java) {
                break
            }
        }
        return list
    }

    open fun drawExpanded(
        canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint,
        expandedSpanSize: Float
    ) {
        val textSizeInformation = getTextSize(paint, text, start, end)
        drawText(
            text, textSizeInformation, mAlignment, canvas,
            textSizeInformation.size, x, y,
            top,
            bottom
        )
    }

    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        val textSizeInformation = getTextSize(paint, text, start, end)
        if (fm != null) {
            fm.bottom = textSizeInformation.fontMetricsInt.bottom
            fm.ascent = textSizeInformation.fontMetricsInt.ascent
            fm.top = textSizeInformation.fontMetricsInt.ascent
            fm.descent = textSizeInformation.fontMetricsInt.descent
            fm.leading = textSizeInformation.fontMetricsInt.leading
        }
        return Math.round(textSizeInformation.size)
    }

    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        drawExpanded(canvas, text, start, end, x, top, y, bottom, paint, 0f)
    }

    companion object {
        private fun mergeFontMetricsInt(
            baseFontMetricsInt: Paint.FontMetricsInt,
            newFontMetricsInt: Paint.FontMetricsInt
        ) {
            baseFontMetricsInt.leading = Math.max(baseFontMetricsInt.leading, newFontMetricsInt.leading)
            baseFontMetricsInt.descent = Math.max(baseFontMetricsInt.descent, newFontMetricsInt.descent)
            baseFontMetricsInt.bottom = Math.max(baseFontMetricsInt.bottom, newFontMetricsInt.bottom)
            baseFontMetricsInt.ascent = Math.min(baseFontMetricsInt.ascent, newFontMetricsInt.ascent)
            baseFontMetricsInt.top = Math.min(baseFontMetricsInt.top, newFontMetricsInt.top)
        }

        private fun centerText(
            textSizeInformation: TextSizeInformation,
            size: Float
        ) {
            val charSequenceElementIterator = textSizeInformation.charSequenceSizedElements.iterator()
            var count = 0
            val extraSpace = size - textSizeInformation.size
            while (charSequenceElementIterator.hasNext()) {
                val charSequenceSizedElement = charSequenceElementIterator.next()
                if (count == 0) {
                    charSequenceSizedElement.spaceBefore = extraSpace / 2
                }
                if (!charSequenceElementIterator.hasNext()) {
                    charSequenceSizedElement.spaceAfter = extraSpace / 2
                }
                count++
            }
        }

        private fun alignTextLeft(
            textSizeInformation: TextSizeInformation,
            size: Float
        ) {
            val charSequenceElementIterator = textSizeInformation.charSequenceSizedElements.iterator()
            var count = 0
            val extraSpace = size - textSizeInformation.size
            while (charSequenceElementIterator.hasNext()) {
                val charSequenceSizedElement = charSequenceElementIterator.next()
                if (!charSequenceElementIterator.hasNext()) {
                    charSequenceSizedElement.spaceAfter = extraSpace
                }
                count++
            }
        }

        private fun alignTextRight(
            textSizeInformation: TextSizeInformation,
            size: Float
        ) {
            val charSequenceElementIterator = textSizeInformation.charSequenceSizedElements.iterator()
            var count = 0
            val extraSpace = size - textSizeInformation.size
            while (charSequenceElementIterator.hasNext()) {
                val charSequenceSizedElement = charSequenceElementIterator.next()
                if (count == 0) {
                    charSequenceSizedElement.spaceBefore = extraSpace
                }
                count++
            }
        }

        private fun justifyText(
            textSizeInformation: TextSizeInformation,
            size: Float, jis: Boolean
        ) {
            var divider = 0f
            var count = 0
            if (textSizeInformation.charSequenceSizedElements.size == 1) {
                centerText(textSizeInformation, size)
                return
            }
            var charSequenceElementIterator = textSizeInformation.charSequenceSizedElements.iterator()
            while (charSequenceElementIterator.hasNext()) {
                val charSequenceSizedElement = charSequenceElementIterator.next()
                if (charSequenceElementIterator.hasNext()) {
                    divider += charSequenceSizedElement.size / 2
                }
                if (count != 0) {
                    divider += charSequenceSizedElement.size / 2
                }
                if (jis) {
                    if (!charSequenceElementIterator.hasNext()) {
                        divider += charSequenceSizedElement.size / 2
                    }
                    if (count == 0) {
                        divider += charSequenceSizedElement.size / 2
                    }
                }
                count++
            }
            val extraSpaceUnit = (size - textSizeInformation.size) / divider
            count = 0
            charSequenceElementIterator = textSizeInformation.charSequenceSizedElements.iterator()
            while (charSequenceElementIterator.hasNext()) {
                val charSequenceSizedElement = charSequenceElementIterator.next()
                if (charSequenceElementIterator.hasNext()) {
                    charSequenceSizedElement.spaceAfter = charSequenceSizedElement.size * extraSpaceUnit / 2
                }
                if (count != 0) {
                    charSequenceSizedElement.spaceBefore = charSequenceSizedElement.size * extraSpaceUnit / 2
                }
                if (jis) {
                    if (!charSequenceElementIterator.hasNext()) {
                        charSequenceSizedElement.spaceAfter += charSequenceSizedElement.size * extraSpaceUnit / 2
                    }
                    if (count == 0) {
                        charSequenceSizedElement.spaceBefore += charSequenceSizedElement.size * extraSpaceUnit / 2
                    }
                }
                count++
            }
        }

        private fun drawBackground(
            aCharSequenceSizedElement: CharSequenceSizedElement,
            aCanvas: Canvas,
            aX: Float,
            aY: Int,
            aFirstChar: Boolean,
            aLastChar: Boolean
        ) {
            if (aCharSequenceSizedElement.textPaint.bgColor != 0) {
                val left = if (aFirstChar) aX + aCharSequenceSizedElement.spaceBefore else aX
                val right =
                    if (aLastChar) aX + aCharSequenceSizedElement.spaceBefore + aCharSequenceSizedElement.size else aX + aCharSequenceSizedElement.spaceBefore + aCharSequenceSizedElement.size +
                            aCharSequenceSizedElement.spaceAfter
                val previousColor = aCharSequenceSizedElement.textPaint.color
                val previousStyle = aCharSequenceSizedElement.textPaint.style
                aCharSequenceSizedElement.textPaint.color = aCharSequenceSizedElement.textPaint.bgColor
                aCharSequenceSizedElement.textPaint.style = Paint.Style.FILL
                aCanvas.drawRect(
                    left,
                    (
                            aY + aCharSequenceSizedElement.fontMetricsInt.top).toFloat(),
                    right,
                    (
                            aY + aCharSequenceSizedElement.fontMetricsInt.bottom).toFloat(), aCharSequenceSizedElement.textPaint
                )
                aCharSequenceSizedElement.textPaint.style = previousStyle
                aCharSequenceSizedElement.textPaint.color = previousColor
            }
        }

        @JvmStatic
        fun drawText(
            aText: CharSequence,
            aTextSizeInformation: TextSizeInformation,
            @Alignment aAlignment: Int,
            aCanvas: Canvas,
            aSpanSize: Float,
            aStartX: Float,
            aY: Int,
            aTop: Int,
            aBottom: Int
        ) {
            when (aAlignment) {
                Alignment.BEGIN -> alignTextLeft(aTextSizeInformation, aSpanSize)
                Alignment.CENTER -> centerText(aTextSizeInformation, aSpanSize)
                Alignment.END -> alignTextRight(aTextSizeInformation, aSpanSize)
                Alignment.JUSTIFIED -> justifyText(aTextSizeInformation, aSpanSize, false)
                Alignment.JIS -> justifyText(aTextSizeInformation, aSpanSize, true)
            }
            var cursor = aStartX
            var count = 0
            for (charSequenceSizedElement in aTextSizeInformation.charSequenceSizedElements) {
                drawBackground(
                    charSequenceSizedElement, aCanvas, cursor, aY,
                    cursor == 0f,
                    count == aTextSizeInformation.charSequenceSizedElements.size - 1
                )
                if (charSequenceSizedElement.charSequenceElement.replacementSpans != null &&
                    charSequenceSizedElement.charSequenceElement.replacementSpans.size > 0
                ) {
                    val replacementSpan =
                        charSequenceSizedElement.charSequenceElement.replacementSpans[charSequenceSizedElement.charSequenceElement.replacementSpans.size - 1]
                    if (replacementSpan is SuperReplacementSpan) {
                        replacementSpan.drawExpanded(
                            aCanvas, aText,
                            charSequenceSizedElement.charSequenceElement.start,
                            charSequenceSizedElement.charSequenceElement.end,
                            cursor,
                            aTop, aY, aBottom, charSequenceSizedElement.textPaint,
                            charSequenceSizedElement.spaceBefore + charSequenceSizedElement.size + charSequenceSizedElement.spaceAfter
                        )
                    } else {
                        replacementSpan.draw(
                            aCanvas, aText, charSequenceSizedElement.charSequenceElement.start,
                            charSequenceSizedElement.charSequenceElement.end,
                            cursor + charSequenceSizedElement.spaceBefore,
                            aTop, aY, aBottom, charSequenceSizedElement.textPaint
                        )
                    }
                } else {
                    if (charSequenceSizedElement.spaceBefore != 0f && count != 0) {
                        val spaceSize = charSequenceSizedElement.textPaint.measureText(" ")
                        val scaleX = charSequenceSizedElement.textPaint.textScaleX
                        charSequenceSizedElement.textPaint.textScaleX = charSequenceSizedElement.spaceBefore / spaceSize
                        aCanvas.drawText(
                            " ", 0, 1, cursor, aY.toFloat(),
                            charSequenceSizedElement.textPaint
                        )
                        charSequenceSizedElement.textPaint.textScaleX = scaleX
                    }
                    aCanvas.drawText(
                        aText, charSequenceSizedElement.charSequenceElement.start,
                        charSequenceSizedElement.charSequenceElement.end,
                        cursor + charSequenceSizedElement.spaceBefore, aY.toFloat(), charSequenceSizedElement.textPaint
                    )
                    if (charSequenceSizedElement.spaceAfter != 0f && count != aTextSizeInformation.charSequenceSizedElements.size - 1) {
                        val spaceSize = charSequenceSizedElement.textPaint.measureText(" ")
                        val scaleX = charSequenceSizedElement.textPaint.textScaleX
                        charSequenceSizedElement.textPaint.textScaleX = charSequenceSizedElement.spaceAfter / spaceSize
                        aCanvas.drawText(
                            " ", 0, 1, cursor + charSequenceSizedElement.spaceBefore + charSequenceSizedElement.size,
                            aY.toFloat(), charSequenceSizedElement.textPaint
                        )
                        charSequenceSizedElement.textPaint.textScaleX = scaleX
                    }
                }
                cursor += charSequenceSizedElement.spaceBefore + charSequenceSizedElement.size + charSequenceSizedElement.spaceAfter
                count++
            }
        }
    }
}