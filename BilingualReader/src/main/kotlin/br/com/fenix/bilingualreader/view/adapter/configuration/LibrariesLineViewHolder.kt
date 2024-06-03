package br.com.fenix.bilingualreader.view.adapter.configuration

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.service.listener.LibrariesCardListener
import com.google.android.material.switchmaterial.SwitchMaterial

class LibrariesLineViewHolder(itemView: View, private val listener: LibrariesCardListener) :
    RecyclerView.ViewHolder(itemView) {

    companion object {
        lateinit var mMapLanguage: HashMap<Libraries, String>
    }

    init {
        val languages = itemView.resources.getStringArray(R.array.languages)
        mMapLanguage = hashMapOf(
            Libraries.ENGLISH to languages[1],
            Libraries.JAPANESE to languages[2],
            Libraries.PORTUGUESE to languages[0],
            Libraries.DEFAULT to languages[3]
        )
    }

    fun bind(library: Library) {
        val container = itemView.findViewById<LinearLayout>(R.id.library_line_container)
        val title = itemView.findViewById<TextView>(R.id.library_line_title)
        val language = itemView.findViewById<TextView>(R.id.library_line_language)
        val path = itemView.findViewById<TextView>(R.id.library_line_path)
        val switch = itemView.findViewById<SwitchMaterial>(R.id.library_line_switch_enabled)

        title.text = library.title
        path.text = library.path
        language.text = mMapLanguage[library.language]

        container.setOnClickListener { listener.onClick(library) }
        container.setOnLongClickListener {
            listener.onClickLong(library, it, layoutPosition)
            true
        }

        switch.isChecked = library.enabled
        switch.setOnCheckedChangeListener { _, isChecked ->
            library.enabled = isChecked
            listener.changeEnable(library)
        }
    }

}