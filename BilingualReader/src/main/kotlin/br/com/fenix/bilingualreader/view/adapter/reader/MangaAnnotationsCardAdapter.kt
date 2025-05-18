package br.com.fenix.bilingualreader.view.adapter.reader

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.MangaAnnotation
import br.com.fenix.bilingualreader.service.listener.MangaAnnotationListener

class MangaAnnotationsCardAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var mListener: MangaAnnotationListener
    private var mList: List<MangaAnnotation> = listOf()

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

    override fun getItemViewType(position: Int): Int = if (mList[position].isTitle) HEADER else CONTENT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER -> MangaAnnotationHeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.grid_card_manga_annnotation_separator, parent, false))
            else -> MangaAnnotationsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.grid_card_manga_annotation, parent, false), mListener)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            HEADER -> {
                (holder as MangaAnnotationHeaderViewHolder).bind(mList[position])
            }
            else -> {
                (holder as MangaAnnotationsViewHolder).bind(mList[position])
            }
        }
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    fun attachListener(listener: MangaAnnotationListener) {
        mListener = listener
    }

    fun updateList(list: List<MangaAnnotation>) {
        mList = list
        notifyDataSetChanged()
    }

}