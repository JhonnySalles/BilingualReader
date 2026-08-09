package br.com.ebook.util

import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object IOUtils {
    const val BUFFER_SIZE = 16 * 1024
    private val LOGGER = LoggerFactory.getLogger(IOUtils::class.java)

    @JvmStatic
    @Throws(IOException::class)
    fun InputStream.copyTo(out: OutputStream) {
        val buffer = ByteArray(BUFFER_SIZE)
        var read: Int
        while (this.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
        }
    }

    @Throws(IOException::class)
    fun ZipFile.getEntryBytes(entryName: String): ByteArray? {
        val entry = this.getEntry(entryName) ?: return null
        return this.getInputStream(entry).use { it.readBytes() }
    }

    @Throws(IOException::class)
    fun ZipFile.getEntryBytes(entry: ZipEntry): ByteArray {
        return this.getInputStream(entry).use { it.readBytes() }
    }

    @Throws(IOException::class)
    fun copyFile(source: File, dest: File) {
        FileInputStream(source).use { srcStream ->
            FileOutputStream(dest).use { destStream ->
                srcStream.copyTo(destStream)
            }
        }
    }

    @JvmStatic
    fun reportException(e: Throwable, message: String? = null) {
        if (message != null) {
            LOGGER.error("Exception occurred: {}. Error: {}", message, e.message, e)
        } else {
            LOGGER.error("Exception occurred: {}", e.message, e)
        }
        try {
            val telemetryClass = Class.forName("br.com.fenix.bilingualreader.util.helpers.Telemetry")
            val instanceField = telemetryClass.getDeclaredField("INSTANCE")
            val instance = instanceField.get(null)
            val recordExceptionMethod = telemetryClass.getMethod("recordException", Throwable::class.java, String::class.java)
            recordExceptionMethod.invoke(instance, e, message)
        } catch (ex: Exception) {
            LOGGER.error("Failed to report exception via Telemetry: {}", ex.message)
        }
    }
}
