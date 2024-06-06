package br.com.fenix.bilingualreader.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import java.time.LocalDateTime

@Entity(
    tableName = DataBaseConsts.HISTORY.TABLE_NAME,
    indices = [Index(value = [DataBaseConsts.HISTORY.COLUMNS.FK_ID_REFERENCE, DataBaseConsts.HISTORY.COLUMNS.FK_ID_LIBRARY])]
)
data class History(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = DataBaseConsts.HISTORY.COLUMNS.ID)
    var id: Long?,

    @ColumnInfo(name = DataBaseConsts.HISTORY.COLUMNS.FK_ID_LIBRARY)
    val fkLibrary: Long,

    @ColumnInfo(name = DataBaseConsts.HISTORY.COLUMNS.FK_ID_REFERENCE)
    val fkReference: Long,

    @ColumnInfo(name = DataBaseConsts.HISTORY.COLUMNS.TYPE)
    val type: Type,

    @ColumnInfo(name = DataBaseConsts.HISTORY.COLUMNS.PAGE_START)
    val pageStart: Int,

    @ColumnInfo(name = DataBaseConsts.HISTORY.COLUMNS.PAGE_END)
    var pageEnd: Int,

    @ColumnInfo(name = DataBaseConsts.HISTORY.COLUMNS.PAGES)
    val pages: Int,

    @ColumnInfo(name = DataBaseConsts.HISTORY.COLUMNS.VOLUME)
    val volume: Int,

    @ColumnInfo(name = DataBaseConsts.HISTORY.COLUMNS.CHAPTERS_READ)
    var chaptersRead: Int,

    @ColumnInfo(name = DataBaseConsts.HISTORY.COLUMNS.DATE_TIME_START)
    val start: LocalDateTime,

    @ColumnInfo(name = DataBaseConsts.HISTORY.COLUMNS.DATE_TIME_END)
    var end: LocalDateTime,

    @ColumnInfo(name = DataBaseConsts.HISTORY.COLUMNS.USE_TTS)
    var useTTS: Boolean
) {

    @Ignore
    constructor(fkLibrary: Long, fkReference: Long, type: Type, pageStart: Int, pages: Int, volume: Int, useTTS: Boolean = false) : this(
        null, fkLibrary, fkReference, type, pageStart, 0, pages, volume, 0, LocalDateTime.now(), LocalDateTime.now(), useTTS
    ) { }



}