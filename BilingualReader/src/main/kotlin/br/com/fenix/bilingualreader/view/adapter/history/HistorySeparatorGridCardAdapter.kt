package br.com.fenix.bilingualreader.view.adapter.history

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Separator
import br.com.fenix.bilingualreader.model.enums.HistoryType
import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.service.listener.HistoryCardListener
import br.com.fenix.bilingualreader.view.components.LibraryCardAnimator

class HistorySeparatorGridCardAdapter(private val type: HistoryType) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), HistoryBaseAdapter {

    private lateinit var mListener: HistoryCardListener
    private var mHistoryList: ArrayList<Any> = arrayListOf()
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

    override fun getItemViewType(position: Int): Int =
        if (mHistoryList[position] is Separator) HEADER else CONTENT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER -> HistorySeparatorHeaderViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.grid_separator_header, parent, false)
            )
            else -> HistorySeparatorGridViewHolder(
                type,
                LayoutInflater.from(parent.context).inflate(R.layout.grid_card_history, parent, false),
                mListener
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            HEADER -> {
                (holder as HistorySeparatorHeaderViewHolder).bind(mHistoryList[position] as Separator)
                if (isAnimation)
                    LibraryCardAnimator.animate(holder.itemView, LibraryCardAnimator.Style.SEPARATOR_TITLE_CENTER)
                else
                    LibraryCardAnimator.clear(holder)
            }
            else -> {
                (holder as HistorySeparatorGridViewHolder).bind(mHistoryList[position] as History)
                if (isAnimation)
                    LibraryCardAnimator.animate(holder.itemView, LibraryCardAnimator.Style.GRID)
                else
                    LibraryCardAnimator.clear(holder)
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        if (holder.itemViewType != HEADER)
            holder.itemView.clearAnimation()
        super.onViewDetachedFromWindow(holder)
    }

    override fun getItemCount(): Int = mHistoryList.size

    override fun updateList(list: ArrayList<Any>) {
        mHistoryList = list
        notifyDataSetChanged()
    }

    override fun attachListener(listener: HistoryCardListener) {
        mListener = listener
    }

    override fun getItem(position: Int): History? {
        val item = mHistoryList.getOrNull(position)
        return if (item is History) item else null
    }

    override fun remove(history: History) {
        val index = mHistoryList.indexOf(history)
        if (index != -1) {
            mHistoryList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    override fun notifyItemChanged(history: History) {
        if (mHistoryList.contains(history))
            notifyItemChanged(mHistoryList.indexOf(history))
    }
}
