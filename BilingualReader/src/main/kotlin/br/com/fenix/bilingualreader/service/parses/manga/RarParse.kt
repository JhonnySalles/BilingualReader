package br.com.fenix.bilingualreader.service.parses.manga

import br.com.fenix.bilingualreader.model.entity.ComicInfo
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import br.com.fenix.bilingualreader.util.helpers.Util
import com.github.junrar.Archive
import com.github.junrar.exception.CrcErrorException
import com.github.junrar.exception.RarException
import com.github.junrar.rarfile.FileHeader
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.core.Persister
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


class RarParse : Parse {

    companion object {
        // Numero maximo de arquivos RAR mantidos em cache de disco simultaneamente.
        private const val MAX_CACHED_ARCHIVES = 6

        // Nome estavel de pasta de cache por arquivo: inclui caminho, tamanho e data de
        // modificacao, de modo que o cache seja reaproveitado entre sessoes e invalidado
        // automaticamente caso o arquivo mude.
        fun cacheFolderName(file: File): String =
            Util.MD5(file.absolutePath + "_" + file.length() + "_" + file.lastModified())

        // Mantem apenas as pastas de cache mais recentes, preservando sempre a atual.
        fun trimCache(baseDir: File, keepFolder: String) {
            try {
                val folders = baseDir.listFiles()?.filter { it.isDirectory } ?: return
                if (folders.size <= MAX_CACHED_ARCHIVES)
                    return

                folders.sortedByDescending { it.lastModified() }
                    .drop(MAX_CACHED_ARCHIVES)
                    .filter { it.name != keepFolder }
                    .forEach { dir ->
                        dir.listFiles()?.forEach { it.delete() }
                        dir.delete()
                    }
            } catch (ignored: Exception) { }
        }
    }

    private val mLOGGER = LoggerFactory.getLogger(RarParse::class.java)

    private val mHeaders = ArrayList<FileHeader>()
    private var mArchive: Archive? = null
    private var mFile: File? = null
    private var mCacheDir: File? = null
    private var mSolidFileExtracted = false
    private var mSubtitles = ArrayList<FileHeader>()
    private var mComicInfo: FileHeader? = null
    private var mFullCover: FileHeader? = null

    override fun parse(file: File?) {
        mFile = file
        mArchive = Archive(file)

        var header = mArchive!!.nextFileHeader()
        while (header != null) {
            if (!header.isDirectory) {
                val name = getName(header)
                if (FileUtil.isImage(name)) {
                    mHeaders.add(header)
                    if (name.contains("volume", true) && name.contains("tudo", true))
                        mFullCover = header
                } else if (FileUtil.isJson(name))
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
        synchronized(this) {
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
                page.use {
                    serializer.read(ComicInfo::class.java, it)
                }
            } catch (e: Exception) {
                mLOGGER.error("Error to get comic info: " + e.message, e)
                Telemetry.recordException(e, "Error to get comic info: " + e.message)
                null
            }
        } else
            null
    }

    override fun getPage(num: Int): InputStream {
        var isSolid = false
        synchronized(this) {
            isSolid = mArchive?.mainHeader?.isSolid ?: false
            if (isSolid && !mSolidFileExtracted) {
                val files = mArchive?.fileHeaders ?: emptyList()
                for (h in files) {
                    if (!h.isDirectory && FileUtil.isImage(getName(h))) {
                        getPageStream(h)
                    }
                }
                mSolidFileExtracted = true
            }
        }
        return getPageStream(mHeaders[num])
    }

    override fun hasFullCover(): Boolean {
        return mFullCover != null
    }

    override fun getFullCover(): InputStream? {
        return if (hasFullCover()) getPageStream(mFullCover!!) else null
    }

    private fun recreateArchive() {
        try {
            mArchive?.close()
        } catch (ignored: Exception) {}
        val archive = if (mFile != null) Archive(mFile) else null
        mArchive = archive
        if (archive != null) {
            val newHeaders = archive.fileHeaders
            for (i in mHeaders.indices) {
                val oldHeader = mHeaders[i]
                val newHeader = newHeaders.find { it.fileName == oldHeader.fileName }
                if (newHeader != null) {
                    mHeaders[i] = newHeader
                }
            }
            for (i in mSubtitles.indices) {
                val oldHeader = mSubtitles[i]
                val newHeader = newHeaders.find { it.fileName == oldHeader.fileName }
                if (newHeader != null) {
                    mSubtitles[i] = newHeader
                }
            }
            mComicInfo?.let { oldHeader ->
                mComicInfo = newHeaders.find { it.fileName == oldHeader.fileName }
            }
        }
    }

    private fun getHeaderByName(name: String): FileHeader? {
        return mArchive?.fileHeaders?.find { getName(it) == name }
    }

    private fun getPageStream(header: FileHeader, isFirst : Boolean = true): InputStream {
        val name = getName(header)
        return try {
            if (mCacheDir != null) {
                val cacheFile = File(mCacheDir, Util.MD5(name))
                if (cacheFile.exists())
                    return FileInputStream(cacheFile)

                val tempFile = File(mCacheDir, Util.MD5(name) + ".tmp")
                synchronized(this) {
                    if (cacheFile.exists())
                        return FileInputStream(cacheFile)

                    val os = FileOutputStream(tempFile)
                    try {
                        val targetHeader = if (isFirst) header else getHeaderByName(name) ?: header
                        mArchive!!.extractFile(targetHeader, os)
                    } catch (e : CrcErrorException) {
                        tempFile.delete()
                        if (isFirst) {
                            Thread.sleep(200)
                            getPageStream(header, false)
                        } else {
                            mLOGGER.error("Error to get page stream: " + e.message, e)
                            throw e
                        }
                    } catch (e: Exception) {
                        tempFile.delete()
                        mLOGGER.error("Error to get page stream (recreating archive): " + e.message, e)
                        if (isFirst) {
                            recreateArchive()
                            Thread.sleep(200)
                            getPageStream(header, false)
                        } else {
                            throw e
                        }
                    } finally {
                        os.close()
                    }
                    tempFile.renameTo(cacheFile)
                }
                return FileInputStream(cacheFile)
            }
            synchronized(this) {
                val targetHeader = if (isFirst) header else getHeaderByName(name) ?: header
                try {
                    val stream = mArchive!!.getInputStream(targetHeader)
                    ByteArrayInputStream(stream.readBytes())
                } catch (e: Exception) {
                    mLOGGER.error("Error to get page stream direct (recreating archive): " + e.message, e)
                    if (isFirst) {
                        recreateArchive()
                        Thread.sleep(200)
                        getPageStream(header, false)
                    } else {
                        throw e
                    }
                }
            }
        } catch (e: RarException) {
            mLOGGER.error("Error to get page stream: " + e.message, e)
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
        mFile = null
    }

    fun setCacheDirectory(cacheDirectory: File?, preserveExisting: Boolean = false) {
        mCacheDir = cacheDirectory
        mCacheDir?.let {
            if (!it.exists())
                it.mkdirs()

            // Quando preserveExisting=true (leitor de manga com pasta estavel por arquivo),
            // o cache extraido e reaproveitado entre sessoes, evitando reextrair RAR solido.
            if (!preserveExisting && it.listFiles() != null) {
                for (f in it.listFiles()!!)
                    f.delete()
            }
        }
    }
}