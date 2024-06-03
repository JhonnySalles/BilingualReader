package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.Languages
import com.google.gson.annotations.SerializedName

data class SubTitleVolume(
    @SerializedName("manga")
    var manga: String,
    @SerializedName("volume")
    val volume: Float,
    @SerializedName("lingua")
    val language: Languages,
    @SerializedName("capitulos")
    val subTitleChapters: List<SubTitleChapter>,
    @SerializedName("vocabulario")
    val vocabulary: MutableSet<Vocabulary?>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubTitleVolume

        if (manga != other.manga) return false
        if (volume != other.volume) return false
        if (language != other.language) return false

        return true
    }

    override fun hashCode(): Int {
        var result = manga.hashCode()
        result = 31 * result + volume.hashCode()
        result = 31 * result + language.hashCode()
        return result
    }
}

