package br.com.fenix.bilingualreader.view.adapter.history

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController
import br.com.fenix.bilingualreader.service.listener.HistoryCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.Util


class HistoryViewHolder(itemView: View, private val listener: HistoryCardListener) : RecyclerView.ViewHolder(itemView) {

    companion object {
        lateinit var mDefaultImageCover: Bitmap
    }

    init {
        mDefaultImageCover = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_2)
    }

    fun bind(history: History) {
        val status = itemView.findViewById<View>(R.id.history_status)
        val image = itemView.findViewById<ImageView>(R.id.history_image_cover)
        val title = itemView.findViewById<TextView>(R.id.history_text_title)
        val lastAccess = itemView.findViewById<TextView>(R.id.history_line_last_access)
        val fileType = itemView.findViewById<TextView>(R.id.history_line_file_type)
        val fileSize = itemView.findViewById<TextView>(R.id.history_line_file_size)
        val pagesRead = itemView.findViewById<TextView>(R.id.history_line_pages)
        val library = itemView.findViewById<TextView>(R.id.history_library)
        val type = itemView.findViewById<TextView>(R.id.history_type)
        val favorite = itemView.findViewById<ImageView>(R.id.history_favorite)
        val subtitle = itemView.findViewById<ImageView>(R.id.history_has_subtitle)
        val cardView = itemView.findViewById<LinearLayout>(R.id.history_card)

        cardView.setOnClickListener { listener.onClick(history) }
        cardView.setOnLongClickListener {
            listener.onClickLong(history, it, layoutPosition)
            true
        }

        image.setImageBitmap(null)
        when (history) {
            is Manga -> MangaImageCoverController.instance.setImageCoverAsync(itemView.context, history, image, mDefaultImageCover)
            is Book -> BookImageCoverController.instance.setImageCoverAsync(itemView.context, history, image, mDefaultImageCover)
        }

        title.text = history.title

        lastAccess.text = if (history.lastAccess != null) GeneralConsts.formatterDate(itemView.context, history.lastAccess!!) else ""
        fileType.text = history.fileType.acronym
        fileSize.text = FileUtil.formatSize(history.fileSize)
        val percent: Float = if (history.bookMark > 0) ((history.bookMark.toFloat() / history.pages) * 100) else 0f
        pagesRead.text = "${history.bookMark} / ${history.pages}" + if (percent > 0) (" (" + Util.formatDecimal(percent) + ")") else ""

        library.text = if (history.fkLibrary == GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA || history.fkLibrary == GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK)
            itemView.context.getString(R.string.manga_library_default).uppercase()
        else
            history.library.title.uppercase()

        type.text = when(history.type) {
            Type.BOOK -> itemView.context.getString(R.string.history_book)
            Type.MANGA -> itemView.context.getString(R.string.history_manga)
            else -> ""
        }

        favorite.visibility = if (history.favorite) View.VISIBLE else View.GONE
        subtitle.visibility  = if (history is Manga && history.hasSubtitle) {
            if (history.lastVocabImport != null)
                subtitle.setImageResource(R.drawable.ico_subtitles_imported)
            else
                subtitle.setImageResource(R.drawable.ico_subtitles_exist)
            View.VISIBLE
        } else View.GONE

        if (history.excluded) {
            cardView.setBackgroundResource(R.drawable.custom_ripple_history_item_deleted)
            status.setBackgroundResource(R.drawable.history_item_deleted_background)
        } else {
            cardView.setBackgroundResource(R.drawable.custom_ripple_history)
            status.setBackgroundResource(R.color.transparent)
        }

    }

}