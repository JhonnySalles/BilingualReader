package br.com.fenix.bilingualreader.service.parses.manga

import br.com.fenix.bilingualreader.model.entity.ComicInfo
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.core.Persister
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

class DirectoryParse : Parse {

    private val mLOGGER = LoggerFactory.getLogger(DirectoryParse::class.java)

    private val mFiles = ArrayList<File>()
    private val mSubtitles = ArrayList<File>()
    private var mComicInfo: File? = null

    override fun parse(file: File?) {
        if (file == null)
            throw IOException("Not file informed.")

        if (!file.isDirectory)
            throw IOException("Not a directory: " + file.absolutePath)

        if (file.listFiles() != null) {
            for (f in file.listFiles()) {
                if (f.isDirectory)
                    throw IOException("Probably not a comic directory")

                if (FileUtil.isImage(f.absolutePath))
                    mFiles.add(f)
                else if (FileUtil.isJson(f.absolutePath))
                    mSubtitles.add(f)
                else if (FileUtil.isXml(f.absolutePath) && f.name.contains("comicinfo", true))
                    mComicInfo = f
            }
        }
        mFiles.sortBy { it.name }
    }

    override fun numPages(): Int {
        return mFiles.size
    }

    override fun getSubtitles(): List<String> {
        val subtitles = arrayListOf<String>()
        mSubtitles.forEach {
            val sub = FileInputStream(it)

            val reader = BufferedReader(sub.reader())
            val content = StringBuilder()
            reader.use { rd ->
                var line = rd.readLine()
                while (line != null) {
                    content.append(line)
                    line = rd.readLine()
                }
            }
            subtitles.add(content.toString())
        }
        return subtitles
    }

    override fun hasSubtitles(): Boolean {
        return mSubtitles.isNotEmpty()
    }

    override fun getSubtitlesNames(): Map<String, Int> {
        val paths = mutableMapOf<String, Int>()

        for ((index, header) in mSubtitles.withIndex()) {
            val path = Util.getNameFromPath(getName(header))
            if (path.isNotEmpty() && !paths.containsKey(path))
                paths[path] = index
        }

        return paths
    }

    private fun getName(file: File): String {
        return file.name
    }

    override fun getPagePath(num: Int): String? {
        if (mFiles.size < num)
            return null
        return getName(mFiles[num])
    }

    override fun getPagePaths(): Map<String, Int> {
        val paths = mutableMapOf<String, Int>()

        for ((index, header) in mFiles.withIndex()) {
            val path = Util.getFolderFromPath(getName(header))
            if (path.isNotEmpty() && !paths.containsKey(path))
                paths[path] = index
        }

        return paths
    }

    override fun getChapters(): IntArray {
        return getPagePaths().filter { it.value != 0 }.map { it.value }.toIntArray()
    }

    override fun isComicInfo(): Boolean = mComicInfo != null

    override fun getComicInfo(): ComicInfo? {
        return if (isComicInfo()) {
            val page = FileInputStream(mComicInfo!!)
            val serializer: Serializer = Persister()
            try {
                serializer.read(ComicInfo::class.java, page)
            } catch (e: Exception) {
                mLOGGER.error("Error to get comic info: " + e.message, e)
                Firebase.crashlytics.apply {
                    setCustomKey("message", "Error to get comic info: " + e.message)
                    recordException(e)
                }
                null
            }
        } else
            null
    }

    override fun getPage(num: Int): InputStream {
        return FileInputStream(mFiles[num])
    }

    override fun destroy(isClearCache: Boolean) {
    }
}