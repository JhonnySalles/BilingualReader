package br.com.fenix.bilingualreader.view.adapter.library

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.MangaGroup
import br.com.fenix.bilingualreader.model.entity.Separator
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.MangaCardListener
import br.com.fenix.bilingualreader.util.helpers.AdapterUtil.AdapterUtils
import br.com.fenix.bilingualreader.view.components.LibraryCardAnimator

class MangaSeriesCardAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), BaseAdapter<Manga, MangaCardListener> {

    private lateinit var mListener: MangaCardListener
    private var mMangaList: MutableList<Any> = mutableListOf()
    private var mOrder: Order = Order.Name
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
            else -> MangaSeriesViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.line_card_manga_series, parent, false),
                mListener
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && LibraryCardAnimator.hasNoAnimationPayload(payloads)) {
            when (getItemViewType(position)) {
                HEADER -> (holder as MangaSeparatorHeaderViewHolder).bind(mMangaList[position] as Separator)
                else -> (holder as MangaSeriesViewHolder).bind(mMangaList[position] as MangaGroup, mOrder)
            }
            LibraryCardAnimator.clear(holder)
            return
        }
        onBindViewHolder(holder, position)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            HEADER -> {
                (holder as MangaSeparatorHeaderViewHolder).bind(mMangaList[position] as Separator)
                if (isAnimation)
                    LibraryCardAnimator.animate(holder.itemView, LibraryCardAnimator.Style.LINE)
                else
                    LibraryCardAnimator.clear(holder)
            }
            else -> {
                (holder as MangaSeriesViewHolder).bind(mMangaList[position] as MangaGroup, mOrder)
                if (isAnimation)
                    LibraryCardAnimator.animate(holder.itemView, LibraryCardAnimator.Style.LINE)
                else
                    LibraryCardAnimator.clear(holder)
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        LibraryCardAnimator.clear(holder)
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        LibraryCardAnimator.clear(holder)
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int = mMangaList.size

    override fun attachListener(listener: MangaCardListener) {
        mListener = listener
    }

    override fun removeList(item: Manga) {
        for (i in mMangaList.indices.reversed()) {
            val group = mMangaList[i]
            if (group is MangaGroup) {
                val index = group.items.indexOf(item)
                if (index != -1) {
                    group.items.removeAt(index)
                    if (group.items.isEmpty()) {
                        mMangaList.removeAt(i)
                        if (i > 0 && mMangaList[i - 1] is Separator)
                            mMangaList.removeAt(i - 1)
                    }
                    notifyDataSetChanged()
                    return
                }
            }
        }
    }

    override fun getItem(position: Int): Manga? = null

    override fun updateList(order: Order, list: MutableList<Manga>) {
        mOrder = order
        if (order == Order.None || list.isEmpty()) {
            mMangaList = list.toMutableList()
        } else {
            val newList = mutableListOf<Any>()
            var last = AdapterUtils.getMangaSeparator(context, order, list[0])
            var groupItems = mutableListOf<Manga>()
            var count = 0
            newList.add(last)
            list.forEach {
                val item = AdapterUtils.getMangaSeparator(context, order, it)
                if (last != item) {
                    last.items = count
                    newList.add(MangaGroup(last.title, groupItems))
                    last = item
                    groupItems = mutableListOf()
                    count = 0
                    newList.add(item)
                }
                count++
                groupItems.add(it)
            }
            last.items = count
            newList.add(MangaGroup(last.title, groupItems))
            mMangaList = newList
        }
        notifyDataSetChanged()
    }
}

class MangaSeriesViewHolder(itemView: View, private val listener: MangaCardListener) :
    RecyclerView.ViewHolder(itemView) {

    companion object {
        private fun createLayout(context: Context): GridLayoutManager {
            val layout = GridLayoutManager(context, 1)
            layout.orientation = RecyclerView.HORIZONTAL
            return layout
        }
    }

    private val layout: GridLayoutManager = createLayout(itemView.context)
    private val adapter: MangaCoverCardAdapter = MangaCoverCardAdapter(listener)

    fun bind(group: MangaGroup, order: Order) {
        val list = itemView.findViewById<RecyclerView>(R.id.manga_series_list)
        list.adapter = adapter
        list.layoutManager = layout
        adapter.updateList(group.items, order)
    }
}
