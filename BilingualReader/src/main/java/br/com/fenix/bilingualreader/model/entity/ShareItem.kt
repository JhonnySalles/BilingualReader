package br.com.fenix.bilingualreader.model.entity

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.*


data class ShareItem(
    @Expose
    @SerializedName("arquivo")
    var file: String,
    @Expose
    @SerializedName("bookMark")
    var bookMark: Int,
    @Expose
    @SerializedName("paginas")
    var pages: Int,
    @Expose
    @SerializedName("favorito")
    var favorite: Boolean,
    @Expose
    @SerializedName("ultimoAcesso")
    var lastAccess: Date
) : Serializable {

    @Expose(serialize = false, deserialize = false)
    var alter : Boolean = false

    constructor(
        manga: Manga
    ) : this(
        manga.fileName, manga.bookMark, manga.pages, manga.favorite, manga.lastAccess!!
    ) {
        alter = true
    }

    constructor(
        book: Book
    ) : this(
        book.fileName, book.bookMark, book.pages, book.favorite, book.lastAccess!!
    ) {
        alter = true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShareItem

        if (file != other.file) return false

        return true
    }

    override fun hashCode(): Int {
        return file.hashCode()
    }
}