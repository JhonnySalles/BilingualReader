package br.com.fenix.bilingualreader.view.adapter.history

import android.view.LayoutInflater
import android.view.ViewGroup
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

                if (isAnimation) {
                    holder.itemView.alpha = 0f
                    holder.itemView.translationX = 100f
                    holder.itemView.animate()
                        .alpha(1f)
                        .translationX(0f)
                        .setDuration(400)
                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                        .start()
                } else {
                    holder.itemView.animate().cancel()
                    holder.itemView.alpha = 1f
                    holder.itemView.translationX = 0f
                }
            }
            else -> {
                (holder as HistoryViewHolder).bind(mHistoryList[position])

                if (isAnimation) {
                    holder.itemView.alpha = 0f
                    holder.itemView.scaleX = 0.5f
                    holder.itemView.scaleY = 0.5f
                    holder.itemView.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                        .start()
                } else {
                    holder.itemView.animate().cancel()
                    holder.itemView.alpha = 1f
                    holder.itemView.scaleX = 1f
                    holder.itemView.scaleY = 1f
                }
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        holder.itemView.animate().cancel()
        holder.itemView.alpha = 1f
        holder.itemView.translationX = 0f
        holder.itemView.scaleX = 1f
        holder.itemView.scaleY = 1f
        holder.itemView.clearAnimation()
        super.onViewDetachedFromWindow(holder)
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

    fun remove(history: History) {
        val index = mHistoryList.indexOf(history)
        if (index != -1) {
            mHistoryList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun getItem(position: Int): History? {
        val item = mHistoryList.getOrNull(position)
        return if (item is History) item else null
    }

    fun notifyItemChanged(history: History) {
        if (mHistoryList.contains(history))
            notifyItemChanged(mHistoryList.indexOf(history))
    }

}