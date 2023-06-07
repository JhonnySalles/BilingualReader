package br.com.fenix.bilingualreader.view.adapter.vocabulary

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.listener.VocabularyCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.view.ui.vocabulary.VocabularyActivity
import com.google.android.material.button.MaterialButton


class VocabularyViewHolder(itemView: View, private val listener: VocabularyCardListener) : RecyclerView.ViewHolder(itemView) {

    companion object {
        private fun openVocabulary(context: Context, vocabulary: String, type: Type) {
            val intent = Intent(context, VocabularyActivity::class.java)
            val bundle = Bundle()
            bundle.putString(GeneralConsts.KEYS.VOCABULARY.TEXT, vocabulary)
            bundle.putSerializable(GeneralConsts.KEYS.VOCABULARY.TYPE, type)
            intent.putExtras(bundle)
            context.startActivity(intent)
        }
    }

    fun bind(vocabulary: Vocabulary, postition: Int) {
        val content = itemView.findViewById<LinearLayout>(R.id.vocabulary_content)
        val title = itemView.findViewById<TextView>(R.id.vocabulary_title)
        val reading = itemView.findViewById<TextView>(R.id.vocabulary_reading)
        val basic = itemView.findViewById<TextView>(R.id.vocabulary_basic_form)
        val meaningPt = itemView.findViewById<TextView>(R.id.vocabulary_meaning_pt)
        val meaningEn = itemView.findViewById<TextView>(R.id.vocabulary_meaning_en)
        val appear = itemView.findViewById<TextView>(R.id.vocabulary_appear)
        val favorite = itemView.findViewById<MaterialButton>(R.id.vocabulary_favorite)

        val manga = itemView.findViewById<MaterialButton>(R.id.vocabulary_manga_button)
        val book = itemView.findViewById<MaterialButton>(R.id.vocabulary_book_button)
        val tatoeba = itemView.findViewById<MaterialButton>(R.id.vocabulary_tatoeba_button)

        content.setOnClickListener { listener.onClick(vocabulary) }

        content.setOnLongClickListener {
            listener.onClickLong(vocabulary, itemView, postition)
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
        basic.text = vocabulary.basicForm
        meaningPt.text = vocabulary.portuguese
        meaningEn.text = vocabulary.english
        appear.text = itemView.context.getString(R.string.vocabulary_appear, vocabulary.appears)

        favorite.setIconResource(if (vocabulary.favorite) R.drawable.ico_favorite_mark else R.drawable.ico_favorite_unmark)

        favorite.setOnClickListener {
            vocabulary.favorite = !vocabulary.favorite
            favorite.setIconResource(if (vocabulary.favorite) R.drawable.ico_animated_favorited_marked else R.drawable.ico_animated_favorited_unmarked)
            (favorite.icon as AnimatedVectorDrawable).start()
            listener.onClickFavorite(vocabulary)
        }

        manga.setOnClickListener {
            openVocabulary(itemView.context, vocabulary.word, Type.MANGA)
        }

        book.setOnClickListener {
            openVocabulary(itemView.context, vocabulary.word, Type.BOOK)
        }

        tatoeba.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(GeneralConsts.LINKS.TATOEBA + vocabulary.word)
            )
            itemView.context.startActivity(intent)
        }

    }

}