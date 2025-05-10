package br.com.fenix.bilingualreader.view.adapter.book

import android.text.Html
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.BookSearch
import br.com.fenix.bilingualreader.service.listener.BookSearchListener

class BookSearchViewHolder(itemView: View, private val listener: BookSearchListener) : RecyclerView.ViewHolder(itemView) {

    fun bind(search: BookSearch) {
        val root = itemView.findViewById<LinearLayout>(R.id.book_search_root)
        val text = itemView.findViewById<TextView>(R.id.book_search_text)
        val page = itemView.findViewById<TextView>(R.id.book_search_page)

        root.setOnClickListener { listener.onClick(search) }

        text.text = Html.fromHtml(search.search)
        page.text = itemView.context.getString(R.string.book_search_page, search.page)
    }

}