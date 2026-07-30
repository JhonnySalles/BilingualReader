package br.com.fenix.bilingualreader.view.adapter.history

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController
import br.com.fenix.bilingualreader.service.listener.HistoryCardListener
import com.google.android.material.card.MaterialCardView

class HistoryCoverCardAdapter(private val listener: HistoryCardListener) :
    RecyclerView.Adapter<HistoryCoverCardAdapter.HistoryCoverViewHolder>() {

    companion object {
        private var mCoverCache: MutableMap<String, Bitmap?> = mutableMapOf()
        fun clearCoverCache() = mCoverCache.clear()

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

        fun formatVolume(volume: String): String {
            val digits = volume.filter { it.isDigit() }
            if (digits.isEmpty())
                return "00"
            return digits.toIntOrNull()?.toString()?.padStart(2, '0') ?: "00"
        }
    }

    private var mList: List<History> = listOf()
    private var mOrder: Order = Order.Series

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryCoverViewHolder {
        val item = LayoutInflater.from(parent.context).inflate(R.layout.line_card_history_cover, parent, false)
        return HistoryCoverViewHolder(item)
    }

    override fun onBindViewHolder(holder: HistoryCoverViewHolder, position: Int) {
        holder.bind(mList[position], mOrder)
    }

    override fun getItemCount(): Int = mList.size

    fun updateList(list: List<History>, order: Order = Order.Series) {
        mList = list
        mOrder = order
        notifyDataSetChanged()
    }

    inner class HistoryCoverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(history: History, order: Order) {
            ensureDefaults(itemView)

            val card = itemView.findViewById<MaterialCardView>(R.id.history_cover_image_card)
            val cover = itemView.findViewById<ImageView>(R.id.history_cover_image)
            val title = itemView.findViewById<TextView>(R.id.history_cover_title)
            val progress = itemView.findViewById<ProgressBar>(R.id.history_cover_progress)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                TooltipCompat.setTooltipText(card, history.title)
            else
                card.tooltipText = history.title

            if (order == Order.Series) {
                title.maxLines = 1
                title.isSingleLine = true
                title.text = formatVolume(history.volume)
            } else {
                title.maxLines = 4
                title.isSingleLine = false
                title.text = history.name
            }

            progress.max = history.pages.coerceAtLeast(1)
            progress.progress = history.bookMark

            card.setOnClickListener { listener.onClick(history) }
            card.setOnLongClickListener {
                listener.onClickLong(history, it, bindingAdapterPosition)
                true
            }

            val defaultCover = when ((1..5).random()) {
                1 -> mDefaultImageCover1
                2 -> mDefaultImageCover2
                3 -> mDefaultImageCover3
                4 -> mDefaultImageCover4
                else -> mDefaultImageCover5
            }

            val cacheKey = "${history.type}_${history.id}"
            if (mCoverCache.contains(cacheKey)) {
                mCoverCache[cacheKey]?.let { cover.setImageBitmap(it) }
                    ?: cover.setImageBitmap(defaultCover)
            } else {
                cover.setImageBitmap(null)
                when (history) {
                    is Manga -> MangaImageCoverController.instance.setImageCoverAsync(
                        itemView.context, history, cover, null, true
                    ) { bitmap ->
                        cacheBitmap(cacheKey, bitmap)
                        if (bitmap == null)
                            cover.setImageBitmap(defaultCover)
                    }
                    is Book -> BookImageCoverController.instance.setImageCoverAsync(
                        itemView.context, history, cover, null, true
                    ) { bitmap ->
                        cacheBitmap(cacheKey, bitmap)
                        if (bitmap == null)
                            cover.setImageBitmap(defaultCover)
                    }
                    else -> cover.setImageBitmap(defaultCover)
                }
            }
        }

        private fun cacheBitmap(key: String, bitmap: Bitmap?) {
            if (bitmap != null) {
                mCoverCache[key] = bitmap
                if (mCoverCache.size > 2000)
                    mCoverCache.remove(mCoverCache.entries.first().key)
            }
        }
    }
}
