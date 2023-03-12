package br.com.fenix.bilingualreader.model.entity

import androidx.room.*
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import java.io.Serializable
import java.time.LocalDateTime

@Entity(
    tableName = DataBaseConsts.BOOK_MARK.TABLE_NAME,
    indices = [Index(value = [DataBaseConsts.BOOK_MARK.COLUMNS.FK_ID_BOOK, DataBaseConsts.BOOK_MARK.COLUMNS.CHAPTER])]
)
data class BookMark(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = DataBaseConsts.BOOK_MARK.COLUMNS.ID)
    var id: Long?,
    @ColumnInfo(name = DataBaseConsts.BOOK_MARK.COLUMNS.FK_ID_BOOK)
    val id_book: Long,
    @ColumnInfo(name = DataBaseConsts.BOOK_MARK.COLUMNS.PAGE)
    var page: Int,
    @ColumnInfo(name = DataBaseConsts.BOOK_MARK.COLUMNS.PAGES)
    var pages: Int,
    @ColumnInfo(name = DataBaseConsts.BOOK_MARK.COLUMNS.TYPE)
    val type: MarkType,
    @ColumnInfo(name = DataBaseConsts.BOOK_MARK.COLUMNS.CHAPTER_NUMBER)
    val chapterNumber: Float,
    @ColumnInfo(name = DataBaseConsts.BOOK_MARK.COLUMNS.CHAPTER)
    val chapter: String,
    @ColumnInfo(name = DataBaseConsts.BOOK_MARK.COLUMNS.TEXT)
    var text: String,
    @ColumnInfo(name = DataBaseConsts.BOOK_MARK.COLUMNS.ANNOTATION)
    var annotation: String,
    @ColumnInfo(name = DataBaseConsts.BOOK_MARK.COLUMNS.FAVORITE)
    var favorite: Boolean,
    @ColumnInfo(name = DataBaseConsts.BOOK_MARK.COLUMNS.COLOR)
    var color: Color,
    @ColumnInfo(name = DataBaseConsts.BOOK_MARK.COLUMNS.ALTERATION)
    var alteration: LocalDateTime,
    @ColumnInfo(name = DataBaseConsts.BOOK_MARK.COLUMNS.CREATED)
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
    @ColumnInfo(name = DataBaseConsts.BOOK_MARK.COLUMNS.COUNT)
    var count: Int = 0
}