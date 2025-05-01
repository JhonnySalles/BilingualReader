package br.com.fenix.bilingualreader.view.adapter.library

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController
import br.com.fenix.bilingualreader.service.listener.MangaCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.Util

class MangaLineViewHolder(itemView: View, private val listener: MangaCardListener) : RecyclerView.ViewHolder(itemView) {

    companion object {
        lateinit var mDefaultImageCover : Bitmap
        lateinit var mIconSubtitleExists : Bitmap
        lateinit var mIconSubtitleImported : Bitmap

        var mDescriptionAuthor: String = ""
        var mDescriptionSeries: String = ""
        var mDescriptionPublisher: String = ""
    }

    init {
        mDefaultImageCover = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_2)

        mDescriptionSeries = itemView.context.getString(R.string.manga_library_line_series) + " "
        mDescriptionPublisher = itemView.context.getString(R.string.manga_library_line_publisher) + " "
        mDescriptionAuthor = itemView.context.getString(R.string.manga_library_line_authors) + " "
    }

    @SuppressLint("SetTextI18n")
    fun bind(manga: Manga) {
        val mangaImage = itemView.findViewById<ImageView>(R.id.manga_line_image_cover)
        val mangaTitle = itemView.findViewById<TextView>(R.id.manga_line_text_title)
        val mangaAuthor = itemView.findViewById<TextView>(R.id.manga_line_author)
        val mangaSeries = itemView.findViewById<TextView>(R.id.manga_line_series)
        val mangaPublisher = itemView.findViewById<TextView>(R.id.manga_line_publisher)
        val mangaLastAccess = itemView.findViewById<TextView>(R.id.manga_line_last_access)
        val mangaFileType = itemView.findViewById<TextView>(R.id.manga_line_file_type)
        val mangaFileSize = itemView.findViewById<TextView>(R.id.manga_line_file_size)
        val mangaPagesRead = itemView.findViewById<TextView>(R.id.manga_line_pages)
        val mangaProgress = itemView.findViewById<ProgressBar>(R.id.manga_line_progress)
        val cardView = itemView.findViewById<LinearLayout>(R.id.manga_line_card)
        val favorite = itemView.findViewById<LinearLayout>(R.id.manga_line_favorite)
        val favoriteIcon = itemView.findViewById<ImageView>(R.id.manga_line_favorite_icon)
        val config = itemView.findViewById<LinearLayout>(R.id.manga_line_config)
        val configIcon = itemView.findViewById<ImageView>(R.id.manga_line_config_icon)
        val subtitle = itemView.findViewById<ImageView>(R.id.manga_line_has_subtitle)

        subtitle.visibility  = if (manga.hasSubtitle) {
            if (manga.lastVocabImport != null)
                subtitle.setImageResource(R.drawable.ico_subtitles_imported)
            else
                subtitle.setImageResource(R.drawable.ico_subtitles_exist)
            View.VISIBLE
        } else
            View.GONE

        favorite.setOnClickListener {
            manga.favorite = !manga.favorite
            favoriteIcon.setImageResource(if (manga.favorite) R.drawable.ico_favorite_mark else R.drawable.ico_favorite_unmark)
            listener.onClickFavorite(manga)
        }

        favoriteIcon.setImageResource(if (manga.favorite) R.drawable.ico_favorite_mark else R.drawable.ico_favorite_unmark)
        config.setOnClickListener { listener.onClickConfig(manga, cardView, itemView, layoutPosition) }

        cardView.setOnClickListener { listener.onClick(manga, itemView) }
        cardView.setOnLongClickListener {
            listener.onClickLong(manga, itemView, layoutPosition)
            true
        }

        mangaImage.setImageBitmap(null)
        MangaImageCoverController.instance.setImageCoverAsync(itemView.context, manga, mangaImage, mDefaultImageCover)

        mangaTitle.text = manga.title
        mangaLastAccess.text = if (manga.lastAccess != null) GeneralConsts.formatterDate(itemView.context, manga.lastAccess!!) else ""
        mangaFileType.text = Util.getExtensionFromPath(manga.path).uppercase()
        mangaFileSize.text = FileUtil.formatSize(manga.fileSize)
        val percent: Float = if (manga.bookMark > 0) ((manga.bookMark.toFloat() / manga.pages) * 100) else 0f
        mangaPagesRead.text = "${manga.bookMark} / ${manga.pages}" + if (percent > 0) (" (" + Util.formatDecimal(percent) + ")") else ""

        mangaAuthor.text = ""
        mangaAuthor.visibility = if (manga.author.isNotEmpty())  {
            mangaAuthor.text = mDescriptionAuthor + manga.author
            View.VISIBLE
        } else
            View.GONE

        mangaSeries.text = ""
        mangaSeries.visibility = if (manga.series.isNotEmpty())  {
            mangaSeries.text = mDescriptionSeries + manga.series
            View.VISIBLE
        } else
            View.GONE

        mangaPublisher.text = ""
        mangaPublisher.visibility = if (manga.publisher.isNotEmpty())  {
            mangaPublisher.text = mDescriptionPublisher + manga.publisher
            View.VISIBLE
        } else
            View.GONE

        mangaProgress.max = manga.pages
        mangaProgress.setProgress(manga.bookMark, false)
    }

}