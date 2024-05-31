package br.com.fenix.bilingualreader.view.components.book

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView


open class WebViewPage(context: Context, attributeSet: AttributeSet?) : WebView(context, attributeSet) {

    constructor(context: Context) : this(context, null)

}