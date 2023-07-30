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
import android.text.TextPaint

class SuperRubySpan // Priority on top for aTextAlignment: if aFurigana contains
// a SuperRubySpan, its aTextAlignment will be applied,
// not aFuriganaAlignment here.
@JvmOverloads constructor(
    private val mFurigana: CharSequence,
    @Alignment aTextAlignment: Int = Alignment.CENTER,
    @field:Alignment @param:Alignment private val mFuriganaAlignment: Int = Alignment.CENTER
) : SuperReplacementSpan(aTextAlignment) {
    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        val textSizeInformation = getTextSize(paint, text, start, end)
        val inheritPaint = TextPaint(paint)

        // For RelativeSizeSpan
        if (textSizeInformation.charSequenceSizedElements.size > 0) {
            inheritPaint.textSize = textSizeInformation.charSequenceSizedElements[0].textPaint.textSize
        }
        val furiganaSizeInformation = getTextSize(
            inheritPaint,
            mFurigana, 0, mFurigana.length
        )
        if (fm != null) {
            fm.bottom = textSizeInformation.fontMetricsInt.bottom
            fm.ascent = textSizeInformation.fontMetricsInt.ascent +
                    (furiganaSizeInformation.fontMetricsInt.ascent - furiganaSizeInformation.fontMetricsInt.descent)
            fm.top = textSizeInformation.fontMetricsInt.ascent +
                    (furiganaSizeInformation.fontMetricsInt.top - furiganaSizeInformation.fontMetricsInt.descent)
            fm.descent = textSizeInformation.fontMetricsInt.descent
            fm.leading = textSizeInformation.fontMetricsInt.leading
        }
        return Math.round(
            Math.max(
                textSizeInformation.size,
                furiganaSizeInformation.size
            )
        )
    }

    override fun drawExpanded(
        canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint,
        expandedSpanSize: Float
    ) {
        val inheritPaint = TextPaint(paint)
        val textSizeInformation = getTextSize(paint, text, start, end)

        // For RelativeSizeSpan
        if (textSizeInformation.charSequenceSizedElements.size > 0) {
            inheritPaint.textSize = textSizeInformation.charSequenceSizedElements[0].textPaint.textSize
        }
        val furiganaSizeInformation = getTextSize(inheritPaint, mFurigana, 0, mFurigana.length)
        val spanSize = Math.round(
            Math.max(
                Math.max(
                    textSizeInformation.size,
                    furiganaSizeInformation.size
                ), expandedSpanSize
            )
        ).toFloat()
        drawText(
            text, textSizeInformation, mAlignment, canvas,
            spanSize, x, y,
            top - textSizeInformation.fontMetricsInt.ascent +
                    furiganaSizeInformation.fontMetricsInt.descent,
            bottom
        )
        drawText(
            mFurigana, furiganaSizeInformation, mFuriganaAlignment, canvas,
            spanSize, x,
            y + textSizeInformation.fontMetricsInt.ascent -
                    furiganaSizeInformation.fontMetricsInt.descent,
            top,
            bottom + textSizeInformation.fontMetricsInt.ascent -
                    furiganaSizeInformation.fontMetricsInt.bottom
        )
    }
}