package br.com.fenix.bilingualreader.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.fenix.bilingualreader.model.enums.TrackStatus
import br.com.fenix.bilingualreader.model.interfaces.Entity as EntityBase
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import java.io.Serializable
import java.time.LocalDateTime

@Entity(
    tableName = DataBaseConsts.TRACK.TABLE_NAME,
    indices = [
        Index(value = [DataBaseConsts.TRACK.COLUMNS.FK_ID_LIBRARY]),
        Index(value = [DataBaseConsts.TRACK.COLUMNS.MAL_ID]),
        Index(value = [DataBaseConsts.TRACK.COLUMNS.ANI_ID])
    ]
)
data class Track(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = DataBaseConsts.TRACK.COLUMNS.ID)
    override var id: Long? = null,

    @ColumnInfo(name = DataBaseConsts.TRACK.COLUMNS.MAL_ID)
    var malId: Long? = null,

    @ColumnInfo(name = DataBaseConsts.TRACK.COLUMNS.ANI_ID)
    var aniId: Long? = null,

    @ColumnInfo(name = DataBaseConsts.TRACK.COLUMNS.FK_ID_LIBRARY)
    var fkLibrary: Long = 0,

    @ColumnInfo(name = DataBaseConsts.TRACK.COLUMNS.TITLE)
    var title: String = "",

    @ColumnInfo(name = DataBaseConsts.TRACK.COLUMNS.TITLE_REGEX)
    var titleRegex: String = "",

    @ColumnInfo(name = DataBaseConsts.TRACK.COLUMNS.TOTAL_VOLUMES)
    var totalVolumes: Int? = null,

    @ColumnInfo(name = DataBaseConsts.TRACK.COLUMNS.TOTAL_CHAPTERS)
    var totalChapters: Int? = null,

    @ColumnInfo(name = DataBaseConsts.TRACK.COLUMNS.STATUS)
    var status: TrackStatus = TrackStatus.READING,

    @ColumnInfo(name = DataBaseConsts.TRACK.COLUMNS.SCORE)
    var score: Float? = null,

    @ColumnInfo(name = DataBaseConsts.TRACK.COLUMNS.SCORE_DATE)
    var scoreDate: LocalDateTime? = null,

    @ColumnInfo(name = DataBaseConsts.TRACK.COLUMNS.CHAPTERS_READ)
    var chaptersRead: Int = 0,

    @ColumnInfo(name = DataBaseConsts.TRACK.COLUMNS.VOLUMES_READ)
    var volumesRead: Int = 0,

    @ColumnInfo(name = DataBaseConsts.TRACK.COLUMNS.LAST_SYNC_DATE)
    var lastSyncDate: LocalDateTime? = null
) : Serializable, EntityBase<Long, Track>
