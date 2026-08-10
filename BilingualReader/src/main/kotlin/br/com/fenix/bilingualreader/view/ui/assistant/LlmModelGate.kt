package br.com.fenix.bilingualreader.view.ui.assistant

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.LlmProvider
import br.com.fenix.bilingualreader.service.llm.LlmBackendFactory
import br.com.fenix.bilingualreader.service.llm.LlmInferenceEngine
import br.com.fenix.bilingualreader.service.llm.LlmModelManager
import br.com.fenix.bilingualreader.service.llm.LlmUnsupportedDeviceException
import br.com.fenix.bilingualreader.service.llm.ModelPrepareState
import br.com.fenix.bilingualreader.service.llm.OnDeviceLlmBackend
import br.com.fenix.bilingualreader.util.helpers.LlmSettings
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
        LlmBackendFactory.requireReadyOrMessage(context)?.let { message ->
            showError(context, message)
            onCancel?.invoke()
            return
        }

        val provider = LlmSettings.effectiveProvider(context)
        if (provider == LlmProvider.OPENROUTER) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        LlmBackendFactory.resolve(context).ensureReady()
                    }
                    onReady()
                } catch (e: Throwable) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    showError(context, resolveErrorMessage(context, e))
                    onCancel?.invoke()
                }
            }
            return
        }

        // On-device path (explicit or AUTO resolved to on-device)
        val manager = LlmModelManager.getInstance(context)
        if (manager.isModelReady()) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        OnDeviceLlmBackend(context).ensureReady()
                    }
                    onReady()
                } catch (e: Throwable) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    showError(context, resolveErrorMessage(context, e))
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
                withContext(Dispatchers.IO) {
                    OnDeviceLlmBackend(context).ensureReady()
                }
                dialog.dismiss()
                onReady()
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (e is InterruptedException) {
                    dialog.dismiss()
                    onCancel?.invoke()
                } else {
                    status.text = resolveErrorMessage(context, e)
                    dialog.getButton(Dialog.BUTTON_NEGATIVE)?.setText(R.string.action_neutral)
                }
            }
        }
    }

    private fun resolveErrorMessage(context: Context, error: Throwable): String {
        return when {
            error is LlmUnsupportedDeviceException ->
                context.getString(R.string.llm_error_unsupported_device)
            LlmInferenceEngine.isNativeLinkFailure(error) ->
                context.getString(R.string.llm_error_unsupported_device)
            LlmInferenceEngine.isModelIncompatibleFailure(error) ->
                context.getString(R.string.llm_error_model_incompatible)
            else ->
                error.message ?: context.getString(R.string.llm_error_load)
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
