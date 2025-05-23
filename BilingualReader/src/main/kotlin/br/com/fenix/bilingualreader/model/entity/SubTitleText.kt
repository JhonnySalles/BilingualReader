package br.com.fenix.bilingualreader.model.entity

import com.google.gson.annotations.SerializedName


data class SubTitleText(
    @SerializedName("texto")
    val text: String,
    @SerializedName("sequencia")
    val sequence: Int,
    val x1: Int,
    val y1: Int,
    val x2: Int,
    var y2: Int
)