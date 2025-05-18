package br.com.fenix.bilingualreader.util.secrets

import android.content.Context
import android.content.res.AssetManager
import br.com.fenix.bilingualreader.view.ui.detail.manga.MangaDetailFragment
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.util.Properties


class Secrets {

    private val mLOGGER = LoggerFactory.getLogger(MangaDetailFragment::class.java)

    companion object Instance {
        private lateinit var mAssets: AssetManager
        private lateinit var INSTANCE: Secrets
        fun getSecrets(context: Context): Secrets {
            if (!::INSTANCE.isInitialized) {
                mAssets = context.assets
                INSTANCE = Secrets()
            }

            return INSTANCE
        }
    }

    private var MY_ANIME_LIST_CLIENT_ID: String = ""
    private var GOOGLE_ID_TOKEN: String = ""

    init {
        try {
            val properties = Properties()
            val assetManager: AssetManager = mAssets
            val inputStream: InputStream = assetManager.open("secrets.properties")
            properties.load(inputStream)

            MY_ANIME_LIST_CLIENT_ID = properties.getProperty("ANIME_LIST_CLIENT_ID")
            GOOGLE_ID_TOKEN = properties.getProperty("GOOGLE_ID_TOKEN")
        } catch (e: IOException) {
            mLOGGER.error("Error to read secrets: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error to read secrets: " + e.message)
                recordException(e)
            }
        }
    }

    fun getMyAnimeListClientId(): String {
        return MY_ANIME_LIST_CLIENT_ID
    }

    fun getGoogleIdToken(): String {
        return GOOGLE_ID_TOKEN
    }

}