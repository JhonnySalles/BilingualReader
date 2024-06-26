package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.Languages
import com.google.gson.annotations.SerializedName


data class SubTitleChapter(
    @SerializedName("manga")
    var manga: String,
    @SerializedName("volume")
    var volume: Float,
    @SerializedName("capitulo")
    val chapter: Float,
    @SerializedName("lingua")
    var language: Languages,
    val scan: String,
    @SerializedName("paginas")
    val subTitlePages: List<SubTitlePage>,
    val extra: Boolean,
    val raw: Boolean,
    @SerializedName("vocabularios")
    val vocabulary: MutableSet<Vocabulary>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubTitleChapter

        if (manga != other.manga) return false
        if (volume != other.volume) return false
        if (chapter != other.chapter) return false
        if (language != other.language) return false
        if (scan != other.scan) return false
        if (extra != other.extra) return false
        if (raw != other.raw) return false

        return true
    }

    override fun hashCode(): Int {
        var result = manga.hashCode()
        result = 31 * result + volume.hashCode()
        result = 31 * result + chapter.hashCode()
        result = 31 * result + language.hashCode()
        result = 31 * result + scan.hashCode()
        result = 31 * result + extra.hashCode()
        result = 31 * result + raw.hashCode()
        return result
    }
}
