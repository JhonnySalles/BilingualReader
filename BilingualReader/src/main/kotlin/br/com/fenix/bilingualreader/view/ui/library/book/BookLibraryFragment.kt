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
import android.provider.BaseColumns
import android.util.Pair
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
import br.com.fenix.bilingualreader.util.helpers.AdapterUtil.AdapterUtils
import br.com.fenix.bilingualreader.util.helpers.AnimationUtil
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.NavigationUtil.NavigationUtils.overrideActivityTransitionCompat
import br.com.fenix.bilingualreader.util.helpers.Notifications
import br.com.fenix.bilingualreader.util.helpers.PopupUtil.PopupUtils
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.util.helpers.blurOnceDeferred
import br.com.fenix.bilingualreader.view.adapter.library.BaseAdapter
import br.com.fenix.bilingualreader.view.adapter.library.BookCoverCardAdapter
import br.com.fenix.bilingualreader.view.adapter.library.BookGridCardAdapter
import br.com.fenix.bilingualreader.view.adapter.library.BookLineCardAdapter
import br.com.fenix.bilingualreader.view.adapter.library.BookSeparatorGridCardAdapter
import br.com.fenix.bilingualreader.view.adapter.library.BookSeparatorLineCardAdapter
import br.com.fenix.bilingualreader.view.adapter.library.BookSeriesCardAdapter
import br.com.fenix.bilingualreader.view.components.BlurAwareItemAnimator
import br.com.fenix.bilingualreader.view.components.ComponentsUtil
import br.com.fenix.bilingualreader.view.components.GlassRenderScheduler
import br.com.fenix.bilingualreader.view.ui.detail.DetailActivity
import br.com.fenix.bilingualreader.view.ui.popup.PopupBookMark
import br.com.fenix.bilingualreader.view.ui.popup.PopupTags
import br.com.fenix.bilingualreader.view.ui.reader.book.BookReaderActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import eightbitlab.com.blurview.BlurView
import io.supercharge.shimmerlayout.ShimmerLayout
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


import br.com.fenix.bilingualreader.view.managers.BookLibraryHandler


class BookLibraryFragment : Fragment(), PopupOrderListener, SwipeRefreshLayout.OnRefreshListener, BookLibraryHandler.Listener {

    private val mLOGGER = LoggerFactory.getLogger(BookLibraryFragment::class.java)

    private val uniqueID: String = UUID.randomUUID().toString()

    private lateinit var mViewModel: BookLibraryViewModel
    private lateinit var mainFunctions: MainListener

    private val bookDetailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val index = mViewModel.updateList(itemRefresh ?: 0)
        if (index >= 0) {
            notifyDataSet(index)
        }
    }

    private val driveAuthorizationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingShareMarkNotification?.let { (id, builder) ->
            if (granted)
                NotificationManagerCompat.from(requireContext()).notify(id, builder.build())
            pendingShareMarkNotification = null
        }
    }

    private var pendingShareMarkNotification: kotlin.Pair<Int, NotificationCompat.Builder>? = null

    private var mRoot: FrameLayout by autoCleared()
    private var mRefreshLayout: SwipeRefreshLayout by autoCleared()
    private var _mRecyclerView: RecyclerView? = null
    private val mRecyclerView: RecyclerView get() = _mRecyclerView!!
    private var mLibraryAdapter: BaseAdapter<Book, BookCardListener>? = null
    private lateinit var miGridType: MenuItem
    private lateinit var miGridOrder: MenuItem
    private lateinit var miSearch: MenuItem
    private var _searchView: SearchView? = null
    private val searchView: SearchView get() = _searchView!!
    private lateinit var mListener: BookCardListener
    private var mScrollUp: FloatingActionButton by autoCleared()
    private var mScrollDown: FloatingActionButton by autoCleared()
    private var mMenuPopupLibrary: FrameLayout by autoCleared()
    private var mMenuPopupLibraryBackground: BlurView by autoCleared()
    private var mPopupLibraryView: ViewPager2 by autoCleared()
    private var mPopupLibraryTab: TabLayout by autoCleared()
    private var mPopupFilterFragment: LibraryBookPopupFilter by autoCleared()
    private var mPopupOrderFragment: LibraryBookPopupOrder by autoCleared()
    private var mPopupTypeFragment: LibraryBookPopupType by autoCleared()
    private var _mBottomSheet: BottomSheetBehavior<FrameLayout>? = null
    private val mBottomSheet: BottomSheetBehavior<FrameLayout> get() = _mBottomSheet!!
    private val mBottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (view == null) return
            val ctx = context ?: return
            val sharedPreferences = GeneralConsts.getSharedPreferences(ctx)
            val isGlass = sharedPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
            if (isGlass) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING || newState == BottomSheetBehavior.STATE_SETTLING) {
                    mMenuPopupLibraryBackground.setBlurAutoUpdate(true)
                } else {
                    mMenuPopupLibraryBackground.setBlurAutoUpdate(false)
                }
            }

            val activity = activity ?: return
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                PopupUtils.updateNavigationBarColor(activity, true)
            } else if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                PopupUtils.updateNavigationBarColor(activity, false)
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            if (view == null) return
            val ctx = context ?: return
            val sharedPreferences = GeneralConsts.getSharedPreferences(ctx)
            val isGlass = sharedPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
            if (isGlass) {
                mMenuPopupLibraryBackground.setBlurAutoUpdate(true)
            }
        }
    }

    private var mSkeletonLayout: LinearLayout by autoCleared()
    private var mShimmer: ShimmerLayout by autoCleared()
    private var mInflater: LayoutInflater by autoCleared()

    private var mPopupTag: PopupTags by autoCleared()

    private val mHandler = Handler(Looper.getMainLooper())
    private val mDismissUpButton = Runnable { mScrollUp.hide() }
    private val mDismissDownButton = Runnable { mScrollDown.hide() }

    companion object {
        var mSortType: Order = Order.Name
        var mSortDesc: Boolean = false
        var mGridType: LibraryBookType = LibraryBookType.LINE

        @VisibleForTesting
        fun setMainListener(fragment: BookLibraryFragment, listener: MainListener) {
            fragment.mainFunctions = listener
        }
    }

    private val mUpdateHandler: Handler = BookLibraryHandler(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProvider(requireActivity()).get(BookLibraryViewModel::class.java)
        loadConfig()

        if (!mViewModel.existStack(uniqueID))
            mViewModel.addStackLibrary(uniqueID, mViewModel.getLibrary())
    }

    private fun createLibraryMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.menu_library_book, menu)

        miGridType = menu.findItem(R.id.menu_book_library_type)
        miGridOrder = menu.findItem(R.id.menu_book_library_list_order)
        miSearch = menu.findItem(R.id.menu_book_library_search)

        _searchView = miSearch.actionView as SearchView
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
                    if (selection.endsWith(":")) {
                        searchSrcTextView.post { searchSrcTextView.showDropDown() }
                    }
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
                            if (newText.endsWith(":")) {
                                searchSrcTextView.post { searchSrcTextView.showDropDown() }
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
            LibraryBookType.SEPARATOR_CAROUSEL -> R.drawable.ico_animated_type_grid_carousel_exit
            LibraryBookType.SEPARATOR_LINE -> R.drawable.ico_animated_type_grid_list_separator_exit
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
        if (view != null) {
            setupPopupBackgrounds()

            mViewModel.getLibrary().let {
                if (it.language == Libraries.DEFAULT)
                    mainFunctions.clearLibraryTitle()
                else
                    mainFunctions.changeLibraryTitle(it.title)
            }

            ScannerBook.getInstance(requireContext()).addUpdateHandler(mUpdateHandler)

            if (!mViewModel.isLoading) {
                if (mViewModel.isEmpty())
                    refresh()
                else
                    mViewModel.updateList { change, indexes ->
                        if (change && indexes.isNotEmpty())
                            notifyDataSet(indexes)
                    }
            }

            if (ScannerBook.getInstance(requireContext()).isRunning(mViewModel.getLibrary()))
                setIsRefreshing(true)
            else
                setIsRefreshing(false)

            val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
            val isGlass = sharedPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
            mMenuPopupLibraryBackground.setBlurEnabled(isGlass)
            if (isGlass && !mViewModel.isLoading) {
                mMenuPopupLibraryBackground.blurOnceDeferred(mHandler, 100)
            } else {
                mMenuPopupLibraryBackground.setBlurAutoUpdate(false)
            }
        }
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

    override fun onDestroyView() {
        mViewModel.clearFilter()
        BookCoverCardAdapter.clearCoverCache()
        (mRecyclerView.itemAnimator as? BlurAwareItemAnimator)?.destroy()
        mHandler.removeCallbacksAndMessages(null)
        _searchView = null
        _mRecyclerView = null
        mLibraryAdapter = null
        _mBottomSheet?.removeBottomSheetCallback(mBottomSheetCallback)
        _mBottomSheet = null
        super.onDestroyView()
    }

    override fun onBookAdd(book: Book) {
        refreshLibraryAddDelayed(book)
    }

    override fun onBookRemove(book: Book) {
        refreshLibraryRemoveDelayed(book)
    }

    override fun onBookUpdateFinished(isProcessed: Boolean) {
        setIsRefreshing(false)
        if (isProcessed && ::mViewModel.isInitialized && _mRecyclerView != null) {
            mViewModel.updateList { change, indexes ->
                if (change && _mRecyclerView != null)
                    notifyDataSet(indexes)
            }
        }
    }

    private fun notifyDataSet(indexes: MutableList<kotlin.Pair<ListMode, Int>>) {
        if (_mRecyclerView == null)
            return
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
        if (_mRecyclerView == null)
            return
        if (insert)
            mRecyclerView.adapter?.notifyItemInserted(index)
        else if (removed)
            mRecyclerView.adapter?.notifyItemRemoved(index)
        else if (range > 1)
            mRecyclerView.adapter?.notifyItemRangeChanged(index, range, AnimationUtil.PROPERTY_NO_ANIMATION)
        else
            mRecyclerView.adapter?.notifyItemChanged(index, AnimationUtil.PROPERTY_NO_ANIMATION)
    }

    private fun refreshLibraryAddDelayed(book: Book) {
        val index = mViewModel.addList(book)
        if (index > -1 && _mRecyclerView != null)
            mRecyclerView.adapter?.notifyItemInserted(index)
    }

    private fun refreshLibraryRemoveDelayed(book: Book) {
        val index = mViewModel.remList(book)
        if (index > -1 && _mRecyclerView != null)
            mRecyclerView.adapter?.notifyItemRemoved(index)
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
                LibraryBookType.SEPARATOR_MEDIUM,
                LibraryBookType.SEPARATOR_CAROUSEL,
                LibraryBookType.SEPARATOR_LINE -> updateList(mViewModel.listBook.value!!)
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
        setAnimationRecycler(true)
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
            LibraryBookType.SEPARATOR_CAROUSEL -> R.drawable.ico_animated_type_grid_carousel_exit
            LibraryBookType.SEPARATOR_LINE -> R.drawable.ico_animated_type_grid_list_separator_exit
            LibraryBookType.LINE -> R.drawable.ico_animated_type_grid_list_exit
        }

        val final: Int? = when (type) {
            LibraryBookType.GRID_BIG -> R.drawable.ico_animated_type_grid_gridbig_enter
            LibraryBookType.GRID_MEDIUM -> R.drawable.ico_animated_type_grid_gridmedium_enter
            LibraryBookType.SEPARATOR_BIG -> R.drawable.ico_animated_type_grid_gridbig_separator_enter
            LibraryBookType.SEPARATOR_MEDIUM -> R.drawable.ico_animated_type_grid_gridmedium_separator_enter
            LibraryBookType.SEPARATOR_CAROUSEL -> R.drawable.ico_animated_type_grid_carousel_enter
            LibraryBookType.SEPARATOR_LINE -> R.drawable.ico_animated_type_grid_list_separator_enter
            LibraryBookType.LINE -> R.drawable.ico_animated_type_grid_list_enter
        }

        if (initial != null && final != null)
            MenuUtil.animatedSequenceDrawable(miGridType, initial, final)

        mGridType = type
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_book_library, container, false)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val contentLayout = root.findViewById<View>(R.id.book_library_content)
            contentLayout?.setPadding(contentLayout.paddingLeft, contentLayout.paddingTop, contentLayout.paddingRight, navBarHeight)
            val popupLayout = root.findViewById<View>(R.id.book_library_popup_menu_library)
            popupLayout?.setPadding(popupLayout.paddingLeft, popupLayout.paddingTop, popupLayout.paddingRight, navBarHeight)
            insets
        }
        mRoot = root.findViewById(R.id.frame_book_library_root)
        _mRecyclerView = root.findViewById(R.id.book_library_recycler_view)
        mRefreshLayout = root.findViewById(R.id.book_library_refresh)
        mScrollUp = root.findViewById(R.id.book_library_scroll_up)
        mScrollDown = root.findViewById(R.id.book_library_scroll_down)

        mMenuPopupLibrary = root.findViewById(R.id.book_library_popup_menu_library)
        mMenuPopupLibraryBackground = root.findViewById(R.id.book_library_popup_header_background)
        mRecyclerView.itemAnimator = BlurAwareItemAnimator()
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
        mScrollUp.setOnLongClickListener {
            (mScrollUp.drawable as AnimatedVectorDrawable).start()
            mRecyclerView.scrollToPosition(0)
            true
        }
        mScrollDown.setOnClickListener {
            setAnimationRecycler(false)
            (mScrollDown.drawable as AnimatedVectorDrawable).start()
            mRecyclerView.smoothScrollToPosition((mRecyclerView.adapter as RecyclerView.Adapter).itemCount)
        }
        mScrollDown.setOnLongClickListener {
            (mScrollDown.drawable as AnimatedVectorDrawable).start()
            mRecyclerView.scrollToPosition((mRecyclerView.adapter as RecyclerView.Adapter).itemCount -1)
            true
        }

        mPopupFilterFragment = LibraryBookPopupFilter()
        mPopupOrderFragment = LibraryBookPopupOrder()
        mPopupTypeFragment = LibraryBookPopupType()

        mPopupOrderFragment.setListener(this)

        BottomSheetBehavior.from(mMenuPopupLibrary).apply {
            peekHeight = 255
            this.state = BottomSheetBehavior.STATE_COLLAPSED
            _mBottomSheet = this
        }
        mBottomSheet.isDraggable = true

        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
        mBottomSheet.addBottomSheetCallback(mBottomSheetCallback)

        PopupUtils.onPopupTouch(requireActivity(), mMenuPopupLibrary, mBottomSheet, root.findViewById<ImageView>(R.id.book_library_popup_menu_order_filter_touch))

        val viewFilterOrderPagerAdapter = ViewPagerAdapter(this)
        viewFilterOrderPagerAdapter.addFragment(
            mPopupTypeFragment,
            resources.getString(R.string.popup_library_book_tab_item_type)
        )
        viewFilterOrderPagerAdapter.addFragment(
            mPopupOrderFragment,
            resources.getString(R.string.popup_library_book_tab_item_ordering)
        )
        viewFilterOrderPagerAdapter.addFragment(
            mPopupFilterFragment,
            resources.getString(R.string.popup_library_book_tab_item_filter)
        )

        mPopupLibraryView.adapter = viewFilterOrderPagerAdapter
        TabLayoutMediator(mPopupLibraryTab, mPopupLibraryView) { tab, position ->
            tab.text = viewFilterOrderPagerAdapter.getPageTitle(position)
        }.attach()

        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState != AbsListView.OnScrollListener.SCROLL_STATE_FLING)
                    setAnimationRecycler(true)

                val isGlass = sharedPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
                val isPopupVisible = _mBottomSheet != null && mBottomSheet.state != BottomSheetBehavior.STATE_HIDDEN
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    (activity as? br.com.fenix.bilingualreader.MainActivity)?.setBlurAutoUpdate(false)
                    if (isGlass) {
                        mMenuPopupLibraryBackground.setBlurAutoUpdate(false)
                        GlassRenderScheduler.requestUpdate(mMenuPopupLibraryBackground)
                        (activity as? br.com.fenix.bilingualreader.MainActivity)?.blurOnceDeferred(50)
                    }
                } else {
                    (activity as? br.com.fenix.bilingualreader.MainActivity)?.setBlurAutoUpdate(true)
                    if (isGlass && isPopupVisible) {
                        mMenuPopupLibraryBackground.setBlurAutoUpdate(true)
                    }
                    if (isGlass) {
                        GlassRenderScheduler.setScrollRateCap(mMenuPopupLibraryBackground, true)
                    }
                }
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

                    val type = mViewModel.libraryType.value
                    if (type == LibraryBookType.SEPARATOR_CAROUSEL) {
                        GlassRenderScheduler.suspendFor(400L, "activityTransition")
                        context?.startActivity(intent)
                        requireActivity().overrideActivityTransitionCompat(R.anim.fade_in_fragment_add_enter, R.anim.fade_out_fragment_remove_exit)
                    } else {
                        val isLine = type == LibraryBookType.LINE || type == LibraryBookType.SEPARATOR_LINE
                        val idText = if (isLine) R.id.book_line_title else R.id.book_grid_title
                        val idAuthor = if (isLine) R.id.book_line_author else R.id.book_grid_sub_title
                        val idProgress = if (isLine) R.id.book_line_progress else R.id.book_grid_progress
                        val idCover = if (isLine) R.id.book_line_image_cover else R.id.book_grid_image_cover

                        val pImageCover: Pair<View, String> = Pair(root.findViewById<ImageView>(idCover), "transition_book_cover")
                        val pTitle: Pair<View, String> = Pair(root.findViewById<TextView>(idText), "transition_book_title")
                        val pAuthor: Pair<View, String> = Pair(root.findViewById<TextView>(idAuthor), "transition_book_author")
                        val pProgress: Pair<View, String> = Pair(root.findViewById<ProgressBar>(idProgress), "transition_progress_bar")

                        val options = ActivityOptions.makeSceneTransitionAnimation(requireActivity(), *arrayOf(pImageCover, pTitle, pAuthor, pProgress))

                        GlassRenderScheduler.suspendFor(400L, "activityTransition")
                        context?.startActivity(intent, options.toBundle())
                        requireActivity().overrideActivityTransitionCompat(R.anim.fade_in_fragment_add_enter, R.anim.fade_out_fragment_remove_exit)
                    }
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
            if (it && _mRecyclerView != null)
                sortList()
            mViewModel.isLoading = false
        }

        if (!Storage.isPermissionGranted(requireContext()))
            Storage.takePermission(requireContext(), requireActivity())

        generateLayout(mViewModel.libraryType.value!!)
        setAnimationRecycler(true)
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
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        mainFunctions.clearLibraryTitle()
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                createLibraryMenu(menu, menuInflater)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_book_library_type -> {
                        mViewModel.changeLibraryType()
                        true
                    }
                    R.id.menu_book_library_list_order -> {
                        onChangeSort()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
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

        val type = mViewModel.libraryType.value
        if (type == LibraryBookType.SEPARATOR_CAROUSEL) {
            GlassRenderScheduler.suspendFor(400L, "activityTransition")
            requireActivity().overrideActivityTransitionCompat(R.anim.fade_in_fragment_add_enter, R.anim.fade_out_fragment_remove_exit)
            bookDetailLauncher.launch(intent)
            return
        }

        val isLine = type == LibraryBookType.LINE || type == LibraryBookType.SEPARATOR_LINE
        val idText = if (isLine) R.id.book_line_title else R.id.book_grid_title
        val idAuthor = if (isLine) R.id.book_line_author else R.id.book_grid_sub_title
        val idProgress = if (isLine) R.id.book_line_progress else R.id.book_grid_progress
        val idCover = if (isLine) R.id.book_line_image_cover else R.id.book_grid_image_cover

        val pImageCover = androidx.core.util.Pair.create(view.findViewById<ImageView>(idCover) as View, "transition_book_cover")
        val pTitle = androidx.core.util.Pair.create(view.findViewById<TextView>(idText) as View, "transition_book_title")
        val pAuthor = androidx.core.util.Pair.create(view.findViewById<TextView>(idAuthor) as View, "transition_book_author")
        val pProgress = androidx.core.util.Pair.create(view.findViewById<ProgressBar>(idProgress) as View, "transition_progress_bar")

        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), pImageCover, pTitle, pAuthor, pProgress)
        GlassRenderScheduler.suspendFor(400L, "activityTransition")
        requireActivity().overrideActivityTransitionCompat(R.anim.fade_in_fragment_add_enter, R.anim.fade_out_fragment_remove_exit)
        bookDetailLauncher.launch(intent, options)
    }

    private fun loadConfig() {
        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
        mSortType = try {
            Order.valueOf(sharedPreferences.getString(GeneralConsts.KEYS.LIBRARY.BOOK_ORDER, Order.Name.toString()).toString())
        } catch (e: Exception) {
            Order.Name
        }

        mGridType = try {
            LibraryBookType.valueOf(
                sharedPreferences.getString(
                    GeneralConsts.KEYS.LIBRARY.BOOK_LIBRARY_TYPE,
                    LibraryBookType.LINE.toString()
                ).toString()
            )
        } catch (e: Exception) {
            LibraryBookType.LINE
        }
        mViewModel.setLibraryType(mGridType)
        mViewModel.sorted(mSortType)
    }

    private fun getGridLayout(): RecyclerView.LayoutManager {
        val type = mViewModel.libraryType.value
        val typeWidth = when (type) {
            LibraryBookType.SEPARATOR_MEDIUM -> LibraryBookType.GRID_MEDIUM
            LibraryBookType.SEPARATOR_BIG -> LibraryBookType.GRID_BIG
            else -> type
        }
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val columnWidth: Int = AdapterUtils.getBookCardSize(requireContext(), typeWidth!!, isLandscape).first + 1
        val spaceCount: Int = max(1, (Resources.getSystem().displayMetrics.widthPixels -3) / columnWidth)
        return when (type) {
            LibraryBookType.SEPARATOR_BIG,
            LibraryBookType.SEPARATOR_MEDIUM -> StaggeredGridLayoutManager(spaceCount, StaggeredGridLayoutManager.VERTICAL)
            else -> GridLayoutManager(requireContext(), spaceCount)
        }
    }

    private fun generateLayout(type: LibraryBookType) {
        when (type) {
            LibraryBookType.LINE -> {
                val lineAdapter = BookLineCardAdapter()
                mLibraryAdapter = lineAdapter
                mRecyclerView.adapter = lineAdapter
                mRecyclerView.layoutManager = GridLayoutManager(requireContext(), 1)
                lineAdapter.attachListener(mListener)
                mRecyclerView.layoutAnimation = null
            }
            LibraryBookType.SEPARATOR_LINE -> {
                val lineAdapter = BookSeparatorLineCardAdapter(requireContext())
                mLibraryAdapter = lineAdapter
                mRecyclerView.adapter = lineAdapter
                mRecyclerView.layoutManager = GridLayoutManager(requireContext(), 1)
                lineAdapter.attachListener(mListener)
                mRecyclerView.layoutAnimation = null
            }
            LibraryBookType.SEPARATOR_CAROUSEL -> {
                val seriesAdapter = BookSeriesCardAdapter(requireContext())
                mLibraryAdapter = seriesAdapter
                mRecyclerView.adapter = seriesAdapter
                mRecyclerView.layoutManager = GridLayoutManager(requireContext(), 1)
                seriesAdapter.attachListener(mListener)
                mRecyclerView.layoutAnimation = null
            }
            else -> {
                val gridAdapter = when (type) {
                    LibraryBookType.SEPARATOR_BIG,
                    LibraryBookType.SEPARATOR_MEDIUM -> BookSeparatorGridCardAdapter(requireContext(), type)
                    else -> BookGridCardAdapter(type)
                }
                mLibraryAdapter = gridAdapter
                mRecyclerView.adapter = gridAdapter
                mRecyclerView.layoutManager = getGridLayout()
                gridAdapter.attachListener(mListener)
                mRecyclerView.layoutAnimation = null
            }
        }
    }

    private fun setAnimationRecycler(isAnimate: Boolean) {
        mLibraryAdapter?.isAnimation = isAnimate
    }

    private fun removeList(book: Book) {
        mLibraryAdapter?.removeList(book)
    }

    private fun updateList(list: MutableList<Book>) {
        mLibraryAdapter?.updateList(mSortType, list)
    }

    private fun observer() {
        mViewModel.loading.observe(viewLifecycleOwner) {
            if (!it) {
                animateReplaceSkeleton()
                (activity as? br.com.fenix.bilingualreader.MainActivity)?.blurOnceDeferred(300)
            } else
                showSkeleton(it)
        }

        mViewModel.listBook.observe(viewLifecycleOwner) {
            updateList(it)
        }
        mViewModel.libraryType.observe(viewLifecycleOwner) {
            onChangeLayout(it)

            if (mViewModel.loading.value == true)
                showSkeleton(true)
        }
    }

    fun setIsRefreshing(enabled: Boolean) {
        try {
            mRefreshLayout.isRefreshing = enabled

            if (_searchView == null || _mRecyclerView == null)
                return

            if (enabled)
                searchView.clearFocus()
            enableSearchView(searchView, !enabled)
        } catch (e: Exception) {
            mLOGGER.error("Disable search button error: " + e.message, e)
            Telemetry.recordException(e, "Disable search button error: " + e.message)
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
                    pendingShareMarkNotification = notifyId to notification
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
                            driveAuthorizationLauncher.launch(shareMark.intent!!)
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
            LibraryBookType.SEPARATOR_MEDIUM,
            LibraryBookType.SEPARATOR_CAROUSEL,
            LibraryBookType.SEPARATOR_LINE -> updateList(mViewModel.listBook.value!!)
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

        override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: ViewHolder): Int {
            if (mViewModel.libraryType.value == LibraryBookType.SEPARATOR_CAROUSEL)
                return 0
            if (viewHolder.itemViewType == 1) { // 1 is HEADER in separator adapters
                return 0
            }
            return super.getSwipeDirs(recyclerView, viewHolder)
        }

        override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
            val position = viewHolder.bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return
            val adapter = mLibraryAdapter ?: return
            val book = adapter.getItem(position) ?: return
            mRecyclerView.post {
                mViewModel.remove(book)
                mRecyclerView.adapter?.notifyItemRemoved(position)
                deleteBook(book, position, swiped = true)
            }
        }

        override fun onSelectedChanged(viewHolder: ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE)
                mRefreshLayout.setEnabled(false)
            else
                mRefreshLayout.setEnabled(true)
        }
    }

    private fun deleteBook(book: Book, position: Int, swiped: Boolean = false) {
        var excluded = false
        val dialog: AlertDialog = MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
                .setTitle(getString(R.string.book_library_menu_delete))
                .setMessage(getString(R.string.book_library_menu_delete_description) + "\n" + book.file.name)
                .setPositiveButton(R.string.action_delete) { _, _ ->
                    deleteFile(book, swiped)
                    excluded = true
                }.setOnDismissListener {
                    if (!excluded && swiped) {
                        mViewModel.add(book, position)
                        mRecyclerView.adapter?.notifyItemInserted(position)
                    }
                }
                .create()
        dialog.show()
    }

    private fun deleteFile(book: Book?, swiped: Boolean = false) {
        if (book?.file != null) {
            if (!swiped) {
                removeList(book)
                mViewModel.delete(book)
            } else {
                mViewModel.delete(book)
            }
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
            mLOGGER.error("Error share book: " + e.message, e)
            Telemetry.recordException(e, "Error share book: " + e.message)
        }
    }

    inner class ViewPagerAdapter(fragment: Fragment) :
        FragmentStateAdapter(fragment) {
        private val fragments: MutableList<Fragment> = ArrayList()
        private val fragmentTitle: MutableList<String> = ArrayList()
        fun addFragment(fragment: Fragment, title: String) {
            fragments.add(fragment)
            fragmentTitle.add(title)
        }

        fun getItem(position: Int): Fragment {
            return fragments[position]
        }

        override fun createFragment(position: Int): Fragment {
            return fragments[position]
        }

        override fun getItemCount(): Int {
            return fragments.size
        }

        fun getPageTitle(position: Int): CharSequence {
            return fragmentTitle[position]
        }
    }

    private fun getSkeletonTitleHeight(): Int =
        resources.getDimension(R.dimen.book_grid_skeleton_title_height).toInt()

    private fun getSkeletonContentRowHeight(type: LibraryBookType): Int {
        val resource = when (type) {
            LibraryBookType.LINE,
            LibraryBookType.SEPARATOR_LINE -> R.dimen.book_line_skeleton_height
            LibraryBookType.SEPARATOR_CAROUSEL -> R.dimen.book_carousel_skeleton_height
            LibraryBookType.SEPARATOR_BIG -> R.dimen.book_grid_skeleton_height_separator_big
            LibraryBookType.SEPARATOR_MEDIUM -> R.dimen.book_grid_skeleton_height_separator_medium
            LibraryBookType.GRID_BIG -> R.dimen.book_grid_skeleton_height_big
            LibraryBookType.GRID_MEDIUM -> R.dimen.book_grid_skeleton_height_medium
        }
        return resources.getDimension(resource).toInt()
    }

    private fun getSkeletonRowCount(type: LibraryBookType): Int {
        val pxHeight = Resources.getSystem().displayMetrics.heightPixels
        val rowHeight = getSkeletonContentRowHeight(type)
        return max(1, ceil((pxHeight / rowHeight.toDouble())).toInt())
    }

    private fun getSkeletonGroupCount(type: LibraryBookType, contentRowsPerGroup: Int = 1): Int {
        val pxHeight = Resources.getSystem().displayMetrics.heightPixels
        val groupHeight = getSkeletonTitleHeight() + (getSkeletonContentRowHeight(type) * contentRowsPerGroup)
        return max(2, min(3, ceil((pxHeight / groupHeight.toDouble())).toInt()))
    }

    private fun getSkeletonCarouselItemPerRow(): Int {
        val itemWidth = resources.getDimension(R.dimen.book_carousel_skeleton_item_width).toInt()
        val margin = resources.getDimension(R.dimen.book_carousel_skeleton_item_margin).toInt()
        return max(1, Resources.getSystem().displayMetrics.widthPixels / (itemWidth + margin))
    }

    private fun addCarouselSkeletonRow(itemCount: Int) {
        val row = mInflater.inflate(R.layout.line_card_book_carousel_skeleton, null)
        val container = row.findViewById<LinearLayout>(R.id.carousel_skeleton_items)
        val width = resources.getDimension(R.dimen.book_carousel_skeleton_item_width).toInt()
        val height = resources.getDimension(R.dimen.book_carousel_skeleton_item_height).toInt()
        val margin = resources.getDimension(R.dimen.book_carousel_skeleton_item_margin).toInt()
        container.removeAllViews()
        for (idx in 0 until itemCount) {
            val item = mInflater.inflate(R.layout.line_card_book_carousel_skeleton_item, null)
            val params = LinearLayout.LayoutParams(width, height)
            params.marginEnd = margin
            item.layoutParams = params
            container.addView(item)
        }
        mSkeletonLayout.addView(row)
    }

    private fun getSkeletonGridItemPerRow(type: LibraryBookType): Int {
        val typeWidth = when (type) {
            LibraryBookType.SEPARATOR_MEDIUM -> LibraryBookType.GRID_MEDIUM
            LibraryBookType.SEPARATOR_BIG -> LibraryBookType.GRID_BIG
            else -> type
        }
        val columnWidth = getSkeletonItemWidth(typeWidth) + 1
        return max(1, (Resources.getSystem().displayMetrics.widthPixels - 3) / columnWidth.toInt())
    }

    private fun getSkeletonItemHeight(type: LibraryBookType): Int {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        return AdapterUtils.getBookCardSize(requireContext(), type, isLandscape).second
    }

    private fun getSkeletonItemWidth(type: LibraryBookType): Int {
        val typeWidth = when (type) {
            LibraryBookType.SEPARATOR_MEDIUM -> LibraryBookType.GRID_MEDIUM
            LibraryBookType.SEPARATOR_BIG -> LibraryBookType.GRID_BIG
            else -> type
        }
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        return AdapterUtils.getBookCardSize(requireContext(), typeWidth, isLandscape).first
    }

    private fun addGridSkeletonRow(type: LibraryBookType) {
        val row = mInflater.inflate(R.layout.grid_card_book_skeleton, null)
        val container = row.findViewById<LinearLayout>(R.id.grid_skeleton_items)
        val height = getSkeletonItemHeight(type)
        val width = getSkeletonItemWidth(type)
        val margin = resources.getDimension(R.dimen.book_grid_skeleton_divider).toInt()
        val items = getSkeletonGridItemPerRow(type)
        val divider = ((Resources.getSystem().displayMetrics.widthPixels.toFloat() - (items * (width + margin))) / items).toInt()
        container.removeAllViews()
        for (idx in 0..items) {
            val item = mInflater.inflate(R.layout.grid_card_book_skeleton_item, null)
            val params = FrameLayout.LayoutParams(width, height)
            params.setMargins(margin, margin, divider, 0)
            item.layoutParams = params
            container.addView(item)
        }
        container.invalidate()
        mSkeletonLayout.addView(row)
    }

    private fun addBookSkeletonTitle() {
        mSkeletonLayout.addView(mInflater.inflate(R.layout.grid_card_book_skeleton_title, null))
    }

    private fun showSkeleton(show: Boolean) {
        if (view == null) return
        if (show) {
            mSkeletonLayout.alpha = 1f
            mSkeletonLayout.removeAllViews()

            val type = mViewModel.libraryType.value ?: LibraryBookType.LINE

            when (type) {
                LibraryBookType.LINE -> {
                    for (i in 0 until getSkeletonRowCount(type))
                        mSkeletonLayout.addView(mInflater.inflate(R.layout.line_card_book_skeleton, null))
                }
                LibraryBookType.SEPARATOR_LINE -> {
                    val lineItemsPerGroup = 2
                    val groups = getSkeletonGroupCount(type, lineItemsPerGroup)
                    repeat(groups) {
                        addBookSkeletonTitle()
                        repeat(lineItemsPerGroup) {
                            mSkeletonLayout.addView(mInflater.inflate(R.layout.line_card_book_skeleton, null))
                        }
                    }
                }
                LibraryBookType.SEPARATOR_CAROUSEL -> {
                    val full = getSkeletonCarouselItemPerRow()
                    val counts = listOf(full, max(1, full - 1), max(1, full - 2))
                    val groups = getSkeletonGroupCount(type)
                    for (i in 0 until groups) {
                        addBookSkeletonTitle()
                        addCarouselSkeletonRow(counts[i])
                    }
                }
                LibraryBookType.SEPARATOR_BIG,
                LibraryBookType.SEPARATOR_MEDIUM -> {
                    val groups = getSkeletonGroupCount(type)
                    repeat(groups) {
                        addBookSkeletonTitle()
                        addGridSkeletonRow(type)
                    }
                }
                else -> {
                    for (i in 0 until getSkeletonRowCount(type))
                        addGridSkeletonRow(type)
                }
            }

            mRecyclerView.animate().cancel()
            mSkeletonLayout.animate().cancel()

            mShimmer.visibility = View.VISIBLE
            mRecyclerView.visibility = View.GONE
            mRecyclerView.alpha = 1f
            mSkeletonLayout.visibility = View.VISIBLE
            mShimmer.startShimmerAnimation()
            mSkeletonLayout.bringToFront()
        } else {
            mShimmer.stopShimmerAnimation()
            mShimmer.visibility = View.GONE
            mSkeletonLayout.visibility = View.GONE
            mSkeletonLayout.alpha = 1f
            mRecyclerView.visibility = View.VISIBLE
            mRecyclerView.alpha = 1f
            setAnimationRecycler(true)
        }

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        MenuUtil.longClick(requireActivity(), R.id.menu_book_library_list_order) {
            if (!mRefreshLayout.isRefreshing)
                onOpenMenuLibrary(1)
        }

        MenuUtil.longClick(requireActivity(), R.id.menu_book_library_type) {
            if (!mRefreshLayout.isRefreshing)
                onOpenMenuLibrary(0)
        }

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            val type = mViewModel.libraryType.value
            if (type != LibraryBookType.LINE && type != LibraryBookType.SEPARATOR_LINE && type != LibraryBookType.SEPARATOR_CAROUSEL) {
                mRecyclerView.layoutManager = getGridLayout()
                mRecyclerView.adapter?.notifyItemRangeChanged(0, mRecyclerView.adapter?.itemCount ?: 0)
            }
        }

        setupPopupBackgrounds()
    }

    override fun onPause() {
        super.onPause()
        if (_mBottomSheet != null && mBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        mMenuPopupLibraryBackground.setBlurAutoUpdate(false)
        mMenuPopupLibraryBackground.setBlurEnabled(false)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        val isGlass = GeneralConsts.getSharedPreferences(requireContext()).getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
        if (hidden) {
            if (_mBottomSheet != null && mBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
                mBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            }
            mMenuPopupLibraryBackground.setBlurAutoUpdate(false)
            mMenuPopupLibraryBackground.setBlurEnabled(false)
        } else {
            mMenuPopupLibraryBackground.setBlurEnabled(isGlass)
            if (isGlass) {
                mMenuPopupLibraryBackground.setBlurAutoUpdate(true)
                mHandler.postDelayed({
                    mMenuPopupLibraryBackground.setBlurAutoUpdate(false)
                }, 100)
            } else {
                mMenuPopupLibraryBackground.setBlurAutoUpdate(false)
            }
        }
    }
    private fun setupPopupBackgrounds() {
        val activity = activity ?: return
        val contentContainer = view?.findViewById<ViewGroup>(R.id.book_library_content)
        PopupUtils.setupPopupBackgrounds(activity, mMenuPopupLibrary, mMenuPopupLibraryBackground, contentContainer)
    }

    private fun animateReplaceSkeleton() {
        setAnimationRecycler(false)
        GlassRenderScheduler.suspendFor(300L, "skeleton")
        mRecyclerView.visibility = View.VISIBLE
        mRecyclerView.alpha = 0f
        mRecyclerView.animate().alpha(1f).setDuration(700).start()
        mSkeletonLayout.animate().alpha(0f).setDuration(1000).withEndAction {
            showSkeleton(false)
            GlassRenderScheduler.requestUpdateAll()
        }.start()
    }

}

private class AutoClearedValueBook<T : Any>(val fragment: Fragment) : ReadWriteProperty<Fragment, T> {
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

private fun <T : Any> Fragment.autoCleared() = AutoClearedValueBook<T>(this)
