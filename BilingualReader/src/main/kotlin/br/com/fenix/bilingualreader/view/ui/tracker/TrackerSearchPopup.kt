package br.com.fenix.bilingualreader.view.ui.tracker

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.service.listener.ApiListener
import br.com.fenix.bilingualreader.service.tracker.anilist.AniListTracker
import br.com.fenix.bilingualreader.service.tracker.mal.MyAnimeListTracker
import br.com.fenix.bilingualreader.service.tracker.model.TrackerSearchResult
import br.com.fenix.bilingualreader.service.tracker.model.TrackerServiceType
import br.com.fenix.bilingualreader.view.adapter.tracker.TrackerSearchAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object TrackerSearchPopup {

    fun show(
        context: Context,
        serviceType: TrackerServiceType,
        initialQuery: String = "",
        onItemSelected: (TrackerSearchResult) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.popup_tracker_search, null)
        val textTitle = view.findViewById<TextView>(R.id.popup_tracker_search_title)
        val inputSearch = view.findViewById<TextInputEditText>(R.id.popup_tracker_search_input)
        val progress = view.findViewById<ProgressBar>(R.id.popup_tracker_search_progress)
        val recycler = view.findViewById<RecyclerView>(R.id.popup_tracker_search_recycler_view)
        val textEmpty = view.findViewById<TextView>(R.id.popup_tracker_search_empty)

        textTitle.text = when (serviceType) {
            TrackerServiceType.MY_ANIME_LIST -> context.getString(R.string.tracker_search_mal_title)
            TrackerServiceType.ANILIST -> context.getString(R.string.tracker_search_anilist_title)
        }

        recycler.layoutManager = LinearLayoutManager(context)

        var dialog: AlertDialog? = null

        val adapter = TrackerSearchAdapter { selectedItem ->
            dialog?.dismiss()
            onItemSelected(selectedItem)
        }
        recycler.adapter = adapter

        val popupScope = CoroutineScope(Dispatchers.Main + Job())
        var searchJob: Job? = null

        val malTracker = if (serviceType == TrackerServiceType.MY_ANIME_LIST) MyAnimeListTracker(context) else null
        val aniTracker = if (serviceType == TrackerServiceType.ANILIST) AniListTracker(context) else null

        fun doSearch(query: String) {
            val cleanQuery = query.trim()
            if (cleanQuery.isEmpty()) {
                progress.visibility = View.GONE
                textEmpty.visibility = View.VISIBLE
                textEmpty.text = context.getString(R.string.tracker_search_hint)
                adapter.updateItems(emptyList())
                return
            }

            progress.visibility = View.VISIBLE
            textEmpty.visibility = View.GONE

            if (serviceType == TrackerServiceType.MY_ANIME_LIST) {
                malTracker?.searchManga(cleanQuery, object : ApiListener<List<TrackerSearchResult>> {
                    override fun onSuccess(result: List<TrackerSearchResult>) {
                        popupScope.launch {
                            progress.visibility = View.GONE
                            if (result.isEmpty()) {
                                textEmpty.visibility = View.VISIBLE
                                textEmpty.text = context.getString(R.string.tracker_search_empty)
                            } else {
                                textEmpty.visibility = View.GONE
                            }
                            adapter.updateItems(result)
                        }
                    }

                    override fun onFailure(message: String) {
                        popupScope.launch {
                            progress.visibility = View.GONE
                            textEmpty.visibility = View.VISIBLE
                            textEmpty.text = context.getString(R.string.tracker_search_empty)
                            adapter.updateItems(emptyList())
                        }
                    }
                })
            } else {
                aniTracker?.searchManga(cleanQuery, object : ApiListener<List<TrackerSearchResult>> {
                    override fun onSuccess(result: List<TrackerSearchResult>) {
                        popupScope.launch {
                            progress.visibility = View.GONE
                            if (result.isEmpty()) {
                                textEmpty.visibility = View.VISIBLE
                                textEmpty.text = context.getString(R.string.tracker_search_empty)
                            } else {
                                textEmpty.visibility = View.GONE
                            }
                            adapter.updateItems(result)
                        }
                    }

                    override fun onFailure(message: String) {
                        popupScope.launch {
                            progress.visibility = View.GONE
                            textEmpty.visibility = View.VISIBLE
                            textEmpty.text = context.getString(R.string.tracker_search_empty)
                            adapter.updateItems(emptyList())
                        }
                    }
                })
            }
        }

        inputSearch.doAfterTextChanged {
            searchJob?.cancel()
            searchJob = popupScope.launch {
                delay(400)
                doSearch(it?.toString().orEmpty())
            }
        }

        inputSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchJob?.cancel()
                doSearch(inputSearch.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }

        if (initialQuery.isNotBlank()) {
            inputSearch.setText(initialQuery)
            doSearch(initialQuery)
        } else {
            textEmpty.visibility = View.VISIBLE
            textEmpty.text = context.getString(R.string.tracker_search_hint)
        }

        dialog = MaterialAlertDialogBuilder(context)
            .setView(view)
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}
