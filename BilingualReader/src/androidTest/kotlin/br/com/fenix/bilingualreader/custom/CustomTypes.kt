package br.com.fenix.bilingualreader.custom

import androidx.annotation.IdRes

class CustomTypes {

    enum class AlertDialogButton(@IdRes val resId: Int) {
        POSITIVE(android.R.id.button1),
        NEGATIVE(android.R.id.button2),
        NEUTRAL(android.R.id.button3)
    }
}