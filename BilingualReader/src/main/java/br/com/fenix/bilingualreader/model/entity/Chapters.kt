package br.com.fenix.bilingualreader.model.entity

import android.graphics.Bitmap

data class Chapters(
    var title: String,
    val number: Int,
    val page: Int,
    var chapter: Float,
    val isTitle: Boolean,
    val parent: Chapters? = null,
    var isSelected: Boolean = false,
    var image: Bitmap? = null
)