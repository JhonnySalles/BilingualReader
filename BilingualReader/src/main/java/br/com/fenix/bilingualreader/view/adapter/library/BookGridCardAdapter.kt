package br.com.fenix.bilingualreader.view.adapter.library

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.service.listener.BookCardListener

class BookGridCardAdapter : RecyclerView.Adapter<BookGridViewHolder>() {

    private lateinit var mListener: BookCardListener
    private var mMangaList: MutableList<Book> = mutableListOf()
    var isAnimation: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookGridViewHolder {
        val item =
            LayoutInflater.from(parent.context).inflate(R.layout.grid_card_manga, parent, false)
        return BookGridViewHolder(item, mListener)
    }

    override fun onBindViewHolder(holder: BookGridViewHolder, position: Int) {
        holder.bind(mMangaList[position])
        if (isAnimation)
            holder.itemView.animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.animation_library_grid)
    }

    override fun onViewDetachedFromWindow(holder: BookGridViewHolder) {
        holder.itemView.clearAnimation()
        super.onViewDetachedFromWindow(holder)
    }

    override fun getItemCount(): Int {
        return mMangaList.size
    }

    fun attachListener(listener: BookCardListener) {
        mListener = listener
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

}