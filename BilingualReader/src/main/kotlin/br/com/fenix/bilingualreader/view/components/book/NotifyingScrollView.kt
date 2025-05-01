package br.com.fenix.bilingualreader.view.components.book

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView
import br.com.fenix.bilingualreader.service.functions.AutoScroll
import br.com.fenix.bilingualreader.service.listener.ScrollChangeListener
import org.slf4j.LoggerFactory


class NotifyingScrollView(context: Context, attributeSet: AttributeSet?) : ScrollView(context, attributeSet), AutoScroll {

    constructor(context: Context) : this(context, null)

    private val mLOGGER = LoggerFactory.getLogger(NotifyingScrollView::class.java)

    private var mScrollListener: ScrollChangeListener? = null

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        mScrollListener?.onScrollChanged()
    }

    fun setScrollChangeListener(listener: ScrollChangeListener?) {
        this.mScrollListener = listener
    }

    override fun isVisible(): Boolean = visibility == VISIBLE

    override fun autoScroll(isBack: Boolean): Boolean {
        val child = getChildAt(0)
        if (child != null) {
            var isScroll = false
            if (isBack) {
                if (scrollY > 0 && scrollY > 10) {
                    isScroll = true
                    smoothScrollTo(0, scrollY - context.resources.displayMetrics.heightPixels)
                }
            } else {
                val childHeight: Int = child.height
                if ((scrollY + height) < (childHeight + paddingTop + paddingBottom - 10)) {
                    isScroll = true
                    smoothScrollTo(0, scrollY + context.resources.displayMetrics.heightPixels)
                }
            }
            return isScroll
        }
        return false
    }

}