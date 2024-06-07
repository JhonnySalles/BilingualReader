package br.com.fenix.bilingualreader.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import java.io.Serializable
import java.time.LocalDateTime


@Entity(
    tableName = DataBaseConsts.BOOK_ANNOTATION.TABLE_NAME,
    indices = [Index(value = [DataBaseConsts.BOOK_ANNOTATION.COLUMNS.FK_ID_BOOK, DataBaseConsts.BOOK_ANNOTATION.COLUMNS.CHAPTER])]
)
data class BookAnnotation(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.ID)
    var id: Long?,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.FK_ID_BOOK)
    val id_book: Long,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.PAGE)
    var page: Int,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.PAGES)
    var pages: Int,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.TYPE)
    val type: MarkType,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.CHAPTER_NUMBER)
    val chapterNumber: Float,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.CHAPTER)
    val chapter: String,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.TEXT)
    var text: String,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.RANGE)
    var range: IntArray,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.ANNOTATION)
    var annotation: String,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.FAVORITE)
    var favorite: Boolean,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.COLOR)
    var color: Color,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.ALTERATION)
    var alteration: LocalDateTime,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.CREATED)
    var created: LocalDateTime
) : Serializable {

    @Ignore
    constructor(
        id_book: Long, page: Int, pages: Int, type: MarkType, chapterNumber: Float, chapter: String, text: String,
        range: IntArray, annotation: String, favorite: Boolean = false, color: Color = Color.None
    ) : this(
        null, id_book, page, pages, type, chapterNumber, chapter, text, range, annotation, favorite,
        color, LocalDateTime.now(), LocalDateTime.now()
    )

    constructor(
        id: Long?, id_book: Long, page: Int, pages: Int, type: MarkType, chapterNumber: Float, chapter: String, text: String, range: IntArray,
        annotation: String, favorite: Boolean, color: Color, alteration: LocalDateTime, created: LocalDateTime, count: Int
    ) : this(
        id, id_book, page, pages, type, chapterNumber, chapter, text, range, annotation, favorite,
        color, alteration, created
    ) {
        this.count = count
    }

    @Ignore
    constructor(other: BookAnnotation) : this(
        other.id, other.id_book, other.page, other.pages, other.type, other.chapterNumber, other.chapter, other.text, other.range, other.annotation,
        other.favorite, other.color, other.alteration, other.created, other.count
    )

    @Ignore
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.COUNT)
    var count: Int = 0

    fun update(other: BookAnnotation) {
        this.id = other.id
        this.page = other.page
        this.pages = other.pages
        this.text = other.text
        this.range = other.range
        this.annotation = other.annotation
        this.favorite = other.favorite
        this.color = other.color
        this.alteration = other.alteration
        this.created = other.created
        this.count = other.count
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BookAnnotation

        if (id != other.id) return false
        if (id_book != other.id_book) return false
        if (page != other.page) return false
        if (pages != other.pages) return false
        if (type != other.type) return false
        if (text != other.text) return false
        if (annotation != other.annotation) return false
        if (favorite != other.favorite) return false
        if (color != other.color) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + id_book.hashCode()
        result = 31 * result + page
        result = 31 * result + pages
        result = 31 * result + type.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + annotation.hashCode()
        result = 31 * result + favorite.hashCode()
        result = 31 * result + color.hashCode()
        return result
    }
}