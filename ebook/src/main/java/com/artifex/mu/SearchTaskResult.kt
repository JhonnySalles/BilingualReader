package com.artifex.mu

import android.graphics.RectF

class SearchTaskResult internal constructor(val txt: String, val pageNumber: Int, val searchBoxes: Array<RectF>) {
    companion object {
        private var singleton: SearchTaskResult? = null
        fun get(): SearchTaskResult? {
            return singleton
        }

        fun set(r: SearchTaskResult?) {
            singleton = r
        }
    }
}