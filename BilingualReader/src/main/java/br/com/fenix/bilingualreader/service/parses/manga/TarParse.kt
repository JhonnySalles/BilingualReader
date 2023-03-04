package br.com.fenix.bilingualreader.service.parses.manga

import br.com.fenix.bilingualreader.model.entity.ComicInfo
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.core.Persister
import java.io.*

class TarParse : Parse {

    private var mEntries = ArrayList<TarEntry>()
    private var mSubtitles = ArrayList<TarEntry>()
    private var mComicInfo: TarEntry? = null

    private class TarEntry(val entry: TarArchiveEntry, val bytes: ByteArray)

    override fun parse(file: File?) {
        mEntries.clear()
        val fis = BufferedInputStream(FileInputStream(file))
        val `is` = TarArchiveInputStream(fis)
        var entry = `is`.nextTarEntry
        while (entry != null) {
            if (entry.isDirectory)
                continue

            if (FileUtil.isImage(entry.name))
                mEntries.add(TarEntry(entry, Util.toByteArray(`is`)!!))
            else if (FileUtil.isJson(entry.name))
                mSubtitles.add(TarEntry(entry, Util.toByteArray(`is`)!!))
            else if (FileUtil.isXml(entry.name) && entry.name.contains("comicinfo", true))
                mComicInfo = TarEntry(entry, Util.toByteArray(`is`)!!)

            entry = `is`.nextTarEntry
        }

        mEntries.sortWith(compareBy<TarEntry> { Util.getFolderFromPath(it.entry.name) }.thenComparing { a, b ->
            Util.getNormalizedNameOrdering(a.entry.name)
                .compareTo(Util.getNormalizedNameOrdering(b.entry.name))
        })
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

    override fun getComicInfo(): ComicInfo? {
        return if (mComicInfo != null) {
            val page = ByteArrayInputStream(mComicInfo!!.bytes)
            val serializer: Serializer = Persister()
            try {
                serializer.read(ComicInfo::class.java, page)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else
            null
    }

    override fun getPage(num: Int): InputStream {
        return ByteArrayInputStream(mEntries[num].bytes)
    }

    override fun getType(): String {
        return "tar"
    }

    override fun destroy(isClearCache: Boolean) {
    }
}