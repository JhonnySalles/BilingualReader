package br.com.fenix.bilingualreader.view.adapter.book

import android.graphics.drawable.AnimatedVectorDrawable
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.service.listener.BookAnnotationListener
import com.google.android.material.button.MaterialButton

class BookAnnotationViewHolder(itemView: View, private val listener: BookAnnotationListener) :
    RecyclerView.ViewHolder(itemView) {

    fun bind(mark: BookAnnotation, position: Int) {
        val root = itemView.findViewById<LinearLayout>(R.id.book_annotation_root)

        val favorite = itemView.findViewById<MaterialButton>(R.id.book_annotation_favorite)
        val options = itemView.findViewById<MaterialButton>(R.id.book_annotation_options)

        val title = itemView.findViewById<TextView>(R.id.book_annotation_title)
        val text = itemView.findViewById<TextView>(R.id.book_annotation_text)
        val color = itemView.findViewById<View>(R.id.book_annotation_color)

        val noteContent = itemView.findViewById<LinearLayout>(R.id.book_annotation_note_content)
        val addNote = itemView.findViewById<MaterialButton>(R.id.book_annotation_without_note)
        val note = itemView.findViewById<TextView>(R.id.book_annotation_note)

        root.setOnClickListener { listener.onClick(mark) }
        addNote.setOnClickListener { listener.onClickNote(mark) }
        note.setOnClickListener { listener.onClickNote(mark) }
        options.setOnClickListener {
            (options.icon as AnimatedVectorDrawable).start()
            listener.onClickOptions(mark, options.rootView, position)
        }

        favorite.setOnClickListener {
            mark.favorite = !mark.favorite
            favorite.setIconResource(if (mark.favorite) R.drawable.ico_animated_favorited_marked else R.drawable.ico_animated_favorited_unmarked)
            (favorite.icon as AnimatedVectorDrawable).start()
            listener.onClickFavorite(mark)
        }

        favorite.setIconResource(if (mark.favorite) R.drawable.ico_favorite_mark else R.drawable.ico_favorite_unmark)


        text.text = mark.text

        if (mark.type == MarkType.BookMark) {
            title.text = itemView.context.getString(R.string.book_annotation_list_title_mark, mark.page)

            note.text = ""

            color.visibility = View.GONE
            noteContent.visibility = View.GONE
        } else {
            title.text = itemView.context.getString(R.string.book_annotation_list_title_detach, mark.page)

            color.visibility = View.VISIBLE
            color.setBackgroundColor(mark.color.getColor())

            noteContent.visibility = View.VISIBLE
            note.text = mark.annotation
            addNote.visibility = if (mark.annotation.isEmpty()) View.VISIBLE else View.GONE
        }
    }

}