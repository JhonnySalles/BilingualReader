package br.com.fenix.bilingualreader.view.ui.history

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Intent
import android.content.res.Resources
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.BaseColumns
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.AbsListView
import android.widget.AutoCompleteTextView
import android.widget.CursorAdapter
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.SearchView
import android.widget.SimpleCursorAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.LibraryMangaType
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.service.listener.HistoryCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.view.adapter.history.HistoryCardAdapter
import br.com.fenix.bilingualreader.view.adapter.library.BaseAdapter
import br.com.fenix.bilingualreader.view.ui.reader.book.BookReaderActivity
import br.com.fenix.bilingualreader.view.ui.reader.manga.MangaReaderActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.supercharge.shimmerlayout.ShimmerLayout
import java.time.LocalDateTime
import kotlin.math.ceil


class HistoryFragment : Fragment() {

    private lateinit var mViewModel: HistoryViewModel
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mScrollUp: FloatingActionButton
    private lateinit var mScrollDown: FloatingActionButton
    private lateinit var miSearch: MenuItem
    private lateinit var searchView: SearchView
    private lateinit var miFilterType: MenuItem

    private lateinit var mSkeletonLayout: LinearLayout
    private lateinit var mShimmer: ShimmerLayout
    private lateinit var mInflater: LayoutInflater

    private var mFilterType: Type? = null

    private val mHandler = Handler(Looper.getMainLooper())
    private val mDismissUpButton = Runnable { mScrollUp.hide() }
    private val mDismissDownButton = Runnable { mScrollDown.hide() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_history, menu)
        super.onCreateOptionsMenu(menu, inflater)

        val miLibrary = menu.findItem(R.id.menu_history_library)
        miLibrary.subMenu?.clear()
        miLibrary.subMenu?.add(Menu.NONE, Menu.NONE, 100, requireContext().getString(R.string.history_menu_choice_all))?.setOnMenuItemClickListener { _: MenuItem? ->
                filterLibrary(null)
                (miLibrary.icon as AnimatedVectorDrawable).start()
                true
            }

        miLibrary.subMenu?.add(Menu.NONE, Menu.NONE, 101, mViewModel.mDefaultLibrary.title)?.setOnMenuItemClickListener { _: MenuItem? ->
            filterLibrary(mViewModel.mDefaultLibrary)
            (miLibrary.icon as AnimatedVectorDrawable).start()
            true
        }

        val manga = miLibrary.subMenu?.addSubMenu(Menu.NONE, Menu.NONE, 102,requireContext().getString(R.string.history_manga))
        val book = miLibrary.subMenu?.addSubMenu(Menu.NONE, Menu.NONE, 103,requireContext().getString(R.string.history_book))
        for (library in mViewModel.getLibraryList())
            when (library.type) {
                Type.BOOK -> book!!.add(library.title)?.setOnMenuItemClickListener { _: MenuItem? ->
                    filterLibrary(library)
                    (miLibrary.icon as AnimatedVectorDrawable).start()
                    true
                }

                Type.MANGA -> manga!!.add(library.title)?.setOnMenuItemClickListener { _: MenuItem? ->
                    filterLibrary(library)
                    (miLibrary.icon as AnimatedVectorDrawable).start()
                    true
                }
            }

        miFilterType = menu.findItem(R.id.menu_history_type)
        miFilterType.subMenu?.clear()
        miFilterType.subMenu?.add(requireContext().getString(R.string.history_menu_choice_all))?.setOnMenuItemClickListener { _: MenuItem? ->
            filterType(null)
            true
        }

        for (type in Type.values()) {
            val title = when (type) {
                Type.MANGA -> requireContext().getString(R.string.history_manga)
                Type.BOOK -> requireContext().getString(R.string.history_book)
            }
            miFilterType.subMenu?.add(title)?.setOnMenuItemClickListener { _: MenuItem? ->
                filterType(type)
                true
            }
        }

        val iconType: Int = if (mFilterType == null)
            R.drawable.ico_menu_type_all
        else when (mFilterType) {
            Type.MANGA -> R.drawable.ico_menu_type_manga
            Type.BOOK -> R.drawable.ico_menu_type_book
            else -> R.drawable.ico_menu_type_all
        }
        miFilterType.setIcon(iconType)

        miSearch = menu.findItem(R.id.menu_history_search)
        searchView = miSearch.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE

        val searchSrcTextView = miSearch.actionView!!.findViewById<View>(Resources.getSystem().getIdentifier("search_src_text", "id", "android")) as AutoCompleteTextView
        searchSrcTextView.threshold = 1
        searchSrcTextView.setDropDownBackgroundResource(R.drawable.list_item_suggestion_background)
        searchSrcTextView.setTextAppearance(R.style.SearchShadow)

        val from = arrayOf(SearchManager.SUGGEST_COLUMN_TEXT_1)
        val to = intArrayOf(R.id.list_item_suggestion)
        val suggestions = Util.getHistoryFilters(requireContext()).keys.toList()
        val cursorAdapter = SimpleCursorAdapter(requireContext(), R.layout.list_item_suggestion, null, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER)

        searchView.suggestionsAdapter = cursorAdapter
        searchView.setOnSuggestionListener(object: SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                return false
            }

            override fun onSuggestionClick(position: Int): Boolean {
                val cursor = searchView.suggestionsAdapter.getItem(position) as Cursor?
                if( cursor != null) {
                    val colum = cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1)
                    val selection = cursor.getString(colum)
                    val query = searchView.query.toString().substringBeforeLast('@', "") + " " + selection
                    searchView.setQuery(query.trim(), false)
                }
                return true
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            private var runFilter = Runnable { }
            private var lastSuggestion = ""
            override fun onQueryTextChange(newText: String?): Boolean {
                val cursor = MatrixCursor(arrayOf(BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1))
                cursorAdapter.changeCursor(cursor)

                if (newText != null) {
                    var substring = newText.substringAfterLast('@', "")
                    if (substring.isNotEmpty()) {
                        if (!substring.contains(':')) {
                            substring = substring.replace("@", "")
                            suggestions.forEachIndexed { index, suggestion ->
                                if (substring.isEmpty())
                                    cursor.addRow(arrayOf(index, "@$suggestion:"))
                                else if (suggestion.contains(substring, true))
                                    cursor.addRow(arrayOf(index, "@$suggestion:"))
                            }
                            return false
                        } else if (!substring.contains(' ')) {
                            mViewModel.getSuggestions(substring).let{
                                substring = substring.substringBefore(":")
                                it.forEachIndexed { index, suggestion ->
                                    run {
                                        if (suggestion.contains(' '))
                                            cursor.addRow(arrayOf(index, "@$substring:\"$suggestion\" "))
                                        else
                                            cursor.addRow(arrayOf(index, "@$substring:$suggestion "))
                                    }
                                }
                            }
                            return false
                        }
                    } else if (newText.endsWith("@", true)) {
                        suggestions.forEachIndexed { index, suggestion ->
                            cursor.addRow(arrayOf(index, "@$suggestion:"))
                        }
                        return false
                    }
                }

                if (newText?.trim().equals(lastSuggestion, true))
                    return false
                lastSuggestion = newText?.trim() ?: ""

                mHandler.removeCallbacks(runFilter)
                runFilter = Runnable { filter(newText) }
                mHandler.postDelayed(runFilter, GeneralConsts.DEFAULTS.DEFAULT_HANDLE_SEARCH_FILTER)

                return false
            }
        })
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_history_library -> {}
        }
        return super.onOptionsItemSelected(menuItem)
    }

    override fun onDestroyOptionsMenu() {
        mViewModel.clearFilter()
        super.onDestroyOptionsMenu()
    }

    private fun filter(text: String?) {
        mViewModel.filter.filter(text)
    }

    private fun filterLibrary(library: Library?) {
        mViewModel.filterLibrary(library)
    }

    private fun filterType(type: Type?) {
        mViewModel.filterType(type)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mViewModel = ViewModelProvider(this)[HistoryViewModel::class.java]

        val root = inflater.inflate(R.layout.fragment_history, container, false)
        mRecyclerView = root.findViewById(R.id.history_list)
        mScrollUp = root.findViewById(R.id.history_scroll_up)
        mScrollDown = root.findViewById(R.id.history_scroll_down)

        mSkeletonLayout = root.findViewById(R.id.skeleton_layout)
        mShimmer = root.findViewById(R.id.shimmer_skeleton)
        mInflater = inflater

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mRecyclerView)
        observer()
        return root
    }

    private var itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val history = mViewModel.getAndRemove(viewHolder.bindingAdapterPosition) ?: return
                val position = viewHolder.bindingAdapterPosition
                var excluded = false
                val dialog: AlertDialog =
                    MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
                        .setTitle(getString(R.string.manga_library_menu_delete))
                        .setMessage(getString(R.string.history_delete_description) + "\n" + history.name)
                        .setPositiveButton(
                            R.string.action_delete
                        ) { _, _ ->
                            mViewModel.deletePermanent(history)
                            mRecyclerView.adapter?.notifyItemRemoved(position)
                            excluded = true
                        }.setOnDismissListener {
                            if (!excluded) {
                                mViewModel.add(history, position)
                                mRecyclerView.adapter?.notifyItemChanged(position)
                            }
                        }
                        .create()
                dialog.show()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val historyAdapter = HistoryCardAdapter()
        mRecyclerView.adapter = historyAdapter
        mRecyclerView.layoutManager = GridLayoutManager(requireContext(), 1)
        val listener = object : HistoryCardListener {
            override fun onClick(history: History) {
                when (history) {
                    is Manga -> open(history)
                    is Book -> open(history)
                }
            }

            override fun onClickLong(history: History, view: View, position: Int) {
                when (history) {
                    is Manga -> openMenu(history, view, position)
                    is Book -> openMenu(history, view, position)
                }
            }
        }
        historyAdapter.attachListener(listener)

        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState != AbsListView.OnScrollListener.SCROLL_STATE_FLING)
                    setAnimationRecycler(true)
            }
        })

        mRecyclerView.setOnScrollChangeListener { _, _, _, _, yOld ->
            if (yOld > 20 && mScrollDown.visibility == View.VISIBLE) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (mHandler.hasCallbacks(mDismissDownButton))
                        mHandler.removeCallbacks(mDismissDownButton)
                } else
                    mHandler.removeCallbacks(mDismissDownButton)

                mScrollDown.hide()
            } else if (yOld < -20 && mScrollUp.visibility == View.VISIBLE) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (mHandler.hasCallbacks(mDismissUpButton))
                        mHandler.removeCallbacks(mDismissUpButton)
                } else
                    mHandler.removeCallbacks(mDismissUpButton)

                mScrollUp.hide()
            }

            if (yOld > 180) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (mHandler.hasCallbacks(mDismissUpButton))
                        mHandler.removeCallbacks(mDismissUpButton)
                } else
                    mHandler.removeCallbacks(mDismissUpButton)

                mHandler.postDelayed(mDismissUpButton, 3000)
                mScrollUp.show()
            } else if (yOld < -180) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (mHandler.hasCallbacks(mDismissDownButton))
                        mHandler.removeCallbacks(mDismissDownButton)
                } else
                    mHandler.removeCallbacks(mDismissDownButton)

                mHandler.postDelayed(mDismissDownButton, 3000)
                mScrollDown.show()
            }
        }

        mScrollUp.visibility = View.GONE
        mScrollDown.visibility = View.GONE

        mScrollUp.setOnClickListener {
            setAnimationRecycler(false)
            (mScrollUp.drawable as AnimatedVectorDrawable).start()
            mRecyclerView.smoothScrollToPosition(0)
        }
        mScrollDown.setOnClickListener {
            setAnimationRecycler(false)
            (mScrollDown.drawable as AnimatedVectorDrawable).start()
            mRecyclerView.smoothScrollToPosition((mRecyclerView.adapter as RecyclerView.Adapter).itemCount)
        }

    }

    private fun setAnimationRecycler(isAnimate: Boolean) {
        (mRecyclerView.adapter as HistoryCardAdapter).isAnimation = isAnimate
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        mViewModel.list {
            if (it > -1)
                mRecyclerView.adapter?.notifyItemChanged(0, it)
            else
                mRecyclerView.adapter?.notifyDataSetChanged()
        }
    }

    private fun updateList(list: ArrayList<History>) {
        (mRecyclerView.adapter as HistoryCardAdapter).updateList(list)
    }

    private fun observer() {
        mViewModel.loading.observe(viewLifecycleOwner) {
            if (it)
                showSkeleton(it)
            else
                animateReplaceSkeleton()
        }

        mViewModel.history.observe(viewLifecycleOwner) {
            updateList(it)
        }

        mViewModel.type.observe(viewLifecycleOwner) {
            onChangeIconFilterType(it)
        }
    }

    private fun onChangeIconFilterType(type: Type?) {
        if (!::miFilterType.isInitialized || mFilterType == type)
            return

        val icon: Int? = if (type == null)  {
            when (mFilterType) {
                Type.MANGA -> R.drawable.ico_animated_menu_type_filter_manga_to_all
                Type.BOOK -> R.drawable.ico_animated_menu_type_filter_book_to_all
                else -> null
            }
        } else if (mFilterType == null)  {
            when (type) {
                Type.MANGA -> R.drawable.ico_animated_menu_type_filter_all_to_manga
                Type.BOOK -> R.drawable.ico_animated_menu_type_filter_all_to_book
            }
        } else {
            when (type) {
                Type.MANGA -> R.drawable.ico_animated_menu_type_filter_book_to_manga
                Type.BOOK -> R.drawable.ico_animated_menu_type_filter_manga_to_book
            }
        }


        if (icon != null)
            MenuUtil.animatedSequenceDrawable(miFilterType, icon)

        mFilterType = type
    }

    private fun open(manga: Manga) {
        if (!manga.excluded && manga.file.exists()) {
            val intent = Intent(context, MangaReaderActivity::class.java)
            val bundle = Bundle()
            manga.lastAccess = LocalDateTime.now()
            bundle.putSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY, manga.library)
            bundle.putString(GeneralConsts.KEYS.MANGA.NAME, manga.title)
            bundle.putInt(GeneralConsts.KEYS.MANGA.MARK, manga.bookMark)
            bundle.putSerializable(GeneralConsts.KEYS.OBJECT.MANGA, manga)
            intent.putExtras(bundle)
            context?.startActivity(intent)
            mViewModel.updateLastAccess(manga)
        } else {
            if (!manga.excluded) {
                manga.excluded = true
                mViewModel.updateDelete(manga)
                mRecyclerView.adapter?.let {
                    (it as HistoryCardAdapter).notifyItemChanged(manga)
                }
            }

            MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
                .setTitle(getString(R.string.manga_excluded))
                .setMessage(manga.file.path)
                .setPositiveButton(
                    R.string.action_neutral
                ) { _, _ -> }
                .create()
                .show()
        }
    }

    private fun open(book: Book) {
        if (!book.excluded && book.file.exists()) {
            val intent = Intent(context, BookReaderActivity::class.java)
            val bundle = Bundle()
            bundle.putSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY, book.library)
            bundle.putString(GeneralConsts.KEYS.BOOK.NAME, book.title)
            bundle.putInt(GeneralConsts.KEYS.BOOK.MARK, book.bookMark)
            bundle.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK, book)
            intent.putExtras(bundle)
            context?.startActivity(intent)
            mViewModel.updateLastAccess(book)
        } else {
            if (!book.excluded) {
                book.excluded = true
                mViewModel.updateDelete(book)
                mRecyclerView.adapter?.let {
                    (it as HistoryCardAdapter).notifyItemChanged(book)
                }
            }

            MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
                .setTitle(getString(R.string.book_excluded))
                .setMessage(book.file.path)
                .setPositiveButton(
                    R.string.action_neutral
                ) { _, _ -> }
                .create()
                .show()
        }
    }

    fun openMenu(manga: Manga, view: View, position: Int) {
        val wrapper = ContextThemeWrapper(requireContext(), R.style.PopupMenu)
        val popup = PopupMenu(wrapper, view, 0, R.attr.popupMenuStyle, R.style.PopupMenu)
        popup.menuInflater.inflate(R.menu.menu_item_manga_file, popup.menu)

        if (manga.favorite)
            popup.menu.findItem(R.id.menu_item_manga_file_favorite).title = getString(R.string.manga_library_menu_favorite_remove)
        else
            popup.menu.findItem(R.id.menu_item_manga_file_favorite).title = getString(R.string.manga_library_menu_favorite_add)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_item_manga_file_favorite -> {
                    manga.favorite = !manga.favorite
                    mViewModel.save(manga)
                    mRecyclerView.adapter?.notifyItemChanged(position)
                }
                R.id.menu_item_manga_file_clear -> {
                    manga.lastAccess = LocalDateTime.MIN
                    manga.bookMark = 0
                    mViewModel.clear(manga)
                    mRecyclerView.adapter?.notifyItemChanged(position)
                }
                R.id.menu_item_manga_file_delete -> {
                    val dialog: AlertDialog =
                        MaterialAlertDialogBuilder(
                            requireActivity(),
                            R.style.AppCompatAlertDialogStyle
                        )
                            .setTitle(getString(R.string.manga_library_menu_delete))
                            .setMessage(getString(R.string.history_delete_description) + "\n" + manga.name)
                            .setPositiveButton(
                                R.string.action_positive
                            ) { _, _ ->
                                mViewModel.deletePermanent(manga)
                                mRecyclerView.adapter?.notifyItemRemoved(position)
                            }
                            .setNegativeButton(
                                R.string.action_negative
                            ) { _, _ -> }
                            .create()
                    dialog.show()
                }
                R.id.menu_item_manga_file_copy_name -> FileUtil(requireContext()).copyName(manga)
            }
            true
        }

        popup.show()
    }

    fun openMenu(book: Book, view: View, position: Int) {
        val wrapper = ContextThemeWrapper(requireContext(), R.style.PopupMenu)
        val popup = PopupMenu(wrapper, view, 0, R.attr.popupMenuStyle, R.style.PopupMenu)
        popup.menuInflater.inflate(R.menu.menu_item_book_file, popup.menu)

        if (book.favorite)
            popup.menu.findItem(R.id.menu_item_book_file_favorite).title = getString(R.string.book_library_menu_favorite_remove)
        else
            popup.menu.findItem(R.id.menu_item_book_file_favorite).title = getString(R.string.book_library_menu_favorite_add)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_item_book_file_favorite -> {
                    book.favorite = !book.favorite
                    mViewModel.save(book)
                    mRecyclerView.adapter?.notifyItemChanged(position)
                }
                R.id.menu_item_book_file_clear -> {
                    book.lastAccess = LocalDateTime.MIN
                    book.bookMark = 0
                    mViewModel.clear(book)
                    mRecyclerView.adapter?.notifyItemChanged(position)
                }
                R.id.menu_item_book_file_delete -> {
                    val dialog: AlertDialog =
                        MaterialAlertDialogBuilder(
                            requireActivity(),
                            R.style.AppCompatAlertDialogStyle
                        )
                            .setTitle(getString(R.string.book_library_menu_delete))
                            .setMessage(getString(R.string.history_delete_description) + "\n" + book.name)
                            .setPositiveButton(
                                R.string.action_positive
                            ) { _, _ ->
                                mViewModel.deletePermanent(book)
                                mRecyclerView.adapter?.notifyItemRemoved(position)
                            }
                            .setNegativeButton(
                                R.string.action_negative
                            ) { _, _ -> }
                            .create()
                    dialog.show()
                }
                R.id.menu_item_book_file_copy_name -> FileUtil(requireContext()).copyName(book)
            }
            true
        }

        popup.show()
    }

    private fun getSkeletonRowCount(): Int {
        val pxHeight: Int = Resources.getSystem().displayMetrics.heightPixels
        val skeletonTitleHeight = resources.getDimension(R.dimen.history_skeleton_title_height).toInt()
        val skeletonRowHeight = resources.getDimension(R.dimen.history_skeleton_height).toInt()
        return ceil(((pxHeight - skeletonTitleHeight) / skeletonRowHeight).toDouble()).toInt()
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            mSkeletonLayout.removeAllViews()

            mSkeletonLayout.addView(mInflater.inflate(R.layout.line_card_history_skeleton_title, null))
            for (i in 0..getSkeletonRowCount())
                mSkeletonLayout.addView(mInflater.inflate(R.layout.line_card_history_skeleton, null))

            mRecyclerView.animate().cancel()
            mSkeletonLayout.animate().cancel()

            mShimmer.visibility = View.VISIBLE
            mRecyclerView.visibility = View.GONE
            mSkeletonLayout.visibility = View.VISIBLE
            mShimmer.startShimmerAnimation()
            mSkeletonLayout.bringToFront()
        } else {
            mShimmer.stopShimmerAnimation()
            mShimmer.visibility = View.GONE
            mSkeletonLayout.visibility = View.GONE
            mRecyclerView.visibility = View.VISIBLE
            setAnimationRecycler(true)
        }
    }

    private fun animateReplaceSkeleton() {
        setAnimationRecycler(false)
        mRecyclerView.visibility = View.VISIBLE
        mRecyclerView.alpha = 0f
        mRecyclerView.animate().alpha(1f).setDuration(700).start()
        mSkeletonLayout.animate().alpha(0f).setDuration(1000).withEndAction { showSkeleton(false) }.start()
    }

}