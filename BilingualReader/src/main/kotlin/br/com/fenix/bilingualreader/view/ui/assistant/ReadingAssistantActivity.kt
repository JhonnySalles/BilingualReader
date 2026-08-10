package br.com.fenix.bilingualreader.view.ui.assistant

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.LlmProvider
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.llm.AssistantSessionHolder
import br.com.fenix.bilingualreader.service.llm.ContextSource
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.LlmSettings
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil
import br.com.fenix.bilingualreader.util.helpers.UserLanguageHelper
import br.com.fenix.bilingualreader.view.adapter.assistant.AssistantMessageAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

class ReadingAssistantActivity : AppCompatActivity() {

    private lateinit var viewModel: ReadingAssistantViewModel
    private val adapter = AssistantMessageAdapter()
    private val scope = MainScope()

    private lateinit var contextSelectorLayout: TextInputLayout
    private lateinit var contextSelector: MaterialAutoCompleteTextView
    private lateinit var contextLabel: TextView
    private lateinit var privacyLabel: TextView
    private lateinit var emptyState: LinearLayout
    private lateinit var suggestionChips: ChipGroup
    private lateinit var messagesList: RecyclerView
    private lateinit var input: TextInputEditText
    private lateinit var sendButton: MaterialButton
    private lateinit var cancelButton: MaterialButton
    private lateinit var loading: ProgressBar

    private var isLoadingContext = false
    private var isGenerating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtil.applyThemeMode(this)
        val theme = Themes.valueOf(
            GeneralConsts.getSharedPreferences(this)
                .getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!
        )
        setTheme(theme.getValue())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reading_assistant)

        if (!UserLanguageHelper.isLlmEnabled(this)) {
            Toast.makeText(this, R.string.llm_disabled, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        viewModel = ViewModelProvider(this)[ReadingAssistantViewModel::class.java]

        val toolbar = findViewById<MaterialToolbar>(R.id.assistant_toolbar)
        MenuUtil.tintToolbar(toolbar, theme)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        contextSelectorLayout = findViewById(R.id.assistant_context_selector_layout)
        contextSelector = findViewById(R.id.assistant_context_selector)
        contextLabel = findViewById(R.id.assistant_context_label)
        privacyLabel = findViewById(R.id.assistant_privacy)
        emptyState = findViewById(R.id.assistant_empty_state)
        suggestionChips = findViewById(R.id.assistant_suggestion_chips)
        input = findViewById(R.id.assistant_input)
        sendButton = findViewById(R.id.assistant_send)
        cancelButton = findViewById(R.id.assistant_cancel)
        loading = findViewById(R.id.assistant_loading)

        messagesList = findViewById(R.id.assistant_messages)
        messagesList.layoutManager = LinearLayoutManager(this)
        messagesList.adapter = adapter

        setupSuggestionChips()
        updatePrivacyLabel()
        contextSelector.keyListener = null
        contextSelector.setOnClickListener { showContextPicker() }
        contextSelector.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showContextPicker()
                contextSelector.clearFocus()
            }
        }
        contextSelectorLayout.setEndIconOnClickListener { showContextPicker() }

        findViewById<MaterialButton>(R.id.assistant_refresh_context).setOnClickListener {
            ensureModelThen { viewModel.refreshContext() }
        }
        sendButton.setOnClickListener { sendCurrentInput() }
        cancelButton.setOnClickListener { viewModel.cancel() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        observe()
        bindSession()
        ensureModelThen { viewModel.refreshContext() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_reading_assistant, menu)
        for (i in 0 until menu.size()) {
            MenuUtil.tintIcons(this, menu.getItem(i).icon, R.attr.toolbarTitleAccents)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_assistant_view_context -> {
                showContextPreview()
                true
            }
            R.id.menu_assistant_clear_history -> {
                confirmClearHistory()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupSuggestionChips() {
        suggestionChips.removeAllViews()
        val prompts = listOf(
            R.string.llm_assistant_chip_summarize,
            R.string.llm_assistant_chip_characters,
            R.string.llm_assistant_chip_explain,
            R.string.llm_assistant_chip_unresolved
        )
        for (resId in prompts) {
            val chip = Chip(this).apply {
                text = getString(resId)
                isCheckable = false
                isClickable = true
                setOnClickListener {
                    if (isLoadingContext || isGenerating) return@setOnClickListener
                    input.setText(getString(resId))
                    sendCurrentInput()
                }
            }
            suggestionChips.addView(chip)
        }
    }

    private fun sendCurrentInput() {
        val question = input.text?.toString().orEmpty().trim()
        if (question.isEmpty() || isLoadingContext || isGenerating) return
        input.setText("")
        viewModel.startQuestionProcess(question)
        ensureModelThen(
            action = {
                viewModel.ask(question, preAsked = true)
            },
            onCancel = {
                viewModel.cancelQuestionProcess()
            }
        )
    }

    private fun confirmClearHistory() {
        MaterialAlertDialogBuilder(this, R.style.AppCompatAlertDialogStyle)
            .setTitle(R.string.llm_assistant_clear_history_title)
            .setMessage(R.string.llm_assistant_clear_history_message)
            .setPositiveButton(R.string.action_positive) { _, _ ->
                viewModel.clearHistory()
            }
            .setNegativeButton(R.string.action_negative, null)
            .show()
    }

    private fun showContextPreview() {
        val preview = viewModel.peekContextPreview()
        if (preview.isBlank()) {
            Toast.makeText(this, R.string.llm_assistant_context_empty, Toast.LENGTH_SHORT).show()
            return
        }
        val padding = (20 * resources.displayMetrics.density).toInt()
        val textView = TextView(this).apply {
            text = preview
            setPadding(padding, padding, padding, padding)
            TextViewCompat.setTextAppearance(this, com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
        }
        val scroll = ScrollView(this).apply {
            addView(textView)
            setPadding(0, 0, 0, padding)
        }
        MaterialAlertDialogBuilder(this, R.style.AppCompatAlertDialogStyle)
            .setTitle(R.string.llm_assistant_view_context)
            .setView(scroll)
            .setPositiveButton(R.string.action_neutral, null)
            .show()
    }

    private fun bindSession() {
        val holder = AssistantSessionHolder
        viewModel.configure(
            type = holder.type,
            title = holder.title,
            page = holder.page,
            referenceId = holder.bookId ?: holder.mangaId,
            bookParse = holder.bookParse,
            mangaParse = holder.mangaParse,
            ocrLanguage = holder.ocrLanguage,
            bookLanguage = holder.bookLanguage,
            preloadSummary = holder.preloadSummary
        )
        supportActionBar?.subtitle = holder.title
        contextSelectorLayout.hint = getString(
            if (viewModel.isBookContext()) R.string.llm_assistant_select_chapters
            else R.string.llm_assistant_select_pages
        )
    }

    private fun observe() {
        viewModel.messages.observe(this) { list ->
            adapter.submit(list)
            emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            if (list.isNotEmpty()) {
                messagesList.post {
                    messagesList.scrollToPosition(list.lastIndex)
                }
            }
        }
        viewModel.contextSource.observe(this) { source ->
            updateContextLabel(source)
        }
        viewModel.contextSummary.observe(this) { summary ->
            contextSelector.setText(summary.orEmpty(), false)
            updateContextLabel(viewModel.contextSource.value ?: ContextSource.EMPTY)
        }
        viewModel.selectionLimitExceeded.observe(this) { limit ->
            if (limit != null) {
                Toast.makeText(
                    this,
                    getString(R.string.llm_assistant_selection_limit, limit),
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.consumeSelectionLimitExceeded()
            }
        }
        viewModel.loading.observe(this) { loadingNow ->
            isLoadingContext = loadingNow
            loading.visibility = if (loadingNow) View.VISIBLE else View.GONE
            updateInteractionEnabled()
        }
        viewModel.generating.observe(this) { generating ->
            isGenerating = generating
            cancelButton.visibility = if (generating) View.VISIBLE else View.GONE
            updateInteractionEnabled()
        }
    }

    private fun updateInteractionEnabled() {
        val enabled = !isLoadingContext && !isGenerating
        sendButton.isEnabled = enabled
        for (i in 0 until suggestionChips.childCount) {
            suggestionChips.getChildAt(i).isEnabled = enabled
        }
    }

    private fun updateContextLabel(source: ContextSource) {
        val sourceText = when (source) {
            ContextSource.SUBTITLES -> getString(R.string.llm_assistant_context_subtitles)
            ContextSource.OCR -> getString(R.string.llm_assistant_context_ocr)
            ContextSource.BOOK_TEXT -> getString(R.string.llm_assistant_context_book)
            ContextSource.EMPTY -> getString(R.string.llm_assistant_context_empty)
        }
        val summary = viewModel.contextSummary.value.orEmpty()
        contextLabel.text = if (summary.isBlank() || source == ContextSource.EMPTY) {
            sourceText
        } else {
            getString(R.string.llm_assistant_context_with_selection, sourceText, summary)
        }
    }

    private fun updatePrivacyLabel() {
        privacyLabel.setText(
            when (LlmSettings.effectiveProvider(this)) {
                LlmProvider.OPENROUTER -> R.string.llm_assistant_privacy_openrouter
                else -> R.string.llm_assistant_privacy
            }
        )
    }

    private fun showContextPicker() {
        val options = viewModel.availableOptions.value.orEmpty()
        val selected = viewModel.selectedIndices.value.orEmpty()
        val type = if (viewModel.isBookContext()) Type.BOOK else Type.MANGA
        AssistantSelectionHelper.showMultiChoice(
            context = this,
            type = type,
            options = options,
            selectedIndices = selected,
            maxSelection = viewModel.maxSelection(),
            onConfirmed = { indices -> viewModel.applySelection(indices) }
        )
    }

    private fun ensureModelThen(action: () -> Unit) {
        LlmModelGate.ensureReady(this, scope, onReady = action)
    }

    private fun ensureModelThen(action: () -> Unit, onCancel: (() -> Unit)?) {
        LlmModelGate.ensureReady(this, scope, onReady = action, onCancel = onCancel)
    }

    override fun onDestroy() {
        scope.cancel()
        if (isFinishing) {
            AssistantSessionHolder.preloadSummary = null
        }
        super.onDestroy()
    }

    companion object {
        fun prepareBook(
            title: String,
            page: Int,
            chapter: Int,
            bookId: Long?,
            parse: br.com.fenix.bilingualreader.service.parses.book.DocumentParse?,
            language: br.com.fenix.bilingualreader.model.enums.Languages?,
            preloadSummary: String? = null
        ) {
            AssistantSessionHolder.clear()
            AssistantSessionHolder.type = Type.BOOK
            AssistantSessionHolder.title = title
            AssistantSessionHolder.page = page
            AssistantSessionHolder.chapter = chapter
            AssistantSessionHolder.bookId = bookId
            AssistantSessionHolder.bookParse = parse
            AssistantSessionHolder.bookLanguage = language
            AssistantSessionHolder.preloadSummary = preloadSummary
        }

        fun prepareManga(
            title: String,
            page: Int,
            mangaId: Long?,
            parse: br.com.fenix.bilingualreader.service.parses.manga.Parse?,
            ocrLanguage: br.com.fenix.bilingualreader.model.enums.Languages?,
            preloadSummary: String? = null
        ) {
            AssistantSessionHolder.clear()
            AssistantSessionHolder.type = Type.MANGA
            AssistantSessionHolder.title = title
            AssistantSessionHolder.page = page
            AssistantSessionHolder.mangaId = mangaId
            AssistantSessionHolder.mangaParse = parse
            AssistantSessionHolder.ocrLanguage = ocrLanguage
            AssistantSessionHolder.preloadSummary = preloadSummary
        }
    }
}
