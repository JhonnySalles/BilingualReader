package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.KanjiJLPT
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import org.slf4j.LoggerFactory

class KanjiRepository(context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(KanjiRepository::class.java)
    private var mDataBase = DataBase.getDataBase(context).getKanjiJLPTDao()

    fun get(id: Long): KanjiJLPT? {
        return try {
            mDataBase.get(id)
        } catch (e: Exception) {
            mLOGGER.error("Error when get KanjiJLPT: " + e.message, e)
            Telemetry.recordException(e, "Error when get KanjiJLPT: " + e.message)
            null
        }
    }

    fun list(): List<KanjiJLPT>? {
        return try {
            mDataBase.list()
        } catch (e: Exception) {
            mLOGGER.error("Error when list KanjiJLPT: " + e.message, e)
            Telemetry.recordException(e, "Error when list KanjiJLPT: " + e.message)
            null
        }
    }


    fun getHashMap(): Map<String, Int>? {
        return try {
            mDataBase.list().associate { it.kanji to it.level }
        } catch (e: Exception) {
            mLOGGER.error("Error when get HashMap: " + e.message, e)
            Telemetry.recordException(e, "Error when get HashMap: " + e.message)
            null
        }
    }

}