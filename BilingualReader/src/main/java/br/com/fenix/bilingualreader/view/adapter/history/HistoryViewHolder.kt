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
import br.com.fenix.bilingualreader.service.controller.ImageCoverController
import br.com.fenix.bilingualreader.service.listener.MangaCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts

class HistoryViewHolder(itemView: View, private val listener: MangaCardListener) :
    RecyclerView.ViewHolder(itemView) {

    companion object {
        lateinit var mDefaultImageCover: Bitmap
    }

    init {
        mDefaultImageCover = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_2)
    }

    fun bind(manga: Manga) {
        val mangaImage = itemView.findViewById<ImageView>(R.id.history_image_cover)
        val mangaTitle = itemView.findViewById<TextView>(R.id.history_text_title)
        val mangaSubTitle = itemView.findViewById<TextView>(R.id.history_sub_title)
        val mangaLibrary = itemView.findViewById<TextView>(R.id.history_library)
        val mangaFavorite = itemView.findViewById<ImageView>(R.id.history_favorite)
        val mangaSubtitle = itemView.findViewById<ImageView>(R.id.history_has_subtitle)
        val cardView = itemView.findViewById<LinearLayout>(R.id.history_card)

        cardView.setOnClickListener { listener.onClick(manga) }
        cardView.setOnLongClickListener {
            listener.onClickLong(manga, it, layoutPosition)
            true
        }

        mangaImage.setImageBitmap(mDefaultImageCover)
        ImageCoverController.instance.setImageCoverAsync(itemView.context, manga, mangaImage)

        mangaTitle.text = manga.title

        if (manga.subTitle.isEmpty()) {
            val title = if (manga.lastAccess != null)
                "${manga.bookMark} / ${manga.pages}  -  ${itemView.resources.getString(R.string.manga_library_last_access)}: ${
                    GeneralConsts.formatterDateTime(
                        itemView.context,
                        manga.lastAccess!!
                    )
                }"
            else
                "${manga.bookMark} / ${manga.pages}"

            mangaSubTitle.text = title
        } else
            mangaSubTitle.text = manga.subTitle

        mangaLibrary.text = if (manga.fkLibrary == GeneralConsts.KEYS.LIBRARY.DEFAULT)
            itemView.context.getString(R.string.manga_library_default).uppercase()
        else
            manga.library.title.uppercase()
        mangaFavorite.visibility = if (manga.favorite) View.VISIBLE else View.GONE
        mangaSubtitle.visibility = if (manga.hasSubtitle) View.VISIBLE else View.GONE

        if (manga.excluded)
            cardView.setBackgroundResource(R.drawable.history_custom_ripple_item_deleted)
        else
            cardView.setBackgroundResource(R.drawable.history_custom_ripple)

    }

}