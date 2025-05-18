package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Date


data class ShareHistory(
    @Expose
    @SerializedName(FIELD_PAGE_START)
    @PropertyName(FIELD_PAGE_START)
    @get:PropertyName(FIELD_PAGE_START)
    val pageStart: Int,

    @Expose
    @SerializedName(FIELD_PAGE_END)
    @PropertyName(FIELD_PAGE_END)
    @get:PropertyName(FIELD_PAGE_END)
    var pageEnd: Int,

    @Expose
    @SerializedName(FIELD_PAGES)
    @PropertyName(FIELD_PAGES)
    @get:PropertyName(FIELD_PAGES)
    var pages: Int,

    @Expose
    @SerializedName(FIELD_COMPLETED)
    @PropertyName(FIELD_COMPLETED)
    @get:PropertyName(FIELD_COMPLETED)
    var completed: Boolean,

    @Expose
    @SerializedName(FIELD_VOLUME)
    @PropertyName(FIELD_VOLUME)
    @get:PropertyName(FIELD_VOLUME)
    val volume: String,

    @Expose
    @SerializedName(FIELD_CHAPTERS_READ)
    @PropertyName(FIELD_CHAPTERS_READ)
    @get:PropertyName(FIELD_CHAPTERS_READ)
    var chaptersRead: Int,

    @Expose
    @SerializedName(FIELD_START)
    @PropertyName(FIELD_START)
    @get:PropertyName(FIELD_START)
    val start: Date,

    @Expose
    @SerializedName(FIELD_END)
    @PropertyName(FIELD_END)
    @get:PropertyName(FIELD_END)
    var end: Date,

    @Expose
    @SerializedName(FIELD_SECONDS_READ)
    @PropertyName(FIELD_SECONDS_READ)
    @get:PropertyName(FIELD_SECONDS_READ)
    var secondsRead: Long,

    @Expose
    @SerializedName(FIELD_AVERAGE_TIME_BY_PAGE)
    @PropertyName(FIELD_AVERAGE_TIME_BY_PAGE)
    @get:PropertyName(FIELD_AVERAGE_TIME_BY_PAGE)
    var averageTimeByPage: Long,

    @Expose
    @SerializedName(FIELD_USE_TTS)
    @PropertyName(FIELD_USE_TTS)
    @get:PropertyName(FIELD_USE_TTS)
    var useTTS: Boolean
) : Serializable {

    companion object {
        const val FIELD_PAGE_START = "paginaInicial"
        const val FIELD_PAGE_END = "paginaFinal"
        const val FIELD_PAGES = "paginas"
        const val FIELD_COMPLETED = "completo"
        const val FIELD_VOLUME = "volume"
        const val FIELD_CHAPTERS_READ = "capitulosLidos"
        const val FIELD_START = "inicio"
        const val FIELD_END = "final"
        const val FIELD_SECONDS_READ = "segundosLidos"
        const val FIELD_AVERAGE_TIME_BY_PAGE = "mediaTempoPorPagina"
        const val FIELD_USE_TTS = "usadoTTS"
    }

    constructor(firebase: Map<String, *>) : this(
        (firebase[FIELD_PAGE_START] as Long).toInt(), (firebase[FIELD_PAGE_END] as Long).toInt(), (firebase[FIELD_PAGES] as Long).toInt(),
        if (firebase.containsKey(FIELD_COMPLETED)) (firebase[FIELD_COMPLETED] as Boolean) else ((firebase[FIELD_PAGE_END] as Long).toInt() >= (firebase[FIELD_PAGES] as Long).toInt()),
        (firebase[FIELD_VOLUME] as String), (firebase[FIELD_CHAPTERS_READ] as Long).toInt(), (firebase[FIELD_START] as Timestamp).toDate(), (firebase[FIELD_END] as Timestamp).toDate(),
        firebase[FIELD_SECONDS_READ] as Long, firebase[FIELD_AVERAGE_TIME_BY_PAGE] as Long, firebase[FIELD_USE_TTS] as Boolean,
    )

    constructor(history: History) : this(
        history.pageStart, history.getPageEnd(), history.getPages(), history.completed, history.volume, history.chaptersRead, GeneralConsts.dateTimeToDate(history.start),
        GeneralConsts.dateTimeToDate(history.getEnd()), history.getSecondsRead(), history.averageTimeByPage, history.useTTS
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShareHistory

        if (pageStart != other.pageStart) return false
        if (pageEnd != other.pageEnd) return false
        if (pages != other.pages) return false
        if (start != other.start) return false
        if (end != other.end) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pageStart
        result = 31 * result + pageEnd
        result = 31 * result + pages
        result = 31 * result + start.hashCode()
        result = 31 * result + (end?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ShareHistory(pageStart=$pageStart, pageEnd=$pageEnd, pages=$pages, completed=$completed, volume=$volume, chaptersRead=$chaptersRead, start=$start, end=$end, secondsRead=$secondsRead, averageTimeByPage=$averageTimeByPage, useTTS=$useTTS)"
    }

}