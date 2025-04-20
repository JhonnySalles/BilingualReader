package br.com.fenix.bilingualreader.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.model.interfaces.Annotation
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
    override val id_parent: Long,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.PAGE)
    var page: Int,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.PAGES)
    var pages: Int,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.FONT_SIZE)
    var fontSize: Float,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.TYPE)
    override val markType: MarkType,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.CHAPTER_NUMBER)
    override val chapterNumber: Float,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.CHAPTER)
    override val chapter: String,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.TEXT)
    var text: String,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.RANGE)
    var range: IntArray,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.ANNOTATION)
    override var annotation: String,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.FAVORITE)
    var favorite: Boolean,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.COLOR)
    var color: Color,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.ALTERATION)
    var alteration: LocalDateTime,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.CREATED)
    var created: LocalDateTime
) : Serializable, Annotation {

    //For a annotation title
    @Ignore
    override val type: Type = Type.BOOK
    @Ignore
    override var isRoot: Boolean = false
    @Ignore
    override var isTitle: Boolean = false
    @Ignore
    override var parent: Annotation? = null
    @Ignore
    override var count: Int = 0

    @Ignore
    constructor(
        id_book: Long, page: Int, pages: Int, fontSize: Float, type: MarkType, chapterNumber: Float, chapter: String, text: String,
        range: IntArray, annotation: String, favorite: Boolean = false, color: Color = Color.None
    ) : this(
        null, id_book, page, pages, fontSize, type, chapterNumber, chapter, text, range, annotation, favorite,
        color, LocalDateTime.now(), LocalDateTime.now()
    )
    @Ignore //For a annotation title
    constructor(id_book: Long, chapterNumber: Float, chapter: String, text: String, annotation: String, isRoot: Boolean = false, isTitle: Boolean = false) : this(
        null, id_book, -1, -1, 0f, MarkType.Annotation, chapterNumber, chapter, text, intArrayOf(), annotation, false,
        Color.None, LocalDateTime.now(), LocalDateTime.now()
    ) {
        this.isRoot = isRoot
        this.isTitle = isTitle
        this.count = 0
    }

    @Ignore
    constructor(other: BookAnnotation) : this(
        other.id, other.id_parent, other.page, other.pages, other.fontSize, other.markType, other.chapterNumber, other.chapter, other.text, other.range,
        other.annotation, other.favorite, other.color, other.alteration, other.created
    ) {
        this.count = other.count
        this.isTitle = other.isTitle
        this.isRoot = other.isRoot
    }

    fun update(other: BookAnnotation) {
        this.id = other.id
        this.page = other.page
        this.pages = other.pages
        this.fontSize = other.fontSize
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
        if (id_parent != other.id_parent) return false
        if (page != other.page) return false
        if (pages != other.pages) return false
        if (markType != other.markType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + id_parent.hashCode()
        result = 31 * result + page
        result = 31 * result + pages
        result = 31 * result + markType.hashCode()
        return result
    }

}