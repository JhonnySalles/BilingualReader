package br.com.fenix.bilingualreader.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts


@Entity(
    tableName = DataBaseConsts.TAGS.TABLE_NAME,
    indices = [Index(value = [DataBaseConsts.TAGS.COLUMNS.NAME])]
)
data class Tags(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = DataBaseConsts.TAGS.COLUMNS.ID)
    var id: Long?,
    @ColumnInfo(name = DataBaseConsts.TAGS.COLUMNS.NAME)
    var name: String,
    @ColumnInfo(name = DataBaseConsts.TAGS.COLUMNS.EXCLUDED)
    var excluded: Boolean = false,
    var isSelected: Boolean = false
)