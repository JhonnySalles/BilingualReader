package br.com.fenix.bilingualreader.view.components.book

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat


open class TextViewPage(context: Context, attributeSet: AttributeSet?) : AppCompatTextView(context, attributeSet) {

    constructor(context: Context) : this(context, null)

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

}