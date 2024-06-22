package br.com.fenix.bilingualreader.view.adapter.annotation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.interfaces.Annotation
import br.com.fenix.bilingualreader.service.listener.BookAnnotationListener


class AnnotationLineAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var mListener: BookAnnotationListener
    private var mAnnotationList: MutableList<Annotation> = arrayListOf()

    companion object {
        private const val BOOK = 2
        private const val TITLE = 1
        private const val CONTENT = 0
    }

    override fun getItemViewType(position: Int): Int = if (mAnnotationList[position].isRoot) BOOK else if (mAnnotationList[position].isTitle) TITLE else CONTENT

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val pos = holder.adapterPosition
        when (getItemViewType(position)) {
            BOOK -> {
                (holder as AnnotationBookViewHolder).bind((mAnnotationList[position] as BookAnnotation), pos == 0)
            }
            TITLE -> {
                (holder as AnnotationTitleViewHolder).bind((mAnnotationList[position] as BookAnnotation), mAnnotationList[position-1].isRoot)
            }
            else -> {
                (holder as AnnotationViewHolder).bind((mAnnotationList[position] as BookAnnotation), pos)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            BOOK -> AnnotationBookViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.line_card_divider_book_title, parent, false), mListener
            )
            TITLE -> AnnotationTitleViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.line_card_divider_book_annotation, parent, false), mListener
            )
            else -> AnnotationViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.line_card_book_annotation, parent, false), mListener
            )
        }
    }

    override fun getItemCount(): Int {
        return mAnnotationList.size
    }

    fun updateList(list: MutableList<Annotation>) {
        mAnnotationList = list
        notifyDataSetChanged()
    }

    fun attachListener(listener: BookAnnotationListener) {
        mListener = listener
    }

    fun notifyItemChanged(item: Annotation) {
        if (mAnnotationList.contains(item))
            notifyItemChanged(mAnnotationList.indexOf(item))
    }

}