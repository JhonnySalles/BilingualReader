package br.com.fenix.bilingualreader.view.ui.statistics

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Statistics
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.StatisticsRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.LibraryUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import br.com.fenix.bilingualreader.view.components.MonthAxisValueFormatter
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.math.round
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


class StatisticsFragment : Fragment() {

    companion object {
        private val decimal = DecimalFormat("0")
    }

    private val mLOGGER = LoggerFactory.getLogger(StatisticsFragment::class.java)

    private lateinit var mRepository: StatisticsRepository

    private var mRoot: FrameLayout by autoCleared()
    private var mContent: ConstraintLayout by autoCleared()
    private var mProgress: BlurView by autoCleared()
    private lateinit var mDefaultAllLibraries: String

    // --------------------------------------------------------- Manga / Comic ---------------------------------------------------------
    private var mMangaReading: TextView by autoCleared()
    private var mMangaToRead: TextView by autoCleared()
    private var mMangaLibrary: TextView by autoCleared()
    private var mMangaRead: TextView by autoCleared()

    private var mMangaCompletePages: TextView by autoCleared()
    private var mMangaCompleteTimes: TextView by autoCleared()

    private var mMangaCurrentPages: TextView by autoCleared()
    private var mMangaCurrentTimes: TextView by autoCleared()

    private var mMangaTotalPages: TextView by autoCleared()
    private var mMangaTotalTime: TextView by autoCleared()
    private var mMangaReadingAverage: TextView by autoCleared()

    private var mMangaYear: TextInputLayout by autoCleared()
    private var mMangaYearAutoComplete: MaterialAutoCompleteTextView by autoCleared()
    private var mMangaChartLibrary: TextInputLayout by autoCleared()
    private var mMangaChartLibraryAutoComplete: MaterialAutoCompleteTextView by autoCleared()
    private var mMangaChart: LineChart by autoCleared()

    private var mMangaSelectYear = 0
    private lateinit var mMangaSelectLibrary:Library

    // --------------------------------------------------------- Book ---------------------------------------------------------

    private var mBookReading: TextView by autoCleared()
    private var mBookToRead: TextView by autoCleared()
    private var mBookLibrary: TextView by autoCleared()
    private var mBookRead: TextView by autoCleared()

    private var mBookCompletePages: TextView by autoCleared()
    private var mBookCompleteTimes: TextView by autoCleared()

    private var mBookCurrentPages: TextView by autoCleared()
    private var mBookCurrentTimes: TextView by autoCleared()

    private var mBookTotalPages: TextView by autoCleared()
    private var mBookTotalTime: TextView by autoCleared()
    private var mBookReadingAverage: TextView by autoCleared()

    private var mBookYear: TextInputLayout by autoCleared()
    private var mBookYearAutoComplete: MaterialAutoCompleteTextView by autoCleared()
    private var mBookChartLibrary: TextInputLayout by autoCleared()
    private var mBookChartLibraryAutoComplete: MaterialAutoCompleteTextView by autoCleared()
    private var mBookChart: LineChart by autoCleared()

    private var mLoading = MutableLiveData(false)
    private val mHandler = Handler(Looper.getMainLooper())

    private var mBookSelectYear = 0
    private lateinit var mBookSelectLibrary: Library

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mRoot = view.findViewById(R.id.frame_statistics_root)
        mContent = view.findViewById(R.id.frame_statistics_content)
        mProgress = view.findViewById(R.id.statistics_progress)

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
        mBookChartLibrary = view.findViewById(R.id.statistics_book_chart_library)
        mBookChartLibraryAutoComplete = view.findViewById(R.id.statistics_book_chart_library_auto_complete)
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
        mMangaChartLibrary = view.findViewById(R.id.statistics_manga_chart_library)
        mMangaChartLibraryAutoComplete = view.findViewById(R.id.statistics_manga_chart_library_auto_complete)
        mMangaChart = view.findViewById(R.id.statistics_manga_chart)

        view.findViewById<TextView>(R.id.statistics_manga_chart_title).text = getString(R.string.statistics_read_by_month, getString(R.string.statistics_sector_manga))
        view.findViewById<TextView>(R.id.statistics_book_chart_title).text = getString(R.string.statistics_read_by_month, getString(R.string.statistics_sector_book))

        mRepository = StatisticsRepository(requireContext())
        mDefaultAllLibraries = requireContext().getString(R.string.statistics_chart_library_all)

        val background = android.graphics.drawable.ColorDrawable(requireContext().getColorFromAttr(R.attr.background))

        val blurAlgorithm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RenderEffectBlur()
        } else {
            RenderScriptBlur(requireContext())
        }
        val decorView = requireActivity().window.decorView
        mProgress.setupWith(decorView.findViewById(android.R.id.content), blurAlgorithm)
            .setFrameClearDrawable(background)
            .setBlurRadius(10F)
        mLoading.value = true

        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val scrollView = mContent.getChildAt(0)
            scrollView?.setPadding(scrollView.paddingLeft, scrollView.paddingTop, scrollView.paddingRight, navBarHeight)
            insets
        }

        mLoading.observe(viewLifecycleOwner) {
            mProgress.visibility = if (it) View.VISIBLE else View.GONE
            mProgress.setBlurAutoUpdate(it)
        }

        view.findViewById<View>(R.id.statistics_manga_reading_card).setOnClickListener { openHistory(Type.MANGA, null) }
        view.findViewById<View>(R.id.statistics_manga_btn_history).setOnClickListener { openHistory(Type.MANGA, mMangaSelectYear) }

        view.findViewById<View>(R.id.statistics_book_reading_card).setOnClickListener { openHistory(Type.BOOK, null) }
        view.findViewById<View>(R.id.statistics_book_btn_history).setOnClickListener { openHistory(Type.BOOK, mBookSelectYear) }

        val statisticsScrollView = view.findViewById<android.widget.ScrollView>(R.id.statistics_scroll_view)
        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
        statisticsScrollView?.setOnScrollChangeListener { _, _, _, _, _ ->
            val isGlass = sharedPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
            if (isGlass) {
                (activity as? br.com.fenix.bilingualreader.MainActivity)?.setBlurAutoUpdate(true)
                mHandler.removeCallbacksAndMessages(null)
                mHandler.postDelayed({
                    (activity as? br.com.fenix.bilingualreader.MainActivity)?.setBlurAutoUpdate(false)
                }, 150)
            }
        }

        loadStatistics()
    }

    override fun onResume() {
        super.onResume()
        mProgress.setBlurAutoUpdate(mLoading.value == true)
    }

    override fun onPause() {
        super.onPause()
        mProgress.setBlurAutoUpdate(false)
    }

    override fun onDestroy() {
        mHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun loadStatistics() {
        mLoading.value = true
        lifecycleScope.launch {
            try {
                val statistics = withContext(Dispatchers.IO) { mRepository.statistics() }

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

                val libraries = withContext(Dispatchers.IO) { mRepository.getLibraryList() }

                val defaultBook = LibraryUtil.getDefault(requireContext(), Type.BOOK)
                val librariesBook = mutableMapOf(Pair(mDefaultAllLibraries, Library(null, mDefaultAllLibraries)), Pair(defaultBook.title, defaultBook))
                librariesBook.putAll(libraries.filter { it.type == Type.BOOK }.associateBy { it.title })

                if (!::mBookSelectLibrary.isInitialized || !librariesBook.contains(mBookSelectLibrary.title)) {
                    mBookSelectLibrary = Library(null, mDefaultAllLibraries)
                }

                val adapterBookLibrary = ArrayAdapter(requireContext(), R.layout.list_item, librariesBook.keys.toTypedArray())
                mBookChartLibraryAutoComplete.setAdapter(adapterBookLibrary)
                mBookChartLibraryAutoComplete.setText(mBookSelectLibrary.title, false)
                mBookChartLibraryAutoComplete.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
                    val selected = parent.getItemAtPosition(position).toString()
                    if (librariesBook.contains(selected)) {
                        mBookSelectLibrary = librariesBook[selected]!!
                        mLoading.value = true
                        lifecycleScope.launch {
                            try {
                                val id = if (mDefaultAllLibraries == mBookSelectLibrary.title) null else mBookSelectLibrary.id
                                val stats = withContext(Dispatchers.IO) {
                                    mRepository.statistics(Type.BOOK, mBookSelectYear, id)
                                }
                                setChartData(mBookChart, getData(stats, mBookSelectYear))
                            } catch (e: Exception) {
                                mLOGGER.error("Error loading book library statistics: " + e.message, e)
                            } finally {
                                mLoading.value = false
                            }
                        }
                    }
                }

                val defaultManga = LibraryUtil.getDefault(requireContext(), Type.MANGA)
                val librariesManga = mutableMapOf(Pair(mDefaultAllLibraries, Library(null, mDefaultAllLibraries)), Pair(defaultManga.title, defaultManga))
                librariesManga.putAll(libraries.filter { it.type == Type.MANGA }.associateBy { it.title })

                if (!::mMangaSelectLibrary.isInitialized || !librariesManga.contains(mMangaSelectLibrary.title)) {
                    mMangaSelectLibrary = Library(null, mDefaultAllLibraries)
                }

                val adapterMangaLibrary = ArrayAdapter(requireContext(), R.layout.list_item, librariesManga.keys.toTypedArray())
                mMangaChartLibraryAutoComplete.setAdapter(adapterMangaLibrary)
                mMangaChartLibraryAutoComplete.setText(mMangaSelectLibrary.title, false)
                mMangaChartLibraryAutoComplete.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
                    val selected = parent.getItemAtPosition(position).toString()
                    if (librariesManga.contains(selected)) {
                        mMangaSelectLibrary = librariesManga[selected]!!
                        mLoading.value = true
                        lifecycleScope.launch {
                            try {
                                val id = if (mDefaultAllLibraries == mMangaSelectLibrary.title) null else mMangaSelectLibrary.id
                                val stats = withContext(Dispatchers.IO) {
                                    mRepository.statistics(Type.MANGA, mMangaSelectYear, id)
                                }
                                setChartData(mMangaChart, getData(stats, mMangaSelectYear))
                            } catch (e: Exception) {
                                mLOGGER.error("Error loading manga library statistics: " + e.message, e)
                            } finally {
                                mLoading.value = false
                            }
                        }
                    }
                }

                val yearsManga = withContext(Dispatchers.IO) {
                    val list = mutableListOf<Int>()
                    list.addAll(mRepository.listYears(Type.MANGA))
                    if (list.isEmpty()) {
                        list.add(LocalDateTime.now().year)
                    }
                    list
                }
                val defaultMangaYear = yearsManga.last()
                if (mMangaSelectYear == 0 || !yearsManga.contains(mMangaSelectYear)) {
                    mMangaSelectYear = defaultMangaYear
                }

                mMangaYearAutoComplete.setAdapter(ArrayAdapter(requireContext(), R.layout.list_item, yearsManga.sortedDescending().toTypedArray()))
                mMangaYearAutoComplete.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
                    mLoading.value = true
                    lifecycleScope.launch {
                        try {
                            val selected = parent.getItemAtPosition(position).toString().toInt()
                            mMangaSelectYear = selected
                            val id = if (mDefaultAllLibraries == mMangaSelectLibrary.title) null else mMangaSelectLibrary.id
                            val stats = withContext(Dispatchers.IO) {
                                mRepository.statistics(Type.MANGA, selected, id)
                            }
                            setChartData(mMangaChart, getData(stats, selected))
                        } catch (e: Exception) {
                            mLOGGER.error("Error loading manga year statistics: " + e.message, e)
                        } finally {
                            mLoading.value = false
                        }
                    }
                }
                mMangaYearAutoComplete.setText(mMangaSelectYear.toString(), false)

                val yearsBook = withContext(Dispatchers.IO) {
                    val list = mutableListOf<Int>()
                    list.addAll(mRepository.listYears(Type.BOOK))
                    if (list.isEmpty()) {
                        list.add(LocalDateTime.now().year)
                    }
                    list
                }
                val defaultBookYear = yearsBook.last()
                if (mBookSelectYear == 0 || !yearsBook.contains(mBookSelectYear)) {
                    mBookSelectYear = defaultBookYear
                }

                mBookYearAutoComplete.setAdapter(ArrayAdapter(requireContext(), R.layout.list_item, yearsBook.sortedDescending().toTypedArray()))
                mBookYearAutoComplete.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
                    mLoading.value = true
                    lifecycleScope.launch {
                        try {
                            val selected = parent.getItemAtPosition(position).toString().toInt()
                            mBookSelectYear = selected
                            val id = if (mDefaultAllLibraries == mBookSelectLibrary.title) null else mBookSelectLibrary.id
                            val stats = withContext(Dispatchers.IO) {
                                mRepository.statistics(Type.BOOK, selected, id)
                            }
                            setChartData(mBookChart, getData(stats, selected))
                        } catch (e: Exception) {
                            mLOGGER.error("Error loading book year statistics: " + e.message, e)
                        } finally {
                            mLoading.value = false
                        }
                    }
                }
                mBookYearAutoComplete.setText(mBookSelectYear.toString(), false)

                val bookLibraryId = if (mDefaultAllLibraries == mBookSelectLibrary.title) null else mBookSelectLibrary.id
                val finalBookStats = withContext(Dispatchers.IO) {
                    mRepository.statistics(Type.BOOK, mBookSelectYear, bookLibraryId)
                }
                setChartData(mBookChart, getData(finalBookStats, mBookSelectYear))

                val mangaLibraryId = if (mDefaultAllLibraries == mMangaSelectLibrary.title) null else mMangaSelectLibrary.id
                val finalMangaStats = withContext(Dispatchers.IO) {
                    mRepository.statistics(Type.MANGA, mMangaSelectYear, mangaLibraryId)
                }
                setChartData(mMangaChart, getData(finalMangaStats, mMangaSelectYear))

            } catch (e: Exception) {
                mLOGGER.error("Error loading statistics: " + e.message, e)
            } finally {
                mLoading.value = false
            }
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
        chart.xAxis.textColor = requireContext().getColorFromAttr(R.attr.colorOnBackground)
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
                values.add(Entry(i.toFloat(), stats.readByMonth.toFloat()))
            else
                values.add(Entry(i.toFloat(), 0F))
        }

        val lineColor = requireContext().getColorFromAttr(R.attr.colorOutline)
        val textColor = requireContext().getColorFromAttr(R.attr.colorOnBackground)

        val lineData = LineDataSet(values, "")

        lineData.setDrawCircles(false)
        lineData.setDrawHorizontalHighlightIndicator(false)
        lineData.setDrawVerticalHighlightIndicator(false)

        lineData.setDrawFilled(true)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val gd = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(lineColor, Color.TRANSPARENT, Color.TRANSPARENT))
            gd.cornerRadius = 0f
            lineData.fillDrawable = gd
        } else
            lineData.fillDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.statistics_chart_gradient)

        lineData.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        lineData.color = lineColor
        lineData.highLightColor = lineColor
        lineData.valueTextColor = textColor
        lineData.valueTextSize = requireContext().resources.getDimension(R.dimen.statistics_chart_point_font_size)
        lineData.valueTypeface = ResourcesCompat.getFont(requireContext(), R.font.comic_sans)

        lineData.lineWidth = 1.75f
        lineData.circleRadius = 5f
        lineData.circleHoleRadius = 2.5f
        lineData.setCircleColor(Color.TRANSPARENT)

        decimal.roundingMode = RoundingMode.UP
        val data = LineData(lineData)
        data.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = decimal.format(value)
        })

        return data
    }

    private fun openHistory(type: Type, year: Int?) {
        val intent = Intent(requireContext(), MenuActivity::class.java)
        val bundle = Bundle()
        bundle.putInt(GeneralConsts.KEYS.FRAGMENT.ID, R.id.frame_history_statistics)
        bundle.putInt(GeneralConsts.KEYS.OBJECT.TYPE, type.ordinal)
        if (year != null)
            bundle.putInt(GeneralConsts.KEYS.OBJECT.STATISTICS_YEAR, year)

        intent.putExtras(bundle)
        requireActivity().overridePendingTransition(R.anim.fade_in_fragment_add_enter, R.anim.fade_out_fragment_remove_exit)
        startActivity(intent)
    }

}

private class AutoClearedValue<T : Any>(val fragment: Fragment) : ReadWriteProperty<Fragment, T> {
    private var _value: T? = null

    init {
        fragment.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_CREATE) {
                    fragment.viewLifecycleOwnerLiveData.observe(fragment) { viewLifecycleOwner ->
                        viewLifecycleOwner?.lifecycle?.addObserver(object : LifecycleEventObserver {
                            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                                if (event == Lifecycle.Event.ON_DESTROY) {
                                    _value = null
                                }
                            }
                        })
                    }
                }
            }
        })
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        return _value ?: throw IllegalStateException(
            "should never call to retrieve value after onDestroyView"
        )
    }

    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T) {
        _value = value
    }
}

private fun <T : Any> Fragment.autoCleared() = AutoClearedValue<T>(this)