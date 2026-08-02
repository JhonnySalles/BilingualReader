package br.com.fenix.bilingualreader.util.helpers

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import br.com.fenix.bilingualreader.model.enums.FileType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FileUtilTest {

    private lateinit var context: Context
    private lateinit var fileUtil: FileUtil

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        fileUtil = FileUtil(context)
        
        mockkStatic(Toast::class)
        every { Toast.makeText(any(), any<String>(), any()) } returns mockk(relaxed = true)
    }

    @Test
    fun testFileTypeChecks() {
        assertTrue(FileUtil.isXml("test.xml"))
        assertTrue(FileUtil.isXml("TEST.XML"))
        assertFalse(FileUtil.isXml("test.json"))

        assertTrue(FileUtil.isJson("test.json"))
        assertFalse(FileUtil.isJson("test.xml"))

        assertTrue(FileUtil.isImage("test.jpg"))
        assertTrue(FileUtil.isImage("test.jpeg"))
        assertTrue(FileUtil.isImage("test.png"))
        assertTrue(FileUtil.isImage("test.webp"))
        assertTrue(FileUtil.isImage("test.bmp"))
        assertTrue(FileUtil.isImage("test.gif"))
        assertTrue(FileUtil.isImage("test.avif"))
        assertTrue(FileUtil.isImage("test.heic"))
        assertTrue(FileUtil.isImage("test.jxl"))
        assertTrue(FileUtil.isImage("test.tif"))
        assertTrue(FileUtil.isImage("test.tiff"))
        assertTrue(FileUtil.isImage("test.pcx"))
        assertTrue(FileUtil.isImage("test.jpf"))
        assertTrue(FileUtil.isImage("test.pbm"))
        assertTrue(FileUtil.isImage("test.iff"))
        assertFalse(FileUtil.isImage("test.pdf"))

        assertTrue(FileUtil.isHtml("test.html"))
        assertTrue(FileUtil.isHtml("test.xhtml"))
        assertFalse(FileUtil.isHtml("test.txt"))
    }

    @Test
    fun testGetFileType() {
        assertEquals(FileType.EPUB, FileUtil.getFileType("test.epub"))
        assertEquals(FileType.CBZ, FileUtil.getFileType("test.cbz"))
        assertEquals(FileType.UNKNOWN, FileUtil.getFileType("test.unknown"))
    }

    @Test
    fun testFormatSize() {
        assertEquals("500 B", FileUtil.formatSize(500L))
        assertEquals("1.0 KB", FileUtil.formatSize(1024L))
        assertEquals("1.0 MB", FileUtil.formatSize(1024L * 1024L))
        assertEquals("1.0 GB", FileUtil.formatSize(1024L * 1024L * 1024L))
        assertEquals("1.0 TB", FileUtil.formatSize(1024L * 1024L * 1024L * 1024L))
    }

    @Test
    fun testCopyName() {
        val file = File("test.txt")
        val clipboard = mockk<ClipboardManager>(relaxed = true)
        every { context.getSystemService(Context.CLIPBOARD_SERVICE) } returns clipboard
        
        fileUtil.copyName(file)
        
        verify { clipboard.setPrimaryClip(any()) }
        verify { Toast.makeText(any(), any<String>(), any()) }
    }

    @Test
    fun testCopyFile() {
        val fromFile = mockk<FileInputStream>(relaxed = true)
        val toFile = mockk<FileOutputStream>(relaxed = true)
        val fromChannel = mockk<FileChannel>(relaxed = true)
        val toChannel = mockk<FileChannel>(relaxed = true)
        
        every { fromFile.channel } returns fromChannel
        every { toFile.channel } returns toChannel
        every { fromChannel.size() } returns 100L
        every { fromChannel.transferTo(any(), any(), any()) } returns 100L
        
        fileUtil.copyFile(fromFile, toFile)
        
        verify { fromChannel.transferTo(0, 100L, toChannel) }
        verify { fromChannel.close() }
        verify { toChannel.close() }
    }
}
