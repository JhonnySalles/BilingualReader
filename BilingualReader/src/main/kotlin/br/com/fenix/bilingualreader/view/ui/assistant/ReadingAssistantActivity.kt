package br.com.fenix.bilingualreader.view.ui.assistant

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.llm.AssistantSessionHolder
import br.com.fenix.bilingualreader.service.llm.ContextSource
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil
import br.com.fenix.bilingualreader.util.helpers.UserLanguageHelper
import br.com.fenix.bilingualreader.view.adapter.assistant.AssistantMessageAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

class ReadingAssistantActivity : AppCompatActivity() {

    private lateinit var viewModel: ReadingAssistantViewModel
    private val adapter = AssistantMessageAdapter()
    private val scope = MainScope()

    private lateinit var contextLabel: TextView
    private lateinit var input: TextInputEditText
    private lateinit var sendButton: MaterialButton
    private lateinit var cancelButton: MaterialButton
    private lateinit var loading: ProgressBar

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
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        MenuUtil.tintToolbar(toolbar, theme)

        contextLabel = findViewById(R.id.assistant_context_label)
        input = findViewById(R.id.assistant_input)
        sendButton = findViewById(R.id.assistant_send)
        cancelButton = findViewById(R.id.assistant_cancel)
        loading = findViewById(R.id.assistant_loading)

        val messages = findViewById<RecyclerView>(R.id.assistant_messages)
        messages.layoutManager = LinearLayoutManager(this)
        messages.adapter = adapter

        findViewById<MaterialButton>(R.id.assistant_refresh_context).setOnClickListener {
            ensureModelThen { viewModel.refreshContext() }
        }
        sendButton.setOnClickListener {
            val question = input.text?.toString().orEmpty().trim()
            if (question.isEmpty()) return@setOnClickListener
            ensureModelThen {
                viewModel.ask(question)
                input.setText("")
            }
        }
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
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_assistant_clear_history -> {
                confirmClearHistory()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
    }

    private fun observe() {
        viewModel.messages.observe(this) { adapter.submit(it) }
        viewModel.contextSource.observe(this) { source ->
            contextLabel.text = when (source) {
                ContextSource.SUBTITLES -> getString(R.string.llm_assistant_context_subtitles)
                ContextSource.OCR -> getString(R.string.llm_assistant_context_ocr)
                ContextSource.BOOK_TEXT -> getString(R.string.llm_assistant_context_book)
                ContextSource.EMPTY -> getString(R.string.llm_assistant_context_empty)
            }
        }
        viewModel.loading.observe(this) { loading.visibility = if (it) View.VISIBLE else View.GONE }
        viewModel.generating.observe(this) { generating ->
            cancelButton.visibility = if (generating) View.VISIBLE else View.GONE
            sendButton.isEnabled = !generating
        }
    }

    private fun ensureModelThen(action: () -> Unit) {
        LlmModelGate.ensureReady(this, scope, onReady = action)
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
            ocrLanguage: br.com.fenix.bilingualreader.model.enums.Languages?
        ) {
            AssistantSessionHolder.clear()
            AssistantSessionHolder.type = Type.MANGA
            AssistantSessionHolder.title = title
            AssistantSessionHolder.page = page
            AssistantSessionHolder.mangaId = mangaId
            AssistantSessionHolder.mangaParse = parse
            AssistantSessionHolder.ocrLanguage = ocrLanguage
        }
    }
}
