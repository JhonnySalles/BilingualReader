package br.com.fenix.bilingualreader.view.ui.assistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.AssistantMessage
import br.com.fenix.bilingualreader.model.enums.AssistantMessageRole
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.llm.BookContextProvider
import br.com.fenix.bilingualreader.service.llm.ContextSource
import br.com.fenix.bilingualreader.service.llm.LlmInferenceEngine
import br.com.fenix.bilingualreader.service.llm.LlmModelManager
import br.com.fenix.bilingualreader.service.llm.LlmPromptBuilder
import br.com.fenix.bilingualreader.service.llm.LlmUnsupportedDeviceException
import br.com.fenix.bilingualreader.service.llm.MangaContextProvider
import br.com.fenix.bilingualreader.service.llm.ReadingContext
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.util.helpers.UserLanguageHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ReadingAssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val _messages = MutableLiveData<List<AssistantMessage>>(emptyList())
    val messages: LiveData<List<AssistantMessage>> = _messages

    private val _contextSource = MutableLiveData(ContextSource.EMPTY)
    val contextSource: LiveData<ContextSource> = _contextSource

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _generating = MutableLiveData(false)
    val generating: LiveData<Boolean> = _generating

    private var readingContext: ReadingContext? = null
    private var generateJob: Job? = null

    private var type: Type = Type.BOOK
    private var title: String = ""
    private var page: Int = 0
    private var bookParse: DocumentParse? = null
    private var mangaParse: Parse? = null
    private var ocrLanguage: Languages? = null
    private var bookLanguage: Languages? = null

    fun configure(
        type: Type,
        title: String,
        page: Int,
        bookParse: DocumentParse? = null,
        mangaParse: Parse? = null,
        ocrLanguage: Languages? = null,
        bookLanguage: Languages? = null,
        preloadSummary: String? = null
    ) {
        this.type = type
        this.title = title
        this.page = page
        this.bookParse = bookParse
        this.mangaParse = mangaParse
        this.ocrLanguage = ocrLanguage
        this.bookLanguage = bookLanguage

        val initial = mutableListOf<AssistantMessage>()
        if (!preloadSummary.isNullOrBlank()) {
            initial.add(AssistantMessage(AssistantMessageRole.ASSISTANT, preloadSummary))
        }
        _messages.value = initial
    }

    fun refreshContext() {
        viewModelScope.launch {
            _loading.value = true
            try {
                readingContext = when (type) {
                    Type.BOOK -> {
                        val parse = bookParse
                        if (parse != null) {
                            BookContextProvider(
                                getApplication(),
                                parse,
                                title,
                                page,
                                bookLanguage
                            ).build()
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
                            ocrLanguage
                        ).build()
                    }
                }
                _contextSource.value = readingContext?.source ?: ContextSource.EMPTY
            } finally {
                _loading.value = false
            }
        }
    }

    fun ask(question: String) {
        if (question.isBlank() || _generating.value == true) return
        val ctx = readingContext
        if (ctx == null || ctx.chunks.isEmpty()) {
            append(AssistantMessage(AssistantMessageRole.SYSTEM, getApplication<Application>().getString(R.string.llm_assistant_context_empty)))
            return
        }

        append(AssistantMessage(AssistantMessageRole.USER, question))
        append(AssistantMessage(AssistantMessageRole.ASSISTANT, getApplication<Application>().getString(R.string.llm_assistant_thinking)))
        _generating.value = true

        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            try {
                val manager = LlmModelManager.getInstance(getApplication())
                val engine = LlmInferenceEngine.getInstance(getApplication())
                if (!manager.isModelReady()) throw IllegalStateException("Model missing")
                engine.ensureLoaded(manager.getModelFile().absolutePath)

                val maxChars = UserLanguageHelper.maxContextChars(getApplication())
                val prompt = LlmPromptBuilder.buildQaPrompt(ctx, question, maxChars)
                engine.generateStreamingTokens(prompt)
                    .catch { e ->
                        replaceLastAssistant(resolveError(e))
                        _generating.value = false
                    }
                    .collect { (text, done) ->
                        replaceLastAssistant(text.ifBlank {
                            getApplication<Application>().getString(R.string.llm_assistant_thinking)
                        })
                        if (done) _generating.value = false
                    }
            } catch (e: Exception) {
                replaceLastAssistant(resolveError(e))
                _generating.value = false
            }
        }
    }

    fun cancel() {
        generateJob?.cancel()
        _generating.value = false
    }

    private fun resolveError(error: Throwable): String {
        val app = getApplication<Application>()
        return when {
            error is LlmUnsupportedDeviceException ||
                LlmInferenceEngine.isNativeLinkFailure(error) ->
                app.getString(R.string.llm_error_unsupported_device)
            else ->
                app.getString(R.string.llm_assistant_error) + "\n" + (error.message ?: "")
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
