package br.com.fenix.bilingualreader.view.ui.assistant

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.util.helpers.LlmSettings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

object AssistantSelectionHelper {

    fun formatBookSelection(context: Context, options: List<String>, indices: Set<Int>): String {
        val labels = indices.sorted().mapNotNull { options.getOrNull(it) }
        return when {
            labels.isEmpty() -> context.getString(R.string.llm_assistant_selection_default_book_pages)
            labels.size <= 3 -> context.getString(
                R.string.llm_assistant_chapters_summary_named,
                labels.size,
                labels.joinToString(", ")
            )
            else -> context.getString(R.string.llm_assistant_chapters_selected, labels.size)
        }
    }

    fun formatMangaSelection(context: Context, indices: Set<Int>): String {
        val pages = indices.sorted().map { it + 1 }
        return when {
            pages.isEmpty() -> context.getString(R.string.llm_assistant_selection_none)
            pages.size == 1 -> context.getString(
                R.string.llm_assistant_pages_summary_count,
                1,
                context.getString(R.string.llm_assistant_page_label, pages.first())
            )
            pages.zipWithNext().all { (a, b) -> b == a + 1 } -> context.getString(
                R.string.llm_assistant_pages_summary_count,
                pages.size,
                context.getString(R.string.llm_assistant_pages_range, pages.first(), pages.last())
            )
            else -> context.getString(R.string.llm_assistant_pages_selected, pages.size)
        }
    }

    fun encodeBookSelection(titles: List<String>): String =
        "c:" + titles.joinToString("|")

    fun encodeMangaSelection(pages0: List<Int>): String =
        "p:" + pages0.distinct().sorted().joinToString(",")

    fun decodeBookSelection(raw: String?, options: List<String>): Set<Int>? {
        if (raw.isNullOrBlank() || !raw.startsWith("c:")) return null
        val titles = raw.removePrefix("c:").split("|").filter { it.isNotBlank() }.toSet()
        if (titles.isEmpty()) return null
        val indices = options.withIndex()
            .filter { it.value in titles }
            .map { it.index }
            .toSet()
        return indices.takeIf { it.isNotEmpty() }
    }

    fun decodeMangaSelection(raw: String?, pageCount: Int): Set<Int>? {
        if (raw.isNullOrBlank() || !raw.startsWith("p:")) return null
        val pages = raw.removePrefix("p:").split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 0 until pageCount }
            .toSet()
        return pages.takeIf { it.isNotEmpty() }
    }

    fun showMultiChoice(
        context: Context,
        type: Type,
        options: List<String>,
        selectedIndices: Set<Int>,
        maxSelection: Int,
        onConfirmed: (Set<Int>) -> Unit
    ) {
        if (options.isEmpty()) {
            Toast.makeText(context, R.string.llm_assistant_selection_none, Toast.LENGTH_SHORT).show()
            return
        }

        val working = BooleanArray(options.size) { it in selectedIndices }
        val titleRes = if (type == Type.BOOK) {
            R.string.llm_assistant_select_chapters
        } else {
            R.string.llm_assistant_select_pages
        }

        val builder = MaterialAlertDialogBuilder(context, R.style.AppCompatAlertDialogMultiChoice)
            .setTitle(titleRes)
            .setMultiChoiceItems(options.toTypedArray(), working) { dialog, which, isChecked ->
                if (isChecked) {
                    val count = working.count { it }
                    if (count >= maxSelection) {
                        working[which] = false
                        (dialog as? androidx.appcompat.app.AlertDialog)?.listView?.setItemChecked(which, false)
                        Toast.makeText(
                            context,
                            context.getString(R.string.llm_assistant_selection_limit, maxSelection),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setMultiChoiceItems
                    }
                }
                working[which] = isChecked
            }
            .setPositiveButton(R.string.action_positive) { _, _ ->
                val next = working.withIndex().filter { it.value }.map { it.index }.toSet()
                if (next.size > maxSelection) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.llm_assistant_selection_limit, maxSelection),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }
                onConfirmed(next)
            }
            .setNegativeButton(R.string.action_negative, null)

        if (type == Type.MANGA) {
            builder.setNeutralButton(R.string.llm_assistant_page_range) { _, _ ->
                showPageRangeDialog(context, options.size, maxSelection) { range ->
                    onConfirmed(range)
                }
            }
        }

        builder.show()
    }

    private fun showPageRangeDialog(
        context: Context,
        pageCount: Int,
        maxSelection: Int,
        onConfirmed: (Set<Int>) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.popup_assistant_page_range, null)
        val fromInput = view.findViewById<TextInputEditText>(R.id.assistant_range_from)
        val toInput = view.findViewById<TextInputEditText>(R.id.assistant_range_to)
        view.findViewById<TextInputLayout>(R.id.assistant_range_from_layout).hint =
            context.getString(R.string.llm_assistant_page_range_from)
        view.findViewById<TextInputLayout>(R.id.assistant_range_to_layout).hint =
            context.getString(R.string.llm_assistant_page_range_to)

        MaterialAlertDialogBuilder(context, R.style.AppCompatAlertDialogStyle)
            .setTitle(R.string.llm_assistant_page_range)
            .setView(view)
            .setPositiveButton(R.string.action_positive) { _, _ ->
                val from1 = fromInput.text?.toString()?.toIntOrNull()
                val to1 = toInput.text?.toString()?.toIntOrNull()
                if (from1 == null || to1 == null || from1 < 1 || to1 < 1) {
                    Toast.makeText(context, R.string.llm_assistant_page_range_invalid, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val start = minOf(from1, to1).coerceIn(1, pageCount)
                val end = maxOf(from1, to1).coerceIn(1, pageCount)
                var from0 = start - 1
                var to0 = end - 1
                if (to0 - from0 + 1 > maxSelection) {
                    to0 = from0 + maxSelection - 1
                    Toast.makeText(
                        context,
                        context.getString(R.string.llm_assistant_selection_limit, maxSelection),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                to0 = to0.coerceAtMost(pageCount - 1)
                onConfirmed((from0..to0).toSet())
            }
            .setNegativeButton(R.string.action_negative, null)
            .show()
    }

    fun maxSelection(context: Context, type: Type): Int {
        return if (type == Type.BOOK) 3
        else LlmSettings.maxMangaPages(context)
    }
}
