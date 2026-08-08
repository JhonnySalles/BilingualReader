package br.com.fenix.bilingualreader.view.adapter.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.MangaCardListener
import br.com.fenix.bilingualreader.view.components.LibraryCardAnimator

class MangaLineCardAdapter : RecyclerView.Adapter<MangaLineViewHolder>(), BaseAdapter<Manga, MangaCardListener> {

    private lateinit var mListener: MangaCardListener
    private var mMangaList: MutableList<Manga> = mutableListOf()
    override var isAnimation: Boolean = true

    override fun onBindViewHolder(holder: MangaLineViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && LibraryCardAnimator.hasNoAnimationPayload(payloads)) {
            holder.bind(mMangaList[position])
            LibraryCardAnimator.clear(holder)
            return
        }
        onBindViewHolder(holder, position)
    }

    override fun onBindViewHolder(holder: MangaLineViewHolder, position: Int) {
        holder.bind(mMangaList[position])
        if (isAnimation)
            LibraryCardAnimator.animate(holder.itemView, LibraryCardAnimator.Style.LINE)
        else
            LibraryCardAnimator.clear(holder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaLineViewHolder {
        val item = LayoutInflater.from(parent.context).inflate(R.layout.line_card_manga, parent, false)
        return MangaLineViewHolder(item, mListener)
    }

    override fun onViewDetachedFromWindow(holder: MangaLineViewHolder) {
        LibraryCardAnimator.clear(holder)
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: MangaLineViewHolder) {
        LibraryCardAnimator.clear(holder)
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int = mMangaList.size

    override fun attachListener(listener: MangaCardListener) {
        mListener = listener
    }

    override fun removeList(item: Manga) {
        val index = mMangaList.indexOf(item)
        if (index != -1) {
            mMangaList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    override fun getItem(position: Int): Manga? = mMangaList.getOrNull(position)

    override fun updateList(order: Order, list: MutableList<Manga>) {
        mMangaList = list
        notifyDataSetChanged()
    }
}
