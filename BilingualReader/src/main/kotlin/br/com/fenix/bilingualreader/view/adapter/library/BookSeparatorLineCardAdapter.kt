package br.com.fenix.bilingualreader.view.adapter.library

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Separator
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.BookCardListener
import br.com.fenix.bilingualreader.util.helpers.AdapterUtil.AdapterUtils

class BookSeparatorLineCardAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), BaseAdapter<Book, BookCardListener> {

    private lateinit var mListener: BookCardListener
    private var mBookList: MutableList<Any> = mutableListOf()
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
            else -> BookLineViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.line_card_book, parent, false),
                mListener
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            HEADER -> {
                (holder as BookSeparatorHeaderViewHolder).bind(mBookList[position] as Separator)
            }
            else -> {
                (holder as BookLineViewHolder).bind(mBookList[position] as Book)
                if (isAnimation)
                    holder.itemView.animation =
                        AnimationUtils.loadAnimation(holder.itemView.context, R.anim.animation_library_line)
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        when (holder.itemViewType) {
            HEADER -> {}
            else -> holder.itemView.clearAnimation()
        }
        super.onViewDetachedFromWindow(holder)
    }

    override fun getItemCount(): Int = mBookList.size

    override fun attachListener(listener: BookCardListener) {
        mListener = listener
    }

    override fun removeList(item: Book) {
        val index = mBookList.indexOf(item)
        if (index != -1) {
            mBookList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    override fun getItem(position: Int): Book? {
        val item = mBookList.getOrNull(position)
        return if (item is Book) item else null
    }

    override fun updateList(order: Order, list: MutableList<Book>) {
        val currentSize = mBookList.size
        if (order == Order.None || list.isEmpty()) {
            mBookList = list.toMutableList()
        } else {
            val newList = mutableListOf<Any>()
            var last = AdapterUtils.getBookSeparator(context, order, list[0])
            var count = 0
            newList.add(last)
            list.forEach {
                val item = AdapterUtils.getBookSeparator(context, order, it)
                if (last != item) {
                    last.items = count
                    last = item
                    count = 0
                    newList.add(item)
                }
                count++
                newList.add(it)
            }
            last.items = count
            mBookList = newList
        }
        notifyItemRangeRemoved(0, currentSize)
        notifyItemRangeInserted(0, mBookList.size)
    }
}
