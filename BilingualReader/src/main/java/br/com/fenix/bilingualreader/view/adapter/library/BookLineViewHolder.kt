package br.com.fenix.bilingualreader.view.adapter.library

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
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import br.com.fenix.bilingualreader.service.listener.BookCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.Util

class BookLineViewHolder(itemView: View, private val listener: BookCardListener) :
    RecyclerView.ViewHolder(itemView) {

    companion object {
        lateinit var mDefaultImageCover: Bitmap
    }

    init {
        mDefaultImageCover = BitmapFactory.decodeResource(itemView.resources, R.mipmap.app_icon)
    }

    fun bind(book: Book) {
        val bookImage = itemView.findViewById<ImageView>(R.id.book_line_image_cover)
        val bookTitle = itemView.findViewById<TextView>(R.id.book_line_title)
        val bookSubTitle = itemView.findViewById<TextView>(R.id.book_line_sub_title)
        val bookFileName = itemView.findViewById<TextView>(R.id.book_line_file_name)
        val bookFileSize = itemView.findViewById<TextView>(R.id.book_line_file_size)
        val bookType = itemView.findViewById<TextView>(R.id.book_line_file_type)
        val bookPagesRead = itemView.findViewById<TextView>(R.id.book_line_pages)
        val bookLastAccess = itemView.findViewById<TextView>(R.id.book_line_last_access)

        val cardView = itemView.findViewById<LinearLayout>(R.id.book_line_card)
        val bookProgress = itemView.findViewById<ProgressBar>(R.id.book_line_progress)
        val favorite = itemView.findViewById<ImageView>(R.id.book_line_favorite)
        val config = itemView.findViewById<ImageView>(R.id.book_line_config)

        bookImage.setImageBitmap(mDefaultImageCover)
        BookImageCoverController.instance.setImageCoverAsync(itemView.context, book, bookImage)

        cardView.setOnClickListener { listener.onClick(book) }
        cardView.setOnLongClickListener {
            listener.onClickLong(book, it, layoutPosition)
            true
        }

        favorite.setOnClickListener { listener.onClickFavorite(book, it, layoutPosition) }
        config.setOnClickListener { listener.onClickConfig(book, it, layoutPosition) }
        config.setOnLongClickListener {
            listener.onClickLongConfig(book, it, layoutPosition)
            true
        }

        favorite.setImageResource(if (book.favorite) R.drawable.ico_favorite_mark else R.drawable.ico_favorite_unmark)

        val image = when ((1..5).random()) {
            1 -> BookGridViewHolder.mDefaultImageCover1
            2 -> BookGridViewHolder.mDefaultImageCover2
            3 -> BookGridViewHolder.mDefaultImageCover3
            4 -> BookGridViewHolder.mDefaultImageCover4
            else -> BookGridViewHolder.mDefaultImageCover5
        }

        bookImage.setImageBitmap(image)
        BookImageCoverController.instance.setImageCoverAsync(itemView.context, book, bookImage)

        bookTitle.text = book.title
        bookSubTitle.text = book.author
        bookFileName.text = book.fileName
        bookFileSize.text = FileUtil.formatSize(book.fileSize)
        bookType.text = book.type.toString()
        val percent: Float =
            if (book.bookMark > 0) ((book.bookMark.toFloat() / book.pages) * 100) else 0f
        bookPagesRead.text = Util.formatDecimal(percent)

        bookLastAccess.text = if (book.lastAccess == null) "" else GeneralConsts.formatterDateTime(
            itemView.context,
            book.lastAccess!!
        )

        bookProgress.max = book.pages
        bookProgress.setProgress(book.bookMark, false)

    }

}