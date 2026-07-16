package br.com.ebook.util

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object IOUtils {
    const val BUFFER_SIZE = 16 * 1024

    @Throws(IOException::class)
    fun InputStream.copyTo(out: OutputStream) {
        val buffer = ByteArray(BUFFER_SIZE)
        var read: Int
        while (this.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
        }
    }

    @Throws(IOException::class)
    fun InputStream.readAllBytes(): ByteArray {
        ByteArrayOutputStream().use { out ->
            this.copyTo(out)
            return out.toByteArray()
        }
    }

    @Throws(IOException::class)
    fun ZipFile.getEntryBytes(entryName: String): ByteArray? {
        val entry = this.getEntry(entryName) ?: return null
        return this.getInputStream(entry).use { it.readAllBytes() }
    }

    @Throws(IOException::class)
    fun ZipFile.getEntryBytes(entry: ZipEntry): ByteArray {
        return this.getInputStream(entry).use { it.readAllBytes() }
    }

    @Throws(IOException::class)
    fun copyFile(source: File, dest: File) {
        FileInputStream(source).use { srcStream ->
            FileOutputStream(dest).use { destStream ->
                srcStream.copyTo(destStream)
            }
        }
    }
}
