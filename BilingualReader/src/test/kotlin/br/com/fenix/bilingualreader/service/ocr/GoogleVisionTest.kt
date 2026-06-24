package br.com.fenix.bilingualreader.service.ocr

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import io.mockk.*
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GoogleVisionTest {

    private lateinit var context: Context
    private lateinit var recognizer: TextRecognizer

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        recognizer = mockk(relaxed = true)
        
        mockkStatic(TextRecognition::class)
        every { TextRecognition.getClient(any()) } returns recognizer
        
        mockkStatic(InputImage::class)
        every { InputImage.fromBitmap(any(), any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun process_callsSetTextOnSuccess() {
        val googleVision = GoogleVision(context)
        val bitmap = mockk<Bitmap>(relaxed = true)
        
        // Mock Vision Text Structure
        val visionText = mockk<Text>(relaxed = true)
        val textBlock = mockk<Text.TextBlock>(relaxed = true)
        every { visionText.textBlocks } returns listOf(textBlock)
        every { textBlock.text } returns "Hello World"
        
        // Mock Task
        val task = mockk<Task<Text>>(relaxed = true)
        every { recognizer.process(any<InputImage>()) } returns task
        
        // Use slot to capture and trigger listener
        val successListenerSlot = slot<OnSuccessListener<Text>>()
        every { task.addOnSuccessListener(capture(successListenerSlot)) } answers {
            successListenerSlot.captured.onSuccess(visionText)
            task
        }
        every { task.addOnFailureListener(any()) } returns task

        var capturedText: ArrayList<String>? = null
        googleVision.process(bitmap) {
            capturedText = it
        }
        
        assertNotNull("Captured text should not be null", capturedText)
        assertEquals("Should have one block of text", 1, capturedText?.size)
        assertEquals("Text content mismatch", "Hello World", capturedText?.get(0))
    }

    @Test
    fun process_handlesFailure() {
        val googleVision = GoogleVision(context)
        val bitmap = mockk<Bitmap>(relaxed = true)
        val exception = Exception("OCR Failed")
        
        val task = mockk<Task<Text>>(relaxed = true)
        every { recognizer.process(any<InputImage>()) } returns task
        
        val failureListenerSlot = slot<OnFailureListener>()
        every { task.addOnFailureListener(capture(failureListenerSlot)) } answers {
            failureListenerSlot.captured.onFailure(exception)
            task
        }

        // We don't expect setText to be called on failure
        var capturedText: ArrayList<String>? = null
        googleVision.process(bitmap) {
            capturedText = it
        }
        
        Assert.assertNull("Captured text should be null on failure", capturedText)
        // verify UI interaction can be added here if needed (e.g., AlertDialog show)
    }
}
