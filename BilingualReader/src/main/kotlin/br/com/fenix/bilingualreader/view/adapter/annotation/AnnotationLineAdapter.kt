package br.com.fenix.bilingualreader.view.adapter.annotation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.entity.MangaAnnotation
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.model.interfaces.Annotation
import br.com.fenix.bilingualreader.service.listener.AnnotationsListener


class AnnotationLineAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var mListener: AnnotationsListener
    private var mAnnotationList: MutableList<Annotation> = arrayListOf()

    companion object {
        private const val MANGA = 4
        private const val BOOK = 3
        private const val ROOT = 2
        private const val TITLE = 1
        private const val CONTENT = 0
    }

    override fun getItemViewType(position: Int): Int {
        val annotation = mAnnotationList[position]
        return if (annotation.isRoot)
            ROOT
        else if (annotation.isTitle)
            TITLE
        else {
            when (annotation.type) {
                Type.MANGA -> MANGA
                Type.BOOK -> BOOK
                else -> CONTENT
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val pos = holder.adapterPosition
        when (getItemViewType(position)) {
            ROOT -> {
                (holder as AnnotationRootViewHolder).bind((mAnnotationList[position]), pos == 0)
            }
            TITLE -> {
                (holder as AnnotationTitleViewHolder).bind((mAnnotationList[position]), mAnnotationList[position-1].isRoot)
            }
            BOOK -> {
                (holder as AnnotationBookViewHolder).bind((mAnnotationList[position] as BookAnnotation), pos)
            }
            MANGA -> {
                (holder as AnnotationMangaViewHolder).bind((mAnnotationList[position] as MangaAnnotation), pos)
            }
            else -> { }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ROOT -> AnnotationRootViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.line_card_divider_annotation_root, parent, false), mListener
            )
            TITLE -> AnnotationTitleViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.line_card_divider_annotation_title, parent, false), mListener
            )
            BOOK -> AnnotationBookViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.line_card_book_annotation, parent, false), mListener
            )
            MANGA -> AnnotationMangaViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.line_card_manga_annotation, parent, false), mListener
            )
            else -> AnnotationViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.line_card_annotation, parent, false), mListener
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

    fun attachListener(listener: AnnotationsListener) {
        mListener = listener
    }

    fun notifyItemChanged(item: Annotation) {
        if (mAnnotationList.contains(item))
            notifyItemChanged(mAnnotationList.indexOf(item))
    }

}