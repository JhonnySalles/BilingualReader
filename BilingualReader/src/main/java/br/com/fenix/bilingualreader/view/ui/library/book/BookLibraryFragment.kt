package br.com.fenix.bilingualreader.view.ui.library.book

import android.app.ActivityOptions
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.*
import android.provider.BaseColumns
import android.util.Pair
import android.view.*
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager.widget.ViewPager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.enums.*
import br.com.fenix.bilingualreader.service.listener.BookCardListener
import br.com.fenix.bilingualreader.service.listener.MainListener
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.service.scanner.ScannerBook
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.view.adapter.library.BookGridCardAdapter
import br.com.fenix.bilingualreader.view.adapter.library.BookLineCardAdapter
import br.com.fenix.bilingualreader.view.components.ComponentsUtil
import br.com.fenix.bilingualreader.view.components.PopupOrderListener
import br.com.fenix.bilingualreader.view.ui.detail.DetailActivity
import br.com.fenix.bilingualreader.view.ui.reader.book.BookReaderActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import org.slf4j.LoggerFactory
import java.util.*
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
    private lateinit var mMenuPopupFilterOrder: FrameLayout
    private lateinit var mPopupFilterOrderView: ViewPager
    private lateinit var mPopupFilterOrderTab: TabLayout
    private lateinit var mPopupFilterFragment: LibraryBookPopupFilter
    private lateinit var mPopupOrderFragment: LibraryBookPopupOrder
    private lateinit var mBottomSheet: BottomSheetBehavior<FrameLayout>

    private var mHandler = Handler(Looper.getMainLooper())
    private val mDismissUpButton = Runnable { mScrollUp.hide() }
    private val mDismissDownButton = Runnable { mScrollDown.hide() }

    companion object {
        var mGridType: LibraryBookType = LibraryBookType.GRID
        var mSortType: Order = Order.Name
        var mSortDesc: Boolean = false
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
        inflater.inflate(R.menu.menu_library_book, menu)
        super.onCreateOptionsMenu(menu, inflater)

        miGridType = menu.findItem(R.id.menu_book_library_grid_type)
        miGridOrder = menu.findItem(R.id.menu_book_library_list_order)
        miSearch = menu.findItem(R.id.menu_book_library_search)
        searchView = miSearch.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                mRefreshLayout.isEnabled = newText == null || newText.isEmpty()
                filter(newText)
                return false
            }
        })

        val searchSrcTextView = miSearch.actionView!!.findViewById<View>(Resources.getSystem().getIdentifier("search_src_text",
            "id", "android")) as AutoCompleteTextView
        searchSrcTextView.threshold = 1
        searchSrcTextView.setDropDownBackgroundResource(R.drawable.list_item_suggestion_background)

        val from = arrayOf(SearchManager.SUGGEST_COLUMN_TEXT_1)
        val to = intArrayOf(R.id.list_item_suggestion)
        val suggestions = Util.getBookFilters(requireContext()).keys.toList()
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
                    val query =  searchView.query.toString().substringBeforeLast('@', "") + " " + selection
                    searchView.setQuery(query.trim(), false)
                }
                return true
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

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
                            if (substring.contains(Util.filterToString(requireContext(), Type.BOOK, br.com.fenix.bilingualreader.model.enums.Filter.Type) , true)) {
                                substring = substring.substringBefore(":")
                                FileType.getBook().forEachIndexed { index, type ->
                                    cursor.addRow(arrayOf(index, "@$substring:$type "))
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

                mRefreshLayout.isEnabled = newText == null || newText.isEmpty()
                filter(newText)
                return false
            }
        })

        enableSearchView(searchView, !mRefreshLayout.isRefreshing)

        MenuUtil.longClick(requireActivity(), R.id.menu_book_library_list_order) {
            if (!mRefreshLayout.isRefreshing)
                onOpenMenuSort()
        }

        val iconGrid: Int = when (mGridType) {
            LibraryBookType.GRID -> R.drawable.ic_type_grid_big
            else -> R.drawable.ic_type_list
        }

        miGridType.setIcon(iconGrid)

        val iconSort: Int = when (mSortType) {
            Order.LastAccess -> R.drawable.ico_animated_sort_to_desc_last_access
            Order.Favorite -> R.drawable.ico_animated_sort_to_desc_favorited
            Order.Date -> R.drawable.ico_animated_sort_to_desc_date_created
            else -> R.drawable.ico_animated_sort_to_desc_name
        }
        miGridOrder.setIcon(iconSort)

        MenuUtil.longClick(requireActivity(), R.id.menu_manga_library_list_order) {
            if (!mRefreshLayout.isRefreshing)
                onOpenMenuSort()
        }

        miGridOrder.setOnMenuItemClickListener {
            onChangeSort()
            true
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
            onRefresh()
        else
            mViewModel.updateList { change, indexes ->
                if (change)
                    notifyDataSet(indexes)
            }

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
            R.id.menu_book_library_grid_type -> onChangeLayout()
            R.id.menu_book_library_list_order -> onChangeSort()
        }
        return super.onOptionsItemSelected(menuItem)
    }

    private fun onOpenMenuSort() {
        mMenuPopupFilterOrder.visibility = View.VISIBLE
        mBottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
        mMenuPopupFilterOrder.translationY = 100F
        mMenuPopupFilterOrder.animate()
            .setDuration(200)
            .translationY(0f)
    }

    private fun onChangeSort() {
        if (mRefreshLayout.isRefreshing)
            return

        val orderBy = when (mViewModel.order.value?.first) {
            Order.Name -> Order.Date
            Order.Date -> Order.Favorite
            Order.Favorite -> Order.LastAccess
            Order.LastAccess -> Order.Genre
            else -> Order.Name
        }

        Toast.makeText(
            requireContext(),
            getString(R.string.menu_reading_book_order_change, mMapOrder[orderBy]),
            Toast.LENGTH_SHORT
        ).show()

        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
        with(sharedPreferences.edit()) {
            this!!.putString(GeneralConsts.KEYS.LIBRARY.BOOK_ORDER, orderBy.toString())
            this.commit()
        }

        if (mViewModel.listBook.value != null) {
            mViewModel.sorted(orderBy)
            val range = (mViewModel.listBook.value?.size ?: 1)
            notifyDataSet(0, range)
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
                    else -> null
                } else
                when (mSortType) {
                    Order.Name -> R.drawable.ico_animated_sort_asc_ico_exit_name
                    Order.Favorite -> R.drawable.ico_animated_sort_asc_ico_exit_favorited
                    Order.LastAccess -> R.drawable.ico_animated_sort_asc_ico_exit_last_access
                    Order.Date -> R.drawable.ico_animated_sort_asc_ico_exit_date_created
                    Order.Author -> R.drawable.ico_animated_sort_asc_ico_exit_author
                    else -> null
                }

            val final: Int? = when (order) {
                Order.Name -> R.drawable.ico_animated_sort_asc_ico_enter_name
                Order.Favorite -> R.drawable.ico_animated_sort_asc_ico_enter_favorited
                Order.LastAccess -> R.drawable.ico_animated_sort_asc_ico_enter_last_access
                Order.Date -> R.drawable.ico_animated_sort_asc_ico_enter_date_created
                Order.Author -> R.drawable.ico_animated_sort_asc_ico_enter_author
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

    private fun onChangeLayout() {
        mGridType = when (mGridType) {
            LibraryBookType.LINE -> LibraryBookType.GRID
            else -> LibraryBookType.LINE
        }

        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
        with(sharedPreferences.edit()) {
            this!!.putString(GeneralConsts.KEYS.LIBRARY.BOOK_LIBRARY_TYPE, mGridType.toString())
            this.commit()
        }

        onChangeIconLayout()
        generateLayout()
        updateList(mViewModel.listBook.value!!)
    }

    private fun onChangeIconLayout() {
        val icon: Int = when (mGridType) {
            LibraryBookType.GRID -> R.drawable.ico_animated_type_grid_list_to_gridbig
            else -> R.drawable.ico_animated_type_grid_gridbig_to_list
        }
        miGridType.setIcon(icon)
        (miGridType.icon as AnimatedVectorDrawable).start()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_book_library, container, false)
        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
        mGridType = LibraryBookType.valueOf(
            sharedPreferences.getString(GeneralConsts.KEYS.LIBRARY.BOOK_LIBRARY_TYPE, LibraryBookType.GRID.toString())
                .toString()
        )

        mMapOrder = hashMapOf(
            Order.Name to getString(R.string.config_option_book_order_name),
            Order.Date to getString(R.string.config_option_book_order_date),
            Order.LastAccess to getString(R.string.config_option_book_order_access),
            Order.Favorite to getString(R.string.config_option_book_order_favorite),
            Order.Author to getString(R.string.config_option_book_order_author),
            Order.Genre to getString(R.string.config_option_book_order_genre)
        )

        mRoot = root.findViewById(R.id.frame_book_library_root)
        mRecyclerView = root.findViewById(R.id.book_library_recycler_view)
        mRefreshLayout = root.findViewById(R.id.book_library_refresh)
        mScrollUp = root.findViewById(R.id.book_library_scroll_up)
        mScrollDown = root.findViewById(R.id.book_library_scroll_down)

        mMenuPopupFilterOrder = root.findViewById(R.id.book_library_popup_menu_order_filter)
        mPopupFilterOrderTab = root.findViewById(R.id.book_library_popup_order_filter_tab)
        mPopupFilterOrderView = root.findViewById(R.id.book_library_popup_order_filter_view_pager)

        root.findViewById<ImageView>(R.id.book_library_popup_menu_order_filter_close)
            .setOnClickListener {
                mMenuPopupFilterOrder.visibility = View.GONE
            }

        ComponentsUtil.setThemeColor(requireContext(), mRefreshLayout)
        mRefreshLayout.setOnRefreshListener(this)
        mRefreshLayout.isEnabled = true
        mRefreshLayout.setProgressViewOffset(
            false,
            (resources.getDimensionPixelOffset(R.dimen.default_navigator_header_margin_top) + 60) * -1,
            10
        )

        mScrollUp.visibility = View.GONE
        mScrollDown.visibility = View.GONE

        mScrollUp.setOnClickListener {
            setAnimationRecycler(false)
            mRecyclerView.smoothScrollToPosition(0)
        }
        mScrollDown.setOnClickListener {
            setAnimationRecycler(false)
            mRecyclerView.smoothScrollToPosition((mRecyclerView.adapter as RecyclerView.Adapter).itemCount)
        }

        mPopupFilterOrderTab.setupWithViewPager(mPopupFilterOrderView)

        mPopupFilterFragment = LibraryBookPopupFilter()
        mPopupOrderFragment = LibraryBookPopupOrder(this)

        BottomSheetBehavior.from(mMenuPopupFilterOrder).apply {
            peekHeight = 195
            this.state = BottomSheetBehavior.STATE_COLLAPSED
            mBottomSheet = this
        }
        mBottomSheet.isDraggable = true

        root.findViewById<ImageView>(R.id.book_library_popup_menu_order_filter_touch)
            .setOnClickListener {
                if (mBottomSheet.state == BottomSheetBehavior.STATE_COLLAPSED)
                    mBottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
                else
                    mBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            }

        val viewFilterOrderPagerAdapter =
            ViewPagerAdapter(requireActivity().supportFragmentManager, 0)
        viewFilterOrderPagerAdapter.addFragment(
            mPopupFilterFragment,
            resources.getString(R.string.popup_library_book_tab_item_filter)
        )
        viewFilterOrderPagerAdapter.addFragment(
            mPopupOrderFragment,
            resources.getString(R.string.popup_library_book_tab_item_ordering)
        )

        mPopupFilterOrderView.adapter = viewFilterOrderPagerAdapter

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
            override fun onClick(book: Book) {
                if (book.file.exists()) {
                    val intent = Intent(context, BookReaderActivity::class.java)
                    val bundle = Bundle()
                    bundle.putSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY, mViewModel.getLibrary())
                    bundle.putString(GeneralConsts.KEYS.BOOK.NAME, book.title)
                    bundle.putInt(GeneralConsts.KEYS.BOOK.MARK, book.bookMark)
                    bundle.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK, book)
                    intent.putExtras(bundle)
                    context?.startActivity(intent)
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
                        R.id.menu_book_config_tag ->  {}
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

            override fun onClickLongConfig(book: Book, root: View, item: View, position: Int) {

            }

        }
        observer()
        mViewModel.list {
            if (it)
                sortList()
        }

        if (!Storage.isPermissionGranted(requireContext()))
            Storage.takePermission(requireContext(), requireActivity())

        generateLayout()
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

        val idText = if (mGridType != LibraryBookType.LINE)
            R.id.book_grid_title
        else
            R.id.book_line_title

        val idAuthor = if (mGridType != LibraryBookType.LINE)
            R.id.book_grid_sub_title
        else
            R.id.book_line_sub_title

        val idProgress = if (mGridType != LibraryBookType.LINE)
            R.id.book_grid_progress
        else
            R.id.book_line_progress

        val idCover = if (mGridType != LibraryBookType.LINE)
            R.id.book_grid_image_cover
        else
            R.id.book_line_image_cover

        val pImageCover: Pair<View, String> = Pair(view.findViewById<ImageView>(idCover), "transition_book_cover")
        val pTitle: Pair<View, String> = Pair(view.findViewById<TextView>(idText), "transition_book_title")
        val pAuthor: Pair<View, String> = Pair(view.findViewById<TextView>(idAuthor), "transition_book_author")
        val pProgress: Pair<View, String> = Pair(view.findViewById<ProgressBar>(idProgress), "transition_progress_bar")

        val options = ActivityOptions
            .makeSceneTransitionAnimation(requireActivity(), *arrayOf(pImageCover, pTitle, pAuthor, pProgress))
        requireActivity().overridePendingTransition(R.anim.fade_in_fragment_add_enter, R.anim.fade_out_fragment_remove_exit)
        startActivityForResult(intent, GeneralConsts.REQUEST.BOOK_DETAIL, options.toBundle())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GeneralConsts.REQUEST.BOOK_DETAIL)
            notifyDataSet(itemRefresh!!)
    }

    private fun loadConfig() {
        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
        mSortType = Order.valueOf(
            sharedPreferences.getString(
                GeneralConsts.KEYS.LIBRARY.BOOK_ORDER,
                Order.Name.toString()
            ).toString()
        )

        mGridType = LibraryBookType.valueOf(
            sharedPreferences.getString(
                GeneralConsts.KEYS.LIBRARY.BOOK_LIBRARY_TYPE,
                LibraryBookType.LINE.toString()
            )
                .toString()
        )

        mViewModel.sorted(mSortType)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == GeneralConsts.REQUEST.PERMISSION_FILES_ACCESS && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            onRefresh()
    }

    private fun generateLayout() {
        if (mGridType != LibraryBookType.LINE) {
            val gridAdapter = BookGridCardAdapter()
            mRecyclerView.adapter = gridAdapter

            val columnWidth: Int = when (mGridType) {
                LibraryBookType.GRID -> resources.getDimension(R.dimen.book_grid_card_layout_width)
                    .toInt()
                else -> resources.getDimension(R.dimen.book_grid_card_layout_width).toInt()
            } + 1

            val spaceCount: Int = max(1, Resources.getSystem().displayMetrics.widthPixels / columnWidth)
            mRecyclerView.layoutManager = GridLayoutManager(requireContext(), spaceCount)
            gridAdapter.attachListener(mListener)
            mRecyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_library_grid)
        } else {
            val lineAdapter = BookLineCardAdapter()
            mRecyclerView.adapter = lineAdapter
            mRecyclerView.layoutManager = GridLayoutManager(requireContext(), 1)
            lineAdapter.attachListener(mListener)
            mRecyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_library_line)
        }
    }

    private fun setAnimationRecycler(isAnimate: Boolean) {
        if (mGridType != LibraryBookType.LINE)
            (mRecyclerView.adapter as BookGridCardAdapter).isAnimation = isAnimate
        else
            (mRecyclerView.adapter as BookLineCardAdapter).isAnimation = isAnimate
    }

    private fun removeList(book: Book) {
        if (mGridType != LibraryBookType.LINE)
            (mRecyclerView.adapter as BookGridCardAdapter).removeList(book)
        else
            (mRecyclerView.adapter as BookLineCardAdapter).removeList(book)
    }

    private fun updateList(list: MutableList<Book>) {
        if (mGridType != LibraryBookType.LINE)
            (mRecyclerView.adapter as BookGridCardAdapter).updateList(list)
        else
            (mRecyclerView.adapter as BookLineCardAdapter).updateList(list)
    }

    private fun observer() {
        mViewModel.listBook.observe(viewLifecycleOwner) {
            updateList(it)
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
        val range = (mViewModel.listBook.value?.size ?: 1)
        notifyDataSet(0, range)
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
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: ViewHolder, target: ViewHolder
        ): Boolean {
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
        val dialog: AlertDialog =
            MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
                .setTitle(getString(R.string.book_library_menu_delete))
                .setMessage(getString(R.string.book_library_menu_delete_description) + "\n" + book.file.name)
                .setPositiveButton(
                    R.string.action_delete
                ) { _, _ ->
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
            shareIntent.type = book.type.getMimeType()
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
}
