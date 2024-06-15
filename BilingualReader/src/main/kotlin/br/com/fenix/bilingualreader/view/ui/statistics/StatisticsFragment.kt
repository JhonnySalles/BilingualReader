package br.com.fenix.bilingualreader.view.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Statistics
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.StatisticsRepository
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import br.com.fenix.bilingualreader.view.components.MonthAxisValueFormatter
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
import org.slf4j.LoggerFactory
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.math.round


class StatisticsFragment : Fragment() {

    companion object {
        private val decimal = DecimalFormat("0")
    }

    private val mLOGGER = LoggerFactory.getLogger(StatisticsFragment::class.java)

    private lateinit var mRepository: StatisticsRepository

    private lateinit var mRoot: FrameLayout
    private lateinit var mContent: ConstraintLayout
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

    private lateinit var mMangaYear: TextInputLayout
    private lateinit var mMangaYearAutoComplete: MaterialAutoCompleteTextView
    private lateinit var mMangaChart: LineChart

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

    private lateinit var mBookYear: TextInputLayout
    private lateinit var mBookYearAutoComplete: MaterialAutoCompleteTextView
    private lateinit var mBookChart: LineChart

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mRoot = view.findViewById(R.id.frame_statistics_root)
        mContent = view.findViewById(R.id.frame_statistics_content)
        mProgress = view.findViewById(R.id.fragment_statistics_progress)

        mBookReading = view.findViewById(R.id.statistics_book_reading)
        mBookToRead = view.findViewById(R.id.statistics_book_to_read)
        mBookLibrary = view.findViewById(R.id.statistics_book_library)
        mBookRead = view.findViewById(R.id.statistics_book_read)
        mBookCompletePages = view.findViewById(R.id.statistics_book_completed_pages)
        mBookCompleteTimes = view.findViewById(R.id.statistics_book_completed_time)
        mBookCurrentPages = view.findViewById(R.id.statistics_book_current_pages)
        mBookCurrentTimes = view.findViewById(R.id.statistics_book_current_time)
        mBookTotalPages = view.findViewById(R.id.statistics_book_total_read_pages)
        mBookTotalTime = view.findViewById(R.id.statistics_book_total_read_times)
        mBookReadingAverage = view.findViewById(R.id.statistics_book_total_read_average)
        mBookYear = view.findViewById(R.id.statistics_book_chart_year)
        mBookYearAutoComplete = view.findViewById(R.id.statistics_book_chart_year_auto_complete)
        mBookChart = view.findViewById(R.id.statistics_book_chart)

        mMangaReading = view.findViewById(R.id.statistics_manga_reading)
        mMangaToRead = view.findViewById(R.id.statistics_manga_to_read)
        mMangaLibrary = view.findViewById(R.id.statistics_manga_library)
        mMangaRead = view.findViewById(R.id.statistics_manga_read)
        mMangaCompletePages = view.findViewById(R.id.statistics_manga_completed_pages)
        mMangaCompleteTimes = view.findViewById(R.id.statistics_manga_completed_time)
        mMangaCurrentPages = view.findViewById(R.id.statistics_manga_current_pages)
        mMangaCurrentTimes = view.findViewById(R.id.statistics_manga_current_time)
        mMangaTotalPages = view.findViewById(R.id.statistics_manga_total_read_pages)
        mMangaTotalTime = view.findViewById(R.id.statistics_manga_total_read_times)
        mMangaReadingAverage = view.findViewById(R.id.statistics_manga_total_read_average)
        mMangaYear = view.findViewById(R.id.statistics_manga_chart_year)
        mMangaYearAutoComplete = view.findViewById(R.id.statistics_manga_chart_year_auto_complete)
        mMangaChart = view.findViewById(R.id.statistics_manga_chart)

        view.findViewById<TextView>(R.id.statistics_manga_chart_title).text = getString(R.string.statistics_read_by_month, getString(R.string.statistics_sector_manga))
        view.findViewById<TextView>(R.id.statistics_book_chart_title).text = getString(R.string.statistics_read_by_month, getString(R.string.statistics_sector_book))

        mRepository = StatisticsRepository(requireContext())

        val background = requireActivity().window.decorView.background

        mProgress.setupWith(mRoot, RenderScriptBlur(requireContext()))
            .setFrameClearDrawable(background)
            .setBlurRadius(10F)

        loadStatistics()
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
                        mMangaReadingAverage.text = getString(
                            R.string.statistics_average,
                            round(statistic.totalReadSeconds.toFloat() / statistic.totalReadPages / 60).toInt()
                        )
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
                        mBookReadingAverage.text = getString(
                            R.string.statistics_average,
                            round(statistic.totalReadSeconds.toFloat() / statistic.totalReadPages / 60).toInt()
                        )
                    }
                }
            }

            setupChart(mBookChart)
            setupChart(mMangaChart)

            val years = mRepository.listYears()

            if (years.isEmpty())
                years.add(LocalDateTime.now().year)

            val adapter = ArrayAdapter(requireContext(), R.layout.list_item, years.toTypedArray())
            mMangaYearAutoComplete.setAdapter(adapter)
            mMangaYearAutoComplete.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
                val selected = parent.getItemAtPosition(position).toString().toInt()
                setChartData(mMangaChart, getData(mRepository.statistics(Type.MANGA, selected), selected))
            }

            mBookYearAutoComplete.setAdapter(adapter)
            mBookYearAutoComplete.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
                val selected = parent.getItemAtPosition(position).toString().toInt()
                setChartData(mBookChart, getData(mRepository.statistics(Type.BOOK, selected), selected))
            }

            val year = years.last()
            mMangaYearAutoComplete.setText(year.toString(), false)
            mBookYearAutoComplete.setText(year.toString(), false)

            setChartData(mBookChart, getData(mRepository.statistics(Type.BOOK, year), year))
            setChartData(mMangaChart, getData(mRepository.statistics(Type.MANGA, year), year))
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
            description += getString(R.string.statistics_format_days, day) + " "

        if (hours > 0)
            description += getString(R.string.statistics_format_hours, hours) + " "

        if (minute > 0)
            description += getString(R.string.statistics_format_minutes, minute) + " "

        if (second > 0)
            description += getString(R.string.statistics_format_seconds, second) + " "

        return description.trim()
    }

    private fun setChartData(chart: LineChart, data: LineData) {
        chart.data = data
        chart.invalidate()
    }

    private fun setupChart(chart: LineChart) {
        chart.setTouchEnabled(true)

        chart.isDragEnabled = true
        chart.setScaleEnabled(true)

        chart.setPinchZoom(true)

        chart.legend.isEnabled = false

        chart.axisLeft.isEnabled = false
        chart.axisLeft.spaceTop = 20f
        chart.axisLeft.spaceBottom = 20f
        chart.axisRight.isEnabled = false

        chart.xAxis.isEnabled = true
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.setGranularity(1f)
        chart.xAxis.setLabelCount(12)
        chart.xAxis.textColor = requireContext().getColorFromAttr(R.attr.colorPrimary)
        chart.xAxis.textSize = requireContext().resources.getDimension(R.dimen.statistics_chart_label_font_size)
        chart.xAxis.valueFormatter = MonthAxisValueFormatter(requireContext())

        chart.animateX(2000)
        chart.description.isEnabled = false
    }

    private fun getData(list: List<Statistics>, year: Int): LineData {
        val values = ArrayList<Entry>()

        val max = if (year == LocalDateTime.now().year) LocalDateTime.now().month.value else 12
        for (i in 1 until (max + 1)) {
            val stats = list.find { it.dateTime?.month?.value?.toFloat() == i.toFloat() }
            if (stats != null)
                values.add(Entry(i.toFloat(), stats.read.toFloat()))
            else
                values.add(Entry(i.toFloat(), 0F))
        }

        val lineColor = requireContext().getColorFromAttr(R.attr.colorOutline)
        val textColor = requireContext().getColorFromAttr(R.attr.colorPrimary)

        val linedata = LineDataSet(values, "")

        linedata.setDrawCircles(false)
        linedata.setDrawHorizontalHighlightIndicator(false)
        linedata.setDrawVerticalHighlightIndicator(false)

        linedata.setDrawFilled(true)
        linedata.fillDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.statistics_chart_gradient)
        linedata.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        linedata.color = lineColor
        linedata.highLightColor = lineColor
        linedata.valueTextColor = textColor
        linedata.valueTextSize = requireContext().resources.getDimension(R.dimen.statistics_chart_point_font_size)
        linedata.valueTypeface = ResourcesCompat.getFont(requireContext(), R.font.comic_sans)

        linedata.lineWidth = 1.75f
        linedata.circleRadius = 5f
        linedata.circleHoleRadius = 2.5f
        linedata.setCircleColor(Color.TRANSPARENT)

        decimal.roundingMode = RoundingMode.UP
        val data = LineData(linedata)
        data.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = decimal.format(value)
        })

        return data
    }

}