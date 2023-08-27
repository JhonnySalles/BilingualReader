package com.artifex.mu

import android.graphics.RectF

class TextWord : RectF() {
    @JvmField
	var w: String

    init {
        w = String()
    }

    fun Add(tc: TextChar) {
        super.union(tc)
        w = w + String(charArrayOf(tc.c))
    }
}