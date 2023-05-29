package br.com.fenix.bilingualreader.view.adapter.vocabulary

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.VocabularyBook


class VocabularyBookListCardAdapter : RecyclerView.Adapter<VocabularyBookListViewHolder>() {

    companion object {
        private var mBookList: MutableMap<Long, Bitmap?> = mutableMapOf()
        fun clearVocabularyBookList() =
            mBookList.clear()
    }

    private var mList: List<VocabularyBook> = listOf()

    override fun onBindViewHolder(holder: VocabularyBookListViewHolder, position: Int) {
        holder.bind(mList[position], mBookList)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VocabularyBookListViewHolder {
        val item = LayoutInflater.from(parent.context)
            .inflate(R.layout.line_card_vocabulary_book_list, parent, false)
        return VocabularyBookListViewHolder(item)
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    fun updateList(list: List<VocabularyBook>) {
        mList = list
        if (list.size > 10)
            notifyItemChanged(0, 10)
        else
            notifyItemChanged(0, list.size)
    }

}