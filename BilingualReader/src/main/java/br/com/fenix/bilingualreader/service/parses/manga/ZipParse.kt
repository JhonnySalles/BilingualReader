package br.com.fenix.bilingualreader.service.parses.manga

import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class ZipParse : Parse {

    private var mZipFile: ZipFile? = null
    private var mEntries = ArrayList<ZipEntry>()
    private var mSubtitles = ArrayList<ZipEntry>()

    override fun parse(file: File?) {
        mZipFile = ZipFile(file?.absolutePath, StandardCharsets.UTF_8)
        mEntries = ArrayList()
        mSubtitles = ArrayList()
        val e = mZipFile!!.entries()
        while (e.hasMoreElements()) {
            val ze = e.nextElement()
            if (!ze.isDirectory && FileUtil.isImage(ze.name))
                mEntries.add(ze)
            else if (!ze.isDirectory && FileUtil.isJson(ze.name))
                mSubtitles.add(ze)
        }

        mEntries.sortWith(compareBy<ZipEntry> { Util.getFolderFromPath(it.name) }.thenComparing { a, b ->
            Util.getNormalizedNameOrdering(a.name).compareTo(Util.getNormalizedNameOrdering(b.name))
        })
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

    override fun getPage(num: Int): InputStream {
        return mZipFile!!.getInputStream(mEntries[num])
    }

    override fun getType(): String {
        return "zip"
    }

    override fun destroy(isClearCache: Boolean) {
        mZipFile?.close()
    }
}