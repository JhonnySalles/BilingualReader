package br.com.fenix.bilingualreader.view.adapter.library

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.service.listener.BookCardListener


class BookLineCardAdapter : RecyclerView.Adapter<BookLineViewHolder>() {

    private lateinit var mListener: BookCardListener
    private var mMangaList: MutableList<Book> = mutableListOf()
    var isAnimation: Boolean = true

    override fun onBindViewHolder(holder: BookLineViewHolder, position: Int) {
        holder.bind(mMangaList[position])
        if (isAnimation)
            holder.itemView.animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.animation_library_line)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookLineViewHolder {
        val item = LayoutInflater.from(parent.context).inflate(R.layout.line_card_book, parent, false)
        return BookLineViewHolder(item, mListener)
    }

    override fun onViewDetachedFromWindow(holder: BookLineViewHolder) {
        holder.itemView.clearAnimation()
        super.onViewDetachedFromWindow(holder)
    }

    override fun getItemCount(): Int {
        return mMangaList.size
    }

    fun removeList(book: Book) {
        if (mMangaList.contains(book))
            notifyItemRemoved(mMangaList.indexOf(book))
        mMangaList.remove(book)
    }

    fun updateList(list: MutableList<Book>) {
        mMangaList = list
        notifyDataSetChanged()
    }

    fun attachListener(listener: BookCardListener) {
        mListener = listener
    }
}