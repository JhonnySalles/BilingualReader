package br.com.fenix.bilingualreader.service.tracker.anilist

import android.content.Context
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.TrackStatus
import br.com.fenix.bilingualreader.service.listener.ApiListener
import br.com.fenix.bilingualreader.service.tracker.RetrofitTracker
import br.com.fenix.bilingualreader.service.tracker.model.TrackerSearchResult
import br.com.fenix.bilingualreader.service.tracker.model.TrackerServiceType
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.secrets.Secrets
import com.google.gson.Gson
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AniListTracker(private val context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(AniListTracker::class.java)
    private val mGson = Gson()

    private val mAniListService = RetrofitTracker.getAnilistService(AniListService::class.java)
    private val mAniListOAuth = RetrofitTracker.getAnilistOAuth(AniListService::class.java)

    private var idClient: String = Secrets.getSecrets(context).getAniListClientId()
    private var clientSecret: String = Secrets.getSecrets(context).getAniListClientSecret()
    private var oAuth: AniListOAuth? = null

    init {
        loadToken()
    }

    private fun loadToken() {
        val prefs = GeneralConsts.getSharedPreferences(context)
        val json = prefs.getString(GeneralConsts.KEYS.TRACKER.ANILIST_TOKEN, null)
        if (!json.isNullOrBlank()) {
            try {
                oAuth = mGson.fromJson(json, AniListOAuth::class.java)
            } catch (e: Exception) {
                mLOGGER.error("Error loading AniList OAuth token: ${e.message}", e)
            }
        }
    }

    private fun saveToken(auth: AniListOAuth?) {
        oAuth = auth
        val prefs = GeneralConsts.getSharedPreferences(context)
        prefs.edit().apply {
            if (auth != null) {
                putString(GeneralConsts.KEYS.TRACKER.ANILIST_TOKEN, mGson.toJson(auth))
            } else {
                remove(GeneralConsts.KEYS.TRACKER.ANILIST_TOKEN)
            }
            apply()
        }
    }

    fun getAuthUrl(): String {
        return "https://anilist.co/api/v2/oauth/authorize?client_id=$idClient&response_type=code"
    }

    fun isLoggedIn(): Boolean {
        return oAuth != null && !oAuth!!.isExpired()
    }

    fun logout() {
        saveToken(null)
    }

    fun getToken(): String? {
        return oAuth?.access_token?.let { if (it.isNotBlank()) "Bearer $it" else null }
    }

    // 1 - Realizar o login de autenticação e obtenção de tokens de acesso
    fun login(authCode: String, redirectUri: String, listener: ApiListener<AniListOAuth>) {
        val call = mAniListOAuth.getAccessToken(
            clientId = idClient,
            clientSecret = clientSecret,
            redirectUri = redirectUri,
            code = authCode
        )
        call.enqueue(object : Callback<AniListOAuth> {
            override fun onResponse(call: Call<AniListOAuth>, response: Response<AniListOAuth>) {
                if (response.isSuccessful && response.body() != null) {
                    val auth = response.body()!!
                    saveToken(auth)
                    listener.onSuccess(auth)
                } else {
                    val error = response.errorBody()?.string() ?: response.message()
                    mLOGGER.error("AniList login error: $error")
                    listener.onFailure("AniList Login HTTP ${response.code()}: $error")
                }
            }

            override fun onFailure(call: Call<AniListOAuth>, t: Throwable) {
                mLOGGER.error("AniList login failure: ${t.message}", t)
                listener.onFailure(context.getString(R.string.api_error))
            }
        })
    }

    private inline fun <T> ensureValidToken(listener: ApiListener<T>, crossinline onReady: (token: String) -> Unit) {
        val currentOAuth = oAuth
        if (currentOAuth == null || currentOAuth.access_token.isBlank()) {
            listener.onFailure("User not authenticated in AniList")
            return
        }

        if (currentOAuth.isExpired()) {
            listener.onFailure("AniList token expired. Please login again.")
            return
        }

        onReady("Bearer ${currentOAuth.access_token}")
    }

    // 2 - Busca de mangas e novels por nome, para obter o id, titulo, score, capa
    private val mGraphQLSearchQuery = """
        query (${'$'}search: String, ${'$'}format: MediaFormat) {
          Page(page: 1, perPage: 50) {
            media(search: ${'$'}search, type: MANGA, format: ${'$'}format) {
              id
              title {
                romaji
                english
                native
              }
              coverImage {
                medium
                large
                extraLarge
              }
              format
              status
              chapters
              volumes
              averageScore
              meanScore
              description(asHtml: false)
            }
          }
        }
    """.trimIndent()

    fun searchManga(query: String, listener: ApiListener<List<TrackerSearchResult>>) {
        searchMedia(query, null, listener)
    }

    fun searchNovel(query: String, listener: ApiListener<List<TrackerSearchResult>>) {
        searchMedia(query, "NOVEL", listener)
    }

    private fun searchMedia(query: String, format: String?, listener: ApiListener<List<TrackerSearchResult>>) {
        val variables = mutableMapOf<String, Any?>("search" to query)
        if (!format.isNullOrBlank()) {
            variables["format"] = format
        }

        val payload = AniListGraphQLRequest(
            query = mGraphQLSearchQuery,
            variables = variables
        )

        val call = mAniListService.searchManga(getToken(), payload)
        call.enqueue(object : Callback<AniListResponse> {
            override fun onResponse(call: Call<AniListResponse>, response: Response<AniListResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val mediaList = response.body()?.data?.page?.media.orEmpty()
                    val results = mediaList.map { media ->
                        val avgScore = media.averageScore ?: media.meanScore
                        TrackerSearchResult(
                            id = media.id,
                            title = media.displayTitle.ifBlank { "AniList ID ${media.id}" },
                            coverUrl = media.coverImage?.extraLarge ?: media.coverImage?.large ?: media.coverImage?.medium,
                            totalVolumes = media.volumes,
                            totalChapters = media.chapters,
                            status = media.status,
                            score = avgScore?.let { it / 10f },
                            synopsis = media.description,
                            serviceType = TrackerServiceType.ANILIST
                        )
                    }
                    listener.onSuccess(results)
                } else {
                    val error = response.errorBody()?.string() ?: response.message()
                    mLOGGER.error("AniList search error: $error")
                    listener.onFailure("AniList Search HTTP ${response.code()}: $error")
                }
            }

            override fun onFailure(call: Call<AniListResponse>, t: Throwable) {
                mLOGGER.error("AniList search failure: ${t.message}", t)
                listener.onFailure(context.getString(R.string.api_error))
            }
        })
    }

    // 3 - Realizar o update no registro do usuario (carregar se está na lista + obter score/volume/capítulo, e atualizar)
    private val mGraphQLStatusQuery = """
        query (${'$'}mediaId: Int) {
          Media(id: ${'$'}mediaId, type: MANGA) {
            id
            title {
              romaji
              english
              native
            }
            status
            chapters
            volumes
            mediaListEntry {
              id
              mediaId
              status
              score(format: POINT_10_DECIMAL)
              progress
              progressVolumes
              repeat
            }
          }
        }
    """.trimIndent()

    fun getUserMediaStatus(mediaId: Long, listener: ApiListener<AniListMediaListEntry?>) {
        val payload = AniListGraphQLRequest(
            query = mGraphQLStatusQuery,
            variables = mapOf("mediaId" to mediaId.toInt())
        )

        val call = mAniListService.getMediaStatus(getToken(), payload)
        call.enqueue(object : Callback<AniListMediaStatusResponse> {
            override fun onResponse(call: Call<AniListMediaStatusResponse>, response: Response<AniListMediaStatusResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val entry = response.body()?.data?.media?.mediaListEntry
                    listener.onSuccess(entry)
                } else {
                    val error = response.errorBody()?.string() ?: response.message()
                    mLOGGER.error("AniList getMediaStatus error: $error")
                    listener.onFailure("AniList Status HTTP ${response.code()}: $error")
                }
            }

            override fun onFailure(call: Call<AniListMediaStatusResponse>, t: Throwable) {
                mLOGGER.error("AniList getMediaStatus failure: ${t.message}", t)
                listener.onFailure(context.getString(R.string.api_error))
            }
        })
    }

    private val mGraphQLSaveEntryMutation = """
        mutation (${'$'}mediaId: Int, ${'$'}status: MediaListStatus, ${'$'}score: Float, ${'$'}progress: Int, ${'$'}progressVolumes: Int) {
          SaveMediaListEntry(mediaId: ${'$'}mediaId, status: ${'$'}status, score: ${'$'}score, progress: ${'$'}progress, progressVolumes: ${'$'}progressVolumes) {
            id
            mediaId
            status
            score(format: POINT_10_DECIMAL)
            progress
            progressVolumes
            repeat
          }
        }
    """.trimIndent()

    fun updateUserMedia(
        mediaId: Long,
        status: TrackStatus? = null,
        score: Float? = null,
        volume: Int? = null,
        chapter: Int? = null,
        listener: ApiListener<AniListMediaListEntry>
    ) {
        ensureValidToken(listener) { bearerToken ->
            val aniStatusStr = status?.let {
                when (it) {
                    TrackStatus.READING -> "CURRENT"
                    TrackStatus.PLAN_TO_READ -> "PLANNING"
                    TrackStatus.COMPLETED -> "COMPLETED"
                    TrackStatus.DROPPED -> "DROPPED"
                    TrackStatus.ON_HOLD -> "PAUSED"
                    TrackStatus.REREADING -> "REPEATING"
                }
            }

            val variables = mutableMapOf<String, Any?>("mediaId" to mediaId.toInt())
            if (aniStatusStr != null) variables["status"] = aniStatusStr
            if (score != null) variables["score"] = score
            if (chapter != null) variables["progress"] = chapter
            if (volume != null) variables["progressVolumes"] = volume

            val payload = AniListGraphQLRequest(
                query = mGraphQLSaveEntryMutation,
                variables = variables
            )

            val call = mAniListService.saveMediaListEntry(bearerToken, payload)
            call.enqueue(object : Callback<AniListSaveEntryResponse> {
                override fun onResponse(call: Call<AniListSaveEntryResponse>, response: Response<AniListSaveEntryResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val entry = response.body()?.data?.saveMediaListEntry
                        if (entry != null) {
                            listener.onSuccess(entry)
                        } else {
                            listener.onFailure("AniList empty response on SaveMediaListEntry")
                        }
                    } else {
                        val error = response.errorBody()?.string() ?: response.message()
                        mLOGGER.error("AniList SaveMediaListEntry error: $error")
                        listener.onFailure("AniList Save Entry HTTP ${response.code()}: $error")
                    }
                }

                override fun onFailure(call: Call<AniListSaveEntryResponse>, t: Throwable) {
                    mLOGGER.error("AniList SaveMediaListEntry failure: ${t.message}", t)
                    listener.onFailure(context.getString(R.string.api_error))
                }
            })
        }
    }
}
