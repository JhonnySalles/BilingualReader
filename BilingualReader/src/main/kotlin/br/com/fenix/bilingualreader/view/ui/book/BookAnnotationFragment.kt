package br.com.fenix.bilingualreader.view.ui.book

import android.content.res.Resources
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.ContextThemeWrapper
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
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.model.enums.Filter
import br.com.fenix.bilingualreader.model.enums.ListMode
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.service.listener.AnnotationListener
import br.com.fenix.bilingualreader.service.listener.BookAnnotationListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.AnimationUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.view.adapter.book.BookAnnotationLineAdapter
import br.com.fenix.bilingualreader.view.ui.annotation.AnnotationPopupFilterChapter
import br.com.fenix.bilingualreader.view.ui.annotation.AnnotationPopupFilterColor
import br.com.fenix.bilingualreader.view.ui.annotation.AnnotationPopupFilterType
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import br.com.fenix.bilingualreader.view.ui.popup.PopupAnnotations
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import org.slf4j.LoggerFactory


class BookAnnotationFragment : Fragment(), AnnotationListener {

    private val mLOGGER = LoggerFactory.getLogger(BookAnnotationFragment::class.java)

    private val mViewModel: BookAnnotationViewModel by activityViewModels()

    private lateinit var mToolbar: Toolbar

    private lateinit var mScrollUp: FloatingActionButton
    private lateinit var mScrollDown: FloatingActionButton
    private lateinit var miSearch: MenuItem
    private lateinit var searchView: SearchView

    private lateinit var mMenuPopupFilter: FrameLayout
    private lateinit var mPopupFilterView: ViewPager
    private lateinit var mPopupFilterTab: TabLayout
    private lateinit var mPopupChaptersTab: TabLayout
    private lateinit var mPopupFilterTypeFragment: AnnotationPopupFilterType
    private lateinit var mPopupFilterColorFragment: AnnotationPopupFilterColor
    private lateinit var mPopupFilterChapterFragment: AnnotationPopupFilterChapter
    private lateinit var mBottomSheet: BottomSheetBehavior<FrameLayout>

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mListener: BookAnnotationListener

    private lateinit var mBook: Book

    private val mHandler = Handler(Looper.getMainLooper())
    private val mDismissUpButton = Runnable { mScrollUp.hide() }
    private val mDismissDownButton = Runnable { mScrollDown.hide() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        arguments?.let {
            mBook = it.getSerializable(GeneralConsts.KEYS.OBJECT.BOOK) as Book
            mViewModel.search(mBook.id!!)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_book_annotation, menu)
        super.onCreateOptionsMenu(menu, inflater)

        miSearch = menu.findItem(R.id.menu_book_annotation_search)
        searchView = miSearch.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null)
                    mViewModel.search(newText)
                else
                    mViewModel.clearSearch()
                return false
            }
        })

        val searchSrcTextView = miSearch.actionView!!.findViewById<View>(Resources.getSystem().getIdentifier("search_src_text", "id", "android")) as AutoCompleteTextView
        searchSrcTextView.setTextAppearance(R.style.SearchShadow)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_book_annotation_filters -> onOpenMenuFilter()
        }
        return super.onOptionsItemSelected(menuItem)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_book_annotation, container, false)

        mRecyclerView = root.findViewById(R.id.book_annotation_recycler_view)

        mScrollUp = root.findViewById(R.id.book_annotation_scroll_up)
        mScrollDown = root.findViewById(R.id.book_annotation_scroll_down)

        mToolbar = root.findViewById(R.id.toolbar_book_annotation)

        mScrollUp.visibility = View.GONE
        mScrollDown.visibility = View.GONE

        (requireActivity() as MenuActivity).setActionBar(mToolbar)

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

        mMenuPopupFilter = root.findViewById(R.id.book_annotation_popup_filter)
        mPopupFilterTab = root.findViewById(R.id.book_annotation_popup_filter_tab)
        mPopupFilterView = root.findViewById(R.id.book_annotation_popup_order_filter_view_pager)

        root.findViewById<ImageView>(R.id.book_annotation_popup_filter_close)
            .setOnClickListener {
                AnimationUtil.animatePopupClose(requireActivity(), mMenuPopupFilter)
            }

        mPopupFilterTab.setupWithViewPager(mPopupFilterView)

        BottomSheetBehavior.from(mMenuPopupFilter).apply {
            peekHeight = 195
            this.state = BottomSheetBehavior.STATE_COLLAPSED
            mBottomSheet = this
        }
        mBottomSheet.isDraggable = true

        root.findViewById<ImageView>(R.id.book_annotation_popup_filter_touch)
            .setOnClickListener {
                if (mBottomSheet.state == BottomSheetBehavior.STATE_COLLAPSED)
                    mBottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
                else
                    mBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            }

        mPopupFilterTypeFragment = AnnotationPopupFilterType()
        mPopupFilterColorFragment = AnnotationPopupFilterColor()
        mPopupFilterChapterFragment = AnnotationPopupFilterChapter()

        mPopupFilterTypeFragment.setListener(this)
        mPopupFilterColorFragment.setListener(this)
        mPopupFilterChapterFragment.setListener(this)

        val viewOrderPagerAdapter = ViewPagerAdapter(childFragmentManager, 0)
        viewOrderPagerAdapter.addFragment(mPopupFilterTypeFragment, resources.getString(R.string.annotation_tab_item_filter))
        viewOrderPagerAdapter.addFragment(mPopupFilterColorFragment, resources.getString(R.string.annotation_tab_item_color))
        viewOrderPagerAdapter.addFragment(mPopupFilterChapterFragment, resources.getString(R.string.annotation_tab_item_chapters))

        mPopupFilterView.adapter = viewOrderPagerAdapter

        return root
    }

    private fun onOpenMenuFilter() {
        mBottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
        AnimationUtil.animatePopupOpen(requireActivity(), mMenuPopupFilter)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mListener = object : BookAnnotationListener {
            override fun onClick(annotation: BookAnnotation) {
                val bundle = Bundle()
                bundle.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK_ANNOTATION, annotation)
                (requireActivity() as MenuActivity).onBack(bundle)
            }

            override fun onClickFavorite(annotation: BookAnnotation) {
                mViewModel.save(annotation)
            }

            override fun onClickOptions(annotation: BookAnnotation, view: View, position: Int) {
                val wrapper = ContextThemeWrapper(requireContext(), R.style.PopupMenu)
                val popup = PopupMenu(wrapper, view, 0, R.attr.popupMenuStyle, R.style.PopupMenu)
                popup.menuInflater.inflate(R.menu.menu_item_book_annotation, popup.menu)

                if (annotation.type != MarkType.Annotation)
                    popup.menu.removeItem(R.id.menu_item_item_book_annotation_change_detach)

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_item_book_annotation_favorite -> {
                            annotation.favorite = !annotation.favorite
                            mViewModel.save(annotation)
                            notifyDataSet(position)
                        }
                        R.id.menu_item_item_book_annotation_delete -> {
                            deleteAnnotation(annotation, position)
                        }
                        R.id.menu_item_item_book_annotation_change_detach -> {
                            val colors = Util.getColors(requireContext())
                            val items = colors.keys.toTypedArray()

                            val title = LinearLayout(requireContext())
                            title.orientation = LinearLayout.VERTICAL
                            title.setPadding(resources.getDimensionPixelOffset(R.dimen.title_index_dialog_padding))
                            val name = TextView(requireContext())
                            name.text = getString(R.string.book_annotation_change_detach_title)
                            name.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.title_index_dialog_size))
                            name.setTextColor(requireContext().getColorFromAttr(R.attr.colorPrimary))
                            title.addView(name)
                            val index = TextView(requireContext())
                            index.text = getString(R.string.book_annotation_change_detach_description)
                            index.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.title_small_index_dialog_size))
                            index.setTextColor(requireContext().getColorFromAttr(R.attr.colorSecondary))
                            title.addView(index)

                            MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
                                .setCustomTitle(title)
                                .setItems(items) { _, selected ->
                                    val color = colors[items[selected]]
                                    if (color != null) {
                                        annotation.color = color
                                        mViewModel.save(annotation)
                                        notifyDataSet(position)
                                    }
                                }.show()
                        }
                    }
                    true
                }

                popup.show()
            }

            override fun onClickNote(annotation: BookAnnotation, position: Int) {
                val onDelete = { obj: BookAnnotation ->
                    mViewModel.delete(obj)
                    true
                }
                PopupAnnotations(requireContext()).popup(annotation, onDelete) { alter ->
                    if (alter)
                        notifyDataSet(position)
                }
            }

        }

        val adapter = BookAnnotationLineAdapter()
        adapter.attachListener(mListener)
        mRecyclerView.adapter = adapter
        mRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mRecyclerView)

        observer()
    }

    private fun observer() {
        mViewModel.annotation.observe(viewLifecycleOwner) {
            (mRecyclerView.adapter as BookAnnotationLineAdapter).updateList(it)
        }

        mViewModel.typeFilter.observe(viewLifecycleOwner) {
            mPopupFilterTypeFragment.setFilters(it)
        }

        mViewModel.colorFilter.observe(viewLifecycleOwner) {
            mPopupFilterColorFragment.setColors(it)
        }

        mViewModel.chapters.observe(viewLifecycleOwner) {
            mPopupFilterChapterFragment.setChapters(it)
        }


        mViewModel.chapterFilter.observe(viewLifecycleOwner) {
            mPopupFilterChapterFragment.setChaptersFilter(it)
        }
    }

    private fun notifyDataSet(indexes: MutableList<Pair<ListMode, Int>>) {
        if (indexes.any { it.first == ListMode.FULL })
            notifyDataSet(0, (mViewModel.annotation.value?.size ?: 1))
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


    private var itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val annotation = mViewModel.getAndRemove(viewHolder.bindingAdapterPosition) ?: return
                val position = viewHolder.bindingAdapterPosition
                deleteAnnotation(annotation, position)
            }
        }

    private fun deleteAnnotation(annotation: BookAnnotation, position: Int) {
        var excluded = false
        val dialog: AlertDialog =
            MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
                .setTitle(getString(R.string.book_annotation_delete))
                .setMessage(
                    getString(
                        R.string.book_annotation_delete_description,
                        getString(annotation.type.getDescription()).lowercase()
                    )
                )
                .setPositiveButton(
                    R.string.action_delete
                ) { _, _ ->
                    mViewModel.delete(annotation)
                    notifyDataSet(position, removed = true)
                    excluded = true
                }.setOnDismissListener {
                    if (!excluded) {
                        mViewModel.add(annotation, position)
                        notifyDataSet(position)
                    }
                }
                .create()
        dialog.show()
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

    override fun getFilters(): Set<Filter> = mViewModel.typeFilter.value!!

    override fun filterType(filter: Filter, isRemove: Boolean) {
        mViewModel.filterType(filter, isRemove)
    }

    override fun getColors(): Set<Color> = mViewModel.colorFilter.value!!

    override fun filterColor(color: Color, isRemove: Boolean) {
        mViewModel.filterColor(color, isRemove)
    }

    override fun getChapters(): Map<String, Float> = mViewModel.chapters.value!!

    override fun filterChapter(chapter: String, isRemove: Boolean) {
        mViewModel.filterChapter(chapter, isRemove)
    }

}