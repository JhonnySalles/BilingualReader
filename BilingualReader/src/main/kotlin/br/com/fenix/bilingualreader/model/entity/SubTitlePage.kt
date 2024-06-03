package br.com.fenix.bilingualreader.model.entity

import com.google.gson.annotations.SerializedName

data class SubTitlePage(
    @SerializedName("nomePagina")
    var name: String,
    @SerializedName("numero")
    val number: Int,
    val hash: String,
    @SerializedName("textos")
    val subTitleTexts: List<SubTitleText>,
    @SerializedName("vocabularios")
    val vocabulary: MutableSet<Vocabulary>
)