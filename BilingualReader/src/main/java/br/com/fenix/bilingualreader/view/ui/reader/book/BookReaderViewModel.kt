package br.com.fenix.bilingualreader.view.ui.reader.book

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import org.slf4j.LoggerFactory

class BookReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val mContext = application.applicationContext
    private var mPreferences: SharedPreferences? = GeneralConsts.getSharedPreferences(mContext)

    private val mLOGGER = LoggerFactory.getLogger(BookReaderViewModel::class.java)

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
    }

    private fun savePreferences() {

    }

}