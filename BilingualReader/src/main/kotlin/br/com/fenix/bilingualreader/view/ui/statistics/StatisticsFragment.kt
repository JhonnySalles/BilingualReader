package br.com.fenix.bilingualreader.view.ui.statistics

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.StatisticsRepository
import eightbitlab.com.blurview.BlurView
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.math.round


class StatisticsFragment : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(StatisticsFragment::class.java)

    private var mRepository: StatisticsRepository = StatisticsRepository(requireContext())


    private lateinit var mRoot: FrameLayout
    private lateinit var mProgress: BlurView

    // --------------------------------------------------------- Manga / Comic ---------------------------------------------------------
    private lateinit var mMangaReading: TextView
    private lateinit var mMangaToRead: TextView
    private lateinit var mMangaLibrary: TextView
    private lateinit var mMangaRead: TextView

    private lateinit var mMangaCompletePages: TextView
    private lateinit var mMangaCompleteTimes: TextView

    private lateinit var mMangaCurrentPages: TextView
    private lateinit var mMangaCurrentTimes: TextView

    private lateinit var mMangaTotalPages: TextView
    private lateinit var mMangaTotalTime: TextView
    private lateinit var mMangaReadingAverage: TextView

    // --------------------------------------------------------- Book ---------------------------------------------------------

    private lateinit var mBookReading: TextView
    private lateinit var mBookToRead: TextView
    private lateinit var mBookLibrary: TextView
    private lateinit var mBookRead: TextView

    private lateinit var mBookCompletePages: TextView
    private lateinit var mBookCompleteTimes: TextView

    private lateinit var mBookCurrentPages: TextView
    private lateinit var mBookCurrentTimes: TextView

    private lateinit var mBookTotalPages: TextView
    private lateinit var mBookTotalTime: TextView
    private lateinit var mBookReadingAverage: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mRoot = view.findViewById(R.id.frame_statistics_root)
        mProgress = view.findViewById(R.id.fragment_statistics_progress)

        mMangaReading = view.findViewById(R.id.statistics_book_reading)
        mMangaToRead = view.findViewById(R.id.statistics_book_to_read)
        mMangaLibrary = view.findViewById(R.id.statistics_book_library)
        mMangaRead = view.findViewById(R.id.statistics_book_read)
        mMangaCompletePages = view.findViewById(R.id.statistics_book_completed_pages)
        mMangaCompleteTimes = view.findViewById(R.id.statistics_book_completed_time)
        mMangaCurrentPages = view.findViewById(R.id.statistics_book_current_pages)
        mMangaCurrentTimes = view.findViewById(R.id.statistics_book_current_time)
        mMangaTotalPages = view.findViewById(R.id.statistics_book_total_read_pages)
        mMangaTotalTime = view.findViewById(R.id.statistics_book_total_read_times)
        mMangaReadingAverage = view.findViewById(R.id.statistics_book_total_read_average)

        mBookReading = view.findViewById(R.id.statistics_manga_reading)
        mBookToRead = view.findViewById(R.id.statistics_manga_to_read)
        mBookLibrary = view.findViewById(R.id.statistics_manga_library)
        mBookRead = view.findViewById(R.id.statistics_manga_read)
        mBookCompletePages = view.findViewById(R.id.statistics_manga_completed_pages)
        mBookCompleteTimes = view.findViewById(R.id.statistics_manga_completed_time)
        mBookCurrentPages = view.findViewById(R.id.statistics_manga_current_pages)
        mBookCurrentTimes = view.findViewById(R.id.statistics_manga_current_time)
        mBookTotalPages = view.findViewById(R.id.statistics_manga_total_read_pages)
        mBookTotalTime = view.findViewById(R.id.statistics_manga_total_read_times)
        mBookReadingAverage = view.findViewById(R.id.statistics_manga_total_read_average)

        view.findViewById<TextView>(R.id.statistics_manga_chart_title).text = getString(R.string.statistics_read_by_month, getString(R.string.statistics_sector_manga))
        view.findViewById<TextView>(R.id.statistics_book_chart_title).text = getString(R.string.statistics_read_by_month, getString(R.string.statistics_sector_book))
    }

    private fun loadStatistics() {
        try {
            mProgress.visibility = View.VISIBLE

            val statistics = mRepository.statistics()

            for (statistic in statistics) {
                when (statistic.type) {
                    Type.MANGA -> {
                        mMangaReading.text = statistic.reading.toString()
                        mMangaToRead.text = statistic.toRead.toString()
                        mMangaLibrary.text = statistic.library.toString()
                        mMangaRead.text = statistic.read.toString()
                        mMangaCompletePages.text = statistic.completeReadingPages.toString()
                        mMangaCompleteTimes.text = generateSeconds(statistic.completeReadingSeconds)
                        mMangaCurrentPages.text = statistic.currentReadingPages.toString()
                        mMangaCurrentTimes.text = generateSeconds(statistic.currentReadingSeconds)
                        mMangaTotalPages.text = statistic.totalReadPages.toString()
                        mMangaTotalTime.text = generateSeconds(statistic.totalReadSeconds)
                        mMangaReadingAverage.text = getString(R.string.statistics_average, round(statistic.totalReadSeconds.toFloat() / statistic.totalReadPages / 60).toInt())
                    }

                    Type.BOOK -> {
                        mBookReading.text = statistic.reading.toString()
                        mBookToRead.text = statistic.toRead.toString()
                        mBookLibrary.text = statistic.library.toString()
                        mBookRead.text = statistic.read.toString()
                        mBookCompletePages.text = statistic.completeReadingPages.toString()
                        mBookCompleteTimes.text = generateSeconds(statistic.completeReadingSeconds)
                        mBookCurrentPages.text = statistic.currentReadingPages.toString()
                        mBookCurrentTimes.text = generateSeconds(statistic.currentReadingSeconds)
                        mBookTotalPages.text = statistic.totalReadPages.toString()
                        mBookTotalTime.text = generateSeconds(statistic.totalReadSeconds)
                        mBookReadingAverage.text = getString(R.string.statistics_average, round(statistic.totalReadSeconds.toFloat() / statistic.totalReadPages / 60).toInt())
                    }
                }
            }
        } finally {
            mProgress.visibility = View.GONE
        }
    }

    private fun generateSeconds(seconds: Long): String {
        val day = TimeUnit.SECONDS.toDays(seconds)
        val hours: Long = TimeUnit.SECONDS.toHours(seconds) - (day * 24)
        val minute: Long = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds) * 60)
        val second: Long = TimeUnit.SECONDS.toSeconds(seconds) - (TimeUnit.SECONDS.toMinutes(seconds) * 60)

        var description = ""

        if (day > 0)
            description += getString(R.string.statistics_format_days, day)

        if (hours > 0)
            description += getString(R.string.statistics_format_hours, hours)

        if (minute > 0)
            description += getString(R.string.statistics_format_minutes, minute)

        if (second > 0)
            description += getString(R.string.statistics_format_seconds, second)

        return description.trim()
    }

}