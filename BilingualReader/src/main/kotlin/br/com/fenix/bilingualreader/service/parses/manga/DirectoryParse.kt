package br.com.fenix.bilingualreader.service.parses.manga

import br.com.fenix.bilingualreader.model.entity.ComicInfo
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import br.com.fenix.bilingualreader.util.helpers.Util
import com.github.junrar.rarfile.FileHeader
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
    private var mCover: Array<File?> = arrayOfNulls<File?>(3)

    override fun parse(file: File?) {
        if (file == null)
            throw IOException("Not file informed.")

        if (!file.isDirectory)
            throw IOException("Not a directory: " + file.absolutePath)

        if (file.listFiles() != null) {
            for (f in file.listFiles()) {
                if (f.isDirectory)
                    throw IOException("Probably not a comic directory")

                if (FileUtil.isImage(f.absolutePath)) {
                    mFiles.add(f)
                    if (f.name.contains("volume", true)) {
                        if (f.name.contains("frente", ignoreCase = false) || f.name.contains("cover", ignoreCase = false) || f.name.contains("front", ignoreCase = false))
                            mCover[0] = f
                        else if (f.name.contains("tras", ignoreCase = false) || f.name.contains("back", ignoreCase = false))
                            mCover[1] = f
                        else if (f.name.contains("tudo", true) || f.name.contains("all", ignoreCase = false) || f.name.contains("everything", ignoreCase = false))
                            mCover[2] = f
                    }
                } else if (FileUtil.isJson(f.absolutePath))
                    mSubtitles.add(f)
                else if (FileUtil.isXml(f.absolutePath) && f.name.contains("comicinfo", true))
                    mComicInfo = f
            }
        }
        mFiles.sortBy { it.name }

        if (mCover[0] == null)
            mCover[0] = mFiles[0]
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
            FileInputStream(mComicInfo!!).use { page ->
                val serializer: Serializer = Persister()
                try {
                    serializer.read(ComicInfo::class.java, page)
                } catch (e: Exception) {
                    mLOGGER.error("Error to get comic info: " + e.message, e)
                    Telemetry.recordException(e, "Error to get comic info: " + e.message)
                    null
                }
            }
        } else
            null
    }

    override fun getPage(num: Int): InputStream {
        return FileInputStream(mFiles[num])
    }

    override fun hasFullCover(): Boolean = mCover[2] != null

    override fun getFullCover(): InputStream? = if (hasFullCover()) FileInputStream(mCover[2]!!) else null

    override fun getCover(): Pair<InputStream?, InputStream?> = Pair(if (mCover[0] != null) FileInputStream(mCover[0]!!) else getPage(0), if (mCover[1] != null) FileInputStream(mCover[1]!!) else null)

    override fun destroy(isClearCache: Boolean) {
    }
}