package br.com.fenix.bilingualreader.view.ui.history

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Intent
import android.content.SharedPreferences
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
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.SearchView
import android.widget.SimpleCursorAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewpager.widget.ViewPager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.HistoryType
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.service.listener.HistoryCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.AdapterUtil.AdapterUtils
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.PopupUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.util.helpers.blurOnceDeferred
import br.com.fenix.bilingualreader.view.adapter.history.HistoryBaseAdapter
import br.com.fenix.bilingualreader.view.adapter.history.HistoryCoverCardAdapter
import br.com.fenix.bilingualreader.view.adapter.history.HistoryLineCardAdapter
import br.com.fenix.bilingualreader.view.adapter.history.HistorySeparatorGridCardAdapter
import br.com.fenix.bilingualreader.view.adapter.history.HistorySeriesCardAdapter
import br.com.fenix.bilingualreader.view.components.BlurAwareItemAnimator
import br.com.fenix.bilingualreader.view.ui.reader.book.BookReaderActivity
import br.com.fenix.bilingualreader.view.ui.reader.manga.MangaReaderActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import eightbitlab.com.blurview.BlurView
import io.supercharge.shimmerlayout.ShimmerLayout
import java.time.LocalDateTime
import kotlin.math.ceil
import kotlin.math.max
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


class HistoryFragment : Fragment() {

    private lateinit var mPreferences: SharedPreferences
    private lateinit var mViewModel: HistoryViewModel
    private var mRecyclerView: RecyclerView by autoCleared()
    private var mScrollUp: FloatingActionButton by autoCleared()
    private var mScrollDown: FloatingActionButton by autoCleared()
    private lateinit var miSearch: MenuItem
    private var searchView: SearchView by autoCleared()
    private lateinit var miFilterType: MenuItem
    private lateinit var miLayoutType: MenuItem

    private var mSkeletonLayout: LinearLayout by autoCleared()
    private var mShimmer: ShimmerLayout by autoCleared()
    private var mInflater: LayoutInflater by autoCleared()

    private var mFilterType: Type? = null
    private var mRoot: FrameLayout by autoCleared()
    private var mMenuPopupHistory: FrameLayout by autoCleared()
    private var mMenuPopupHistoryBackground: BlurView by autoCleared()
    private var mPopupHistoryView: ViewPager by autoCleared()
    private var mPopupHistoryTab: TabLayout by autoCleared()
    private var mPopupLibrariesFragment: HistoryPopupLibraries by autoCleared()
    private var mPopupOrderFragment: HistoryPopupOrder by autoCleared()
    private var mPopupTypeFragment: HistoryPopupType by autoCleared()
    private var mPopupContentTypeFragment: HistoryPopupContentType by autoCleared()
    private var mPopupYearFragment: HistoryPopupYear by autoCleared()
    private lateinit var miGridOrder: MenuItem
    private var mSortType: Order = Order.LastAccess
    private var mSortDesc: Boolean = false
    private var mHistoryType: HistoryType = HistoryType.SEPARATOR_LINE
    private lateinit var mListener: HistoryCardListener
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
                    mMenuPopupHistoryBackground.setBlurAutoUpdate(true)
                } else {
                    mMenuPopupHistoryBackground.setBlurAutoUpdate(false)
                }
            }

            val activity = activity ?: return
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                PopupUtil.PopupUtils.updateNavigationBarColor(activity, true)
            } else if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                PopupUtil.PopupUtils.updateNavigationBarColor(activity, false)
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            if (view == null) return
            val ctx = context ?: return
            val sharedPreferences = GeneralConsts.getSharedPreferences(ctx)
            val isGlass = sharedPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
            if (isGlass) {
                mMenuPopupHistoryBackground.setBlurAutoUpdate(true)
            }
        }
    }

    private val mHandler = Handler(Looper.getMainLooper())
    private val mDismissUpButton = Runnable { mScrollUp.hide() }
    private val mDismissDownButton = Runnable { mScrollDown.hide() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        mPreferences = GeneralConsts.getSharedPreferences(requireContext())
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

        MenuUtil.longClick(requireActivity(), R.id.menu_history_library) {
            onOpenMenuHistory(3)
        }

        val manga = miLibrary.subMenu?.addSubMenu(Menu.NONE, Menu.NONE, 102,requireContext().getString(R.string.history_manga))
        val book = miLibrary.subMenu?.addSubMenu(Menu.NONE, Menu.NONE, 103,requireContext().getString(R.string.history_book))
        for (library in mViewModel.libraries.value ?: emptyList())
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

        MenuUtil.longClick(requireActivity(), R.id.menu_history_type) {
            onOpenMenuHistory(2)
        }

        val iconType: Int = if (mFilterType == null)
            R.drawable.ico_menu_type_all
        else when (mFilterType) {
            Type.MANGA -> R.drawable.ico_menu_type_manga
            Type.BOOK -> R.drawable.ico_menu_type_book
            else -> R.drawable.ico_menu_type_all
        }
        miFilterType.setIcon(iconType)

        miGridOrder = menu.findItem(R.id.menu_history_list_order)
        val currentOrder = mViewModel.order.value ?: Pair(Order.LastAccess, false)
        mSortType = currentOrder.first
        mSortDesc = currentOrder.second
        val iconSort: Int = when (mSortType) {
            Order.Name, Order.Series -> if (mSortDesc) R.drawable.ico_animated_sort_to_desc_name else R.drawable.ico_animated_sort_to_asc_name
            Order.Favorite -> if (mSortDesc) R.drawable.ico_animated_sort_to_desc_favorited else R.drawable.ico_animated_sort_to_asc_favorited
            Order.Author -> if (mSortDesc) R.drawable.ico_animated_sort_to_desc_author else R.drawable.ico_animated_sort_to_asc_author
            Order.Genre -> if (mSortDesc) R.drawable.ico_animated_sort_to_desc_tag else R.drawable.ico_animated_sort_to_asc_tag
            else -> if (mSortDesc) R.drawable.ico_animated_sort_to_desc_last_access else R.drawable.ico_animated_sort_to_asc_last_access
        }
        miGridOrder.setIcon(iconSort)

        miLayoutType = menu.findItem(R.id.menu_history_type_layout)
        mHistoryType = mViewModel.historyType.value ?: HistoryType.SEPARATOR_LINE
        miLayoutType.setIcon(getLayoutIcon(mHistoryType, exit = true))

        MenuUtil.longClick(requireActivity(), R.id.menu_history_list_order) {
            onOpenMenuHistory(1)
        }

        MenuUtil.longClick(requireActivity(), R.id.menu_history_type_layout) {
            onOpenMenuHistory(0)
        }

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
            R.id.menu_history_list_order -> onChangeSort()
            R.id.menu_history_type_layout -> mViewModel.changeHistoryType()
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
        mRoot = root as FrameLayout
        mRecyclerView = root.findViewById(R.id.history_statistics_list)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            mRecyclerView.setPadding(mRecyclerView.paddingLeft, mRecyclerView.paddingTop, mRecyclerView.paddingRight, navBarHeight)
            insets
        }

        mScrollUp = root.findViewById(R.id.history_statistics_scroll_up)
        mScrollDown = root.findViewById(R.id.history_statistics_scroll_down)

        mSkeletonLayout = root.findViewById(R.id.skeleton_layout)
        mShimmer = root.findViewById(R.id.shimmer_skeleton)
        mInflater = inflater

        mMenuPopupHistory = root.findViewById(R.id.history_popup_menu)
        mMenuPopupHistoryBackground = root.findViewById(R.id.history_popup_header_background)
        mRecyclerView.itemAnimator = BlurAwareItemAnimator(listOf(mMenuPopupHistoryBackground))
        mPopupHistoryTab = root.findViewById(R.id.history_popup_tab)
        mPopupHistoryView = root.findViewById(R.id.history_popup_view_pager)

        mPopupHistoryTab.setupWithViewPager(mPopupHistoryView)
        mPopupTypeFragment = HistoryPopupType()
        mPopupOrderFragment = HistoryPopupOrder()
        mPopupContentTypeFragment = HistoryPopupContentType()
        mPopupLibrariesFragment = HistoryPopupLibraries()
        mPopupYearFragment = HistoryPopupYear()

        BottomSheetBehavior.from(mMenuPopupHistory).apply {
            peekHeight = 255
            this.state = BottomSheetBehavior.STATE_COLLAPSED
            _mBottomSheet = this
        }
        mBottomSheet.isDraggable = true
        mBottomSheet.addBottomSheetCallback(mBottomSheetCallback)

        PopupUtil.onPopupTouch(requireActivity(), mMenuPopupHistory, mBottomSheet, root.findViewById<View>(R.id.history_popup_menu_order_filter_touch))

        val viewFilterOrderPagerAdapter = ViewPagerAdapter(childFragmentManager, 0)
        viewFilterOrderPagerAdapter.addFragment(
            mPopupTypeFragment,
            resources.getString(R.string.popup_history_tab_type)
        )
        viewFilterOrderPagerAdapter.addFragment(
            mPopupOrderFragment,
            resources.getString(R.string.popup_library_manga_tab_item_ordering)
        )
        viewFilterOrderPagerAdapter.addFragment(
            mPopupContentTypeFragment,
            resources.getString(R.string.popup_history_tab_content)
        )
        viewFilterOrderPagerAdapter.addFragment(
            mPopupLibrariesFragment,
            resources.getString(R.string.config_title_libraries)
        )
        viewFilterOrderPagerAdapter.addFragment(
            mPopupYearFragment,
            resources.getString(R.string.popup_history_tab_year)
        )
        mPopupHistoryView.adapter = viewFilterOrderPagerAdapter

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mRecyclerView)
        observer()
        return root
    }

    inner class ViewPagerAdapter(fm: FragmentManager, behavior: Int) :
        FragmentPagerAdapter(fm, behavior) {
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

    fun onOpenMenuHistory(tab: Int) {
        if (_mBottomSheet == null)
            return

        if (mBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED && mPopupHistoryView.currentItem == tab) {
            mBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            return
        }

        mPopupHistoryView.currentItem = tab
        mMenuPopupHistory.visibility = View.VISIBLE
        mBottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
        val isGlass = mPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
        if (isGlass) {
            mMenuPopupHistoryBackground.blurOnceDeferred(mHandler, 100)
        }
    }


    private fun setupPopupBackgrounds() {
        val activity = activity ?: return
        val contentContainer = view?.findViewById<ViewGroup>(R.id.history_content) ?: mRoot
        PopupUtil.setupPopupBackgrounds(activity, mMenuPopupHistory, mMenuPopupHistoryBackground, contentContainer)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        MenuUtil.longClick(requireActivity(), R.id.menu_history_library) {
            onOpenMenuHistory(3)
        }
        MenuUtil.longClick(requireActivity(), R.id.menu_history_list_order) {
            onOpenMenuHistory(1)
        }
        MenuUtil.longClick(requireActivity(), R.id.menu_history_type_layout) {
            onOpenMenuHistory(0)
        }
        MenuUtil.longClick(requireActivity(), R.id.menu_history_type) {
            onOpenMenuHistory(2)
        }
        if (mHistoryType == HistoryType.SEPARATOR_BIG || mHistoryType == HistoryType.SEPARATOR_MEDIUM)
            generateLayout(mHistoryType)
        setupPopupBackgrounds()
    }

    private var itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                if (mHistoryType == HistoryType.SEPARATOR_CAROUSEL ||
                    mHistoryType == HistoryType.SEPARATOR_BIG ||
                    mHistoryType == HistoryType.SEPARATOR_MEDIUM
                )
                    return 0
                if (viewHolder.itemViewType == 1) // HEADER
                    return 0
                return super.getSwipeDirs(recyclerView, viewHolder)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return
                val adapter = mRecyclerView.adapter as? HistoryBaseAdapter ?: return
                val history = adapter.getItem(position) ?: return
                mRecyclerView.post {
                    mViewModel.remove(history)
                    updateList(mViewModel.history.value ?: arrayListOf())

                    var excluded = false
                    val dialog: AlertDialog =
                        MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
                            .setTitle(getString(R.string.manga_library_menu_delete))
                            .setMessage(getString(R.string.history_delete_description) + "\n" + history.name)
                            .setPositiveButton(
                                R.string.action_delete
                            ) { _, _ ->
                                mViewModel.deletePermanent(history)
                                excluded = true
                            }.setOnDismissListener {
                                if (!excluded) {
                                    mViewModel.add(history)
                                    updateList(mViewModel.history.value ?: arrayListOf())
                                }
                            }
                            .create()
                    dialog.show()
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mListener = object : HistoryCardListener {
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
        generateLayout(mViewModel.historyType.value ?: HistoryType.SEPARATOR_LINE)

        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState != AbsListView.OnScrollListener.SCROLL_STATE_FLING)
                    setAnimationRecycler(true)

                val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
                val isGlass = sharedPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
                val isPopupVisible = _mBottomSheet != null && mBottomSheet.state != BottomSheetBehavior.STATE_HIDDEN
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    (activity as? br.com.fenix.bilingualreader.MainActivity)?.setBlurAutoUpdate(false)
                    if (isGlass) {
                        mMenuPopupHistoryBackground.setBlurAutoUpdate(false)
                    }
                } else {
                    (activity as? br.com.fenix.bilingualreader.MainActivity)?.setBlurAutoUpdate(true)
                    if (isGlass && isPopupVisible) {
                        mMenuPopupHistoryBackground.setBlurAutoUpdate(true)
                    }
                }
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

    }

    private fun getGridLayout(type: HistoryType): RecyclerView.LayoutManager {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val columnWidth = AdapterUtils.getHistoryCardSize(requireContext(), type, isLandscape).first + 1
        val spaceCount = max(1, (Resources.getSystem().displayMetrics.widthPixels - 3) / columnWidth)
        return StaggeredGridLayoutManager(spaceCount, StaggeredGridLayoutManager.VERTICAL)
    }

    private fun generateLayout(type: HistoryType) {
        mHistoryType = type
        val adapter: HistoryBaseAdapter = when (type) {
            HistoryType.SEPARATOR_CAROUSEL -> HistorySeriesCardAdapter()
            HistoryType.SEPARATOR_BIG,
            HistoryType.SEPARATOR_MEDIUM -> HistorySeparatorGridCardAdapter(type)
            else -> HistoryLineCardAdapter()
        }
        adapter.attachListener(mListener)
        if (adapter is HistorySeriesCardAdapter)
            adapter.setOrder(mViewModel.order.value?.first ?: Order.LastAccess)
        mRecyclerView.adapter = adapter as RecyclerView.Adapter<*>
        mRecyclerView.layoutManager = when (type) {
            HistoryType.SEPARATOR_BIG,
            HistoryType.SEPARATOR_MEDIUM -> getGridLayout(type)
            else -> GridLayoutManager(requireContext(), 1)
        }
        updateList(mViewModel.history.value ?: arrayListOf())
    }

    private fun getLayoutIcon(type: HistoryType, exit: Boolean): Int {
        return when (type) {
            HistoryType.LINE -> if (exit) R.drawable.ico_animated_type_grid_list_exit else R.drawable.ico_animated_type_grid_list_enter
            HistoryType.SEPARATOR_LINE -> if (exit) R.drawable.ico_animated_type_grid_list_separator_exit else R.drawable.ico_animated_type_grid_list_separator_enter
            HistoryType.SEPARATOR_MEDIUM -> if (exit) R.drawable.ico_animated_type_grid_gridmedium_separator_exit else R.drawable.ico_animated_type_grid_gridmedium_separator_enter
            HistoryType.SEPARATOR_CAROUSEL -> if (exit) R.drawable.ico_animated_type_grid_carousel_exit else R.drawable.ico_animated_type_grid_carousel_enter
            HistoryType.SEPARATOR_BIG -> if (exit) R.drawable.ico_animated_type_grid_gridbig_separator_exit else R.drawable.ico_animated_type_grid_gridbig_separator_enter
        }
    }

    private fun onChangeIconLayout(type: HistoryType) {
        if (!::miLayoutType.isInitialized)
            return

        val initial = getLayoutIcon(mHistoryType, exit = true)
        val final = getLayoutIcon(type, exit = false)
        MenuUtil.animatedSequenceDrawable(miLayoutType, initial, final)
        mHistoryType = type
    }

    private fun setAnimationRecycler(isAnimate: Boolean) {
        (mRecyclerView.adapter as? HistoryBaseAdapter)?.isAnimation = isAnimate
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        if (view != null) {
            setupPopupBackgrounds()
            mViewModel.list {
                if (it > -1)
                    mRecyclerView.adapter?.notifyItemChanged(0, it)
                else
                    mRecyclerView.adapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (view != null) {
            if (_mBottomSheet != null && mBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
                mBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            }
            mMenuPopupHistoryBackground.setBlurAutoUpdate(false)
            mMenuPopupHistoryBackground.setBlurEnabled(false)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (view != null) {
            val isGlass = mPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
            if (hidden) {
                if (_mBottomSheet != null && mBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
                    mBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
                }
                mMenuPopupHistoryBackground.setBlurAutoUpdate(false)
                mMenuPopupHistoryBackground.setBlurEnabled(false)
            } else {
                mMenuPopupHistoryBackground.setBlurEnabled(isGlass)
                if (isGlass) {
                    mMenuPopupHistoryBackground.blurOnceDeferred(mHandler, 100)
                } else {
                    mMenuPopupHistoryBackground.setBlurAutoUpdate(false)
                }
            }
        }
    }

    private fun updateList(list: ArrayList<Any>) {
        val adapter = mRecyclerView.adapter as? HistoryBaseAdapter ?: return
        if (adapter is HistorySeriesCardAdapter)
            adapter.setOrder(mViewModel.order.value?.first ?: Order.LastAccess)
        adapter.updateList(list)
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

        mViewModel.libraries.observe(viewLifecycleOwner) {
            activity?.invalidateOptionsMenu()
        }

        mViewModel.selectedLibraries.observe(viewLifecycleOwner) {
            activity?.invalidateOptionsMenu()
        }

        mViewModel.order.observe(viewLifecycleOwner) {
            if (it.first == mSortType && it.second == mSortDesc)
                return@observe
            val isDesc = if (mSortType == it.first) it.second else null
            onChangeIconSort(it.first, isDesc)
        }

        mViewModel.historyType.observe(viewLifecycleOwner) {
            if (!::mListener.isInitialized)
                return@observe
            if (it == mHistoryType && mRecyclerView.adapter != null)
                return@observe
            onChangeIconLayout(it)
            generateLayout(it)
        }
    }

    private fun onChangeSort() {
        val orderBy = when (mViewModel.order.value?.first) {
            Order.LastAccess -> Order.Name
            Order.Name -> Order.Favorite
            Order.Favorite -> Order.Series
            Order.Series -> Order.Author
            Order.Author -> Order.Genre
            Order.Genre -> Order.LastAccess
            else -> Order.LastAccess
        }

        android.widget.Toast.makeText(
            requireContext(),
            getString(R.string.menu_manga_reading_order_change, getString(orderBy.getDescription())),
            android.widget.Toast.LENGTH_SHORT
        ).show()

        mViewModel.sorted(orderBy, false)
    }

    private fun onChangeIconSort(order: Order, isDesc: Boolean?) {
        if (!::miGridOrder.isInitialized) {
            mSortType = order
            mSortDesc = isDesc ?: false
            return
        }

        if (isDesc != null) {
            val icon: Int? = when (order) {
                Order.Name, Order.Series -> if (isDesc) R.drawable.ico_animated_sort_to_desc_name else R.drawable.ico_animated_sort_to_asc_name
                Order.Favorite -> if (isDesc) R.drawable.ico_animated_sort_to_desc_favorited else R.drawable.ico_animated_sort_to_asc_favorited
                Order.LastAccess -> if (isDesc) R.drawable.ico_animated_sort_to_desc_last_access else R.drawable.ico_animated_sort_to_asc_last_access
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
                    Order.Name, Order.Series -> R.drawable.ico_animated_sort_desc_to_asc_ico_exit_name
                    Order.Favorite -> R.drawable.ico_animated_sort_desc_to_asc_ico_exit_favorited
                    Order.LastAccess -> R.drawable.ico_animated_sort_desc_to_asc_ico_exit_last_access
                    Order.Author -> R.drawable.ico_animated_sort_desc_to_asc_ico_exit_author
                    Order.Genre -> R.drawable.ico_animated_sort_desc_to_asc_ico_exit_tag
                    else -> null
                } else
                when (mSortType) {
                    Order.Name, Order.Series -> R.drawable.ico_animated_sort_asc_ico_exit_name
                    Order.Favorite -> R.drawable.ico_animated_sort_asc_ico_exit_favorited
                    Order.LastAccess -> R.drawable.ico_animated_sort_asc_ico_exit_last_access
                    Order.Author -> R.drawable.ico_animated_sort_asc_ico_exit_author
                    Order.Genre -> R.drawable.ico_animated_sort_asc_ico_exit_tag
                    else -> null
                }

            val final: Int? = when (order) {
                Order.Name, Order.Series -> R.drawable.ico_animated_sort_asc_ico_enter_name
                Order.Favorite -> R.drawable.ico_animated_sort_asc_ico_enter_favorited
                Order.LastAccess -> R.drawable.ico_animated_sort_asc_ico_enter_last_access
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
                    (it as HistoryBaseAdapter).notifyItemChanged(manga)
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
                    (it as HistoryBaseAdapter).notifyItemChanged(book)
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
                    (mRecyclerView.adapter as? HistoryBaseAdapter)?.notifyItemChanged(manga)
                }
                R.id.menu_item_manga_file_clear -> {
                    manga.lastAccess = LocalDateTime.MIN
                    manga.bookMark = 0
                    mViewModel.clear(manga)
                    (mRecyclerView.adapter as? HistoryBaseAdapter)?.notifyItemChanged(manga)
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
                                mViewModel.remove(manga)
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
                    (mRecyclerView.adapter as? HistoryBaseAdapter)?.notifyItemChanged(book)
                }
                R.id.menu_item_book_file_clear -> {
                    book.lastAccess = LocalDateTime.MIN
                    book.bookMark = 0
                    mViewModel.clear(book)
                    (mRecyclerView.adapter as? HistoryBaseAdapter)?.notifyItemChanged(book)
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
                                mViewModel.remove(book)
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
        if (view == null) return
        if (show) {
            mSkeletonLayout.alpha = 1f
            mSkeletonLayout.removeAllViews()

            mSkeletonLayout.addView(mInflater.inflate(R.layout.line_card_history_skeleton_title, null))
            for (i in 0..getSkeletonRowCount())
                mSkeletonLayout.addView(mInflater.inflate(R.layout.line_card_history_skeleton, null))

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

    private fun animateReplaceSkeleton() {
        setAnimationRecycler(false)
        mRecyclerView.visibility = View.VISIBLE
        mRecyclerView.alpha = 0f
        mRecyclerView.animate().alpha(1f).setDuration(700).start()
        mSkeletonLayout.animate().alpha(0f).setDuration(1000).withEndAction { showSkeleton(false) }.start()
    }

    override fun onDestroyView() {
        HistoryCoverCardAdapter.clearCoverCache()
        mHandler.removeCallbacksAndMessages(null)
        _mBottomSheet?.removeBottomSheetCallback(mBottomSheetCallback)
        _mBottomSheet = null
        super.onDestroyView()
    }

}

private class AutoClearedValueHistory<T : Any>(val fragment: Fragment) : ReadWriteProperty<Fragment, T> {
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

private fun <T : Any> Fragment.autoCleared() = AutoClearedValueHistory<T>(this)