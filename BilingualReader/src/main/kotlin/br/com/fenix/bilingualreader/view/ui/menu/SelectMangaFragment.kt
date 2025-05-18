package br.com.fenix.bilingualreader.view.ui.menu

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.AnimatedVectorDrawable
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
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView
import android.widget.SearchView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.LibraryMangaType
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.service.listener.MangaCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.AdapterUtil.AdapterUtils
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.view.adapter.library.BaseAdapter
import br.com.fenix.bilingualreader.view.adapter.library.MangaGridCardAdapter
import br.com.fenix.bilingualreader.view.adapter.library.MangaLineCardAdapter
import br.com.fenix.bilingualreader.view.adapter.library.MangaSeparatorGridCardAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.slf4j.LoggerFactory
import kotlin.math.max


class SelectMangaFragment : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(SelectMangaFragment::class.java)

    private val mViewModel: SelectMangaViewModel by viewModels()

    private lateinit var mRoot: ConstraintLayout
    private lateinit var mScrollUp: FloatingActionButton
    private lateinit var mScrollDown: FloatingActionButton
    private lateinit var mRecycler: RecyclerView

    private lateinit var mTitle: TextView
    private lateinit var mToolbar: androidx.appcompat.widget.Toolbar
    private lateinit var miSearch: MenuItem
    private lateinit var searchView: SearchView

    private lateinit var mListener: MangaCardListener

    private val mHandler = Handler(Looper.getMainLooper())

    private val mDismissUpButton = Runnable { mScrollUp.hide() }
    private val mDismissDownButton = Runnable { mScrollDown.hide() }

    companion object {
        var mGridType: LibraryMangaType = LibraryMangaType.GRID_BIG
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        if (savedInstanceState == null) {
            mViewModel.clearMangaSelected()

            if (requireArguments().containsKey(GeneralConsts.KEYS.MANGA.ID))
                mViewModel.id = requireArguments().getLong(GeneralConsts.KEYS.MANGA.ID)

            if (requireArguments().containsKey(GeneralConsts.KEYS.MANGA.NAME))
                mViewModel.manga = requireArguments().getString(GeneralConsts.KEYS.MANGA.NAME)!!

            mViewModel.setDefaultLibrary(Libraries.PORTUGUESE)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_select_manga, menu)
        super.onCreateOptionsMenu(menu, inflater)

        miSearch = menu.findItem(R.id.menu_select_manga_search)
        searchView = miSearch.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                mViewModel.filter.filter(newText)
                return false
            }
        })

        val searchSrcTextView = miSearch.actionView!!.findViewById<View>(Resources.getSystem().getIdentifier("search_src_text", "id", "android")) as AutoCompleteTextView
        searchSrcTextView.setTextAppearance(R.style.SearchShadow)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_select_manga, container, false)

        mGridType = LibraryMangaType.valueOf(
            GeneralConsts.getSharedPreferences(requireContext())
                .getString(GeneralConsts.KEYS.LIBRARY.MANGA_LIBRARY_TYPE, LibraryMangaType.LINE.toString())
                .toString()
        )

        mRoot = root.findViewById(R.id.select_manga_root)
        mRecycler = root.findViewById(R.id.select_manga_recycler)
        mScrollUp = root.findViewById(R.id.select_manga_scroll_up)
        mScrollDown = root.findViewById(R.id.select_manga_scroll_down)
        mToolbar = root.findViewById(R.id.toolbar_select_manga)
        mTitle = root.findViewById(R.id.toolbar_select_manga_title)
        val theme = Themes.valueOf(GeneralConsts.getSharedPreferences(requireContext()).getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!)
        MenuUtil.tintToolbar(mToolbar, theme)

        (requireActivity() as MenuActivity).setActionBar(mToolbar)

        registerForContextMenu(mTitle)

        mScrollUp.visibility = View.GONE
        mScrollDown.visibility = View.GONE

        mScrollUp.setOnClickListener {
            (mScrollUp.drawable as AnimatedVectorDrawable).start()
            mRecycler.smoothScrollToPosition(0)
        }
        mScrollUp.setOnLongClickListener {
            (mScrollUp.drawable as AnimatedVectorDrawable).start()
            mRecycler.scrollToPosition(0)
            true
        }
        mScrollDown.setOnClickListener {
            (mScrollDown.drawable as AnimatedVectorDrawable).start()
            mRecycler.smoothScrollToPosition((mRecycler.adapter as RecyclerView.Adapter).itemCount)
        }
        mScrollDown.setOnLongClickListener {
            (mScrollDown.drawable as AnimatedVectorDrawable).start()
            mRecycler.scrollToPosition((mRecycler.adapter as RecyclerView.Adapter).itemCount -1)
            true
        }

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

        observer()
        mViewModel.list(mViewModel.id, mViewModel.manga) { }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerLayout()
    }

    private fun recyclerLayout() {
        val type = LibraryMangaType.valueOf(
            GeneralConsts.getSharedPreferences(requireContext()).getString(
                GeneralConsts.KEYS.LIBRARY.MANGA_LIBRARY_TYPE,
                LibraryMangaType.LINE.toString()
            )
                .toString()
        )

        if (type != LibraryMangaType.LINE) {
            val gridAdapter = when (type) {
                LibraryMangaType.SEPARATOR_BIG,
                LibraryMangaType.SEPARATOR_MEDIUM -> MangaSeparatorGridCardAdapter(requireContext(), type)
                else -> MangaGridCardAdapter(type)
            }
            mRecycler.adapter = gridAdapter
            mRecycler.layoutManager = getGridLayout(type)
            gridAdapter.attachListener(mListener)
            mRecycler.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_library_grid)
        } else {
            val lineAdapter = MangaLineCardAdapter()
            mRecycler.adapter = lineAdapter
            mRecycler.layoutManager = GridLayoutManager(requireContext(), 1)
            lineAdapter.attachListener(mListener)
            mRecycler.layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_library_line)
        }
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
    }

    private fun changeLibrary(library: Library) {
        mViewModel.changeLibrary(library)
        titleLibrary()
    }

    private fun titleLibrary() {
        mTitle.text = mViewModel.getLibrary().title
    }

    private fun observer() {
        mViewModel.listMangas.observe(viewLifecycleOwner) {
            (mRecycler.adapter as BaseAdapter<Manga, *>).updateList(Order.None, it)
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

    private fun getGridLayout(type: LibraryMangaType): RecyclerView.LayoutManager {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val columnWidth: Int = AdapterUtils.getMangaCardSize(requireContext(), type, isLandscape).first + 1
        val spaceCount: Int = max(1, (Resources.getSystem().displayMetrics.widthPixels -3) / columnWidth)
        return when (type) {
            LibraryMangaType.SEPARATOR_BIG,
            LibraryMangaType.SEPARATOR_MEDIUM -> StaggeredGridLayoutManager(spaceCount, StaggeredGridLayoutManager.VERTICAL)
            else -> GridLayoutManager(requireContext(), spaceCount)
        }
    }

}