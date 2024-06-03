package br.com.fenix.bilingualreader.view.components.book

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView


open class TextViewPage(context: Context, attributeSet: AttributeSet?) : AppCompatTextView(context, attributeSet) {

    constructor(context: Context) : this(context, null)

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

}