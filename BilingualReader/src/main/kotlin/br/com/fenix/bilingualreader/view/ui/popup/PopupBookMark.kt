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
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.listener.BookParseListener
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.HistoryRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


class PopupBookMark(var context: Context, var manager: FragmentManager) {

    private val mPreferences = GeneralConsts.getSharedPreferences(context)
    private lateinit var mPopup: AlertDialog

    fun getPopupBookMark(book: Book, onUpdate: (Int) -> (Unit), onClose: (Boolean, Int, LocalDateTime) -> (Unit)) {
        if (book.pages <= 1) {
            val fontSize = mPreferences.getFloat(GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE, GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE_DEFAULT).toInt()
            var document : DocumentParse? = null
            val listener: BookParseListener = object : BookParseListener {
                override fun onLoading(isFinished: Boolean, isLoaded: Boolean) {
                    if (isFinished && isLoaded) {
                        book.pages = document!!.getPageCount(fontSize)
                        BookRepository(context).update(book)
                        mMax = book.pages

                        if (book.bookMark <= 0) {
                            mNewBookMark = book.pages
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
            document = DocumentParse(book.path, book.password, fontSize, context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE, listener)
        }

        mMax = book.pages
        mNewBookMark = book.bookMark
        mNewDate = book.lastAccess ?: LocalDateTime.now()

        if (book.bookMark <= 0)
            mNewBookMark = book.pages

        mPopup = MaterialAlertDialogBuilder(context, R.style.AppCompatMaterialAlertDialog)
            .setView(createPopup(context, LayoutInflater.from(context)))
            .setCancelable(true)
            .setNeutralButton(R.string.popup_book_mark_read) { _, _ -> mBookMarkPageEdit.setText(book.pages.toString()) }
            .setNegativeButton(R.string.action_cancel) { _, _ -> onClose(false, book.bookMark, book.lastAccess ?: LocalDateTime.now()) }
            .setPositiveButton(R.string.action_confirm, null)
            .create()

        mPopup.show()
        mPopup.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            if (validate()) {
                HistoryRepository(context).save(History(null, book.fkLibrary!!, book.id!!, Type.BOOK, book.bookMark, mNewBookMark, book.pages, book.volume, 0, mNewDate, mNewDate, 0, 0, useTTS = false, isNotify = false))
                onClose(true, mNewBookMark, mNewDate)
                mPopup.dismiss()
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

    private fun selectDate(lastDate: LocalDateTime) {
        val date = lastDate
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val constraints = CalendarConstraints.Builder()
            .setStart(date)
            .build()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTheme(R.style.AppCompatMaterialDatePicker)
            .setTitleText(R.string.popup_book_select_date)
            .setCalendarConstraints(constraints)
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener {
            datePicker.selection?.let { selection ->
                val date = Instant.ofEpochMilli(selection)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                mNewDate = date.atTime(lastDate.hour, lastDate.minute)
                mBookMarkDateEdit.setText(GeneralConsts.formatterDate(context, mNewDate))
            }
        }

        datePicker.show(manager, "")
    }

    private fun selectTime(lastTime: LocalDateTime) {
        val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setTheme(R.style.AppCompatMaterialTimePicker)
                .setHour(lastTime.hour)
                .setMinute(lastTime.minute)
                .setTitleText(R.string.popup_book_select_time)
                .build()

        timePicker.addOnPositiveButtonClickListener {
            val time = LocalTime.of(timePicker.hour, timePicker.minute)
            mNewDate = LocalDateTime.of(lastTime.toLocalDate(), time)
            mBookMarkTimeEdit.setText(mNewDate.format(DateTimeFormatter.ofPattern(GeneralConsts.PATTERNS.TIME_PATTERN)))
        }
        timePicker.show(manager, "")
    }

}