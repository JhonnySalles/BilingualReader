package br.com.fenix.bilingualreader.service.tracker.anilist

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class AniListOAuth(
    @SerializedName("token_type")
    val token_type: String = "Bearer",
    @SerializedName("expires_in")
    val expires_in: Long = 0L,
    @SerializedName("access_token")
    val access_token: String,
    @SerializedName("refresh_token")
    val refresh_token: String? = null,
    val created_at: Long = System.currentTimeMillis()
) : Serializable {
    fun isExpired() = if (expires_in > 0) System.currentTimeMillis() > created_at + (expires_in * 1000) else false
}

data class AniListGraphQLRequest(
    val query: String,
    val variables: Map<String, Any?> = emptyMap()
)

data class AniListResponse(
    val data: AniListPageData?
)

data class AniListPageData(
    @SerializedName("Page")
    val page: AniListPage?
)

data class AniListPage(
    val media: List<AniListMedia>?
)

data class AniListMedia(
    val id: Long,
    val title: AniListTitle?,
    val coverImage: AniListCoverImage?,
    val format: String?,
    val status: String?,
    val chapters: Int?,
    val volumes: Int?,
    val averageScore: Int? = null,
    val meanScore: Int? = null,
    val description: String? = null
) {
    val displayTitle: String
        get() = title?.english?.takeIf { it.isNotBlank() }
            ?: title?.romaji?.takeIf { it.isNotBlank() }
            ?: title?.native.orEmpty()
}

data class AniListTitle(
    val romaji: String?,
    val english: String?,
    val native: String?
)

data class AniListCoverImage(
    val medium: String?,
    val large: String?,
    val extraLarge: String? = null
)

data class AniListMediaStatusResponse(
    val data: AniListMediaStatusData?
)

data class AniListMediaStatusData(
    @SerializedName("Media")
    val media: AniListMediaDetail?
)

data class AniListMediaDetail(
    val id: Long,
    val title: AniListTitle?,
    val status: String?,
    val chapters: Int?,
    val volumes: Int?,
    val mediaListEntry: AniListMediaListEntry?
)

data class AniListMediaListEntry(
    val id: Long,
    val mediaId: Long? = null,
    val status: String? = null,
    val score: Float? = null,
    val progress: Int = 0,
    val progressVolumes: Int = 0,
    val repeat: Int = 0
)

data class AniListSaveEntryResponse(
    val data: AniListSaveEntryData?
)

data class AniListSaveEntryData(
    @SerializedName("SaveMediaListEntry")
    val saveMediaListEntry: AniListMediaListEntry?
)

