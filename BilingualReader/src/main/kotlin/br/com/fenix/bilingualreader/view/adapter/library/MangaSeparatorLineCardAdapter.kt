package br.com.fenix.bilingualreader.view.adapter.library

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.Separator
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.MangaCardListener
import br.com.fenix.bilingualreader.util.helpers.AdapterUtil.AdapterUtils

class MangaSeparatorLineCardAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), BaseAdapter<Manga, MangaCardListener> {

    private lateinit var mListener: MangaCardListener
    private var mMangaList: MutableList<Any> = mutableListOf()
    override var isAnimation: Boolean = true

    companion object {
        private const val HEADER = 1
        private const val CONTENT = 0
    }

    override fun getItemViewType(position: Int): Int =
        if (mMangaList[position] is Separator) HEADER else CONTENT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER -> MangaSeparatorHeaderViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.grid_separator_header, parent, false)
            )
            else -> MangaLineViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.line_card_manga, parent, false),
                mListener
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            HEADER -> {
                (holder as MangaSeparatorHeaderViewHolder).bind(mMangaList[position] as Separator)
            }
            else -> {
                (holder as MangaLineViewHolder).bind(mMangaList[position] as Manga)
                if (isAnimation)
                    holder.itemView.animation =
                        AnimationUtils.loadAnimation(holder.itemView.context, R.anim.animation_library_line)
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        when (holder.itemViewType) {
            HEADER -> {}
            else -> holder.itemView.clearAnimation()
        }
        super.onViewDetachedFromWindow(holder)
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

    override fun getItem(position: Int): Manga? {
        val item = mMangaList.getOrNull(position)
        return if (item is Manga) item else null
    }

    override fun updateList(order: Order, list: MutableList<Manga>) {
        val currentSize = mMangaList.size
        if (order == Order.None || list.isEmpty()) {
            mMangaList = list.toMutableList()
        } else {
            val newList = mutableListOf<Any>()
            var last = AdapterUtils.getMangaSeparator(context, order, list[0])
            var count = 0
            newList.add(last)
            list.forEach {
                val item = AdapterUtils.getMangaSeparator(context, order, it)
                if (last != item) {
                    last.items = count
                    last = item
                    count = 0
                    newList.add(item)
                }
                count++
                newList.add(it)
            }
            last.items = count
            mMangaList = newList
        }
        notifyItemRangeRemoved(0, currentSize)
        notifyItemRangeInserted(0, mMangaList.size)
    }
}
