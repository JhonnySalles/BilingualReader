package br.com.fenix.bilingualreader.service.japanese

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Kanjax

class VocabularyKanjiAdapter(context: Context, layout: Int, dataSet: List<Kanjax>) : ArrayAdapter<Kanjax>(context, layout, dataSet) {
    var mContext: Context

    // View lookup cache
    private class ViewHolder {
        var root: LinearLayout? = null
        var title: TextView? = null
        var portuguese: TextView? = null
        var english: TextView? = null
    }

    private var lastPosition = -1

    init {
        mContext = context
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        // Get the data item for this position
        var convertView = view
        val item: Kanjax? = getItem(position)

        val viewHolder: ViewHolder
        if (convertView == null) {
            viewHolder = ViewHolder()
            val inflater = LayoutInflater.from(context)
            convertView = inflater.inflate(R.layout.line_card_kanji, parent, false)
            viewHolder.root = convertView.findViewById(R.id.kanji_line_container)
            viewHolder.title = convertView.findViewById(R.id.kanji_line_title)
            viewHolder.portuguese = convertView.findViewById(R.id.kanji_line_portuguese)
            viewHolder.english = convertView.findViewById(R.id.kanji_line_english)
            convertView.tag = viewHolder
        } else
            viewHolder = convertView.tag as ViewHolder

        lastPosition = position
        viewHolder.title!!.text = item!!.kanji
        viewHolder.portuguese!!.text = item.keyword
        viewHolder.english!!.text = item.keywordPt
        return convertView!!
    }
}