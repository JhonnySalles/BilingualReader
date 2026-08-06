package br.com.fenix.bilingualreader.view.adapter.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.LibraryMangaType
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.MangaCardListener
import br.com.fenix.bilingualreader.view.components.LibraryCardAnimator

class MangaGridCardAdapter(var type: LibraryMangaType) : RecyclerView.Adapter<MangaGridViewHolder>(), BaseAdapter<Manga, MangaCardListener> {

    private lateinit var mListener: MangaCardListener
    private var mMangaList: MutableList<Manga> = mutableListOf()
    override var isAnimation: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaGridViewHolder {
        val item = LayoutInflater.from(parent.context).inflate(R.layout.grid_card_manga, parent, false)
        return MangaGridViewHolder(type, item, mListener)
    }

    override fun onBindViewHolder(holder: MangaGridViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && LibraryCardAnimator.hasNoAnimationPayload(payloads)) {
            holder.bind(mMangaList[position])
            LibraryCardAnimator.clear(holder)
            return
        }
        onBindViewHolder(holder, position)
    }

    override fun onBindViewHolder(holder: MangaGridViewHolder, position: Int) {
        holder.bind(mMangaList[position])
        if (isAnimation)
            LibraryCardAnimator.animate(holder.itemView, LibraryCardAnimator.Style.GRID)
        else
            LibraryCardAnimator.clear(holder)
    }

    override fun onViewDetachedFromWindow(holder: MangaGridViewHolder) {
        LibraryCardAnimator.clear(holder)
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: MangaGridViewHolder) {
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
