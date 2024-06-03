package br.com.fenix.bilingualreader.view.adapter.library

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.Separator
import br.com.fenix.bilingualreader.model.enums.LibraryMangaType
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.MangaCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts

class MangaSeparatorGridCardAdapter(var context: Context, var type: LibraryMangaType) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), BaseAdapter<Manga, MangaCardListener> {

    private lateinit var mListener: MangaCardListener
    private var mMangaList: MutableList<*> = mutableListOf<Manga>()
    override var isAnimation: Boolean = true

    private val mFavorite = context.getString(R.string.manga_library_separator_favorite)
    private val mNotFavorite = context.getString(R.string.manga_library_separator_non_favorite)

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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            HEADER -> {
                (holder as MangaSeparatorHeaderViewHolder).bind(mMangaList[position] as Separator)
            }
            else -> {
                (holder as MangaSeparatorGridViewHolder).bind(mMangaList[position] as Manga)
                if (isAnimation)
                    holder.itemView.animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.animation_library_grid)
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        when (holder.itemViewType) {
            HEADER -> { }
            else -> {
                holder.itemView.clearAnimation()
            }
        }
        super.onViewDetachedFromWindow(holder)
    }

    override fun getItemCount(): Int {
        return mMangaList.size
    }

    override fun attachListener(listener: MangaCardListener) {
        mListener = listener
    }

    override fun removeList(manga: Manga) {
        if (mMangaList.contains(manga))
            notifyItemRemoved(mMangaList.indexOf(manga))
        mMangaList.remove(manga)
    }

    private fun getSeparator(order: Order, manga: Manga) : Separator {
        val title = when(order) {
            Order.Name -> manga.title.substring(0, 1).uppercase()
            Order.Date -> GeneralConsts.formatCountDays(context, manga.dateCreate)
            Order.LastAccess  -> GeneralConsts.formatCountDays(context, manga.lastAccess)
            Order.Favorite -> if (manga.favorite) mFavorite else mNotFavorite
            else -> ""
        }
        return Separator(title)
    }

    override fun updateList(order: Order, list: MutableList<Manga>) {
        val currentSize = mMangaList.size
        if (order == Order.None || list.isEmpty())
            mMangaList = list
        else {
            val newList = mutableListOf<Any>()
            var last = getSeparator(order, list[0])
            var count = 0
            newList.add(last)
            list.forEach {
                val item = getSeparator(order, it)
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
        notifyItemRangeRemoved(0, currentSize)
        notifyItemRangeInserted(0, mMangaList.size)
    }

}