package br.com.fenix.bilingualreader.view.ui.library.book

import android.Manifest
import android.app.ActivityOptions
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.BaseColumns
import android.util.Pair
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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.SimpleCursorAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager.widget.ViewPager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.LibraryBookType
import br.com.fenix.bilingualreader.model.enums.ListMode
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.model.enums.ShareMarkType
import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.service.listener.BookCardListener
import br.com.fenix.bilingualreader.service.listener.MainListener
import br.com.fenix.bilingualreader.service.listener.PopupOrderListener
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.service.scanner.ScannerBook
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.AnimationUtil
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.Notifications
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.view.adapter.library.BaseAdapter
import br.com.fenix.bilingualreader.view.adapter.library.BookGridCardAdapter
import br.com.fenix.bilingualreader.view.adapter.library.BookLineCardAdapter
import br.com.fenix.bilingualreader.view.adapter.library.BookSeparatorGridCardAdapter
import br.com.fenix.bilingualreader.view.components.ComponentsUtil
import br.com.fenix.bilingualreader.view.ui.detail.DetailActivity
import br.com.fenix.bilingualreader.view.ui.popup.PopupBookMark
import br.com.fenix.bilingualreader.view.ui.popup.PopupTags
import br.com.fenix.bilingualreader.view.ui.reader.book.BookReaderActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import io.supercharge.shimmerlayout.ShimmerLayout
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.max


class BookLibraryFragment : Fragment(), PopupOrderListener, SwipeRefreshLayout.OnRefreshListener {

    private val mLOGGER = LoggerFactory.getLogger(BookLibraryFragment::class.java)

    private val uniqueID: String = UUID.randomUUID().toString()

    private lateinit var mViewModel: BookLibraryViewModel
    private lateinit var mainFunctions: MainListener

    private lateinit var mMapOrder: HashMap<Order, String>
    private lateinit var mRoot: FrameLayout
    private lateinit var mRefreshLayout: SwipeRefreshLayout
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var miGridType: MenuItem
    private lateinit var miGridOrder: MenuItem
    private lateinit var miSearch: MenuItem
    private lateinit var searchView: SearchView
    private lateinit var mListener: BookCardListener
    private lateinit var mScrollUp: FloatingActionButton
    private lateinit var mScrollDown: FloatingActionButton
    private lateinit var mMenuPopupLibrary: FrameLayout
    private lateinit var mPopupLibraryView: ViewPager
    private lateinit var mPopupLibraryTab: TabLayout
    private lateinit var mPopupFilterFragment: LibraryBookPopupFilter
    private lateinit var mPopupOrderFragment: LibraryBookPopupOrder
    private lateinit var mPopupTypeFragment: LibraryBookPopupType
    private lateinit var mBottomSheet: BottomSheetBehavior<FrameLayout>

    private lateinit var mSkeletonLayout: LinearLayout
    private lateinit var mShimmer: ShimmerLayout
    private lateinit var mInflater: LayoutInflater

    private lateinit var mPopupTag: PopupTags

    private val mHandler = Handler(Looper.getMainLooper())
    private val mDismissUpButton = Runnable { mScrollUp.hide() }
    private val mDismissDownButton = Runnable { mScrollDown.hide() }

    companion object {
        var mSortType: Order = Order.Name
        var mSortDesc: Boolean = false
        var mGridType: LibraryBookType = LibraryBookType.LINE
    }

    private val mUpdateHandler: Handler = UpdateHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProvider(requireActivity()).get(BookLibraryViewModel::class.java)
        loadConfig()
        setHasOptionsMenu(true)

        if (!mViewModel.existStack(uniqueID))
            mViewModel.addStackLibrary(uniqueID, mViewModel.getLibrary())
    }

    override fun onDestroyOptionsMenu() {
        mViewModel.clearFilter()
        super.onDestroyOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_library_book, menu)
        super.onCreateOptionsMenu(menu, inflater)

        miGridType = menu.findItem(R.id.menu_book_library_type)
        miGridOrder = menu.findItem(R.id.menu_book_library_list_order)
        miSearch = menu.findItem(R.id.menu_book_library_search)

        searchView = miSearch.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE

        val searchSrcTextView = miSearch.actionView!!.findViewById<View>(Resources.getSystem().getIdentifier("search_src_text", "id", "android")) as AutoCompleteTextView
        searchSrcTextView.threshold = 1
        searchSrcTextView.setDropDownBackgroundResource(R.drawable.list_item_suggestion_background)
        searchSrcTextView.setTextAppearance(R.style.SearchShadow)

        val from = arrayOf(SearchManager.SUGGEST_COLUMN_TEXT_1)
        val to = intArrayOf(R.id.list_item_suggestion)
        val suggestions = Util.getBookFilters(requireContext()).keys.toList()
        val cursorAdapter = SimpleCursorAdapter(requireContext(), R.layout.list_item_suggestion, null, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER)
        mViewModel.loadTags()

        searchView.suggestionsAdapter = cursorAdapter
        searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                return false
            }

            override fun onSuggestionClick(position: Int): Boolean {
                val cursor = searchView.suggestionsAdapter.getItem(position) as Cursor?
                if (cursor != null) {
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
                val cursor = MatrixCursor(
                    arrayOf(
                        BaseColumns._ID,
                        SearchManager.SUGGEST_COLUMN_TEXT_1
                    )
                )
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

                mRefreshLayout.isEnabled = newText.isNullOrEmpty()
                mHandler.removeCallbacks(runFilter)
                runFilter = Runnable { filter(newText) }
                mHandler.postDelayed(runFilter, GeneralConsts.DEFAULTS.DEFAULT_HANDLE_SEARCH_FILTER)
                return false
            }
        })

        enableSearchView(searchView, !mRefreshLayout.isRefreshing)

        val iconGrid: Int = when (mViewModel.libraryType.value) {
            LibraryBookType.GRID_BIG -> R.drawable.ico_animated_type_grid_gridbig_exit
            LibraryBookType.GRID_MEDIUM -> R.drawable.ico_animated_type_grid_gridmedium_exit
            LibraryBookType.SEPARATOR_BIG -> R.drawable.ico_animated_type_grid_gridbig_separator_exit
            LibraryBookType.SEPARATOR_MEDIUM -> R.drawable.ico_animated_type_grid_gridmedium_separator_exit
            LibraryBookType.LINE -> R.drawable.ico_animated_type_grid_list_exit
            else -> R.drawable.ico_animated_type_grid_list_exit
        }

        miGridType.setIcon(iconGrid)

        val iconSort: Int = when (mSortType) {
            Order.LastAccess -> R.drawable.ico_animated_sort_to_desc_last_access
            Order.Favorite -> R.drawable.ico_animated_sort_to_desc_favorited
            Order.Date -> R.drawable.ico_animated_sort_to_desc_date_created
            Order.Author -> R.drawable.ico_animated_sort_to_desc_author
            Order.Genre -> R.drawable.ico_animated_sort_to_desc_tag
            else -> R.drawable.ico_animated_sort_to_desc_name
        }
        miGridOrder.setIcon(iconSort)

        MenuUtil.longClick(requireActivity(), R.id.menu_book_library_list_order) {
            if (!mRefreshLayout.isRefreshing)
                onOpenMenuLibrary(1)
        }

        MenuUtil.longClick(requireActivity(), R.id.menu_book_library_type) {
            if (!mRefreshLayout.isRefreshing)
                onOpenMenuLibrary(0)
        }

        mViewModel.order.observe(viewLifecycleOwner) {
            if (it.first == mSortType && it.second == mSortDesc)
                return@observe

            val isDesc = if (mSortType == it.first) it.second else null
            onChangeIconSort(it.first, isDesc)
        }
    }

    private fun filter(text: String?) {
        mViewModel.filter.filter(text)
    }

    override fun onResume() {
        super.onResume()

        mViewModel.getLibrary().let {
            if (it.language == Libraries.DEFAULT)
                mainFunctions.clearLibraryTitle()
            else
                mainFunctions.changeLibraryTitle(it.title)
        }

        ScannerBook.getInstance(requireContext()).addUpdateHandler(mUpdateHandler)

        if (mViewModel.isEmpty())
            refresh()
        else
            mViewModel.updateList { change, indexes ->
                if (change && indexes.isNotEmpty())
                    notifyDataSet(indexes)
            }

        mViewModel.isLaunch = false
        if (ScannerBook.getInstance(requireContext()).isRunning(mViewModel.getLibrary()))
            setIsRefreshing(true)
        else
            setIsRefreshing(false)
    }

    override fun onStop() {
        ScannerBook.getInstance(requireContext()).removeUpdateHandler(mUpdateHandler)
        mainFunctions.clearLibraryTitle()
        super.onStop()
    }

    override fun onDestroy() {
        mViewModel.removeStackLibrary(uniqueID)
        super.onDestroy()
    }

    private inner class UpdateHandler : Handler() {
        override fun handleMessage(msg: Message) {
            val obj = msg.obj
            when (msg.what) {
                GeneralConsts.SCANNER.MESSAGE_BOOK_UPDATED_ADD -> refreshLibraryAddDelayed(obj as Book)
                GeneralConsts.SCANNER.MESSAGE_BOOK_UPDATED_REMOVE -> refreshLibraryRemoveDelayed(obj as Book)
                GeneralConsts.SCANNER.MESSAGE_BOOK_UPDATE_FINISHED -> {
                    setIsRefreshing(false)
                    if (obj as Boolean && ::mViewModel.isInitialized) { // Bug when rotate is necessary verify is initialized
                        mViewModel.updateList { change, _ ->
                            if (change)
                                sortList()
                        }
                    }
                }
            }
        }
    }

    private fun notifyDataSet(indexes: MutableList<kotlin.Pair<ListMode, Int>>) {
        if (indexes.any { it.first == ListMode.FULL })
            notifyDataSet(0, (mViewModel.listBook.value?.size ?: 1))
        else {
            for (index in indexes)
                when (index.first) {
                    ListMode.ADD -> notifyDataSet(index.second, insert = true)
                    ListMode.REM -> notifyDataSet(index.second, removed = true)
                    ListMode.MOD -> notifyDataSet(index.second)
                    else -> notifyDataSet(index.second)
                }
        }
    }

    private fun notifyDataSet(index: Int, range: Int = 0, insert: Boolean = false, removed: Boolean = false) {
        if (insert)
            mRecyclerView.adapter?.notifyItemInserted(index)
        else if (removed)
            mRecyclerView.adapter?.notifyItemRemoved(index)
        else if (range > 1)
            mRecyclerView.adapter?.notifyItemRangeChanged(index, range)
        else
            mRecyclerView.adapter?.notifyItemChanged(index)
    }

    private fun refreshLibraryAddDelayed(book: Book) {
        val index = mViewModel.addList(book)
        if (index > -1)
            mRecyclerView.adapter?.notifyItemInserted(index)
    }

    private fun refreshLibraryRemoveDelayed(book: Book) {
        val index = mViewModel.remList(book)
        if (index > -1)
            mRecyclerView.adapter?.notifyItemRemoved(index)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_book_library_type -> mViewModel.changeLibraryType()
            R.id.menu_book_library_list_order -> onChangeSort()
        }
        return super.onOptionsItemSelected(menuItem)
    }

    private fun onOpenMenuLibrary(select: Int = 0) {
        mPopupLibraryTab.selectTab(mPopupLibraryTab.getTabAt(select))
        mBottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
        AnimationUtil.animatePopupOpen(requireActivity(), mMenuPopupLibrary)
    }

    private fun onChangeSort() {
        if (mRefreshLayout.isRefreshing)
            return

        val orderBy = when (mViewModel.order.value?.first) {
            Order.Name -> Order.Date
            Order.Date -> Order.Favorite
            Order.Favorite -> Order.LastAccess
            Order.LastAccess -> Order.Genre
            Order.Genre -> Order.Author
            Order.Author -> Order.Series
            else -> Order.Name
        }

        Toast.makeText(
            requireContext(),
            getString(R.string.menu_reading_book_order_change, getString(orderBy.getDescription())),
            Toast.LENGTH_SHORT
        ).show()

        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
        with(sharedPreferences.edit()) {
            this!!.putString(GeneralConsts.KEYS.LIBRARY.BOOK_ORDER, orderBy.toString())
            this.commit()
        }

        if (mViewModel.listBook.value != null) {
            mViewModel.sorted(orderBy)
            when (mViewModel.libraryType.value) {
                LibraryBookType.SEPARATOR_BIG,
                LibraryBookType.SEPARATOR_MEDIUM -> updateList(mViewModel.listBook.value!!)
                else -> notifyDataSet(0, (mViewModel.listBook.value?.size ?: 1))
            }
        }
    }

    private fun onChangeIconSort(order: Order, isDesc: Boolean?) {
        if (isDesc != null) {
            val icon: Int? = when (order) {
                Order.Name -> if (isDesc) R.drawable.ico_animated_sort_to_desc_name else R.drawable.ico_animated_sort_to_asc_name
                Order.Favorite -> if (isDesc) R.drawable.ico_animated_sort_to_desc_favorited else R.drawable.ico_animated_sort_to_asc_favorited
                Order.LastAccess -> if (isDesc) R.drawable.ico_animated_sort_to_desc_last_access else R.drawable.ico_animated_sort_to_asc_last_access
                Order.Date -> if (isDesc) R.drawable.ico_animated_sort_to_desc_date_created else R.drawable.ico_animated_sort_to_asc_date_created
                Order.Author -> if (isDesc) R.drawable.ico_animated_sort_to_desc_author else R.drawable.ico_animated_sort_to_asc_author
                Order.Genre -> if (isDesc) R.drawable.ico_animated_sort_to_desc_tag else R.drawable.ico_animated_sort_to_asc_tag
                else -> null
            }
            mSortDesc = isDesc
            if (icon != null)
                MenuUtil.animatedSequenceDrawable(miGridOrder, icon)
        } else {
            val initial: Int? = if (mSortDesc)
                when (mSortType) {
                    Order.Name -> R.drawable.ico_animated_sort_desc_to_asc_ico_exit_name
                    Order.Favorite -> R.drawable.ico_animated_sort_desc_to_asc_ico_exit_favorited
                    Order.LastAccess -> R.drawable.ico_animated_sort_desc_to_asc_ico_exit_last_access
                    Order.Date -> R.drawable.ico_animated_sort_desc_to_asc_ico_exit_date_created
                    Order.Author -> R.drawable.ico_animated_sort_desc_to_asc_ico_exit_author
                    Order.Genre -> R.drawable.ico_animated_sort_desc_to_asc_ico_exit_tag
                    else -> null
                } else
                when (mSortType) {
                    Order.Name -> R.drawable.ico_animated_sort_asc_ico_exit_name
                    Order.Favorite -> R.drawable.ico_animated_sort_asc_ico_exit_favorited
                    Order.LastAccess -> R.drawable.ico_animated_sort_asc_ico_exit_last_access
                    Order.Date -> R.drawable.ico_animated_sort_asc_ico_exit_date_created
                    Order.Author -> R.drawable.ico_animated_sort_asc_ico_exit_author
                    Order.Genre -> R.drawable.ico_animated_sort_asc_ico_exit_tag
                    else -> null
                }

            val final: Int? = when (order) {
                Order.Name -> R.drawable.ico_animated_sort_asc_ico_enter_name
                Order.Favorite -> R.drawable.ico_animated_sort_asc_ico_enter_favorited
                Order.LastAccess -> R.drawable.ico_animated_sort_asc_ico_enter_last_access
                Order.Date -> R.drawable.ico_animated_sort_asc_ico_enter_date_created
                Order.Author -> R.drawable.ico_animated_sort_asc_ico_enter_author
                Order.Genre -> R.drawable.ico_animated_sort_asc_ico_enter_tag
                else -> null
            }

            if (initial != null && final != null)
                MenuUtil.animatedSequenceDrawable(miGridOrder, initial, final)

            mSortDesc = false
        }
        mSortType = order
    }

    private fun sortList() {
        mViewModel.sorted()
        val range = (mViewModel.listBook.value?.size ?: 1)
        notifyDataSet(0, range)
    }

    private fun onChangeLayout(type: LibraryBookType) {
        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
        with(sharedPreferences.edit()) {
            this!!.putString(GeneralConsts.KEYS.LIBRARY.BOOK_LIBRARY_TYPE, type.toString())
            this.commit()
        }

        onChangeIconLayout(type)
        generateLayout(type)
        updateList(mViewModel.listBook.value!!)
    }

    private fun onChangeIconLayout(type: LibraryBookType) {
        if (!::miGridType.isInitialized)
            return

        val initial: Int? = when (mGridType) {
            LibraryBookType.GRID_BIG -> R.drawable.ico_animated_type_grid_gridbig_exit
            LibraryBookType.GRID_MEDIUM -> R.drawable.ico_animated_type_grid_gridmedium_exit
            LibraryBookType.SEPARATOR_BIG -> R.drawable.ico_animated_type_grid_gridbig_separator_exit
            LibraryBookType.SEPARATOR_MEDIUM -> R.drawable.ico_animated_type_grid_gridmedium_separator_exit
            LibraryBookType.LINE -> R.drawable.ico_animated_type_grid_list_exit
            else -> null
        }

        val final: Int? = when (type) {
            LibraryBookType.GRID_BIG -> R.drawable.ico_animated_type_grid_gridbig_enter
            LibraryBookType.GRID_MEDIUM -> R.drawable.ico_animated_type_grid_gridmedium_enter
            LibraryBookType.SEPARATOR_BIG -> R.drawable.ico_animated_type_grid_gridbig_separator_enter
            LibraryBookType.SEPARATOR_MEDIUM -> R.drawable.ico_animated_type_grid_gridmedium_separator_enter
            LibraryBookType.LINE -> R.drawable.ico_animated_type_grid_list_enter
            else -> null
        }

        if (initial != null && final != null)
            MenuUtil.animatedSequenceDrawable(miGridType, initial, final)

        mGridType = type
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_book_library, container, false)

        mRoot = root.findViewById(R.id.frame_book_library_root)
        mRecyclerView = root.findViewById(R.id.book_library_recycler_view)
        mRefreshLayout = root.findViewById(R.id.book_library_refresh)
        mScrollUp = root.findViewById(R.id.book_library_scroll_up)
        mScrollDown = root.findViewById(R.id.book_library_scroll_down)

        mMenuPopupLibrary = root.findViewById(R.id.book_library_popup_menu_library)
        mPopupLibraryTab = root.findViewById(R.id.book_library_popup_library_tab)
        mPopupLibraryView = root.findViewById(R.id.book_library_popup_library_view_pager)

        mSkeletonLayout = root.findViewById(R.id.skeleton_layout)
        mShimmer = root.findViewById(R.id.shimmer_skeleton)
        mInflater = inflater

        ComponentsUtil.setThemeColor(requireContext(), mRefreshLayout)
        mRefreshLayout.setOnRefreshListener(this)
        mRefreshLayout.isEnabled = true
        mRefreshLayout.setProgressViewOffset(false, (resources.getDimensionPixelOffset(R.dimen.default_navigator_header_margin_top) + 60) * -1, 10)

        mPopupTag = PopupTags(requireContext())

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

        mPopupLibraryTab.setupWithViewPager(mPopupLibraryView)

        mPopupFilterFragment = LibraryBookPopupFilter()
        mPopupOrderFragment = LibraryBookPopupOrder()
        mPopupTypeFragment = LibraryBookPopupType()

        mPopupOrderFragment.setListener(this)

        BottomSheetBehavior.from(mMenuPopupLibrary).apply {
            peekHeight = 255
            this.state = BottomSheetBehavior.STATE_COLLAPSED
            mBottomSheet = this
        }
        mBottomSheet.isDraggable = true

        root.findViewById<ImageView>(R.id.book_library_popup_menu_order_filter_touch).let {
            it.setOnClickListener {
                if (mBottomSheet.state == BottomSheetBehavior.STATE_COLLAPSED)
                    mBottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
                else
                    mBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            }
            it.setOnLongClickListener {
                AnimationUtil.animatePopupClose(requireActivity(), mMenuPopupLibrary)
                true
            }
        }

        val viewFilterOrderPagerAdapter = ViewPagerAdapter(childFragmentManager, 0)
        viewFilterOrderPagerAdapter.addFragment(
            mPopupTypeFragment,
            resources.getString(R.string.popup_library_book_tab_item_type)
        )
        viewFilterOrderPagerAdapter.addFragment(
            mPopupFilterFragment,
            resources.getString(R.string.popup_library_book_tab_item_filter)
        )
        viewFilterOrderPagerAdapter.addFragment(
            mPopupOrderFragment,
            resources.getString(R.string.popup_library_book_tab_item_ordering)
        )

        mPopupLibraryView.adapter = viewFilterOrderPagerAdapter

        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState != AbsListView.OnScrollListener.SCROLL_STATE_FLING)
                    setAnimationRecycler(true)
            }
        })

        mRecyclerView.setOnScrollChangeListener { _, _, _, _, yOld ->
            if (mRefreshLayout.isRefreshing)
                return@setOnScrollChangeListener

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

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mRecyclerView)

        mListener = object : BookCardListener {
            override fun onClick(book: Book, root: View) {
                if (book.file.exists()) {
                    val intent = Intent(context, BookReaderActivity::class.java)
                    val bundle = Bundle()
                    bundle.putSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY, mViewModel.getLibrary())
                    bundle.putString(GeneralConsts.KEYS.BOOK.NAME, book.title)
                    bundle.putInt(GeneralConsts.KEYS.BOOK.MARK, book.bookMark)
                    bundle.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK, book)
                    intent.putExtras(bundle)

                    val idText = if (mViewModel.libraryType.value != LibraryBookType.LINE)
                        R.id.book_grid_title
                    else
                        R.id.book_line_title

                    val idAuthor = if (mViewModel.libraryType.value != LibraryBookType.LINE)
                        R.id.book_grid_sub_title
                    else
                        R.id.book_line_author

                    val idProgress = if (mViewModel.libraryType.value != LibraryBookType.LINE)
                        R.id.book_grid_progress
                    else
                        R.id.book_line_progress

                    val idCover = if (mViewModel.libraryType.value != LibraryBookType.LINE)
                        R.id.book_grid_image_cover
                    else
                        R.id.book_line_image_cover

                    val pImageCover: Pair<View, String> = Pair(root.findViewById<ImageView>(idCover), "transition_book_cover")
                    val pTitle: Pair<View, String> = Pair(root.findViewById<TextView>(idText), "transition_book_title")
                    val pAuthor: Pair<View, String> = Pair(root.findViewById<TextView>(idAuthor), "transition_book_author")
                    val pProgress: Pair<View, String> = Pair(root.findViewById<ProgressBar>(idProgress), "transition_progress_bar")

                    val options = ActivityOptions.makeSceneTransitionAnimation(requireActivity(), *arrayOf(pImageCover, pTitle, pAuthor, pProgress))

                    context?.startActivity(intent, options.toBundle())
                    requireActivity().overridePendingTransition(R.anim.fade_in_fragment_add_enter, R.anim.fade_out_fragment_remove_exit)
                } else {
                    removeList(book)
                    mViewModel.delete(book)
                    MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
                        .setTitle(getString(R.string.book_excluded))
                        .setMessage(getString(R.string.file_not_found))
                        .setPositiveButton(
                            R.string.action_neutral
                        ) { _, _ -> }
                        .create()
                        .show()
                }
            }

            override fun onClickFavorite(book: Book) {
                mViewModel.save(book)
            }

            override fun onClickConfig(book: Book, root: View, item: View, position: Int) {
                val wrapper = ContextThemeWrapper(requireContext(), R.style.PopupMenu)
                val popup = PopupMenu(wrapper, item, 0, R.attr.popupMenuStyle, R.style.PopupMenu)
                popup.menuInflater.inflate(R.menu.menu_book_config, popup.menu)

                popup.setOnMenuItemClickListener { menu ->
                    when (menu.itemId) {
                        R.id.menu_book_config_send -> shareBook(book)
                        R.id.menu_book_config_clear_progress -> {
                            mViewModel.clearHistory(book)
                            notifyDataSet(position)
                        }

                        R.id.menu_book_config_delete -> deleteBook(book, position)
                        R.id.menu_book_config_detail -> goBookDetail(book, root, position)
                        R.id.menu_book_config_tag -> {
                            mPopupTag.getPopupTags(book) { mViewModel.loadTags() }
                        }
                        R.id.menu_book_config_book_mark -> {
                            val onUpdate: (History) -> (Unit) = {
                                mViewModel.save(it as Book)
                                mViewModel.updateList(position)
                                notifyDataSet(position)
                            }
                            PopupBookMark(requireActivity(), requireActivity().supportFragmentManager)
                                .getPopupBookMark(book, onUpdate) { change, book ->
                                    if (change)
                                        onUpdate(book)
                                }
                        }
                    }
                    true
                }
                popup.show()
            }

            override fun onClickLong(book: Book, view: View, position: Int) {
                if (mRefreshLayout.isRefreshing)
                    return

                goBookDetail(book, view, position)
            }

        }
        observer()
        mViewModel.list {
            if (it)
                sortList()
        }

        if (!Storage.isPermissionGranted(requireContext()))
            Storage.takePermission(requireContext(), requireActivity())

        generateLayout(mViewModel.libraryType.value!!)
        setIsRefreshing(true)
        ScannerBook.getInstance(requireContext()).scanLibrary(mViewModel.getLibrary())

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            // Prevent backpress if query is actived
            override fun handleOnBackPressed() {
                if (searchView.query.isNotEmpty())
                    searchView.setQuery("", true)
                else if (!searchView.isIconified)
                    searchView.isIconified = true
                else {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        mainFunctions.clearLibraryTitle()
        return root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainListener)
            mainFunctions = context
    }

    private var itemRefresh: Int? = null
    private fun goBookDetail(book: Book, view: View, position: Int) {
        itemRefresh = position
        val intent = Intent(requireContext(), DetailActivity::class.java)
        val bundle = Bundle()
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY, mViewModel.getLibrary())
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK, book)
        intent.putExtras(bundle)

        val idText = if (mViewModel.libraryType.value != LibraryBookType.LINE)
            R.id.book_grid_title
        else
            R.id.book_line_title

        val idAuthor = if (mViewModel.libraryType.value != LibraryBookType.LINE)
            R.id.book_grid_sub_title
        else
            R.id.book_line_author

        val idProgress = if (mViewModel.libraryType.value != LibraryBookType.LINE)
            R.id.book_grid_progress
        else
            R.id.book_line_progress

        val idCover = if (mViewModel.libraryType.value != LibraryBookType.LINE)
            R.id.book_grid_image_cover
        else
            R.id.book_line_image_cover

        val pImageCover: Pair<View, String> = Pair(view.findViewById<ImageView>(idCover), "transition_book_cover")
        val pTitle: Pair<View, String> = Pair(view.findViewById<TextView>(idText), "transition_book_title")
        val pAuthor: Pair<View, String> = Pair(view.findViewById<TextView>(idAuthor), "transition_book_author")
        val pProgress: Pair<View, String> = Pair(view.findViewById<ProgressBar>(idProgress), "transition_progress_bar")

        val options = ActivityOptions.makeSceneTransitionAnimation(requireActivity(), *arrayOf(pImageCover, pTitle, pAuthor, pProgress))
        requireActivity().overridePendingTransition(R.anim.fade_in_fragment_add_enter, R.anim.fade_out_fragment_remove_exit)
        startActivityForResult(intent, GeneralConsts.REQUEST.BOOK_DETAIL, options.toBundle())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GeneralConsts.REQUEST.BOOK_DETAIL)
            notifyDataSet(mViewModel.updateList(itemRefresh ?: 0))
    }

    private fun loadConfig() {
        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
        mSortType = Order.valueOf(sharedPreferences.getString(GeneralConsts.KEYS.LIBRARY.BOOK_ORDER, Order.Name.toString()).toString())

        mGridType = LibraryBookType.valueOf(
            sharedPreferences.getString(
                GeneralConsts.KEYS.LIBRARY.BOOK_LIBRARY_TYPE,
                LibraryBookType.LINE.toString()
            ).toString()
        )
        mViewModel.setLibraryType(mGridType)
        mViewModel.sorted(mSortType)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == GeneralConsts.REQUEST.PERMISSION_FILES_ACCESS && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            refresh()
    }

    private fun getGridLayout(): RecyclerView.LayoutManager {
        val type = mViewModel.libraryType.value
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val columnWidth: Int = when (type) {
            LibraryBookType.SEPARATOR_BIG -> resources.getDimension(R.dimen.book_separator_grid_card_layout_width).toInt()
            LibraryBookType.SEPARATOR_MEDIUM -> if (isLandscape) resources.getDimension(R.dimen.book_separator_grid_card_layout_width_landscape_medium).toInt() else resources.getDimension(R.dimen.book_separator_grid_card_layout_width_medium).toInt()
            LibraryBookType.GRID_BIG -> resources.getDimension(R.dimen.book_grid_card_layout_width).toInt()
            LibraryBookType.GRID_MEDIUM -> if (isLandscape) resources.getDimension(R.dimen.book_grid_card_layout_width_landscape_medium).toInt() else resources.getDimension(R.dimen.book_grid_card_layout_width_medium).toInt()
            else -> resources.getDimension(R.dimen.book_grid_card_layout_width).toInt()
        } + 1

        val spaceCount: Int = max(1, (Resources.getSystem().displayMetrics.widthPixels -3) / columnWidth)
        return when (type) {
            LibraryBookType.SEPARATOR_BIG,
            LibraryBookType.SEPARATOR_MEDIUM -> StaggeredGridLayoutManager(spaceCount, StaggeredGridLayoutManager.VERTICAL)
            else -> GridLayoutManager(requireContext(), spaceCount)
        }
    }

    private fun generateLayout(type: LibraryBookType) {
        if (type == LibraryBookType.LINE) {
            val lineAdapter = BookLineCardAdapter()
            mRecyclerView.adapter = lineAdapter
            mRecyclerView.layoutManager = GridLayoutManager(requireContext(), 1)
            lineAdapter.attachListener(mListener)
            mRecyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_library_line)
        } else {
            val gridAdapter = when (type) {
                LibraryBookType.SEPARATOR_BIG,
                LibraryBookType.SEPARATOR_MEDIUM -> BookSeparatorGridCardAdapter(requireContext(), type)
                else -> BookGridCardAdapter(type)
            }
            mRecyclerView.adapter = gridAdapter
            mRecyclerView.layoutManager = getGridLayout()
            gridAdapter.attachListener(mListener)
            mRecyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_library_grid)
        }
    }

    private fun setAnimationRecycler(isAnimate: Boolean) {
        (mRecyclerView.adapter as BaseAdapter<*, *>).isAnimation = isAnimate
    }

    private fun removeList(book: Book) {
        (mRecyclerView.adapter as BaseAdapter<Book, *>).removeList(book)
    }

    private fun updateList(list: MutableList<Book>) {
        (mRecyclerView.adapter as BaseAdapter<Book, *>).updateList(mSortType, list)
    }

    private fun observer() {
        mViewModel.loading.observe(viewLifecycleOwner) {
            if (!it)
                animateReplaceSkeleton()
            else
                showSkeleton(it)
        }

        mViewModel.listBook.observe(viewLifecycleOwner) {
            updateList(it)
        }
        mViewModel.libraryType.observe(viewLifecycleOwner) {
            onChangeLayout(it)

            if (mSkeletonLayout.isVisible)
                showSkeleton(true)
        }
    }

    fun setIsRefreshing(enabled: Boolean) {
        try {
            mRefreshLayout.isRefreshing = enabled

            if (!::searchView.isInitialized || !::mRecyclerView.isInitialized)
                return

            if (enabled)
                searchView.clearFocus()
            enableSearchView(searchView, !enabled)
        } catch (e: Exception) {
            mLOGGER.error("Disable search button error: " + e.message, e)
        }
    }

    private fun enableSearchView(view: View, enabled: Boolean) {
        view.isEnabled = enabled
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                enableSearchView(child, enabled)
            }
        }
    }

    override fun onRefresh() {
        shareMarksToCloud()
        refresh()
    }

    private fun shareMarksToCloud() {
        GeneralConsts.getSharedPreferences(requireContext()).let { share ->
            if (share.getBoolean(GeneralConsts.KEYS.SYSTEM.SHARE_MARK_ENABLED, false)) {
                val notification = Notifications.getNotification(requireContext(), getString(R.string.notifications_share_mark_drive_title), getString(R.string.notifications_share_mark_drive_content))
                val notificationManager = NotificationManagerCompat.from(requireContext())
                val notifyId = Notifications.getID()

                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
                    !GeneralConsts.getSharedPreferences(requireContext()).getBoolean(GeneralConsts.KEYS.LIBRARIES.NOTIFICATION_SOLICITED, false)) {
                    GeneralConsts.getSharedPreferences(requireContext()).edit(commit = true) { putBoolean(GeneralConsts.KEYS.LIBRARIES.NOTIFICATION_SOLICITED, true) }
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.POST_NOTIFICATIONS), GeneralConsts.REQUEST.PERMISSION_NOTIFICATIONS)
                }

                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                    notificationManager.notify(notifyId, notification.build())

                mViewModel.processShareMarks(requireContext(), notifyId) { shareMark: ShareMarkType, idNotification: Int ->
                    val msg = when (shareMark) {
                        ShareMarkType.SUCCESS, ShareMarkType.NOTIFY_DATA_SET -> {
                            if (shareMark == ShareMarkType.NOTIFY_DATA_SET)
                                sortList()

                            if (ShareMarkType.send > 0 || ShareMarkType.receive > 0)
                                getString(R.string.book_share_mark_processed, ShareMarkType.send, ShareMarkType.receive)
                            else
                                getString(R.string.book_share_mark_without_alteration)
                        }

                        ShareMarkType.NOT_ALTERATION -> getString(R.string.book_share_mark_without_alteration)
                        ShareMarkType.NEED_PERMISSION_DRIVE -> {
                            startActivityForResult(shareMark.intent, GeneralConsts.REQUEST.DRIVE_AUTHORIZATION)
                            getString(R.string.book_share_mark_drive_need_permission)
                        }

                        ShareMarkType.NOT_CONNECT_FIREBASE -> getString(R.string.book_share_mark_firebase_not_connected)
                        ShareMarkType.NOT_CONNECT_DRIVE -> getString(R.string.book_share_mark_drive_need_sign_in)
                        ShareMarkType.ERROR_DOWNLOAD -> getString(R.string.book_share_mark_error_download)
                        ShareMarkType.ERROR_UPLOAD -> getString(R.string.book_share_mark_error_upload)
                        ShareMarkType.ERROR_NETWORK -> getString(R.string.book_share_mark_error_network)
                        ShareMarkType.SYNC_IN_PROGRESS -> getString(R.string.book_share_mark_sync_in_progress)
                        else -> getString(R.string.book_share_mark_unprocessed)
                    }

                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    notification.setContentText(msg)
                        .setProgress(0, 0, false)
                        .setOngoing(false)

                    if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                        notificationManager.notify(idNotification, notification.build())
                }
            }
        }
    }

    private fun refresh() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mHandler.hasCallbacks(mDismissUpButton))
                mHandler.removeCallbacks(mDismissUpButton)
            if (mHandler.hasCallbacks(mDismissDownButton))
                mHandler.removeCallbacks(mDismissDownButton)
        } else {
            mHandler.removeCallbacks(mDismissUpButton)
            mHandler.removeCallbacks(mDismissDownButton)
        }

        mScrollUp.hide()
        mScrollDown.hide()

        mViewModel.updateList { change, _ -> if (change) sortList() }

        if (!ScannerBook.getInstance(requireContext()).isRunning(mViewModel.getLibrary())) {
            setIsRefreshing(true)
            ScannerBook.getInstance(requireContext()).scanLibrary(mViewModel.getLibrary())
        }
    }

    override fun popupOrderOnChange() {
        when (mViewModel.libraryType.value) {
            LibraryBookType.SEPARATOR_BIG,
            LibraryBookType.SEPARATOR_MEDIUM -> updateList(mViewModel.listBook.value!!)
            else -> notifyDataSet(0, (mViewModel.listBook.value?.size ?: 1))
        }
    }

    override fun popupSorted(order: Order) {
        mViewModel.sorted(order)
    }

    override fun popupSorted(order: Order, isDesc: Boolean) {
        mViewModel.sorted(order, isDesc)
    }

    override fun popupGetOrder(): kotlin.Pair<Order, Boolean>? {
        return mViewModel.order.value
    }

    override fun popupGetObserver(): LiveData<kotlin.Pair<Order, Boolean>> {
        return mViewModel.order
    }

    private var itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun onMove(recyclerView: RecyclerView, viewHolder: ViewHolder, target: ViewHolder): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
            val book = mViewModel.getAndRemove(viewHolder.bindingAdapterPosition) ?: return
            val position = viewHolder.bindingAdapterPosition
            deleteBook(book, position)
        }
    }

    private fun deleteBook(book: Book, position: Int) {
        var excluded = false
        val dialog: AlertDialog = MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
                .setTitle(getString(R.string.book_library_menu_delete))
                .setMessage(getString(R.string.book_library_menu_delete_description) + "\n" + book.file.name)
                .setPositiveButton(R.string.action_delete) { _, _ ->
                    deleteFile(book)
                    notifyDataSet(position, removed = true)
                    excluded = true
                }.setOnDismissListener {
                    if (!excluded) {
                        mViewModel.add(book, position)
                        notifyDataSet(position)
                    }
                }
                .create()
        dialog.show()
    }

    private fun deleteFile(book: Book?) {
        if (book?.file != null) {
            removeList(book)
            mViewModel.delete(book)
            if (book.file.exists()) {
                val isDeleted = book.file.delete()
                mLOGGER.info("File deleted ${book.name}: $isDeleted")
            }
        }
    }

    private fun shareBook(book: Book) {
        try {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.type = book.fileType.getMimeType()
            shareIntent.putExtra(Intent.EXTRA_TEXT, book.fileName)
            shareIntent.putExtra(Intent.EXTRA_STREAM, book.file.toURI())
            startActivity(
                Intent.createChooser(
                    shareIntent,
                    requireContext().getString(R.string.book_library_share)
                )
            )
        } catch (e: Exception) {
            mLOGGER.error("Error share book.", e)
        }
    }

    inner class ViewPagerAdapter(fm: FragmentManager, behavior: Int) : FragmentPagerAdapter(fm, behavior) {
        private val fragments: MutableList<Fragment> = ArrayList()
        private val fragmentTitle: MutableList<String> = ArrayList()
        fun addFragment(fragment: Fragment, title: String) {
            fragments.add(fragment)
            fragmentTitle.add(title)
        }

        override fun getItem(position: Int): Fragment {
            return fragments[position]
        }

        override fun getCount(): Int {
            return fragments.size
        }

        override fun getPageTitle(position: Int): CharSequence {
            return fragmentTitle[position]
        }
    }

    private fun getSkeletonRowCount(type: LibraryBookType): Int {
        val pxHeight: Int = Resources.getSystem().displayMetrics.heightPixels
        val skeletonTitleHeight = if (type == LibraryBookType.SEPARATOR_MEDIUM || type == LibraryBookType.SEPARATOR_BIG) resources.getDimension(R.dimen.book_grid_skeleton_title_height).toInt() else 0
        val resource = when(type) {
            LibraryBookType.LINE -> R.dimen.book_line_skeleton_height
            LibraryBookType.SEPARATOR_BIG,
            LibraryBookType.GRID_BIG -> R.dimen.book_grid_skeleton_height_big
            else -> R.dimen.book_grid_skeleton_height_medium
        }
        val skeletonRowHeight = resources.getDimension(resource).toInt()
        return ceil(((pxHeight - skeletonTitleHeight) / skeletonRowHeight).toDouble()).toInt()
    }

    private fun getSkeletonGridItemPerRow(type: LibraryBookType): Int {
        val columnWidth = resources.getDimension(getSkeletonItemWidth(type)) + resources.getDimension(R.dimen.manga_grid_skeleton_divider) + 1
        return max(1, Resources.getSystem().displayMetrics.widthPixels / columnWidth.toInt())
    }

    private fun getSkeletonItemHeight(type: LibraryBookType) : Int {
        return if (type == LibraryBookType.SEPARATOR_BIG || type == LibraryBookType.GRID_BIG) R.dimen.book_grid_skeleton_card_image_big_height else R.dimen.book_grid_skeleton_card_image_medium_height
    }

    private fun getSkeletonItemWidth(type: LibraryBookType) : Int {
        return if (type == LibraryBookType.SEPARATOR_BIG || type == LibraryBookType.GRID_BIG) R.dimen.book_grid_skeleton_card_image_big_width else R.dimen.book_grid_skeleton_card_image_medium_width
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            mSkeletonLayout.removeAllViews()

            val type = mViewModel.libraryType.value ?: LibraryBookType.LINE

            if (type == LibraryBookType.SEPARATOR_BIG || type == LibraryBookType.SEPARATOR_MEDIUM)
                mSkeletonLayout.addView(mInflater.inflate(R.layout.grid_card_book_skeleton_title, null))

            for (i in 0..getSkeletonRowCount(type)) {
                if (type == LibraryBookType.LINE)
                    mSkeletonLayout.addView(mInflater.inflate(R.layout.line_card_book_skeleton, null))
                else {
                    val row = mInflater.inflate(R.layout.grid_card_book_skeleton, null)
                    var container = row.findViewById<LinearLayout>(R.id.grid_skeleton_items)
                    val height = resources.getDimension(getSkeletonItemHeight(type)).toInt()
                    val width = resources.getDimension(getSkeletonItemWidth(type)).toInt()
                    val margin = resources.getDimension(R.dimen.book_grid_skeleton_divider).toInt()
                    val items = getSkeletonGridItemPerRow(type)
                    val divider = ((Resources.getSystem().displayMetrics.widthPixels.toFloat() - (items * (width + margin))) / items).toInt()
                    container.removeAllViews()
                    for (i in 0.. items) {
                        val item = mInflater.inflate(R.layout.grid_card_book_skeleton_item, null)
                        val params = FrameLayout.LayoutParams(width, height)
                        params.setMargins(margin, margin, divider, 0)
                        item.layoutParams = params
                        container.addView(item)
                    }
                    container.invalidate()
                    mSkeletonLayout.addView(row)
                }
            }

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
