package br.com.fenix.bilingualreader.view.adapter.library

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.LibraryMangaType
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.MangaCardListener

class MangaGridCardAdapter(var type: LibraryMangaType) : RecyclerView.Adapter<MangaGridViewHolder>(), BaseAdapter<Manga, MangaCardListener> {

    private lateinit var mListener: MangaCardListener
    private var mMangaList: MutableList<Manga> = mutableListOf()
    override var isAnimation: Boolean = true
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaGridViewHolder {
        val item = LayoutInflater.from(parent.context).inflate(R.layout.grid_card_manga, parent, false)
        return MangaGridViewHolder(type, item, mListener)
    }

    override fun onBindViewHolder(holder: MangaGridViewHolder, position: Int) {
        holder.bind(mMangaList[position])
        if (isAnimation) {
            holder.itemView.alpha = 0f
            holder.itemView.translationX = 100f
            holder.itemView.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(200)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        } else {
            holder.itemView.animate().cancel()
            holder.itemView.alpha = 1f
            holder.itemView.translationX = 0f
        }
    }

    override fun onViewDetachedFromWindow(holder: MangaGridViewHolder) {
        holder.itemView.animate().cancel()
        holder.itemView.alpha = 1f
        holder.itemView.translationX = 0f
        holder.itemView.clearAnimation()
        super.onViewDetachedFromWindow(holder)
    }

    override fun getItemCount(): Int {
        return mMangaList.size
    }

    override fun attachListener(listener: MangaCardListener) {
        mListener = listener
    }

    override fun removeList(manga: Manga) {
        val index = mMangaList.indexOf(manga)
        if (index != -1) {
            mMangaList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    override fun getItem(position: Int): Manga? {
        return mMangaList.getOrNull(position)
    }

    override fun updateList(order: Order, list: MutableList<Manga>) {
        val currentSize = mMangaList.size
        mMangaList = list
        notifyItemRangeRemoved(0, currentSize)
        notifyItemRangeInserted(0, mMangaList.size)
    }

}