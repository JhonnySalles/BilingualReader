package br.com.fenix.bilingualreader.view.adapter.history

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.service.listener.HistoryCardListener


class HistoryCardAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var mListener: HistoryCardListener
    private var mHistoryList: ArrayList<History> = arrayListOf()
    var isAnimation: Boolean = true

    companion object {
        private const val HEADER = 1
        private const val CONTENT = 0
    }

    override fun getItemViewType(position: Int): Int = if (mHistoryList[position].id == null) HEADER else CONTENT

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            HEADER -> {
                (holder as HistoryHeaderViewHolder).bind(mHistoryList[position])

                if (isAnimation)
                    holder.itemView.animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.animation_history_holder)
            }
            else -> {
                (holder as HistoryViewHolder).bind(mHistoryList[position])

                if (isAnimation)
                    holder.itemView.animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.animation_history)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER -> HistoryHeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.line_card_divider_history, parent, false), mListener)
            else -> HistoryViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.line_card_history, parent, false), mListener)
        }
    }

    override fun getItemCount(): Int {
        return mHistoryList.size
    }

    fun updateList(list: ArrayList<History>) {
        mHistoryList = list
        notifyDataSetChanged()
    }

    fun attachListener(listener: HistoryCardListener) {
        mListener = listener
    }

    fun notifyItemChanged(history: History) {
        if (mHistoryList.contains(history))
            notifyItemChanged(mHistoryList.indexOf(history))
    }

}