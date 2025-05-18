package br.com.fenix.bilingualreader.model.interfaces

import android.view.MotionEvent

interface BaseImageView {
    fun getPointerCoordinate(e: MotionEvent): FloatArray
    val isApplyPercent : Boolean
    fun getScrollPercent(): Triple<Float, Float, Float>
    fun setScrollPercent(percent: Triple<Float, Float, Float>)
}