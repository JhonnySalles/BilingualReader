package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.History
import org.slf4j.LoggerFactory

class HistoryRepository(var context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(HistoryRepository::class.java)
    private var mDataBase = DataBase.getDataBase(context).getHistoryDao()

    fun save(obj: History): Long {
        return mDataBase.save(obj)
    }

    fun update(obj: History) {
        mDataBase.update(obj)
    }

    fun delete(obj: History) {
        if (obj.id != null)
            mDataBase.delete(obj)
    }


}