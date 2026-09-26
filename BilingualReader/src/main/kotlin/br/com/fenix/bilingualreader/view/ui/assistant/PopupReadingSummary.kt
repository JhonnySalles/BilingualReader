package br.com.fenix.bilingualreader.view.ui.assistant

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.llm.BookTextExtractor
import br.com.fenix.bilingualreader.service.llm.ChapterSummaryService
import br.com.fenix.bilingualreader.service.llm.LlmInferenceEngine
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.util.helpers.LlmSettings
import br.com.fenix.bilingualreader.util.helpers.UserLanguageHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object PopupReadingSummary {

    fun showBook(
        context: Context,
        scope: CoroutineScope,
        book: Book,
        parse: DocumentParse,
        currentPage0Based: Int
    ) {
        if (!ensureLlmEnabled(context)) return

        val ranges = BookTextExtractor.buildChapterRanges(parse)
        val options = ranges.map { it.title }
        val restored = book.id?.let { id ->
            AssistantSelectionHelper.decodeBookSelection(
                LlmSettings.loadSelection(context, Type.BOOK, id),
                options
            )
        }
        val selected = (restored ?: run {
            val defaults = BookTextExtractor.selectLastChapters(
                ranges,
                currentPage0Based + 1,
                LlmSettings.defaultBookChapters()
            )
            val defaultTitles = defaults.map { it.title }.toSet()
            ranges.withIndex()
                .filter { it.value.title in defaultTitles }
                .map { it.index }
                .toSet()
        }).toMutableSet()

        show(
            context = context,
            scope = scope,
            type = Type.BOOK,
            selectorHintRes = R.string.llm_assistant_select_chapters,
            options = options,
            selectedIndices = selected,
            maxSelection = LlmSettings.maxBookChapters(context),
            formatSummary = { indices ->
                AssistantSelectionHelper.formatBookSelection(context, options, indices)
            },
            persistSelection = { indices ->
                book.id?.let { id ->
                    val titles = indices.sorted().mapNotNull { options.getOrNull(it) }
                    LlmSettings.saveSelection(
                        context,
                        Type.BOOK,
                        id,
                        AssistantSelectionHelper.encodeBookSelection(titles)
                    )
                }
            },
            prepareText = { indices ->
                val chosen = indices.sorted().mapNotNull { ranges.getOrNull(it) }
                val service = ChapterSummaryService(context)
                val (text, titles) = service.prepareChaptersText(parse, chosen)
                Triple(text, ChapterSummaryService.selectionKeyFromTitles(titles), book.chapter)
            },
            workTitle = book.title.ifBlank { book.name },
            referenceId = book.id,
            openAssistant = { summary ->
                ReadingAssistantActivity.prepareBook(
                    title = book.title.ifBlank { book.name },
                    page = currentPage0Based,
                    chapter = book.chapter,
                    bookId = book.id,
                    parse = parse,
                    language = book.language,
                    preloadSummary = summary
                )
                context.startActivity(Intent(context, ReadingAssistantActivity::class.java))
            }
        )
    }

    fun showManga(
        context: Context,
        scope: CoroutineScope,
        manga: Manga,
        parse: Parse?,
        currentPage0Based: Int,
        ocrLanguage: Languages?
    ) {
        if (!ensureLlmEnabled(context)) return

        val pageCount = parse?.numPages() ?: 0
        if (pageCount <= 0) {
            MaterialAlertDialogBuilder(context, R.style.AppCompatAlertDialogStyle)
                .setTitle(R.string.llm_summary_title)
                .setMessage(R.string.llm_summary_empty)
                .setPositiveButton(R.string.action_neutral, null)
                .show()
            return
        }

        val options = (0 until pageCount).map { pageIndex ->
            context.getString(R.string.llm_assistant_page_label, pageIndex + 1)
        }
        val restored = manga.id?.let { id ->
            AssistantSelectionHelper.decodeMangaSelection(
                LlmSettings.loadSelection(context, Type.MANGA, id),
                pageCount
            )
        }
        val selected = (restored ?: run {
            val radius = LlmSettings.defaultMangaRadius()
            val from = (currentPage0Based - radius).coerceAtLeast(0)
            val to = (currentPage0Based + radius).coerceAtMost(pageCount - 1)
            (from..to).toSet()
        }).toMutableSet()
        val title = manga.title.ifBlank { manga.name }

        show(
            context = context,
            scope = scope,
            type = Type.MANGA,
            selectorHintRes = R.string.llm_assistant_select_pages,
            options = options,
            selectedIndices = selected,
            maxSelection = LlmSettings.maxMangaPages(context),
            formatSummary = { indices ->
                AssistantSelectionHelper.formatMangaSelection(context, indices)
            },
            persistSelection = { indices ->
                manga.id?.let { id ->
                    LlmSettings.saveSelection(
                        context,
                        Type.MANGA,
                        id,
                        AssistantSelectionHelper.encodeMangaSelection(indices.sorted())
                    )
                }
            },
            prepareText = { indices ->
                val pages = indices.sorted()
                val service = ChapterSummaryService(context)
                val text = service.prepareMangaPagesText(
                    parse = parse,
                    title = title,
                    currentPage0Based = currentPage0Based,
                    pages = pages,
                    ocrLanguage = ocrLanguage,
                    referenceId = manga.id
                )
                Triple(text, ChapterSummaryService.selectionKeyFromPages(pages), currentPage0Based)
            },
            workTitle = title,
            referenceId = manga.id,
            openAssistant = { summary ->
                ReadingAssistantActivity.prepareManga(
                    title = title,
                    page = currentPage0Based,
                    mangaId = manga.id,
                    parse = parse,
                    ocrLanguage = ocrLanguage,
                    preloadSummary = summary
                )
                context.startActivity(Intent(context, ReadingAssistantActivity::class.java))
            }
        )
    }

    private fun show(
        context: Context,
        scope: CoroutineScope,
        type: Type,
        selectorHintRes: Int,
        options: List<String>,
        selectedIndices: MutableSet<Int>,
        maxSelection: Int,
        formatSummary: (Set<Int>) -> String,
        persistSelection: (Set<Int>) -> Unit,
        prepareText: suspend (Set<Int>) -> Triple<String, String, Int>,
        workTitle: String,
        referenceId: Long?,
        openAssistant: (String) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.popup_assistant_chapter_summary, null)
        val selectorLayout = view.findViewById<TextInputLayout>(R.id.summary_selector_layout)
        val selector = view.findViewById<MaterialAutoCompleteTextView>(R.id.summary_selector)
        val generateButton = view.findViewById<MaterialButton>(R.id.summary_generate)
        val progress = view.findViewById<ProgressBar>(R.id.summary_progress)
        val text = view.findViewById<TextView>(R.id.summary_text)

        selectorLayout.hint = context.getString(selectorHintRes)
        selector.keyListener = null
        selector.setText(formatSummary(selectedIndices), false)
        text.text = context.getString(R.string.llm_summary_select_hint)

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

        fun openPicker() {
            AssistantSelectionHelper.showMultiChoice(
                context = context,
                type = type,
                options = options,
                selectedIndices = selectedIndices,
                maxSelection = maxSelection,
                onConfirmed = { next ->
                    selectedIndices.clear()
                    selectedIndices.addAll(next)
                    persistSelection(selectedIndices)
                    selector.setText(formatSummary(selectedIndices), false)
                }
            )
        }

        selector.setOnClickListener { openPicker() }
        selector.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                openPicker()
                selector.clearFocus()
            }
        }
        selectorLayout.setEndIconOnClickListener { openPicker() }

        dialog.setOnShowListener {
            val positive = dialog.getButton(android.app.Dialog.BUTTON_POSITIVE)
            positive.isEnabled = false
            positive.setOnClickListener {
                openAssistant(lastSummary.ifBlank { return@setOnClickListener })
                dialog.dismiss()
            }

            generateButton.setOnClickListener {
                if (selectedIndices.isEmpty()) {
                    Toast.makeText(context, R.string.llm_assistant_selection_none, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                job?.cancel()
                positive.isEnabled = false
                lastSummary = ""
                text.setText(R.string.llm_summary_generating)
                progress.visibility = View.VISIBLE
                generateButton.isEnabled = false

                LlmModelGate.ensureReady(context, scope, onReady = {
                    job = scope.launch {
                        try {
                            val (chaptersText, selectionKey, chapterEnd) = withContext(Dispatchers.IO) {
                                prepareText(selectedIndices.toSet())
                            }
                            if (chaptersText.isBlank()) {
                                progress.visibility = View.GONE
                                generateButton.isEnabled = true
                                text.setText(R.string.llm_summary_empty)
                                return@launch
                            }

                            val userLang = UserLanguageHelper.getUserLanguage(context)
                            ChapterSummaryService(context).summarizeStreaming(
                                title = workTitle,
                                chaptersText = chaptersText,
                                userLanguage = userLang,
                                referenceId = referenceId,
                                chapterEnd = chapterEnd,
                                selectionKey = selectionKey
                            ).catch { e ->
                                progress.visibility = View.GONE
                                generateButton.isEnabled = true
                                text.text = resolveLlmError(context, e)
                            }.collect { (partial, done) ->
                                lastSummary = partial
                                text.text = partial.ifBlank {
                                    context.getString(R.string.llm_summary_generating)
                                }
                                if (done) {
                                    progress.visibility = View.GONE
                                    generateButton.isEnabled = true
                                    positive.isEnabled = partial.isNotBlank()
                                }
                            }
                        } catch (e: Throwable) {
                            progress.visibility = View.GONE
                            generateButton.isEnabled = true
                            text.text = resolveLlmError(context, e)
                        }
                    }
                }, onCancel = { dialog.dismiss() })
            }
        }

        dialog.show()
    }

    private fun ensureLlmEnabled(context: Context): Boolean {
        if (UserLanguageHelper.isLlmEnabled(context)) return true
        MaterialAlertDialogBuilder(context, R.style.AppCompatAlertDialogStyle)
            .setTitle(R.string.alert_title)
            .setMessage(R.string.llm_disabled)
            .setPositiveButton(R.string.action_neutral, null)
            .show()
        return false
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
