package br.com.fenix.bilingualreader.view.adapter.book

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.service.listener.AnnotationsListener


class BookAnnotationLineAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var mListener: AnnotationsListener
    private var mBookAnnotationList: MutableList<BookAnnotation> = arrayListOf()

    companion object {
        private const val HEADER = 1
        private const val CONTENT = 0
    }

    override fun getItemViewType(position: Int): Int = if (mBookAnnotationList[position].id == null) HEADER else CONTENT

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val pos = holder.adapterPosition
        when (getItemViewType(position)) {
            HEADER -> {
                (holder as BookAnnotationHeaderViewHolder).bind(mBookAnnotationList[position], pos == 0)
            }
            else -> {
                (holder as BookAnnotationViewHolder).bind(mBookAnnotationList[position], pos)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER -> BookAnnotationHeaderViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.line_card_divider_annotation_title, parent, false), mListener
            )
            else -> BookAnnotationViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.line_card_book_annotation, parent, false), mListener
            )
        }
    }

    override fun getItemCount(): Int {
        return mBookAnnotationList.size
    }

    fun updateList(list: MutableList<BookAnnotation>) {
        mBookAnnotationList = list
        notifyDataSetChanged()
    }

    fun attachListener(listener: AnnotationsListener) {
        mListener = listener
    }

    fun notifyItemChanged(annotation: BookAnnotation) {
        if (mBookAnnotationList.contains(annotation))
            notifyItemChanged(mBookAnnotationList.indexOf(annotation))
    }

}