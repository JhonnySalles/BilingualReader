package br.com.fenix.bilingualreader.service.tracker.anilist

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface AniListService {

    @POST("oauth/token")
    @FormUrlEncoded
    fun getAccessToken(
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("code") code: String
    ): Call<AniListOAuth>

    @POST("/")
    @Headers("Content-Type: application/json", "Accept: application/json")
    fun searchManga(
        @Header("Authorization") token: String? = null,
        @Body request: AniListGraphQLRequest
    ): Call<AniListResponse>

    @POST("/")
    @Headers("Content-Type: application/json", "Accept: application/json")
    fun getMediaStatus(
        @Header("Authorization") token: String?,
        @Body request: AniListGraphQLRequest
    ): Call<AniListMediaStatusResponse>

    @POST("/")
    @Headers("Content-Type: application/json", "Accept: application/json")
    fun saveMediaListEntry(
        @Header("Authorization") token: String,
        @Body request: AniListGraphQLRequest
    ): Call<AniListSaveEntryResponse>
}
