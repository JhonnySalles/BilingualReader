package br.com.fenix.bilingualreader.view.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.History
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import com.google.android.material.button.MaterialButton
import java.time.format.DateTimeFormatter

class ReadingHistoryAdapter(
    private val context: Context,
    var items: MutableList<History>,
    private val onRecalculateClick: (Int, History, ImageView) -> Unit
) : RecyclerView.Adapter<ReadingHistoryAdapter.ViewHolder>() {

    private val dateFormatter = DateTimeFormatter.ofPattern(GeneralConsts.PATTERNS.DATE_PATTERN)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.adapter_reading_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        val dateStr = item.start.format(dateFormatter)
        holder.date.text = dateStr

        val pagesStr = "${item.pageStart} - ${item.getPageEnd()}"
        holder.pages.text = pagesStr

        val totalSeconds = item.getSecondsRead()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        holder.time.text = timeStr

        holder.btnRecalc.setOnClickListener {
            onRecalculateClick(position, item, holder.icoRecalc)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.adapter_reading_history_date)
        val pages: TextView = view.findViewById(R.id.adapter_reading_history_pages)
        val time: TextView = view.findViewById(R.id.adapter_reading_history_time)
        val btnRecalc: MaterialButton = view.findViewById(R.id.adapter_reading_history_btn_recalc)
        val icoRecalc: ImageView = view.findViewById(R.id.adapter_reading_history_ico_recalc)
    }
}
