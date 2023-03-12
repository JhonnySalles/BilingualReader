package br.com.fenix.bilingualreader.view.ui.book

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.BookMark
import br.com.fenix.bilingualreader.model.entity.BookSearch
import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.BookSearchListener
import br.com.fenix.bilingualreader.view.adapter.book.BookSearchLineAdapter
import br.com.fenix.bilingualreader.view.adapter.vocabulary.VocabularyMangaListCardAdapter
import br.com.fenix.bilingualreader.view.components.InitializeVocabulary
import br.com.fenix.bilingualreader.view.components.PopupOrderListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.slf4j.LoggerFactory


class BookSearchFragment : Fragment(), PopupOrderListener, InitializeVocabulary<Vocabulary> {

    private val mLOGGER = LoggerFactory.getLogger(BookSearchFragment::class.java)

    private lateinit var mScrollUp: FloatingActionButton
    private lateinit var mScrollDown: FloatingActionButton
    private lateinit var mHistoryContent: ListView
    private lateinit var mHistoryListView: ListView
    private lateinit var mRecyclerView: RecyclerView

    private lateinit var mListener: BookSearchListener

    private var mHandler = Handler(Looper.getMainLooper())
    private val mDismissUpButton = Runnable { mScrollUp.hide() }
    private val mDismissDownButton = Runnable { mScrollDown.hide() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_book_mark, menu)
        super.onCreateOptionsMenu(menu, inflater)

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
        val root = inflater.inflate(R.layout.fragment_book_search, container, false)

        mRecyclerView = root.findViewById(R.id.book_search_recycler_view)
        mHistoryContent = root.findViewById(R.id.book_search_history_content)
        mHistoryListView = root.findViewById(R.id.book_search_history)

        mScrollUp = root.findViewById(R.id.book_search_scroll_up)
        mScrollDown = root.findViewById(R.id.book_search_scroll_down)


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

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mListener = object : BookSearchListener {
            override fun onClick(search: BookSearch) {
                TODO("Not yet implemented")
            }

            override fun onClickLong(search: BookSearch, view: View, position: Int) {
                TODO("Not yet implemented")
            }

        }

        val adapter = BookSearchLineAdapter()
        adapter.attachListener(mListener)
        mRecyclerView.adapter = adapter
        mRecyclerView.layoutManager = LinearLayoutManager(requireContext())
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

    var mInitialVocabulary = ""
    override fun setVocabulary(vocabulary: String) {
        mInitialVocabulary = vocabulary
    }

    override fun setObject(obj: Vocabulary) {
        TODO("Not yet implemented")
    }

    override fun popupOrderOnChange() {}

    override fun popupSorted(order: Order) {
        TODO("Not yet implemented")
    }

    override fun popupSorted(order: Order, isDesc: Boolean) {
        TODO("Not yet implemented")
    }

    override fun popupGetOrder(): Pair<Order, Boolean>? {
        TODO("Not yet implemented")
    }

    override fun popupGetObserver(): LiveData<Pair<Order, Boolean>> {
        TODO("Not yet implemented")
    }

}