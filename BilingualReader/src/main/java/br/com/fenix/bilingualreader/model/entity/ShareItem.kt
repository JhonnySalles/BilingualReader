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
    var lastAccess: Date,
    @Expose
    @SerializedName(ShareItem.FIELD_SYNC)
    var sync: Date
) : Serializable {

    companion object {
        const val FIELD_SYNC = "sincronizado"
    }

    @Expose(serialize = false, deserialize = false)
    var alter : Boolean = false

    @Expose(serialize = false, deserialize = false)
    var processed : Boolean = false

    constructor(
        manga: Manga
    ) : this(
        manga.name, manga.bookMark, manga.pages, manga.favorite, manga.lastAccess!!, Date()
    ) {
        alter = true
        processed = true
    }

    constructor(
        book: Book
    ) : this(
        book.name, book.bookMark, book.pages, book.favorite, book.lastAccess!!, Date()
    ) {
        alter = true
        processed = true
    }

    fun merge(manga: Manga) {
        this.bookMark = manga.bookMark
        this.lastAccess = manga.lastAccess!!
        this.favorite = manga.favorite
        this.alter = true
        this.processed = true
        this.sync = Date()
    }
    fun merge(book: Book) {
        this.bookMark = book.bookMark
        this.lastAccess = book.lastAccess!!
        this.favorite = book.favorite
        this.alter = true
        this.processed = true
        this.sync = Date()
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

    override fun toString(): String {
        return "ShareItem(file='$file', bookMark=$bookMark, pages=$pages, favorite=$favorite, sync=$sync, alter=$alter, processed=$processed)"
    }
}