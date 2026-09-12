package br.com.fenix.bilingualreader.service.tracker.model

enum class TrackerServiceType(val displayName: String) {
    MY_ANIME_LIST("MyAnimeList"),
    ANILIST("AniList")
}

data class TrackerSearchResult(
    val id: Long,
    val title: String,
    val coverUrl: String? = null,
    val totalVolumes: Int? = null,
    val totalChapters: Int? = null,
    val status: String? = null,
    val score: Float? = null,
    val synopsis: String? = null,
    val serviceType: TrackerServiceType
)
