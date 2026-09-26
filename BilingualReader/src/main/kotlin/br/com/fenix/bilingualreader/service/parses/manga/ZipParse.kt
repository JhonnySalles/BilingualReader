package br.com.fenix.bilingualreader.service.parses.manga

import br.com.fenix.bilingualreader.model.entity.ComicInfo
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import br.com.fenix.bilingualreader.util.helpers.Util
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.core.Persister
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class ZipParse : Parse {

    private val mLOGGER = LoggerFactory.getLogger(ZipParse::class.java)

    private var mZipFile: ZipFile? = null
    private var mEntries = ArrayList<ZipEntry>()
    private var mSubtitles = ArrayList<ZipEntry>()
    private var mComicInfo: ZipEntry? = null
    private var mCover: Array<ZipEntry?> = arrayOfNulls<ZipEntry?>(3)

    override fun parse(file: File?) {
        mZipFile = ZipFile(file?.absolutePath, StandardCharsets.UTF_8)
        mEntries = ArrayList()
        mSubtitles = ArrayList()
        val e = mZipFile!!.entries()
        while (e.hasMoreElements()) {
            val ze = e.nextElement()

            if (ze.isDirectory)
                continue

            if (FileUtil.isImage(ze.name)) {
                mEntries.add(ze)
                val fileName = Util.getNameFromPath(ze.name)
                if (fileName.contains("volume", true)) {
                    val cover = fileName.lowercase().substringAfterLast("volume")
                    if (cover.contains("frente", ignoreCase = true) || cover.contains("cover", ignoreCase = true) || cover.contains("front", ignoreCase = true))
                        mCover[0] = ze
                    else if (cover.contains("tras", ignoreCase = true) || cover.contains("back", ignoreCase = true))
                        mCover[1] = ze
                    else if (cover.contains("tudo", ignoreCase = true) || cover.contains("all", ignoreCase = true) || cover.contains("everything", ignoreCase = true))
                        mCover[2] = ze
                }
            } else if (FileUtil.isJson(ze.name))
                mSubtitles.add(ze)
            else if (FileUtil.isXml(ze.name) && ze.name.contains("comicinfo", true))
                mComicInfo = ze
        }

        mEntries.sortWith(compareBy<ZipEntry> { Util.getFolderFromPath(it.name) }.thenComparing { a, b ->
            Util.getNormalizedNameOrdering(a.name).compareTo(Util.getNormalizedNameOrdering(b.name))
        })

        if (mCover[0] == null)
            mCover[0] = mEntries[0]
    }

    override fun numPages(): Int {
        return mEntries.size
    }

    override fun getSubtitles(): List<String> {
        val subtitles = arrayListOf<String>()
        mSubtitles.forEach {
            val sub = mZipFile!!.getInputStream(it)

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

    private fun getName(entry: ZipEntry): String {
        return entry.name
    }

    override fun getPagePath(num: Int): String? {
        if (mEntries.size < num)
            return null
        return getName(mEntries[num])
    }

    override fun getPagePaths(): Map<String, Int> {
        val paths = mutableMapOf<String, Int>()

        for ((index, entry) in mEntries.withIndex()) {
            val path = Util.getFolderFromPath(getName(entry))
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
            val page = mZipFile!!.getInputStream(mComicInfo!!)
            val serializer: Serializer = Persister()
            try {
                serializer.read(ComicInfo::class.java, page)
            } catch (e :Exception) {
                mLOGGER.error("Error to get comic info: " + e.message, e)
                Telemetry.recordException(e, "Error to get comic info: " + e.message)
                null
            }
        } else
            null
    }

    override fun getPage(num: Int): InputStream {
        return mZipFile!!.getInputStream(mEntries[num])
    }

    override fun hasFullCover(): Boolean = mCover[2] != null

    override fun getFullCover(): InputStream? = if (hasFullCover()) mZipFile!!.getInputStream(mCover[2]!!) else null

    override fun getCover(): Pair<InputStream?, InputStream?> = Pair(if (mCover[0] != null) mZipFile!!.getInputStream(mCover[0]!!) else getPage(0), if (mCover[1] != null) mZipFile!!.getInputStream(mCover[1]!!) else null)

    override fun destroy(isClearCache: Boolean) {
        mZipFile?.close()
    }
}