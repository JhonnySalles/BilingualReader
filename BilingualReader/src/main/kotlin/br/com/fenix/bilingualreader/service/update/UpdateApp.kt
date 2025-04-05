package br.com.fenix.bilingualreader.service.update

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Environment
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.service.listener.ApiListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.DecimalFormat
import java.text.NumberFormat


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
        DownloadApp(link).execute()
    }


    private inner class DownloadApp(var url: String) : AsyncTask<String?, Int?, File?>() {

        private lateinit var mPopup : AlertDialog
        private lateinit var mProgress : ProgressBar
        private lateinit var mPercent : TextView
        private lateinit var mSize : TextView

        override fun onPreExecute() {
            super.onPreExecute()

            val layout = LayoutInflater.from(mContext).inflate(R.layout.popup_update_app, null, false)
            mProgress = layout.findViewById(R.id.popup_update_progress_bar)
            mPercent = layout.findViewById(R.id.popup_update_percent)
            mSize = layout.findViewById(R.id.popup_update_size)

            mPopup = MaterialAlertDialogBuilder(mContext, R.style.AppCompatMaterialAlertDialog)
                .setView(layout)
                .create()

            mPopup.show()
        }

        override fun doInBackground(vararg params: String?): File? {
            var count = 0
            return try {
                val download = mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                val file = File(download, "BilingualReader.apk");
                if (file.exists())
                    file.delete()

                val output: OutputStream = FileOutputStream(file)

                val request: Request = Request.Builder().url(url).build()
                val response = OkHttpClient().newCall(request).execute();

                val body = response.body()
                val length = body!!.contentLength()
                val byte = body.byteStream()

                val input = BufferedInputStream(byte)

                val data = ByteArray(1024)
                var total: Long = 0

                while ((input.read(data).also { count = it }) != -1) {
                    total += count.toLong()

                    publishProgress(total.toInt(), length.toInt())
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()

                file
            } catch (e: IOException) {
                mLOGGER.error("Error to download update file", e)
                null
            }
        }

        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)
            val progress = values[0]!!
            val length = values[1]!!

            val percent = progress * 100 / length
            mProgress.progress = percent

            mPercent.text = mContext.getString(R.string.percent, percent)
            mSize.text = mContext.getString(R.string.file_size_percent, bytesCaption(progress.toLong()), bytesCaption(length.toLong()))
        }

        override fun onPostExecute(apk: File?) {
            super.onPostExecute(apk)
            mPopup.dismiss()

            if (apk != null) {
                val uri = FileProvider.getUriForFile(mContext, "br.com.fenix.bilingualreader.provider", apk)
                val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                mContext.startActivity(intent)
            }
        }


        private val mSpaceKB: Double = 1024.0
        private val mSpaceMB: Double = 1024 * mSpaceKB
        private val mSpaceGB: Double = 1024 * mSpaceMB
        private val mSpaceTB: Double = 1024 * mSpaceGB

        private fun bytesCaption(sizeInBytes: Long): String {
            val nf: NumberFormat = DecimalFormat()
            nf.setMaximumFractionDigits(2)

            return try {
                if (sizeInBytes < mSpaceKB) {
                    nf.format(sizeInBytes) + " Byte(s)"
                } else if (sizeInBytes < mSpaceMB) {
                    nf.format(sizeInBytes / mSpaceKB) + " KB"
                } else if (sizeInBytes < mSpaceGB) {
                    nf.format(sizeInBytes / mSpaceMB) + " MB"
                } else if (sizeInBytes < mSpaceTB) {
                    nf.format(sizeInBytes / mSpaceGB) + " GB"
                } else {
                    nf.format(sizeInBytes / mSpaceTB) + " TB"
                }
            } catch (e: Exception) {
                "$sizeInBytes Byte(s)"
            }
        }
    }

}