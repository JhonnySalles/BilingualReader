package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.History
import org.slf4j.LoggerFactory

class HistoryRepository(var context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(HistoryRepository::class.java)
    private var mDataBase = DataBase.getDataBase(context).getHistoryDao()


    fun save(obj: History): Long {
        return if (obj.id != null) {
            mDataBase.update(obj)
            obj.id!!
        } else
            mDataBase.save(obj)
    }


}