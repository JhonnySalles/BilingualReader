package br.com.fenix.bilingualreader.view.ui.library.manga

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.*
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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager.widget.ViewPager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.LibraryType
import br.com.fenix.bilingualreader.model.enums.ListMod
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.MainListener
import br.com.fenix.bilingualreader.service.listener.MangaCardListener
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.service.scanner.ScannerManga
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.view.adapter.library.MangaGridCardAdapter
import br.com.fenix.bilingualreader.view.adapter.library.MangaLineCardAdapter
import br.com.fenix.bilingualreader.view.components.ComponentsUtil
import br.com.fenix.bilingualreader.view.components.PopupOrderSortedListener
import br.com.fenix.bilingualreader.view.ui.detail.DetailActivity
import br.com.fenix.bilingualreader.view.ui.reader.manga.MangaReaderActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.math.max


class MangaLibraryFragment : Fragment(), PopupOrderSortedListener, SwipeRefreshLayout.OnRefreshListener {

    private val mLOGGER = LoggerFactory.getLogger(MangaLibraryFragment::class.java)

    private val uniqueID: String = UUID.randomUUID().toString()

    private lateinit var mViewModel: MangaLibraryViewModel
    private lateinit var mainFunctions: MainListener

    private lateinit var mMapOrder: HashMap<Order, String>
    private lateinit var mRoot: FrameLayout
    private lateinit var mRefreshLayout: SwipeRefreshLayout
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var miGridType: MenuItem
    private lateinit var miGridOrder: MenuItem
    private lateinit var miSearch: MenuItem
    private lateinit var searchView: SearchView
    private lateinit var mListener: MangaCardListener
    private lateinit var mScrollUp: FloatingActionButton
    private lateinit var mScrollDown: FloatingActionButton
    private lateinit var mMenuPopupFilterOrder: FrameLayout
    private lateinit var mPopupFilterOrderView: ViewPager
    private lateinit var mPopupFilterOrderTab: TabLayout
    private lateinit var mPopupFilterFragment: PopupFilter
    private lateinit var mPopupOrderFragment: PopupOrder
    private lateinit var mBottomSheet: BottomSheetBehavior<FrameLayout>

    private var mHandler = Handler(Looper.getMainLooper())
    private val mDismissUpButton = Runnable { mScrollUp.hide() }
    private val mDismissDownButton = Runnable { mScrollDown.hide() }

    companion object {
        var mGridType: LibraryType = LibraryType.GRID_BIG
    }

    private val mUpdateHandler: Handler = UpdateHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProvider(requireActivity())[MangaLibraryViewModel::class.java]
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
        inflater.inflate(R.menu.menu_library_manga, menu)
        super.onCreateOptionsMenu(menu, inflater)

        miGridType = menu.findItem(R.id.menu_manga_library_grid_type)
        miGridOrder = menu.findItem(R.id.menu_manga_library_list_order)
        miSearch = menu.findItem(R.id.menu_manga_library_search)
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
        val icon: Int = when (mGridType) {
            LibraryType.GRID_SMALL -> R.drawable.ic_type_grid_small
            LibraryType.GRID_BIG -> R.drawable.ic_type_grid_big
            LibraryType.GRID_MEDIUM -> R.drawable.ic_type_grid_medium
            else -> R.drawable.ic_type_list
        }
        miGridType.setIcon(icon)

        MenuUtil.longClick(requireActivity(), R.id.menu_manga_library_list_order) {
            onOpenMenuSort()
        }

        miGridOrder.setOnMenuItemClickListener {
            onChangeSort()
            true
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

        ScannerManga.getInstance(requireContext()).addUpdateHandler(mUpdateHandler)

        if (mViewModel.isEmpty())
            onRefresh()
        else
            mViewModel.updateList { change, indexes ->
                if (change)
                    notifyDataSet(indexes)
            }

        if (ScannerManga.getInstance(requireContext()).isRunning())
            setIsRefreshing(true)
        else
            setIsRefreshing(false)
    }

    override fun onStop() {
        ScannerManga.getInstance(requireContext()).removeUpdateHandler(mUpdateHandler)
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
                GeneralConsts.SCANNER.MESSAGE_MANGA_UPDATED_ADD -> refreshLibraryAddDelayed(obj as Manga)
                GeneralConsts.SCANNER.MESSAGE_MANGA_UPDATED_REMOVE -> refreshLibraryRemoveDelayed(
                    obj as Manga
                )
                GeneralConsts.SCANNER.MESSAGE_MANGA_UPDATE_FINISHED -> {
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
            notifyDataSet(0, (mViewModel.listMangas.value?.size ?: 1))
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

    private fun notifyDataSet(
        index: Int,
        range: Int = 0,
        insert: Boolean = false,
        removed: Boolean = false
    ) {
        if (insert)
            mRecyclerView.adapter?.notifyItemInserted(index)
        else if (removed)
            mRecyclerView.adapter?.notifyItemRemoved(index)
        else if (range > 1)
            mRecyclerView.adapter?.notifyItemRangeChanged(index, range)
        else
            mRecyclerView.adapter?.notifyItemChanged(index)
    }

    private fun refreshLibraryAddDelayed(manga: Manga) {
        val index = mViewModel.addList(manga)
        if (index > -1)
            mRecyclerView.adapter?.notifyItemInserted(index)
    }

    private fun refreshLibraryRemoveDelayed(manga: Manga) {
        val index = mViewModel.remList(manga)
        if (index > -1)
            mRecyclerView.adapter?.notifyItemRemoved(index)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_manga_library_grid_type -> onChangeLayout()
            R.id.menu_manga_library_list_order -> onChangeSort()
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
            else -> Order.Name
        }

        Toast.makeText(
            requireContext(),
            getString(R.string.menu_manga_reading_order_change) + " ${mMapOrder[orderBy]}",
            Toast.LENGTH_SHORT
        ).show()

        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
        with(sharedPreferences.edit()) {
            this!!.putString(GeneralConsts.KEYS.LIBRARY.MANGA_ORDER, orderBy.toString())
            this.commit()
        }

        if (mViewModel.listMangas.value != null) {
            mViewModel.sorted(orderBy)
            val range = (mViewModel.listMangas.value?.size ?: 1)
            notifyDataSet(0, range)
        }

    }

    private fun sortList() {
        mViewModel.sorted()
        val range = (mViewModel.listMangas.value?.size ?: 1)
        notifyDataSet(0, range)
    }

    override fun popupOrderOnChange() {
        val range = (mViewModel.listMangas.value?.size ?: 1)
        notifyDataSet(0, range)
    }

    private fun onChangeLayout() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        mGridType = when (mGridType) {
            LibraryType.LINE -> LibraryType.GRID_BIG
            LibraryType.GRID_BIG -> LibraryType.GRID_MEDIUM
            LibraryType.GRID_MEDIUM -> if (isLandscape) LibraryType.GRID_SMALL else LibraryType.LINE
            else -> LibraryType.LINE
        }

        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
        with(sharedPreferences.edit()) {
            this!!.putString(GeneralConsts.KEYS.LIBRARY.MANGA_LIBRARY_TYPE, mGridType.toString())
            this.commit()
        }

        onChangeIconLayout()
        generateLayout()
        updateList(mViewModel.listMangas.value!!)
    }

    private fun onChangeIconLayout() {
        val icon: Int = when (mGridType) {
            LibraryType.GRID_SMALL -> R.drawable.ico_animated_type_grid_gridmedium_to_gridsmall
            LibraryType.GRID_BIG -> R.drawable.ico_animated_type_grid_list_to_gridbig
            LibraryType.GRID_MEDIUM -> R.drawable.ico_animated_type_grid_gridbig_to_gridmedium
            else -> if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                R.drawable.ico_animated_type_grid_gridsmall_to_list
            else
                R.drawable.ico_animated_type_grid_gridmedium_to_list
        }
        miGridType.setIcon(icon)
        (miGridType.icon as AnimatedVectorDrawable).start()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_manga_library, container, false)
        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
        mGridType = LibraryType.valueOf(
            sharedPreferences.getString(
                GeneralConsts.KEYS.LIBRARY.MANGA_LIBRARY_TYPE,
                LibraryType.LINE.toString()
            )
                .toString()
        )

        mMapOrder = hashMapOf(
            Order.Name to getString(R.string.config_option_manga_order_name),
            Order.Date to getString(R.string.config_option_manga_order_date),
            Order.LastAccess to getString(R.string.config_option_manga_order_access),
            Order.Favorite to getString(R.string.config_option_manga_order_favorite)
        )

        mRoot = root.findViewById(R.id.frame_manga_library_root)
        mRecyclerView = root.findViewById(R.id.manga_library_recycler_view)
        mRefreshLayout = root.findViewById(R.id.manga_library_refresh)
        mScrollUp = root.findViewById(R.id.manga_library_scroll_up)
        mScrollDown = root.findViewById(R.id.manga_library_scroll_down)

        mMenuPopupFilterOrder = root.findViewById(R.id.popup_menu_order_filter)
        mPopupFilterOrderTab = root.findViewById(R.id.popup_order_filter_tab)
        mPopupFilterOrderView = root.findViewById(R.id.popup_order_filter_view_pager)

        root.findViewById<ImageView>(R.id.popup_menu_order_filter_close).setOnClickListener {
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
            (mScrollUp.drawable as AnimatedVectorDrawable).start()
            mRecyclerView.smoothScrollToPosition(0)
        }
        mScrollDown.setOnClickListener {
            setAnimationRecycler(false)
            (mScrollDown.drawable as AnimatedVectorDrawable).start()
            mRecyclerView.smoothScrollToPosition((mRecyclerView.adapter as RecyclerView.Adapter).itemCount)
        }

        mPopupFilterOrderTab.setupWithViewPager(mPopupFilterOrderView)

        mPopupFilterFragment = PopupFilter()
        mPopupOrderFragment = PopupOrder()
        mPopupOrderFragment.addChangeListener(this)

        BottomSheetBehavior.from(mMenuPopupFilterOrder).apply {
            peekHeight = 195
            this.state = BottomSheetBehavior.STATE_COLLAPSED
            mBottomSheet = this
        }
        mBottomSheet.isDraggable = true

        root.findViewById<ImageView>(R.id.popup_menu_order_filter_touch).setOnClickListener {
            if (mBottomSheet.state == BottomSheetBehavior.STATE_COLLAPSED)
                mBottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
            else
                mBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        val viewTranslatePagerAdapter =  ViewPagerAdapter(requireActivity().supportFragmentManager, 0)
        viewTranslatePagerAdapter.addFragment(
            mPopupFilterFragment,
            resources.getString(R.string.popup_library_manga_tab_item_filter)
        )
        viewTranslatePagerAdapter.addFragment(
            mPopupOrderFragment,
            resources.getString(R.string.popup_library_manga_tab_item_ordering)
        )

        mPopupFilterOrderView.adapter = viewTranslatePagerAdapter

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

        mListener = object : MangaCardListener {
            override fun onClick(manga: Manga) {
                if (manga.file.exists()) {
                    val intent = Intent(context, MangaReaderActivity::class.java)
                    val bundle = Bundle()
                    bundle.putSerializable(
                        GeneralConsts.KEYS.OBJECT.LIBRARY,
                        mViewModel.getLibrary()
                    )
                    bundle.putString(GeneralConsts.KEYS.MANGA.NAME, manga.title)
                    bundle.putInt(GeneralConsts.KEYS.MANGA.MARK, manga.bookMark)
                    bundle.putSerializable(GeneralConsts.KEYS.OBJECT.MANGA, manga)
                    intent.putExtras(bundle)
                    context?.startActivity(intent)
                    requireActivity().overridePendingTransition(
                        R.anim.fade_in_fragment_add_enter,
                        R.anim.fade_out_fragment_remove_exit
                    )
                } else {
                    removeList(manga)
                    mViewModel.delete(manga)
                    MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
                        .setTitle(getString(R.string.manga_excluded))
                        .setMessage(getString(R.string.file_not_found))
                        .setPositiveButton(
                            R.string.action_neutral
                        ) { _, _ -> }
                        .create()
                        .show()
                }
            }

            override fun onClickLong(manga: Manga, view: View, position: Int) {
                if (mRefreshLayout.isRefreshing)
                    return

                goMangaDetail(manga, view, position)
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
        ScannerManga.getInstance(requireContext()).scanLibrary(mViewModel.getLibrary())

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            // Prevent backpress if query is actived
            override fun handleOnBackPressed() {
                if (searchView.query.isNotEmpty())
                    searchView.setQuery("", true)
                else if (!searchView.isIconified)
                    searchView.isIconified = true
                else {
                    isEnabled = false
                    mViewModel.restoreLastStackLibrary(uniqueID)
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
    private fun goMangaDetail(manga: Manga, view: View, position: Int) {
        itemRefresh = position
        val intent = Intent(requireContext(), DetailActivity::class.java)
        val bundle = Bundle()
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY, mViewModel.getLibrary())
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.MANGA, manga)
        intent.putExtras(bundle)
        val idText = if (mGridType != LibraryType.LINE)
            R.id.manga_grid_text_title
        else
            R.id.manga_line_text_title

        val idProgress = if (mGridType != LibraryType.LINE)
            R.id.manga_grid_progress
        else
            R.id.manga_line_progress

        val idCover = if (mGridType != LibraryType.LINE)
            R.id.manga_grid_image_cover
        else
            R.id.manga_line_image_cover

        val pImageCover: Pair<View, String> = Pair(view.findViewById<ImageView>(idCover), "transition_manga_cover")
        val pTitle: Pair<View, String> = Pair(view.findViewById<TextView>(idText), "transition_manga_title")
        val pProgress: Pair<View, String> = Pair(view.findViewById<ProgressBar>(idProgress), "transition_progress_bar")

        val options = ActivityOptions
            .makeSceneTransitionAnimation(
                requireActivity(),
                *arrayOf(pImageCover, pTitle, pProgress)
            )
        requireActivity().overridePendingTransition(
            R.anim.fade_in_fragment_add_enter,
            R.anim.fade_out_fragment_remove_exit
        )
        startActivityForResult(intent, GeneralConsts.REQUEST.MANGA_DETAIL, options.toBundle())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GeneralConsts.REQUEST.MANGA_DETAIL)
            notifyDataSet(itemRefresh!!)
    }

    private fun loadConfig() {
        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
        val orderBy = Order.valueOf(
            sharedPreferences.getString(
                GeneralConsts.KEYS.LIBRARY.MANGA_ORDER,
                Order.Name.toString()
            ).toString()
        )
        mViewModel.sorted(orderBy)
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
        if (mGridType != LibraryType.LINE) {
            val gridAdapter = MangaGridCardAdapter()
            mRecyclerView.adapter = gridAdapter

            val isLandscape =
                resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val columnWidth: Int = when (mGridType) {
                LibraryType.GRID_BIG -> resources.getDimension(R.dimen.manga_grid_card_layout_width)
                    .toInt()
                LibraryType.GRID_MEDIUM -> if (isLandscape) resources.getDimension(R.dimen.manga_grid_card_layout_width_landscape_medium)
                    .toInt() else resources.getDimension(R.dimen.manga_grid_card_layout_width_medium)
                    .toInt()
                LibraryType.GRID_SMALL -> if (isLandscape) resources.getDimension(R.dimen.manga_grid_card_layout_width_small)
                    .toInt()
                else resources.getDimension(R.dimen.manga_grid_card_layout_width).toInt()
                else -> resources.getDimension(R.dimen.manga_grid_card_layout_width).toInt()
            } + 1

            val spaceCount: Int =
                max(1, Resources.getSystem().displayMetrics.widthPixels / columnWidth)
            mRecyclerView.layoutManager = GridLayoutManager(requireContext(), spaceCount)
            gridAdapter.attachListener(mListener)
            mRecyclerView.layoutAnimation =
                AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_library_grid)
        } else {
            val lineAdapter = MangaLineCardAdapter()
            mRecyclerView.adapter = lineAdapter
            mRecyclerView.layoutManager = GridLayoutManager(requireContext(), 1)
            lineAdapter.attachListener(mListener)
            mRecyclerView.layoutAnimation =
                AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_library_line)
        }
    }

    private fun setAnimationRecycler(isAnimate: Boolean) {
        if (mGridType != LibraryType.LINE)
            (mRecyclerView.adapter as MangaGridCardAdapter).isAnimation = isAnimate
        else
            (mRecyclerView.adapter as MangaLineCardAdapter).isAnimation = isAnimate
    }

    private fun removeList(manga: Manga) {
        if (mGridType != LibraryType.LINE)
            (mRecyclerView.adapter as MangaGridCardAdapter).removeList(manga)
        else
            (mRecyclerView.adapter as MangaLineCardAdapter).removeList(manga)
    }

    private fun updateList(list: MutableList<Manga>) {
        if (mGridType != LibraryType.LINE)
            (mRecyclerView.adapter as MangaGridCardAdapter).updateList(list)
        else
            (mRecyclerView.adapter as MangaLineCardAdapter).updateList(list)
    }

    private fun observer() {
        mViewModel.listMangas.observe(viewLifecycleOwner) {
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

        if (!ScannerManga.getInstance(requireContext()).isRunning()) {
            setIsRefreshing(true)
            ScannerManga.getInstance(requireContext()).scanLibrary(mViewModel.getLibrary())
        }
    }

    private var itemTouchHelperCallback =
        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: ViewHolder, target: ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
                val manga = mViewModel.getAndRemove(viewHolder.bindingAdapterPosition) ?: return
                val position = viewHolder.bindingAdapterPosition
                var excluded = false
                val dialog: AlertDialog =
                    MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
                        .setTitle(getString(R.string.manga_library_menu_delete))
                        .setMessage(getString(R.string.manga_library_menu_delete_description) + "\n" + manga.file.name)
                        .setPositiveButton(
                            R.string.action_delete
                        ) { _, _ ->
                            deleteFile(manga)
                            notifyDataSet(position, removed = true)
                            excluded = true
                        }.setOnDismissListener {
                            if (!excluded) {
                                mViewModel.add(manga, position)
                                notifyDataSet(position)
                            }
                        }
                        .create()
                dialog.show()
            }
        }

    private fun deleteFile(manga: Manga?) {
        if (manga?.file != null) {
            removeList(manga)
            mViewModel.delete(manga)
            if (manga.file.exists()) {
                val isDeleted = manga.file.delete()
                mLOGGER.info("File deleted ${manga.name}: $isDeleted")
            }
        }
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

}
