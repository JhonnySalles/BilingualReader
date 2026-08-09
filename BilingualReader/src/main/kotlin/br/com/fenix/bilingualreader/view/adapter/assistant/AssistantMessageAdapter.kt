package br.com.fenix.bilingualreader.view.adapter.assistant

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.AssistantMessage
import br.com.fenix.bilingualreader.model.enums.AssistantMessageRole

class AssistantMessageAdapter : RecyclerView.Adapter<AssistantMessageAdapter.Holder>() {

    private val items = mutableListOf<AssistantMessage>()

    fun submit(list: List<AssistantMessage>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_assistant_message, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.assistant_message_text)

        fun bind(message: AssistantMessage) {
            textView.text = message.text
            val params = textView.layoutParams as FrameLayout.LayoutParams
            params.gravity = when (message.role) {
                AssistantMessageRole.USER -> Gravity.END
                else -> Gravity.START
            }
            textView.layoutParams = params
            textView.alpha = if (message.role == AssistantMessageRole.SYSTEM) 0.75f else 1f
        }
    }
}
