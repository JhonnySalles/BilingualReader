package br.com.fenix.bilingualreader.view.adapter.history

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController
import br.com.fenix.bilingualreader.service.listener.MangaCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.Util

class HistoryViewHolder(itemView: View, private val listener: MangaCardListener) :
    RecyclerView.ViewHolder(itemView) {

    companion object {
        lateinit var mDefaultImageCover: Bitmap
    }

    init {
        mDefaultImageCover = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_2)
    }

    fun bind(manga: Manga) {
        val mangaStatus = itemView.findViewById<View>(R.id.history_status)
        val mangaImage = itemView.findViewById<ImageView>(R.id.history_image_cover)
        val mangaTitle = itemView.findViewById<TextView>(R.id.history_text_title)
        val mangaLastAccess = itemView.findViewById<TextView>(R.id.history_line_last_access)
        val mangaFileType = itemView.findViewById<TextView>(R.id.history_line_file_type)
        val mangaFileSize = itemView.findViewById<TextView>(R.id.history_line_file_size)
        val mangaPagesRead = itemView.findViewById<TextView>(R.id.history_line_pages)
        val mangaLibrary = itemView.findViewById<TextView>(R.id.history_library)
        val mangaFavorite = itemView.findViewById<ImageView>(R.id.history_favorite)
        val mangaSubtitle = itemView.findViewById<ImageView>(R.id.history_has_subtitle)
        val cardView = itemView.findViewById<LinearLayout>(R.id.history_card)

        cardView.setOnClickListener { listener.onClick(manga) }
        cardView.setOnLongClickListener {
            listener.onClickLong(manga, it, layoutPosition)
            true
        }

        mangaImage.setImageBitmap(null)
        MangaImageCoverController.instance.setImageCoverAsync(itemView.context, manga, mangaImage, mDefaultImageCover)

        mangaTitle.text = manga.title

        mangaLastAccess.text = if (manga.lastAccess != null) GeneralConsts.formatterDate(itemView.context, manga.lastAccess!!) else ""
        mangaFileType.text = manga.type.acronym
        mangaFileSize.text = FileUtil.formatSize(manga.fileSize)
        val percent: Float = if (manga.bookMark > 0) ((manga.bookMark.toFloat() / manga.pages) * 100) else 0f
        mangaPagesRead.text = "${manga.bookMark} / ${manga.pages}" + if (percent > 0) (" (" + Util.formatDecimal(percent) + ")") else ""

        mangaLibrary.text = if (manga.fkLibrary == GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA)
            itemView.context.getString(R.string.manga_library_default).uppercase()
        else
            manga.library.title.uppercase()
        mangaFavorite.visibility = if (manga.favorite) View.VISIBLE else View.GONE
        mangaSubtitle.visibility = if (manga.hasSubtitle) View.VISIBLE else View.GONE

        if (manga.excluded) {
            cardView.setBackgroundResource(R.drawable.history_custom_ripple_item_deleted)
            mangaStatus.setBackgroundResource(R.drawable.history_item_deleted_background)
        } else {
            cardView.setBackgroundResource(R.drawable.history_custom_ripple)
            mangaStatus.setBackgroundResource(R.drawable.history_item_background)
        }

    }

}