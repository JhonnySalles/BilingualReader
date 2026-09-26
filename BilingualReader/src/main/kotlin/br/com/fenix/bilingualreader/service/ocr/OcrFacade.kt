package br.com.fenix.bilingualreader.service.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import br.com.fenix.bilingualreader.model.enums.Languages
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import org.slf4j.LoggerFactory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OcrBlock(
    val text: String,
    val boundingBox: Rect?
)

data class OcrResult(
    val blocks: List<OcrBlock>,
    val fullText: String
)

class OcrFacade {

    private val mLOGGER = LoggerFactory.getLogger(OcrFacade::class.java)

    suspend fun recognize(bitmap: Bitmap, language: Languages?): OcrResult {
        val options = if (language == Languages.JAPANESE)
            JapaneseTextRecognizerOptions.Builder().build()
        else
            TextRecognizerOptions.DEFAULT_OPTIONS

        val recognizer = TextRecognition.getClient(options)
        val input = InputImage.fromBitmap(bitmap, 0)

        return suspendCancellableCoroutine { cont ->
            recognizer.process(input)
                .addOnSuccessListener { visionText ->
                    val blocks = visionText.textBlocks.map { block ->
                        OcrBlock(block.text, block.boundingBox)
                    }
                    if (cont.isActive) cont.resume(OcrResult(blocks, visionText.text))
                    recognizer.close()
                }
                .addOnFailureListener { e ->
                    mLOGGER.error("OCR failed: ${e.message}", e)
                    recognizer.close()
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: OcrFacade? = null

        fun getInstance(): OcrFacade {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OcrFacade().also { INSTANCE = it }
            }
        }
    }
}
