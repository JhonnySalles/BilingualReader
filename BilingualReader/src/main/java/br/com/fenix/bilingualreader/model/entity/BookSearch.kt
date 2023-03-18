package br.com.fenix.bilingualreader.model.entity

import androidx.room.*
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import java.io.Serializable
import java.time.LocalDateTime

@Entity(
    tableName = DataBaseConsts.BOOK_SEARCH_HISTORY.TABLE_NAME,
    indices = [Index(value = [DataBaseConsts.BOOK_SEARCH_HISTORY.COLUMNS.FK_ID_BOOK])]
)
data class BookSearch(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = DataBaseConsts.BOOK_SEARCH_HISTORY.COLUMNS.ID)
    var id: Long?,
    @ColumnInfo(name = DataBaseConsts.BOOK_SEARCH_HISTORY.COLUMNS.FK_ID_BOOK)
    val id_book: Long,
    @ColumnInfo(name = DataBaseConsts.BOOK_SEARCH_HISTORY.COLUMNS.SEARCH)
    var search: String,
    @ColumnInfo(name = DataBaseConsts.BOOK_SEARCH_HISTORY.COLUMNS.DATE)
    var date: LocalDateTime,
    @Ignore
    var page: Int,
    @Ignore
    var chapter: Float,
    @Ignore
    val isTitle: Boolean,
    @Ignore
    val parent: BookSearch? = null,
) : Serializable {
    @Ignore
    constructor(
        id_book: Long, search: String
    ) : this(null, id_book, search, LocalDateTime.now(), 0, 0F, true, null)

    constructor(
        id: Long?, id_book: Long, search: String, date: LocalDateTime,
    ) : this(id, id_book, search, date, 0, 0F, true, null)

    fun toAnnotation(pages: Int): BookAnnotation {
        val chapter = parent!!
        return BookAnnotation(
            id_book,
            page,
            pages,
            MarkType.BookMark,
            chapter.chapter,
            chapter.search,
            search,
            ""
        )
    }

}