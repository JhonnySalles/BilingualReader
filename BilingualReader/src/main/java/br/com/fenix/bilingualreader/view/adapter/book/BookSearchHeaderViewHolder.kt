package br.com.fenix.bilingualreader.view.adapter.book

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.BookSearch
import br.com.fenix.bilingualreader.service.listener.BookSearchListener

class BookSearchHeaderViewHolder(itemView: View, private val listener: BookSearchListener) :
    RecyclerView.ViewHolder(itemView) {

    fun bind(search: BookSearch) {
        itemView.findViewById<TextView>(R.id.book_search_title).text = search.search
    }

}