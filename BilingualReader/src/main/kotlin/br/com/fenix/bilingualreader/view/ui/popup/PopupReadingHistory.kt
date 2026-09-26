package br.com.fenix.bilingualreader.view.ui.popup

import android.animation.ObjectAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.History
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.functions.ReadingTimeCalculator
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.HistoryRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.view.adapter.ReadingHistoryAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import br.com.fenix.bilingualreader.model.interfaces.History as Obj

class PopupReadingHistory(private val context: Context) {

    private lateinit var mPopup: AlertDialog
    private val mHistoryRepository = HistoryRepository(context)
    private val mMangaRepository = MangaRepository(context)
    private val mBookRepository = BookRepository(context)
    private val mCalculator = ReadingTimeCalculator(context)
    private lateinit var mAdapter: ReadingHistoryAdapter
    private val mItems = mutableListOf<History>()

    fun show(obj: Obj, onConfirm: () -> Unit = {}) {
        val view = LayoutInflater.from(context).inflate(R.layout.popup_reading_history, null)

        val recycler = view.findViewById<RecyclerView>(R.id.popup_history_recycler)
        recycler.layoutManager = LinearLayoutManager(context)

        mAdapter = ReadingHistoryAdapter(context, mItems) { position, item, icoView ->
            val animator = ObjectAnimator.ofFloat(icoView, View.ROTATION, 0f, 360f)
            animator.duration = 400
            animator.interpolator = LinearInterpolator()
            animator.start()

            val pagesRead = item.getPageEnd() - item.pageStart
            val newTime = if (obj.type == Type.MANGA) {
                mCalculator.calculateMangaReadingTime(pagesRead)
            } else {
                val avgWordsPerPage = 250L // Default fallback
                mCalculator.calculateBookReadingTime(pagesRead * avgWordsPerPage)
            }
            item.setSecondsRead(newTime)
            item.secondsReadAutomatic = true
            mAdapter.notifyItemChanged(position)
        }
        recycler.adapter = mAdapter

        mPopup = MaterialAlertDialogBuilder(context, R.style.AppCompatMaterialAlertDialog)
            .setView(view)
            .setPositiveButton(R.string.action_confirm) { _, _ ->
                saveHistory(obj)
                onConfirm()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()

        loadHistory(obj)
    }

    private fun loadHistory(obj: Obj) {
        CoroutineScope(Dispatchers.IO).launch {
            val historyList = mHistoryRepository.find(obj.type, obj.fkLibrary ?: 0L, obj.id ?: 0L)
            withContext(Dispatchers.Main) {
                mItems.clear()
                mItems.addAll(historyList)
                mAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun saveHistory(obj: Obj) {
        CoroutineScope(Dispatchers.IO).launch {
            mItems.forEach { item ->
                mHistoryRepository.update(item)
            }
            
            if (obj.type == Type.MANGA) {
                mMangaRepository.updateLastAlteration(obj.id ?: 0L)
            } else {
                mBookRepository.updateLastAlteration(obj.id ?: 0L)
            }
        }
    }
}
