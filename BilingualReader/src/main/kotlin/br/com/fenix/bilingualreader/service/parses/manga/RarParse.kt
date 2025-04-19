package br.com.fenix.bilingualreader.service.parses.manga

import br.com.fenix.bilingualreader.model.entity.ComicInfo
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import com.github.junrar.Archive
import com.github.junrar.exception.CrcErrorException
import com.github.junrar.exception.RarException
import com.github.junrar.rarfile.FileHeader
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.core.Persister
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


class RarParse : Parse {

    private val mHeaders = ArrayList<FileHeader>()
    private var mArchive: Archive? = null
    private var mCacheDir: File? = null
    private var mSolidFileExtracted = false
    private var mSubtitles = ArrayList<FileHeader>()
    private var mComicInfo: FileHeader? = null

    override fun parse(file: File?) {
        mArchive = Archive(file)

        var header = mArchive!!.nextFileHeader()
        while (header != null) {
            if (!header.isDirectory) {
                val name = getName(header)
                if (FileUtil.isImage(name))
                    mHeaders.add(header)
                else if (FileUtil.isJson(name))
                    mSubtitles.add(header)
                else if (FileUtil.isXml(name) && name.contains("comicinfo", true))
                    mComicInfo = header
            }
            header = mArchive!!.nextFileHeader()
        }

        mHeaders.sortWith(compareBy<FileHeader> { Util.getFolderFromPath(it.fileName) }.thenComparing { a, b ->
            Util.getNormalizedNameOrdering(a.fileName)
                .compareTo(Util.getNormalizedNameOrdering(b.fileName))
        })
    }

    private fun getName(header: FileHeader): String {
        return header.fileName
    }

    override fun numPages(): Int {
        return mHeaders.size
    }

    override fun getSubtitles(): List<String> {
        val subtitles = arrayListOf<String>()
        mSubtitles.forEach {
            val sub = mArchive!!.getInputStream(it)
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

    override fun getPagePath(num: Int): String? {
        if (mHeaders.isEmpty() || mHeaders.size < num)
            return null
        return getName(mHeaders[num])
    }

    override fun getPagePaths(): Map<String, Int> {
        val paths = mutableMapOf<String, Int>()

        for ((index, header) in mHeaders.withIndex()) {
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
            val page = getPageStream(mComicInfo!!)
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
        if (mArchive!!.mainHeader.isSolid) {
            synchronized(this) {
                if (!mSolidFileExtracted) {
                    for (h in mArchive!!.fileHeaders) {
                        if (!h.isDirectory && FileUtil.isImage(getName(h))) {
                            getPageStream(h)
                        }
                    }
                    mSolidFileExtracted = true
                }
            }
        }
        return getPageStream(mHeaders[num])
    }

    private fun getPageStream(header: FileHeader, isFirst : Boolean = true): InputStream {
        return try {
            if (mCacheDir != null) {
                val name = getName(header)
                val cacheFile = File(mCacheDir, Util.MD5(name))
                if (cacheFile.exists())
                    return FileInputStream(cacheFile)

                synchronized(this) {
                    val os = FileOutputStream(cacheFile)
                    try {
                        mArchive!!.extractFile(header, os)
                    } catch (e : CrcErrorException) {
                        cacheFile.delete()
                        if (isFirst) {
                            Thread.sleep(200)
                            getPageStream(header, false)
                        } else {
                            e.printStackTrace()
                            throw e
                        }
                    } catch (e: Exception) {
                        cacheFile.delete()
                        e.printStackTrace()
                        throw e
                    } finally {
                        os.close()
                    }
                }
                return FileInputStream(cacheFile)
            }
            mArchive!!.getInputStream(header)
        } catch (e: RarException) {
            throw IOException("Unable to parse rar: " + e.message, e)
        }
    }

    override fun destroy(isClearCache: Boolean) {
        if (isClearCache) {
            if (mCacheDir != null) {
                mCacheDir?.listFiles()?.let {
                    for (f in it)
                        f.delete()
                }
                mCacheDir?.delete()
            }
        }
        mHeaders.clear()
        mArchive?.close()
        mArchive = null
    }

    fun setCacheDirectory(cacheDirectory: File?) {
        mCacheDir = cacheDirectory
        mCacheDir?.let {
            if (!it.exists())
                it.mkdirs()

            if (it.listFiles() != null) {
                for (f in it.listFiles()!!)
                    f.delete()
            }
        }
    }
}