package br.com.fenix.bilingualreader.view.components

import android.view.GestureDetector
import android.view.MotionEvent

interface WindowListener : GestureDetector.OnGestureListener {
    fun onTouch(e: MotionEvent): Boolean
    fun onResize(e: MotionEvent): Boolean
}