package br.com.fenix.bilingualreader.view.adapter.vocabulary

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.service.listener.VocabularyCardListener


class VocabularyBookCardAdapter(var listener: VocabularyCardListener) :
    PagingDataAdapter<Vocabulary, VocabularyBookViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VocabularyBookViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.line_card_vocabulary_book, parent, false), listener
        )

    override fun onBindViewHolder(holder: VocabularyBookViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Vocabulary>() {
            override fun areItemsTheSame(oldItem: Vocabulary, newItem: Vocabulary): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Vocabulary, newItem: Vocabulary): Boolean =
                oldItem == newItem
        }
    }
}


