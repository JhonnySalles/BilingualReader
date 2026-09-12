package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.time.format.DateTimeFormatter
import java.util.Date


data class ExportItem(
    @Expose
    @SerializedName(FIELD_FILE)
    var file: String,

    @Expose
    @SerializedName(FIELD_TYPE)
    var type: String,

    @Expose
    @SerializedName(FIELD_BOOKMARK)
    var bookMark: Int,

    @Expose
    @SerializedName(FIELD_PAGES)
    var pages: Int,

    @Expose
    @SerializedName(FIELD_COMPLETED)
    var completed: Boolean,

    @Expose
    @SerializedName(FIELD_FAVORITE)
    var favorite: Boolean,

    @Expose
    @SerializedName(FIELD_LASTACCESS)
    var lastAccess: Date,

    @Expose
    @SerializedName(FIELD_SYNC)
    var sync: Date = Date(),

    @Expose
    @SerializedName(FIELD_HISTORY)
    var history: MutableMap<String, ShareHistory>? = mutableMapOf(),

    @Expose
    @SerializedName(FIELD_ANNOTATION)
    var annotation: MutableMap<String, ShareAnnotation>? = mutableMapOf()
) : Serializable {

    companion object {
        const val FIELD_FILE = "arquivo"
        const val FIELD_TYPE = "tipo"
        const val FIELD_BOOKMARK = "bookMark"
        const val FIELD_PAGES = "paginas"
        const val FIELD_COMPLETED = "completo"
        const val FIELD_FAVORITE = "favorito"
        const val FIELD_LASTACCESS = "ultimoAcesso"
        const val FIELD_SYNC = "sincronizado"
        const val FIELD_HISTORY = "historico"
        const val FIELD_ANNOTATION = "anotacao"

        const val TYPE_MANGA = "manga"
        const val TYPE_BOOK = "livro"

        const val PARSE_DATE_TIME = "yyyy-MM-dd-HH:mm:ss"
    }

    constructor(manga: Manga, list: List<History>, annotations: List<MangaAnnotation>) : this(
        manga.name,
        TYPE_MANGA,
        manga.bookMark,
        manga.pages,
        manga.completed || (manga.pages > 0 && manga.bookMark >= manga.pages),
        manga.favorite,
        GeneralConsts.dateTimeToDate(manga.lastAccess ?: GeneralConsts.SHARE_MARKS.MIN_DATE_TIME),
        Date()
    ) {
        for (hist in list)
            this.history?.set(hist.start.format(DateTimeFormatter.ofPattern(PARSE_DATE_TIME)), ShareHistory(hist))

        for (ann in annotations)
            this.annotation?.set(ann.created.format(DateTimeFormatter.ofPattern(PARSE_DATE_TIME)), ShareAnnotation(ann))
    }

    constructor(book: Book, histories: List<History>, annotations: List<BookAnnotation>) : this(
        book.name,
        TYPE_BOOK,
        book.bookMark,
        book.pages,
        book.completed || (book.pages > 0 && book.bookMark >= book.pages),
        book.favorite,
        GeneralConsts.dateTimeToDate(book.lastAccess ?: GeneralConsts.SHARE_MARKS.MIN_DATE_TIME),
        Date()
    ) {
        for (hist in histories)
            this.history?.set(hist.start.format(DateTimeFormatter.ofPattern(PARSE_DATE_TIME)), ShareHistory(hist))

        for (ann in annotations)
            this.annotation?.set(ann.created.format(DateTimeFormatter.ofPattern(PARSE_DATE_TIME)), ShareAnnotation(ann))
    }
}
