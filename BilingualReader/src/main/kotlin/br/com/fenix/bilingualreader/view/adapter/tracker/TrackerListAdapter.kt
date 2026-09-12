package br.com.fenix.bilingualreader.view.adapter.tracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Track
import br.com.fenix.bilingualreader.service.listener.TrackerCardListener

class TrackerListAdapter(
    private val listener: TrackerCardListener
) : RecyclerView.Adapter<TrackerListAdapter.TrackerListViewHolder>() {

    private var mList: MutableList<Track> = mutableListOf()
    private var mLibrariesMap: Map<Long, Library> = emptyMap()
    private var mCountsMap: Map<Long, Int> = emptyMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackerListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.line_card_tracker, parent, false)
        return TrackerListViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: TrackerListViewHolder, position: Int) {
        val track = mList[position]
        val library = mLibrariesMap[track.fkLibrary]
        val count = mCountsMap[track.id] ?: 0
        holder.bind(track, library, count)
    }

    override fun getItemCount(): Int = mList.size

    fun updateList(list: List<Track>, libraries: Map<Long, Library> = mLibrariesMap, counts: Map<Long, Int> = mCountsMap) {
        mList = list.toMutableList()
        mLibrariesMap = libraries
        mCountsMap = counts
        notifyDataSetChanged()
    }

    class TrackerListViewHolder(
        itemView: View,
        private val listener: TrackerCardListener
    ) : RecyclerView.ViewHolder(itemView) {

        private val textTitle: TextView = itemView.findViewById(R.id.tracker_line_title)
        private val textStatus: TextView = itemView.findViewById(R.id.tracker_line_status)
        private val textLibrary: TextView = itemView.findViewById(R.id.tracker_line_library)
        private val textItemsCount: TextView = itemView.findViewById(R.id.tracker_line_items_count)
        private val textRegex: TextView = itemView.findViewById(R.id.tracker_line_regex)
        private val textProgress: TextView = itemView.findViewById(R.id.tracker_line_progress)
        private val textScore: TextView = itemView.findViewById(R.id.tracker_line_score)

        fun bind(track: Track, library: Library?, linkedCount: Int) {
            val context = itemView.context

            textTitle.text = track.title.ifBlank { track.titleRegex }
            textStatus.text = track.status.description

            val libraryName = if (library != null) {
                "${library.type.name} - ${library.title}"
            } else {
                context.getString(R.string.tracker_library)
            }
            textLibrary.text = libraryName
            textItemsCount.text = context.getString(R.string.tracker_items_count, linkedCount)

            textRegex.text = "Regex: ${track.titleRegex}"

            val volStr = if (track.totalVolumes != null && track.totalVolumes!! > 0) {
                "Vol: ${track.volumesRead}/${track.totalVolumes}"
            } else {
                "Vol: ${track.volumesRead}"
            }

            val chapStr = if (track.totalChapters != null && track.totalChapters!! > 0) {
                "Cap: ${track.chaptersRead}/${track.totalChapters}"
            } else {
                "Cap: ${track.chaptersRead}"
            }

            textProgress.text = "$volStr | $chapStr"

            if (track.score != null && track.score!! > 0) {
                textScore.visibility = View.VISIBLE
                textScore.text = "★ ${track.score}"
            } else {
                textScore.visibility = View.GONE
            }

            itemView.setOnClickListener {
                listener.onClick(track)
            }

            itemView.setOnLongClickListener {
                listener.onLongClick(track)
            }
        }
    }
}
