package br.com.fenix.bilingualreader.service.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.widget.Toast
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
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
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GoogleVisionTest {

    private lateinit var context: Context
    private lateinit var googleVision: GoogleVision
    private lateinit var recognizer: TextRecognizer
    private lateinit var task: Task<Text>

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        googleVision = GoogleVision.getInstance(context)
        
        recognizer = mockk(relaxed = true)
        task = mockk(relaxed = true)

        mockkStatic(TextRecognition::class)
        every { TextRecognition.getClient(any()) } returns recognizer
        
        mockkStatic(InputImage::class)
        every { InputImage.fromBitmap(any<Bitmap>(), any<Int>()) } returns mockk(relaxed = true)

        mockkStatic(FirebaseCrashlytics::class)
        mockkStatic("com.google.firebase.crashlytics.ktx.FirebaseCrashlyticsKt")
        every { Firebase.crashlytics } returns mockk<FirebaseCrashlytics>(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testProcessSuccess() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val visionText = mockk<Text>()
        val block1 = mockk<Text.TextBlock>()
        
        every { visionText.textBlocks } returns listOf(block1)
        every { block1.text } returns "Hello World"

        // Setup Task to trigger SuccessListener immediately
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
        assertEquals(1, result!!.size)
        assertEquals("Hello World", result!![0])
        
        verify { TextRecognition.getClient(any()) }
        verify { recognizer.process(any<InputImage>()) }
    }

    @Test
    fun testProcessFailure() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val exception = Exception("OCR Error")

        // Mock MaterialAlertDialogBuilder to avoid crash
        mockkConstructor(MaterialAlertDialogBuilder::class)
        every { anyConstructed<MaterialAlertDialogBuilder>().setTitle(any<String>()) } returns mockk(relaxed = true)
        every { anyConstructed<MaterialAlertDialogBuilder>().setMessage(any<String>()) } returns mockk(relaxed = true)
        every { anyConstructed<MaterialAlertDialogBuilder>().setPositiveButton(any<Int>(), any()) } returns mockk(relaxed = true)
        every { anyConstructed<MaterialAlertDialogBuilder>().create() } returns mockk(relaxed = true)

        every { recognizer.process(any<InputImage>()) } returns task
        every { task.addOnSuccessListener(any()) } returns task
        every { task.addOnFailureListener(any()) } answers {
            val listener = it.invocation.args[0] as OnFailureListener
            listener.onFailure(exception)
            task
        }

        var result: ArrayList<String>? = null
        googleVision.process(bitmap) {
            result = it
        }

        assertNull("Result should be null on failure since setter is not called", result)
        verify { Firebase.crashlytics.recordException(exception) }
    }
}
