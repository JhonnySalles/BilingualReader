package br.com.fenix.bilingualreader.view.adapter.popup

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Tags
import br.com.fenix.bilingualreader.service.listener.TagsListener
import com.google.android.material.button.MaterialButton

class TagsAdapter(context: Context, layout: Int, dataSet: List<Tags>, private val listener: TagsListener) : ArrayAdapter<Tags>(context, layout, dataSet) {

    // View lookup cache
    private class ViewHolder {
        var root: LinearLayout? = null
        var tag: CheckBox? = null
        var delete: MaterialButton? = null
    }

    private var lastPosition = -1

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        // Get the data item for this position
        var convertView = view
        val item: Tags? = getItem(position)

        val viewHolder: ViewHolder
        if (convertView == null) {
            viewHolder = ViewHolder()
            val inflater = LayoutInflater.from(context)
            convertView = inflater.inflate(R.layout.list_line_tag, parent, false)
            viewHolder.root = convertView.findViewById(R.id.list_line_tag_container)
            viewHolder.tag = convertView.findViewById(R.id.list_line_tag_checkbox)
            viewHolder.delete = convertView.findViewById(R.id.list_line_tag_delete)
            convertView.tag = viewHolder
        } else
            viewHolder = convertView.tag as ViewHolder

        lastPosition = position
        viewHolder.tag!!.text = item!!.name
        viewHolder.tag!!.setOnCheckedChangeListener { _, isChecked ->
            item.isSelected = isChecked
            listener.onCheckedChange(item)
        }
        viewHolder.tag!!.isChecked = item.isSelected
        viewHolder.delete!!.setOnClickListener {
            listener.onDelete(item, convertView!!, position)
        }
        return convertView!!
    }
}