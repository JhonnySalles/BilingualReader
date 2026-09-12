package br.com.fenix.bilingualreader.service.tracker.mal

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

class MyAnimeListTracker(var mContext: Context) {

    private val mLOGGER = LoggerFactory.getLogger(MyAnimeListTracker::class.java)
    private val mGson = Gson()

    private val mMyAnimeListAuth = RetrofitTracker.getMalOAuth(MyAnimeListService::class.java)
    private val mMyAnimeList = RetrofitTracker.getMalService(MyAnimeListService::class.java)
    private var idClient: String = Secrets.getSecrets(mContext).getMyAnimeListClientId()
    private var oAuth: OAuth? = null

    init {
        loadToken()
    }

    private fun loadToken() {
        val prefs = GeneralConsts.getSharedPreferences(mContext)
        val json = prefs.getString(GeneralConsts.KEYS.TRACKER.MAL_TOKEN, null)
        if (!json.isNullOrBlank()) {
            try {
                oAuth = mGson.fromJson(json, OAuth::class.java)
            } catch (e: Exception) {
                mLOGGER.error("Error loading MAL OAuth token: ${e.message}", e)
            }
        }
    }

    private fun saveToken(auth: OAuth?) {
        oAuth = auth
        val prefs = GeneralConsts.getSharedPreferences(mContext)
        prefs.edit().apply {
            if (auth != null) {
                putString(GeneralConsts.KEYS.TRACKER.MAL_TOKEN, mGson.toJson(auth))
            } else {
                remove(GeneralConsts.KEYS.TRACKER.MAL_TOKEN)
            }
            apply()
        }
    }

    fun getAuthUrl(codeChallenge: String): String {
        return "https://myanimelist.net/v1/oauth2/authorize?response_type=code&client_id=$idClient&code_challenge=$codeChallenge&code_challenge_method=plain"
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

    private fun getIdClientHeader(): String? = if (isLoggedIn()) null else idClient

    // 1 - Realizar o login de autenticação e obtenção de tokens de acesso
    fun login(authCode: String, codeVerifier: String, listener: ApiListener<OAuth>) {
        val call = mMyAnimeListAuth.login(
            idClient = idClient,
            authCode = authCode,
            codeVerifier = codeVerifier
        )
        call.enqueue(object : Callback<OAuth> {
            override fun onResponse(call: Call<OAuth>, response: Response<OAuth>) {
                if (response.isSuccessful && response.body() != null) {
                    val auth = response.body()!!
                    saveToken(auth)
                    listener.onSuccess(auth)
                } else {
                    val error = response.errorBody()?.string() ?: response.message()
                    mLOGGER.error("MAL login error: $error")
                    listener.onFailure("MAL Login HTTP ${response.code()}: $error")
                }
            }

            override fun onFailure(call: Call<OAuth>, t: Throwable) {
                mLOGGER.error("MAL login failure: ${t.message}", t)
                listener.onFailure(mContext.getString(R.string.api_error))
            }
        })
    }

    fun refreshToken(listener: ApiListener<OAuth>? = null) {
        val currentOAuth = oAuth
        if (currentOAuth == null || currentOAuth.refresh_token.isBlank()) {
            listener?.onFailure("No refresh token available")
            return
        }

        val call = mMyAnimeListAuth.refreshToken(
            idClient = idClient,
            refreshToken = currentOAuth.refresh_token
        )
        call.enqueue(object : Callback<OAuth> {
            override fun onResponse(call: Call<OAuth>, response: Response<OAuth>) {
                if (response.isSuccessful && response.body() != null) {
                    val newAuth = response.body()!!
                    saveToken(newAuth)
                    listener?.onSuccess(newAuth)
                } else {
                    val error = response.errorBody()?.string() ?: response.message()
                    mLOGGER.error("MAL refresh token error: $error")
                    listener?.onFailure("MAL refresh token error: $error")
                }
            }

            override fun onFailure(call: Call<OAuth>, t: Throwable) {
                mLOGGER.error("MAL refresh token failure: ${t.message}", t)
                listener?.onFailure(mContext.getString(R.string.api_error))
            }
        })
    }

    private inline fun <T> ensureValidToken(listener: ApiListener<T>, crossinline onReady: (token: String) -> Unit) {
        val currentOAuth = oAuth
        if (currentOAuth == null) {
            listener.onFailure("User not authenticated in MyAnimeList")
            return
        }

        if (currentOAuth.isExpired()) {
            refreshToken(object : ApiListener<OAuth> {
                override fun onSuccess(result: OAuth) {
                    val bearerToken = "Bearer ${result.access_token}"
                    onReady(bearerToken)
                }

                override fun onFailure(message: String) {
                    listener.onFailure(message)
                }
            })
        } else {
            val bearerToken = "Bearer ${currentOAuth.access_token}"
            onReady(bearerToken)
        }
    }

    // 2 - Busca de mangas e novels por nome, para obter o id, titulo, score, capa
    fun searchManga(search: String, listener: ApiListener<List<TrackerSearchResult>>) {
        val fields = "id,title,main_picture,mean,num_volumes,num_chapters,status,synopsis,media_type"
        val call = mMyAnimeList.getListManga(getToken(), getIdClientHeader(), search, limit = 50, fields = fields)
        call.enqueue(object : Callback<MalMangaList> {
            override fun onResponse(call: Call<MalMangaList>, response: Response<MalMangaList>) {
                if (response.isSuccessful && response.body() != null) {
                    val list = response.body()!!.data.map { node ->
                        val item = node.item
                        TrackerSearchResult(
                            id = item.id.toLong(),
                            title = item.title.ifBlank { "MAL ID ${item.id}" },
                            coverUrl = item.mainPicture?.large ?: item.mainPicture?.medium,
                            totalVolumes = if (item.volumes > 0) item.volumes else null,
                            totalChapters = if (item.chapters > 0) item.chapters else null,
                            status = item.status?.name,
                            score = item.mean?.toFloatOrNull(),
                            synopsis = item.synopsis,
                            serviceType = TrackerServiceType.MY_ANIME_LIST
                        )
                    }
                    listener.onSuccess(list)
                } else {
                    val error = response.errorBody()?.string() ?: response.message()
                    listener.onFailure("MAL Search error: $error")
                }
            }

            override fun onFailure(call: Call<MalMangaList>, t: Throwable) {
                mLOGGER.error("MAL search failure: ${t.message}", t)
                listener.onFailure(mContext.getString(R.string.api_error))
            }
        })
    }

    fun searchNovel(search: String, listener: ApiListener<List<TrackerSearchResult>>) {
        val fields = "id,title,main_picture,mean,num_volumes,num_chapters,status,synopsis,media_type"
        val call = mMyAnimeList.getListManga(getToken(), getIdClientHeader(), search, limit = 50, fields = fields)
        call.enqueue(object : Callback<MalMangaList> {
            override fun onResponse(call: Call<MalMangaList>, response: Response<MalMangaList>) {
                if (response.isSuccessful && response.body() != null) {
                    val list = response.body()!!.data
                        .map { it.item }
                        .filter { it.mediaType == MEDIA.NOVEL }
                        .map { item ->
                            TrackerSearchResult(
                                id = item.id.toLong(),
                                title = item.title.ifBlank { "MAL ID ${item.id}" },
                                coverUrl = item.mainPicture?.large ?: item.mainPicture?.medium,
                                totalVolumes = if (item.volumes > 0) item.volumes else null,
                                totalChapters = if (item.chapters > 0) item.chapters else null,
                                status = item.status?.name,
                                score = item.mean?.toFloatOrNull(),
                                synopsis = item.synopsis,
                                serviceType = TrackerServiceType.MY_ANIME_LIST
                            )
                        }
                    listener.onSuccess(list)
                } else {
                    val error = response.errorBody()?.string() ?: response.message()
                    listener.onFailure("MAL Search error: $error")
                }
            }

            override fun onFailure(call: Call<MalMangaList>, t: Throwable) {
                mLOGGER.error("MAL search failure: ${t.message}", t)
                listener.onFailure(mContext.getString(R.string.api_error))
            }
        })
    }

    // 3 - Realizar o update no registro do usuário (carregar se está na lista + obter score/volume/capítulo, e atualizar)
    fun getUserMangaStatus(idManga: Long, listener: ApiListener<MalStatus?>) {
        ensureValidToken(listener) { bearerToken ->
            val call = mMyAnimeList.getMangaDetail(
                token = bearerToken,
                idClient = null,
                idManga = idManga,
                fields = "id,title,my_list_status,num_volumes,num_chapters"
            )
            call.enqueue(object : Callback<MalMangaDetail> {
                override fun onResponse(call: Call<MalMangaDetail>, response: Response<MalMangaDetail>) {
                    if (response.isSuccessful && response.body() != null) {
                        listener.onSuccess(response.body()!!.myListStatus)
                    } else {
                        val error = response.errorBody()?.string() ?: response.message()
                        listener.onFailure("MAL Get Status error: $error")
                    }
                }

                override fun onFailure(call: Call<MalMangaDetail>, t: Throwable) {
                    mLOGGER.error("MAL Get Status failure: ${t.message}", t)
                    listener.onFailure(mContext.getString(R.string.api_error))
                }
            })
        }
    }

    fun updateUserManga(
        idManga: Long,
        status: TrackStatus? = null,
        score: Int? = null,
        volume: Int? = null,
        chapter: Int? = null,
        isRereading: Boolean? = null,
        listener: ApiListener<MalStatus>
    ) {
        ensureValidToken(listener) { bearerToken ->
            val malStatusStr = status?.let {
                when (it) {
                    TrackStatus.READING -> "reading"
                    TrackStatus.COMPLETED -> "completed"
                    TrackStatus.ON_HOLD -> "on_hold"
                    TrackStatus.DROPPED -> "dropped"
                    TrackStatus.PLAN_TO_READ -> "plan_to_read"
                    TrackStatus.REREADING -> "reading"
                }
            }

            val call = mMyAnimeList.updateMangaList(
                token = bearerToken,
                idManga = idManga,
                status = malStatusStr,
                isRereading = isRereading ?: (status == TrackStatus.REREADING),
                score = score,
                volumesRead = volume,
                chaptersRead = chapter
            )
            call.enqueue(object : Callback<MalStatus> {
                override fun onResponse(call: Call<MalStatus>, response: Response<MalStatus>) {
                    if (response.isSuccessful && response.body() != null) {
                        listener.onSuccess(response.body()!!)
                    } else {
                        val error = response.errorBody()?.string() ?: response.message()
                        mLOGGER.error("MAL updateMangaList error: $error")
                        listener.onFailure("MAL Update error: $error")
                    }
                }

                override fun onFailure(call: Call<MalStatus>, t: Throwable) {
                    mLOGGER.error("MAL updateMangaList failure: ${t.message}", t)
                    listener.onFailure(mContext.getString(R.string.api_error))
                }
            })
        }
    }

    // Legacy / Convenience overloads
    fun getListManga(search: String, listener: ApiListener<List<MalMangaDetail>>) {
        val call = mMyAnimeList.getListManga(getToken(), getIdClientHeader(), search, limit = 20)
        call.enqueue(object : Callback<MalMangaList> {
            override fun onResponse(call: Call<MalMangaList>, response: Response<MalMangaList>) {
                if (response.isSuccessful && response.body() != null) {
                    listener.onSuccess(MalTransform.getList(response.body()!!.data))
                } else {
                    listener.onFailure(response.toString())
                }
            }

            override fun onFailure(call: Call<MalMangaList>, t: Throwable) {
                mLOGGER.error(t.message, t)
                listener.onFailure(mContext.getString(R.string.api_error))
            }
        })
    }

    fun getListNovel(search: String, listener: ApiListener<List<MalMangaDetail>>) {
        val call = mMyAnimeList.getListManga(getToken(), getIdClientHeader(), search, limit = 50)
        call.enqueue(object : Callback<MalMangaList> {
            override fun onResponse(call: Call<MalMangaList>, response: Response<MalMangaList>) {
                if (response.isSuccessful && response.body() != null) {
                    listener.onSuccess(MalTransform.getList(response.body()!!.data).filter { it.mediaType == MEDIA.NOVEL })
                } else {
                    listener.onFailure(response.toString())
                }
            }

            override fun onFailure(call: Call<MalMangaList>, t: Throwable) {
                mLOGGER.error(t.message, t)
                listener.onFailure(mContext.getString(R.string.api_error))
            }
        })
    }
}