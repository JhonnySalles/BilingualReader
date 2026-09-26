package br.com.fenix.bilingualreader.view.adapter.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Separator
import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.service.listener.HistoryCardListener
import br.com.fenix.bilingualreader.view.components.LibraryCardAnimator


class HistoryLineCardAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(), HistoryBaseAdapter {

    private lateinit var mListener: HistoryCardListener
    private var mHistoryList: ArrayList<Any> = arrayListOf()
    override var isAnimation: Boolean = true

    companion object {
        private const val HEADER = 1
        private const val CONTENT = 0
    }

    override fun getItemViewType(position: Int): Int =
        if (mHistoryList[position] is Separator) HEADER else CONTENT

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            HEADER -> {
                (holder as HistoryHeaderViewHolder).bind(mHistoryList[position] as Separator)

                if (isAnimation)
                    LibraryCardAnimator.animate(holder.itemView, LibraryCardAnimator.Style.SEPARATOR_TITLE_RIGHT)
                else
                    LibraryCardAnimator.clear(holder)
            }
            else -> {
                (holder as HistoryViewHolder).bind(mHistoryList[position] as History)

                if (isAnimation)
                    LibraryCardAnimator.animate(holder.itemView, LibraryCardAnimator.Style.LINE)
                else
                    LibraryCardAnimator.clear(holder)
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
