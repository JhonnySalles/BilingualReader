package br.com.ebook.foobnix.pdf.info

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.text.format.DateFormat
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import br.com.ebook.foobnix.android.utils.Apps
import br.com.ebook.foobnix.android.utils.Safe
import br.com.ebook.foobnix.android.utils.TxtUtils
import br.com.ebook.foobnix.ext.CacheZipUtils
import br.com.ebook.foobnix.pdf.info.model.BookCSS
import br.com.ebook.foobnix.pdf.info.wrapper.AppState
import br.com.ebook.universalimageloader.core.ImageLoader
import org.ebookdroid.BookType
import org.ebookdroid.common.cache.CacheManager
import org.mozilla.universalchardet.UniversalDetector
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileFilter
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.DecimalFormat
import java.util.Arrays
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.regex.Pattern

class ExtUtils {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(ExtUtils::class.java)

        private const val IMAGE_PNG_BASE64 = "image/png;base64,"
        private const val IMAGE_JPEG_BASE64 = "image/jpeg;base64,"
        const val REFLOW_EPUB = "-reflow.epub"
        const val REFLOW_HTML = "-reflow.html"
        private const val IMAGE_BEGIN = "<image-begin>"
        private const val IMAGE_END = "<image-end>"

        @JvmField
        var ES: ExecutorService = Executors.newFixedThreadPool(4)

        @JvmField
        val otherExts: List<String> = Arrays.asList(*AppState.OTHER_BOOK_EXT)

        @JvmField
        val lirbeExt: List<String> = Arrays.asList(*AppState.LIBRE_EXT)

        @JvmField
        val imageExts: List<String> = Arrays.asList(".png", ".jpg", ".jpeg", ".gif")

        @JvmField
        val imageMimes: List<String> = Arrays.asList("image/png", "image/jpg", "image/jpeg", "image/gif")

        @JvmField
        val archiveExts: List<String> = Arrays.asList(*AppState.OTHER_ARCH_EXT)

        @JvmField
        val browseExts: MutableList<String> = ArrayList(BookType.getAllSupportedExtensions())

        @JvmField
        val mimeCache: MutableMap<String, String> = HashMap()

        @JvmField
        val seachExts: MutableList<String> = ArrayList()

        private var dateFormat: java.text.DateFormat? = null
        private var context: Context? = null

        @JvmField
        val audio: List<String> = Arrays.asList(".mp3", ".mp4", ".wav", ".ogg", ".m4a")

        @JvmField
        val video: List<String> = Arrays.asList(".webm", ".m3u8", ".ts", ".flv", ".mp4", ".3gp", ".mov", ".avi", ".wmv", ".mp4", ".m4v")

        @JvmField
        val filter: FileFilter = FileFilter { pathname ->
            for (s in browseExts) {
                if (pathname.name.endsWith(s)) return@FileFilter true
            }
            pathname.isDirectory
        }

        init {
            browseExts.addAll(otherExts)
            browseExts.addAll(archiveExts)
            browseExts.addAll(imageExts)
            browseExts.addAll(lirbeExt)
            browseExts.add(".json")
            browseExts.addAll(BookCSS.fontExts)
            browseExts.addAll(Arrays.asList(*AppState.OTHER_BOOK_MEDIA))

            mimeCache[".tpz"] = "application/x-topaz-ebook"
            mimeCache[".azw1"] = "application/x-topaz-ebook"
            mimeCache[".pgn"] = " application/x-chess-pgn"
            mimeCache[".jpeg"] = "image/jpeg"
            mimeCache[".jpg"] = "image/jpeg"
            mimeCache[".png"] = "image/png"
            mimeCache[".chm"] = "application/x-chm"
            mimeCache[".xps"] = "application/vnd.ms-xpsdocument"
            mimeCache[".lit"] = "application/x-ms-reader"
            mimeCache[".doc"] = "application/msword"
            mimeCache[".docx"] = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            mimeCache[".ppt"] = "application/vnd.ms-powerpoint"
            mimeCache[".pptx"] = "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            mimeCache[".odt"] = "application/vnd.oasis.opendocument.text"
            mimeCache[".odp"] = "application/vnd.oasis.opendocument.presentation"
            mimeCache[".gz"] = "application/x-gzip"
            mimeCache[".zip"] = "application/x-compressed-zip"
            mimeCache[".rar"] = "application/x-rar-compressed"
            mimeCache[".cbr"] = "application/x-cbr"
            mimeCache[".cbt"] = "application/x-cbr"
            mimeCache[".cb7"] = "application/x-cbr"
            mimeCache[".mp3"] = "audio/mpeg"
            mimeCache[".mp4"] = "audio/mp4"
            mimeCache[".wav"] = "audio/vnd.wav"
            mimeCache[".ogg"] = "audio/ogg"
            mimeCache[".m4a"] = "audio/m4a"
            mimeCache[".m3u8"] = "application/x-mpegURL"
            mimeCache[".ts"] = "video/MP2T"
            mimeCache[".flv"] = "video/x-flv"
            mimeCache[".m4v"] = "video/x-m4v"
            mimeCache[".3gp"] = "video/3gpp"
            mimeCache[".mov"] = "video/quicktime"
            mimeCache[".avi"] = "video/x-msvideo"
            mimeCache[".wmv"] = "video/x-ms-wmv"
            mimeCache[".webm"] = "video/webm"
        }

        @JvmStatic
        fun openFile(a: Activity, file: File) {
            if (doifFileExists(a, file)) {
                if (isZip(file)) {
                    if (CacheZipUtils.isSingleAndSupportEntryFile(file).first) {
                        showDocument(a, file)
                    }
                } else if (isNotSupportedFile(file)) {
                    openWith(a, file)
                } else {
                    showDocument(a, file)
                }
            }
        }

        @JvmStatic
        fun isMediaContent(path: String?): Boolean {
            if (TxtUtils.isEmpty(path)) {
                return false
            }
            val pathLow = path!!.trim().lowercase(Locale.getDefault())
            for (ext in audio) {
                if (pathLow.endsWith(ext)) {
                    return true
                }
            }
            for (ext in video) {
                if (pathLow.endsWith(ext)) {
                    return true
                }
            }
            return false
        }

        @JvmStatic
        fun upperCaseFirst(text: String): String {
            var txt = text
            if (txt.length >= 1) {
                txt = txt.trim()
                txt = txt.substring(0, 1).uppercase(Locale.getDefault()) + txt.substring(1)
            }
            return txt
        }

        @JvmStatic
        fun isNotSupportedFile(file: File): Boolean {
            return !BookType.isSupportedExtByPath(file.path)
        }

        @JvmStatic
        fun isImageOrEpub(file: File): Boolean {
            return isImageFile(file) || isFileArchive(file) || BookType.EPUB.`is`(file.path)
        }

        @JvmStatic
        fun isNoTextLayerForamt(name: String?): Boolean {
            return BookType.DJVU.`is`(name) || BookType.CBZ.`is`(name) || BookType.TIFF.`is`(name)
        }

        @JvmStatic
        fun getMimeTypeByUri(uri: Uri): String? {
            var mimeType: String? = null
            try {
                if (uri.scheme == ContentResolver.SCHEME_CONTENT && context != null) {
                    val cr = context!!.contentResolver
                    mimeType = cr.getType(uri)
                }
                if (mimeType == null) {
                    val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.path)
                    mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
                }
            } catch (e: Exception) {
                LOGGER.error("Error get mime type by uri: {}", e.message, e)
            }
            return mimeType
        }

        @JvmStatic
        fun isImageFile(file: File?): Boolean {
            return file != null && file.isFile && isImagePath(file.name)
        }

        @JvmStatic
        fun isImagePath(path: String?): Boolean {
            if (path == null) {
                return false
            }
            val name = path.lowercase(Locale.getDefault())
            for (ext in imageExts) {
                if (name.endsWith(ext)) {
                    return true
                }
            }
            return false
        }

        @JvmStatic
        fun isImageMime(mime: String?): Boolean {
            if (mime == null) {
                return false
            }
            val mimeLow = mime.lowercase(Locale.getDefault())
            for (ext in imageMimes) {
                if (ext == mimeLow) {
                    return true
                }
            }
            return false
        }

        @JvmStatic
        fun isLibreFile(file: File?): Boolean {
            if (file != null && file.isFile) {
                val name = file.name.lowercase(Locale.getDefault())
                for (ext in lirbeExt) {
                    if (name.endsWith(ext)) {
                        return true
                    }
                }
            }
            return false
        }

        @JvmStatic
        fun isOtherFile(file: File?): Boolean {
            if (file != null && file.isFile) {
                val name = file.name.lowercase(Locale.getDefault())
                for (ext in otherExts) {
                    if (name.endsWith(ext)) {
                        return true
                    }
                }
            }
            return false
        }

        @JvmStatic
        fun isFileArchive(name: String?): Boolean {
            if (name == null) {
                return false
            }
            val nameLow = name.lowercase(Locale.getDefault())
            for (ext in archiveExts) {
                if (nameLow.endsWith(ext)) {
                    return true
                }
            }
            return false
        }

        @JvmStatic
        fun isFileArchive(file: File?): Boolean {
            if (file != null && file.isFile) {
                val name = file.name.lowercase(Locale.getDefault())
                for (ext in archiveExts) {
                    if (name.endsWith(ext)) {
                        return true
                    }
                }
            }
            return false
        }

        @JvmStatic
        fun isFontFile(name: String): Boolean {
            val nameLow = name.lowercase(Locale.getDefault())
            for (ext in BookCSS.fontExts) {
                if (nameLow.endsWith(ext)) {
                    return true
                }
            }
            return false
        }

        @JvmStatic
        fun init(c: Context) {
            context = c
            dateFormat = DateFormat.getDateFormat(c)
            updateSearchExts()
        }

        @JvmStatic
        fun getFileExtension(file: File): String {
            return getFileExtension(file.name)
        }

        @JvmStatic
        fun getFileExtension(name: String?): String {
            var fileName = name ?: return ""
            if (fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1)
            }
            if (!fileName.contains(".")) {
                return ""
            }
            return try {
                fileName.substring(fileName.lastIndexOf(".") + 1)
            } catch (e: Exception) {
                ""
            }
        }

        @JvmStatic
        fun getFileNameWithoutExt(name: String): String {
            if (!name.contains(".")) {
                return name
            }
            return name.substring(0, name.lastIndexOf("."))
        }

        @JvmStatic
        fun getFileName(name: String): String {
            if (!name.contains("/")) {
                return name
            }
            return try {
                name.substring(name.lastIndexOf("/") + 1)
            } catch (e: Exception) {
                name
            }
        }

        @JvmField
        val SPECIAL_CHARACTER: Pattern = Pattern.compile("[^\\w\\d\\s\\-.,/\\\\]")

        @JvmStatic
        fun validNameFileCharacter(input: String, outputDir: String, name: String): String {
            if (SPECIAL_CHARACTER.matcher(input).find()) {
                try {
                    return Files.copy(
                        File(input).toPath(),
                        File(outputDir, name + "." + getFileExtension(input)).toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    ).toString()
                } catch (e: Exception) {
                    throw RuntimeException("Error to copy file to output dir: " + e.message)
                }
            }
            return input
        }

        @JvmStatic
        fun updateSearchExts() {
            val result: MutableList<String> = ArrayList()
            seachExts.clear()

            if (AppState.get().supportPDF) result.add(".pdf")
            if (AppState.get().supportXPS) result.add(".xps")
            if (AppState.get().supportEPUB) result.add(".epub")
            if (AppState.get().supportDJVU) result.add(".djvu")

            if (AppState.get().supportFB2) {
                result.add(".fb2")
                if (!AppState.get().supportZIP) result.add(".fb2.zip")
            }

            if (AppState.get().supportTXT) {
                result.add(".txt")
                result.add(".html")
                result.add(".xhtml")
                if (!AppState.get().supportZIP) result.add(".txt.zip")
            }

            if (AppState.get().supportRTF) {
                result.add(".rtf")
                if (!AppState.get().supportZIP) result.add(".rtf.zip")
            }

            if (AppState.get().supportMOBI) {
                result.add(".mobi")
                result.add(".azw")
                result.add(".azw3")
            }

            if (AppState.get().supportCBZ) {
                result.add(".cbz")
                result.add(".cbr")
            }

            if (AppState.get().supportZIP) result.addAll(archiveExts)

            if (AppState.get().supportOther) {
                result.addAll(otherExts)
                result.addAll(lirbeExt)
            }

            for (ext in result) {
                seachExts.add(ext)
            }
        }

        @JvmStatic
        fun getFileFilter(): FileFilter {
            return filter
        }

        @JvmStatic
        fun doifFileExists(c: Context?, file: File?): Boolean {
            if (file != null && file.isFile) return true
            if (c != null && file != null) {
                Toast.makeText(c, "Arquivo não encontrado " + file.path, Toast.LENGTH_LONG).show()
            }
            return false
        }

        @JvmStatic
        fun doifFileExists(c: Context?, path: String): Boolean {
            return doifFileExists(c, File(path))
        }

        @JvmStatic
        fun isTextFomat(intent: Intent?): Boolean {
            if (intent == null || intent.data == null || intent.data!!.path == null) {
                return false
            }
            return isTextFomat(intent.data!!.path)
        }

        @JvmStatic
        @Synchronized
        fun isTextFomat(path: String?): Boolean {
            if (path == null) return false
            return (BookType.ZIP.`is`(path) || BookType.EPUB.`is`(path) || BookType.FB2.`is`(path) ||
                    BookType.TXT.`is`(path) || BookType.RTF.`is`(path) || BookType.HTML.`is`(path) ||
                    BookType.MOBI.`is`(path) || BookType.DOCX.`is`(path) || BookType.ODT.`is`(path) ||
                    BookType.MD.`is`(path))
        }

        @JvmStatic
        @Synchronized
        fun isZip(path: File): Boolean {
            return isZip(path.path)
        }

        @JvmStatic
        @Synchronized
        fun isZip(path: String?): Boolean {
            if (path == null) return false
            return path.lowercase(Locale.getDefault()).endsWith(".zip")
        }

        @JvmStatic
        @Synchronized
        fun isNoMetaFomat(path: String?): Boolean {
            if (path == null) return false
            return (BookType.TXT.`is`(path) || BookType.RTF.`is`(path) || BookType.HTML.`is`(path) ||
                    BookType.PDF.`is`(path) || BookType.DJVU.`is`(path) || BookType.CBZ.`is`(path) ||
                    BookType.MD.`is`(path))
        }

        @JvmStatic
        fun getDateFormat(file: File): String {
            return if (dateFormat != null) dateFormat!!.format(file.lastModified()) else ""
        }

        @JvmStatic
        fun readableFileSize(size: Long): String {
            if (size <= 0) return "0"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            return DecimalFormat("#,##0").format(size / Math.pow(1024.0, digitGroups.toDouble())) + "" + units[digitGroups]
        }

        @JvmStatic
        fun isNotValidFile(file: File?): Boolean {
            return !isValidFile(file)
        }

        @JvmStatic
        fun isValidFile(file: File?): Boolean {
            return file != null && file.isFile
        }

        @JvmStatic
        fun isValidFile(path: String?): Boolean {
            return path != null && isValidFile(File(path))
        }

        @JvmStatic
        fun isValidFile(uri: Uri?): Boolean {
            return uri != null && isValidFile(uri.path)
        }

        @JvmStatic
        fun showDocument(c: Context, file: File): Boolean {
            return showDocument(c, file, -1)
        }

        @JvmStatic
        fun showDocument(c: Context, file: File, page: Int): Boolean {
            ImageLoader.instance?.clearAllTasks()
            if (AppState.get().isRememberMode) {
                showDocumentWithoutDialog(c, file, page)
                return true
            }
            return true
        }

        @JvmStatic
        fun showDocumentWithoutDialog(c: Context, file: File, page: Int) {
            showDocument(c, Uri.fromFile(file), page)
        }

        @JvmStatic
        fun showDocument(c: Activity, uri: Uri?): Boolean {
            var filePath = CacheManager.getFilePathFromAttachmentIfNeed(c)
            if (TxtUtils.isEmpty(filePath) && uri != null && uri.path != null) {
                filePath = uri.path
            }
            return showDocument(c, File(filePath), -1)
        }

        @JvmStatic
        fun showDocument(c: Context, uri: Uri?, page: Int) {
            Safe.run { showDocumentInner(c, uri, page) }
        }

        @JvmStatic
        fun showDocumentInner(c: Context, uri: Uri?, page: Int) {
            if (!isValidFile(uri)) {
                Toast.makeText(c, "Arquivo não encontrado", Toast.LENGTH_LONG).show()
                return
            }
            if (AppState.get().isAlwaysOpenAsMagazine) {
                openHorizontalView(c, File(uri!!.path), page - 1)
            }
        }

        private fun openHorizontalView(c: Context, file: File?, page: Int) {
            if (file == null) {
                Toast.makeText(c, "Arquivo não encontrado", Toast.LENGTH_LONG).show()
                return
            }
            if (!isValidFile(file.path)) {
                Toast.makeText(c, "Arquivo não encontrado", Toast.LENGTH_LONG).show()
            }
        }

        @JvmStatic
        fun createOpenFileIntent(context: Context, file: File): Intent {
            val extension = extensionFromName(file.name)
            var mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (mimeType == null) {
                mimeType = getMimeType(file)
            }
            val openIntent = Intent().apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(getUriProvider(context, file), mimeType)
            }
            val packageManager = context.packageManager
            val defaultAppInfo = packageManager.resolveActivity(openIntent, PackageManager.MATCH_DEFAULT_ONLY)
            if (defaultAppInfo != null && !defaultAppInfo.activityInfo.name.endsWith("ResolverActivity")) {
                return openIntent
            }
            val targetedOpenIntents = ArrayList<Intent>()
            val appInfoList = packageManager.queryIntentActivities(openIntent, PackageManager.MATCH_DEFAULT_ONLY)
            if (appInfoList.isEmpty()) {
                return openIntent
            }
            Collections.sort(appInfoList, Comparator { first, second ->
                val firstName = packageManager.getApplicationLabel(first.activityInfo.applicationInfo).toString()
                val secondName = packageManager.getApplicationLabel(second.activityInfo.applicationInfo).toString()
                firstName.compareTo(secondName, ignoreCase = true)
            })
            for (appInfo in appInfoList) {
                val packageName = appInfo.activityInfo.packageName
                if (packageName == context.packageName) {
                    continue
                }
                val targetedOpenIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(getUriProvider(context, file), mimeType)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setPackage(packageName)
                }
                targetedOpenIntents.add(targetedOpenIntent)
            }
            if (targetedOpenIntents.isEmpty()) {
                return openIntent
            }
            val remove = targetedOpenIntents.removeAt(targetedOpenIntents.size - 1)
            val createChooser = Intent.createChooser(remove, "Selecione")
            return createChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedOpenIntents.toArray(arrayOf<Parcelable>()))
        }

        @JvmStatic
        fun extensionFromName(fileName: String): String {
            val dotPosition = fileName.lastIndexOf('.')
            return if (dotPosition == -1 || dotPosition == fileName.length - 1) "" else fileName.substring(dotPosition + 1).lowercase(Locale.getDefault())
        }

        @JvmStatic
        fun openWith(a: Context, file: File) {
            a.startActivity(createOpenFileIntent(a, file))
        }

        @JvmStatic
        fun getUriProvider(a: Context, file: File): Uri {
            return if (Build.VERSION.SDK_INT >= 24) {
                FileProvider.getUriForFile(a, Apps.getPackageName(a) + ".provider", file)
            } else {
                Uri.fromFile(file)
            }
        }

        @JvmStatic
        fun getMimeType(file: File): String {
            var mime = ""
            try {
                val name = file.name.lowercase(Locale.getDefault())
                val ext = getFileExtension(name)
                val mimeType = mimeCache["." + ext]
                if (mimeType != null) {
                    mime = mimeType
                } else {
                    val codecType = BookType.getByUri(name)
                    mime = codecType?.firstMimeType ?: ""
                }
            } catch (e: Exception) {
                mime = "application/" + getFileExtension(file)
            }
            return mime
        }

        @JvmStatic
        fun determineEncoding(fis: InputStream): String {
            var encoding: String? = null
            try {
                val detector = UniversalDetector(null)
                var nread: Int
                val buf = ByteArray(1024)
                while (fis.read(buf).also { nread = it } > 0 && !detector.isDone) {
                    detector.handleData(buf, 0, nread)
                }
                detector.dataEnd()
                encoding = detector.detectedCharset
                detector.reset()
                fis.close()
            } catch (e: Exception) {
                LOGGER.error("Error determine encoding: {}", e.message, e)
            }
            return encoding ?: "UTF-8"
        }
    }
}
