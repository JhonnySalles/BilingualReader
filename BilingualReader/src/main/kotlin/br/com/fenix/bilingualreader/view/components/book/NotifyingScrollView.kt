package br.com.fenix.bilingualreader.view.components.book

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView
import br.com.fenix.bilingualreader.service.listener.ScrollChangeListener


class NotifyingScrollView(context: Context, attributeSet: AttributeSet?) : ScrollView(context, attributeSet) {

    constructor(context: Context) : this(context, null) {}

    private var mScroollListener: ScrollChangeListener? = null

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        mScroollListener?.onScrollChanged()
    }

    fun setScrollChangeListener(listener: ScrollChangeListener?) {
        this.mScroollListener = listener
    }

}