package br.com.fenix.bilingualreader.view.adapter.library

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.enums.LibraryBookType
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import br.com.fenix.bilingualreader.service.listener.BookCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.AdapterUtil.AdapterUtils
import br.com.fenix.bilingualreader.util.helpers.Util
import com.google.android.material.card.MaterialCardView


class BookSeparatorGridViewHolder(var type: LibraryBookType, itemView: View, private val listener: BookCardListener) : RecyclerView.ViewHolder(itemView) {

    companion object {
        var mIsLandscape: Boolean = false
        var mBookCardSize: Pair<Int, Int> = Pair(0, 0)
        lateinit var mDefaultImageCover1: Bitmap
        lateinit var mDefaultImageCover2: Bitmap
        lateinit var mDefaultImageCover3: Bitmap
        lateinit var mDefaultImageCover4: Bitmap
        lateinit var mDefaultImageCover5: Bitmap
    }

    init {
        mIsLandscape = itemView.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        mBookCardSize = AdapterUtils.getBookCardSize(itemView.context, type, mIsLandscape)

        mDefaultImageCover1 = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_1)
        mDefaultImageCover2 = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_2)
        mDefaultImageCover3 = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_3)
        mDefaultImageCover4 = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_4)
        mDefaultImageCover5 = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_5)
    }

    @SuppressLint("SetTextI18n")
    fun bind(book: Book) {
        val bookImage = itemView.findViewById<ImageView>(R.id.book_grid_image_cover)
        val bookTitle = itemView.findViewById<TextView>(R.id.book_grid_title)
        val bookSubTitle = itemView.findViewById<TextView>(R.id.book_grid_sub_title)
        val bookType = itemView.findViewById<TextView>(R.id.book_grid_file_type)
        val bookPagesRead = itemView.findViewById<TextView>(R.id.book_grid_pages)
        val bookLastAccess = itemView.findViewById<TextView>(R.id.book_grid_last_access)

        val cardView = itemView.findViewById<MaterialCardView>(R.id.book_grid_card)
        val bookProgress = itemView.findViewById<ProgressBar>(R.id.book_grid_progress)
        val favorite = itemView.findViewById<LinearLayout>(R.id.book_grid_favorite)
        val favoriteIcon = itemView.findViewById<ImageView>(R.id.book_grid_favorite_icon)
        val config = itemView.findViewById<LinearLayout>(R.id.book_grid_config)
        val configIcon = itemView.findViewById<ImageView>(R.id.book_grid_config_icon)

        cardView.layoutParams.width = mBookCardSize.first
        cardView.layoutParams.height = mBookCardSize.second
        cardView.setOnClickListener { listener.onClick(book, itemView) }
        cardView.setOnLongClickListener {
            listener.onClickLong(book, itemView, layoutPosition)
            true
        }

        favorite.setOnClickListener {
            book.favorite = !book.favorite
            favoriteIcon.setImageResource(if (book.favorite) R.drawable.ico_favorite_mark else R.drawable.ico_favorite_unmark)
            listener.onClickFavorite(book)
        }

        favoriteIcon.setImageResource(if (book.favorite) R.drawable.ico_favorite_mark else R.drawable.ico_favorite_unmark)
        config.setOnClickListener { listener.onClickConfig(book, cardView, configIcon, layoutPosition) }

        val image = when ((1..5).random()) {
            1 -> mDefaultImageCover1
            2 -> mDefaultImageCover2
            3 -> mDefaultImageCover3
            4 -> mDefaultImageCover4
            else -> mDefaultImageCover5
        }

        bookImage.setImageBitmap(null)
        BookImageCoverController.instance.setImageCoverAsync(itemView.context, book, bookImage, image)

        bookTitle.text = book.title
        bookSubTitle.text = book.author
        bookSubTitle.visibility = if (book.author.isEmpty()) View.GONE else View.VISIBLE

        bookType.text = Util.getExtensionFromPath(book.path).uppercase()
        val percent: Float = if (book.bookMark >= book.pages) 100f else if (book.bookMark > 0) ((book.bookMark.toFloat() / book.pages) * 100) else 0f
        bookPagesRead.text = Util.formatDecimal(percent)

        val isSmall = book.lastAccess != null && book.bookMark > 0 && type != LibraryBookType.GRID_BIG && type != LibraryBookType.SEPARATOR_BIG
        bookLastAccess.text = if (book.lastAccess == null) "" else GeneralConsts.formatterDate(itemView.context, book.lastAccess!!, isSmall)

        bookProgress.max = book.pages
        bookProgress.setProgress(book.bookMark, false)
    }

}