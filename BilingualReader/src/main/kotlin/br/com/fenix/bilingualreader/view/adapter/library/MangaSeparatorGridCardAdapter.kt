package br.com.fenix.bilingualreader.view.adapter.library

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.Separator
import br.com.fenix.bilingualreader.model.enums.LibraryMangaType
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.MangaCardListener
import br.com.fenix.bilingualreader.util.helpers.AdapterUtil.AdapterUtils
import br.com.fenix.bilingualreader.view.components.LibraryCardAnimator

class MangaSeparatorGridCardAdapter(var context: Context, var type: LibraryMangaType) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), BaseAdapter<Manga, MangaCardListener> {

    private lateinit var mListener: MangaCardListener
    private var mMangaList: MutableList<Any> = mutableListOf()
    override var isAnimation: Boolean = true

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

    override fun getItemViewType(position: Int): Int = if (mMangaList[position] is Separator) HEADER else CONTENT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER -> MangaSeparatorHeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.grid_separator_header, parent, false))
            else ->  {
                val item = LayoutInflater.from(parent.context).inflate(R.layout.grid_card_manga, parent, false)
                MangaSeparatorGridViewHolder(type, item, mListener)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && LibraryCardAnimator.hasNoAnimationPayload(payloads)) {
            when (getItemViewType(position)) {
                HEADER -> (holder as MangaSeparatorHeaderViewHolder).bind(mMangaList[position] as Separator)
                else -> {
                    (holder as MangaSeparatorGridViewHolder).bind(mMangaList[position] as Manga)
                    LibraryCardAnimator.clear(holder)
                }
            }
            return
        }
        onBindViewHolder(holder, position)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            HEADER -> {
                (holder as MangaSeparatorHeaderViewHolder).bind(mMangaList[position] as Separator)
            }
            else -> {
                (holder as MangaSeparatorGridViewHolder).bind(mMangaList[position] as Manga)
                if (isAnimation)
                    LibraryCardAnimator.animate(holder.itemView, LibraryCardAnimator.Style.GRID)
                else
                    LibraryCardAnimator.clear(holder)
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        when (holder.itemViewType) {
            HEADER -> { }
            else -> LibraryCardAnimator.clear(holder)
        }
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        when (holder.itemViewType) {
            HEADER -> { }
            else -> LibraryCardAnimator.clear(holder)
        }
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int {
        return mMangaList.size
    }

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

    override fun getItem(position: Int): Manga? {
        val item = mMangaList.getOrNull(position)
        return if (item is Manga) item else null
    }

    override fun updateList(order: Order, list: MutableList<Manga>) {
        if (order == Order.None || list.isEmpty())
            mMangaList = list.toMutableList()
        else {
            val newList = mutableListOf<Any>()
            var last = AdapterUtils.getMangaSeparator(context, order, list[0])
            var count = 0
            newList.add(last)
            list.forEach {
                val item = AdapterUtils.getMangaSeparator(context, order, it)
                if (last != item) {
                    last.items = count
                    last = item
                    newList.add(item)
                }
                count++
                newList.add(it)
            }
            last.items = count
            mMangaList = newList
        }
        notifyDataSetChanged()
    }

}