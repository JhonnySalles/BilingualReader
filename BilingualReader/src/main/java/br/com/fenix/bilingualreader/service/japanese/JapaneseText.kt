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

import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import java.util.regex.Pattern

object JapaneseText {
    private val m_examplePattern: Pattern = Pattern.compile("\\{([^\\};]+);([^\\};]+)\\}")

    fun spannify(
        aSpannableStringBuilder: SpannableStringBuilder,
        aString: String
    ) {
        var offset = 0
        var insertPos = aSpannableStringBuilder.length
        while (offset < aString.length) {
            val nextOffset = aString.offsetByCodePoints(offset, 1)
            val substring = aString.substring(offset, nextOffset)
            val substringLength = substring.length
            aSpannableStringBuilder.append(substring)
            aSpannableStringBuilder.setSpan(
                SuperReplacementSpan(),
                insertPos, insertPos + substringLength,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            insertPos += substringLength
            offset = nextOffset
        }
    }

    fun spannifyWithFurigana(
        aSpannableStringBuilder: SpannableStringBuilder,
        aString: String,
        aRelativeSize: Float
    ) {
        val matcher = m_examplePattern.matcher(aString)
        var previousMatchEnd = 0
        while (matcher.find()) {
            val matchStart = matcher.start()
            if (matchStart > previousMatchEnd) {
                spannify(aSpannableStringBuilder, aString.substring(previousMatchEnd, matchStart))
            }
            val text = matcher.group(1)
            val furigana = matcher.group(2)
            val spanStart = aSpannableStringBuilder.length
            if (text != null) {
                aSpannableStringBuilder.append(text)
                if (furigana != null) {
                    val furiganaSpannable = SpannableString(furigana)
                    furiganaSpannable.setSpan(
                        RelativeSizeSpan(aRelativeSize), 0, furiganaSpannable.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    aSpannableStringBuilder.setSpan(
                        SuperRubySpan(furiganaSpannable, SuperReplacementSpan.Alignment.JIS, SuperReplacementSpan.Alignment.JIS),
                        spanStart, spanStart + text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            previousMatchEnd = matcher.end()
        }
        if (previousMatchEnd < aString.length - 1) {
            spannify(aSpannableStringBuilder, aString.substring(previousMatchEnd, aString.length - 1))
        }
    }
}