package br.com.fenix.bilingualreader.view.ui.book

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookSearch
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
import org.ebookdroid.core.codec.CodecDocument
import org.slf4j.LoggerFactory


class BookSearchFragment : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(BookSearchFragment::class.java)

    private val mViewModel: BookSearchViewModel by activityViewModels()
    private val mAnnotations: BookAnnotationViewModel by activityViewModels()

    private lateinit var mToolbar: androidx.appcompat.widget.Toolbar
    private lateinit var miSearch: MenuItem
    private lateinit var searchView: SearchView

    private lateinit var mProgressInSearch: ProgressBar
    private lateinit var mClearHistory: MaterialButton
    private lateinit var mScrollUp: FloatingActionButton
    private lateinit var mScrollDown: FloatingActionButton
    private lateinit var mHistoryContent: LinearLayout
    private lateinit var mHistoryListView: ListView
    private lateinit var mRecyclerView: RecyclerView

    private var mHistoryList = ArrayList<BookSearch>()
    private lateinit var mHistoryListener: BookSearchHistoryListener
    private lateinit var mSearchListener: BookSearchListener

    private var mHandler = Handler(Looper.getMainLooper())
    private val mDismissUpButton = Runnable { mScrollUp.hide() }
    private val mDismissDownButton = Runnable { mScrollDown.hide() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        arguments?.let {
            val book = it.getSerializable(GeneralConsts.KEYS.OBJECT.BOOK) as Book
            val path = it.getString(GeneralConsts.KEYS.OBJECT.DOCUMENT_PATH)
            val password = it.getString(GeneralConsts.KEYS.OBJECT.DOCUMENT_PASSWORD)
            val fontSize = it.getInt(GeneralConsts.KEYS.OBJECT.DOCUMENT_FONT_SIZE)
            mViewModel.initialize(book, SharedData.getDocumentParse() ?: DocumentParse(path!!, password!!, fontSize))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_book_search, menu)
        super.onCreateOptionsMenu(menu, inflater)

        miSearch = menu.findItem(R.id.menu_book_search)
        searchView = miSearch.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null)
                    mViewModel.search(query)
                else
                    mViewModel.clearSearch()

                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        searchView.setOnCloseListener {
            mViewModel.clearSearch()
            false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_book_search, container, false)

        mRecyclerView = root.findViewById(R.id.book_search_recycler_view)
        mHistoryContent = root.findViewById(R.id.book_search_history_content)
        mHistoryListView = root.findViewById(R.id.book_search_history_list)

        mClearHistory = root.findViewById(R.id.book_search_history_clear)

        mProgressInSearch = root.findViewById(R.id.book_search_in_progress)
        mScrollUp = root.findViewById(R.id.book_search_scroll_up)
        mScrollDown = root.findViewById(R.id.book_search_scroll_down)

        mToolbar = root.findViewById(R.id.toolbar_book_search)

        mScrollUp.visibility = View.GONE
        mScrollDown.visibility = View.GONE

        (requireActivity() as MenuActivity).setActionBar(mToolbar)

        mClearHistory.setOnClickListener {
            mViewModel.deleteAll()
        }

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
                mViewModel.delete(search)
            }
        }

        mHistoryListView.adapter = BookSearchHistoryAdapter(
            requireContext(),
            R.layout.line_card_book_search_history,
            mHistoryList,
            mHistoryListener
        )

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
                            mAnnotations.save(search.toAnnotation(mViewModel.getPageCount()))
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
        mViewModel.inSearching.observe(viewLifecycleOwner) {
            mProgressInSearch.visibility = if (it) View.VISIBLE else View.GONE
        }

        mViewModel.search.observe(viewLifecycleOwner) {
            mRecyclerView.visibility = if (it.isNotEmpty()) View.VISIBLE else View.GONE
            (mRecyclerView.adapter as BookSearchLineAdapter).updateList(it)
        }

        mViewModel.history.observe(viewLifecycleOwner) {
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

}