package br.com.fenix.bilingualreader.view.ui.assistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.AssistantHistory
import br.com.fenix.bilingualreader.model.enums.AssistantMessage
import br.com.fenix.bilingualreader.model.enums.AssistantMessageRole
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.llm.BookContextProvider
import br.com.fenix.bilingualreader.service.llm.BookTextExtractor
import br.com.fenix.bilingualreader.service.llm.ContextSource
import br.com.fenix.bilingualreader.service.llm.LlmBackendFactory
import br.com.fenix.bilingualreader.service.llm.LlmInferenceEngine
import br.com.fenix.bilingualreader.service.llm.LlmPromptBuilder
import br.com.fenix.bilingualreader.service.llm.LlmUnsupportedDeviceException
import br.com.fenix.bilingualreader.service.llm.MangaContextProvider
import br.com.fenix.bilingualreader.service.llm.ReadingContext
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.service.repository.AssistantHistoryRepository
import br.com.fenix.bilingualreader.util.helpers.LlmSettings
import br.com.fenix.bilingualreader.util.helpers.UserLanguageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReadingAssistantViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PREVIEW_MAX_CHARS = 8000
    }

    private val historyRepository = AssistantHistoryRepository(application)

    private val _messages = MutableLiveData<List<AssistantMessage>>(emptyList())
    val messages: LiveData<List<AssistantMessage>> = _messages

    private val _contextSource = MutableLiveData(ContextSource.EMPTY)
    val contextSource: LiveData<ContextSource> = _contextSource

    private val _contextSummary = MutableLiveData("")
    val contextSummary: LiveData<String> = _contextSummary

    private val _availableOptions = MutableLiveData<List<String>>(emptyList())
    val availableOptions: LiveData<List<String>> = _availableOptions

    private val _selectedIndices = MutableLiveData<Set<Int>>(emptySet())
    val selectedIndices: LiveData<Set<Int>> = _selectedIndices

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _generating = MutableLiveData(false)
    val generating: LiveData<Boolean> = _generating

    private val _selectionLimitExceeded = MutableLiveData<Int?>(null)
    val selectionLimitExceeded: LiveData<Int?> = _selectionLimitExceeded

    private val _contextPreviewText = MutableLiveData("")
    val contextPreviewText: LiveData<String> = _contextPreviewText

    private var readingContext: ReadingContext? = null
    private var generateJob: Job? = null

    private var type: Type = Type.BOOK
    private var referenceId: Long? = null
    private var title: String = ""
    private var page: Int = 0
    private var bookParse: DocumentParse? = null
    private var mangaParse: Parse? = null
    private var ocrLanguage: Languages? = null
    private var bookLanguage: Languages? = null

    private var bookRanges: List<BookTextExtractor.ChapterRange> = emptyList()

    fun configure(
        type: Type,
        title: String,
        page: Int,
        referenceId: Long? = null,
        bookParse: DocumentParse? = null,
        mangaParse: Parse? = null,
        ocrLanguage: Languages? = null,
        bookLanguage: Languages? = null,
        preloadSummary: String? = null
    ) {
        this.type = type
        this.title = title
        this.page = page
        this.referenceId = referenceId
        this.bookParse = bookParse
        this.mangaParse = mangaParse
        this.ocrLanguage = ocrLanguage
        this.bookLanguage = bookLanguage

        initAvailableOptions()

        val initial = mutableListOf<AssistantMessage>()
        if (referenceId != null) {
            historyRepository.find(type, referenceId).forEach { row ->
                if (row.role == AssistantMessageRole.USER || row.role == AssistantMessageRole.ASSISTANT) {
                    initial.add(AssistantMessage(row.role, row.message))
                }
            }
        }
        if (!preloadSummary.isNullOrBlank()) {
            initial.add(AssistantMessage(AssistantMessageRole.ASSISTANT, preloadSummary))
        }
        _messages.value = initial
    }

    fun isBookContext(): Boolean = type == Type.BOOK

    fun maxSelection(): Int = AssistantSelectionHelper.maxSelection(getApplication(), type)

    fun consumeSelectionLimitExceeded() {
        _selectionLimitExceeded.value = null
    }

    /**
     * Applies multi-choice selection by option indices. Returns false if over the limit.
     */
    fun applySelection(checkedIndices: BooleanArray): Boolean {
        val selected = checkedIndices.withIndex()
            .filter { it.value }
            .map { it.index }
            .toSet()
        return applySelection(selected)
    }

    fun applySelection(selected: Set<Int>): Boolean {
        val limit = maxSelection()
        if (selected.size > limit) {
            _selectionLimitExceeded.value = limit
            return false
        }
        _selectedIndices.value = selected
        persistCurrentSelection()
        updateContextSummary()
        refreshContext()
        return true
    }

    fun refreshContext() {
        viewModelScope.launch {
            _loading.value = true
            try {
                readingContext = when (type) {
                    Type.BOOK -> {
                        val parse = bookParse
                        if (parse != null) {
                            val selected = selectedBookRanges()
                            BookContextProvider(
                                getApplication(),
                                parse,
                                title,
                                page,
                                bookLanguage
                            ).build(selected)
                        } else {
                            emptyContext()
                        }
                    }
                    Type.MANGA -> {
                        MangaContextProvider(
                            getApplication(),
                            mangaParse,
                            title,
                            page,
                            ocrLanguage,
                            referenceId
                        ).build(selectedMangaPages())
                    }
                }
                _contextSource.value = readingContext?.source ?: ContextSource.EMPTY
                updateContextSummary()
                updateContextPreview()
            } finally {
                _loading.value = false
            }
        }
    }

    fun peekContextPreview(): String = _contextPreviewText.value.orEmpty()

    fun isContextReady(): Boolean {
        val ctx = readingContext
        return ctx != null && ctx.chunks.isNotEmpty()
    }

    fun ask(question: String) {
        if (question.isBlank() || _generating.value == true) return
        val ctx = readingContext
        if (ctx == null || ctx.chunks.isEmpty()) {
            append(AssistantMessage(AssistantMessageRole.SYSTEM, getApplication<Application>().getString(R.string.llm_assistant_context_empty)))
            return
        }

        append(AssistantMessage(AssistantMessageRole.USER, question))
        persistMessage(AssistantMessageRole.USER, question)
        append(AssistantMessage(AssistantMessageRole.ASSISTANT, getApplication<Application>().getString(R.string.llm_assistant_thinking)))
        _generating.value = true

        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            try {
                val maxChars = UserLanguageHelper.maxContextChars(getApplication())
                val request = LlmPromptBuilder.buildQaRequest(ctx, question, maxChars)
                val backend = LlmBackendFactory.resolve(getApplication(), br.com.fenix.bilingualreader.model.enums.LlmUse.QA)
                backend.ensureReady()
                backend.generateStreaming(request)
                    .catch { e ->
                        val errorText = resolveError(e)
                        replaceLastAssistant(errorText)
                        persistMessage(AssistantMessageRole.ASSISTANT, errorText)
                        _generating.value = false
                    }
                    .collect { (text, done) ->
                        val display = text.ifBlank {
                            getApplication<Application>().getString(R.string.llm_assistant_thinking)
                        }
                        replaceLastAssistant(display)
                        if (done) {
                            if (text.isNotBlank()) {
                                persistMessage(AssistantMessageRole.ASSISTANT, text)
                            }
                            _generating.value = false
                        }
                    }
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val errorText = resolveError(e)
                replaceLastAssistant(errorText)
                persistMessage(AssistantMessageRole.ASSISTANT, errorText)
                _generating.value = false
            }
        }
    }

    fun cancel() {
        generateJob?.cancel()
        _generating.value = false
        removeThinkingPlaceholder()
    }

    fun clearHistory() {
        val id = referenceId ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                historyRepository.clear(type, id)
            }
            _messages.value = emptyList()
        }
    }

    private fun initAvailableOptions() {
        val app = getApplication<Application>()
        when (type) {
            Type.BOOK -> {
                val parse = bookParse
                bookRanges = if (parse != null) {
                    BookContextProvider(app, parse, title, page, bookLanguage).allChapterRanges()
                } else {
                    emptyList()
                }
                val options = bookRanges.map { it.title }
                _availableOptions.value = options
                val restored = referenceId?.let { id ->
                    AssistantSelectionHelper.decodeBookSelection(
                        LlmSettings.loadSelection(app, type, id),
                        options
                    )
                }
                if (restored != null) {
                    _selectedIndices.value = restored
                } else {
                    val defaults = BookTextExtractor.selectLastChapters(
                        bookRanges,
                        page + 1,
                        LlmSettings.defaultBookChapters()
                    )
                    val defaultTitles = defaults.map { it.title }.toSet()
                    _selectedIndices.value = bookRanges.withIndex()
                        .filter { it.value.title in defaultTitles }
                        .map { it.index }
                        .toSet()
                }
            }
            Type.MANGA -> {
                bookRanges = emptyList()
                val pageCount = mangaParse?.numPages() ?: 0
                _availableOptions.value = (0 until pageCount).map { pageIndex ->
                    app.getString(R.string.llm_assistant_page_label, pageIndex + 1)
                }
                val restored = referenceId?.let { id ->
                    AssistantSelectionHelper.decodeMangaSelection(
                        LlmSettings.loadSelection(app, type, id),
                        pageCount
                    )
                }
                if (restored != null) {
                    _selectedIndices.value = restored
                } else {
                    val radius = LlmSettings.defaultMangaRadius()
                    val from = (page - radius).coerceAtLeast(0)
                    val to = (page + radius).coerceAtMost((pageCount - 1).coerceAtLeast(0))
                    _selectedIndices.value = if (pageCount <= 0) emptySet() else (from..to).toSet()
                }
            }
        }
        updateContextSummary()
    }

    private fun persistCurrentSelection() {
        val id = referenceId ?: return
        val selected = _selectedIndices.value.orEmpty()
        val encoded = when (type) {
            Type.BOOK -> {
                val titles = selected.sorted().mapNotNull { bookRanges.getOrNull(it)?.title }
                AssistantSelectionHelper.encodeBookSelection(titles)
            }
            Type.MANGA -> AssistantSelectionHelper.encodeMangaSelection(selected.sorted())
        }
        LlmSettings.saveSelection(getApplication(), type, id, encoded)
    }

    private fun selectedBookRanges(): List<BookTextExtractor.ChapterRange> {
        val indices = _selectedIndices.value.orEmpty()
        return bookRanges.withIndex()
            .filter { it.index in indices }
            .map { it.value }
    }

    private fun selectedMangaPages(): List<Int> {
        return _selectedIndices.value.orEmpty().sorted()
    }

    private fun updateContextSummary() {
        val app = getApplication<Application>()
        val options = _availableOptions.value.orEmpty()
        val selected = _selectedIndices.value.orEmpty()
        _contextSummary.value = when (type) {
            Type.BOOK -> AssistantSelectionHelper.formatBookSelection(app, options, selected)
            Type.MANGA -> AssistantSelectionHelper.formatMangaSelection(app, selected)
        }
    }

    private fun updateContextPreview() {
        val full = readingContext?.joinedText().orEmpty()
        if (full.isBlank()) {
            _contextPreviewText.value = ""
            return
        }
        val truncated = LlmPromptBuilder.truncate(full, PREVIEW_MAX_CHARS)
        val app = getApplication<Application>()
        _contextPreviewText.value = app.getString(
            R.string.llm_assistant_context_preview_body,
            full.length,
            truncated
        )
    }

    private fun removeThinkingPlaceholder() {
        val thinking = getApplication<Application>().getString(R.string.llm_assistant_thinking)
        val list = (_messages.value ?: emptyList()).toMutableList()
        val index = list.indexOfLast { it.role == AssistantMessageRole.ASSISTANT }
        if (index >= 0 && list[index].text == thinking) {
            list.removeAt(index)
            _messages.value = list
        }
    }

    private fun persistMessage(role: AssistantMessageRole, message: String) {
        val id = referenceId ?: return
        if (message.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            historyRepository.save(AssistantHistory(id, type, role, message))
        }
    }

    private fun resolveError(error: Throwable): String {
        val app = getApplication<Application>()
        return when {
            error is LlmUnsupportedDeviceException ||
                LlmInferenceEngine.isNativeLinkFailure(error) ->
                app.getString(R.string.llm_error_unsupported_device)
            LlmInferenceEngine.isModelIncompatibleFailure(error) ->
                app.getString(R.string.llm_error_model_incompatible)
            else ->
                error.message ?: app.getString(R.string.llm_assistant_error)
        }
    }

    private fun emptyContext(): ReadingContext {
        return ReadingContext(
            type = type,
            title = title,
            sourceLanguage = null,
            userLanguage = UserLanguageHelper.getUserLanguage(getApplication()),
            chunks = emptyList(),
            source = ContextSource.EMPTY
        )
    }

    private fun append(message: AssistantMessage) {
        _messages.value = (_messages.value ?: emptyList()) + message
    }

    private fun replaceLastAssistant(text: String) {
        val list = (_messages.value ?: emptyList()).toMutableList()
        val index = list.indexOfLast { it.role == AssistantMessageRole.ASSISTANT }
        if (index >= 0) {
            list[index] = AssistantMessage(AssistantMessageRole.ASSISTANT, text)
            _messages.value = list
        }
    }
}
