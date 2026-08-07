package br.com.fenix.bilingualreader.view.ui.vocabulary


import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.functions.InitializeVocabulary
import br.com.fenix.bilingualreader.service.listener.PopupOrderListener
import br.com.fenix.bilingualreader.service.listener.VocabularyCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.AnimationUtil
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.PopupUtil.PopupUtils
import br.com.fenix.bilingualreader.util.helpers.blurOnceDeferred
import br.com.fenix.bilingualreader.view.adapter.vocabulary.VocabularyCardAdapter
import br.com.fenix.bilingualreader.view.adapter.vocabulary.VocabularyLoadState
import br.com.fenix.bilingualreader.view.adapter.vocabulary.VocabularyMangaListCardAdapter
import br.com.fenix.bilingualreader.view.components.ComponentsUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import eightbitlab.com.blurview.BlurView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory


class VocabularyFragment : Fragment(), PopupOrderListener, SwipeRefreshLayout.OnRefreshListener,
    InitializeVocabulary<Vocabulary> {

    private val mLOGGER = LoggerFactory.getLogger(VocabularyFragment::class.java)

    private val mViewModel: VocabularyViewModel by viewModels()

    private lateinit var mRoot: CoordinatorLayout
    private lateinit var mRefreshLayout: SwipeRefreshLayout
    private lateinit var mScrollUp: FloatingActionButton
    private lateinit var mScrollDown: FloatingActionButton
    private lateinit var mRecyclerView: RecyclerView

    private lateinit var mFavorite: MenuItem
    private lateinit var miOrder: MenuItem
    private lateinit var miSearch: MenuItem
    private lateinit var searchView: SearchView

    private lateinit var mMenuPopupFilterOrder: FrameLayout
    private lateinit var mMenuPopupLibraryBackground: BlurView
    private lateinit var mPopupFilterOrderView: ViewPager2
    private lateinit var mPopupFilterOrderTab: TabLayout
    private lateinit var mPopupOrderFragment: VocabularyPopupOrder
    private lateinit var mBottomSheet: BottomSheetBehavior<FrameLayout>

    private val mBottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            val ctx = context ?: return
            val sharedPreferences = GeneralConsts.getSharedPreferences(ctx)
            val isGlass = sharedPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
            if (isGlass && ::mMenuPopupLibraryBackground.isInitialized) {
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
            val ctx = context ?: return
            val sharedPreferences = GeneralConsts.getSharedPreferences(ctx)
            val isGlass = sharedPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
            if (isGlass && ::mMenuPopupLibraryBackground.isInitialized) {
                mMenuPopupLibraryBackground.setBlurAutoUpdate(true)
            }
        }
    }

    private lateinit var mListener: VocabularyCardListener

    private lateinit var mMapOrder: HashMap<Order, String>
    private val mHandler = Handler(Looper.getMainLooper())
    private val mDismissUpButton = Runnable { mScrollUp.hide() }
    private val mDismissDownButton = Runnable { mScrollDown.hide() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_vocabulary, container, false)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val contentLayout = root.findViewById<View>(R.id.vocabulary_refresh)
            contentLayout?.setPadding(contentLayout.paddingLeft, contentLayout.paddingTop, contentLayout.paddingRight, navBarHeight)
            val popupLayout = root.findViewById<View>(R.id.vocabulary_popup_menu_order_filter)
            popupLayout?.setPadding(popupLayout.paddingLeft, popupLayout.paddingTop, popupLayout.paddingRight, navBarHeight)
            insets
        }

        mMapOrder = hashMapOf(
            Order.Description to getString(R.string.config_option_vocabulary_order_description),
            Order.Frequency to getString(R.string.config_option_vocabulary_order_frequency),
            Order.Favorite to getString(R.string.config_option_vocabulary_order_favorite)
        )

        mRoot = root.findViewById(R.id.vocabulary_root)
        mRecyclerView = root.findViewById(R.id.vocabulary_recycler)
        mRefreshLayout = root.findViewById(R.id.vocabulary_refresh)

        mScrollUp = root.findViewById(R.id.vocabulary_scroll_up)
        mScrollDown = root.findViewById(R.id.vocabulary_scroll_down)

        mMenuPopupFilterOrder = root.findViewById(R.id.vocabulary_popup_menu_order_filter)
        mMenuPopupLibraryBackground = root.findViewById(R.id.vocabulary_popup_header_background)

        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
                val isGlass = sharedPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
                val isPopupVisible = ::mBottomSheet.isInitialized && mBottomSheet.state != BottomSheetBehavior.STATE_HIDDEN
                if (isGlass) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        (activity as? br.com.fenix.bilingualreader.MainActivity)?.setBlurAutoUpdate(false)
                        if (::mMenuPopupLibraryBackground.isInitialized) {
                            mMenuPopupLibraryBackground.setBlurAutoUpdate(false)
                        }
                    } else {
                        (activity as? br.com.fenix.bilingualreader.MainActivity)?.setBlurAutoUpdate(true)
                        if (isPopupVisible && ::mMenuPopupLibraryBackground.isInitialized) {
                            mMenuPopupLibraryBackground.setBlurAutoUpdate(true)
                        }
                    }
                }
            }
        })

        mPopupFilterOrderTab = root.findViewById(R.id.vocabulary_popup_order_filter_tab)
        mPopupFilterOrderView = root.findViewById(R.id.vocabulary_popup_order_filter_view_pager)

        mScrollUp.visibility = View.GONE
        mScrollDown.visibility = View.GONE

        mViewModel.isQuery.observe(viewLifecycleOwner) {
            mRefreshLayout.isRefreshing = it
        }

        ComponentsUtil.setThemeColor(requireContext(), mRefreshLayout)
        mRefreshLayout.setOnRefreshListener(this)
        mRefreshLayout.isEnabled = true

        mScrollUp.setOnClickListener {
            (mScrollUp.drawable as AnimatedVectorDrawable).start()
            mRecyclerView.smoothScrollToPosition(0)
        }
        mScrollUp.setOnLongClickListener {
            (mScrollUp.drawable as AnimatedVectorDrawable).start()
            mRecyclerView.scrollToPosition(0)
            true
        }
        mScrollDown.setOnClickListener {
            (mScrollDown.drawable as AnimatedVectorDrawable).start()
            mRecyclerView.smoothScrollToPosition((mRecyclerView.adapter as RecyclerView.Adapter).itemCount)
        }
        mScrollDown.setOnLongClickListener {
            (mScrollDown.drawable as AnimatedVectorDrawable).start()
            mRecyclerView.scrollToPosition((mRecyclerView.adapter as RecyclerView.Adapter).itemCount -1)
            true
        }

        mRecyclerView.setOnScrollChangeListener { _, _, _, _, yOld ->
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
                    if (mHandler.hasCallbacks(mDismissDownButton))
                        mHandler.removeCallbacks(mDismissDownButton)
                } else
                    mHandler.removeCallbacks(mDismissDownButton)

                mHandler.postDelayed(mDismissDownButton, 3000)
                mScrollDown.show()
            }
        }
        mPopupOrderFragment = VocabularyPopupOrder()
        mPopupOrderFragment.setListener(this)

        BottomSheetBehavior.from(mMenuPopupFilterOrder).apply {
            peekHeight = 195
            this.state = BottomSheetBehavior.STATE_COLLAPSED
            mBottomSheet = this
        }
        mBottomSheet.addBottomSheetCallback(mBottomSheetCallback)

        PopupUtils.onPopupTouch(requireActivity(), mMenuPopupFilterOrder, mBottomSheet, root.findViewById<ImageView>(R.id.vocabulary_popup_menu_order_filter_touch))

        val viewOrderPagerAdapter = ViewPagerAdapter(this)
        viewOrderPagerAdapter.addFragment(
            mPopupOrderFragment,
            resources.getString(R.string.popup_vocabulary_tab_item_ordering)
        )

        mPopupFilterOrderView.adapter = viewOrderPagerAdapter
        TabLayoutMediator(mPopupFilterOrderTab, mPopupFilterOrderView) { tab, position ->
            tab.text = viewOrderPagerAdapter.getPageTitle(position)
        }.attach()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_vocabulary, menu)

                mFavorite = menu.findItem(R.id.menu_vocabulary_favorite)

                setFavorite(mViewModel.getFavorite())
                mFavorite.setOnMenuItemClickListener {
                    val favorite = !mViewModel.getFavorite()
                    mFavorite.setIcon(if (favorite) R.drawable.ico_animated_favorited_marked else R.drawable.ico_animated_favorited_unmarked)
                    (mFavorite.icon as AnimatedVectorDrawable).start()
                    mViewModel.setQueryFavorite(favorite)
                    true
                }

                miOrder = menu.findItem(R.id.menu_vocabulary_list_order)
                miOrder.setOnMenuItemClickListener {
                    mViewModel.setQueryOrder(!mViewModel.getOrder().second)
                    true
                }

                miSearch = menu.findItem(R.id.menu_vocabulary_search)
                searchView = miSearch.actionView as SearchView
                searchView.imeOptions = EditorInfo.IME_ACTION_DONE
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        mViewModel.setQuery(
                            query ?: ""
                        )
                        return false
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        return false
                    }
                })

                val searchSrcTextView = miSearch.actionView!!.findViewById<View>(Resources.getSystem().getIdentifier("search_src_text", "id", "android")) as AutoCompleteTextView
                searchSrcTextView.setTextAppearance(R.style.SearchShadow)

                MenuUtil.longClick(requireActivity(), R.id.menu_vocabulary_list_order) {
                    if (!mRefreshLayout.isRefreshing)
                        onOpenMenuSort()
                }

                miOrder.setOnMenuItemClickListener {
                    onChangeSort()
                    true
                }

                mViewModel.order.observe(viewLifecycleOwner) {
                    if (it.first == VocabularyActivity.mSortType && it.second == VocabularyActivity.mSortDesc)
                        return@observe

                    val isChange = VocabularyActivity.mSortType != it.first
                    onChangeIconSort(it.first, it.second, isChange)
                }

                if (VocabularyActivity.mVocabularySelect.isNotEmpty()) {
                    searchView.setQuery(VocabularyActivity.mVocabularySelect, true)
                    searchView.isIconified = false
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        mListener = object : VocabularyCardListener {
            override fun onClick(vocabulary: Vocabulary) {
                if (mMenuPopupFilterOrder.visibility != View.GONE)
                    mMenuPopupFilterOrder.visibility = View.GONE
            }

            override fun onClickLong(vocabulary: Vocabulary, view: View, position: Int) {
            }

            override fun onClickFavorite(vocabulary: Vocabulary) {
                mViewModel.update(vocabulary)
            }
        }

        val adapter = VocabularyCardAdapter(mListener)
        mRecyclerView.adapter = adapter.withLoadStateFooter(VocabularyLoadState())
        mRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            mViewModel.vocabularyPager().collectLatest {
                adapter.submitData(it)
            }
        }
    }

    private fun onChangeIconSort(order: Order, isDesc: Boolean, isChange: Boolean) {
        if (!::miOrder.isInitialized)
            return

        if (!isChange) {
            val icon: Int? = when (order) {
                Order.Description -> if (isDesc) R.drawable.ico_animated_sort_to_desc_name else R.drawable.ico_animated_sort_to_asc_name
                Order.Favorite -> if (isDesc) R.drawable.ico_animated_sort_to_desc_favorited else R.drawable.ico_animated_sort_to_asc_favorited
                Order.Frequency -> if (isDesc) R.drawable.ico_animated_sort_to_desc_frequency else R.drawable.ico_animated_sort_to_asc_frequency
                else -> null
            }
            if (icon != null)
                MenuUtil.animatedSequenceDrawable(miOrder, icon)
        } else {
            val initial: Int? = if (VocabularyActivity.mSortDesc)
                when (VocabularyActivity.mSortType) {
                    Order.Description -> R.drawable.ico_animated_sort_desc_to_asc_ico_exit_name
                    Order.Favorite -> R.drawable.ico_animated_sort_desc_to_asc_ico_exit_favorited
                    Order.Frequency -> R.drawable.ico_animated_sort_desc_to_asc_ico_exit_frequency
                    else -> null
                } else
                when (VocabularyActivity.mSortType) {
                    Order.Description -> R.drawable.ico_animated_sort_asc_ico_exit_name
                    Order.Favorite -> R.drawable.ico_animated_sort_asc_ico_exit_favorited
                    Order.Frequency -> R.drawable.ico_animated_sort_asc_ico_exit_frequency
                    else -> null
                }

            val intermediary: Int? = when (order) {
                Order.Description -> R.drawable.ico_animated_sort_asc_ico_enter_name
                Order.Favorite -> R.drawable.ico_animated_sort_asc_ico_enter_favorited
                Order.Frequency -> R.drawable.ico_animated_sort_asc_ico_enter_frequency
                else -> null
            }

            val final: Int? = if (isDesc) {
                when (order) {
                    Order.Description -> R.drawable.ico_animated_sort_to_desc_name
                    Order.Favorite -> R.drawable.ico_animated_sort_to_desc_favorited
                    Order.Frequency -> R.drawable.ico_animated_sort_to_desc_frequency
                    else -> null
                }
            } else null

            if (initial != null && intermediary != null && final != null)
                MenuUtil.animatedSequenceDrawable(miOrder, initial, intermediary, final)
            else if (initial != null && intermediary != null)
                MenuUtil.animatedSequenceDrawable(miOrder, initial, intermediary)
        }
        VocabularyActivity.mSortDesc = isDesc
        VocabularyActivity.mSortType = order
    }

    private fun onOpenMenuSort() {
        mBottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
        AnimationUtil.animatePopupOpen(requireActivity(), mMenuPopupFilterOrder)
    }

    private fun onChangeSort() {
        if (mRefreshLayout.isRefreshing)
            return

        val orderBy = when (mViewModel.order.value?.first) {
            Order.Description -> Order.Frequency
            Order.Frequency -> Order.Favorite
            Order.Favorite -> Order.Description
            else -> Order.Description
        }

        Toast.makeText(
            requireContext(),
            getString(R.string.menu_manga_reading_order_change, mMapOrder[orderBy]),
            Toast.LENGTH_SHORT
        ).show()

        if (orderBy == Order.Frequency)
            mViewModel.sorted(orderBy, true)
        else
            mViewModel.sorted(orderBy)
    }

    override fun onDestroyView() {
        if (::mBottomSheet.isInitialized) {
            mBottomSheet.removeBottomSheetCallback(mBottomSheetCallback)
        }
        super.onDestroyView()
    }

    override fun onDestroy() {
        if (::mPopupOrderFragment.isInitialized)
            mPopupOrderFragment.clearListener()
        VocabularyMangaListCardAdapter.clearVocabularyMangaList()

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

    private fun setFavorite(favorite: Boolean) {
        mFavorite.setIcon(if (favorite) R.drawable.ico_favorite_mark else R.drawable.ico_favorite_unmark)
    }

    override fun onRefresh() {
        if (::searchView.isInitialized)
            mViewModel.setQuery(searchView.query.toString(), mFavorite.isChecked)
        else
            mViewModel.setQuery("", mFavorite.isChecked)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        MenuUtil.longClick(requireActivity(), R.id.menu_vocabulary_list_order) {
            if (!mRefreshLayout.isRefreshing)
                onOpenMenuSort()
        }

        val myAdapter = mRecyclerView.adapter
        mRecyclerView.adapter = myAdapter

        setupPopupBackgrounds()
    }

    override fun setObject(obj: Vocabulary) {
        TODO("Not yet implemented")
    }

    override fun popupOrderOnChange() {}

    override fun popupSorted(order: Order) {
        mViewModel.sorted(order)
    }

    override fun popupSorted(order: Order, isDesc: Boolean) {
        mViewModel.sorted(order, isDesc)
    }

    override fun popupGetOrder(): Pair<Order, Boolean>? {
        return mViewModel.order.value
    }

    override fun popupGetObserver(): LiveData<Pair<Order, Boolean>> {
        return mViewModel.order
    }

    override fun onPause() {
        super.onPause()
        if (::mBottomSheet.isInitialized && mBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        if (::mMenuPopupLibraryBackground.isInitialized) {
            mMenuPopupLibraryBackground.setBlurAutoUpdate(false)
            mMenuPopupLibraryBackground.setBlurEnabled(false)
        }
    }

    override fun onResume() {
        super.onResume()
        setupPopupBackgrounds()

        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
        val isGlass = sharedPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
        if (::mMenuPopupLibraryBackground.isInitialized) {
            mMenuPopupLibraryBackground.setBlurEnabled(isGlass)
            if (isGlass) {
                mMenuPopupLibraryBackground.blurOnceDeferred(mHandler, 100)
            } else {
                mMenuPopupLibraryBackground.setBlurAutoUpdate(false)
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        val isGlass = GeneralConsts.getSharedPreferences(requireContext()).getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
        if (hidden) {
            if (::mBottomSheet.isInitialized && mBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
                mBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            }
            if (::mMenuPopupLibraryBackground.isInitialized) {
                mMenuPopupLibraryBackground.setBlurAutoUpdate(false)
                mMenuPopupLibraryBackground.setBlurEnabled(false)
            }
        } else {
            if (::mMenuPopupLibraryBackground.isInitialized) {
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
    }
    private fun setupPopupBackgrounds() {
        val activity = activity ?: return
        val contentContainer = view?.findViewById<ViewGroup>(R.id.vocabulary_refresh)
        PopupUtils.setupPopupBackgrounds(activity, mMenuPopupFilterOrder, mMenuPopupLibraryBackground, contentContainer)
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
}