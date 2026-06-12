package br.com.fenix.bilingualreader.view.adapter.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.BookCardListener
import br.com.fenix.bilingualreader.util.helpers.AnimationUtil


class BookLineCardAdapter : RecyclerView.Adapter<BookLineViewHolder>(), BaseAdapter<Book, BookCardListener> {

    private lateinit var mListener: BookCardListener
    private var mMangaList: MutableList<Book> = mutableListOf()
    override var isAnimation: Boolean = true

    override fun onBindViewHolder(holder: BookLineViewHolder, position: Int) {
        holder.bind(mMangaList[position])
        if (isAnimation) {
            holder.itemView.alpha = 0f
            holder.itemView.translationY = 100f
            holder.itemView.scaleX = 0.5f
            holder.itemView.scaleY = 0.5f
            holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        } else {
            holder.itemView.animate().cancel()
            holder.itemView.alpha = 1f
            holder.itemView.translationY = 0f
            holder.itemView.scaleX = 1f
            holder.itemView.scaleY = 1f
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookLineViewHolder {
        val item = LayoutInflater.from(parent.context).inflate(R.layout.line_card_book, parent, false)
        return BookLineViewHolder(item, mListener)
    }

    override fun onBindViewHolder(holder: BookLineViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads.contains(AnimationUtil.PROPERTY_NO_ANIMATION)) {
            holder.bind(mMangaList[position])
            return
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onViewDetachedFromWindow(holder: BookLineViewHolder) {
        holder.itemView.animate().cancel()
        holder.itemView.alpha = 1f
        holder.itemView.translationY = 0f
        holder.itemView.scaleX = 1f
        holder.itemView.scaleY = 1f
        holder.itemView.clearAnimation()
        super.onViewDetachedFromWindow(holder)
    }

    override fun getItemCount(): Int {
        return mMangaList.size
    }

    override fun attachListener(listener: BookCardListener) {
        mListener = listener
    }

    override fun removeList(book: Book) {
        val index = mMangaList.indexOf(book)
        if (index != -1) {
            mMangaList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    override fun getItem(position: Int): Book? {
        return mMangaList.getOrNull(position)
    }

    override fun updateList(order: Order, list: MutableList<Book>) {
        val currentSize = mMangaList.size
        mMangaList = list
        notifyItemRangeRemoved(0, currentSize)
        notifyItemRangeInserted(0, mMangaList.size)
    }

}