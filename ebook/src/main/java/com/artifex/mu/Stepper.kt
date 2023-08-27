package com.artifex.mu

import android.annotation.SuppressLint
import android.os.Build
import android.view.View

class Stepper(protected val mPoster: View, protected val mTask: Runnable) {
    protected var mPending = false
    @SuppressLint("NewApi")
    fun prod() {
        if (!mPending) {
            mPending = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mPoster.postOnAnimation {
                    mPending = false
                    mTask.run()
                }
            } else {
                mPoster.post {
                    mPending = false
                    mTask.run()
                }
            }
        }
    }
}