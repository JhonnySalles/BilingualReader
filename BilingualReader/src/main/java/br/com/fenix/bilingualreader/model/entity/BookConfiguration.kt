package br.com.fenix.bilingualreader.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.fenix.bilingualreader.model.enums.AlignmentLayoutType
import br.com.fenix.bilingualreader.model.enums.FontType
import br.com.fenix.bilingualreader.model.enums.MarginLayoutType
import br.com.fenix.bilingualreader.model.enums.ScrollingType
import br.com.fenix.bilingualreader.model.enums.SpacingLayoutType
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import java.io.Serializable

@Entity(
    tableName = DataBaseConsts.BOOK_CONFIGURATION.TABLE_NAME,
    indices = [Index(value = [DataBaseConsts.BOOK_CONFIGURATION.COLUMNS.FK_ID_BOOK])]
)
data class BookConfiguration (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = DataBaseConsts.BOOK_CONFIGURATION.COLUMNS.ID)
    var id: Long?,
    @ColumnInfo(name = DataBaseConsts.BOOK_CONFIGURATION.COLUMNS.FK_ID_BOOK)
    val idBook: Long,
    @ColumnInfo(name = DataBaseConsts.BOOK_CONFIGURATION.COLUMNS.ALIGNMENT)
    var alignment: AlignmentLayoutType,
    @ColumnInfo(name = DataBaseConsts.BOOK_CONFIGURATION.COLUMNS.MARGIN)
    var margin: MarginLayoutType,
    @ColumnInfo(name = DataBaseConsts.BOOK_CONFIGURATION.COLUMNS.SPACING)
    var spacing: SpacingLayoutType,
    @ColumnInfo(name = DataBaseConsts.BOOK_CONFIGURATION.COLUMNS.FONT_TYPE)
    var fontType: FontType,
    @ColumnInfo(name = DataBaseConsts.BOOK_CONFIGURATION.COLUMNS.FONT_SIZE)
    var fontSize: Float,
    @ColumnInfo(name = DataBaseConsts.BOOK_CONFIGURATION.COLUMNS.SCROLLING)
    var scrolling: ScrollingType
) : Serializable