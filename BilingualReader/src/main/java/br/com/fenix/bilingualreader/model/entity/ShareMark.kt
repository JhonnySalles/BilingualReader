package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.Type
import com.google.gson.annotations.SerializedName
import java.util.*

data class ShareMark(
    @SerializedName("verificacao")
    var lastVerify: Date,
    @SerializedName("alteracao")
    var lastAlteration: Date,
    @SerializedName("tipo")
    var type: Type,
    @SerializedName("itens")
    var marks: Set<ShareItem> = setOf()
)