package br.com.fenix.bilingualreader.view.adapter.assistant

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.AssistantMessage
import br.com.fenix.bilingualreader.model.enums.AssistantMessageRole
import com.google.android.material.color.MaterialColors

class AssistantMessageAdapter : ListAdapter<AssistantMessage, AssistantMessageAdapter.Holder>(DiffCallback) {

    fun submit(list: List<AssistantMessage>, commitCallback: Runnable? = null) {
        submitList(list, commitCallback)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_assistant_message, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    private object DiffCallback : DiffUtil.ItemCallback<AssistantMessage>() {
        override fun areItemsTheSame(oldItem: AssistantMessage, newItem: AssistantMessage): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: AssistantMessage, newItem: AssistantMessage): Boolean =
            oldItem == newItem
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.assistant_message_text)

        fun bind(message: AssistantMessage) {
            textView.text = message.text
            val params = textView.layoutParams as FrameLayout.LayoutParams
            val context = itemView.context

            textView.setOnLongClickListener {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Assistant Message", message.text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, R.string.llm_assistant_copied_to_clipboard, Toast.LENGTH_SHORT).show()
                true
            }

            when (message.role) {
                AssistantMessageRole.USER -> {
                    params.gravity = Gravity.END
                    textView.setBackgroundResource(R.drawable.bg_assistant_bubble_user)
                    textView.setTextColor(
                        MaterialColors.getColor(textView, com.google.android.material.R.attr.colorOnPrimaryContainer)
                    )
                    textView.alpha = 1f
                }
                AssistantMessageRole.ASSISTANT -> {
                    params.gravity = Gravity.START
                    textView.setBackgroundResource(R.drawable.bg_assistant_bubble_assistant)
                    textView.setTextColor(
                        MaterialColors.getColor(textView, com.google.android.material.R.attr.colorOnSurface)
                    )
                    textView.alpha = 1f
                }
                AssistantMessageRole.SYSTEM -> {
                    params.gravity = Gravity.CENTER_HORIZONTAL
                    textView.setBackgroundResource(R.drawable.bg_assistant_bubble_system)
                    val typed = TypedValue()
                    val resolved = context.theme.resolveAttribute(
                        com.google.android.material.R.attr.colorOnSurfaceVariant,
                        typed,
                        true
                    )
                    if (resolved) {
                        textView.setTextColor(
                            if (typed.resourceId != 0) ContextCompat.getColor(context, typed.resourceId)
                            else typed.data
                        )
                    }
                    textView.alpha = 0.85f
                }
            }
            textView.layoutParams = params
        }
    }
}
