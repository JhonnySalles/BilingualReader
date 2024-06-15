package br.com.fenix.bilingualreader.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit


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
    private var end: LocalDateTime,

    @ColumnInfo(name = DataBaseConsts.HISTORY.COLUMNS.SECONDS_READ)
    private var secondsRead: Long,

    @ColumnInfo(name = DataBaseConsts.HISTORY.COLUMNS.AVERAGE_TIME_PAGE)
    var averageTimeByPage: Long,

    @ColumnInfo(name = DataBaseConsts.HISTORY.COLUMNS.USE_TTS)
    var useTTS: Boolean,

    @ColumnInfo(name = DataBaseConsts.HISTORY.COLUMNS.NOTIFIED)
    var isNotify: Boolean
) {

    @Ignore
    constructor(fkLibrary: Long, fkReference: Long, type: Type, pageStart: Int, pages: Int, volume: Int, averageTimeByPage: Long = 0, useTTS: Boolean = false, isNotify: Boolean = false) : this(
        null, fkLibrary, fkReference, type, pageStart, 0, pages, volume, 0, LocalDateTime.now(), LocalDateTime.now(), 0, averageTimeByPage, useTTS, isNotify
    ) { }

    fun getEnd() = end

    fun setEnd(end: LocalDateTime) {
        this.end = end
        secondsRead = ChronoUnit.SECONDS.between(start, end)
    }

    fun getSecondsRead() = secondsRead

}