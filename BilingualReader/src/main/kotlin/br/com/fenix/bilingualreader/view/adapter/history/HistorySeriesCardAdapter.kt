package br.com.fenix.bilingualreader.view.adapter.history

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.HistoryGroup
import br.com.fenix.bilingualreader.model.entity.Separator
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.service.listener.HistoryCardListener

class HistorySeriesCardAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(), HistoryBaseAdapter {

    private lateinit var mListener: HistoryCardListener
    private var mHistoryList: ArrayList<Any> = arrayListOf()
    private var mOrder: Order = Order.LastAccess
    override var isAnimation: Boolean = true

    companion object {
        private const val HEADER = 1
        private const val CONTENT = 0
    }

    override fun getItemViewType(position: Int): Int =
        if (mHistoryList[position] is Separator) HEADER else CONTENT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER -> HistoryHeaderViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.line_card_divider_history, parent, false),
                mListener
            )
            else -> HistorySeriesViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.line_card_history_series, parent, false),
                mListener
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            HEADER -> {
                (holder as HistoryHeaderViewHolder).bind(mHistoryList[position] as Separator)
                if (isAnimation)
                    holder.itemView.animation =
                        AnimationUtils.loadAnimation(holder.itemView.context, R.anim.animation_history_holder)
            }
            else -> {
                (holder as HistorySeriesViewHolder).bind(mHistoryList[position] as HistoryGroup, mOrder)
                if (isAnimation)
                    holder.itemView.animation =
                        AnimationUtils.loadAnimation(holder.itemView.context, R.anim.animation_history)
            }
        }
    }

    override fun getItemCount(): Int = mHistoryList.size

    override fun updateList(list: ArrayList<Any>) {
        mHistoryList = list
        notifyDataSetChanged()
    }

    fun setOrder(order: Order) {
        mOrder = order
    }

    override fun attachListener(listener: HistoryCardListener) {
        mListener = listener
    }

    override fun getItem(position: Int): History? = null

    override fun remove(history: History) {
        var changed = false
        for (i in mHistoryList.indices.reversed()) {
            val item = mHistoryList[i]
            if (item is HistoryGroup) {
                val index = item.items.indexOf(history)
                if (index != -1) {
                    item.items.removeAt(index)
                    if (item.items.isEmpty()) {
                        mHistoryList.removeAt(i)
                        if (i > 0 && mHistoryList[i - 1] is Separator)
                            mHistoryList.removeAt(i - 1)
                    }
                    changed = true
                    break
                }
            }
        }
        if (changed)
            notifyDataSetChanged()
    }

    override fun notifyItemChanged(history: History) {
        for (i in mHistoryList.indices) {
            val item = mHistoryList[i]
            if (item is HistoryGroup && item.items.contains(history)) {
                notifyItemChanged(i)
                return
            }
        }
    }
}

class HistorySeriesViewHolder(itemView: View, private val listener: HistoryCardListener) :
    RecyclerView.ViewHolder(itemView) {

    companion object {
        private fun createLayout(context: Context): GridLayoutManager {
            val layout = GridLayoutManager(context, 1)
            layout.orientation = RecyclerView.HORIZONTAL
            return layout
        }
    }

    private val layout: GridLayoutManager = createLayout(itemView.context)
    private val adapter: HistoryCoverCardAdapter = HistoryCoverCardAdapter(listener)

    fun bind(group: HistoryGroup, order: Order) {
        val list = itemView.findViewById<RecyclerView>(R.id.history_series_list)
        list.adapter = adapter
        list.layoutManager = layout
        adapter.updateList(group.items, order)
    }
}
