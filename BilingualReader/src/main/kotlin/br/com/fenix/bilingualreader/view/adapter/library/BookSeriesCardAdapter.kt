package br.com.fenix.bilingualreader.view.adapter.library

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookGroup
import br.com.fenix.bilingualreader.model.entity.Separator
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.BookCardListener
import br.com.fenix.bilingualreader.util.helpers.AdapterUtil.AdapterUtils

class BookSeriesCardAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), BaseAdapter<Book, BookCardListener> {

    private lateinit var mListener: BookCardListener
    private var mBookList: MutableList<Any> = mutableListOf()
    private var mOrder: Order = Order.Name
    override var isAnimation: Boolean = true

    companion object {
        private const val HEADER = 1
        private const val CONTENT = 0
    }

    override fun getItemViewType(position: Int): Int =
        if (mBookList[position] is Separator) HEADER else CONTENT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER -> BookSeparatorHeaderViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.grid_separator_header, parent, false)
            )
            else -> BookSeriesViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.line_card_book_series, parent, false),
                mListener
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            HEADER -> {
                (holder as BookSeparatorHeaderViewHolder).bind(mBookList[position] as Separator)
                if (isAnimation)
                    holder.itemView.animation =
                        AnimationUtils.loadAnimation(holder.itemView.context, R.anim.animation_library_line)
            }
            else -> {
                (holder as BookSeriesViewHolder).bind(mBookList[position] as BookGroup, mOrder)
                if (isAnimation)
                    holder.itemView.animation =
                        AnimationUtils.loadAnimation(holder.itemView.context, R.anim.animation_library_line)
            }
        }
    }

    override fun getItemCount(): Int = mBookList.size

    override fun attachListener(listener: BookCardListener) {
        mListener = listener
    }

    override fun removeList(item: Book) {
        for (i in mBookList.indices.reversed()) {
            val group = mBookList[i]
            if (group is BookGroup) {
                val index = group.items.indexOf(item)
                if (index != -1) {
                    group.items.removeAt(index)
                    if (group.items.isEmpty()) {
                        mBookList.removeAt(i)
                        if (i > 0 && mBookList[i - 1] is Separator)
                            mBookList.removeAt(i - 1)
                    }
                    notifyDataSetChanged()
                    return
                }
            }
        }
    }

    override fun getItem(position: Int): Book? = null

    override fun updateList(order: Order, list: MutableList<Book>) {
        mOrder = order
        val currentSize = mBookList.size
        if (order == Order.None || list.isEmpty()) {
            mBookList = list.toMutableList()
        } else {
            val newList = mutableListOf<Any>()
            var last = AdapterUtils.getBookSeparator(context, order, list[0])
            var groupItems = mutableListOf<Book>()
            var count = 0
            newList.add(last)
            list.forEach {
                val item = AdapterUtils.getBookSeparator(context, order, it)
                if (last != item) {
                    last.items = count
                    newList.add(BookGroup(last.title, groupItems))
                    last = item
                    groupItems = mutableListOf()
                    count = 0
                    newList.add(item)
                }
                count++
                groupItems.add(it)
            }
            last.items = count
            newList.add(BookGroup(last.title, groupItems))
            mBookList = newList
        }
        notifyItemRangeRemoved(0, currentSize)
        notifyItemRangeInserted(0, mBookList.size)
    }
}

class BookSeriesViewHolder(itemView: View, private val listener: BookCardListener) :
    RecyclerView.ViewHolder(itemView) {

    companion object {
        private fun createLayout(context: Context): GridLayoutManager {
            val layout = GridLayoutManager(context, 1)
            layout.orientation = RecyclerView.HORIZONTAL
            return layout
        }
    }

    private val layout: GridLayoutManager = createLayout(itemView.context)
    private val adapter: BookCoverCardAdapter = BookCoverCardAdapter(listener)

    fun bind(group: BookGroup, order: Order) {
        val list = itemView.findViewById<RecyclerView>(R.id.book_series_list)
        list.adapter = adapter
        list.layoutManager = layout
        adapter.updateList(group.items, order)
    }
}
