package br.com.fenix.bilingualreader.view.ui.book

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookSearch
import br.com.fenix.bilingualreader.service.listener.BookParseListener
import br.com.fenix.bilingualreader.service.listener.BookSearchHistoryListener
import br.com.fenix.bilingualreader.service.listener.BookSearchListener
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.repository.SharedData
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.view.adapter.book.BookSearchHistoryAdapter
import br.com.fenix.bilingualreader.view.adapter.book.BookSearchLineAdapter
import br.com.fenix.bilingualreader.view.adapter.vocabulary.VocabularyMangaListCardAdapter
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.slf4j.LoggerFactory


class BookSearchFragment : Fragment(), BookParseListener {

    private val mLOGGER = LoggerFactory.getLogger(BookSearchFragment::class.java)

    private val mViewModelBookSearch: BookSearchViewModel by activityViewModels()
    private val mViewModelAnnotations: BookAnnotationViewModel by activityViewModels()

    private lateinit var mToolbar: Toolbar
    private lateinit var miSearch: MenuItem
    private lateinit var searchView: SearchView

    private lateinit var mProgressContent: CoordinatorLayout
    private lateinit var mProgressIndicator: ProgressBar
    private lateinit var mSearchStop: MaterialButton
    private lateinit var mClearHistory: MaterialButton
    private lateinit var mScrollUp: FloatingActionButton
    private lateinit var mScrollDown: FloatingActionButton
    private lateinit var mHistoryContent: LinearLayout
    private lateinit var mHistoryListView: ListView
    private lateinit var mRecyclerView: RecyclerView

    private var mHistoryList = ArrayList<BookSearch>()
    private lateinit var mHistoryListener: BookSearchHistoryListener
    private lateinit var mSearchListener: BookSearchListener

    private val mHandler = Handler(Looper.getMainLooper())
    private val mDismissUpButton = Runnable { mScrollUp.hide() }
    private val mDismissDownButton = Runnable { mScrollDown.hide() }

    private var mInitialSearch: BookSearch? = null
    private var mFontSize: Float = GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE_DEFAULT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        arguments?.let {
            val book = it.getSerializable(GeneralConsts.KEYS.OBJECT.BOOK) as Book
            val path = it.getString(GeneralConsts.KEYS.OBJECT.DOCUMENT_PATH)
            val password = it.getString(GeneralConsts.KEYS.OBJECT.DOCUMENT_PASSWORD)
            val fontSize = it.getInt(GeneralConsts.KEYS.OBJECT.DOCUMENT_FONT_SIZE)
            val isJapaneseStyle = it.getBoolean(GeneralConsts.KEYS.OBJECT.DOCUMENT_JAPANESE_STYLE)

            if (it.containsKey(GeneralConsts.KEYS.OBJECT.BOOK_FONT_SIZE))
                mFontSize = it.getFloat(GeneralConsts.KEYS.OBJECT.BOOK_FONT_SIZE)

            val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            val parse = SharedData.getDocumentParse() ?: DocumentParse(path!!, password!!, fontSize, isLandscape, isJapaneseStyle,this)
            mViewModelBookSearch.initialize(requireContext(), book, parse)

            if (it.containsKey(GeneralConsts.KEYS.OBJECT.BOOK_SEARCH)) {
                mInitialSearch = it.getSerializable(GeneralConsts.KEYS.OBJECT.BOOK_SEARCH) as BookSearch
                it.remove(GeneralConsts.KEYS.OBJECT.BOOK_SEARCH)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_book_search, menu)
        super.onCreateOptionsMenu(menu, inflater)

        miSearch = menu.findItem(R.id.menu_book_search)
        searchView = miSearch.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null)
                    mViewModelBookSearch.search(query)
                else
                    mViewModelBookSearch.clearSearch()

                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        val searchSrcTextView = miSearch.actionView!!.findViewById<View>(Resources.getSystem().getIdentifier("search_src_text", "id", "android")) as AutoCompleteTextView
        searchSrcTextView.setTextAppearance(R.style.SearchShadow)

        searchView.setOnCloseListener {
            mViewModelBookSearch.clearSearch()
            false
        }

        if (mInitialSearch != null) {
            searchView.setQuery(mInitialSearch!!.search, true)
            mInitialSearch = null
            searchView.isIconified = false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_book_search, container, false)

        mRecyclerView = root.findViewById(R.id.book_search_recycler_view)
        mHistoryContent = root.findViewById(R.id.book_search_history_content)
        mHistoryListView = root.findViewById(R.id.book_search_history_list)

        mClearHistory = root.findViewById(R.id.book_search_history_clear)

        mProgressContent = root.findViewById(R.id.book_search_progress_content)
        mProgressIndicator = root.findViewById(R.id.book_search_in_progress)
        mSearchStop = root.findViewById(R.id.book_search_stop)
        mScrollUp = root.findViewById(R.id.book_search_scroll_up)
        mScrollDown = root.findViewById(R.id.book_search_scroll_down)

        mToolbar = root.findViewById(R.id.toolbar_book_search)

        (requireActivity() as MenuActivity).setActionBar(mToolbar)

        mProgressContent.visibility = if (mViewModelBookSearch.parse != null && mViewModelBookSearch.parse!!.isLoading()) View.VISIBLE else View.GONE
        mScrollUp.visibility = View.GONE
        mScrollDown.visibility = View.GONE

        mSearchStop.setOnClickListener {
            mViewModelBookSearch.stopSearch = true
        }

        mClearHistory.setOnClickListener {
            mViewModelBookSearch.deleteAll()
        }

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

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mHistoryListener = object : BookSearchHistoryListener {
            override fun onClick(search: BookSearch) {
                searchView.isIconified = false
                searchView.setQuery(search.search, true)
            }

            override fun onDelete(search: BookSearch, view: View, position: Int) {
                mViewModelBookSearch.delete(search)
            }
        }

        mHistoryListView.adapter = BookSearchHistoryAdapter(requireContext(), R.layout.line_card_book_search_history, mHistoryList, mHistoryListener)

        mSearchListener = object : BookSearchListener {
            override fun onClick(search: BookSearch) {
                val bundle = Bundle()
                bundle.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK_SEARCH, search)
                (requireActivity() as MenuActivity).onBack(bundle)
            }

            override fun onClickLong(search: BookSearch, view: View, position: Int) {
                val wrapper = ContextThemeWrapper(requireContext(), R.style.PopupMenu)
                val popup = PopupMenu(wrapper, view, 0, R.attr.popupMenuStyle, R.style.PopupMenu)
                popup.menuInflater.inflate(R.menu.menu_item_book_search, popup.menu)

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_item_book_search_add_annotation -> {
                            mViewModelAnnotations.save(search.toAnnotation(mViewModelBookSearch.getPageCount(), mFontSize))
                        }
                    }
                    true
                }

                popup.show()
            }

        }

        val adapter = BookSearchLineAdapter()
        adapter.attachListener(mSearchListener)
        mRecyclerView.adapter = adapter
        mRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        observer()
    }

    private fun observer() {
        mViewModelBookSearch.inSearching.observe(viewLifecycleOwner) {
            mProgressContent.visibility = if (it) View.VISIBLE else View.GONE
        }

        mViewModelBookSearch.search.observe(viewLifecycleOwner) {
            mRecyclerView.visibility = if (it.isNotEmpty()) View.VISIBLE else View.GONE
            (mRecyclerView.adapter as BookSearchLineAdapter).updateList(it)
        }

        mViewModelBookSearch.history.observe(viewLifecycleOwner) {
            mHistoryList.clear()
            mHistoryList.addAll(it)
            (mHistoryListView.adapter as BookSearchHistoryAdapter).notifyDataSetChanged()
        }
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

    override fun onLoading(isFinished: Boolean, isLoaded: Boolean) {
        if (::mProgressContent.isInitialized)
            mProgressContent.visibility = if (isFinished) View.GONE else View.VISIBLE
    }

    override fun onSearching(isSearching: Boolean) {

    }

    override fun onConverting(isConverting: Boolean) {

    }

}