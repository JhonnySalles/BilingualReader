package br.com.fenix.bilingualreader.view.adapter.library

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Separator
import br.com.fenix.bilingualreader.model.enums.LibraryBookType
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.BookCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts

class BookSeparatorGridCardAdapter(var context: Context, var type: LibraryBookType) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), BaseAdapter<Book, BookCardListener> {

    private lateinit var mListener: BookCardListener
    private var mBookList: MutableList<*> = mutableListOf<Book>()
    override var isAnimation: Boolean = true

    private val mFavorite = context.getString(R.string.book_library_separator_favorite)
    private val mNotFavorite = context.getString(R.string.book_library_separator_non_favorite)

    companion object {
        private const val HEADER = 1
        private const val CONTENT = 0
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)

        val lp = holder.itemView.layoutParams
        when (holder.itemViewType) {
            HEADER -> {
                if (lp is StaggeredGridLayoutManager.LayoutParams)
                    lp.isFullSpan = true
            }
            else -> {
                if (lp is StaggeredGridLayoutManager.LayoutParams)
                    lp.isFullSpan = false
            }
        }
    }

    override fun getItemViewType(position: Int): Int = if (mBookList[position] is Separator) HEADER else CONTENT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER -> BookSeparatorHeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.grid_separator_header, parent, false))
            else ->  {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.grid_card_book, parent, false)
                BookSeparatorGridViewHolder(type, item, mListener)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            HEADER -> {
                (holder as BookSeparatorHeaderViewHolder).bind(mBookList[position] as Separator)
            }
            else -> {
                (holder as BookSeparatorGridViewHolder).bind(mBookList[position] as Book)
                if (isAnimation)
                    holder.itemView.animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.animation_library_grid)
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        when (holder.itemViewType) {
            HEADER -> { }
            else -> {
                holder.itemView.clearAnimation()
            }
        }
        super.onViewDetachedFromWindow(holder)
    }

    override fun getItemCount(): Int {
        return mBookList.size
    }

    override fun attachListener(listener: BookCardListener) {
        mListener = listener
    }

    override fun removeList(book: Book) {
        if (mBookList.contains(book))
            notifyItemRemoved(mBookList.indexOf(book))
        mBookList.remove(book)
    }

    private fun getSeparator(order: Order, book: Book) : Separator {
        val title = when(order) {
            Order.Name -> book.title.substring(0, 1).uppercase()
            Order.Date -> GeneralConsts.formatCountDays(context, book.dateCreate)
            Order.LastAccess  -> GeneralConsts.formatCountDays(context, book.lastAccess)
            Order.Author  -> if (book.author.isEmpty()) "" else book.author.lowercase()
            Order.Genre  -> if (book.genre.isEmpty()) "" else book.genre.lowercase()
            Order.Series  -> if (book.series.isEmpty()) "" else book.series.lowercase()
            Order.Favorite -> if (book.favorite) mFavorite else mNotFavorite
            else -> ""
        }
        return Separator(title)
    }

    override fun updateList(order: Order, list: MutableList<Book>) {
        val currentSize = mBookList.size
        if (order == Order.None || list.isEmpty())
            mBookList = list
        else {
            val newList = mutableListOf<Any>()
            var last = getSeparator(order, list[0])
            var count = 0
            newList.add(last)
            list.forEach {
                val item = getSeparator(order, it)
                if (last != item) {
                    last.items = count
                    last = item
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