package br.com.fenix.bilingualreader.view.ui.library.book

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.*
import android.util.Pair
import android.view.*
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.enums.LibraryBookType
import br.com.fenix.bilingualreader.model.enums.ListMod
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.BookCardListener
import br.com.fenix.bilingualreader.service.listener.MainListener
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.service.scanner.ScannerBook
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.view.adapter.library.BookGridCardAdapter
import br.com.fenix.bilingualreader.view.adapter.library.BookLineCardAdapter
import br.com.fenix.bilingualreader.view.components.ComponentsUtil
import br.com.fenix.bilingualreader.view.ui.manga_detail.MangaDetailActivity
import br.com.fenix.bilingualreader.view.ui.reader.book.BookReaderActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.slf4j.LoggerFactory
import kotlin.math.max


class BookLibraryFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private val mLOGGER = LoggerFactory.getLogger(BookLibraryFragment::class.java)

    private lateinit var mViewModel: BookLibraryViewModel
    private lateinit var mainFunctions: MainListener

    private var mOrderBy: Order = Order.Name

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

    private var mHandler = Handler(Looper.getMainLooper())
    private val mDismissUpButton = Runnable { mScrollUp.hide() }
    private val mDismissDownButton = Runnable { mScrollDown.hide() }

    companion object {
        var mGridType: LibraryBookType = LibraryBookType.GRID
    }

    private val mUpdateHandler: Handler = UpdateHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProvider(requireActivity()).get(BookLibraryViewModel::class.java)
        loadConfig()
        setHasOptionsMenu(true)
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

        enableSearchView(searchView, !mRefreshLayout.isRefreshing)
        onChangeIconLayout()
    }

    private fun filter(text: String?) {
        mViewModel.filter.filter(text)
    }

    override fun onResume() {
        super.onResume()

        /*ScannerBook.getInstance(requireContext()).addUpdateHandler(mUpdateHandler)
        if (ScannerBook.getInstance(requireContext()).isRunning())
            setIsRefreshing(true)*/

        if (mViewModel.isEmpty())
            onRefresh()
        else
            mViewModel.updateList { change, indexes ->
                if (change)
                    notifyDataSet(indexes)
            }

        setIsRefreshing(false)
    }

    override fun onStop() {
        //ScannerBook.getInstance(requireContext()).removeUpdateHandler(mUpdateHandler)
        mainFunctions.clearLibraryTitle()
        super.onStop()
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

    private fun notifyDataSet(indexes: MutableList<kotlin.Pair<ListMod, Int>>) {
        if (indexes.any { it.first == ListMod.FULL })
            notifyDataSet(0, (mViewModel.listBook.value?.size ?: 1))
        else {
            for (index in indexes)
                when (index.first) {
                    ListMod.ADD -> notifyDataSet(index.second, insert = true)
                    ListMod.REM -> notifyDataSet(index.second, removed = true)
                    ListMod.MOD -> notifyDataSet(index.second)
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

    private fun onChangeSort() {
        if (mRefreshLayout.isRefreshing)
            return

        mOrderBy = when (mOrderBy) {
            Order.Name -> Order.Date
            Order.Date -> Order.Favorite
            Order.Favorite -> Order.LastAccess
            Order.LastAccess -> Order.Genre
            else -> Order.Name
        }

        Toast.makeText(
            requireContext(),
            getString(R.string.menu_reading_book_order_change, mMapOrder[mOrderBy]),
            Toast.LENGTH_SHORT
        ).show()

        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
        with(sharedPreferences.edit()) {
            this!!.putString(GeneralConsts.KEYS.LIBRARY.MANGA_ORDER, mOrderBy.toString())
            this.commit()
        }

        if (mViewModel.listBook.value != null)
            sortList()

    }

    private fun sortList() {
        mViewModel.sorted(mOrderBy)
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
            this!!.putString(GeneralConsts.KEYS.LIBRARY.MANGA_LIBRARY_TYPE, mGridType.toString())
            this.commit()
        }

        onChangeIconLayout()
        generateLayout()
        updateList(mViewModel.listBook.value!!)
    }

    private fun onChangeIconLayout() {
        val icon: Int = when (mGridType) {
            LibraryBookType.GRID -> R.drawable.ic_type_grid_big
            else -> R.drawable.ic_type_list
        }
        miGridType.setIcon(icon)
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

        generateLayout()
        setIsRefreshing(true)
        //ScannerBook.getInstance(requireContext()).scanLibrary()

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
        return
        itemRefresh = position
        val intent = Intent(requireContext(), MangaDetailActivity::class.java)
        val bundle = Bundle()
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK, book)
        intent.putExtras(bundle)

        val idText = if (mGridType != LibraryBookType.LINE)
            R.id.manga_grid_text_title
        else
            R.id.manga_line_text_title

        val idProgress = if (mGridType != LibraryBookType.LINE)
            R.id.manga_grid_progress
        else
            R.id.manga_line_progress

        val title = view.findViewById<TextView>(idText)
        val progress = view.findViewById<ProgressBar>(idProgress)
        val pImageCover: Pair<View, String> = Pair(view, "transition_book_cover")
        val pTitleCover: Pair<View, String> = Pair(title, "transition_book_title")
        val pProgress: Pair<View, String> = Pair(progress, "transition_progress_bar")

        val options = ActivityOptions
            .makeSceneTransitionAnimation(requireActivity(), *arrayOf(pImageCover, pTitleCover, pProgress))
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
        mOrderBy = Order.valueOf(
            sharedPreferences.getString(
                GeneralConsts.KEYS.LIBRARY.BOOK_ORDER,
                Order.Name.toString()
            ).toString()
        )
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

        if (!ScannerBook.getInstance(requireContext()).isRunning()) {
            setIsRefreshing(true)
            ScannerBook.getInstance(requireContext()).scanLibrary()
        }
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

}
