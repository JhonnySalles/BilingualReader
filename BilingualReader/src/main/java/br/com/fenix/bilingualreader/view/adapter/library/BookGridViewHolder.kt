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
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import br.com.fenix.bilingualreader.service.listener.BookCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Util
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
        mIsLandscape =
            itemView.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        mMangaCardWidth =
            itemView.resources.getDimension(R.dimen.manga_grid_card_layout_width).toInt()
        mMangaCardHeight =
            itemView.resources.getDimension(R.dimen.manga_grid_card_layout_height).toInt()
        mMangaCardWidthMedium =
            itemView.resources.getDimension(R.dimen.manga_grid_card_layout_width_medium).toInt()
        mMangaCardWidthLandscapeMedium =
            itemView.resources.getDimension(R.dimen.manga_grid_card_layout_width_landscape_medium)
                .toInt()
        mMangaCardWidthSmall =
            itemView.resources.getDimension(R.dimen.manga_grid_card_layout_width_small).toInt()
        mMangaCardHeightSmall =
            itemView.resources.getDimension(R.dimen.manga_grid_card_layout_height_small).toInt()
        mMangaImageSmall =
            itemView.resources.getDimension(R.dimen.manga_grid_card_image_small).toInt()
        mMangaImage = itemView.resources.getDimension(R.dimen.manga_grid_card_image).toInt()

        mDefaultImageCover1 =
            BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_1)
        mDefaultImageCover2 =
            BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_2)
        mDefaultImageCover3 =
            BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_3)
        mDefaultImageCover4 =
            BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_4)
        mDefaultImageCover5 =
            BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_5)
    }

    fun bind(book: Book) {
        val bookImage = itemView.findViewById<ImageView>(R.id.book_grid_image_cover)
        val bookTitle = itemView.findViewById<TextView>(R.id.book_grid_title)
        val bookSubTitle = itemView.findViewById<TextView>(R.id.book_grid_sub_title)
        val bookType = itemView.findViewById<TextView>(R.id.book_grid_file_type)
        val bookPagesRead = itemView.findViewById<TextView>(R.id.book_grid_pages)
        val bookLastAccess = itemView.findViewById<TextView>(R.id.book_grid_last_access)

        val cardView = itemView.findViewById<MaterialCardView>(R.id.book_grid_card)
        val bookProgress = itemView.findViewById<ProgressBar>(R.id.book_grid_progress)
        val favorite = itemView.findViewById<ImageView>(R.id.book_grid_favorite)
        val config = itemView.findViewById<ImageView>(R.id.book_grid_config)

        cardView.setOnClickListener { listener.onClick(book) }
        cardView.setOnLongClickListener {
            listener.onClickLong(book, it, layoutPosition)
            true
        }

        favorite.setOnClickListener {
            book.favorite = !book.favorite
            favorite.setImageResource(if (book.favorite) R.drawable.ico_favorite_mark else R.drawable.ico_favorite_unmark)
            listener.onClickFavorite(book)
        }
        config.setOnClickListener { listener.onClickConfig(book, cardView, it, layoutPosition) }
        config.setOnLongClickListener {
            listener.onClickLongConfig(book, cardView, it, layoutPosition)
            true
        }

        favorite.setImageResource(if (book.favorite) R.drawable.ico_favorite_mark else R.drawable.ico_favorite_unmark)

        val image = when ((1..5).random()) {
            1 -> mDefaultImageCover1
            2 -> mDefaultImageCover2
            3 -> mDefaultImageCover3
            4 -> mDefaultImageCover4
            else -> mDefaultImageCover5
        }

        bookImage.setImageBitmap(image)
        BookImageCoverController.instance.setImageCoverAsync(itemView.context, book, bookImage)

        bookTitle.text = book.title
        bookSubTitle.text = book.author
        bookSubTitle.visibility = if (book.author.isEmpty()) View.GONE else View.VISIBLE

        bookType.text = Util.getExtensionFromPath(book.path).uppercase()
        val percent: Float =
            if (book.bookMark > 0) ((book.bookMark.toFloat() / book.pages) * 100) else 0f
        bookPagesRead.text = Util.formatDecimal(percent)

        bookLastAccess.text = if (book.lastAccess == null) "" else GeneralConsts.formatterDate(
            itemView.context,
            book.lastAccess!!
        )

        bookProgress.max = book.pages
        bookProgress.setProgress(book.bookMark, false)
    }

}