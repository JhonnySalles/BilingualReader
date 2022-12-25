package br.com.fenix.bilingualreader.view.adapter.vocabulary

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.service.listener.VocabularyCardListener
import br.com.fenix.bilingualreader.util.helpers.Util
import com.google.android.material.button.MaterialButton

class VocabularyBookViewHolder(itemView: View, private val listener: VocabularyCardListener) :
    RecyclerView.ViewHolder(itemView) {

    companion object {
        private fun createLayout(context: Context): GridLayoutManager {
            val layout = GridLayoutManager(context, 1)
            layout.orientation = RecyclerView.HORIZONTAL
            return layout
        }

        private fun createAdapter(): VocabularyBookListCardAdapter =
            VocabularyBookListCardAdapter()
    }

    val layout: GridLayoutManager = createLayout(itemView.context)
    val adapter: VocabularyBookListCardAdapter = createAdapter()

    fun bind(vocabulary: Vocabulary) {
        val content = itemView.findViewById<LinearLayout>(R.id.vocabulary_book_content)
        val title = itemView.findViewById<TextView>(R.id.vocabulary_book_title)
        val reading = itemView.findViewById<TextView>(R.id.vocabulary_book_reading)
        val meaning = itemView.findViewById<TextView>(R.id.vocabulary_book_meaning)
        val appear = itemView.findViewById<TextView>(R.id.vocabulary_book_appear)
        val mangaList = itemView.findViewById<RecyclerView>(R.id.vocabulary_book_list)
        val favorite = itemView.findViewById<MaterialButton>(R.id.vocabulary_book_favorite)

        content.setOnLongClickListener {
            val clipboard =
                itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip =
                ClipData.newPlainText("Copied Text", vocabulary.word + " " + vocabulary.portuguese)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(
                itemView.context,
                itemView.context.getString(
                    R.string.action_copy,
                    vocabulary.word + " " + vocabulary.portuguese
                ),
                Toast.LENGTH_LONG
            ).show()

            true
        }

        title.text = Util.setVerticalText(vocabulary.word)
        reading.text = vocabulary.reading + (if (!vocabulary.revised) '¹' else "")
        meaning.text = vocabulary.portuguese
        appear.text = itemView.context.getString(R.string.vocabulary_appear, vocabulary.appears)

        favorite.setIconResource(if (vocabulary.favorite) R.drawable.ic_favorite_mark else R.drawable.ic_favorite_unmark)

        favorite.setOnClickListener {
            vocabulary.favorite = !vocabulary.favorite
            favorite.setIconResource(if (vocabulary.favorite) R.drawable.ic_favorite_mark else R.drawable.ic_favorite_unmark)
            listener.onClickFavorite(vocabulary)
        }

        mangaList.adapter = adapter
        mangaList.layoutManager = layout
        adapter.updateList(vocabulary.vocabularyBooks)
    }

}