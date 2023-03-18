package br.com.fenix.bilingualreader.view.adapter.book

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.BookSearch
import br.com.fenix.bilingualreader.service.listener.BookSearchHistoryListener
import com.google.android.material.button.MaterialButton


class BookSearchHistoryAdapter(
    context: Context,
    layout: Int,
    dataSet: ArrayList<BookSearch>,
    private val listener: BookSearchHistoryListener
) :
    ArrayAdapter<BookSearch>(context, layout, dataSet) {
    var mContext: Context

    // View lookup cache
    private class ViewHolder {
        var root: LinearLayout? = null
        var historyTitle: TextView? = null
        var historyDelete: MaterialButton? = null
    }

    private var lastPosition = -1

    init {
        mContext = context
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        // Get the data item for this position
        var convertView = view
        val dataModel: BookSearch? = getItem(position)

        val viewHolder: ViewHolder
        if (convertView == null) {
            viewHolder = ViewHolder()
            val inflater = LayoutInflater.from(context)
            convertView = inflater.inflate(R.layout.line_card_book_search_history, parent, false)
            viewHolder.root =
                convertView.findViewById<View>(R.id.book_search_history) as LinearLayout
            viewHolder.historyTitle =
                convertView.findViewById<View>(R.id.book_search_history_title) as TextView
            viewHolder.historyDelete =
                convertView.findViewById<View>(R.id.book_search_history_delete) as MaterialButton
            convertView.tag = viewHolder
        } else
            viewHolder = convertView.tag as ViewHolder

        lastPosition = position
        viewHolder.historyTitle!!.text = dataModel!!.search
        viewHolder.root!!.setOnClickListener { listener.onClick(dataModel) }
        viewHolder.historyDelete!!.setOnClickListener {
            listener.onDelete(
                dataModel,
                convertView!!,
                position
            )
        }
        return convertView!!
    }
}