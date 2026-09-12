package br.com.fenix.bilingualreader.view.ui.tracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Track
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.listener.TrackerCardListener
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.LibraryRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.service.repository.TrackRepository
import br.com.fenix.bilingualreader.service.tracker.TrackerMatcher
import br.com.fenix.bilingualreader.view.adapter.tracker.TrackerListAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrackerListFragment : Fragment(), TrackerCardListener {

    private lateinit var mTrackRepository: TrackRepository
    private lateinit var mLibraryRepository: LibraryRepository
    private lateinit var mMangaRepository: MangaRepository
    private lateinit var mBookRepository: BookRepository

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mEmptyView: TextView
    private lateinit var mFabAdd: FloatingActionButton
    private lateinit var mAdapter: TrackerListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_tracker_list, container, false)

        val context = requireContext()
        mTrackRepository = TrackRepository(context)
        mLibraryRepository = LibraryRepository(context)
        mMangaRepository = MangaRepository(context)
        mBookRepository = BookRepository(context)

        mRecyclerView = root.findViewById(R.id.tracker_recycler_view)
        mEmptyView = root.findViewById(R.id.tracker_empty_view)
        mFabAdd = root.findViewById(R.id.tracker_fab_add)

        mRecyclerView.layoutManager = LinearLayoutManager(context)
        mAdapter = TrackerListAdapter(this)
        mRecyclerView.adapter = mAdapter

        mFabAdd.setOnClickListener {
            openTrackerDetail(null)
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        loadTrackers()
    }

    private fun loadTrackers() {
        lifecycleScope.launch(Dispatchers.IO) {
            val tracks = mTrackRepository.listAll()
            val libraries = mLibraryRepository.listEnabled()
            val librariesMap = libraries.associateBy { it.id ?: 0L }

            // Pre-calculate counts for each library
            val countsMap = mutableMapOf<Long, Int>()
            val libraryItemsCache = mutableMapOf<Long, List<String>>()

            for (lib in libraries) {
                val libId = lib.id ?: continue
                val items = if (lib.type == Type.MANGA) {
                    mMangaRepository.list(lib)?.map { it.title.ifBlank { it.file?.name ?: "" } } ?: emptyList()
                } else {
                    mBookRepository.list(lib)?.map { it.title.ifBlank { it.file?.name ?: "" } } ?: emptyList()
                }
                libraryItemsCache[libId] = items
            }

            for (track in tracks) {
                val items = libraryItemsCache[track.fkLibrary] ?: emptyList()
                var count = 0
                for (item in items) {
                    if (TrackerMatcher.matches(track.titleRegex, track.title, item)) {
                        count++
                    }
                }
                track.id?.let { countsMap[it] = count }
            }

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                if (tracks.isEmpty()) {
                    mEmptyView.visibility = View.VISIBLE
                    mRecyclerView.visibility = View.GONE
                } else {
                    mEmptyView.visibility = View.GONE
                    mRecyclerView.visibility = View.VISIBLE
                }
                mAdapter.updateList(tracks, librariesMap, countsMap)
            }
        }
    }

    override fun onClick(track: Track) {
        openTrackerDetail(track.id)
    }

    private fun openTrackerDetail(trackId: Long?) {
        TrackerActivity.start(requireContext(), trackId = trackId)
    }
}
