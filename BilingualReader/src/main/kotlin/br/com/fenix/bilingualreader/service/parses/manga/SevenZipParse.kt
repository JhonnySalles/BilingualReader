package br.com.fenix.bilingualreader.service.parses.manga

import br.com.fenix.bilingualreader.model.entity.ComicInfo
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.core.Persister
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

class SevenZipParse : Parse {

    private var mEntries = ArrayList<SevenZEntry>()
    private var mSubtitles = ArrayList<SevenZEntry>()
    private var mComicInfo: SevenZEntry? = null

    private class SevenZEntry(val entry: SevenZArchiveEntry, val bytes: ByteArray)

    override fun parse(file: File?) {
        mEntries.clear()
        val sevenZFile = SevenZFile(file)
        var entry = sevenZFile.nextEntry
        while (entry != null) {
            if (entry.isDirectory) {
                entry = sevenZFile.nextEntry
                continue
            }

            if (FileUtil.isImage(entry.name)) {
                val content = ByteArray(entry.size.toInt())
                sevenZFile.read(content)
                mEntries.add(SevenZEntry(entry, content))
            } else if (FileUtil.isJson(entry.name)) {
                val content = ByteArray(entry.size.toInt())
                sevenZFile.read(content)
                mSubtitles.add(SevenZEntry(entry, content))
            } else if (FileUtil.isXml(entry.name) && entry.name.contains("comicinfo", true)) {
                val content = ByteArray(entry.size.toInt())
                sevenZFile.read(content)
                mComicInfo = SevenZEntry(entry, content)
            }

            entry = sevenZFile.nextEntry
        }

        mEntries.sortWith(compareBy<SevenZEntry> { Util.getFolderFromPath(it.entry.name) }.thenComparing { a, b ->
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

        for ((index, item) in mSubtitles.withIndex()) {
            val path = Util.getNameFromPath(getName(item.entry))
            if (path.isNotEmpty() && !paths.containsKey(path))
                paths[path] = index
        }

        return paths
    }

    private fun getName(entry: SevenZArchiveEntry): String {
        return entry.name
    }

    override fun getPagePath(num: Int): String? {
        if (mEntries.size < num)
            return null
        return getName((mEntries[num].entry))
    }

    override fun getPagePaths(): Map<String, Int> {
        val paths = mutableMapOf<String, Int>()

        for ((index, item) in mEntries.withIndex()) {
            val path = Util.getFolderFromPath(getName(item.entry))
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

    override fun destroy(isClearCache: Boolean) {
    }
}