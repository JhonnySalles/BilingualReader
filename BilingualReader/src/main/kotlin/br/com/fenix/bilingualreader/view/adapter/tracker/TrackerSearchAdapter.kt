package br.com.fenix.bilingualreader.view.adapter.tracker

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.collection.LruCache
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.service.tracker.model.TrackerSearchResult
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class TrackerSearchAdapter(
    private var items: List<TrackerSearchResult> = emptyList(),
    private val onItemClick: (TrackerSearchResult) -> Unit
) : RecyclerView.Adapter<TrackerSearchAdapter.ViewHolder>() {

    companion object {
        private val sImageCache = LruCache<String, Bitmap>(50)
    }

    private val mAdapterScope = CoroutineScope(Dispatchers.Main + Job())

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: List<TrackerSearchResult>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.line_card_tracker_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val coverImage: ShapeableImageView = itemView.findViewById(R.id.tracker_search_item_cover)
        private val textTitle: TextView = itemView.findViewById(R.id.tracker_search_item_title)
        private val textId: TextView = itemView.findViewById(R.id.tracker_search_item_id)
        private val textStatus: TextView = itemView.findViewById(R.id.tracker_search_item_status)
        private val textDetails: TextView = itemView.findViewById(R.id.tracker_search_item_details)
        private val textSynopsis: TextView = itemView.findViewById(R.id.tracker_search_item_synopsis)

        private var imageLoadJob: Job? = null

        fun bind(item: TrackerSearchResult) {
            val context = itemView.context
            textTitle.text = item.title
            textId.text = "ID: ${item.id}"

            if (!item.status.isNullOrBlank()) {
                textStatus.text = item.status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                textStatus.visibility = View.VISIBLE
            } else {
                textStatus.visibility = View.GONE
            }

            val details = mutableListOf<String>()
            item.totalVolumes?.let { if (it > 0) details.add(context.getString(R.string.tracker_search_volumes_format, it)) }
            item.totalChapters?.let { if (it > 0) details.add(context.getString(R.string.tracker_search_chapters_format, it)) }
            if (details.isNotEmpty()) {
                textDetails.text = details.joinToString(" • ")
                textDetails.visibility = View.VISIBLE
            } else {
                textDetails.visibility = View.GONE
            }

            if (!item.synopsis.isNullOrBlank()) {
                textSynopsis.text = item.synopsis.trim()
                textSynopsis.visibility = View.VISIBLE
            } else {
                textSynopsis.visibility = View.GONE
            }

            coverImage.setImageResource(R.drawable.ico_tracker)
            imageLoadJob?.cancel()

            val coverUrl = item.coverUrl
            if (!coverUrl.isNullOrBlank()) {
                val cachedBmp = sImageCache.get(coverUrl)
                if (cachedBmp != null) {
                    coverImage.setImageBitmap(cachedBmp)
                } else {
                    imageLoadJob = mAdapterScope.launch(Dispatchers.IO) {
                        try {
                            val stream = URL(coverUrl).openStream()
                            val bmp = BitmapFactory.decodeStream(stream)
                            if (bmp != null) {
                                sImageCache.put(coverUrl, bmp)
                                withContext(Dispatchers.Main) {
                                    coverImage.setImageBitmap(bmp)
                                }
                            }
                        } catch (_: Exception) {
                            // Ignore image load failure, keeps fallback icon
                        }
                    }
                }
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
