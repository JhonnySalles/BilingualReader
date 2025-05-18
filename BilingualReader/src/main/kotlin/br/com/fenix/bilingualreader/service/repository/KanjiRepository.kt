package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.KanjiJLPT
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import org.slf4j.LoggerFactory

class KanjiRepository(context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(KanjiRepository::class.java)
    private var mDataBase = DataBase.getDataBase(context).getKanjiJLPTDao()

    fun get(id: Long): KanjiJLPT? {
        return try {
            mDataBase.get(id)
        } catch (e: Exception) {
            mLOGGER.error("Error when get KanjiJLPT: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when get KanjiJLPT: " + e.message)
                recordException(e)
            }
            null
        }
    }

    fun list(): List<KanjiJLPT>? {
        return try {
            mDataBase.list()
        } catch (e: Exception) {
            mLOGGER.error("Error when list KanjiJLPT: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when list KanjiJLPT: " + e.message)
                recordException(e)
            }
            null
        }
    }


    fun getHashMap(): Map<String, Int>? {
        return try {
            mDataBase.list().associate { it.kanji to it.level }
        } catch (e: Exception) {
            mLOGGER.error("Error when get HashMap: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error when get HashMap: " + e.message)
                recordException(e)
            }
            null
        }
    }

}