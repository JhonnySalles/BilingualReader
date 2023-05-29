package br.com.fenix.bilingualreader.model.entity

import androidx.room.*
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import java.io.Serializable
import java.util.*

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
    var date: Date,
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
    constructor( // History search
        id_book: Long, search: String,
    ) : this(null, id_book, search, Date(), 0, 0f, true, null)

    @Ignore
    constructor( // Search title
        id_book: Long, search: String, chapter: Float,
    ) : this(null, id_book, search, Date(), 0, chapter, true, null)

    @Ignore
    constructor( // Search content
        id_book: Long, search: String, page: Int, parent: BookSearch
    ) : this(null, id_book, search, Date(), page, parent.chapter, false, parent)

    @Ignore
    constructor( // Search content
        id_book: Long, search: String, page: Int, chapter: Float, parent: BookSearch? = null
    ) : this(null, id_book, search, Date(), page, chapter, false, parent)

    constructor(
        id: Long?, id_book: Long, search: String, date: Date,
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