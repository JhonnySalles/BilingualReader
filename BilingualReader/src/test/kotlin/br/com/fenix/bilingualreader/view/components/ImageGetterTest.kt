package br.com.fenix.bilingualreader.view.components

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.util.DisplayMetrics
import android.widget.TextView
import br.com.fenix.bilingualreader.util.helpers.ImageUtil
import br.com.fenix.bilingualreader.util.helpers.TextUtil
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ImageGetterTest {

    private lateinit var context: Context
    private lateinit var textView: TextView
    private lateinit var resources: Resources
    private lateinit var displayMetrics: DisplayMetrics
    private lateinit var configuration: Configuration

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        textView = mockk(relaxed = true)
        resources = mockk(relaxed = true)
        displayMetrics = DisplayMetrics()
        displayMetrics.widthPixels = 1000
        displayMetrics.heightPixels = 2000
        configuration = Configuration()
        configuration.orientation = Configuration.ORIENTATION_PORTRAIT

        every { context.resources } returns resources
        every { resources.displayMetrics } returns displayMetrics
        every { resources.configuration } returns configuration

        mockkObject(TextUtil)
        mockkObject(ImageUtil)
        mockkStatic("android.graphics.Bitmap")
        mockkObject(Firebase)
        
        mockkStatic("com.google.firebase.crashlytics.ktx.FirebaseCrashlyticsKt")
        
        val mockCrashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { Firebase.crashlytics } returns mockCrashlytics
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getDrawable returns drawable even when string is empty`() {
        val imageGetter = ImageGetter(context, textView)
        val drawable = imageGetter.getDrawable("")
        assertNotNull(drawable)
    }

    @Test
    fun `getDrawable decodes image and scales it if too wide`() {
        val base64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg=="
        val tag = "<img>something,$base64</img>"
        
        every { TextUtil.getImageFromTag(any()) } returns tag
        
        val bitmap: Bitmap = mockk(relaxed = true)
        every { bitmap.width } returns 2000
        every { bitmap.height } returns 1000
        
        every { ImageUtil.decodeImageBase64(any()) } returns bitmap
        
        val scaledBitmap: Bitmap = mockk(relaxed = true)
        every { scaledBitmap.width } returns 1000
        every { scaledBitmap.height } returns 500
        
        every { Bitmap.createScaledBitmap(any<Bitmap>(), any<Int>(), any<Int>(), any<Boolean>()) } returns scaledBitmap

        val imageGetter = ImageGetter(context, textView)
        val drawable = imageGetter.getDrawable(tag)

        assertNotNull(drawable)
        verify { ImageUtil.decodeImageBase64(any()) }
    }

    @Test
    fun `getDrawable handles exceptions gracefully and returns default drawable`() {
        every { TextUtil.getImageFromTag(any()) } throws RuntimeException("Test Failure")
        
        val imageGetter = ImageGetter(context, textView)
        val drawable = imageGetter.getDrawable("invalid")
        
        assertNotNull(drawable)
    }
}
