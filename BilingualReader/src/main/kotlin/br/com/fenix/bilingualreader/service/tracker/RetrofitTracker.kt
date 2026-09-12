package br.com.fenix.bilingualreader.service.tracker
 
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
 
class RetrofitTracker private constructor() {
    companion object {
        private var MAL_INSTANCE: Retrofit? = null
        private var MAL_OAUTH_INSTANCE: Retrofit? = null
        private var ANILIST_INSTANCE: Retrofit? = null
        private var ANILIST_OAUTH_INSTANCE: Retrofit? = null

        private const val MAL_BASE_OAUTH_URL = "https://myanimelist.net/v1/oauth2/"
        private const val MAL_BASE_API_URL = "https://api.myanimelist.net/v2/"

        private const val ANILIST_BASE_API_URL = "https://graphql.anilist.co/"
        private const val ANILIST_BASE_OAUTH_URL = "https://anilist.co/api/v2/"

        private fun createHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build()
        }

        private fun createRetrofit(baseUrl: String, client: OkHttpClient = createHttpClient()): Retrofit {
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        @Synchronized
        fun <T> getMalService(service: Class<T>): T {
            if (MAL_INSTANCE == null) {
                MAL_INSTANCE = createRetrofit(MAL_BASE_API_URL)
            }
            return MAL_INSTANCE!!.create(service)
        }

        @Synchronized
        fun <T> getMalOAuth(service: Class<T>): T {
            if (MAL_OAUTH_INSTANCE == null) {
                MAL_OAUTH_INSTANCE = createRetrofit(MAL_BASE_OAUTH_URL)
            }
            return MAL_OAUTH_INSTANCE!!.create(service)
        }

        @Synchronized
        fun <T> getAnilistService(service: Class<T>): T {
            if (ANILIST_INSTANCE == null) {
                ANILIST_INSTANCE = createRetrofit(ANILIST_BASE_API_URL)
            }
            return ANILIST_INSTANCE!!.create(service)
        }

        @Synchronized
        fun <T> getAnilistOAuth(service: Class<T>): T {
            if (ANILIST_OAUTH_INSTANCE == null) {
                ANILIST_OAUTH_INSTANCE = createRetrofit(ANILIST_BASE_OAUTH_URL)
            }
            return ANILIST_OAUTH_INSTANCE!!.create(service)
        }

        // Compatibility aliases
        fun <T> getService(service: Class<T>): T = getMalService(service)
        fun <T> getOAuth(service: Class<T>): T = getMalOAuth(service)
    }
}