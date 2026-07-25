package br.com.fenix.bilingualreader.service.parses.manga

import br.com.fenix.bilingualreader.model.entity.ComicInfo
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import br.com.fenix.bilingualreader.util.helpers.Util
import com.github.junrar.rarfile.FileHeader
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.core.Persister
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class TarParse : Parse {

    private val mLOGGER = LoggerFactory.getLogger(TarParse::class.java)

    private var mEntries = ArrayList<TarEntry>()
    private var mSubtitles = ArrayList<TarEntry>()
    private var mComicInfo: TarEntry? = null
    private var mCover: Array<TarEntry?> = arrayOfNulls<TarEntry?>(3)
    private class TarEntry(val entry: TarArchiveEntry, val bytes: ByteArray)

    override fun parse(file: File?) {
        mEntries.clear()
        BufferedInputStream(FileInputStream(file)).use { fis ->
            TarArchiveInputStream(fis).use { tar ->
                var entry = tar.nextTarEntry
                while (entry != null) {
                    if (entry.isDirectory) {
                        entry = tar.nextTarEntry
                        continue
                    }

                    if (FileUtil.isImage(entry.name)) {
                        val tarEntry = TarEntry(entry, Util.toByteArray(tar)!!)
                        mEntries.add(tarEntry)
                        if (entry.name.contains("volume", true)) {
                            if (entry.name.contains("frente", ignoreCase = false) || entry.name.contains("cover", ignoreCase = false) || entry.name.contains("front", ignoreCase = false))
                                mCover[0] = tarEntry
                            else if (entry.name.contains("tras", ignoreCase = false) || entry.name.contains("back", ignoreCase = false))
                                mCover[1] = tarEntry
                            else if (entry.name.contains("tudo", true) || entry.name.contains("all", ignoreCase = false) || entry.name.contains("everything", ignoreCase = false))
                                mCover[2] = tarEntry
                        }
                    } else if (FileUtil.isJson(entry.name))
                        mSubtitles.add(TarEntry(entry, Util.toByteArray(tar)!!))
                    else if (FileUtil.isXml(entry.name) && entry.name.contains("comicinfo", true))
                        mComicInfo = TarEntry(entry, Util.toByteArray(tar)!!)

                    entry = tar.nextTarEntry
                }
            }
        }

        mEntries.sortWith(compareBy<TarEntry> { Util.getFolderFromPath(it.entry.name) }.thenComparing { a, b ->
            Util.getNormalizedNameOrdering(a.entry.name)
                .compareTo(Util.getNormalizedNameOrdering(b.entry.name))
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
            val sub = ByteArrayInputStream(it.bytes)

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

        for ((index, entry) in mEntries.withIndex()) {
            val path = Util.getFolderFromPath(getName(entry))
            if (path.isNotEmpty() && !paths.containsKey(path))
                paths[path] = index
        }

        return paths
    }

    private fun getName(entry: TarEntry): String {
        return entry.entry.name
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
            val page = ByteArrayInputStream(mComicInfo!!.bytes)
            val serializer: Serializer = Persister()
            try {
                serializer.read(ComicInfo::class.java, page)
            } catch (e: Exception) {
                mLOGGER.error("Error to get comic info: " + e.message, e)
                Telemetry.recordException(e, "Error to get comic info: " + e.message)
                null
            }
        } else
            null
    }

    override fun getPage(num: Int): InputStream {
        return ByteArrayInputStream(mEntries[num].bytes)
    }

    override fun hasFullCover(): Boolean = mCover[2] != null

    override fun getFullCover(): InputStream? = if (hasFullCover()) ByteArrayInputStream(mCover[2]!!.bytes) else null

    override fun getCover(): Pair<InputStream?, InputStream?> = Pair(if (mCover[0] != null) ByteArrayInputStream(mCover[0]!!.bytes) else getPage(0), if (mCover[1] != null) ByteArrayInputStream(mCover[1]!!.bytes) else null)

    override fun destroy(isClearCache: Boolean) {
    }
}