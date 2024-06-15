package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.Type
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Date


data class ShareMark(
    @Expose
    @SerializedName("origem")
    var origin: String?,
    @Expose
    @SerializedName("alteracao")
    var lastAlteration: Date?,
    @Expose
    @SerializedName("tipo")
    var type: Type?,
    @Expose
    @SerializedName("itens")
    var marks: MutableSet<ShareItem>?
) : Serializable {
    constructor(type: Type) : this("", null, type, mutableSetOf())
}