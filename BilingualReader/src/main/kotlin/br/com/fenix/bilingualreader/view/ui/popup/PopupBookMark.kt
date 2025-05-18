package br.com.fenix.bilingualreader.view.ui.popup

import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.History
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.service.listener.BookParseListener
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.parses.manga.ParseFactory
import br.com.fenix.bilingualreader.service.parses.manga.RarParse
import br.com.fenix.bilingualreader.service.repository.HistoryRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Util
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
    private lateinit var mPopup: AlertDialog

    fun getPopupBookMark(obj: Obj, onUpdate: (Obj) -> (Unit), onClose: (Boolean, Obj) -> (Unit)) {
        processPages(obj, onUpdate)

        mMax = obj.pages
        mNewBookMark = obj.bookMark
        mNewDate = obj.lastAccess ?: LocalDateTime.now()

        if (mNewDate.isEqual(GeneralConsts.SHARE_MARKS.MIN_DATE_TIME))
            mNewDate = LocalDateTime.now()

        if (obj.bookMark <= 0)
            mNewBookMark = obj.pages

        mPopup = MaterialAlertDialogBuilder(context, R.style.AppCompatMaterialAlertDialog)
            .setView(createPopup(context, LayoutInflater.from(context)))
            .setCancelable(true)
            .setNeutralButton(R.string.popup_book_mark_read, null)
            .setNegativeButton(R.string.action_cancel) { _, _ -> onClose(false, obj) }
            .setPositiveButton(R.string.action_confirm, null)
            .create()

        mPopup.show()
        mPopup.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener { mBookMarkPageEdit.setText(obj.pages.toString()) }
        mPopup.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            if (validate()) {
                HistoryRepository(context).save(
                    History(
                        null, obj.fkLibrary!!, obj.id!!, obj.type, obj.bookMark, mNewBookMark, obj.pages, mNewBookMark == mMax,
                        obj.volume,0, mNewDate, mNewDate, 0, 0, useTTS = false, isNotify = false
                    )
                )
                obj.bookMark = mNewBookMark
                obj.lastAccess = mNewDate
                onClose(true, obj)
                mPopup.dismiss()
            }
        }
    }

    private fun processPages(obj: Obj, onUpdate: (Obj) -> (Unit)) {
        if (obj.pages <= 1) {
            when (obj) {
                is Book -> {
                    val fontSize = mPreferences.getFloat(GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE, GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE_DEFAULT).toInt()

                    var document: DocumentParse? = null
                    val listener : BookParseListener = object : BookParseListener {
                        override fun onLoading(isFinished: Boolean, isLoaded: Boolean) {
                            if (isFinished && isLoaded) {
                                obj.pages = document!!.getPageCount(fontSize)
                                onUpdate(obj)
                                mMax = obj.pages

                                if (obj.bookMark <= 0) {
                                    mNewBookMark = obj.pages
                                    mBookMarkPageEdit.setText(mNewBookMark.toString())
                                }
                            }
                        }

                        override fun onSearching(isSearching: Boolean) {
                            TODO("Not yet implemented")
                        }

                        override fun onConverting(isConverting: Boolean) {
                            TODO("Not yet implemented")
                        }
                    }

                    document = DocumentParse(obj.path, obj.password, fontSize, context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE, false, listener)
                }
                is Manga -> {
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
                    } finally {
                        Util.destroyParse(parse)
                    }
                }
            }
        }
    }

    private var mMax: Int = 0
    private var mNewBookMark: Int = 0
    private var mNewDate: LocalDateTime = LocalDateTime.now()
    private lateinit var mBookMarkPageEdit: TextInputEditText
    private lateinit var mBookMarkPage: TextInputLayout
    private lateinit var mBookMarkDateEdit: TextInputEditText
    private lateinit var mBookMarkDate: TextInputLayout
    private lateinit var mBookMarkTimeEdit: TextInputEditText
    private lateinit var mBookMarkTime: TextInputLayout

    private fun createPopup(context: Context, inflater: LayoutInflater): View? {
        val root = inflater.inflate(R.layout.popup_book_mark, null, false)

        mBookMarkPageEdit = root.findViewById(R.id.popup_book_mark_page_edit)
        mBookMarkPage = root.findViewById(R.id.popup_book_mark_page)
        mBookMarkDateEdit = root.findViewById(R.id.popup_book_mark_date_edit)
        mBookMarkDate = root.findViewById(R.id.popup_book_mark_date)
        mBookMarkTimeEdit = root.findViewById(R.id.popup_book_mark_time_edit)
        mBookMarkTime = root.findViewById(R.id.popup_book_mark_time)

        mBookMarkDateEdit.setOnClickListener { selectDate(mNewDate) }
        mBookMarkTimeEdit.setOnClickListener { selectTime(mNewDate) }

        mBookMarkDate.endIconMode = TextInputLayout.END_ICON_NONE
        mBookMarkTime.endIconMode = TextInputLayout.END_ICON_NONE

        mBookMarkPageEdit.setText(mNewBookMark.toString())
        mBookMarkDateEdit.setText(GeneralConsts.formatterDate(context, mNewDate))
        mBookMarkTimeEdit.setText(mNewDate.format(DateTimeFormatter.ofPattern(GeneralConsts.PATTERNS.TIME_PATTERN)))

        return root
    }

    private fun validate(): Boolean {
        var validated = true

        mBookMarkPage.isErrorEnabled = false
        mBookMarkPage.error = ""
        mBookMarkDate.isErrorEnabled = false
        mBookMarkDate.error = ""

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
            if (mNewBookMark > mMax) {
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
            mBookMarkDate.isErrorEnabled = true
            mBookMarkDate.error = context.getString(R.string.popup_book_mark_necessary_time)
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