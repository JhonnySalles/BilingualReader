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

        var mDescriptionAuthor: String = ""
        var mDescriptionPublisher: String = ""
    }

    init {
        mDefaultImageCover = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_2)

        mDescriptionPublisher = itemView.context.getString(R.string.book_library_line_publisher) + " "
        mDescriptionAuthor = itemView.context.getString(R.string.book_library_line_authors) + " "
    }

    fun bind(book: Book) {
        val bookImage = itemView.findViewById<ImageView>(R.id.book_line_image_cover)
        val bookTitle = itemView.findViewById<TextView>(R.id.book_line_title)
        val bookAuthor = itemView.findViewById<TextView>(R.id.book_line_author)
        val bookPublisher = itemView.findViewById<TextView>(R.id.book_line_publisher)
        val bookFileName = itemView.findViewById<TextView>(R.id.book_line_file_name)
        val bookFileSize = itemView.findViewById<TextView>(R.id.book_line_file_size)
        val bookType = itemView.findViewById<TextView>(R.id.book_line_file_type)
        val bookPagesRead = itemView.findViewById<TextView>(R.id.book_line_pages)
        val bookLastAccess = itemView.findViewById<TextView>(R.id.book_line_last_access)

        val cardView = itemView.findViewById<LinearLayout>(R.id.book_line_card)
        val bookProgress = itemView.findViewById<ProgressBar>(R.id.book_line_progress)
        val favorite = itemView.findViewById<LinearLayout>(R.id.book_line_favorite)
        val favoriteIcon = itemView.findViewById<ImageView>(R.id.book_line_favorite_icon)
        val config = itemView.findViewById<LinearLayout>(R.id.book_line_config)
        val configIcon = itemView.findViewById<ImageView>(R.id.book_line_config_icon)

        bookImage.setImageBitmap(null)
        BookImageCoverController.instance.setImageCoverAsync(itemView.context, book, bookImage, mDefaultImageCover)

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

        bookImage.setImageBitmap(null)
        BookImageCoverController.instance.setImageCoverAsync(itemView.context, book, bookImage, mDefaultImageCover)

        bookTitle.text = book.title
        bookAuthor.text = if (book.author.isNotEmpty()) mDescriptionAuthor + book.author else ""
        bookFileName.text = book.fileName
        bookFileSize.text = FileUtil.formatSize(book.fileSize)
        bookType.text = Util.getExtensionFromPath(book.path).uppercase()
        val percent: Float = if (book.bookMark >= book.pages) 100f else if (book.bookMark > 0) ((book.bookMark.toFloat() / book.pages) * 100) else 0f
        bookPagesRead.text = if (book.bookMark > 0)
           itemView.context.getString(R.string.book_page_read, book.bookMark, book.pages, Util.formatDecimal(percent))
        else
            Util.formatDecimal(percent)

        bookPublisher.text = ""
        bookPublisher.visibility = if (book.publisher.isNotEmpty()) {
            bookPublisher.text = mDescriptionPublisher + book.publisher
            View.VISIBLE
        } else
            View.GONE

        bookLastAccess.text = if (book.lastAccess == null) "" else GeneralConsts.formatterDateTime(itemView.context, book.lastAccess!!)
        bookLastAccess.visibility = if (book.lastAccess == null) View.GONE else View.VISIBLE

        bookProgress.max = book.pages
        bookProgress.setProgress(book.bookMark, false)
    }

}