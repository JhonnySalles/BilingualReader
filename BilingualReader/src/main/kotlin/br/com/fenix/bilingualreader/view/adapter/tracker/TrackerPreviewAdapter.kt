package br.com.fenix.bilingualreader.view.adapter.tracker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController

sealed class TrackerPreviewItem {
    data class MangaItem(val manga: Manga) : TrackerPreviewItem()
    data class BookItem(val book: Book) : TrackerPreviewItem()
}

class TrackerPreviewViewHolder(
    itemView: View,
    private val onMangaClick: ((Manga) -> Unit)?,
    private val onBookClick: ((Book) -> Unit)?
) : RecyclerView.ViewHolder(itemView) {

    private val imageCover: ImageView = itemView.findViewById(R.id.tracker_preview_image_cover)
    private val textTitle: TextView = itemView.findViewById(R.id.tracker_preview_title)
    private val textSeries: TextView = itemView.findViewById(R.id.tracker_preview_series)
    private val textAuthor: TextView = itemView.findViewById(R.id.tracker_preview_author)
    private val defaultCover: Bitmap = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_2)

    private val prefixSeries = itemView.context.getString(R.string.manga_library_line_series) + " "
    private val prefixAuthor = itemView.context.getString(R.string.manga_library_line_authors) + " "

    fun bind(item: TrackerPreviewItem) {
        when (item) {
            is TrackerPreviewItem.MangaItem -> bindManga(item.manga)
            is TrackerPreviewItem.BookItem -> bindBook(item.book)
        }
    }

    private fun bindManga(manga: Manga) {
        textTitle.text = manga.title

        if (manga.series.isNotBlank()) {
            textSeries.text = prefixSeries + manga.series
            textSeries.visibility = View.VISIBLE
        } else {
            textSeries.visibility = View.GONE
        }

        if (manga.author.isNotBlank()) {
            textAuthor.text = prefixAuthor + manga.author
            textAuthor.visibility = View.VISIBLE
        } else {
            textAuthor.visibility = View.GONE
        }

        imageCover.setImageBitmap(null)
        MangaImageCoverController.instance.setImageCoverAsync(itemView.context, manga, imageCover, defaultCover)

        itemView.setOnClickListener { onMangaClick?.invoke(manga) }
    }

    private fun bindBook(book: Book) {
        textTitle.text = book.title

        if (book.series.isNotBlank()) {
            textSeries.text = prefixSeries + book.series
            textSeries.visibility = View.VISIBLE
        } else {
            textSeries.visibility = View.GONE
        }

        if (book.author.isNotBlank()) {
            textAuthor.text = prefixAuthor + book.author
            textAuthor.visibility = View.VISIBLE
        } else {
            textAuthor.visibility = View.GONE
        }

        imageCover.setImageBitmap(null)
        BookImageCoverController.instance.setImageCoverAsync(itemView.context, book, imageCover, defaultCover)

        itemView.setOnClickListener { onBookClick?.invoke(book) }
    }
}

class TrackerPreviewAdapter(
    private val onMangaClick: ((Manga) -> Unit)? = null,
    private val onBookClick: ((Book) -> Unit)? = null
) : RecyclerView.Adapter<TrackerPreviewViewHolder>() {

    private val mItems: MutableList<TrackerPreviewItem> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackerPreviewViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.line_card_tracker_preview, parent, false)
        return TrackerPreviewViewHolder(view, onMangaClick, onBookClick)
    }

    override fun onBindViewHolder(holder: TrackerPreviewViewHolder, position: Int) {
        holder.bind(mItems[position])
    }

    override fun getItemCount(): Int = mItems.size

    fun getItems(): List<TrackerPreviewItem> = mItems

    fun updateItems(items: List<TrackerPreviewItem>) {
        mItems.clear()
        mItems.addAll(items)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int): TrackerPreviewItem? {
        if (position in mItems.indices) {
            val removed = mItems.removeAt(position)
            notifyItemRemoved(position)
            return removed
        }
        return null
    }
}
