package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import com.google.gson.annotations.SerializedName
import java.util.*


data class ShareItem(
    @SerializedName("arquivo")
    var file: String,
    @SerializedName("bookMark")
    var bookMark: Int,
    @SerializedName("paginas")
    var pages: Int,
    @SerializedName("favorito")
    var favorite: Boolean,
    @SerializedName("ultimoAcesso")
    var lastAccess: Date
) {

    constructor(
        manga: Manga
    ) : this(
        manga.fileName, manga.bookMark, manga.pages, manga.favorite, GeneralConsts.dateTimeToDate(manga.lastAccess!!)
    )

    constructor(
        book: Book
    ) : this(
        book.fileName, book.bookMark, book.pages, book.favorite, GeneralConsts.dateTimeToDate(book.lastAccess!!)
    )

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