package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Statistics
import br.com.fenix.bilingualreader.model.enums.Type
import org.slf4j.LoggerFactory
import java.time.LocalDateTime


class StatisticsRepository(var context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(StatisticsRepository::class.java)
    private var mDataBase = DataBase.getDataBase(context).getStatisticsDao()


    fun statistics(): List<Statistics> = mDataBase.statistics()

    fun statistics(type: Type, dateStart: LocalDateTime, dateEnd: LocalDateTime): List<Statistics>  {
        return when (type) {
            Type.MANGA -> mDataBase.statisticsManga(dateStart, dateEnd)
            Type.BOOK -> mDataBase.statisticsBook(dateStart, dateEnd)
        }
    }

    fun statistics(type: Type, year: Int): List<Statistics> {
        val dateStart = LocalDateTime.of(year, 1, 1, 0, 0)
        val dateEnd = LocalDateTime.of(year, 12, 31, 23, 59, 59)

        return when (type) {
            Type.MANGA -> mDataBase.statisticsManga(dateStart, dateEnd)
            Type.BOOK -> mDataBase.statisticsBook(dateStart, dateEnd)
        }
    }

    fun listYears() = mDataBase.listYears()

}