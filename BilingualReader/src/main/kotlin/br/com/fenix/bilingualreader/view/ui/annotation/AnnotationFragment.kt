package br.com.fenix.bilingualreader.view.ui.annotation

import android.content.Intent
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
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.entity.MangaAnnotation
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.model.enums.Filter
import br.com.fenix.bilingualreader.model.enums.ListMode
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.listener.AnnotationListener
import br.com.fenix.bilingualreader.service.listener.AnnotationsListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.AnimationUtil
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.view.adapter.annotation.AnnotationLineAdapter
import br.com.fenix.bilingualreader.view.adapter.annotation.AnnotationRootViewHolder
import br.com.fenix.bilingualreader.view.adapter.annotation.AnnotationTitleViewHolder
import br.com.fenix.bilingualreader.view.ui.popup.PopupAnnotations
import br.com.fenix.bilingualreader.view.ui.reader.book.BookReaderActivity
import br.com.fenix.bilingualreader.view.ui.reader.manga.MangaReaderActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import org.slf4j.LoggerFactory


class AnnotationFragment : Fragment(), AnnotationListener {

    private val mLOGGER = LoggerFactory.getLogger(AnnotationFragment::class.java)

    private val mViewModel: AnnotationViewModel by activityViewModels()

    private lateinit var mScrollUp: FloatingActionButton
    private lateinit var mScrollDown: FloatingActionButton
    private lateinit var miSearch: MenuItem
    private lateinit var searchView: SearchView
    private lateinit var miFilterType: MenuItem

    private lateinit var mMenuPopupFilter: FrameLayout
    private lateinit var mPopupFilterView: ViewPager
    private lateinit var mPopupFilterTab: TabLayout
    private lateinit var mPopupChaptersTab: TabLayout
    private lateinit var mPopupFilterTypeFragment: AnnotationPopupFilterType
    private lateinit var mPopupFilterColorFragment: AnnotationPopupFilterColor
    private lateinit var mPopupFilterChapterFragment: AnnotationPopupFilterChapter
    private lateinit var mBottomSheet: BottomSheetBehavior<FrameLayout>

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mListener: AnnotationsListener

    private var mFilterType: Type? = null

    private val mHandler = Handler(Looper.getMainLooper())
    private val mDismissUpButton = Runnable { mScrollUp.hide() }
    private val mDismissDownButton = Runnable { mScrollDown.hide() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        mViewModel.findAll()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_annotation, menu)
        super.onCreateOptionsMenu(menu, inflater)

        miFilterType = menu.findItem(R.id.menu_annotation_type)
        miFilterType.subMenu?.clear()
        miFilterType.subMenu?.add(requireContext().getString(R.string.annotation_menu_choice_all))?.setOnMenuItemClickListener { _: MenuItem? ->
            filterType(null)
            true
        }

        for (type in Type.values()) {
            val title = when (type) {
                Type.MANGA -> requireContext().getString(R.string.annotation_manga)
                Type.BOOK -> requireContext().getString(R.string.annotation_book)
            }
            miFilterType.subMenu?.add(title)?.setOnMenuItemClickListener { _: MenuItem? ->
                filterType(type)
                true
            }
        }

        val iconType: Int = if (mFilterType == null)
            R.drawable.ico_menu_type_all
        else when (mFilterType) {
            Type.MANGA -> R.drawable.ico_menu_type_manga
            Type.BOOK -> R.drawable.ico_menu_type_book
            else -> R.drawable.ico_menu_type_all
        }
        miFilterType.setIcon(iconType)

        miSearch = menu.findItem(R.id.menu_annotation_search)
        searchView = miSearch.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            private var runFilter = Runnable { }
            override fun onQueryTextChange(newText: String?): Boolean {
                mHandler.removeCallbacks(runFilter)
                runFilter = Runnable {
                    if (newText != null)
                        mViewModel.search(newText)
                    else
                        mViewModel.clearSearch()
                }
                mHandler.postDelayed(runFilter, GeneralConsts.DEFAULTS.DEFAULT_HANDLE_SEARCH_FILTER)
                return false
            }
        })

        val searchSrcTextView = miSearch.actionView!!.findViewById<View>(Resources.getSystem().getIdentifier("search_src_text", "id", "android")) as AutoCompleteTextView
        searchSrcTextView.setTextAppearance(R.style.SearchShadow)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_annotation_filters -> {
                (menuItem.icon as AnimatedVectorDrawable).start()
                onOpenMenuFilter()
            }
        }
        return super.onOptionsItemSelected(menuItem)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_annotation, container, false)

        mRecyclerView = root.findViewById(R.id.annotation_recycler_view)

        mScrollUp = root.findViewById(R.id.annotation_scroll_up)
        mScrollDown = root.findViewById(R.id.annotation_scroll_down)

        mScrollUp.visibility = View.GONE
        mScrollDown.visibility = View.GONE

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

        mMenuPopupFilter = root.findViewById(R.id.annotation_popup_filter)
        mPopupFilterTab = root.findViewById(R.id.annotation_popup_filter_tab)
        mPopupFilterView = root.findViewById(R.id.annotation_popup_order_filter_view_pager)

        mPopupFilterTab.setupWithViewPager(mPopupFilterView)

        BottomSheetBehavior.from(mMenuPopupFilter).apply {
            peekHeight = 195
            this.state = BottomSheetBehavior.STATE_COLLAPSED
            mBottomSheet = this
        }
        mBottomSheet.isDraggable = true

        root.findViewById<ImageView>(R.id.annotation_popup_filter_touch)
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

        mListener = object : AnnotationsListener {
            override fun onClick(annotation: br.com.fenix.bilingualreader.model.interfaces.Annotation) {
                when (annotation.type) {
                    Type.BOOK -> {
                        val book = mViewModel.getBook(annotation.id_parent) ?: return
                        if (book.file.exists()) {
                            val intent = Intent(context, BookReaderActivity::class.java)
                            val bundle = Bundle()
                            bundle.putSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY, book.library)
                            bundle.putString(GeneralConsts.KEYS.BOOK.NAME, book.title)

                            if ((annotation as BookAnnotation).page >= 0 && annotation.page < book.pages)
                                bundle.putInt(GeneralConsts.KEYS.BOOK.MARK, annotation.page +1)
                            else
                                bundle.putInt(GeneralConsts.KEYS.BOOK.MARK, book.bookMark)

                            bundle.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK, book)
                            intent.putExtras(bundle)
                            context?.startActivity(intent)
                            requireActivity().overridePendingTransition(R.anim.fade_in_fragment_add_enter, R.anim.fade_out_fragment_remove_exit)
                        } else {
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
                    Type.MANGA -> {
                        val manga = mViewModel.getManga(annotation.id_parent) ?: return
                        if (manga.file.exists()) {
                            val intent = Intent(context, MangaReaderActivity::class.java)
                            val bundle = Bundle()
                            bundle.putSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY, manga.library)
                            bundle.putString(GeneralConsts.KEYS.MANGA.NAME, manga.title)

                            if ((annotation as MangaAnnotation).page >= 0 && annotation.page < manga.pages)
                                bundle.putInt(GeneralConsts.KEYS.MANGA.MARK, annotation.page)
                            else
                                bundle.putInt(GeneralConsts.KEYS.MANGA.MARK, manga.bookMark)

                            bundle.putSerializable(GeneralConsts.KEYS.OBJECT.MANGA, manga)
                            intent.putExtras(bundle)
                            context?.startActivity(intent)
                            requireActivity().overridePendingTransition(R.anim.fade_in_fragment_add_enter, R.anim.fade_out_fragment_remove_exit)
                        } else {
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
                }
            }

            override fun onClickFavorite(annotation: br.com.fenix.bilingualreader.model.interfaces.Annotation) {
                if (annotation.type == Type.BOOK)
                    mViewModel.save(annotation as BookAnnotation)
            }

            override fun onClickOptions(annotation: br.com.fenix.bilingualreader.model.interfaces.Annotation, view: View, position: Int) {
                if (annotation.type == Type.BOOK) {
                    val wrapper = ContextThemeWrapper(requireContext(), R.style.PopupMenu)
                    val popup = PopupMenu(wrapper, view, 0, R.attr.popupMenuStyle, R.style.PopupMenu)
                    popup.menuInflater.inflate(R.menu.menu_item_book_annotation, popup.menu)

                    if ((annotation as BookAnnotation).markType != MarkType.Annotation)
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
                                name.setTextColor(requireContext().getColorFromAttr(R.attr.colorOnBackground))
                                title.addView(name)
                                val index = TextView(requireContext())
                                index.text = getString(R.string.book_annotation_change_detach_description)
                                index.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.title_small_index_dialog_size))
                                index.setTextColor(requireContext().getColorFromAttr(R.attr.colorOnSecondary))
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
            }

            override fun onClickNote(annotation: br.com.fenix.bilingualreader.model.interfaces.Annotation, position: Int) {
                if (annotation.type == Type.BOOK) {
                    val onDelete = { obj: BookAnnotation ->
                        mViewModel.delete(obj) { index, isRemove ->
                            notifyDataSet(index, removed = isRemove)
                        }
                        true
                    }
                    PopupAnnotations(requireContext()).popup(annotation as BookAnnotation, onDelete) { alter ->
                        if (alter)
                            notifyDataSet(position)
                    }
                }
            }

        }

        val adapter = AnnotationLineAdapter()
        adapter.attachListener(mListener)
        mRecyclerView.adapter = adapter
        mRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mRecyclerView)

        observer()
    }

    private fun observer() {
        mViewModel.annotation.observe(viewLifecycleOwner) {
            (mRecyclerView.adapter as AnnotationLineAdapter).updateList(it)
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

        mViewModel.type.observe(viewLifecycleOwner) {
            onChangeIconFilterType(it)
        }
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

        override fun getSwipeDirs (recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            if (viewHolder is AnnotationRootViewHolder || viewHolder is AnnotationTitleViewHolder)
                return 0

            return super.getSwipeDirs(recyclerView, viewHolder)
        }
    }

    private fun deleteAnnotation(annotation: br.com.fenix.bilingualreader.model.interfaces.Annotation, position: Int) {
        var excluded = false
        val dialog: AlertDialog =
            MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
                .setTitle(getString(R.string.book_annotation_delete))
                .setMessage(
                    getString(
                        R.string.book_annotation_delete_description,
                        getString(annotation.markType.getDescription()).lowercase()
                    )
                )
                .setPositiveButton(
                    R.string.action_delete
                ) { _, _ ->
                    mViewModel.delete(annotation) { index, isRemove ->
                        notifyDataSet(index, removed = isRemove)
                    }
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

    private fun filterType(type: Type?) {
        mViewModel.filterType(type)
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