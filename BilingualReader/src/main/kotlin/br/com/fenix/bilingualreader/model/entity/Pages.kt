package br.com.fenix.bilingualreader.model.entity

import android.graphics.Bitmap


data class Pages(
    var name: String,
    val number: Int,
    val page: Int,
    var isSelected: Boolean = false,
    var image: Bitmap? = null
)