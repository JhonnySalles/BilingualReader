package com.artifex.mu

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Handler

internal class ProgressDialogX(context: Context?) : ProgressDialog(context) {
    var isCancelled = false
        private set

    override fun cancel() {
        isCancelled = true
        super.cancel()
    }
}

abstract class SearchTask(private val mContext: Context, private val mCore: MuPDFCore?) {
    private val mHandler: Handler
    private val mAlertBuilder: AlertDialog.Builder
    private var mSearchTask: AsyncTask<Void?, Int?, SearchTaskResult?>? = null

    init {
        mHandler = Handler()
        mAlertBuilder = AlertDialog.Builder(mContext)
    }

    protected abstract fun onTextFound(result: SearchTaskResult?)
    fun stop() {
        if (mSearchTask != null) {
            mSearchTask!!.cancel(true)
            mSearchTask = null
        }
    }

    fun go(text: String?, direction: Int, displayPage: Int, searchPage: Int) {
        if (mCore == null) return
        stop()
        val startIndex = if (searchPage == -1) displayPage else searchPage + direction
        val progressDialog = ProgressDialogX(mContext)
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog.setTitle("Procurando...")
        progressDialog.setOnCancelListener { stop() }
        progressDialog.max = mCore.countPages()
        mSearchTask = object : AsyncTask<Void?, Int?, SearchTaskResult?>() {
            protected override fun doInBackground(vararg params: Void?): SearchTaskResult? {
                var index = startIndex
                while (0 <= index && index < mCore.countPages() && !isCancelled) {
                    publishProgress(index)
                    val searchHits = mCore.searchPage(index, text)
                    if (searchHits != null && searchHits.size > 0) return SearchTaskResult(text!!, index, searchHits)
                    index += direction
                }
                return null
            }

            protected override fun onPostExecute(result: SearchTaskResult?) {
                progressDialog.cancel()
                if (result != null) {
                    onTextFound(result)
                } else {
                    mAlertBuilder.setTitle("Texto não encontrado")
                    val alert = mAlertBuilder.create()
                    alert.setButton(
                        AlertDialog.BUTTON_POSITIVE, "Fechar",
                        null as DialogInterface.OnClickListener?
                    )
                    alert.show()
                }
            }

            override fun onCancelled() {
                progressDialog.cancel()
            }

            protected override fun onProgressUpdate(vararg values: Int?) {
                progressDialog.progress = values[0] ?: 0
            }

            override fun onPreExecute() {
                super.onPreExecute()
                mHandler.postDelayed({
                    if (!progressDialog.isCancelled) {
                        progressDialog.show()
                        progressDialog.progress = startIndex
                    }
                }, SEARCH_PROGRESS_DELAY.toLong())
            }
        }
        mSearchTask!!.execute()
    }

    companion object {
        private const val SEARCH_PROGRESS_DELAY = 200
    }
}