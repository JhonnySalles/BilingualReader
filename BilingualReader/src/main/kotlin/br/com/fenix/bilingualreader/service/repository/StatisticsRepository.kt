package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Statistics
import br.com.fenix.bilingualreader.model.enums.Type
import org.slf4j.LoggerFactory
import java.time.LocalDateTime


class StatisticsRepository(var context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(StatisticsRepository::class.java)
    private var mStatisticsDataBase = DataBase.getDataBase(context).getStatisticsDao()
    private val mLibraryRepository: LibraryRepository = LibraryRepository(context)


    fun statistics(): List<Statistics> = mStatisticsDataBase.statistics()

    fun statistics(type: Type, dateStart: LocalDateTime, dateEnd: LocalDateTime): List<Statistics>  {
        return when (type) {
            Type.MANGA -> mStatisticsDataBase.statisticsManga(dateStart, dateEnd)
            Type.BOOK -> mStatisticsDataBase.statisticsBook(dateStart, dateEnd)
        }
    }

    fun statistics(type: Type, year: Int, library: Long?): List<Statistics> {
        val dateStart = LocalDateTime.of(year, 1, 1, 0, 0)
        val dateEnd = LocalDateTime.of(year, 12, 31, 23, 59, 59)

        return if (library != null)
            when (type) {
                Type.MANGA -> mStatisticsDataBase.statisticsManga(dateStart, dateEnd, library)
                Type.BOOK -> mStatisticsDataBase.statisticsBook(dateStart, dateEnd, library)
            }
        else
            when (type) {
                Type.MANGA -> mStatisticsDataBase.statisticsManga(dateStart, dateEnd)
                Type.BOOK -> mStatisticsDataBase.statisticsBook(dateStart, dateEnd)
            }
    }

    fun listYears(type: Type) = mStatisticsDataBase.listYears(type.toString())


    fun getLibraryList(): List<Library> {
        val list = mutableListOf<Library>()
        list.addAll(mLibraryRepository.list(Type.MANGA))
        list.addAll(mLibraryRepository.list(Type.BOOK))
        return list
    }

}