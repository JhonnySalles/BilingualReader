package br.com.fenix.bilingualreader.view.adapter.library

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.LibraryMangaType
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController
import br.com.fenix.bilingualreader.service.listener.MangaCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.view.components.TextViewWithBorder
import com.google.android.material.card.MaterialCardView


class MangaGridViewHolder(var type: LibraryMangaType, itemView: View, private val listener: MangaCardListener) : RecyclerView.ViewHolder(itemView) {

    companion object {
        var mIsLandscape: Boolean = false
        var mMangaCardWidth: Int = 0
        var mMangaCardHeight: Int = 0
        var mMangaCardWidthMedium: Int = 0
        var mMangaCardWidthLandscapeMedium: Int = 0
        var mMangaCardWidthSmall: Int = 0
        var mMangaCardHeightSmall: Int = 0
        var mMangaImage: Int = 0
        var mMangaImageSmall: Int = 0
        lateinit var mDefaultImageCover1: Bitmap
        lateinit var mDefaultImageCover2: Bitmap
        lateinit var mDefaultImageCover3: Bitmap
        lateinit var mDefaultImageCover4: Bitmap
        lateinit var mDefaultImageCover5: Bitmap
    }

    init {
        mIsLandscape = itemView.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        mMangaCardWidth = itemView.resources.getDimension(R.dimen.manga_grid_card_layout_width).toInt()
        mMangaCardHeight = itemView.resources.getDimension(R.dimen.manga_grid_card_layout_height).toInt()
        mMangaCardWidthMedium = itemView.resources.getDimension(R.dimen.manga_grid_card_layout_width_medium).toInt()
        mMangaCardWidthLandscapeMedium = itemView.resources.getDimension(R.dimen.manga_grid_card_layout_width_landscape_medium).toInt()
        mMangaCardWidthSmall = itemView.resources.getDimension(R.dimen.manga_grid_card_layout_width_small).toInt()
        mMangaCardHeightSmall = itemView.resources.getDimension(R.dimen.manga_grid_card_layout_height_small).toInt()
        mMangaImageSmall = itemView.resources.getDimension(R.dimen.manga_grid_card_image_small).toInt()
        mMangaImage = itemView.resources.getDimension(R.dimen.manga_grid_card_image).toInt()

        mDefaultImageCover1 = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_1)
        mDefaultImageCover2 = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_2)
        mDefaultImageCover3 = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_3)
        mDefaultImageCover4 = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_4)
        mDefaultImageCover5 = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_5)
    }

    @SuppressLint("SetTextI18n")
    fun bind(manga: Manga) {
        val mangaImage = itemView.findViewById<ImageView>(R.id.manga_grid_image_cover)
        val mangaTitle = itemView.findViewById<TextViewWithBorder>(R.id.manga_grid_text_title)
        val mangaFileType = itemView.findViewById<TextViewWithBorder>(R.id.manga_grid_file_type)
        val mangaPagesRead = itemView.findViewById<TextViewWithBorder>(R.id.manga_grid_pages)
        val mangaLastAccess = itemView.findViewById<TextViewWithBorder>(R.id.manga_grid_last_access)
        val mangaProgress = itemView.findViewById<ProgressBar>(R.id.manga_grid_progress)
        val cardView = itemView.findViewById<MaterialCardView>(R.id.manga_grid_card)
        val favorite = itemView.findViewById<ImageView>(R.id.manga_grid_favorite)
        val subtitle = itemView.findViewById<ImageView>(R.id.manga_grid_has_subtitle)

        if (manga.favorite)
            favorite.visibility = View.VISIBLE
        else
            favorite.visibility = View.GONE

        subtitle.visibility  = if (manga.hasSubtitle) {
            if (manga.lastVocabImport != null)
                subtitle.setImageResource(R.drawable.ic_subtitles_imported)
            else
                subtitle.setImageResource(R.drawable.ic_subtitles_exist)
            View.VISIBLE
        } else View.GONE

        when (type) {
            LibraryMangaType.GRID_MEDIUM -> cardView.layoutParams.width = if (mIsLandscape) mMangaCardWidthLandscapeMedium else mMangaCardWidthMedium
            LibraryMangaType.GRID_SMALL ->
                if (mIsLandscape) {
                    cardView.layoutParams.width = mMangaCardWidthSmall
                    cardView.layoutParams.height = mMangaCardHeightSmall
                    mangaImage.layoutParams.height = mMangaImageSmall
                }
            else -> {
                cardView.layoutParams.width = mMangaCardWidth
                cardView.layoutParams.height = mMangaCardHeight
                mangaImage.layoutParams.height = mMangaImage
            }
        }

        cardView.setOnClickListener { listener.onClick(manga) }
        cardView.setOnLongClickListener {
            listener.onClickLong(manga, it, layoutPosition)
            true
        }

        val image = when ((1..5).random()) {
            1 -> mDefaultImageCover1
            2 -> mDefaultImageCover2
            3 -> mDefaultImageCover3
            4 -> mDefaultImageCover4
            else -> mDefaultImageCover5
        }

        mangaImage.setImageBitmap(null)
        MangaImageCoverController.instance.setImageCoverAsync(itemView.context, manga, mangaImage, image)

        val isSmall = manga.lastAccess != null && manga.bookMark > 0 && type != LibraryMangaType.GRID_BIG

        mangaTitle.text = manga.title
        mangaFileType.text = Util.getExtensionFromPath(manga.path).uppercase()
        mangaLastAccess.text = if (manga.lastAccess != null) GeneralConsts.formatterDate(itemView.context, manga.lastAccess!!, isSmall) else ""
        mangaPagesRead.text = if (isSmall) {
            val percent: Float = (manga.bookMark.toFloat() / manga.pages) * 100
            "${Util.formatDecimal(percent)} %"
        } else
             "${manga.bookMark} / ${manga.pages}"

        mangaProgress.max = manga.pages
        mangaProgress.setProgress(manga.bookMark, false)
    }

}