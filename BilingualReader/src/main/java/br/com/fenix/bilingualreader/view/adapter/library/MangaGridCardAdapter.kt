package br.com.fenix.bilingualreader.view.adapter.library

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.service.listener.MangaCardListener

class MangaGridCardAdapter : RecyclerView.Adapter<MangaGridViewHolder>() {

    private lateinit var mListener: MangaCardListener
    private var mMangaList: MutableList<Manga> = mutableListOf()
    var isAnimation: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaGridViewHolder {
        val item =
            LayoutInflater.from(parent.context).inflate(R.layout.grid_card_manga, parent, false)
        return MangaGridViewHolder(item, mListener)
    }

    override fun onBindViewHolder(holder: MangaGridViewHolder, position: Int) {
        holder.bind(mMangaList[position])
        if (isAnimation)
            holder.itemView.animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.animation_library_grid)
    }

    override fun onViewDetachedFromWindow(holder: MangaGridViewHolder) {
        holder.itemView.clearAnimation()
        super.onViewDetachedFromWindow(holder)
    }

    override fun getItemCount(): Int {
        return mMangaList.size
    }

    fun attachListener(listener: MangaCardListener) {
        mListener = listener
    }

    fun removeList(manga: Manga) {
        if (mMangaList.contains(manga))
            notifyItemRemoved(mMangaList.indexOf(manga))
        mMangaList.remove(manga)
    }

    fun updateList(list: MutableList<Manga>) {
        mMangaList = list
        notifyDataSetChanged()
    }

}