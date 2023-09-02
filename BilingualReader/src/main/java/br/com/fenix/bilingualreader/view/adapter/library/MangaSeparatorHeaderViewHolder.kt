package br.com.fenix.bilingualreader.view.adapter.library

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Separator


class MangaSeparatorHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(separator: Separator) {
        itemView.findViewById<TextView>(R.id.separator_title).text = separator.title

        val items = itemView.findViewById<TextView>(R.id.separator_items)
        items.visibility = if (separator.items > 0) {
            items.text = separator.items.toString()
            View.VISIBLE
        } else
            View.GONE
    }

}