package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.util.constants.GeneralConsts
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
    @SerializedName("sincronizado")
    var sync: Date
) : Serializable {

    @Expose(serialize = false, deserialize = false)
    var alter : Boolean = false

    @Expose(serialize = false, deserialize = false)
    var processed : Boolean = false

    constructor(
        manga: Manga
    ) : this(
        manga.fileName, manga.bookMark, manga.pages, manga.favorite, GeneralConsts.dateTimeToDate(manga.lastAccess!!)
    ) {
        alter = true
        processed = true
        sync = Date()
    }

    constructor(
        book: Book
    ) : this(
        book.fileName, book.bookMark, book.pages, book.favorite, GeneralConsts.dateTimeToDate(book.lastAccess!!)
    ) {
        alter = true
        processed = true
        sync = Date()
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
}