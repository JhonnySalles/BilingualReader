package br.com.fenix.bilingualreader.view.ui.popup

import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.History
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.functions.ReadingTimeCalculator
import br.com.fenix.bilingualreader.service.listener.BookParseListener
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.parses.manga.ParseFactory
import br.com.fenix.bilingualreader.service.parses.manga.RarParse
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.HistoryRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Util
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import br.com.fenix.bilingualreader.model.interfaces.History as Obj


class PopupBookMark(var context: Context, var manager: FragmentManager) {

    private val mPreferences = GeneralConsts.getSharedPreferences(context)
    private val mCalculator = ReadingTimeCalculator(context)
    private lateinit var mPopup: AlertDialog

    private var mMax: Int = 0
    private var mNewBookMark: Int = 0
    private var mNewDate: LocalDateTime = LocalDateTime.now()
    private var mReadingDurationSeconds: Long = 0L
    private var mIsManualReadingTime: Boolean = false
    private var mWordCount: Long = 0L

    private lateinit var mBookMarkPageEdit: TextInputEditText
    private lateinit var mBookMarkPage: TextInputLayout
    private lateinit var mBookMarkDateEdit: TextInputEditText
    private lateinit var mBookMarkDate: TextInputLayout
    private lateinit var mBookMarkTimeEdit: TextInputEditText
    private lateinit var mBookMarkTime: TextInputLayout
    private lateinit var mReadingDurationEdit: TextInputEditText
    private lateinit var mReadingDuration: TextInputLayout
    private lateinit var mBtnAutoCalc: MaterialButton

    private var mCurrentObj: Obj? = null

    fun getPopupBookMark(obj: Obj, onUpdate: (Obj) -> (Unit), onClose: (Boolean, Obj) -> (Unit)) {
        mCurrentObj = obj
        mMax = obj.pages
        mNewBookMark = obj.bookMark
        mNewDate = obj.lastAccess ?: LocalDateTime.now()

        if (mNewDate.isEqual(GeneralConsts.SHARE_MARKS.MIN_DATE_TIME))
            mNewDate = LocalDateTime.now()

        if (obj.bookMark <= 0)
            mNewBookMark = obj.pages

        mReadingDurationSeconds = calculatePreviewTime(obj, mNewBookMark)

        mPopup = MaterialAlertDialogBuilder(context, R.style.AppCompatMaterialAlertDialog)
            .setView(createPopup(context, LayoutInflater.from(context)))
            .setCancelable(true)
            .setNeutralButton(R.string.popup_book_mark_read, null)
            .setNegativeButton(R.string.action_cancel) { _, _ -> onClose(false, obj) }
            .setPositiveButton(R.string.action_confirm, null)
            .create()

        mPopup.show()
        processPages(obj, onUpdate)

        mPopup.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener {
            mBookMarkPageEdit.setText(obj.pages.toString())
        }

        mPopup.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            if (validate()) {
                val pagesRead = maxOf(1, mNewBookMark - obj.bookMark)
                val averageTimeByPage = if (pagesRead > 0) mReadingDurationSeconds / pagesRead else mReadingDurationSeconds

                HistoryRepository(context).save(
                    History(
                        null, obj.fkLibrary!!, obj.id!!, obj.type, obj.bookMark, mNewBookMark, obj.pages, mNewBookMark == mMax,
                        obj.volume, 0, mNewDate, mNewDate, mReadingDurationSeconds, averageTimeByPage, useTTS = false, isNotify = false,
                        wordCount = mWordCount, secondsReadAutomatic = !mIsManualReadingTime
                    )
                )

                if (obj.type == Type.MANGA) {
                    MangaRepository(context).updateLastAlteration(obj.id!!, LocalDateTime.now())
                } else {
                    BookRepository(context).updateLastAlteration(obj.id!!, LocalDateTime.now())
                }

                obj.bookMark = mNewBookMark
                obj.lastAccess = mNewDate
                onClose(true, obj)
                mPopup.dismiss()
            }
        }
    }

    private fun calculatePreviewTime(obj: Obj, targetPage: Int): Long {
        val pagesRead = maxOf(1, targetPage - obj.bookMark)
        return if (obj.type == Type.MANGA) {
            mCalculator.calculateMangaReadingTime(pagesRead)
        } else {
            if (mWordCount > 0L) {
                mCalculator.calculateBookReadingTime(mWordCount)
            } else {
                val avgWordsPerPage = 250L
                mCalculator.calculateBookReadingTime(pagesRead * avgWordsPerPage)
            }
        }
    }

    private fun processPages(obj: Obj, onUpdate: (Obj) -> (Unit)) {
        when (obj) {
            is Book -> {
                val fontSize = mPreferences.getFloat(GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE, GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE_DEFAULT).toInt()

                var document: DocumentParse? = null
                val listener: BookParseListener = object : BookParseListener {
                    override fun onLoading(isFinished: Boolean, isLoaded: Boolean) {
                        if (isFinished && isLoaded) {
                            obj.pages = document!!.getPageCount(fontSize)
                            onUpdate(obj)
                            mMax = obj.pages

                            if (obj.bookMark <= 0) {
                                mNewBookMark = obj.pages
                                mBookMarkPageEdit.setText(mNewBookMark.toString())
                            }

                            val startPage = obj.bookMark
                            val endPage = if (mNewBookMark > 0) mNewBookMark else obj.pages
                            mWordCount = mCalculator.countWordsFromDocument(document!!, startPage, endPage)

                            if (!mIsManualReadingTime) {
                                mReadingDurationSeconds = mCalculator.calculateBookReadingTime(mWordCount)
                                mReadingDurationEdit.post {
                                    mReadingDurationEdit.setText(formatDuration(mReadingDurationSeconds))
                                }
                            }
                        }
                    }

                    override fun onSearching(isSearching: Boolean) {}
                    override fun onConverting(isConverting: Boolean) {}
                }

                document = DocumentParse(obj.path, obj.password, fontSize, context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE, false, listener)
            }
            is Manga -> {
                if (obj.pages <= 1) {
                    val parse = ParseFactory.create(obj.file) ?: return
                    try {
                        if (parse is RarParse) {
                            val folder = GeneralConsts.CACHE_FOLDER.RAR + '/' + Util.normalizeNameCache(obj.file.nameWithoutExtension)
                            val cacheDir = File(GeneralConsts.getCacheDir(context), folder)
                            (parse as RarParse?)!!.setCacheDirectory(cacheDir)
                        }

                        obj.pages = parse.numPages()
                        onUpdate(obj)

                        mMax = obj.pages
                        if (obj.bookMark <= 0) {
                            mNewBookMark = obj.pages
                            mBookMarkPageEdit.setText(mNewBookMark.toString())
                        }
                        if (!mIsManualReadingTime) {
                            mReadingDurationSeconds = calculatePreviewTime(obj, mNewBookMark)
                            mReadingDurationEdit.setText(formatDuration(mReadingDurationSeconds))
                        }
                    } finally {
                        Util.destroyParse(parse)
                    }
                }
            }
        }
    }

    private fun createPopup(context: Context, inflater: LayoutInflater): View? {
        val root = inflater.inflate(R.layout.popup_book_mark, null, false)

        mBookMarkPageEdit = root.findViewById(R.id.popup_book_mark_page_edit)
        mBookMarkPage = root.findViewById(R.id.popup_book_mark_page)
        mBookMarkDateEdit = root.findViewById(R.id.popup_book_mark_date_edit)
        mBookMarkDate = root.findViewById(R.id.popup_book_mark_date)
        mBookMarkTimeEdit = root.findViewById(R.id.popup_book_mark_time_edit)
        mBookMarkTime = root.findViewById(R.id.popup_book_mark_time)
        mReadingDurationEdit = root.findViewById(R.id.popup_book_mark_reading_duration_edit)
        mReadingDuration = root.findViewById(R.id.popup_book_mark_reading_duration)
        mBtnAutoCalc = root.findViewById(R.id.popup_book_mark_btn_auto_calc)

        mBookMarkDateEdit.setOnClickListener { selectDate(mNewDate) }
        mBookMarkTimeEdit.setOnClickListener { selectTime(mNewDate) }
        mReadingDurationEdit.setOnClickListener { selectDuration() }
        mReadingDurationEdit.setOnLongClickListener {
            pasteDurationFromClipboard()
        }
        mReadingDuration.setOnClickListener { selectDuration() }
        mReadingDuration.setOnLongClickListener {
            pasteDurationFromClipboard()
        }
        mBtnAutoCalc.setOnClickListener { autoCalculateDuration() }

        mBookMarkDate.endIconMode = TextInputLayout.END_ICON_NONE
        mBookMarkTime.endIconMode = TextInputLayout.END_ICON_NONE
        mReadingDuration.endIconMode = TextInputLayout.END_ICON_NONE

        mBookMarkPageEdit.setText(mNewBookMark.toString())
        mBookMarkDateEdit.setText(GeneralConsts.formatterDate(context, mNewDate))
        mBookMarkTimeEdit.setText(mNewDate.format(DateTimeFormatter.ofPattern(GeneralConsts.PATTERNS.TIME_PATTERN)))
        mReadingDurationEdit.setText(formatDuration(mReadingDurationSeconds))

        mBookMarkPageEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val pageStr = s?.toString()?.trim() ?: ""
                if (pageStr.isNotEmpty() && !pageStr.contains("\\D".toRegex())) {
                    val page = pageStr.toIntOrNull() ?: return
                    mNewBookMark = page
                    if (!mIsManualReadingTime && mCurrentObj != null) {
                        mReadingDurationSeconds = calculatePreviewTime(mCurrentObj!!, mNewBookMark)
                        mReadingDurationEdit.setText(formatDuration(mReadingDurationSeconds))
                    }
                }
            }
        })

        return root
    }

    private fun formatDuration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun parseDuration(input: String): Long? {
        val text = input.trim().replace(",", ".").substringBefore(".")
        val parts = text.split(":")
        return when (parts.size) {
            3 -> {
                val h = parts[0].trim().toLongOrNull() ?: return null
                val m = parts[1].trim().toLongOrNull() ?: return null
                val s = parts[2].trim().toLongOrNull() ?: return null
                (h * 3600) + (m * 60) + s
            }
            2 -> {
                val h = parts[0].trim().toLongOrNull() ?: return null
                val m = parts[1].trim().toLongOrNull() ?: return null
                (h * 3600) + (m * 60)
            }
            1 -> {
                parts[0].trim().toLongOrNull()
            }
            else -> null
        }
    }

    private fun pasteDurationFromClipboard(): Boolean {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        val clip = clipboard?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString()?.trim().orEmpty()
            val parsedSeconds = parseDuration(text)
            if (parsedSeconds != null) {
                mReadingDurationSeconds = parsedSeconds
                mIsManualReadingTime = true
                mReadingDurationEdit.setText(formatDuration(mReadingDurationSeconds))
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.action_copy, formatDuration(mReadingDurationSeconds)),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return true
            }
        }
        return false
    }

    private fun selectDuration() {
        val initialHours = (mReadingDurationSeconds / 3600).toInt().coerceIn(0, 99)
        val initialMinutes = ((mReadingDurationSeconds % 3600) / 60).toInt().coerceIn(0, 59)
        val initialSeconds = (mReadingDurationSeconds % 60).toInt().coerceIn(0, 59)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_duration_picker, null)
        val hoursPicker = view.findViewById<android.widget.NumberPicker>(R.id.duration_picker_hours)
        val minutesPicker = view.findViewById<android.widget.NumberPicker>(R.id.duration_picker_minutes)
        val secondsPicker = view.findViewById<android.widget.NumberPicker>(R.id.duration_picker_seconds)

        hoursPicker.minValue = 0
        hoursPicker.maxValue = 99
        hoursPicker.value = initialHours
        hoursPicker.setFormatter { String.format("%02d", it) }

        minutesPicker.minValue = 0
        minutesPicker.maxValue = 59
        minutesPicker.value = initialMinutes
        minutesPicker.setFormatter { String.format("%02d", it) }

        secondsPicker.minValue = 0
        secondsPicker.maxValue = 59
        secondsPicker.value = initialSeconds
        secondsPicker.setFormatter { String.format("%02d", it) }

        MaterialAlertDialogBuilder(context, R.style.AppCompatMaterialAlertDialog)
            .setTitle(R.string.popup_book_mark_reading_time)
            .setView(view)
            .setPositiveButton(R.string.action_confirm) { _, _ ->
                hoursPicker.clearFocus()
                minutesPicker.clearFocus()
                secondsPicker.clearFocus()
                mReadingDurationSeconds = (hoursPicker.value * 3600L) + (minutesPicker.value * 60L) + secondsPicker.value.toLong()
                mIsManualReadingTime = true
                mReadingDurationEdit.setText(formatDuration(mReadingDurationSeconds))
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun autoCalculateDuration() {
        mIsManualReadingTime = false
        if (mCurrentObj != null) {
            mReadingDurationSeconds = calculatePreviewTime(mCurrentObj!!, mNewBookMark)
            mReadingDurationEdit.setText(formatDuration(mReadingDurationSeconds))
        }
    }

    private fun validate(): Boolean {
        var validated = true

        mBookMarkPage.isErrorEnabled = false
        mBookMarkPage.error = ""
        mBookMarkDate.isErrorEnabled = false
        mBookMarkDate.error = ""
        mBookMarkTime.isErrorEnabled = false
        mBookMarkTime.error = ""

        if (mBookMarkPageEdit.text == null || mBookMarkPageEdit.text?.toString()?.isEmpty() == true) {
            validated = false
            mBookMarkPage.isErrorEnabled = true
            mBookMarkPage.error = context.getString(R.string.popup_book_mark_necessary_page)
        } else if (mBookMarkPageEdit.text.toString().contains("\\D".toRegex())) {
            validated = false
            mBookMarkPage.isErrorEnabled = true
            mBookMarkPage.error = context.getString(R.string.popup_book_mark_only_number)
        } else {
            mNewBookMark = Integer.valueOf(mBookMarkPageEdit.text.toString())
            if (mNewBookMark > mMax && mMax > 0) {
                validated = false
                mBookMarkPage.isErrorEnabled = true
                mBookMarkPage.error = context.getString(R.string.popup_book_mark_exceed_max, mMax)
            }
        }

        if (mBookMarkDateEdit.text == null || mBookMarkDateEdit.text?.toString()?.isEmpty() == true) {
            validated = false
            mBookMarkDate.isErrorEnabled = true
            mBookMarkDate.error = context.getString(R.string.popup_book_mark_necessary_date)
        }

        if (mBookMarkTimeEdit.text == null || mBookMarkTimeEdit.text?.toString()?.isEmpty() == true) {
            validated = false
            mBookMarkTime.isErrorEnabled = true
            mBookMarkTime.error = context.getString(R.string.popup_book_mark_necessary_time)
        }

        return validated
    }

    private fun formatDatePicker(date: LocalDateTime) = date.atZone(ZoneId.systemDefault()).toInstant().atZone(ZoneId.ofOffset("UTC", ZoneOffset.UTC)).toInstant().toEpochMilli()

    private fun formatDateTime(date: Long) = LocalDateTime.ofInstant(Instant.ofEpochMilli(date), ZoneId.ofOffset("UTC", ZoneOffset.UTC)).atZone(ZoneId.systemDefault()).toLocalDate()

    private fun selectDate(lastDate: LocalDateTime) {
        val initial = LocalDateTime.of(2000, 1, 1, 0, 0)
            .truncatedTo(ChronoUnit.DAYS)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()

        val constraints = CalendarConstraints.Builder()
            .setStart(initial)
            .build()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTheme(R.style.AppCompatMaterialDatePicker)
            .setTitleText(R.string.popup_book_select_date)
            .setCalendarConstraints(constraints)
            .setSelection(formatDatePicker(lastDate))
            .build()

        datePicker.addOnPositiveButtonClickListener {
            datePicker.selection?.let { selection ->
                val date = formatDateTime(selection)
                mNewDate = date.atTime(lastDate.hour, lastDate.minute)
                mBookMarkDateEdit.setText(GeneralConsts.formatterDate(context, mNewDate))
            }
        }

        datePicker.show(manager, "")
    }

    private fun selectTime(lastTime: LocalDateTime) {
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(lastTime.hour)
            .setMinute(lastTime.minute)
            .setTitleText(R.string.popup_book_select_time)
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val time = LocalTime.of(timePicker.hour, timePicker.minute, 0, 0)
            mNewDate = LocalDateTime.of(lastTime.toLocalDate(), time)
            mBookMarkTimeEdit.setText(mNewDate.format(DateTimeFormatter.ofPattern(GeneralConsts.PATTERNS.TIME_PATTERN)))
        }
        timePicker.show(manager, "")
    }

}