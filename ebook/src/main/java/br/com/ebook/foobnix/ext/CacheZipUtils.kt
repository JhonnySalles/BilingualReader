package br.com.ebook.foobnix.ext

import android.content.Context
import androidx.core.util.Pair
import br.com.ebook.util.IOUtils
import br.com.ebook.util.IOUtils.copyTo
import org.ebookdroid.BookType
import org.slf4j.LoggerFactory
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object CacheZipUtils {
    private val LOGGER = LoggerFactory.getLogger(CacheZipUtils::class.java)

    enum class CacheDir(val type: String) {
        ZipApp("ZipApp"),
        ZipService("ZipService");

        fun removeCacheContent() {
            try {
                removeFiles(getDir().listFiles())
            } catch (e: Exception) {
                LOGGER.error("Error to remove cache: {}", e.message, e)
            }
        }

        fun getDir(): File {
            val p = parent ?: throw IllegalStateException("Parent cache directory is not initialized")
            return File(p, type)
        }

        companion object {
            @JvmField
            var parent: File? = null

            @JvmStatic
            fun createCacheDirs() {
                val p = parent ?: return
                values().forEach { folder ->
                    val root = File(p, folder.type)
                    if (!root.exists()) {
                        root.mkdirs()
                    }
                }
            }
        }
    }

    @JvmField
    var CACHE_UN_ZIP_DIR: File? = null
    
    @JvmField
    var CACHE_BOOK_DIR: File? = null
    
    @JvmField
    var CACHE_WEB: File? = null
    
    @JvmField
    var ATTACHMENTS_CACHE_DIR: File? = null
    
    @JvmField
    val cacheLock = ReentrantLock()

    @JvmStatic
    fun init(c: Context, dir: File) {
        CACHE_BOOK_DIR = File(dir, "Book")
        CACHE_UN_ZIP_DIR = File(dir, "UnZip")
        ATTACHMENTS_CACHE_DIR = File(dir, "Attachments")
        CACHE_WEB = File(dir, "Web")

        CacheDir.parent = dir

        createAllCacheDirs()
        CacheDir.createCacheDirs()
    }

    @JvmStatic
    fun createAllCacheDirs() {
        CACHE_BOOK_DIR?.let { if (!it.exists()) it.mkdirs() }
        ATTACHMENTS_CACHE_DIR?.let { if (!it.exists()) it.mkdirs() }
        CACHE_WEB?.let { if (!it.exists()) it.mkdirs() }
    }

    @JvmStatic
    fun removeFiles(files: Array<File>?) {
        try {
            files?.forEach { file ->
                file.delete()
            }
        } catch (e: Exception) {
            LOGGER.error("Error to remove files: {}", e.message, e)
        }
    }

    @JvmStatic
    fun removeFiles(files: Array<File>?, exept: File?) {
        try {
            if (files == null || exept == null) return
            files.forEach { file ->
                if (!file.name.startsWith(exept.name)) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error to remove files: {}", e.message, e)
        }
    }

    @JvmStatic
    fun isSingleAndSupportEntryFile(file: File): Pair<Boolean, String> {
        return try {
            isSingleAndSupportEntry(file)
        } catch (e: Exception) {
            Pair(false, "")
        }
    }

    @JvmStatic
    fun isSingleAndSupportEntry(file: File?): Pair<Boolean, String> {
        if (file == null) return Pair(false, "")
        var name = ""
        try {
            ZipFile(file, StandardCharsets.UTF_8).use { zipFile ->
                var find = false
                val entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    name = entry.name
                    if (find) {
                        return Pair(false, "")
                    }
                    find = true
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error to validate if zip has single entry: {}", e.message, e)
        }
        return Pair(BookType.isSupportedExtByPath(name), name)
    }

    class UnZipRes(
        @JvmField val originalPath: String, 
        @JvmField val unZipPath: String?, 
        @JvmField val entryName: String?
    )

    @JvmStatic
    fun extracIfNeed(path: String, folder: CacheDir): UnZipRes {
        if (!path.endsWith(".zip")) {
            return UnZipRes(path, path, null)
        }

        folder.removeCacheContent()

        try {
            val file = File(path)
            if (isSingleAndSupportEntry(file).first == false) {
                return UnZipRes(path, path, null)
            }

            ZipFile(file, StandardCharsets.UTF_8).use { zipFile ->
                val entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (BookType.isSupportedExtByPath(entry.name)) {
                        val out = File(folder.getDir(), entry.name)
                        BufferedOutputStream(FileOutputStream(out)).use { fos ->
                            zipFile.getInputStream(entry).use { it.copyTo(fos) }
                        }
                        LOGGER.info("Unpack archive: {}", file.path)
                        return UnZipRes(path, file.path, entry.name)
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error to extract zip cache: {}", e.message, e)
        }
        return UnZipRes(path, path, null)
    }

    @JvmStatic
    fun writeToStream(zipInputStream: InputStream, out: OutputStream) {
        try {
            zipInputStream.use { input ->
                out.use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error writing to stream: {}", e.message, e)
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun zipFolder(srcFolder: String, destZipFile: String) {
        FileOutputStream(destZipFile).use { fileWriter ->
            ZipOutputStream(fileWriter).use { zip ->
                zip.setLevel(0)
                addFolderToZip("", srcFolder, zip)
                zip.flush()
            }
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    private fun addFileToZip(path: String, srcFile: String, zip: ZipOutputStream) {
        val file = File(srcFile)
        if (file.isDirectory) {
            addFolderToZip(path, srcFile, zip)
        } else {
            val buf = ByteArray(IOUtils.BUFFER_SIZE)
            var len: Int
            FileInputStream(srcFile).use { fis ->
                zip.putNextEntry(ZipEntry(path + "/" + file.name))
                while (fis.read(buf).also { len = it } > 0) {
                    zip.write(buf, 0, len)
                }
            }
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    private fun addFolderToZip(path: String, srcFolder: String, zip: ZipOutputStream) {
        val folder = File(srcFolder)
        folder.list()?.forEach { fileName ->
            val nextPath = if (path.isEmpty()) folder.name else "$path/${folder.name}"
            addFileToZip(nextPath, "$srcFolder/$fileName", zip)
        }
    }

    @JvmStatic
    fun deleteDir(file: File) {
        file.listFiles()?.forEach { f ->
            deleteDir(f)
        }
        file.delete()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copyFile(source: File, dest: File) {
        IOUtils.copyFile(source, dest)
    }
}
