package br.com.fenix.bilingualreader.service.update

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitUpdate private constructor() {
    companion object {
        private lateinit var INSTANCE: Retrofit
        private const val BASE_API_URL = "https://firebaseappdistribution.googleapis.com/"
        private fun getRetrofitInstance(): Retrofit {
            if (!::INSTANCE.isInitialized) {
                synchronized(RetrofitUpdate::class) {
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
    }

}