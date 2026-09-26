package br.com.fenix.bilingualreader.view.ui.tracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.Track
import br.com.fenix.bilingualreader.model.enums.TrackStatus
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.LibraryRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.service.repository.TrackRepository
import br.com.fenix.bilingualreader.service.tracker.TrackerMatcher
import br.com.fenix.bilingualreader.service.tracker.model.TrackerServiceType
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.view.adapter.tracker.TrackerPreviewAdapter
import br.com.fenix.bilingualreader.view.adapter.tracker.TrackerPreviewItem
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class TrackerFragment : Fragment() {

    companion object {
        const val ARG_TRACK_ID = "arg_track_id"
        const val ARG_LIBRARY_ID = "arg_library_id"
        const val ARG_PREFILL_TITLE = "arg_prefill_title"
        const val ARG_PREFILL_REGEX = "arg_prefill_regex"
        const val ARG_PREFILL_MAL_ID = "arg_prefill_mal_id"
        const val ARG_PREFILL_ANI_ID = "arg_prefill_ani_id"
        const val ARG_PREFILL_VOL = "arg_prefill_vol"
        const val ARG_PREFILL_CHAP = "arg_prefill_chap"

        fun newInstance(
            trackId: Long? = null,
            libraryId: Long? = null,
            prefillTitle: String? = null,
            prefillRegex: String? = null,
            prefillMalId: Long? = null,
            prefillAniId: Long? = null,
            prefillVol: Int? = null,
            prefillChap: Float? = null
        ): TrackerFragment {
            val fragment = TrackerFragment()
            val args = Bundle()
            if (trackId != null) args.putLong(ARG_TRACK_ID, trackId)
            if (libraryId != null) args.putLong(ARG_LIBRARY_ID, libraryId)
            if (prefillTitle != null) args.putString(ARG_PREFILL_TITLE, prefillTitle)
            if (prefillRegex != null) args.putString(ARG_PREFILL_REGEX, prefillRegex)
            if (prefillMalId != null) args.putLong(ARG_PREFILL_MAL_ID, prefillMalId)
            if (prefillAniId != null) args.putLong(ARG_PREFILL_ANI_ID, prefillAniId)
            if (prefillVol != null) args.putInt(ARG_PREFILL_VOL, prefillVol)
            if (prefillChap != null) args.putFloat(ARG_PREFILL_CHAP, prefillChap)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var mTrackRepository: TrackRepository
    private lateinit var mLibraryRepository: LibraryRepository
    private lateinit var mMangaRepository: MangaRepository
    private lateinit var mBookRepository: BookRepository

    private var mCurrentTrack: Track? = null
    private var mLibraries: List<Library> = emptyList()
    private var mSelectedLibrary: Library? = null

    private var mMangasInSelectedLib: List<Manga> = emptyList()
    private var mBooksInSelectedLib: List<Book> = emptyList()

    private lateinit var mInputLibrary: MaterialAutoCompleteTextView
    private lateinit var mInputTitle: TextInputEditText
    private lateinit var mInputRegex: MaterialAutoCompleteTextView
    private lateinit var mInputMalIdLayout: TextInputLayout
    private lateinit var mInputMalId: TextInputEditText
    private lateinit var mInputAniIdLayout: TextInputLayout
    private lateinit var mInputAniId: TextInputEditText
    private lateinit var mInputTotalVolumes: TextInputEditText
    private lateinit var mInputTotalChapters: TextInputEditText
    private lateinit var mInputStatus: MaterialAutoCompleteTextView
    private lateinit var mInputScore: TextInputEditText
    private lateinit var mInputVolumesRead: TextInputEditText
    private lateinit var mInputChaptersRead: TextInputEditText
    private lateinit var mBtnDelete: Button
    private lateinit var mBtnSync: Button
    private lateinit var mBtnSave: Button

    private lateinit var mTextPreviewCount: TextView
    private lateinit var mTextPreviewEmpty: TextView
    private lateinit var mRecyclerPreview: RecyclerView
    private lateinit var mPreviewAdapter: TrackerPreviewAdapter

    private var mFilterJob: Job? = null
    private var mIsInitializing: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_tracker, container, false)

        val context = requireContext()
        mTrackRepository = TrackRepository(context)
        mLibraryRepository = LibraryRepository(context)
        mMangaRepository = MangaRepository(context)
        mBookRepository = BookRepository(context)

        mInputLibrary = root.findViewById(R.id.tracker_input_library)
        mInputTitle = root.findViewById(R.id.tracker_input_title)
        mInputRegex = root.findViewById(R.id.tracker_input_regex)
        mInputMalIdLayout = root.findViewById(R.id.tracker_input_mal_id_layout)
        mInputMalId = root.findViewById(R.id.tracker_input_mal_id)
        mInputAniIdLayout = root.findViewById(R.id.tracker_input_ani_id_layout)
        mInputAniId = root.findViewById(R.id.tracker_input_ani_id)
        mInputTotalVolumes = root.findViewById(R.id.tracker_input_total_volumes)
        mInputTotalChapters = root.findViewById(R.id.tracker_input_total_chapters)
        mInputStatus = root.findViewById(R.id.tracker_input_status)
        mInputScore = root.findViewById(R.id.tracker_input_score)
        mInputVolumesRead = root.findViewById(R.id.tracker_input_volumes_read)
        mInputChaptersRead = root.findViewById(R.id.tracker_input_chapters_read)
        mBtnDelete = root.findViewById(R.id.tracker_button_delete)
        mBtnSync = root.findViewById(R.id.tracker_button_sync)
        mBtnSave = root.findViewById(R.id.tracker_button_save)

        mTextPreviewCount = root.findViewById(R.id.tracker_preview_count)
        mTextPreviewEmpty = root.findViewById(R.id.tracker_preview_empty)
        mRecyclerPreview = root.findViewById(R.id.tracker_preview_recycler_view)

        mRecyclerPreview.layoutManager = LinearLayoutManager(context)
        mPreviewAdapter = TrackerPreviewAdapter()
        mRecyclerPreview.adapter = mPreviewAdapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    mPreviewAdapter.removeItem(pos)
                    val count = mPreviewAdapter.itemCount
                    mTextPreviewCount.text = getString(R.string.tracker_items_count, count)
                    if (count == 0) {
                        mTextPreviewEmpty.visibility = View.VISIBLE
                        mRecyclerPreview.visibility = View.GONE
                    }
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(mRecyclerPreview)

        val statusOptions = TrackStatus.entries.map { it.description }
        val statusAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, statusOptions)
        mInputStatus.setAdapter(statusAdapter)

        setupListeners()
        loadData()

        return root
    }

    private fun extractRawText(input: String): String {
        var text = input.trim()
        text = text.removePrefix("^(?i)").removePrefix("(?i)^").removePrefix("(?i)").removePrefix("^")
        text = text.removePrefix("^(?:\\[.*?\\]|\\(.*?\\))?\\s*")
        text = text.removePrefix(".*")
        text = text.removeSuffix("$").removeSuffix(".*")
        text = text.replace(Regex("""\\s\*\(\?:v\|vol\|c\|ch\|cap\)\?\.\*"""), "")
        text = text.replace(Regex("""\\(.)"""), "$1")
        return text.trim()
    }

    private fun updateRegexSuggestions(baseText: String) {
        val raw = if (baseText.isNotBlank()) extractRawText(baseText) else ""
        if (raw.isBlank() || !isAdded) return

        val escaped = Regex.escape(raw)
        val suggestions = listOf(
            "^$escaped.*",
            ".*$escaped.*",
            "^(?i)$escaped.*",
            "^(?:\\[.*?\\]|\\(.*?\\))?\\s*$escaped.*",
            "^$escaped\\s*(?:v|vol|c|ch|cap)?\\s*\\d*.*",
            "^$escaped$"
        ).distinct()

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, suggestions)
        mInputRegex.setAdapter(adapter)
    }

    private fun setupListeners() {
        mInputRegex.doAfterTextChanged {
            if (!mIsInitializing) {
                scheduleFilterPreview()
            }
        }

        mInputRegex.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val current = mInputTitle.text?.toString()?.ifBlank { mInputRegex.text?.toString() } ?: ""
                updateRegexSuggestions(current)
            }
        }

        mInputTitle.doAfterTextChanged {
            if (!mIsInitializing) {
                val title = it?.toString()?.trim() ?: ""
                if (title.isNotEmpty()) {
                    updateRegexSuggestions(title)
                    if (mInputRegex.text.isNullOrBlank()) {
                        mInputRegex.setText(Regex.escape(title))
                    }
                }
            }
        }

        mInputMalIdLayout.setEndIconOnClickListener {
            val initialQuery = mInputTitle.text?.toString()?.trim().orEmpty()
            TrackerSearchPopup.show(requireContext(), TrackerServiceType.MY_ANIME_LIST, initialQuery) { result ->
                mInputMalId.setText(result.id.toString())
                if (mInputTitle.text.isNullOrBlank()) {
                    mInputTitle.setText(result.title)
                }
                result.totalVolumes?.let { if (it > 0 && mInputTotalVolumes.text.isNullOrBlank()) mInputTotalVolumes.setText(it.toString()) }
                result.totalChapters?.let { if (it > 0 && mInputTotalChapters.text.isNullOrBlank()) mInputTotalChapters.setText(it.toString()) }
            }
        }

        mInputAniIdLayout.setEndIconOnClickListener {
            val initialQuery = mInputTitle.text?.toString()?.trim().orEmpty()
            TrackerSearchPopup.show(requireContext(), TrackerServiceType.ANILIST, initialQuery) { result ->
                mInputAniId.setText(result.id.toString())
                if (mInputTitle.text.isNullOrBlank()) {
                    mInputTitle.setText(result.title)
                }
                result.totalVolumes?.let { if (it > 0 && mInputTotalVolumes.text.isNullOrBlank()) mInputTotalVolumes.setText(it.toString()) }
                result.totalChapters?.let { if (it > 0 && mInputTotalChapters.text.isNullOrBlank()) mInputTotalChapters.setText(it.toString()) }
            }
        }

        mInputLibrary.setOnItemClickListener { _, _, position, _ ->
            if (position in mLibraries.indices) {
                mSelectedLibrary = mLibraries[position]
                loadLibraryItemsAndFilter()
            }
        }

        mBtnSave.setOnClickListener {
            saveTracker()
        }

        mBtnDelete.setOnClickListener {
            deleteTracker()
        }

        mBtnSync.setOnClickListener {
            Toast.makeText(requireContext(), R.string.tracker_sync, Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadData() {
        val trackId = arguments?.getLong(ARG_TRACK_ID) ?: 0L
        val libraryId = arguments?.getLong(ARG_LIBRARY_ID) ?: 0L

        mIsInitializing = true

        lifecycleScope.launch(Dispatchers.IO) {
            val defaultManga = Library(GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA, getString(R.string.manga_library_default), "", type = Type.MANGA)
            val defaultBook = Library(GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK, getString(R.string.book_library_default), "", type = Type.BOOK)
            val dbLibraries = mLibraryRepository.listEnabled()
            mLibraries = listOf(defaultManga, defaultBook) + dbLibraries

            val track = if (trackId > 0) mTrackRepository.get(trackId) else null

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                mCurrentTrack = track

                val libraryLabels = mLibraries.map { "${it.type.name} - ${it.title}" }
                val libAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, libraryLabels)
                mInputLibrary.setAdapter(libAdapter)

                if (track != null) {
                    mSelectedLibrary = mLibraries.find { it.id == track.fkLibrary }
                    mInputTitle.setText(track.title)
                    mInputRegex.setText(track.titleRegex)
                    mInputMalId.setText(track.malId?.toString() ?: "")
                    mInputAniId.setText(track.aniId?.toString() ?: "")
                    mInputTotalVolumes.setText(track.totalVolumes?.toString() ?: "")
                    mInputTotalChapters.setText(track.totalChapters?.toString() ?: "")
                    mInputStatus.setText(track.status.description, false)
                    mInputScore.setText(track.score?.toString() ?: "")
                    mInputVolumesRead.setText(track.volumesRead.toString())
                    mInputChaptersRead.setText(track.chaptersRead.toString())
                    mBtnDelete.visibility = View.VISIBLE
                } else {
                    mSelectedLibrary = if (libraryId != 0L) mLibraries.find { it.id == libraryId } else mLibraries.firstOrNull()
                    val prefillTitle = arguments?.getString(ARG_PREFILL_TITLE) ?: ""
                    val prefillRegex = arguments?.getString(ARG_PREFILL_REGEX) ?: (if (prefillTitle.isNotBlank()) Regex.escape(prefillTitle) else "")
                    val prefillMal = arguments?.getLong(ARG_PREFILL_MAL_ID)
                    val prefillAni = arguments?.getLong(ARG_PREFILL_ANI_ID)
                    val prefillVol = arguments?.getInt(ARG_PREFILL_VOL) ?: 0
                    val prefillChap = arguments?.getFloat(ARG_PREFILL_CHAP)?.toInt() ?: 0

                    mInputTitle.setText(prefillTitle)
                    mInputRegex.setText(prefillRegex)
                    if (prefillMal != null && prefillMal > 0) mInputMalId.setText(prefillMal.toString())
                    if (prefillAni != null && prefillAni > 0) mInputAniId.setText(prefillAni.toString())
                    mInputStatus.setText(TrackStatus.READING.description, false)
                    mInputVolumesRead.setText(prefillVol.toString())
                    mInputChaptersRead.setText(prefillChap.toString())
                    mBtnDelete.visibility = View.GONE
                }

                mSelectedLibrary?.let { lib ->
                    val label = "${lib.type.name} - ${lib.title}"
                    mInputLibrary.setText(label, false)
                }

                loadLibraryItemsAndFilter {
                    mIsInitializing = false
                }
            }
        }
    }

    private fun loadLibraryItemsAndFilter(onCompleted: (() -> Unit)? = null) {
        val lib = mSelectedLibrary ?: run {
            onCompleted?.invoke()
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            if (lib.type == Type.MANGA) {
                mMangasInSelectedLib = mMangaRepository.list(lib) ?: emptyList()
                mBooksInSelectedLib = emptyList()
            } else {
                mBooksInSelectedLib = mBookRepository.list(lib) ?: emptyList()
                mMangasInSelectedLib = emptyList()
            }
            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                filterPreview()
                onCompleted?.invoke()
            }
        }
    }

    private fun scheduleFilterPreview() {
        mFilterJob?.cancel()
        mFilterJob = lifecycleScope.launch {
            delay(150)
            filterPreview()
        }
    }

    private fun filterPreview() {
        val regex = mInputRegex.text?.toString()?.trim() ?: ""
        val title = mInputTitle.text?.toString()?.trim() ?: ""

        val matchedItems = mutableListOf<TrackerPreviewItem>()

        if (mSelectedLibrary?.type == Type.MANGA) {
            for (manga in mMangasInSelectedLib) {
                val fileName = manga.file.name.ifBlank { manga.name }
                val candidateName = manga.title.ifBlank { fileName }
                if (TrackerMatcher.matches(regex, title, candidateName) ||
                    (fileName.isNotEmpty() && TrackerMatcher.matches(regex, title, fileName))) {
                    matchedItems.add(TrackerPreviewItem.MangaItem(manga))
                }
            }
        } else {
            for (book in mBooksInSelectedLib) {
                val fileName = book.fileName.ifBlank { book.name }
                val candidateName = book.title.ifBlank { fileName }
                if (TrackerMatcher.matches(regex, title, candidateName) ||
                    (fileName.isNotEmpty() && TrackerMatcher.matches(regex, title, fileName))) {
                    matchedItems.add(TrackerPreviewItem.BookItem(book))
                }
            }
        }

        mTextPreviewCount.text = getString(R.string.tracker_items_count, matchedItems.size)
        if (matchedItems.isEmpty()) {
            mTextPreviewEmpty.visibility = View.VISIBLE
            mRecyclerPreview.visibility = View.GONE
        } else {
            mTextPreviewEmpty.visibility = View.GONE
            mRecyclerPreview.visibility = View.VISIBLE
        }

        mPreviewAdapter.updateItems(matchedItems)
    }

    private fun saveTracker() {
        val library = mSelectedLibrary
        if (library == null || library.id == null) {
            Toast.makeText(requireContext(), R.string.tracker_library, Toast.LENGTH_SHORT).show()
            return
        }

        val title = mInputTitle.text?.toString()?.trim() ?: ""
        val regex = mInputRegex.text?.toString()?.trim() ?: ""
        val malId = mInputMalId.text?.toString()?.trim()?.toLongOrNull()
        val aniId = mInputAniId.text?.toString()?.trim()?.toLongOrNull()
        val totalVols = mInputTotalVolumes.text?.toString()?.trim()?.toIntOrNull()
        val totalChaps = mInputTotalChapters.text?.toString()?.trim()?.toIntOrNull()
        val status = TrackStatus.fromString(mInputStatus.text?.toString())
        val score = mInputScore.text?.toString()?.trim()?.toFloatOrNull()
        val volsRead = mInputVolumesRead.text?.toString()?.trim()?.toIntOrNull() ?: 0
        val chapsRead = mInputChaptersRead.text?.toString()?.trim()?.toIntOrNull() ?: 0

        if (title.isBlank() && regex.isBlank()) {
            Toast.makeText(requireContext(), R.string.tracker_series_title, Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val trackToSave = mCurrentTrack?.copy() ?: Track(
                fkLibrary = library.id!!,
                title = title,
                titleRegex = regex
            )

            trackToSave.fkLibrary = library.id!!
            trackToSave.title = title
            trackToSave.titleRegex = regex
            trackToSave.malId = malId
            trackToSave.aniId = aniId
            trackToSave.totalVolumes = totalVols
            trackToSave.totalChapters = totalChaps
            trackToSave.status = status
            if (score != trackToSave.score) {
                trackToSave.score = score
                trackToSave.scoreDate = if (score != null) LocalDateTime.now() else null
            }
            trackToSave.volumesRead = volsRead
            trackToSave.chaptersRead = chapsRead

            if (trackToSave.id != null && trackToSave.id != 0L) {
                mTrackRepository.update(trackToSave)
            } else {
                val newId = mTrackRepository.save(trackToSave)
                trackToSave.id = newId
            }

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                Toast.makeText(requireContext(), R.string.tracker_save_success, Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun deleteTracker() {
        val track = mCurrentTrack ?: return
        val id = track.id ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            mTrackRepository.deleteById(id)
            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                Toast.makeText(requireContext(), R.string.action_delete, Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}
