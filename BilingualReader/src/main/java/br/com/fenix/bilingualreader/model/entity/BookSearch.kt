package br.com.fenix.bilingualreader.model.entity

import androidx.room.*
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
    @ColumnInfo(name = DataBaseConsts.BOOK_SEARCH_HISTORY.COLUMNS.PAGE)
    var page: Int,
    @ColumnInfo(name = DataBaseConsts.BOOK_SEARCH_HISTORY.COLUMNS.DATE)
    var date: LocalDateTime,
    @Ignore
    val isTitle: Boolean,
    @Ignore
    val parent: BookSearch? = null,
) : Serializable