package br.com.fenix.bilingualreader.model.interfaces

import android.text.style.ClickableSpan
import android.view.View


abstract class LongClickableSpan : ClickableSpan() {
    abstract fun onLongClick(view: View?)

}