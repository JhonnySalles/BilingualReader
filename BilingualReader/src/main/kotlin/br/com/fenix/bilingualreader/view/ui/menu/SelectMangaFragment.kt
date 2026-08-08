package br.com.fenix.bilingualreader.view.ui.menu

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.LibraryMangaType
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.service.listener.MangaCardListener
import br.com.fenix.bilingualreader.service.listener.PopupOrderListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.AdapterUtil.AdapterUtils
import br.com.fenix.bilingualreader.util.helpers.AnimationUtil
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.PopupUtil.PopupUtils
import br.com.fenix.bilingualreader.util.helpers.blurOnceDeferred
import br.com.fenix.bilingualreader.view.adapter.library.BaseAdapter
import br.com.fenix.bilingualreader.view.adapter.library.MangaGridCardAdapter
import br.com.fenix.bilingualreader.view.adapter.library.MangaLineCardAdapter
import br.com.fenix.bilingualreader.view.adapter.library.MangaSeparatorGridCardAdapter
import br.com.fenix.bilingualreader.view.adapter.library.MangaSeparatorLineCardAdapter
import br.com.fenix.bilingualreader.view.adapter.library.MangaSeriesCardAdapter
import br.com.fenix.bilingualreader.view.components.BlurAwareItemAnimator
import br.com.fenix.bilingualreader.view.components.GlassRenderScheduler
import br.com.fenix.bilingualreader.view.ui.library.manga.LibraryMangaPopupOrder
import br.com.fenix.bilingualreader.view.ui.library.manga.LibraryMangaPopupType
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.GlassSetup
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur
import io.supercharge.shimmerlayout.ShimmerLayout
import org.slf4j.LoggerFactory
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min


class SelectMangaFragment : Fragment(), PopupOrderListener {

    private val mLOGGER = LoggerFactory.getLogger(SelectMangaFragment::class.java)

    private val mViewModel: SelectMangaViewModel by viewModels()

    private lateinit var mRoot: ConstraintLayout
    private lateinit var mScrollUp: FloatingActionButton
    private lateinit var mScrollDown: FloatingActionButton
    private lateinit var mRecycler: RecyclerView
    private var mMangaAdapter: BaseAdapter<Manga, MangaCardListener>? = null

    private lateinit var mToolbar: androidx.appcompat.widget.Toolbar
    private lateinit var mBlurTop: BlurView
    private lateinit var mPreferences: android.content.SharedPreferences
    private lateinit var miSearch: MenuItem
    private lateinit var miGridType: MenuItem
    private lateinit var miGridOrder: MenuItem
    private lateinit var searchView: SearchView

    private lateinit var mListener: MangaCardListener

    private val mHandler = Handler(Looper.getMainLooper())

    private val mDismissUpButton = Runnable { mScrollUp.hide() }
    private val mDismissDownButton = Runnable { mScrollDown.hide() }

    private lateinit var mMenuPopupLibrary: FrameLayout
    private lateinit var mMenuPopupLibraryBackground: BlurView
    private lateinit var mPopupLibraryView: ViewPager2
    private lateinit var mPopupLibraryTab: TabLayout
    private lateinit var mPopupOrderFragment: LibraryMangaPopupOrder
    private lateinit var mPopupTypeFragment: LibraryMangaPopupType
    private var _mBottomSheet: BottomSheetBehavior<FrameLayout>? = null
    private val mBottomSheet: BottomSheetBehavior<FrameLayout> get() = _mBottomSheet!!

    private lateinit var mSkeletonLayout: LinearLayout
    private lateinit var mShimmer: ShimmerLayout
    private lateinit var mInflater: LayoutInflater

    private var mSortType: Order = Order.Name
    private var mSortDesc: Boolean = false
    private val mOrderLiveData = MutableLiveData<Pair<Order, Boolean>>(Pair(Order.Name, false))

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

    companion object {
        var mGridType: LibraryMangaType = LibraryMangaType.GRID_BIG
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            mViewModel.clearMangaSelected()

            if (requireArguments().containsKey(GeneralConsts.KEYS.MANGA.ID))
                mViewModel.id = requireArguments().getLong(GeneralConsts.KEYS.MANGA.ID)

            if (requireArguments().containsKey(GeneralConsts.KEYS.MANGA.NAME))
                mViewModel.manga = requireArguments().getString(GeneralConsts.KEYS.MANGA.NAME)!!

            mViewModel.setDefaultLibrary(Libraries.PORTUGUESE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_select_manga, container, false)

        mGridType = try {
            LibraryMangaType.valueOf(
                GeneralConsts.getSharedPreferences(requireContext())
                    .getString(GeneralConsts.KEYS.LIBRARY.MANGA_LIBRARY_TYPE, LibraryMangaType.LINE.toString())
                    .toString()
            )
        } catch (_: Exception) {
            LibraryMangaType.LINE
        }

        mRoot = root.findViewById(R.id.select_manga_root)
        mRecycler = root.findViewById(R.id.select_manga_recycler)
        mScrollUp = root.findViewById(R.id.select_manga_scroll_up)
        mScrollDown = root.findViewById(R.id.select_manga_scroll_down)
        mToolbar = root.findViewById(R.id.toolbar_select_manga)
        mBlurTop = root.findViewById(R.id.select_manga_blur_top)
        mPreferences = GeneralConsts.getSharedPreferences(requireContext())

        mMenuPopupLibrary = root.findViewById(R.id.select_manga_popup_menu_library)
        mMenuPopupLibraryBackground = root.findViewById(R.id.select_manga_popup_header_background)
        mPopupLibraryTab = root.findViewById(R.id.select_manga_popup_library_tab)
        mPopupLibraryView = root.findViewById(R.id.select_manga_popup_library_view_pager)

        mSkeletonLayout = root.findViewById(R.id.skeleton_layout)
        mShimmer = root.findViewById(R.id.shimmer_skeleton)
        mInflater = inflater

        mRecycler.itemAnimator = BlurAwareItemAnimator()

        val theme = Themes.valueOf(mPreferences.getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!)
        MenuUtil.tintToolbar(mToolbar, theme)

        (requireActivity() as MenuActivity).setActionBar(mToolbar)
        setupBlurViews()
        setupTitleBackgrounds()
        setupWindowInsets()

        registerForContextMenu(mToolbar)

        mScrollUp.visibility = View.GONE
        mScrollDown.visibility = View.GONE

        mScrollUp.setOnClickListener {
            setAnimationRecycler(false)
            (mScrollUp.drawable as AnimatedVectorDrawable).start()
            mRecycler.smoothScrollToPosition(0)
        }
        mScrollUp.setOnLongClickListener {
            (mScrollUp.drawable as AnimatedVectorDrawable).start()
            mRecycler.scrollToPosition(0)
            true
        }
        mScrollDown.setOnClickListener {
            setAnimationRecycler(false)
            (mScrollDown.drawable as AnimatedVectorDrawable).start()
            mRecycler.smoothScrollToPosition((mRecycler.adapter as RecyclerView.Adapter).itemCount)
        }
        mScrollDown.setOnLongClickListener {
            (mScrollDown.drawable as AnimatedVectorDrawable).start()
            mRecycler.scrollToPosition((mRecycler.adapter as RecyclerView.Adapter).itemCount - 1)
            true
        }

        mPopupOrderFragment = LibraryMangaPopupOrder()
        mPopupTypeFragment = LibraryMangaPopupType()
        mPopupOrderFragment.setListener(this)

        BottomSheetBehavior.from(mMenuPopupLibrary).apply {
            peekHeight = 255
            this.state = BottomSheetBehavior.STATE_COLLAPSED
            _mBottomSheet = this
        }
        mBottomSheet.isDraggable = true
        mBottomSheet.addBottomSheetCallback(mBottomSheetCallback)

        PopupUtils.onPopupTouch(requireActivity(), mMenuPopupLibrary, mBottomSheet, root.findViewById<View>(R.id.select_manga_popup_menu_order_filter_touch))

        val viewPagerAdapter = ViewPagerAdapter(this)
        viewPagerAdapter.addFragment(mPopupTypeFragment, resources.getString(R.string.popup_library_manga_tab_item_type))
        viewPagerAdapter.addFragment(mPopupOrderFragment, resources.getString(R.string.popup_library_manga_tab_item_ordering))

        mPopupLibraryView.adapter = viewPagerAdapter
        TabLayoutMediator(mPopupLibraryTab, mPopupLibraryView) { tab, position ->
            tab.text = viewPagerAdapter.getPageTitle(position)
        }.attach()

        mRecycler.setOnScrollChangeListener { _, _, _, _, yOld ->
            if (yOld > 20) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (mHandler.hasCallbacks(mDismissDownButton))
                        mHandler.removeCallbacks(mDismissDownButton)
                } else
                    mHandler.removeCallbacks(mDismissDownButton)

                mScrollDown.hide()
            } else if (yOld < -20) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (mHandler.hasCallbacks(mDismissUpButton))
                        mHandler.removeCallbacks(mDismissUpButton)
                } else
                    mHandler.removeCallbacks(mDismissUpButton)

                mScrollUp.hide()
            }

            if (yOld > 150) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (mHandler.hasCallbacks(mDismissUpButton))
                        mHandler.removeCallbacks(mDismissUpButton)
                } else
                    mHandler.removeCallbacks(mDismissUpButton)

                mHandler.postDelayed(mDismissUpButton, 3000)
                mScrollUp.show()
            } else if (yOld < -150) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (mHandler.hasCallbacks(mDismissUpButton))
                        mHandler.removeCallbacks(mDismissUpButton)
                } else
                    mHandler.removeCallbacks(mDismissUpButton)

                mHandler.postDelayed(mDismissDownButton, 3000)
                mScrollDown.show()
            }
        }

        mRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState != RecyclerView.SCROLL_STATE_SETTLING)
                    setAnimationRecycler(true)

                val isGlass = mPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
                val isPopupVisible = _mBottomSheet != null && mBottomSheet.state != BottomSheetBehavior.STATE_HIDDEN
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (::mBlurTop.isInitialized) {
                        mBlurTop.setBlurAutoUpdate(false)
                        GlassRenderScheduler.requestUpdate(mBlurTop)
                        mBlurTop.blurOnceDeferred(mHandler, 50)
                    }
                    if (isGlass && ::mMenuPopupLibraryBackground.isInitialized) {
                        mMenuPopupLibraryBackground.setBlurAutoUpdate(false)
                        GlassRenderScheduler.requestUpdate(mMenuPopupLibraryBackground)
                    }
                } else {
                    if (::mBlurTop.isInitialized) {
                        mBlurTop.setBlurAutoUpdate(true)
                        if (isGlass) {
                            GlassRenderScheduler.setScrollRateCap(mBlurTop, true)
                        }
                    }
                    if (isGlass && isPopupVisible && ::mMenuPopupLibraryBackground.isInitialized) {
                        mMenuPopupLibraryBackground.setBlurAutoUpdate(true)
                    }
                    if (isGlass && ::mMenuPopupLibraryBackground.isInitialized) {
                        GlassRenderScheduler.setScrollRateCap(mMenuPopupLibraryBackground, true)
                    }
                }
            }
        })

        mListener = object : MangaCardListener {
            override fun onClick(manga: Manga, root: View) {
                val bundle = Bundle()
                bundle.putSerializable(GeneralConsts.KEYS.OBJECT.MANGA, manga)
                (requireActivity() as MenuActivity).onBack(bundle)
            }

            override fun onClickFavorite(manga: Manga) { }
            override fun onClickConfig(manga: Manga, root: View, item: View, position: Int) { }
            override fun onClickLong(manga: Manga, view: View, position: Int) {}
        }

        showSkeleton(true)
        observer()
        mViewModel.list(mViewModel.id, mViewModel.manga) {
            showSkeleton(false)
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        generateLayout(mGridType)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.menu_select_manga, menu)

                miGridType = menu.findItem(R.id.menu_manga_library_type)
                miGridOrder = menu.findItem(R.id.menu_manga_library_list_order)
                miSearch = menu.findItem(R.id.menu_select_manga_search)

                searchView = miSearch.actionView as SearchView
                searchView.imeOptions = EditorInfo.IME_ACTION_DONE
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean = false

                    override fun onQueryTextChange(newText: String?): Boolean {
                        mViewModel.filter.filter(newText)
                        return false
                    }
                })

                val searchSrcTextView = miSearch.actionView!!.findViewById<View>(Resources.getSystem().getIdentifier("search_src_text", "id", "android")) as AutoCompleteTextView
                searchSrcTextView.setTextAppearance(R.style.SearchShadow)

                val iconGrid: Int = when (mGridType) {
                    LibraryMangaType.GRID_SMALL -> R.drawable.ico_animated_type_grid_gridsmall_exit
                    LibraryMangaType.GRID_BIG -> R.drawable.ico_animated_type_grid_gridbig_exit
                    LibraryMangaType.GRID_MEDIUM -> R.drawable.ico_animated_type_grid_gridmedium_exit
                    LibraryMangaType.SEPARATOR_BIG -> R.drawable.ico_animated_type_grid_gridbig_separator_exit
                    LibraryMangaType.SEPARATOR_MEDIUM -> R.drawable.ico_animated_type_grid_gridmedium_separator_exit
                    LibraryMangaType.SEPARATOR_CAROUSEL -> R.drawable.ico_animated_type_grid_carousel_exit
                    LibraryMangaType.SEPARATOR_LINE -> R.drawable.ico_animated_type_grid_list_separator_exit
                    LibraryMangaType.LINE -> R.drawable.ico_animated_type_grid_list_exit
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

                MenuUtil.longClick(requireActivity(), R.id.menu_manga_library_list_order) {
                    onOpenMenuLibrary(1)
                }

                MenuUtil.longClick(requireActivity(), R.id.menu_manga_library_type) {
                    onOpenMenuLibrary(0)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_manga_library_type -> {
                        onChangeLayoutNext()
                        true
                    }
                    R.id.menu_manga_library_list_order -> {
                        onChangeSortNext()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun onOpenMenuLibrary(select: Int = 0) {
        mPopupLibraryTab.selectTab(mPopupLibraryTab.getTabAt(select))
        mBottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
        AnimationUtil.animatePopupOpen(requireActivity(), mMenuPopupLibrary)
    }

    private fun onChangeSortNext() {
        val orderBy = when (mSortType) {
            Order.Name -> Order.Date
            Order.Date -> Order.Favorite
            Order.Favorite -> Order.LastAccess
            Order.LastAccess -> Order.Genre
            Order.Genre -> Order.Author
            Order.Author -> Order.Series
            else -> Order.Name
        }
        popupSorted(orderBy)
    }

    private fun onChangeLayoutNext() {
        val nextType = when (mGridType) {
            LibraryMangaType.GRID_SMALL -> LibraryMangaType.GRID_MEDIUM
            LibraryMangaType.GRID_MEDIUM -> LibraryMangaType.GRID_BIG
            LibraryMangaType.GRID_BIG -> LibraryMangaType.LINE
            LibraryMangaType.LINE -> LibraryMangaType.SEPARATOR_LINE
            LibraryMangaType.SEPARATOR_LINE -> LibraryMangaType.SEPARATOR_MEDIUM
            LibraryMangaType.SEPARATOR_MEDIUM -> LibraryMangaType.SEPARATOR_BIG
            LibraryMangaType.SEPARATOR_BIG -> LibraryMangaType.SEPARATOR_CAROUSEL
            LibraryMangaType.SEPARATOR_CAROUSEL -> LibraryMangaType.GRID_SMALL
        }
        onChangeLayout(nextType)
    }

    private fun onChangeLayout(type: LibraryMangaType) {
        onChangeIconLayout(type)
        generateLayout(type)
        setAnimationRecycler(true)
        mViewModel.listMangas.value?.let { updateList(it) }
    }

    private fun onChangeIconLayout(type: LibraryMangaType) {
        if (!::miGridType.isInitialized)
            return

        val initial: Int? = when (mGridType) {
            LibraryMangaType.GRID_SMALL -> R.drawable.ico_animated_type_grid_gridsmall_exit
            LibraryMangaType.GRID_BIG -> R.drawable.ico_animated_type_grid_gridbig_exit
            LibraryMangaType.GRID_MEDIUM -> R.drawable.ico_animated_type_grid_gridmedium_exit
            LibraryMangaType.SEPARATOR_BIG -> R.drawable.ico_animated_type_grid_gridbig_separator_exit
            LibraryMangaType.SEPARATOR_MEDIUM -> R.drawable.ico_animated_type_grid_gridmedium_separator_exit
            LibraryMangaType.SEPARATOR_CAROUSEL -> R.drawable.ico_animated_type_grid_carousel_exit
            LibraryMangaType.SEPARATOR_LINE -> R.drawable.ico_animated_type_grid_list_separator_exit
            LibraryMangaType.LINE -> R.drawable.ico_animated_type_grid_list_exit
        }

        val final: Int? = when (type) {
            LibraryMangaType.GRID_SMALL -> R.drawable.ico_animated_type_grid_gridsmall_enter
            LibraryMangaType.GRID_BIG -> R.drawable.ico_animated_type_grid_gridbig_enter
            LibraryMangaType.GRID_MEDIUM -> R.drawable.ico_animated_type_grid_gridmedium_enter
            LibraryMangaType.SEPARATOR_BIG -> R.drawable.ico_animated_type_grid_gridbig_separator_enter
            LibraryMangaType.SEPARATOR_MEDIUM -> R.drawable.ico_animated_type_grid_gridmedium_separator_enter
            LibraryMangaType.SEPARATOR_CAROUSEL -> R.drawable.ico_animated_type_grid_carousel_enter
            LibraryMangaType.SEPARATOR_LINE -> R.drawable.ico_animated_type_grid_list_separator_enter
            LibraryMangaType.LINE -> R.drawable.ico_animated_type_grid_list_enter
        }

        if (initial != null && final != null)
            MenuUtil.animatedSequenceDrawable(miGridType, initial, final)

        mGridType = type
    }

    private fun onChangeIconSort(order: Order, isDesc: Boolean?) {
        if (!::miGridOrder.isInitialized)
            return

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

    private fun generateLayout(type: LibraryMangaType) {
        when (type) {
            LibraryMangaType.LINE -> {
                val lineAdapter = MangaLineCardAdapter()
                mMangaAdapter = lineAdapter
                mRecycler.adapter = lineAdapter
                mRecycler.layoutManager = GridLayoutManager(requireContext(), 1)
                lineAdapter.attachListener(mListener)
                mRecycler.layoutAnimation = null
            }
            LibraryMangaType.SEPARATOR_LINE -> {
                val lineAdapter = MangaSeparatorLineCardAdapter(requireContext())
                mMangaAdapter = lineAdapter
                mRecycler.adapter = lineAdapter
                mRecycler.layoutManager = GridLayoutManager(requireContext(), 1)
                lineAdapter.attachListener(mListener)
                mRecycler.layoutAnimation = null
            }
            LibraryMangaType.SEPARATOR_CAROUSEL -> {
                val seriesAdapter = MangaSeriesCardAdapter(requireContext())
                mMangaAdapter = seriesAdapter
                mRecycler.adapter = seriesAdapter
                mRecycler.layoutManager = GridLayoutManager(requireContext(), 1)
                seriesAdapter.attachListener(mListener)
                mRecycler.layoutAnimation = null
            }
            else -> {
                val gridAdapter = when (type) {
                    LibraryMangaType.SEPARATOR_BIG,
                    LibraryMangaType.SEPARATOR_MEDIUM -> MangaSeparatorGridCardAdapter(requireContext(), type)
                    else -> MangaGridCardAdapter(type)
                }
                mMangaAdapter = gridAdapter
                mRecycler.adapter = gridAdapter
                mRecycler.layoutManager = getGridLayout()
                gridAdapter.attachListener(mListener)
                mRecycler.layoutAnimation = null
            }
        }
    }

    private fun setAnimationRecycler(isAnimate: Boolean) {
        mMangaAdapter?.isAnimation = isAnimate
    }

    private fun updateList(list: MutableList<Manga>) {
        mMangaAdapter?.updateList(mSortType, list)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        for (library in mViewModel.getLibraryList())
            menu.add(library.title).setOnMenuItemClickListener { _: MenuItem? ->
                changeLibrary(library)
                true
            }
    }

    override fun onResume() {
        super.onResume()
        titleLibrary()
        setupTitleBackgrounds()
        val isGlass = mPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
        if (::mBlurTop.isInitialized) {
            mBlurTop.setBlurEnabled(isGlass)
            if (isGlass) {
                mBlurTop.setBlurAutoUpdate(true)
                mHandler.postDelayed({ mBlurTop.setBlurAutoUpdate(false) }, 100)
            } else {
                mBlurTop.setBlurAutoUpdate(false)
            }
        }
        setupPopupBackgrounds()
        mMenuPopupLibraryBackground.setBlurEnabled(isGlass)
    }

    override fun onPause() {
        if (::mBlurTop.isInitialized) {
            mBlurTop.setBlurAutoUpdate(false)
            mBlurTop.setBlurEnabled(false)
        }
        if (_mBottomSheet != null && mBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        if (::mMenuPopupLibraryBackground.isInitialized) {
            mMenuPopupLibraryBackground.setBlurAutoUpdate(false)
            mMenuPopupLibraryBackground.setBlurEnabled(false)
        }
        super.onPause()
    }

    private fun changeLibrary(library: Library) {
        mViewModel.changeLibrary(library)
        titleLibrary()
    }

    private fun titleLibrary() {
        mToolbar.title = mViewModel.getLibrary().title
    }

    private fun observer() {
        mViewModel.listMangas.observe(viewLifecycleOwner) {
            updateList(it)
        }
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mHandler.hasCallbacks(mDismissUpButton))
                mHandler.removeCallbacks(mDismissUpButton)
            if (mHandler.hasCallbacks(mDismissDownButton))
                mHandler.removeCallbacks(mDismissDownButton)
        } else {
            mHandler.removeCallbacks(mDismissUpButton)
            mHandler.removeCallbacks(mDismissDownButton)
        }

        super.onDestroy()
    }

    override fun onDestroyView() {
        (mRecycler.itemAnimator as? BlurAwareItemAnimator)?.destroy()
        mMangaAdapter = null
        _mBottomSheet?.removeBottomSheetCallback(mBottomSheetCallback)
        _mBottomSheet = null
        super.onDestroyView()
    }

    private fun getGridLayout(): RecyclerView.LayoutManager {
        val type = mGridType
        val typeWidth = when (type) {
            LibraryMangaType.SEPARATOR_MEDIUM -> LibraryMangaType.GRID_MEDIUM
            LibraryMangaType.SEPARATOR_BIG -> LibraryMangaType.GRID_BIG
            else -> type
        }
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val columnWidth: Int = AdapterUtils.getMangaCardSize(requireContext(), typeWidth, isLandscape).first + 1
        val spaceCount: Int = max(1, (Resources.getSystem().displayMetrics.widthPixels - 3) / columnWidth)
        return when (type) {
            LibraryMangaType.SEPARATOR_BIG,
            LibraryMangaType.SEPARATOR_MEDIUM -> StaggeredGridLayoutManager(spaceCount, StaggeredGridLayoutManager.VERTICAL)
            else -> GridLayoutManager(requireContext(), spaceCount)
        }
    }

    private fun setupBlurViews() {
        if (!::mBlurTop.isInitialized)
            return
        val context = requireContext()
        val decorView = requireActivity().window.decorView
        val background = decorView.background ?: ColorDrawable(android.graphics.Color.BLACK)
        val blurAlgorithm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) RenderEffectBlur() else RenderScriptBlur(context)
        val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)
        GlassSetup.setupGlass(mBlurTop, rootView, blurAlgorithm)
            .setFrameClearDrawable(background)
            .setBlurRadius(15f)
    }

    private fun setupPopupBackgrounds() {
        if (!::mMenuPopupLibraryBackground.isInitialized)
            return
        val context = requireContext()
        val decorView = requireActivity().window.decorView
        val background = decorView.background ?: ColorDrawable(android.graphics.Color.BLACK)
        val blurAlgorithm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) RenderEffectBlur() else RenderScriptBlur(context)
        val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)
        GlassSetup.setupGlass(mMenuPopupLibraryBackground, rootView, blurAlgorithm)
            .setFrameClearDrawable(background)
            .setBlurRadius(15f)
    }

    private fun setupTitleBackgrounds() {
        if (!::mBlurTop.isInitialized)
            return
        val barLayout = view?.findViewById<View>(R.id.content_toolbar_select_manga)
        val activity = activity ?: return
        MenuUtil.setupToolbar(activity, mToolbar, mBlurTop, barLayout)
    }

    private fun setupWindowInsets() {
        if (!::mBlurTop.isInitialized)
            return
        val rootView = view ?: return
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            mBlurTop.setPadding(mBlurTop.paddingLeft, systemBars.top, mBlurTop.paddingRight, mBlurTop.paddingBottom)
            
            mBlurTop.post {
                val recycler = rootView.findViewById<RecyclerView>(R.id.select_manga_recycler)
                recycler?.setPadding(recycler.paddingLeft, mBlurTop.height, recycler.paddingRight, systemBars.bottom)
            }
            
            insets
        }
    }

    override fun popupOrderOnChange() {
        mViewModel.listMangas.value?.let { updateList(it) }
    }

    override fun popupSorted(order: Order) {
        onChangeIconSort(order, null)
        mOrderLiveData.value = Pair(mSortType, mSortDesc)
        popupOrderOnChange()
    }

    override fun popupSorted(order: Order, isDesc: Boolean) {
        onChangeIconSort(order, isDesc)
        mOrderLiveData.value = Pair(mSortType, mSortDesc)
        popupOrderOnChange()
    }

    override fun popupGetOrder(): Pair<Order, Boolean>? {
        return mOrderLiveData.value
    }

    override fun popupGetObserver(): LiveData<Pair<Order, Boolean>> {
        return mOrderLiveData
    }

    private fun getSkeletonTitleHeight(): Int =
        resources.getDimension(R.dimen.manga_grid_skeleton_title_height).toInt()

    private fun getSkeletonContentRowHeight(type: LibraryMangaType): Int {
        val resource = when (type) {
            LibraryMangaType.LINE,
            LibraryMangaType.SEPARATOR_LINE -> R.dimen.manga_line_skeleton_height
            LibraryMangaType.SEPARATOR_CAROUSEL -> R.dimen.manga_carousel_skeleton_height
            LibraryMangaType.SEPARATOR_BIG -> R.dimen.manga_grid_skeleton_height_separator_big
            LibraryMangaType.SEPARATOR_MEDIUM -> R.dimen.manga_grid_skeleton_height_separator_medium
            LibraryMangaType.GRID_BIG -> R.dimen.manga_grid_skeleton_height_big
            LibraryMangaType.GRID_MEDIUM -> R.dimen.manga_grid_skeleton_height_big
            LibraryMangaType.GRID_SMALL -> R.dimen.manga_grid_skeleton_height_small
        }
        return resources.getDimension(resource).toInt()
    }

    private fun getSkeletonRowCount(type: LibraryMangaType): Int {
        val pxHeight = Resources.getSystem().displayMetrics.heightPixels
        val rowHeight = getSkeletonContentRowHeight(type)
        return max(1, ceil((pxHeight / rowHeight.toDouble())).toInt())
    }

    private fun getSkeletonGroupCount(type: LibraryMangaType, contentRowsPerGroup: Int = 1): Int {
        val pxHeight = Resources.getSystem().displayMetrics.heightPixels
        val groupHeight = getSkeletonTitleHeight() + (getSkeletonContentRowHeight(type) * contentRowsPerGroup)
        return max(2, min(3, ceil((pxHeight / groupHeight.toDouble())).toInt()))
    }

    private fun getSkeletonCarouselItemPerRow(): Int {
        val itemWidth = resources.getDimension(R.dimen.manga_carousel_skeleton_item_width).toInt()
        val margin = resources.getDimension(R.dimen.manga_carousel_skeleton_item_margin).toInt()
        return max(1, Resources.getSystem().displayMetrics.widthPixels / (itemWidth + margin))
    }

    private fun addCarouselSkeletonRow(itemCount: Int) {
        val row = mInflater.inflate(R.layout.line_card_manga_carousel_skeleton, null)
        val container = row.findViewById<LinearLayout>(R.id.carousel_skeleton_items)
        val width = resources.getDimension(R.dimen.manga_carousel_skeleton_item_width).toInt()
        val height = resources.getDimension(R.dimen.manga_carousel_skeleton_item_height).toInt()
        val margin = resources.getDimension(R.dimen.manga_carousel_skeleton_item_margin).toInt()
        container.removeAllViews()
        for (idx in 0 until itemCount) {
            val item = mInflater.inflate(R.layout.line_card_manga_carousel_skeleton_item, null)
            val params = LinearLayout.LayoutParams(width, height)
            params.marginEnd = margin
            item.layoutParams = params
            container.addView(item)
        }
        mSkeletonLayout.addView(row)
    }

    private fun getSkeletonGridItemPerRow(type: LibraryMangaType): Int {
        val typeWidth = when (type) {
            LibraryMangaType.SEPARATOR_MEDIUM -> LibraryMangaType.GRID_MEDIUM
            LibraryMangaType.SEPARATOR_BIG -> LibraryMangaType.GRID_BIG
            else -> type
        }
        val columnWidth = getSkeletonItemWidth(typeWidth) + 1
        return max(1, (Resources.getSystem().displayMetrics.widthPixels - 3) / columnWidth.toInt())
    }

    private fun getSkeletonItemHeight(type: LibraryMangaType): Int {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        return AdapterUtils.getMangaCardSize(requireContext(), type, isLandscape).second
    }

    private fun getSkeletonItemWidth(type: LibraryMangaType): Int {
        val typeWidth = when (type) {
            LibraryMangaType.SEPARATOR_MEDIUM -> LibraryMangaType.GRID_MEDIUM
            LibraryMangaType.SEPARATOR_BIG -> LibraryMangaType.GRID_BIG
            else -> type
        }
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        return AdapterUtils.getMangaCardSize(requireContext(), typeWidth, isLandscape).first
    }

    private fun addGridSkeletonRow(type: LibraryMangaType) {
        val row = mInflater.inflate(R.layout.grid_card_manga_skeleton, null)
        val container = row.findViewById<LinearLayout>(R.id.grid_skeleton_items)
        val height = getSkeletonItemHeight(type)
        val width = getSkeletonItemWidth(type)
        val margin = resources.getDimension(R.dimen.manga_grid_skeleton_divider).toInt()
        val items = getSkeletonGridItemPerRow(type)
        val divider = ((Resources.getSystem().displayMetrics.widthPixels.toFloat() - (items * (width + margin))) / items).toInt()
        container.removeAllViews()
        for (idx in 0..items) {
            val item = mInflater.inflate(R.layout.grid_card_manga_skeleton_item, null)
            val params = FrameLayout.LayoutParams(width, height)
            params.setMargins(margin, margin, divider, 0)
            item.layoutParams = params
            container.addView(item)
        }
        container.invalidate()
        mSkeletonLayout.addView(row)
    }

    private fun addMangaSkeletonTitle() {
        mSkeletonLayout.addView(mInflater.inflate(R.layout.grid_card_manga_skeleton_title, null))
    }

    private fun showSkeleton(show: Boolean) {
        if (view == null) return
        if (show) {
            mSkeletonLayout.alpha = 1f
            mSkeletonLayout.removeAllViews()

            val type = mGridType

            when (type) {
                LibraryMangaType.LINE -> {
                    for (i in 0 until getSkeletonRowCount(type))
                        mSkeletonLayout.addView(mInflater.inflate(R.layout.line_card_manga_skeleton, null))
                }
                LibraryMangaType.SEPARATOR_LINE -> {
                    val lineItemsPerGroup = 2
                    val groups = getSkeletonGroupCount(type, lineItemsPerGroup)
                    repeat(groups) {
                        addMangaSkeletonTitle()
                        repeat(lineItemsPerGroup) {
                            mSkeletonLayout.addView(mInflater.inflate(R.layout.line_card_manga_skeleton, null))
                        }
                    }
                }
                LibraryMangaType.SEPARATOR_CAROUSEL -> {
                    val full = getSkeletonCarouselItemPerRow()
                    val counts = listOf(full, max(1, full - 1), max(1, full - 2))
                    val groups = getSkeletonGroupCount(type)
                    for (i in 0 until groups) {
                        addMangaSkeletonTitle()
                        addCarouselSkeletonRow(counts[i])
                    }
                }
                LibraryMangaType.SEPARATOR_BIG,
                LibraryMangaType.SEPARATOR_MEDIUM -> {
                    val groups = getSkeletonGroupCount(type)
                    repeat(groups) {
                        addMangaSkeletonTitle()
                        addGridSkeletonRow(type)
                    }
                }
                else -> {
                    for (i in 0 until getSkeletonRowCount(type))
                        addGridSkeletonRow(type)
                }
            }

            mRecycler.animate().cancel()
            mSkeletonLayout.animate().cancel()

            mShimmer.visibility = View.VISIBLE
            mRecycler.visibility = View.GONE
            mRecycler.alpha = 1f
            mSkeletonLayout.visibility = View.VISIBLE
            mShimmer.startShimmerAnimation()
            mSkeletonLayout.bringToFront()
        } else {
            mShimmer.stopShimmerAnimation()
            mShimmer.visibility = View.GONE
            mSkeletonLayout.visibility = View.GONE
            mSkeletonLayout.alpha = 1f
            mRecycler.visibility = View.VISIBLE
            mRecycler.alpha = 1f
            setAnimationRecycler(true)
        }
    }

    inner class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        private val fragments: MutableList<Fragment> = ArrayList()
        private val fragmentTitle: MutableList<String> = ArrayList()
        fun addFragment(fragment: Fragment, title: String) {
            fragments.add(fragment)
            fragmentTitle.add(title)
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

}