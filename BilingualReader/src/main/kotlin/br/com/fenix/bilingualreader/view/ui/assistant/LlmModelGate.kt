package br.com.fenix.bilingualreader.view.ui.assistant

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.service.llm.LlmInferenceEngine
import br.com.fenix.bilingualreader.service.llm.LlmModelManager
import br.com.fenix.bilingualreader.service.llm.ModelPrepareState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object LlmModelGate {

    fun ensureReady(
        context: Context,
        scope: CoroutineScope,
        onReady: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        val manager = LlmModelManager.getInstance(context)
        if (manager.isModelReady()) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        LlmInferenceEngine.getInstance(context).ensureLoaded(manager.getModelFile().absolutePath)
                    }
                    onReady()
                } catch (e: Exception) {
                    showError(context, e.message ?: context.getString(R.string.llm_error_load))
                    onCancel?.invoke()
                }
            }
            return
        }

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_llm_download, null)
        val progress = view.findViewById<ProgressBar>(R.id.llm_download_progress)
        val status = view.findViewById<TextView>(R.id.llm_download_status)
        status.text = context.getString(R.string.llm_prepare_message)

        var job: Job? = null
        val dialog = MaterialAlertDialogBuilder(context, R.style.AppCompatAlertDialogStyle)
            .setTitle(R.string.llm_prepare_title)
            .setView(view)
            .setNegativeButton(R.string.action_cancel) { d, _ ->
                manager.cancelExtract()
                job?.cancel()
                d.dismiss()
                onCancel?.invoke()
            }
            .setCancelable(false)
            .create()

        dialog.show()

        job = scope.launch {
            launch {
                manager.state.collectLatest { state ->
                    when (state) {
                        is ModelPrepareState.Extracting -> {
                            progress.isIndeterminate = state.totalBytes <= 0
                            progress.progress = state.progress
                            status.text = context.getString(R.string.llm_prepare_progress, state.progress)
                        }
                        is ModelPrepareState.Error -> {
                            status.text = state.message
                        }
                        else -> Unit
                    }
                }
            }

            try {
                val file = withContext(Dispatchers.IO) { manager.ensureModel() }
                withContext(Dispatchers.IO) {
                    LlmInferenceEngine.getInstance(context).ensureLoaded(file.absolutePath)
                }
                dialog.dismiss()
                onReady()
            } catch (e: Exception) {
                if (e is InterruptedException) {
                    dialog.dismiss()
                    onCancel?.invoke()
                } else {
                    status.text = e.message ?: context.getString(R.string.llm_prepare_error)
                    dialog.getButton(Dialog.BUTTON_NEGATIVE)?.setText(R.string.action_neutral)
                }
            }
        }
    }

    private fun showError(context: Context, message: String) {
        MaterialAlertDialogBuilder(context, R.style.AppCompatAlertDialogStyle)
            .setTitle(R.string.alert_title)
            .setMessage(message)
            .setPositiveButton(R.string.action_neutral, null)
            .show()
    }
}
