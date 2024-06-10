package br.com.fenix.bilingualreader.view.components

import android.text.style.ClickableSpan
import android.view.View


abstract class LongClickableSpan : ClickableSpan() {
    abstract fun onLongClick(view: View?)

}