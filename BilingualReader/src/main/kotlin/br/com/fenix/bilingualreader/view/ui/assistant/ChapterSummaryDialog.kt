package br.com.fenix.bilingualreader.view.ui.assistant

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.service.llm.ChapterSummaryService
import br.com.fenix.bilingualreader.service.llm.LlmInferenceEngine
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.util.helpers.UserLanguageHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ChapterSummaryDialog {

    fun show(
        context: Context,
        scope: CoroutineScope,
        book: Book,
        parse: DocumentParse,
        currentPage0Based: Int
    ) {
        if (!UserLanguageHelper.isLlmEnabled(context)) {
            MaterialAlertDialogBuilder(context, R.style.AppCompatAlertDialogStyle)
                .setTitle(R.string.alert_title)
                .setMessage(R.string.llm_disabled)
                .setPositiveButton(R.string.action_neutral, null)
                .show()
            return
        }

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_chapter_summary, null)
        val progress = view.findViewById<ProgressBar>(R.id.summary_progress)
        val text = view.findViewById<TextView>(R.id.summary_text)
        text.setText(R.string.llm_summary_generating)

        var job: Job? = null
        var lastSummary = ""

        val dialog = MaterialAlertDialogBuilder(context, R.style.AppCompatAlertDialogStyle)
            .setTitle(R.string.llm_summary_title)
            .setView(view)
            .setNegativeButton(R.string.action_cancel) { d, _ ->
                job?.cancel()
                d.dismiss()
            }
            .setPositiveButton(R.string.llm_summary_open_assistant, null)
            .setCancelable(true)
            .create()

        dialog.setOnShowListener {
            val positive = dialog.getButton(android.app.Dialog.BUTTON_POSITIVE)
            positive.isEnabled = false
            positive.setOnClickListener {
                ReadingAssistantActivity.prepareBook(
                    title = book.title ?: book.name,
                    page = currentPage0Based,
                    chapter = book.chapter,
                    bookId = book.id,
                    parse = parse,
                    language = book.language,
                    preloadSummary = lastSummary.ifBlank { null }
                )
                context.startActivity(Intent(context, ReadingAssistantActivity::class.java))
                dialog.dismiss()
            }
        }

        dialog.show()

        LlmModelGate.ensureReady(context, scope, onReady = {
            job = scope.launch {
                try {
                    val service = ChapterSummaryService(context)
                    val (chaptersText, _) = withContext(Dispatchers.IO) {
                        service.prepareChaptersText(parse, currentPage0Based)
                    }
                    if (chaptersText.isBlank()) {
                        progress.visibility = android.view.View.GONE
                        text.setText(R.string.llm_summary_empty)
                        return@launch
                    }

                    val userLang = UserLanguageHelper.getUserLanguage(context)
                    service.summarizeStreaming(
                        title = book.title ?: book.name,
                        chaptersText = chaptersText,
                        userLanguage = userLang,
                        bookId = book.id,
                        chapterEnd = book.chapter
                    ).catch { e ->
                        progress.visibility = android.view.View.GONE
                        text.text = resolveLlmError(context, e)
                    }.collect { (partial, done) ->
                        lastSummary = partial
                        text.text = partial.ifBlank { context.getString(R.string.llm_summary_generating) }
                        if (done) {
                            progress.visibility = android.view.View.GONE
                            dialog.getButton(android.app.Dialog.BUTTON_POSITIVE)?.isEnabled =
                                partial.isNotBlank()
                        }
                    }
                } catch (e: Throwable) {
                    progress.visibility = android.view.View.GONE
                    text.text = resolveLlmError(context, e)
                }
            }
        }, onCancel = { dialog.dismiss() })
    }

    private fun resolveLlmError(context: Context, error: Throwable): String {
        return when {
            error is br.com.fenix.bilingualreader.service.llm.LlmUnsupportedDeviceException ||
                LlmInferenceEngine.isNativeLinkFailure(error) ->
                context.getString(R.string.llm_error_unsupported_device)
            LlmInferenceEngine.isModelIncompatibleFailure(error) ->
                context.getString(R.string.llm_error_model_incompatible)
            else ->
                error.message ?: context.getString(R.string.llm_assistant_error)
        }
    }
}
