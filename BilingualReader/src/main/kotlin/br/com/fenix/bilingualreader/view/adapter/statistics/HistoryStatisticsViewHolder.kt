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
        private var mDefaultsLoaded = false

        private fun ensureDefaults(itemView: View) {
            if (mDefaultsLoaded) return
            mDefaultImageCover = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_2)
            mDescriptionSeries = itemView.context.getString(R.string.manga_library_line_series) + " "
            mDescriptionPublisher = itemView.context.getString(R.string.manga_library_line_publisher) + " "
            mDescriptionAuthor = itemView.context.getString(R.string.manga_library_line_authors) + " "
            mDefaultsLoaded = true
        }
    }

    private val status: View = itemView.findViewById(R.id.history_status)
    private val image: ImageView = itemView.findViewById(R.id.history_image_cover)
    private val title: TextView = itemView.findViewById(R.id.history_text_title)
    private val lastAccess: TextView = itemView.findViewById(R.id.history_line_last_access)
    private val pagesReadOverall: TextView = itemView.findViewById(R.id.history_line_pages)
    private val pagesReadDaily: TextView = itemView.findViewById(R.id.history_line_pages_read)
    private val timeReadDaily: TextView = itemView.findViewById(R.id.history_line_time_read)
    private val library: TextView = itemView.findViewById(R.id.history_library)
    private val type: TextView = itemView.findViewById(R.id.history_type)
    private val favorite: ImageView = itemView.findViewById(R.id.history_favorite)
    private val subtitle: ImageView = itemView.findViewById(R.id.history_has_subtitle)
    private val cardView: LinearLayout = itemView.findViewById(R.id.history_card)
    private val series: TextView = itemView.findViewById(R.id.history_line_series)
    private val author: TextView = itemView.findViewById(R.id.history_line_author)
    private val publisher: TextView = itemView.findViewById(R.id.history_line_publisher)
    private val progress: ProgressBar = itemView.findViewById(R.id.history_line_progress)

    init {
        ensureDefaults(itemView)
    }

    fun bind(history: History) {
        cardView.setOnClickListener { listener.onClick(history) }
        cardView.setOnLongClickListener {
            listener.onClickLong(history, it, layoutPosition)
            true
        }

        val base = if (history is HistoryStatistics) history.base else history
        when (base) {
            is Manga -> MangaImageCoverController.instance.setImageCoverAsync(itemView.context, base, image, mDefaultImageCover)
            is Book -> BookImageCoverController.instance.setImageCoverAsync(itemView.context, base, image, mDefaultImageCover)
        }

        title.text = history.title
        val themeColor = when (history.type) {
            Type.MANGA -> androidx.core.content.ContextCompat.getColor(itemView.context, R.color.card_manga_indigo_base)
            Type.BOOK -> androidx.core.content.ContextCompat.getColor(itemView.context, R.color.card_book_amber_base)
        }
        title.setTextColor(themeColor)
        favorite.imageTintList = android.content.res.ColorStateList.valueOf(themeColor)

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

        val progressDrawableRes = when (history.type) {
            Type.MANGA -> R.drawable.progress_bar_manga
            Type.BOOK -> R.drawable.progress_bar_book
        }
        progress.progressTintList = null
        progress.progressBackgroundTintList = null
        progress.progressDrawable = androidx.core.content.ContextCompat.getDrawable(itemView.context, progressDrawableRes)

        progress.max = history.pages
        progress.setProgress(history.bookMark, false)

        library.text = if (history.fkLibrary == GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA || history.fkLibrary == GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK)
            itemView.context.getString(R.string.manga_library_default).uppercase()
        else
            history.library.title.uppercase()

        type.text = when (history.type) {
            Type.BOOK -> itemView.context.getString(R.string.history_book)
            Type.MANGA -> itemView.context.getString(R.string.history_manga)
        }

        favorite.visibility = if (history.favorite) View.VISIBLE else View.GONE
        subtitle.visibility = if (base is Manga && base.hasSubtitle) {
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
