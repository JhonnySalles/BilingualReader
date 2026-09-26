package br.com.fenix.bilingualreader.service.ocr

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.service.translate.MlKitTranslator
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import br.com.fenix.bilingualreader.util.helpers.UserLanguageHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class GoogleVision(private var context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(GoogleVision::class.java)

    companion object {
        private lateinit var INSTANCE: GoogleVision

        fun getInstance(context: Context): GoogleVision {
            if (!::INSTANCE.isInitialized)
                INSTANCE = GoogleVision(context)
            return INSTANCE
        }
    }

    fun process(image: Bitmap, setText: (ArrayList<String>) -> (Unit)) {
        process(image, language = null, translate = false, setText = setText)
    }

    fun process(image: Bitmap, language: Languages?, setText: (ArrayList<String>) -> (Unit)) {
        process(image, language, translate = false, setText = setText)
    }

    fun process(
        image: Bitmap,
        language: Languages?,
        translate: Boolean,
        setText: (ArrayList<String>) -> (Unit)
    ) {
        Toast.makeText(
            context,
            context.resources.getString(
                if (translate) R.string.ocr_translate_processing
                else R.string.ocr_google_vision_get_request
            ),
            Toast.LENGTH_SHORT
        ).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = OcrFacade.getInstance().recognize(image, language)
                val texts = ArrayList<String>()
                if (!translate) {
                    texts.addAll(result.blocks.map { it.text })
                } else {
                    val userLang = UserLanguageHelper.getUserLanguage(context)
                    val translator = MlKitTranslator.getInstance(context)
                    val hint = language
                    for (block in result.blocks) {
                        val (translated, _) = translator.translateIfNeeded(block.text, hint, userLang)
                        texts.add(translated)
                    }
                    if (texts.isEmpty() && result.fullText.isNotBlank()) {
                        val (translated, _) = translator.translateIfNeeded(result.fullText, hint, userLang)
                        texts.add(translated)
                    }
                }

                withContext(Dispatchers.Main) {
                    if (texts.isEmpty()) {
                        Toast.makeText(
                            context,
                            context.resources.getString(R.string.ocr_google_vision_not_detected),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        if (translate) {
                            Toast.makeText(
                                context,
                                context.resources.getString(R.string.ocr_translate_done),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        setText(texts)
                    }
                }
            } catch (e: Exception) {
                mLOGGER.error("Error to process google vision ocr: " + e.message, e)
                Telemetry.recordException(e, "Error to process google vision ocr: " + e.message)
                withContext(Dispatchers.Main) {
                    val msg = if (!e.message.isNullOrEmpty()) e.message
                    else context.getString(R.string.ocr_google_vision_error)

                    MaterialAlertDialogBuilder(context, R.style.AppCompatAlertDialogStyle)
                        .setTitle(context.getString(R.string.alert_title))
                        .setMessage(msg)
                        .setPositiveButton(R.string.action_neutral) { _, _ -> }
                        .create()
                        .show()
                }
            }
        }
    }
}
