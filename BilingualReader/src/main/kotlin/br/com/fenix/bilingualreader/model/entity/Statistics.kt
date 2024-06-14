package br.com.fenix.bilingualreader.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import java.time.LocalDateTime


@Entity
data class Statistics (
    @ColumnInfo(name = DataBaseConsts.STATISTICS.COLUMNS.READING)
    val reading: Long,
    @ColumnInfo(name = DataBaseConsts.STATISTICS.COLUMNS.TO_READ)
    val toRead: Long,
    @ColumnInfo(name = DataBaseConsts.STATISTICS.COLUMNS.LIBRARY)
    val library: Long,
    @ColumnInfo(name = DataBaseConsts.STATISTICS.COLUMNS.READ)
    val read: Long,

    @ColumnInfo(name = DataBaseConsts.STATISTICS.COLUMNS.COMPLETE_READING_PAGES)
    val completeReadingPages: Long,
    @ColumnInfo(name = DataBaseConsts.STATISTICS.COLUMNS.COMPLETE_READING_SECONDS)
    val completeReadingSeconds: Long,
    @ColumnInfo(name = DataBaseConsts.STATISTICS.COLUMNS.CURRENT_READING_PAGES)
    val currentReadingPages: Long,
    @ColumnInfo(name = DataBaseConsts.STATISTICS.COLUMNS.CURRENT_READING_SECONDS)
    val currentReadingSeconds: Long,

    @ColumnInfo(name = DataBaseConsts.STATISTICS.COLUMNS.TOTAL_READ_PAGES)
    val totalReadPages: Long,
    @ColumnInfo(name = DataBaseConsts.STATISTICS.COLUMNS.TOTAL_READ_SECONDS)
    val totalReadSeconds: Long,
    @ColumnInfo(name = DataBaseConsts.STATISTICS.COLUMNS.DATE_TIME)
    val dateTime: LocalDateTime,
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = DataBaseConsts.STATISTICS.COLUMNS.TYPE)
    val type: Type
)