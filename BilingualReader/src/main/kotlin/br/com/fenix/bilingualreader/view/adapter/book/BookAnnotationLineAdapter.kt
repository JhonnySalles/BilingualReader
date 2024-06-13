package br.com.fenix.bilingualreader.view.adapter.book

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.service.listener.BookAnnotationListener


class BookAnnotationLineAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var mListener: BookAnnotationListener
    private var mBookAnnotationList: MutableList<BookAnnotation> = arrayListOf()

    companion object {
        private const val HEADER = 1
        private const val CONTENT = 0
    }

    override fun getItemViewType(position: Int): Int = if (mBookAnnotationList[position].id == null) HEADER else CONTENT

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            HEADER -> {
                (holder as BookAnnotationHeaderViewHolder).bind(mBookAnnotationList[position], position == 0)
            }
            else -> {
                (holder as BookAnnotationViewHolder).bind(mBookAnnotationList[position], position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER -> BookAnnotationHeaderViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.line_card_divider_book_annotation, parent, false), mListener
            )
            else -> BookAnnotationViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.line_card_book_annotation, parent, false), mListener
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

    fun attachListener(listener: BookAnnotationListener) {
        mListener = listener
    }

    fun notifyItemChanged(mark: BookAnnotation) {
        if (mBookAnnotationList.contains(mark))
            notifyItemChanged(mBookAnnotationList.indexOf(mark))
    }

}