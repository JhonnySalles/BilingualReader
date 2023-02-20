package br.com.fenix.bilingualreader.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.fenix.bilingualreader.model.enums.*
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import java.io.Serializable

@Entity(
    tableName = DataBaseConsts.BOOKCONFIGURATION.TABLE_NAME,
    indices = [Index(value = [DataBaseConsts.BOOKCONFIGURATION.COLUMNS.FK_ID_BOOK])]
)
data class BookConfiguration (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = DataBaseConsts.BOOKCONFIGURATION.COLUMNS.ID)
    var id: Long?,
    @ColumnInfo(name = DataBaseConsts.BOOKCONFIGURATION.COLUMNS.FK_ID_BOOK)
    var idBook: Long,
    @ColumnInfo(name = DataBaseConsts.BOOKCONFIGURATION.COLUMNS.ALIGNMENT)
    var alignment: AlignmentType,
    @ColumnInfo(name = DataBaseConsts.BOOKCONFIGURATION.COLUMNS.MARGIN)
    var margin: MarginType,
    @ColumnInfo(name = DataBaseConsts.BOOKCONFIGURATION.COLUMNS.SPACING)
    var spacing: SpacingType,
    @ColumnInfo(name = DataBaseConsts.BOOKCONFIGURATION.COLUMNS.SCROLLING)
    var scrolling: ScrollingType,
    @ColumnInfo(name = DataBaseConsts.BOOKCONFIGURATION.COLUMNS.FONT_TYPE)
    var fontType: FontType,
    @ColumnInfo(name = DataBaseConsts.BOOKCONFIGURATION.COLUMNS.FONT_SIZE)
    var fontSize: Float
) : Serializable