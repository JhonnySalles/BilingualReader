package br.com.fenix.bilingualreader.view.adapter.history

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
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.HistoryType
import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController
import br.com.fenix.bilingualreader.service.listener.HistoryCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.AdapterUtil.AdapterUtils
import br.com.fenix.bilingualreader.util.helpers.Util
import com.google.android.material.card.MaterialCardView

class HistorySeparatorGridViewHolder(
    private val type: HistoryType,
    itemView: View,
    private val listener: HistoryCardListener
) : RecyclerView.ViewHolder(itemView) {

    companion object {
        lateinit var mDefaultImageCover1: Bitmap
        lateinit var mDefaultImageCover2: Bitmap
        lateinit var mDefaultImageCover3: Bitmap
        lateinit var mDefaultImageCover4: Bitmap
        lateinit var mDefaultImageCover5: Bitmap
        private var mDefaultsLoaded = false

        private fun ensureDefaults(itemView: View) {
            if (mDefaultsLoaded) return
            mDefaultImageCover1 = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_1)
            mDefaultImageCover2 = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_2)
            mDefaultImageCover3 = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_3)
            mDefaultImageCover4 = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_4)
            mDefaultImageCover5 = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_5)
            mDefaultsLoaded = true
        }
    }

    @SuppressLint("SetTextI18n")
    fun bind(history: History) {
        ensureDefaults(itemView)

        val historyImage = itemView.findViewById<ImageView>(R.id.history_grid_image_cover)
        val historyTitle = itemView.findViewById<TextView>(R.id.history_grid_title)
        val historySubTitle = itemView.findViewById<TextView>(R.id.history_grid_sub_title)
        val historyType = itemView.findViewById<TextView>(R.id.history_grid_file_type)
        val historyPagesRead = itemView.findViewById<TextView>(R.id.history_grid_pages)
        val historyLastAccess = itemView.findViewById<TextView>(R.id.history_grid_last_access)
        val cardView = itemView.findViewById<MaterialCardView>(R.id.history_grid_card)
        val historyProgress = itemView.findViewById<ProgressBar>(R.id.history_grid_progress)
        val favorite = itemView.findViewById<LinearLayout>(R.id.history_grid_favorite)

        val isLandscape = itemView.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val cardSize = AdapterUtils.getHistoryCardSize(itemView.context, type, isLandscape)
        cardView.layoutParams.width = cardSize.first
        cardView.layoutParams.height = cardSize.second
        cardView.setOnClickListener { listener.onClick(history) }
        cardView.setOnLongClickListener {
            listener.onClickLong(history, itemView, layoutPosition)
            true
        }

        favorite.visibility = if (history.favorite) View.VISIBLE else View.GONE

        val image = when ((1..5).random()) {
            1 -> mDefaultImageCover1
            2 -> mDefaultImageCover2
            3 -> mDefaultImageCover3
            4 -> mDefaultImageCover4
            else -> mDefaultImageCover5
        }

        historyImage.setImageBitmap(null)
        val coverSource = if (history is br.com.fenix.bilingualreader.model.entity.HistoryStatistics) history.base else history
        when (coverSource) {
            is Manga -> MangaImageCoverController.instance.setImageCoverAsync(itemView.context, coverSource, historyImage, image)
            is Book -> BookImageCoverController.instance.setImageCoverAsync(itemView.context, coverSource, historyImage, image)
            else -> historyImage.setImageBitmap(image)
        }

        historyTitle.text = history.title
        historySubTitle.text = history.author
        historySubTitle.visibility = if (history.author.isEmpty()) View.GONE else View.VISIBLE

        historyType.text = history.fileType.acronym
        val percent: Float = if (history.bookMark >= history.pages) 100f
        else if (history.bookMark > 0) ((history.bookMark.toFloat() / history.pages) * 100)
        else 0f
        historyPagesRead.text = Util.formatDecimal(percent)

        val isSmall = history.lastAccess != null && history.bookMark > 0 && type != HistoryType.SEPARATOR_BIG
        historyLastAccess.text = if (history.lastAccess == null) ""
        else GeneralConsts.formatterDate(itemView.context, history.lastAccess!!, isSmall)

        historyProgress.max = history.pages.coerceAtLeast(1)
        historyProgress.setProgress(history.bookMark, false)
    }

}
