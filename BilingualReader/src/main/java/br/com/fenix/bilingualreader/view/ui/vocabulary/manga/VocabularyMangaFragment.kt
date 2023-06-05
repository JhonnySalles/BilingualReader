package br.com.fenix.bilingualreader.view.ui.vocabulary.manga

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager.widget.ViewPager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.VocabularyCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.view.adapter.vocabulary.VocabularyLoadState
import br.com.fenix.bilingualreader.view.adapter.vocabulary.VocabularyMangaCardAdapter
import br.com.fenix.bilingualreader.view.adapter.vocabulary.VocabularyMangaListCardAdapter
import br.com.fenix.bilingualreader.view.components.ComponentsUtil
import br.com.fenix.bilingualreader.view.components.InitializeVocabulary
import br.com.fenix.bilingualreader.view.components.PopupOrderListener
import br.com.fenix.bilingualreader.view.ui.vocabulary.VocabularyFragment
import br.com.fenix.bilingualreader.view.ui.vocabulary.VocabularyPopupOrder
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory


class VocabularyMangaFragment : Fragment(), PopupOrderListener,
    SwipeRefreshLayout.OnRefreshListener,
    InitializeVocabulary<Manga> {

    private val mLOGGER = LoggerFactory.getLogger(VocabularyMangaFragment::class.java)

    private val mViewModel: VocabularyMangaViewModel by viewModels()

    private var mManga: Manga? = null

    private lateinit var mRoot: CoordinatorLayout
    private lateinit var mRefreshLayout: SwipeRefreshLayout
    private lateinit var mScrollUp: FloatingActionButton
    private lateinit var mScrollDown: FloatingActionButton
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mMangaContent: LinearLayout
    private lateinit var mMangaName: TextInputLayout
    private lateinit var mMangaNameEditText: TextInputEditText

    private lateinit var mMenuPopupFilterOrder: FrameLayout
    private lateinit var mPopupFilterOrderView: ViewPager
    private lateinit var mPopupFilterOrderTab: TabLayout
    private lateinit var mPopupOrderFragment: VocabularyPopupOrder
    private lateinit var mBottomSheet: BottomSheetBehavior<FrameLayout>

    private lateinit var mFavorite: MenuItem
    private lateinit var miOrder: MenuItem
    private lateinit var miSearch: MenuItem
    private lateinit var searchView: SearchView

    private lateinit var mListener: VocabularyCardListener

    private lateinit var mMapOrder: HashMap<Order, String>
    private var mHandler = Handler(Looper.getMainLooper())
    private val mDismissUpButton = Runnable { mScrollUp.hide() }
    private val mDismissDownButton = Runnable { mScrollDown.hide() }

    private val mSetQuery = Runnable {
        mViewModel.setQuery(
            mMangaNameEditText.text?.toString() ?: "",
            searchView.query.toString()
        )
    }

    companion object {
        var mSortType: Order = Order.Description
        var mSortDesc: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_vocabulary, menu)
        super.onCreateOptionsMenu(menu, inflater)

        mFavorite = menu.findItem(R.id.menu_vocabulary_favorite)

        mFavorite.setIcon(if (mViewModel.getFavorite()) R.drawable.ico_favorite_mark else R.drawable.ico_favorite_unmark)
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
                    mMangaNameEditText.text?.toString() ?: "",
                    query ?: ""
                )
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        MenuUtil.longClick(requireActivity(), R.id.menu_vocabulary_list_order) {
            if (!mRefreshLayout.isRefreshing)
                onOpenMenuSort()
        }

        miOrder.setOnMenuItemClickListener {
            onChangeSort()
            true
        }

        mViewModel.order.observe(viewLifecycleOwner) {
            if (it.first == VocabularyFragment.mSortType && it.second == VocabularyFragment.mSortDesc)
                return@observe

            val isDesc = if (VocabularyFragment.mSortType == it.first) it.second else null
            onChangeIconSort(it.first, isDesc)
        }

        if (mInitialVocabulary.isNotEmpty())
            searchView.setQuery(mInitialVocabulary, true)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_history_manga_library -> {}
        }
        return super.onOptionsItemSelected(menuItem)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_vocabulary_manga, container, false)

        mMapOrder = hashMapOf(
            Order.Description to getString(R.string.config_option_vocabulary_order_description),
            Order.Frequency to getString(R.string.config_option_vocabulary_order_frequency),
            Order.Favorite to getString(R.string.config_option_vocabulary_order_favorite)
        )

        mRoot = root.findViewById(R.id.vocabulary_manga_root)
        mRecyclerView = root.findViewById(R.id.vocabulary_manga_recycler)
        mRefreshLayout = root.findViewById(R.id.vocabulary_manga_refresh)

        mMangaContent = root.findViewById(R.id.popup_vocabulary_manga_content)
        mMangaName = root.findViewById(R.id.vocabulary_manga_text)
        mMangaNameEditText = root.findViewById(R.id.vocabulary_manga_edittext)

        mScrollUp = root.findViewById(R.id.vocabulary_manga_scroll_up)
        mScrollDown = root.findViewById(R.id.vocabulary_manga_scroll_down)

        mMenuPopupFilterOrder = root.findViewById(R.id.vocabulary_manga_popup_menu_order_filter)
        mPopupFilterOrderTab = root.findViewById(R.id.vocabulary_manga_popup_order_filter_tab)
        mPopupFilterOrderView =
            root.findViewById(R.id.vocabulary_manga_popup_order_filter_view_pager)

        root.findViewById<ImageView>(R.id.vocabulary_manga_popup_menu_order_filter_close)
            .setOnClickListener {
                mMenuPopupFilterOrder.visibility = View.GONE
            }

        mScrollUp.visibility = View.GONE
        mScrollDown.visibility = View.GONE

        mViewModel.isQuery.observe(viewLifecycleOwner) {
            mRefreshLayout.isRefreshing = it
        }

        ComponentsUtil.setThemeColor(requireContext(), mRefreshLayout)
        mRefreshLayout.setOnRefreshListener(this)
        mRefreshLayout.isEnabled = true

        mMangaNameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (mHandler.hasCallbacks(mSetQuery))
                        mHandler.removeCallbacks(mSetQuery)
                } else
                    mHandler.removeCallbacks(mSetQuery)

                mHandler.postDelayed(mSetQuery, 1000)
            }
        })

        mScrollUp.setOnClickListener {
            (mScrollUp.drawable as AnimatedVectorDrawable).start()
            mRecyclerView.smoothScrollToPosition(0)
        }
        mScrollDown.setOnClickListener {
            (mScrollDown.drawable as AnimatedVectorDrawable).start()
            mRecyclerView.smoothScrollToPosition((mRecyclerView.adapter as RecyclerView.Adapter).itemCount)
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

        mPopupFilterOrderTab.setupWithViewPager(mPopupFilterOrderView)
        mPopupOrderFragment = VocabularyPopupOrder(this)

        BottomSheetBehavior.from(mMenuPopupFilterOrder).apply {
            peekHeight = 195
            this.state = BottomSheetBehavior.STATE_COLLAPSED
            mBottomSheet = this
            if (resources.getBoolean(R.bool.isTablet))
                this.maxWidth = Util.dpToPx(requireContext(), 350)
        }
        mBottomSheet.isDraggable = true

        root.findViewById<ImageView>(R.id.vocabulary_manga_popup_menu_order_filter_touch)
            .setOnClickListener {
                if (mBottomSheet.state == BottomSheetBehavior.STATE_COLLAPSED)
                    mBottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
                else
                    mBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            }

        val viewOrderPagerAdapter = ViewPagerAdapter(childFragmentManager, 0)
        viewOrderPagerAdapter.addFragment(
            mPopupOrderFragment,
            resources.getString(R.string.popup_vocabulary_tab_item_ordering)
        )

        mPopupFilterOrderView.adapter = viewOrderPagerAdapter

        mManga?.let {
            mMangaNameEditText.setText(it.name)
            mViewModel.setQuery(it.name, mInitialVocabulary)
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mListener = object : VocabularyCardListener {
            override fun onClick(vocabulary: Vocabulary) {
            }

            override fun onClickLong(vocabulary: Vocabulary, view: View, position: Int) {
            }

            override fun onClickFavorite(vocabulary: Vocabulary) {
                mViewModel.update(vocabulary)
            }
        }

        val adapter = VocabularyMangaCardAdapter(mListener)
        mRecyclerView.adapter = adapter.withLoadStateFooter(VocabularyLoadState())
        mRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            mViewModel.vocabularyPager().collectLatest {
                adapter.submitData(it)
            }
        }
    }

    private fun onChangeIconSort(order: Order, isDesc: Boolean?) {
        if (isDesc != null) {
            val icon: Int? = when (order) {
                Order.Description -> if (isDesc) R.drawable.ico_animated_sort_to_desc_name else R.drawable.ico_animated_sort_to_asc_name
                Order.Favorite -> if (isDesc) R.drawable.ico_animated_sort_to_desc_favorited else R.drawable.ico_animated_sort_to_asc_favorited
                Order.Frequency -> if (isDesc) R.drawable.ico_animated_sort_to_desc_frequency else R.drawable.ico_animated_sort_to_asc_frequency
                else -> null
            }
            VocabularyFragment.mSortDesc = isDesc
            if (icon != null)
                MenuUtil.animatedSequenceDrawable(miOrder, icon)
        } else {
            val initial: Int? = if (VocabularyFragment.mSortDesc)
                when (VocabularyFragment.mSortType) {
                    Order.Description -> R.drawable.ico_animated_sort_desc_to_asc_ico_exit_name
                    Order.Favorite -> R.drawable.ico_animated_sort_desc_to_asc_ico_exit_favorited
                    Order.Frequency -> R.drawable.ico_animated_sort_desc_to_asc_ico_exit_frequency
                    else -> null
                } else
                when (VocabularyFragment.mSortType) {
                    Order.Description -> R.drawable.ico_animated_sort_asc_ico_exit_name
                    Order.Favorite -> R.drawable.ico_animated_sort_asc_ico_exit_favorited
                    Order.Frequency -> R.drawable.ico_animated_sort_asc_ico_exit_frequency
                    else -> null
                }

            val final: Int? = when (order) {
                Order.Description -> R.drawable.ico_animated_sort_asc_ico_enter_name
                Order.Favorite -> R.drawable.ico_animated_sort_asc_ico_enter_favorited
                Order.Frequency -> R.drawable.ico_animated_sort_asc_ico_enter_frequency
                else -> null
            }

            if (initial != null && final != null)
                MenuUtil.animatedSequenceDrawable(miOrder, initial, final)

            VocabularyFragment.mSortDesc = false
        }
        VocabularyFragment.mSortType = order
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
            Order.Description -> Order.Frequency
            Order.Frequency -> Order.Favorite
            Order.Favorite -> Order.Description
            else -> Order.Description
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

        mViewModel.sorted(orderBy)
    }

    override fun onDestroy() {
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

    override fun onRefresh() {
        if (::searchView.isInitialized)
            mViewModel.setQuery(
                mMangaNameEditText.text.toString(),
                searchView.query.toString(),
                mFavorite.isChecked
            )
        else
            mViewModel.setQuery(mMangaNameEditText.text.toString(), "", mFavorite.isChecked)
    }

    var mInitialVocabulary = ""
    override fun setVocabulary(vocabulary: String) {
        mInitialVocabulary = vocabulary
    }

    override fun setObject(obj: Manga) {
        mManga = obj
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