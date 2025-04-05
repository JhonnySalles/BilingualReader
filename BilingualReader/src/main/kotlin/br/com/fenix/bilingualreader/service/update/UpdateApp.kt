package br.com.fenix.bilingualreader.service.update

import android.content.Context
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.service.listener.ApiListener
import br.com.fenix.bilingualreader.service.tracker.mal.MyAnimeListService
import br.com.fenix.bilingualreader.service.tracker.mal.MyAnimeListTracker
import br.com.fenix.bilingualreader.service.tracker.mal.OAuth
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.common.io.ByteStreams
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.io.InputStream


class UpdateApp(var mContext: Context) {

    private val mLOGGER = LoggerFactory.getLogger(UpdateApp::class.java)

    private val mUpdate = RetrofitUpdate.getService(AppDistributionService::class.java)
    private var token: String? = null

    private fun getToken() : String {
        if (token == null) {
            val json: InputStream = mContext.assets.open("app-distribution.json")
            val credential = GoogleCredential.fromStream(json)
            token = credential.accessToken
            token!!
        }

        return token!!
    }

    fun consult(listener: ApiListener<Releases>) {
        val token = getToken()
        val call = mUpdate.getLastVersion(token)

        call.enqueue(object : Callback<Releases> {
            override fun onResponse(call: Call<Releases>, response: Response<Releases>) {
                if (response.code() == 200)
                    listener.onSuccess(response.body()!!)
                else
                    listener.onFailure(response.raw().toString())
            }

            override fun onFailure(call: Call<Releases>, t: Throwable) {
                mLOGGER.error(t.message, t.stackTrace)
                listener.onFailure(mContext.getString(R.string.api_error))
            }
        })
    }

    fun download(link : String) {

        val request: Request = Request.Builder().url(link).build()
        OkHttpClient().newCall(request).enqueue(object : Callback<ByteStreams> {
            override fun onResponse(call: Call<ByteStreams>, response: Response<ByteStreams>) {

            }

            override fun onFailure(call: Call<ByteStreams>, t: Throwable) {
                mLOGGER.error(t.message, t.stackTrace)
            }
        })
    }
}