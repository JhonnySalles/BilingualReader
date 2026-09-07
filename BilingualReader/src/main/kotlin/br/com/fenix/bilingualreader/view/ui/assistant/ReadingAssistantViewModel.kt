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
import br.com.fenix.bilingualreader.service.llm.AssistantSessionHolder
import br.com.fenix.bilingualreader.service.llm.BookContextProvider
import br.com.fenix.bilingualreader.service.llm.BookTextExtractor
import br.com.fenix.bilingualreader.service.llm.ContextSource
import br.com.fenix.bilingualreader.service.llm.LlmBackendFactory
import br.com.fenix.bilingualreader.service.llm.LlmChatMessage
import br.com.fenix.bilingualreader.service.llm.LlmInferenceEngine
import br.com.fenix.bilingualreader.service.llm.LlmPromptBuilder
import br.com.fenix.bilingualreader.service.llm.LlmUnsupportedDeviceException
import br.com.fenix.bilingualreader.service.llm.MangaContextProvider
import br.com.fenix.bilingualreader.service.llm.ReadingContext
import br.com.fenix.bilingualreader.service.llm.SmartContextOrchestrator
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.service.repository.AssistantHistoryRepository
import br.com.fenix.bilingualreader.util.helpers.LlmSettings
import br.com.fenix.bilingualreader.util.helpers.Telemetry
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

    private val messageList = mutableListOf<AssistantMessage>()
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

    private val _contextSizeInfo = MutableLiveData<Pair<Int, Int>>(0 to 4096)
    val contextSizeInfo: LiveData<Pair<Int, Int>> = _contextSizeInfo

    private val _wordCount = MutableLiveData(0)
    val wordCount: LiveData<Int> = _wordCount

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
        setMessages(initial)
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

    private var lastLoadedSelectionKey: String? = null

    fun refreshContext(force: Boolean = false) {
        val selected = _selectedIndices.value.orEmpty().sorted().joinToString(",")
        val currentKey = "${type}_${referenceId}_${page}_${selected}"
        if (!force && lastLoadedSelectionKey == currentKey && readingContext != null) {
            return
        }

        viewModelScope.launch {
            _loading.value = true
            try {
                readingContext = when (type) {
                    Type.BOOK -> {
                        val parse = bookParse
                        if (parse != null) {
                            val selectedRanges = selectedBookRanges()
                            BookContextProvider(
                                getApplication(),
                                parse,
                                title,
                                page,
                                bookLanguage
                            ).build(selectedRanges)
                        } else {
                            emptyContext()
                        }
                    }
                    Type.MANGA -> {
                        val selectedPages = selectedMangaPages()
                        val provider = MangaContextProvider(
                            getApplication(),
                            mangaParse,
                            title,
                            page,
                            ocrLanguage,
                            referenceId
                        )
                        if (selectedPages.isEmpty()) {
                            provider.build(LlmSettings.defaultMangaRadius())
                        } else {
                            provider.build(selectedPages)
                        }
                    }
                }
                lastLoadedSelectionKey = currentKey
                _contextSource.value = readingContext?.source ?: ContextSource.EMPTY
                updateContextSummary()
                updateContextPreview()
            } catch (e: Throwable) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Telemetry.recordException(e, "ReadingAssistantViewModel refreshContext failed")
                }
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

    fun startQuestionProcess(question: String) {
        if (question.isBlank() || _generating.value == true) return
        append(AssistantMessage(AssistantMessageRole.USER, question))
        persistMessage(AssistantMessageRole.USER, question)
        append(AssistantMessage(AssistantMessageRole.ASSISTANT, getApplication<Application>().getString(R.string.llm_assistant_loading_model)))
        _generating.value = true
    }

    fun cancelQuestionProcess() {
        _generating.value = false
        removeThinkingPlaceholder()
    }

    fun ask(question: String, preAsked: Boolean = false) {
        if (question.isBlank()) return
        val ctx = readingContext
        if (ctx == null || ctx.chunks.isEmpty()) {
            val emptyMsg = getApplication<Application>().getString(R.string.llm_assistant_context_empty)
            if (preAsked) {
                replaceLastAssistant(emptyMsg)
            } else {
                append(AssistantMessage(AssistantMessageRole.SYSTEM, emptyMsg))
            }
            _generating.value = false
            return
        }

        if (!preAsked) {
            append(AssistantMessage(AssistantMessageRole.USER, question))
            persistMessage(AssistantMessageRole.USER, question)
            append(AssistantMessage(AssistantMessageRole.ASSISTANT, getApplication<Application>().getString(R.string.llm_assistant_loading_model)))
            _generating.value = true
        }

        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            try {
                val (historyList, ragQuery) = buildTrimmedHistory(question)
                val maxChars = UserLanguageHelper.maxContextChars(getApplication())
                val orchestrator = SmartContextOrchestrator(getApplication(), ctx)
                orchestrator.prepareIndex()
                val relevantContext = orchestrator.getRelevantContext(ragQuery, maxChars)

                val request = LlmPromptBuilder.buildQaRequest(relevantContext, question, maxChars, historyList)
                val backend = LlmBackendFactory.resolve(
                    getApplication(),
                    br.com.fenix.bilingualreader.model.enums.LlmUse.QA,
                    type,
                    customModel = AssistantSessionHolder.selectedOpenRouterModel
                )
                backend.ensureReady()

                replaceLastAssistant(getApplication<Application>().getString(R.string.llm_assistant_thinking), notifyImmediately = true)

                var lastStreamUpdateTime = 0L
                val streamThrottleMs = 50L

                backend.generateStreaming(request)
                    .catch { e ->
                        Telemetry.recordException(e, "LlmBackend generateStreaming error")
                        val errorText = resolveError(e)
                        replaceLastAssistant(errorText, notifyImmediately = true)
                        persistMessage(AssistantMessageRole.ASSISTANT, errorText)
                        _generating.value = false
                    }
                    .collect { (text, done) ->
                        val display = text.ifBlank {
                            getApplication<Application>().getString(R.string.llm_assistant_thinking)
                        }
                        val now = System.currentTimeMillis()
                        if (done || now - lastStreamUpdateTime >= streamThrottleMs) {
                            lastStreamUpdateTime = now
                            replaceLastAssistant(display, notifyImmediately = true)
                        } else {
                            replaceLastAssistant(display, notifyImmediately = false)
                        }

                        if (done) {
                            replaceLastAssistant(display, notifyImmediately = true)
                            if (text.isNotBlank()) {
                                persistMessage(AssistantMessageRole.ASSISTANT, text)
                            }
                            _generating.value = false
                        }
                    }
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Telemetry.recordException(e, "ReadingAssistantViewModel ask error")
                val errorText = resolveError(e)
                replaceLastAssistant(errorText, notifyImmediately = true)
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
            messageList.clear()
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
                _selectedIndices.value = emptySet()
            }
            Type.MANGA -> {
                bookRanges = emptyList()
                val pageCount = mangaParse?.numPages() ?: 0
                _availableOptions.value = (0 until pageCount).map { pageIndex ->
                    app.getString(R.string.llm_assistant_page_label, pageIndex + 1)
                }
                _selectedIndices.value = emptySet()
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
        val rc = readingContext
        val full = rc?.joinedText.orEmpty()
        val app = getApplication<Application>()
        val maxChars = UserLanguageHelper.maxContextChars(app)
        _contextSizeInfo.value = full.length to maxChars
        _wordCount.value = rc?.wordCount ?: 0

        if (full.isBlank()) {
            _contextPreviewText.value = ""
            return
        }
        val truncated = LlmPromptBuilder.truncate(full, PREVIEW_MAX_CHARS)
        _contextPreviewText.value = truncated
    }

    private fun removeThinkingPlaceholder() {
        val thinking = getApplication<Application>().getString(R.string.llm_assistant_thinking)
        val loadingModel = getApplication<Application>().getString(R.string.llm_assistant_loading_model)
        val index = messageList.indexOfLast { it.role == AssistantMessageRole.ASSISTANT }
        if (index >= 0 && (messageList[index].text == thinking || messageList[index].text == loadingModel)) {
            messageList.removeAt(index)
            _messages.value = messageList.toList()
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
        var current: Throwable? = error
        while (current != null) {
            val msg = current.message.orEmpty()
            if (msg.contains("OUT_OF_RANGE", ignoreCase = true) || msg.contains("Input is too long", ignoreCase = true)) {
                return error.message ?: app.getString(R.string.llm_assistant_error)
            }
            current = current.cause
        }
        return when {
            error is LlmUnsupportedDeviceException ||
                LlmInferenceEngine.isNativeLinkFailure(error) ->
                app.getString(R.string.llm_error_unsupported_device)
            LlmInferenceEngine.isModelIncompatibleFailure(error) ->
                app.getString(R.string.llm_error_model_incompatible)
            else ->
                error.message.takeIf { !it.isNullOrBlank() } ?: app.getString(R.string.llm_assistant_error)
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

    private fun setMessages(list: List<AssistantMessage>) {
        messageList.clear()
        messageList.addAll(list)
        _messages.value = messageList.toList()
    }

    private fun append(message: AssistantMessage) {
        messageList.add(message)
        _messages.value = messageList.toList()
    }

    private fun replaceLastAssistant(text: String, notifyImmediately: Boolean = true) {
        val index = messageList.indexOfLast { it.role == AssistantMessageRole.ASSISTANT }
        if (index >= 0) {
            val existing = messageList[index]
            messageList[index] = existing.copy(text = text)
        } else {
            messageList.add(AssistantMessage(AssistantMessageRole.ASSISTANT, text))
        }
        if (notifyImmediately) {
            _messages.value = messageList.toList()
        }
    }

    private fun buildTrimmedHistory(currentQuestion: String): Pair<List<LlmChatMessage>, String> {
        val app = getApplication<Application>()
        val maxChars = LlmSettings.maxHistoryChars(app)
        val rawMessages = _messages.value.orEmpty()
            .filter { it.role == AssistantMessageRole.USER || it.role == AssistantMessageRole.ASSISTANT }
            .filter {
                it.text != app.getString(R.string.llm_assistant_loading_model) &&
                it.text != app.getString(R.string.llm_assistant_thinking) &&
                it.text.isNotBlank()
            }

        val candidateMessages = if (rawMessages.isNotEmpty() && rawMessages.last().role == AssistantMessageRole.USER && rawMessages.last().text == currentQuestion) {
            rawMessages.dropLast(1)
        } else rawMessages

        val historyList = mutableListOf<LlmChatMessage>()
        var totalChars = 0
        var lastUserMsgText = ""

        for (msg in candidateMessages.reversed()) {
            if (msg.role == AssistantMessageRole.USER && lastUserMsgText.isEmpty()) {
                lastUserMsgText = msg.text
            }
            val textLength = msg.text.length
            if (totalChars + textLength > maxChars && historyList.isNotEmpty()) {
                break
            }
            val roleStr = if (msg.role == AssistantMessageRole.USER) "user" else "assistant"
            historyList.add(0, LlmChatMessage(roleStr, msg.text))
            totalChars += textLength
            if (historyList.size >= 4) break
        }

        val ragQuery = if (lastUserMsgText.isNotBlank()) "$lastUserMsgText $currentQuestion" else currentQuestion
        return historyList to ragQuery
    }
}
