package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Kanjax
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import org.slf4j.LoggerFactory

class KanjaxRepository(context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(KanjaxRepository::class.java)
    private var mDataBase = DataBase.getDataBase(context).getKanjaxDao()

    fun get(id: Long): Kanjax? {
        return try {
            mDataBase.get(id)
        } catch (e: Exception) {
            mLOGGER.error("Error when get Kanjax: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when get SubTitle: " + e.message)
                recordException(e)
            }
            null
        }
    }

    fun get(kanji: String): Kanjax? {
        return try {
            mDataBase.get(kanji)
        } catch (e: Exception) {
            mLOGGER.error("Error when get Kanjax: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when get SubTitle: " + e.message)
                recordException(e)
            }
            null
        }
    }

    fun list(): List<Kanjax>? {
        return try {
            mDataBase.list()
        } catch (e: Exception) {
            mLOGGER.error("Error when list Kanjax: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when get SubTitle: " + e.message)
                recordException(e)
            }
            null
        }
    }

}