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
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.ANNOTATION)
    var annotation: String,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.FAVORITE)
    var favorite: Boolean,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.COLOR)
    var color: Color,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.ALTERATION)
    var alteration: LocalDateTime,
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.CREATED)
    var created: LocalDateTime,
) : Serializable {
    @Ignore
    constructor(
        id_book: Long, page: Int, pages: Int, type: MarkType, chapterNumber: Float, chapter: String, text: String,
        annotation: String, favorite: Boolean = false, color: Color = Color.None,
    ) : this(
        null, id_book, page, pages, type, chapterNumber, chapter, text, annotation, favorite,
        color, LocalDateTime.now(), LocalDateTime.now()
    )

    constructor(
        id: Long?, id_book: Long, page: Int, pages: Int, type: MarkType, chapterNumber: Float, chapter: String, text: String,
        annotation: String, favorite: Boolean, color: Color, alteration: LocalDateTime, created: LocalDateTime, count: Int
    ) : this(
        id, id_book, page, pages, type, chapterNumber, chapter, text, annotation, favorite,
        color, alteration, created
    ) {
        this.count = count
    }

    @Ignore
    @ColumnInfo(name = DataBaseConsts.BOOK_ANNOTATION.COLUMNS.COUNT)
    var count: Int = 0
}