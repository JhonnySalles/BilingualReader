package br.com.fenix.bilingualreader.view.ui.vocabulary.book

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.service.listener.VocabularyCardListener
import br.com.fenix.bilingualreader.view.adapter.vocabulary.VocabularyBookCardAdapter
import br.com.fenix.bilingualreader.view.adapter.vocabulary.VocabularyBookListCardAdapter
import br.com.fenix.bilingualreader.view.adapter.vocabulary.VocabularyLoadState
import br.com.fenix.bilingualreader.view.components.ComponentsUtil
import br.com.fenix.bilingualreader.view.components.InitializeVocabulary
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory


class VocabularyBookFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener,
    InitializeVocabulary<Book> {

    private val mLOGGER = LoggerFactory.getLogger(VocabularyBookFragment::class.java)

    private val mViewModel: VocabularyBookViewModel by viewModels()

    private var mBook: Book? = null

    private lateinit var mRoot: ConstraintLayout
    private lateinit var mRefreshLayout: SwipeRefreshLayout
    private lateinit var mScrollUp: FloatingActionButton
    private lateinit var mScrollDown: FloatingActionButton
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mBookContent: LinearLayout
    private lateinit var mBookName: TextInputLayout
    private lateinit var mBookNameEditText: TextInputEditText

    private lateinit var mFavorite: MenuItem
    private lateinit var miSearch: MenuItem
    private lateinit var searchView: SearchView

    private lateinit var mListener: VocabularyCardListener

    private var mHandler = Handler(Looper.getMainLooper())

    private val mDismissUpButton = Runnable { mScrollUp.hide() }
    private val mDismissDownButton = Runnable { mScrollDown.hide() }

    private val mSetQuery = Runnable {
        mViewModel.setQuery(
            mBookNameEditText.text?.toString() ?: "",
            searchView.query.toString()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_vocabulary, menu)
        super.onCreateOptionsMenu(menu, inflater)

        mFavorite = menu.findItem(R.id.menu_vocabulary_favorite)

        setFavorite(mViewModel.getFavorite())
        mFavorite.setOnMenuItemClickListener {
            val favorite = !mViewModel.getFavorite()
            mFavorite.setIcon(if (favorite) R.drawable.ico_animated_favorited_marked else R.drawable.ico_animated_favorited_unmarked)
            (mFavorite.icon as AnimatedVectorDrawable).start()
            mViewModel.setQueryFavorite(favorite)
            true
        }

        val miOrder = menu.findItem(R.id.menu_vocabulary_list_order)
        miOrder.setOnMenuItemClickListener {
            mViewModel.setQueryOrder(!mViewModel.getOrder())
            true
        }

        miSearch = menu.findItem(R.id.menu_vocabulary_search)
        searchView = miSearch.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                mViewModel.setQuery(
                    mBookNameEditText.text?.toString() ?: "",
                    query ?: ""
                )
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

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
        val root = inflater.inflate(R.layout.fragment_vocabulary_book, container, false)

        mRoot = root.findViewById(R.id.vocabulary_book_root)
        mRecyclerView = root.findViewById(R.id.vocabulary_book_recycler)
        mRefreshLayout = root.findViewById(R.id.vocabulary_book_refresh)

        mBookContent = root.findViewById(R.id.vocabulary_book_content)
        mBookName = root.findViewById(R.id.vocabulary_book_text)
        mBookNameEditText = root.findViewById(R.id.vocabulary_book_edittext)

        mScrollUp = root.findViewById(R.id.vocabulary_book_scroll_up)
        mScrollDown = root.findViewById(R.id.vocabulary_book_scroll_down)

        mScrollUp.visibility = View.GONE
        mScrollDown.visibility = View.GONE

        mViewModel.isQuery.observe(viewLifecycleOwner) {
            mRefreshLayout.isRefreshing = it
        }

        ComponentsUtil.setThemeColor(requireContext(), mRefreshLayout)
        mRefreshLayout.setOnRefreshListener(this)
        mRefreshLayout.isEnabled = true

        mBookNameEditText.addTextChangedListener(object : TextWatcher {
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

        mScrollUp.setOnClickListener { mRecyclerView.smoothScrollToPosition(0) }
        mScrollDown.setOnClickListener {
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

        mBook?.let {
            mBookNameEditText.setText(it.name)
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

        val adapter = VocabularyBookCardAdapter(mListener)
        mRecyclerView.adapter = adapter.withLoadStateFooter(VocabularyLoadState())
        mRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            mViewModel.vocabularyPager().collectLatest {
                adapter.submitData(it)
            }
        }
    }

    override fun onDestroy() {
        VocabularyBookListCardAdapter.clearVocabularyBookList()

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
            mViewModel.setQuery(
                mBookNameEditText.text.toString(),
                searchView.query.toString(),
                mFavorite.isChecked
            )
        else
            mViewModel.setQuery(mBookNameEditText.text.toString(), "", mFavorite.isChecked)
    }

    var mInitialVocabulary = ""
    override fun setVocabulary(vocabulary: String) {
        mInitialVocabulary = vocabulary
    }

    override fun setObject(obj: Book) {
        mBook = obj
    }
}