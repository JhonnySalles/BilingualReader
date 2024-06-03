package br.com.fenix.bilingualreader.view.adapter.chapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Chapters
import br.com.fenix.bilingualreader.service.listener.ChapterCardListener


class ChaptersGridAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var mListener: ChapterCardListener
    private var mChaptersList: List<Chapters> = listOf()

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

    override fun getItemViewType(position: Int): Int = if (mChaptersList[position].isTitle) HEADER else CONTENT

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            HEADER -> {
                (holder as ChaptersHeaderViewHolder).bind(mChaptersList[position])
            }
            else -> {
                (holder as ChaptersViewHolder).bind(mChaptersList[position], position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER -> ChaptersHeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.grid_separator_header, parent, false))
            else -> ChaptersViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.grid_card_chapters, parent, false), mListener
            )
        }
    }

    override fun getItemCount(): Int {
        return mChaptersList.size
    }

    fun updateList(list: List<Chapters>) {
        mChaptersList = list
        notifyDataSetChanged()
    }

    fun attachListener(listener: ChapterCardListener) {
        mListener = listener
    }

    fun notifyItemChanged(page: Chapters) {
        if (mChaptersList.contains(page))
            notifyItemChanged(mChaptersList.indexOf(page))
    }

}