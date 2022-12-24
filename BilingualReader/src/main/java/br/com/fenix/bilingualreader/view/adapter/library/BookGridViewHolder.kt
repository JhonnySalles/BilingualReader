package br.com.fenix.bilingualreader.view.adapter.library

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.service.listener.BookCardListener
import com.google.android.material.card.MaterialCardView


class BookGridViewHolder(itemView: View, private val listener: BookCardListener) :
    RecyclerView.ViewHolder(itemView) {

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

    fun bind(book: Book) {
        val mangaImage = itemView.findViewById<ImageView>(R.id.manga_grid_image_cover)
        val mangaTitle = itemView.findViewById<TextView>(R.id.manga_grid_text_title)
        val mangaSubTitle = itemView.findViewById<TextView>(R.id.manga_grid_sub_title)
        val mangaProgress = itemView.findViewById<ProgressBar>(R.id.manga_grid_progress)
        val cardView = itemView.findViewById<MaterialCardView>(R.id.manga_grid_card)
        val favorite = itemView.findViewById<ImageView>(R.id.manga_grid_favorite)
        val subtitle = itemView.findViewById<ImageView>(R.id.manga_grid_has_subtitle)

    }

}