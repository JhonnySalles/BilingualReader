package br.com.fenix.bilingualreader.view.adapter.statistics

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
import br.com.fenix.bilingualreader.model.entity.HistoryStatistics
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController
import br.com.fenix.bilingualreader.service.listener.HistoryCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Util
import java.time.format.DateTimeFormatter

class HistoryStatisticsViewHolder(itemView: View, private val listener: HistoryCardListener) : RecyclerView.ViewHolder(itemView) {

    companion object {
        lateinit var mDefaultImageCover: Bitmap
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

    fun bind(history: History) {
        val status = itemView.findViewById<View>(R.id.history_status)
        val image = itemView.findViewById<ImageView>(R.id.history_image_cover)
        val title = itemView.findViewById<TextView>(R.id.history_text_title)
        val lastAccess = itemView.findViewById<TextView>(R.id.history_line_last_access)
        val pagesReadOverall = itemView.findViewById<TextView>(R.id.history_line_pages)
        val pagesReadDaily = itemView.findViewById<TextView>(R.id.history_line_pages_read)
        val timeReadDaily = itemView.findViewById<TextView>(R.id.history_line_time_read)
        val library = itemView.findViewById<TextView>(R.id.history_library)
        val type = itemView.findViewById<TextView>(R.id.history_type)
        val favorite = itemView.findViewById<ImageView>(R.id.history_favorite)
        val subtitle = itemView.findViewById<ImageView>(R.id.history_has_subtitle)
        val cardView = itemView.findViewById<LinearLayout>(R.id.history_card)

        val series = itemView.findViewById<TextView>(R.id.history_line_series)
        val author = itemView.findViewById<TextView>(R.id.history_line_author)
        val publisher = itemView.findViewById<TextView>(R.id.history_line_publisher)
        val progress = itemView.findViewById<ProgressBar>(R.id.history_line_progress)

        cardView.setOnClickListener { listener.onClick(history) }
        cardView.setOnLongClickListener {
            listener.onClickLong(history, it, layoutPosition)
            true
        }

        image.setImageBitmap(null)
        val base = if (history is HistoryStatistics) history.base else history
        when (base) {
            is Manga -> MangaImageCoverController.instance.setImageCoverAsync(itemView.context, base, image, mDefaultImageCover)
            is Book -> BookImageCoverController.instance.setImageCoverAsync(itemView.context, base, image, mDefaultImageCover)
        }

        title.text = history.title

        // Format exact session time (hour and minute) or fallback
        lastAccess.text = if (history.lastAccess != null) {
            history.lastAccess!!.format(DateTimeFormatter.ofPattern("HH:mm"))
        } else {
            ""
        }

        val percent: Float = if (history.bookMark > 0) ((history.bookMark.toFloat() / history.pages) * 100) else 0f
        pagesReadOverall.text = "${history.bookMark} / ${history.pages}" + if (percent > 0) (" (" + Util.formatDecimal(percent) + ")") else ""

        if (history is HistoryStatistics) {
            pagesReadDaily.text = "+${history.pagesRead} pág."
            pagesReadDaily.visibility = View.VISIBLE
            timeReadDaily.text = formatTime(history.timeRead)
            timeReadDaily.visibility = View.VISIBLE
        } else {
            pagesReadDaily.visibility = View.GONE
            timeReadDaily.visibility = View.GONE
        }

        var authorText = ""
        var seriesText = ""
        var publisherText = ""

        when (base) {
            is Manga -> {
                authorText = base.author
                seriesText = base.series
                publisherText = base.publisher
            }
            is Book -> {
                authorText = base.author
                publisherText = base.publisher
            }
        }

        author.text = ""
        author.visibility = if (authorText.isNotEmpty()) {
            author.text = mDescriptionAuthor + authorText
            View.VISIBLE
        } else View.GONE

        series.text = ""
        series.visibility = if (seriesText.isNotEmpty()) {
            series.text = mDescriptionSeries + seriesText
            View.VISIBLE
        } else View.GONE

        publisher.text = ""
        publisher.visibility = if (publisherText.isNotEmpty()) {
            publisher.text = mDescriptionPublisher + publisherText
            View.VISIBLE
        } else View.GONE

        progress.max = history.pages
        progress.setProgress(history.bookMark, false)

        library.text = if (history.fkLibrary == GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA || history.fkLibrary == GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK)
            itemView.context.getString(R.string.manga_library_default).uppercase()
        else
            history.library.title.uppercase()

        type.text = when(history.type) {
            Type.BOOK -> itemView.context.getString(R.string.history_book)
            Type.MANGA -> itemView.context.getString(R.string.history_manga)
            else -> ""
        }

        favorite.visibility = if (history.favorite) View.VISIBLE else View.GONE
        subtitle.visibility  = if (base is Manga && base.hasSubtitle) {
            if (base.lastVocabImport != null)
                subtitle.setImageResource(R.drawable.ico_subtitles_imported)
            else
                subtitle.setImageResource(R.drawable.ico_subtitles_exist)
            View.VISIBLE
        } else View.GONE

        if (history.excluded) {
            cardView.setBackgroundResource(R.drawable.custom_ripple_history_item_deleted)
            status.setBackgroundResource(R.drawable.history_item_deleted_background)
        } else {
            cardView.setBackgroundResource(R.drawable.custom_ripple_history)
            status.setBackgroundResource(R.color.transparent)
        }
    }

    private fun formatTime(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return when {
            hrs > 0 -> "${hrs}h ${mins}m"
            mins > 0 -> "${mins}m ${secs}s"
            else -> "${secs}s"
        }
    }

}
