package br.com.fenix.bilingualreader.service.ocr

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import br.com.fenix.bilingualreader.model.enums.Languages

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class GoogleVisionTest {

    private lateinit var googleVision: GoogleVision
    private lateinit var recognizer: TextRecognizer
    private lateinit var task: Task<Text>

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        googleVision = GoogleVision(context)
        recognizer = mockk(relaxed = true)
        task = mockk(relaxed = true)
        mockkStatic(TextRecognition::class)
        mockkStatic(InputImage::class)
        every { TextRecognition.getClient(any()) } returns recognizer
        every { InputImage.fromBitmap(any<Bitmap>(), any<Int>()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() { unmockkAll() }

    @Test
    fun testOcrFlow() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val visionText = mockkClass(Text::class, relaxed = true)
        
        every { recognizer.process(any<InputImage>()) } returns task
        every { task.addOnSuccessListener(any()) } answers {
            val listener = it.invocation.args[0] as OnSuccessListener<Text>
            listener.onSuccess(visionText)
            task
        }
        every { task.addOnFailureListener(any()) } returns task

        var result: ArrayList<String>? = null
        googleVision.process(bitmap) {
            result = it
        }

        assertNotNull(result)
    }
}
