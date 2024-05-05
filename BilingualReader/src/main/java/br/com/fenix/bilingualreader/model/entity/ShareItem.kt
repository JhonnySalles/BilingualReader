package br.com.fenix.bilingualreader.model.entity


import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Date


data class ShareItem(
    @Expose
    @SerializedName(ShareItem.FIELD_FILE)
    @PropertyName(ShareItem.FIELD_FILE)
    @get:PropertyName(ShareItem.FIELD_FILE)
    var file: String,
    @Expose
    @SerializedName(ShareItem.FIELD_BOOKMARK)
    @PropertyName(ShareItem.FIELD_BOOKMARK)
    @get:PropertyName(ShareItem.FIELD_BOOKMARK)
    var bookMark: Int,
    @Expose
    @SerializedName(ShareItem.FIELD_PAGES)
    @PropertyName(ShareItem.FIELD_PAGES)
    @get:PropertyName(ShareItem.FIELD_PAGES)
    var pages: Int,
    @Expose
    @SerializedName(ShareItem.FIELD_FAVORITE)
    @PropertyName(ShareItem.FIELD_FAVORITE)
    @get:PropertyName(ShareItem.FIELD_FAVORITE)
    var favorite: Boolean,
    @Expose
    @SerializedName(ShareItem.FIELD_LASTACCESS)
    @PropertyName(ShareItem.FIELD_LASTACCESS)
    @get:PropertyName(ShareItem.FIELD_LASTACCESS)
    var lastAccess: Date,
    @Expose
    @SerializedName(ShareItem.FIELD_SYNC)
    @PropertyName(ShareItem.FIELD_SYNC)
    @get:PropertyName(ShareItem.FIELD_SYNC)
    var sync: Date
) : Serializable {

    companion object {
        const val FIELD_FILE = "arquivo"
        const val FIELD_BOOKMARK = "bookMark"
        const val FIELD_PAGES = "paginas"
        const val FIELD_FAVORITE = "favorito"
        const val FIELD_LASTACCESS = "ultimoAcesso"
        const val FIELD_SYNC = "sincronizado"
    }

    @Exclude
    @get:Exclude
    @Expose(serialize = false, deserialize = false)
    var alter: Boolean = false

    @Exclude
    @get:Exclude
    @Expose(serialize = false, deserialize = false)
    var processed: Boolean = false

    constructor(
        firebase: Map<String, *>
    ) : this(
        firebase[ShareItem.FIELD_FILE] as String, (firebase[ShareItem.FIELD_BOOKMARK] as Long).toInt(), (firebase[ShareItem.FIELD_PAGES] as Long).toInt(),
        firebase[ShareItem.FIELD_FAVORITE] as Boolean, Date(), Date()
    ) {
        this.lastAccess = (firebase[ShareItem.FIELD_LASTACCESS] as Timestamp).toDate()
        this.sync = (firebase[ShareItem.FIELD_SYNC] as Timestamp).toDate()
    }

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
        if (javaClass != other?.javaClass)
            return false

        other as ShareItem

        if (file != other.file)
            return false

        return true
    }

    override fun hashCode(): Int {
        return file.hashCode()
    }

    override fun toString(): String {
        return "ShareItem(file='$file', bookMark=$bookMark, pages=$pages, favorite=$favorite, sync=$sync, alter=$alter, processed=$processed)"
    }
}