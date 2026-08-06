package br.com.fenix.bilingualreader.view.adapter.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.enums.LibraryBookType
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.BookCardListener
import br.com.fenix.bilingualreader.view.components.LibraryCardAnimator

class BookGridCardAdapter(var type: LibraryBookType) : RecyclerView.Adapter<BookGridViewHolder>(), BaseAdapter<Book, BookCardListener> {

    private lateinit var mListener: BookCardListener
    private var mMangaList: MutableList<Book> = mutableListOf()
    override var isAnimation: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookGridViewHolder {
        val item = LayoutInflater.from(parent.context).inflate(R.layout.grid_card_book, parent, false)
        return BookGridViewHolder(type, item, mListener)
    }

    override fun onBindViewHolder(holder: BookGridViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && LibraryCardAnimator.hasNoAnimationPayload(payloads)) {
            holder.bind(mMangaList[position])
            LibraryCardAnimator.clear(holder)
            return
        }
        onBindViewHolder(holder, position)
    }

    override fun onBindViewHolder(holder: BookGridViewHolder, position: Int) {
        holder.bind(mMangaList[position])
        if (isAnimation)
            LibraryCardAnimator.animate(holder.itemView, LibraryCardAnimator.Style.GRID)
        else
            LibraryCardAnimator.clear(holder)
    }

    override fun onViewDetachedFromWindow(holder: BookGridViewHolder) {
        LibraryCardAnimator.clear(holder)
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: BookGridViewHolder) {
        LibraryCardAnimator.clear(holder)
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int {
        return mMangaList.size
    }

    override fun attachListener(listener: BookCardListener) {
        mListener = listener
    }

    override fun removeList(item: Book) {
        val index = mMangaList.indexOf(item)
        if (index != -1) {
            mMangaList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    override fun getItem(position: Int): Book? {
        return mMangaList.getOrNull(position)
    }

    override fun updateList(order: Order, list: MutableList<Book>) {
        mMangaList = list
        notifyDataSetChanged()
    }

}
