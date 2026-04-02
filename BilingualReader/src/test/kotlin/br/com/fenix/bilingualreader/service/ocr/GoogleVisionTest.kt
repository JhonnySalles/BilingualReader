package br.com.fenix.bilingualreader.service.ocr

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.mockk.*
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GoogleVisionTest {

    private lateinit var context: Context
    private lateinit var recognizer: TextRecognizer

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = mockk(relaxed = true)
        recognizer = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun process_callsSetTextOnSuccess() {
        // Mock InputImage and TextRecognition inside the test
        mockkStatic(InputImage::class)
        every { InputImage.fromBitmap(any<Bitmap>(), any()) } returns mockk(relaxed = true)
        
        mockkStatic(TextRecognition::class)
        every { TextRecognition.getClient(any<TextRecognizerOptions>()) } returns recognizer

        // Mock Firebase correctly
        mockkStatic(FirebaseCrashlytics::class)
        val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { FirebaseCrashlytics.getInstance() } returns crashlytics
        mockkObject(Firebase)
        every { Firebase.crashlytics } returns crashlytics
        val googleVision = GoogleVision(context)
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        val visionText = mockk<Text>(relaxed = true)
        val textBlock = mockk<Text.TextBlock>(relaxed = true)
        every { visionText.textBlocks } returns listOf(textBlock)
        every { textBlock.text } returns "Hello"

        val task = mockk<Task<Text>>(relaxed = true)
        every { recognizer.process(any<InputImage>()) } returns task
        
        val successListener = slot<OnSuccessListener<Text>>()
        every { task.addOnSuccessListener(capture(successListener)) } answers {
            successListener.captured.onSuccess(visionText)
            task
        }

        var result: ArrayList<String>? = null
        val latch = CountDownLatch(1)
        
        googleVision.process(bitmap) { 
            result = it 
            latch.countDown()
        }

        latch.await(5, TimeUnit.SECONDS)
        assertNotNull(result)
        assertEquals("Hello", result?.get(0))
    }
}
