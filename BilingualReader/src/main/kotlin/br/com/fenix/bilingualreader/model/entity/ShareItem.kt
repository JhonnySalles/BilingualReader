package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date


data class ShareItem(
    @Expose
    @SerializedName(FIELD_FILE)
    @PropertyName(FIELD_FILE)
    @get:PropertyName(FIELD_FILE)
    var file: String,
    @Expose
    @SerializedName(FIELD_BOOKMARK)
    @PropertyName(FIELD_BOOKMARK)
    @get:PropertyName(FIELD_BOOKMARK)
    var bookMark: Int,
    @Expose
    @SerializedName(FIELD_PAGES)
    @PropertyName(FIELD_PAGES)
    @get:PropertyName(FIELD_PAGES)
    var pages: Int,
    @Expose
    @SerializedName(FIELD_COMPLETED)
    @PropertyName(FIELD_COMPLETED)
    @get:PropertyName(FIELD_COMPLETED)
    var completed: Boolean,
    @Expose
    @SerializedName(FIELD_FAVORITE)
    @PropertyName(FIELD_FAVORITE)
    @get:PropertyName(FIELD_FAVORITE)
    var favorite: Boolean,
    @Expose
    @SerializedName(FIELD_LASTACCESS)
    @PropertyName(FIELD_LASTACCESS)
    @get:PropertyName(FIELD_LASTACCESS)
    var lastAccess: Date,
    @Expose
    @SerializedName(FIELD_SYNC)
    @PropertyName(FIELD_SYNC)
    @get:PropertyName(FIELD_SYNC)
    var sync: Date = Date(),
    @Expose
    @SerializedName(FIELD_HISTORY)
    @PropertyName(FIELD_HISTORY)
    @get:PropertyName(FIELD_HISTORY)
    var history: MutableMap<String, ShareHistory>? = mutableMapOf(),
    @Expose
    @SerializedName(FIELD_ANNOTATION)
    @PropertyName(FIELD_ANNOTATION)
    @get:PropertyName(FIELD_ANNOTATION)
    var annotation: MutableMap<String, ShareAnnotation>? = mutableMapOf()
) : Serializable {

    companion object {
        const val FIELD_FILE = "arquivo"
        const val FIELD_BOOKMARK = "bookMark"
        const val FIELD_PAGES = "paginas"
        const val FIELD_COMPLETED = "completo"
        const val FIELD_FAVORITE = "favorito"
        const val FIELD_LASTACCESS = "ultimoAcesso"
        const val FIELD_SYNC = "sincronizado"
        const val FIELD_HISTORY = "historico"
        const val FIELD_ANNOTATION = "anotacao"

        const val PARSE_DATE_TIME = "yyyy-MM-dd-HH:mm:ss"
    }

    @Exclude
    @get:Exclude
    @Expose(serialize = false, deserialize = false)
    var id: Long = 0

    @Exclude
    @get:Exclude
    @Expose(serialize = false, deserialize = false)
    var idLibrary: Long = 0

    @Exclude
    @get:Exclude
    @Expose(serialize = false, deserialize = false)
    var alter: Boolean = false

    @Exclude
    @get:Exclude
    @Expose(serialize = false, deserialize = false)
    var received: Boolean = false

    @Exclude
    @get:Exclude
    @Expose(serialize = false, deserialize = false)
    var processed: Boolean = false

    constructor(firebase: Map<String, *>) : this(
        firebase[FIELD_FILE] as String, (firebase[FIELD_BOOKMARK] as Long).toInt(), (firebase[FIELD_PAGES] as Long).toInt(),
        if (firebase.containsKey(FIELD_COMPLETED)) (firebase[FIELD_COMPLETED] as Boolean) else ((firebase[FIELD_BOOKMARK] as Long).toInt() >= (firebase[FIELD_PAGES] as Long).toInt()),
        firebase[FIELD_FAVORITE] as Boolean, Date(), Date()
    ) {
        this.lastAccess = (firebase[FIELD_LASTACCESS] as Timestamp).toDate()
        this.sync = (firebase[FIELD_SYNC] as Timestamp).toDate()
        if (firebase.containsKey(FIELD_HISTORY))
            getHistory(firebase[FIELD_HISTORY] as Map<String, *>)

        if (firebase.containsKey(FIELD_ANNOTATION))
            getAnnotation(firebase[FIELD_ANNOTATION] as Map<String, *>)
    }

    private fun getHistory(histories: Map<String, *>) {
        for (history in histories)
            this.history?.set(history.key, ShareHistory(history.value as Map<String, *>))
    }

    private fun getAnnotation(annotations: Map<String, *>) {
        for (annotation in annotations)
            this.annotation?.set(annotation.key, ShareAnnotation(annotation.value as Map<String, *>))
    }

    constructor(manga: Manga, list: List<History>) : this(manga.name, manga.bookMark, manga.pages, manga.completed, manga.favorite, GeneralConsts.dateTimeToDate(manga.lastAccess ?: GeneralConsts.SHARE_MARKS.MIN_DATE_TIME)) {
        alter = true
        processed = true
        id = manga.id ?: 0
        idLibrary = manga.fkLibrary ?: 0

        for (history in list)
            this.history?.set(history.start.format(DateTimeFormatter.ofPattern(PARSE_DATE_TIME)), ShareHistory(history))
    }

    constructor(book: Book, histories: List<History>, annotations: List<BookAnnotation>) : this(book.name, book.bookMark, book.pages, book.completed, book.favorite, GeneralConsts.dateTimeToDate(book.lastAccess ?: GeneralConsts.SHARE_MARKS.MIN_DATE_TIME)) {
        alter = true
        processed = true
        id = book.id ?: 0
        idLibrary = book.fkLibrary ?: 0

        for (history in histories)
            this.history?.set(history.start.format(DateTimeFormatter.ofPattern(PARSE_DATE_TIME)), ShareHistory(history))

        for (annotation in annotations)
            this.annotation?.set(annotation.created.format(DateTimeFormatter.ofPattern(PARSE_DATE_TIME)), ShareAnnotation(annotation))
    }

    fun refreshHistory(histories: List<History>) {
        this.history = mutableMapOf()
        for (history in histories)
            this.history?.set(history.start.format(DateTimeFormatter.ofPattern(PARSE_DATE_TIME)), ShareHistory(history))
    }

    fun refreshAnnotations(annotations: List<BookAnnotation>) {
        this.annotation = mutableMapOf()
        for (annotation in annotations)
            this.annotation?.set(annotation.created.format(DateTimeFormatter.ofPattern(PARSE_DATE_TIME)), ShareAnnotation(annotation))
    }

    fun merge(manga: Manga) {
        this.bookMark = manga.bookMark
        this.pages = manga.pages
        this.completed = manga.completed
        this.lastAccess = GeneralConsts.dateTimeToDate(manga.lastAccess ?: GeneralConsts.SHARE_MARKS.MIN_DATE_TIME)
        this.favorite = manga.favorite
        this.alter = true
        this.processed = true
    }

    fun merge(book: Book) {
        this.bookMark = book.bookMark
        this.pages = book.pages
        this.completed = book.completed
        this.lastAccess = GeneralConsts.dateTimeToDate(book.lastAccess ?: GeneralConsts.SHARE_MARKS.MIN_DATE_TIME)
        this.favorite = book.favorite
        this.alter = true
        this.processed = true
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