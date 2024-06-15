package br.com.fenix.bilingualreader.model.entity

import android.net.Uri


data class Speech(
    val page: Int,
    val sequence: Int,
    val text: String,
    val html: String,
    var audio: Uri? = null,
    var isRead: Boolean = false
)