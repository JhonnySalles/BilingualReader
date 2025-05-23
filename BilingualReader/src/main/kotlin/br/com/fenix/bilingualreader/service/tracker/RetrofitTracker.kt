package br.com.fenix.bilingualreader.service.tracker

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitTracker private constructor() {
    companion object {
        private lateinit var INSTANCE: Retrofit
        private lateinit var INSTANCEOAUTH: Retrofit
        private const val BASE_OAUTH_Url = "https://myanimelist.net/v1/oauth2/"
        private const val BASE_API_URL = "https://api.myanimelist.net/v2/"
        private fun getRetrofitInstance(): Retrofit {
            if (!::INSTANCE.isInitialized) {
                synchronized(RetrofitTracker::class) {
                    val httpClient = OkHttpClient.Builder()
                    httpClient.readTimeout(20, TimeUnit.SECONDS)
                    httpClient.connectTimeout(20, TimeUnit.SECONDS)
                    INSTANCE = Retrofit.Builder()
                        .baseUrl(BASE_API_URL)
                        .client(httpClient.build())
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                }
            }
            return INSTANCE
        }

        fun <T> getService(service: Class<T>): T {
            return getRetrofitInstance().create(service)
        }

        fun <T> getOAuth(service: Class<T>): T {
            if (!::INSTANCEOAUTH.isInitialized) {
                synchronized(RetrofitTracker::class) {
                    val httpClient = OkHttpClient.Builder()
                    INSTANCEOAUTH = Retrofit.Builder()
                        .baseUrl(BASE_OAUTH_Url)
                        .client(httpClient.build())
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                }
            }
            return INSTANCEOAUTH.create(service)
        }
    }

}