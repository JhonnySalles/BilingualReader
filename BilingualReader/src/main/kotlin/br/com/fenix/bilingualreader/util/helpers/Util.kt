package br.com.fenix.bilingualreader.util.helpers

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.NestedScrollView
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Palette.Swatch
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.Filter
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.LibraryBookType
import br.com.fenix.bilingualreader.model.enums.LibraryMangaType
import br.com.fenix.bilingualreader.model.enums.Position
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.model.enums.TouchScreen
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.ocr.ImageProcess
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.Math.abs
import java.math.BigInteger
import java.nio.channels.FileChannel
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.experimental.and
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random


class Util {
    companion object Utils {
        fun getScreenDpWidth(context: Context): Int {
            val displayMetrics = context.resources.displayMetrics
            return Math.round(displayMetrics.widthPixels / displayMetrics.density)
        }

        fun getHeapSize(context: Context): Int {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val isLargeHeap = context.applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP != 0
            var memoryClass = am.memoryClass
            if (isLargeHeap)
                memoryClass = am.largeMemoryClass

            return 1024 * memoryClass
        }

        fun calculateBitmapSize(bitmap: Bitmap): Int {
            val sizeInBytes: Int = bitmap.byteCount
            return sizeInBytes / 1024
        }

        fun getDeviceWidth(): Int {
            val displayMetrics = Resources.getSystem().displayMetrics
            return (displayMetrics.widthPixels / displayMetrics.density).roundToInt()
        }

        fun getDeviceHeight(): Int {
            val displayMetrics = Resources.getSystem().displayMetrics
            return (displayMetrics.heightPixels / displayMetrics.density).roundToInt()
        }

        fun screenHeight(): Int {
            return Resources.getSystem().displayMetrics.heightPixels
        }

        fun screenWidth(): Int {
            return Resources.getSystem().displayMetrics.widthPixels
        }

        fun MD5(string: String): String {
            return try {
                val md = MessageDigest.getInstance("MD5")
                return BigInteger(1, md.digest(string.toByteArray())).toString(16).padStart(32, '0')
            } catch (e: NoSuchAlgorithmException) {
                string.replace("/", ".")
            }
        }

        fun MD5(image: InputStream): String {
            return try {
                val buffer = ByteArray(1024)
                val digest = MessageDigest.getInstance("MD5")
                var numRead = 0
                while (numRead != -1) {
                    numRead = image.read(buffer)
                    if (numRead > 0) digest.update(buffer, 0, numRead)
                }
                val md5Bytes = digest.digest()
                var returnVal = ""
                for (element in md5Bytes)
                    returnVal += Integer.toString((element and 0xff.toByte()) + 0x100, 16)
                        .substring(1)

                returnVal
            } catch (e: Exception) {
                ""
            } finally {
                closeInputStream(image)
            }
        }

        fun calculateMemorySize(context: Context, percentage: Int): Int {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryClass = activityManager.largeMemoryClass
            return 1024 * 1024 * memoryClass / percentage
        }

        fun dpToPx(context: Context, dp: Int): Int {
            val displayMetrics = context.resources.displayMetrics
            return (dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
        }

        fun pxToDp(context: Context, px: Int): Int {
            val displayMetrics = context.resources.displayMetrics
            return (px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
        }

        fun toByteArray(`is`: InputStream): ByteArray? {
            val output = ByteArrayOutputStream()
            return try {
                val b = ByteArray(4096)
                var n: Int
                while (`is`.read(b).also { n = it } != -1) {
                    output.write(b, 0, n)
                }
                output.toByteArray()
            } finally {
                output.close()
            }
        }

        fun toOutputStream(`is`: InputStream): OutputStream {
            val output = ByteArrayOutputStream()
            val b = ByteArray(4096)
            var n: Int
            while (`is`.read(b).also { n = it } != -1) {
                output.write(b, 0, n)
            }
            return output
        }

        fun closeInputStream(input: InputStream?) {
            if (input != null) {
                try {
                    input.close()
                } catch (e: Exception) {
                }
            }
        }

        fun closeOutputStream(output: OutputStream?) {
            if (output != null) {
                try {
                    output.close()
                } catch (e: Exception) {
                }
            }
        }

        fun destroyParse(parse: Parse?, isClearCache: Boolean = true) {
            if (parse != null) {
                try {
                    parse.destroy(isClearCache)
                } catch (e: Exception) {
                }
            }
        }

        fun getNameFromPath(path: String): String {
            return if (path.contains('/'))
                path.substringAfterLast("/")
            else if (path.contains('\\'))
                path.substringAfterLast('\\')
            else
                path
        }

        fun getNameWithoutExtensionFromPath(path: String): String {
            var name = if (path.contains('/'))
                path.substringAfterLast("/")
            else if (path.contains('\\'))
                path.substringAfterLast('\\')
            else
                path

            name = if (name.contains('.'))
                name.substringBeforeLast(".")
            else
                name

            return name
        }

        fun getNameWithoutVolumeAndChapter(manga: String): String {
            if (manga.isEmpty()) return manga

            var name = manga

            if (name.contains(" - "))
                name = name.substringBeforeLast(" - ")

            name = if (name.contains("volume", true))
                name.substringBeforeLast("volume", "").replace("volume", "", true)
            else if (name.contains("capitulo", true))
                name.substringBeforeLast("capitulo").replace("capitulo", "", true)
            else if (name.contains("capítulo", true))
                name.substringBeforeLast("capítulo").replace("capítulo", "", true)
            else name

            return name
        }

        fun getExtensionFromPath(path: String): String {
            return if (path.contains('.'))
                path.substringAfterLast(".")
            else
                path
        }

        fun normalizeNameCache(name: String, prefix: String = "", isRandom: Boolean = true): String {
            val normalize = if (name.contains("-"))
                name.substringBefore("-")
            else if (name.contains(" "))
                name.substringBefore(" ")
            else
                name

            val random = if (isRandom) (0..1000000).random() else ""
            return prefix + normalize.replace("[^\\w\\d ]".toRegex(), "").replace(" ", "_").trim().plus(random).lowercase()
        }

        fun normalizeFilePath(path: String): String {
            var folder: String = path

            if (folder.contains("primary"))
                folder = folder.replaceFirst("primary", "emulated/0")

            if (folder.contains("/tree"))
                folder = folder.replace("/tree", "/storage").replace(":", "/")
            else if (folder.contains("/document"))
                folder = folder.replace("/document", "/storage").replace(":", "/")

            return folder
        }

        fun getChapterFromPath(path: String): Float {
            if (path.isEmpty()) return -1f

            var folder = if (path.contains('/', true))
                path.replaceAfterLast('/', "").replace("/", "", false).lowercase()
            else
                path.replaceAfterLast('\\', "").replace("\\", "", false).lowercase()

            folder = if (folder.contains("capitulo", true))
                folder.substringAfterLast("capitulo").replace("capitulo", "", true)
            else if (folder.contains("capítulo", true))
                folder.substringAfterLast("capítulo").replace("capítulo", "", true)
            else folder

            return folder.toFloatOrNull() ?: -1f
        }

        fun getFolderFromPath(path: String): String {
            // Two validations are needed, because the rar file only has the base values, with the beginning already in the folder when it exists
            val isFolder = path.contains('/') || path.contains('\\')

            var folder = if (path.contains('/'))
                path.replaceAfterLast('/', "").substring(0, path.lastIndexOf('/'))
            else if (path.contains('\\'))
                path.replaceAfterLast('\\', "").substring(0, path.lastIndexOf('\\'))
            else
                path

            folder = if (folder.contains('/'))
                folder.replaceBeforeLast('/', "").replaceFirst("/", "")
            else if (folder.contains('\\'))
                folder.replaceBeforeLast('\\', "").replaceFirst("/", "")
            else
                folder

            return if (!isFolder) "" else folder
        }

        private fun getNumberAtEnd(str: String): String {
            var numbers = ""
            val m: Matcher =
                Pattern.compile("\\d+$|\\d+\\w$|\\d+\\.\\d+$|(\\(|\\{|\\[)\\d+(\\)|\\]|\\})$")
                    .matcher(str)
            while (m.find())
                numbers = m.group()

            return numbers
        }

        private fun getPadding(name: String, numbers: String): String {
            return if (name.contains(Regex("\\d+$")))
                numbers.padStart(10, '0')
            else if (name.contains(Regex("\\d+\\w\$")))
                numbers.replace(Regex("\\w\$"), "")
                    .padStart(10, '0') + numbers.replace(Regex("\\d+"), "")
            else if (name.contains(Regex("\\d+\\.\\d+\$")))
                numbers.replace(Regex("\\.\\d+\$"), "").padStart(10, '0') + '.' + numbers.replace(
                    Regex("\\d+\\."),
                    ""
                )
            else if (name.contains(Regex("(\\(|\\{|\\[)\\d+(\\)|\\]|\\})$")))
                numbers.replace(Regex("[^0-9]"), "").padStart(10, '0')
            else
                numbers
        }

        fun getNormalizedNameOrdering(path: String): String {
            val name: String = getNameWithoutExtensionFromPath(path)
            val numbers = getNumberAtEnd(name)
            return if (numbers.isEmpty())
                getNameFromPath(path)
            else
                name.substring(0, name.lastIndexOf(numbers)) + getPadding(
                    name,
                    numbers
                ) + getExtensionFromPath(path)
        }

        var googleLang: String = ""
        private var mapLanguages: HashMap<String, Languages>? = null
        fun getLanguages(context: Context): HashMap<String, Languages> {
            return if (mapLanguages != null)
                mapLanguages!!
            else {
                val languages = context.resources.getStringArray(R.array.languages)
                googleLang = languages[3]
                mapLanguages = hashMapOf(
                    languages[0] to Languages.PORTUGUESE,
                    languages[1] to Languages.ENGLISH,
                    languages[2] to Languages.JAPANESE,
                    languages[3] to Languages.PORTUGUESE_GOOGLE
                )
                mapLanguages!!
            }
        }

        fun stringToLanguage(context: Context, language: String): Languages? {
            val mapLanguages = getLanguages(context)
            return if (mapLanguages.containsKey(language)) mapLanguages[language] else null
        }

        fun languageToString(context: Context, language: Languages): String {
            val mapLanguages = getLanguages(context)
            return if (mapLanguages.containsValue(language))
                mapLanguages.filter { language == it.value }.keys.first()
            else
                ""
        }

        fun getColors(context: Context): Map<String, Color> {
            return Color.getColors().filter { it != Color.None }.associateBy { context.getString(it.getDescription()) }
        }


        private var mapMangaFilter: HashMap<String, Filter>? = null
        private var mapBookFilter: HashMap<String, Filter>? = null
        fun getMangaFilters(context: Context): HashMap<String, Filter> {
            return if (mapMangaFilter != null)
                mapMangaFilter!!
            else {
                val types = context.resources.getStringArray(R.array.manga_filters)
                mapMangaFilter = hashMapOf(
                    types[0] to Filter.Author,
                    types[1] to Filter.Publisher,
                    types[2] to Filter.Series,
                    types[3] to Filter.Type,
                    types[4] to Filter.Volume
                )
                mapMangaFilter!!
            }
        }

        fun getBookFilters(context: Context): HashMap<String, Filter> {
            return if (mapBookFilter != null)
                mapBookFilter!!
            else {
                val types = context.resources.getStringArray(R.array.book_filters)
                mapBookFilter = hashMapOf(
                    types[0] to Filter.Author,
                    types[1] to Filter.Publisher,
                    types[2] to Filter.Tag,
                    types[3] to Filter.Type
                )
                mapBookFilter!!
            }
        }

        fun stringToFilter(context: Context, type: Type, text: String, contains: Boolean = false): Filter {
            var filter = Filter.None
            val mapFilters = when (type) {
                Type.MANGA -> getMangaFilters(context)
                Type.BOOK -> getBookFilters(context)
            }
            for (item in mapFilters)
                if ((contains && item.key.contains(text, true)) || (!contains && item.key.equals(text, true))) {
                    filter = item.value
                    break
                }
            return filter
        }

        private var mapHistoryFilter: HashMap<String, Filter>? = null
        fun getHistoryFilters(context: Context): HashMap<String, Filter> {
            return if (mapHistoryFilter != null)
                mapHistoryFilter!!
            else {
                mapHistoryFilter = hashMapOf<String, Filter>()

                for (item in getMangaFilters(context))
                    if (!mapHistoryFilter!!.containsValue(item.value))
                        mapHistoryFilter!!.put(item.key, item.value)

                for (item in getBookFilters(context))
                    if (!mapHistoryFilter!!.containsValue(item.value))
                        mapHistoryFilter!!.put(item.key, item.value)

                mapHistoryFilter!!
            }
        }

        fun historyStringToFilter(context: Context, text : String, contains: Boolean = false): Filter {
            var filter = Filter.None
            val mapFilters = getHistoryFilters(context)
            for (item in mapFilters)
                if ((contains && item.key.contains(text, true)) || (!contains && item.key.equals(text, true))) {
                    filter = item.value
                    break
                }
            return filter
        }

        fun filterToString(context: Context, type: Type, filter: Filter): String {
            val mapFilters = when (type) {
                Type.MANGA -> getMangaFilters(context)
                Type.BOOK -> getBookFilters(context)
            }
            return if (mapFilters.containsValue(filter))
                mapFilters.filter { filter == it.value }.keys.first()
            else
                ""
        }

        fun choiceLanguage(context: Context, theme: Int = R.style.AppCompatMaterialAlertList, ignoreGoogle: Boolean = true, setLanguage: (language: Languages) -> (Unit)) {
            val mapLanguage = getLanguages(context)
            val items = if (ignoreGoogle)
                mapLanguage.keys.filterNot { it == googleLang }.toTypedArray()
            else
                mapLanguage.keys.toTypedArray()

            MaterialAlertDialogBuilder(context, theme)
                .setTitle(context.resources.getString(R.string.languages_choice))
                .setItems(items) { _, selected ->
                    val language = mapLanguage[items[selected]]
                    if (language != null)
                        setLanguage(language)
                }
                .show()
        }

        fun getNameFromMangaTitle(text: String): String {
            return text.substringBeforeLast("Volume").replace(" - ", "").trim()
        }

        fun setBold(text: String): String =
            "<b>$text</b>"

        fun setVerticalText(text: String): String {
            var vertical: String = ""
            for (c in text)
                vertical += c + "\n"

            return vertical
        }

        fun getDivideStrings(text: String, delimiter: Char = '\n', occurrences: Int = 10): Pair<String, String> {
            var postion = text.length
            var occurence = 0
            for ((i, c) in text.withIndex()) {
                if (c == delimiter) {
                    occurence++
                    postion = i
                }
                if (occurence >= occurrences)
                    break
            }

            val string1 = text.substring(0, postion)
            val string2 = if (postion >= text.length) "" else text.substring(postion, text.length)

            return Pair(string1, string2)
        }

        fun formatDecimal(percent: Float): String {
            return "%,.2f".format(percent)
        }

        fun intArrayToString(array: IntArray): String {
            if (array.isEmpty())
                return ""

            return array.joinToString(",")
        }

        fun stringToIntArray(array: String): IntArray {
            if (array.isEmpty())
                return intArrayOf()

            return array.split(",").map { it.toInt() }.toIntArray()
        }

    }
}

class FileUtil(val context: Context) {

    companion object FileUtil {

        fun isXml(filename: String): Boolean {
            return filename.lowercase(Locale.getDefault())
                .matches(Regex(".*\\.(xml)$"))
        }

        fun isJson(filename: String): Boolean {
            return filename.lowercase(Locale.getDefault())
                .matches(Regex(".*\\.(json)$"))
        }

        fun isImage(filename: String): Boolean {
            return filename.lowercase(Locale.getDefault())
                .matches(Regex(".*\\.(jpg|jpeg|bmp|gif|png|webp)$"))
        }

        fun isHtml(filename: String): Boolean {
            return filename.lowercase(Locale.getDefault())
                .matches(Regex(".*\\.(html|xhtml)$"))
        }

        fun getFileType(filename: String): FileType {
            return try {
                FileType.getType(filename)
            } catch (e: Exception) {
                FileType.UNKNOWN
            }
        }

        fun formatSize(size: Long): String {
            if (size < 1024) return "$size B"
            val z = (63 - java.lang.Long.numberOfLeadingZeros(size)) / 10
            return String.format("%.1f %sB", size.toDouble() / (1L shl z * 10), " KMGTPE"[z])
        }

    }

    /**
     * Copies an asset file from assets to phone internal storage, if it doesn't already exist
     * Will be copied to path <prefix> + <assetName> in files directory
     * Returns true if copied, false otherwise (including if file already exists)
     */
    fun copyAssetToFilesIfNotExist(prefix: String, assetName: String, dir: String = ""): Boolean {
        val directory = dir.ifEmpty { context.filesDir.absolutePath }
        val file = File(directory, prefix + assetName)
        if (file.exists())
            return false

        val inputStream: InputStream = context.assets.open(assetName)
        File(directory, prefix).mkdirs()
        // Copy in 10mb chunks to avoid going oom for larger files
        inputStream.copyTo(file.outputStream(), 10000)
        inputStream.close()
        return true
    }

    fun copyFile(fromFile: FileInputStream, toFile: FileOutputStream) {
        var fromChannel: FileChannel? = null
        var toChannel: FileChannel? = null
        try {
            fromChannel = fromFile.channel
            toChannel = toFile.channel
            fromChannel.transferTo(0, fromChannel.size(), toChannel)
        } finally {
            try {
                fromChannel?.close()
            } finally {
                toChannel?.close()
            }
        }
    }

    fun copyName(file: File) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", file.name)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(
            context,
            context.getString(R.string.action_copy_name, file.name),
            Toast.LENGTH_LONG
        ).show()
    }

    fun copyName(manga: Manga) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", manga.fileName)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(
            context,
            context.getString(R.string.action_copy_name, manga.fileName),
            Toast.LENGTH_LONG
        ).show()
    }

    fun copyName(book: Book) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", book.fileName)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(
            context,
            context.getString(R.string.action_copy_name, book.fileName),
            Toast.LENGTH_LONG
        ).show()
    }

}

class MsgUtil {
    companion object MsgUtil {
        fun validPermission(grantResults: IntArray): Boolean {
            var permiss = true
            for (grant in grantResults)
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    permiss = false
                    break
                }
            return permiss
        }

        fun validPermission(context: Context, grantResults: IntArray) {
            if (!validPermission(grantResults))
                MaterialAlertDialogBuilder(context, R.style.AppCompatAlertDialogStyle)
                    .setTitle(context.getString(R.string.alert_permission_files_access_denied_title))
                    .setMessage(context.getString(R.string.alert_permission_files_access_denied))
                    .setPositiveButton(R.string.action_neutral) { _, _ -> }.create().show()
        }

        inline fun alert(
            context: Context, title: String, message: String, theme: Int = R.style.AppCompatMaterialAlertDialog,
            crossinline action: (dialog: DialogInterface, which: Int) -> Unit,
        ) {
            MaterialAlertDialogBuilder(context, theme)
                .setTitle(title).setMessage(message)
                .setPositiveButton(
                    R.string.action_positive
                ) { dialog, which ->
                    action(dialog, which)
                }
                .create().show()
        }

        inline fun alert(
            context: Context, title: String, message: String, theme: Int = R.style.AppCompatMaterialAlertDialog,
            crossinline positiveAction: (dialog: DialogInterface, which: Int) -> Unit,
            crossinline negativeAction: (dialog: DialogInterface, which: Int) -> Unit,
        ) {
            MaterialAlertDialogBuilder(context, theme)
                .setTitle(title).setMessage(message)
                .setPositiveButton(
                    R.string.action_positive
                ) { dialog, which ->
                    positiveAction(dialog, which)
                }
                .setNegativeButton(
                    R.string.action_negative
                ) { dialog, which ->
                    negativeAction(dialog, which)
                }
                .create().show()
        }

        inline fun error(
            context: Context, title: String, message: String, theme: Int = R.style.AppCompatMaterialErrorDialogStyle,
            crossinline action: (dialog: DialogInterface, which: Int) -> Unit,
        ) {
            MaterialAlertDialogBuilder(context, theme)
                .setTitle(title).setMessage(message)
                .setPositiveButton(
                    R.string.action_positive
                ) { dialog, which ->
                    action(dialog, which)
                }
                .create().show()
        }
    }
}

class LibraryUtil {
    companion object LibraryUtils {
        fun getDefault(context: Context, type: Type): Library {
            val base = DataBase.getDataBase(context).getLibrariesDao()
            val string = if (type == Type.BOOK)
                context.getString(R.string.book_library_default)
            else
                context.getString(R.string.manga_library_default)
            val key = if (type == Type.BOOK)
                GeneralConsts.KEYS.LIBRARY.DEFAULT_BOOK
            else
                GeneralConsts.KEYS.LIBRARY.DEFAULT_MANGA
            return Library(
                key,
                string,
                base.getDefault(key)?.path ?: "",
                type = type,
                excluded = true
            )
        }
    }
}

class ImageUtil {
    companion object ImageUtils {
        private var initTouchDown = 0L
        private var initPos: PointF = PointF(0f, 0f)
        private var mScaleFactor = 1.0f

        @SuppressLint("ClickableViewAccessibility")
        fun setZoomPinch(context: Context, image: ImageView, oneClick: () -> Unit) {
            val mScaleListener = object : SimpleOnScaleGestureListener() {
                var mPrevScale = 0f
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    var scale = mScaleFactor * detector.scaleFactor
                    scale = max(1.0f, min(scale, 5.0f))

                    if ((mPrevScale > detector.scaleFactor && mScaleFactor < scale) || (mPrevScale < detector.scaleFactor && mScaleFactor > scale)) {
                        mScaleFactor = scale
                        image.scaleX = mScaleFactor
                        image.scaleY = mScaleFactor
                    }


                    mPrevScale = detector.scaleFactor
                    return true
                }
            }
            val mScaleGestureDetector = ScaleGestureDetector(context, mScaleListener)
            image.setOnTouchListener { view: View, event: MotionEvent ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initTouchDown = System.currentTimeMillis()
                        initPos = PointF(event.x, event.y)
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        image.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(300L)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    super.onAnimationEnd(animation)
                                    mScaleFactor = 1.0f
                                    image.scaleX = mScaleFactor
                                    image.scaleY = mScaleFactor
                                }
                            }).start()

                        val isTouchDuration = System.currentTimeMillis() - initTouchDown < 300
                        val isTouchLength = abs(event.x - initPos.x) + abs(event.y - initPos.y) < 10

                        if (isTouchLength && isTouchDuration)
                            view.performClick()
                    }

                    else -> {
                        mScaleGestureDetector.onTouchEvent(event)
                    }
                }

                true
            }

            image.setOnClickListener { oneClick() }
        }

        fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2

                while (halfHeight / inSampleSize > reqHeight
                    && halfWidth / inSampleSize > reqWidth
                ) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }

        fun imageToByteArray(image: Bitmap): ByteArray? {
            val output = ByteArrayOutputStream()
            return output.use { otp ->
                image.compress(Bitmap.CompressFormat.JPEG, 100, otp)
                otp.toByteArray()
            }
        }

        fun encodeImageBase64(image: Bitmap): String {
            return android.util.Base64.encodeToString(
                imageToByteArray(image),
                android.util.Base64.DEFAULT
            )
        }

        fun decodeImageBase64(image: String): Bitmap? {
            val imageBytes = android.util.Base64.decode(image, android.util.Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }

        fun imageToInputStream(image: Bitmap): InputStream {
            val output = ByteArrayOutputStream()
            return output.use { otp ->
                image.compress(Bitmap.CompressFormat.JPEG, 100, otp)
                ByteArrayInputStream(output.toByteArray())
            }
        }

        fun applyCoverEffect(context: Context, cover: Bitmap?, type: Type) : Bitmap? {
            val image = cover ?: (AppCompatResources.getDrawable(context, R.mipmap.reader_cover_not_found)?.toBitmap() ?: return null)
            val cover = ImageProcess.toGrayscale(image.copy(Bitmap.Config.ARGB_8888, true))
            val canvas = Canvas(cover)

            val effect = when(type) {
                Type.MANGA -> AppCompatResources.getDrawable(context, R.mipmap.book_not_found_effect)
                Type.BOOK -> AppCompatResources.getDrawable(context, R.mipmap.book_not_found_effect)
            }

            effect?.setBounds(0, 0, cover.width, cover.height)
            effect?.draw(canvas)
            return cover
        }

    }
}

class MenuUtil {
    companion object MenuUtils {

        fun tintBackground(context: Context, background: View) {
            background.setBackgroundColor(context.getColorFromAttr(R.attr.background))
        }

        fun tintToolbar(toolbar: Toolbar, theme: Themes) {
            toolbar.popupTheme = theme.getValue()
            toolbar.context.setTheme(getToolbarTheme(theme))
        }

        fun getToolbarTheme(theme: Themes): Int {
            return when (theme) {
                Themes.BLUE -> R.style.MainToolbarTheme_Blue
                Themes.OCEAN_BLUE -> R.style.MainToolbarTheme_OceanBlue
                Themes.GREEN -> R.style.MainToolbarTheme_Green
                Themes.FOREST_GREEN -> R.style.MainToolbarTheme_ForestGreen
                Themes.PINK -> R.style.MainToolbarTheme_Pink
                Themes.RED -> R.style.MainToolbarTheme_Red
                Themes.BLOOD_RED -> R.style.MainToolbarTheme_BloodRed
                else -> R.style.MainToolbarTheme
            }
        }

        fun tintColor(context: Context, textView: TextView) {
            textView.setTextColor(context.getColorFromAttr(R.attr.colorSurfaceContainer))
        }

        fun tintColor(context: Context, textInput: TextInputLayout) {
            textInput.hintTextColor = ColorStateList.valueOf(context.getColorFromAttr(R.attr.colorOnBackground))
            textInput.boxBackgroundColor = context.getColorFromAttr(R.attr.colorOnSurface)
            textInput.boxStrokeColor = context.getColorFromAttr(R.attr.background)
            textInput.placeholderTextColor = ColorStateList.valueOf(context.getColorFromAttr(R.attr.colorOnBackground))
            tintIcons(context, textInput.startIconDrawable, R.attr.colorOnBackground)
            tintIcons(context, textInput.endIconDrawable, R.attr.colorOnBackground)
        }

        fun tintIcons(context: Context, icon: Drawable?, color: Int) {
            icon?.setTint(context.getColorFromAttr(color))
        }

        fun tintIcons(context: Context, icon: Drawable) {
            icon.setTint(context.getColorFromAttr(R.attr.colorSurfaceContainer))
        }

        fun tintIcons(context: Context, icon: ImageView) {
            icon.setColorFilter(context.getColorFromAttr(R.attr.colorSurfaceContainer))
        }

        fun tintAllIcons(context: Context, menu: Menu) {
            for (i in 0 until menu.size())
                menu.getItem(i).icon?.setTint(context.getColorFromAttr(R.attr.colorSurfaceContainer))
        }

        fun tintIcons(context: Context, searchView: SearchView) {
            tintIcons(
                context,
                searchView.findViewById<ImageView>(
                    context.resources.getIdentifier(
                        "android:id/search_button",
                        null,
                        null
                    )
                )
            )
            tintIcons(
                context,
                searchView.findViewById<ImageView>(
                    context.resources.getIdentifier(
                        "android:id/search_close_btn",
                        null,
                        null
                    )
                )
            )
            tintIcons(
                context,
                searchView.findViewById<ImageView>(
                    context.resources.getIdentifier(
                        "android:id/search_mag_icon",
                        null,
                        null
                    )
                )
            )
            tintIcons(
                context,
                searchView.findViewById<ImageView>(
                    context.resources.getIdentifier(
                        "android:id/search_voice_btn",
                        null,
                        null
                    )
                )
            )
        }

        fun tintIcons(context: Context, drawer: DrawerArrowDrawable) {
            drawer.color = context.getColorFromAttr(R.attr.colorSurfaceContainer)
        }

        fun longClick(activity: Activity, menuItem: Int, longCLick: () -> (Unit)) {
            Handler(Looper.getMainLooper()).postDelayed({
                activity.findViewById<View>(menuItem)?.setOnLongClickListener {
                    longCLick()
                    true
                }
            }, 1000)
        }

        fun animatedSequenceDrawable(menu: MenuItem, vararg id: Int) {
            executeAnimatedSequence(menu, 0, id)
        }

        private fun executeAnimatedSequence(menu: MenuItem, sequence: Int, id: IntArray) {
            menu.setIcon(id[sequence])
            (menu.icon as AnimatedVectorDrawable).registerAnimationCallback(object :
                Animatable2.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    super.onAnimationEnd(drawable)
                    val next = sequence.plus(1)
                    if (id.size > next)
                        executeAnimatedSequence(menu, next, id)
                }
            })
            (menu.icon as AnimatedVectorDrawable).start()
        }
    }
}

class ThemeUtil {
    companion object ThemeUtils {

        private var mapThemes: HashMap<String, Themes>? = null
        fun getThemes(context: Context): HashMap<String, Themes> {
            return if (mapThemes != null)
                mapThemes!!
            else {
                val themes = context.resources.getStringArray(R.array.themes)
                mapThemes = hashMapOf(
                    themes[0] to Themes.ORIGINAL,
                    themes[1] to Themes.BLOOD_RED,
                    themes[2] to Themes.BLUE,
                    themes[3] to Themes.FOREST_GREEN,
                    themes[4] to Themes.GREEN,
                    themes[5] to Themes.OCEAN_BLUE,
                    themes[6] to Themes.PINK,
                    themes[7] to Themes.RED,
                )
                mapThemes!!
            }
        }

        fun themeDescription(context: Context, themes: Themes): String {
            val mapThemes = getThemes(context)
            return if (mapThemes.containsValue(themes))
                mapThemes.filter { themes == it.value }.keys.first()
            else
                ""
        }

        @ColorInt
        fun Context.getColorFromAttr(@AttrRes attrColor: Int, typedValue: TypedValue = TypedValue(), resolveRefs: Boolean = true): Int {
            theme.resolveAttribute(attrColor, typedValue, resolveRefs)
            return typedValue.data
        }

        fun statusBarTransparentTheme(window: Window, isDarkTheme: Boolean, statusBarDrawable: Drawable? = null, @ColorInt statusBarColor: Int? = null, isLightStatus: Boolean = false) {
            val wic = WindowInsetsControllerCompat(window, window.decorView)

            if (isDarkTheme)
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            else
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)

            if (isLightStatus)
                window.decorView.systemUiVisibility = (window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)

            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            if (statusBarDrawable != null || statusBarColor != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                val background = statusBarDrawable ?: ColorDrawable(statusBarColor!!)
                window.setBackgroundDrawable(background)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

        }

        fun changeStatusColorFromListener(window: Window, scrollView: NestedScrollView, initialStatusDark: Boolean, isDarkTheme: Boolean, limit: Int = 1000) {
            val wic = WindowInsetsControllerCompat(window, window.decorView)

            wic.isAppearanceLightStatusBars = !initialStatusDark
            scrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                if ((scrollY < limit && oldScrollY > limit) || (scrollY > limit && oldScrollY < limit)) {
                    if (scrollY <= limit)
                        wic.isAppearanceLightStatusBars = !initialStatusDark
                    else
                        wic.isAppearanceLightStatusBars = !isDarkTheme
                }
            }
        }
    }
}

class FontUtil {
    companion object FontUtils {
        fun dipToPixels(context: Context, dipValue: Float): Float {
            val metrics = context.resources.displayMetrics
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics)
        }

        fun pixelToDips(context: Context, pixelValue: Float): Int {
            val metrics = context.resources.displayMetrics
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, pixelValue, metrics).roundToInt()
        }

    }
}

class TextUtil {
    companion object TextUtils {

        private val mPartsDivs = listOf(".", "!", ";", "?", ":")
        fun getParts(text: String): Array<String> {
            var max = -1
            for (ch in mPartsDivs) {
                val last = text.lastIndexOf(ch)
                if (last > max) {
                    max = last
                }
            }
            if (max == -1) {
                max = text.lastIndexOf(",")
            }
            val firstPart = if (max > 0) text.substring(0, max + 1) else text
            val secondPart = if (max > 0) text.substring(max + 1) else ""
            return arrayOf(firstPart, secondPart)
        }

        fun formatHtml(html: String, endLine: String = "<br/>"): String {
            return replaceEndLine(html.replace("<p>", "").replace("</p>", ""), endLine)
        }

        fun replaceEndLine(html: String, character: String = ""): String {
            var page = html
            page = page.replace("-<end-line>", character)
            page = page.replace("- <end-line>", character)
            page = page.replace("<end-line>", " $character")
            return page
        }

        fun replaceImages(html: String): String {
            var page = html
            page = page.replace("<image-begin>[\\w\\W]*<image-end>".toRegex(), "")
            page = page.replace("< ?(img)[^>]*>".toRegex(), "")
            return page
        }

        fun replaceHtmlTTS(html: String?, endLineSeparator: String = " "): String {
            var page = html ?: return ""
            page = page.replace("</?[b|i]>|</?tt>|<p>".toRegex(), "")
            page = page.replace("</p>", " ")
            page = page.replace("<br/>".toRegex(), endLineSeparator)
            page = page.replace("&nbsp;", " ").replace("&lt;", " ").replace("&gt;", "")
                .replace("&amp;", " ").replace("&quot;", " ")
            page = page.replace("['|*]".toRegex(), "")
            page = page.replace("  ", " ").replace("  ", " ")
            page = page.replace(".", ". ").replace(" .", ".")
            page = page.replace("(?u)(\\w+)(-\\s)".toRegex(), "$1")
            return replaceHtmlTags(page).trim()
        }

        fun replaceHtmlTags(html: String): String = html.replace("<[^>]*>".toRegex(), "")

        fun highlightWordInText(html: String, contain: String, @ColorInt color: Int): String {
            val color = ColorUtil.getColor(color)
            return highlightWordInText(html, contain, color)
        }

        fun highlightWordInText(html: String, contain: String, color: String): String = replaceHtmlTags(html).replace(contain, "<font color=$color>$contain</font>")

        fun clearHighlightWordInText(html: String): String = replaceHtmlTags(html)

        fun getImageFromTag(html: String) = html.substringAfter("<img src=\"").substringBefore("\" />")

        fun isOnlyImageOnHtml(html: String): Boolean = html.contains("< ?(img)[^>]*>".toRegex()) && replaceHtmlTags(html).trim().isEmpty()
    }
}

class AnimationUtil {
    companion object AnimationUtils {
        const val duration = 200L
        fun animatePopupOpen(activity: Activity, frame: FrameLayout, isVertical: Boolean = true, navigationColor: Boolean = true, ending: () -> (Unit) = {}) {
            frame.visibility = View.VISIBLE
            if (isVertical) {
                if (navigationColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    activity.window?.run {
                        navigationBarColor = activity.getColorFromAttr(R.attr.colorSurfaceVariant)
                        WindowCompat.getInsetsController(this, this.decorView).isAppearanceLightNavigationBars = true
                    }

                val positionInitial = frame.translationY
                frame.translationY = positionInitial + 200F
                frame.animate()
                    .setDuration(duration)
                    .translationY(positionInitial)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                        }
                    })
            } else {
                val positionInitial = frame.translationX
                frame.translationX = 200F
                frame.animate()
                    .setDuration(duration)
                    .translationX(positionInitial)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                        }
                    })
            }
        }

        fun animatePopupClose(activity: Activity, frame: FrameLayout, isVertical: Boolean = true, navigationColor: Boolean = true) {
            if (isVertical) {
                val positionInitial = frame.translationY
                frame.animate()
                    .setDuration(duration)
                    .translationY(positionInitial + 200F)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            frame.visibility = View.GONE
                            frame.translationY = positionInitial

                            if (navigationColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                                activity.window.navigationBarColor = android.graphics.Color.TRANSPARENT
                        }
                    })
            } else {
                val positionInitial = frame.translationX
                frame.animate()
                    .setDuration(duration)
                    .translationX(positionInitial + 200F)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            frame.visibility = View.GONE
                            frame.translationX = positionInitial
                        }
                    })
            }
        }

    }
}

class ColorUtil {
    companion object ColorsUtils {
        fun @receiver:ColorInt Int.isDark(): Boolean = ColorUtils.calculateLuminance(this) < 0.5

        const val LIGHTNESS_UNKNOWN = 0
        const val LIGHTNESS_DARK = 1
        const val LIGHTNESS_LIGHT = 2
        private fun isDark(palette: Palette?) : Int {
            var mostPopulous: Swatch? = null
            if (palette != null) {
                for (swatch in palette.swatches) {
                    if (mostPopulous == null || swatch.population > mostPopulous.population) {
                        mostPopulous = swatch
                    }
                }
            }

            mostPopulous ?: return LIGHTNESS_UNKNOWN
            return if (mostPopulous.rgb.isDark()) LIGHTNESS_DARK else LIGHTNESS_LIGHT
        }

        fun isDarkColor(bitmap: Bitmap, isDark: (Boolean) -> (Unit)) {
            Palette.from(bitmap)
                .maximumColorCount(3)
                .clearFilters()
                .setRegion(0 , 0, bitmap.width, bitmap.height / 2)
                .generate { palette ->
                    val dark = when (isDark(palette)) {
                        LIGHTNESS_DARK -> true
                        else -> false
                    }
                    isDark(dark)
                }
        }

        fun isDarkColor(bitmap: Bitmap, iconWidth: Int, iconHeight: Int, isPositionRight: Boolean = false, isDark: (Boolean) -> (Unit)) {
            Palette.from(bitmap)
                .maximumColorCount(3)
                .clearFilters()
                .setRegion( if(isPositionRight) bitmap.width - iconWidth else 0, 0, bitmap.width, iconWidth)
                .generate { palette ->
                    val dark = when (isDark(palette)) {
                        LIGHTNESS_DARK -> true
                        else -> false
                    }
                    isDark(dark)
                }
        }

        fun getColorPalette(bitmap: Bitmap, position: Rect) : Int {
            val palette = Palette.from(bitmap)
                .maximumColorCount(3)
                .clearFilters()
                .setRegion(position.left, position.top, position.right, position.bottom)
                .generate()

            var mostPopulous: Swatch? = null
            if (palette != null) {
                for (swatch in palette.swatches) {
                    if (mostPopulous == null || swatch.population > mostPopulous.population) {
                        mostPopulous = swatch
                    }
                }
            }

            mostPopulous ?: return android.graphics.Color.WHITE
            return mostPopulous.rgb
        }

        fun getColor(exadecimal: String) : Int = android.graphics.Color.parseColor(exadecimal)
        fun getColor(@ColorInt color: Int) : String = String.format("#%06X", (0xFFFFFF and color))

        fun randomColor(): Int {
            val red = Random.nextInt(100, 256)
            val green = Random.nextInt(100, 256)
            val blue = Random.nextInt(100, 256)

            return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
        }
    }
}

class PopupUtil {
    companion object PopupUtils {
        fun onGlobalLayout(view: View, runnable: Runnable) {
            val listener: OnGlobalLayoutListener = object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    runnable.run()
                }
            }
            view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        }

        fun googleTranslate(context: Context, text: String) {
            try {
                val intent = Intent()
                intent.setAction(Intent.ACTION_SEND)
                intent.putExtra(Intent.EXTRA_TEXT, text)
                intent.putExtra("key_text_input", text)
                intent.putExtra("key_text_output", "")
                intent.putExtra("key_language_from", "en")
                intent.putExtra("key_language_to", "")
                intent.putExtra("key_suggest_translation", "")
                intent.putExtra("key_from_floating_window", false)
                intent.setComponent(
                    ComponentName(
                        "com.google.android.apps.translate",
                        "com.google.android.apps.translate.TranslateActivity"
                    )
                )
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, context.getString(R.string.alert_error_call_google_translate), Toast.LENGTH_SHORT).show()
            }
        }

    }
}

class ListUtil {
    companion object ListUtils {
        inline fun <T, R> Iterable<T>.mapToSet(transform: (T) -> R): Set<R> = mapTo(HashSet(), transform)

        private val delimiters = listOf(';',',')
        fun listFromString(value : String, delimiter: Char = ';') : List<String> = if (value.isEmpty()) listOf() else value.split(delimiter).filter { it.isNotEmpty() }

        fun listFromString(value : String) : List<String> {
            var list = listOf<String>()
            for (delimiter in delimiters)
                if (value.contains(delimiter)){
                    list = listFromString(value, delimiter)
                    break
                }
            return list
        }

    }
}

class AdapterUtil {
    companion object AdapterUtils {
        private var mIsLandscape: Boolean = false
        private val mMangaCardSize = mutableMapOf<LibraryMangaType, Pair<Int, Int>>()
        private val mBookCardSize = mutableMapOf<LibraryBookType, Pair<Int, Int>>()

        private fun validLandscape(isLandscape: Boolean) {
            if (mIsLandscape != isLandscape) {
                mIsLandscape = isLandscape
                mMangaCardSize.clear()
                mBookCardSize.clear()
            }
        }

        private fun setMangaCardSize(context: Context, type: LibraryMangaType) : Pair<Int, Int> {
            val width = when (type) {
                LibraryMangaType.GRID_SMALL -> context.resources.getDimension(R.dimen.manga_grid_card_layout_width_small).toInt()
                LibraryMangaType.GRID_BIG -> context.resources.getDimension(R.dimen.manga_grid_card_layout_width_big).toInt()
                LibraryMangaType.SEPARATOR_BIG -> context.resources.getDimension(R.dimen.manga_separator_grid_card_layout_width_big).toInt()
                LibraryMangaType.GRID_MEDIUM -> context.resources.getDimension(if (mIsLandscape) R.dimen.manga_grid_card_layout_width_landscape_medium else R.dimen.manga_grid_card_layout_width_medium).toInt()
                LibraryMangaType.SEPARATOR_MEDIUM -> context.resources.getDimension(if (mIsLandscape) R.dimen.manga_separator_grid_card_layout_width_landscape_medium else R.dimen.manga_separator_grid_card_layout_width_medium).toInt()
                LibraryMangaType.LINE -> -1
            }

            val height = when (type) {
                LibraryMangaType.GRID_SMALL -> context.resources.getDimension(R.dimen.manga_grid_card_layout_height_small).toInt()
                LibraryMangaType.GRID_BIG -> context.resources.getDimension(R.dimen.manga_grid_card_layout_height_big).toInt()
                LibraryMangaType.SEPARATOR_BIG -> context.resources.getDimension(R.dimen.manga_separator_grid_card_layout_height_big).toInt()
                LibraryMangaType.GRID_MEDIUM -> context.resources.getDimension(if (mIsLandscape) R.dimen.manga_grid_card_layout_height_landscape_medium else R.dimen.manga_grid_card_layout_height_medium).toInt()
                LibraryMangaType.SEPARATOR_MEDIUM -> context.resources.getDimension(if (mIsLandscape) R.dimen.manga_separator_grid_card_layout_height_landscape_medium else R.dimen.manga_separator_grid_card_layout_height_medium).toInt()
                LibraryMangaType.LINE -> context.resources.getDimension(R.dimen.manga_line_card_layout_height).toInt()
            }

            val size = Pair(width, height)
            mMangaCardSize[type] = size
            return size
        }

        private fun setBookCardSize(context: Context, type: LibraryBookType) : Pair<Int, Int> {
            val width = when (type) {
                LibraryBookType.GRID_BIG -> context.resources.getDimension(R.dimen.book_grid_card_layout_width_big).toInt()
                LibraryBookType.SEPARATOR_BIG -> context.resources.getDimension(R.dimen.book_separator_grid_card_layout_width_big).toInt()
                LibraryBookType.GRID_MEDIUM -> context.resources.getDimension(if (mIsLandscape) R.dimen.book_grid_card_layout_width_landscape_medium else R.dimen.book_grid_card_layout_width_medium).toInt()
                LibraryBookType.SEPARATOR_MEDIUM -> context.resources.getDimension(if (mIsLandscape) R.dimen.book_separator_grid_card_layout_width_landscape_medium else R.dimen.book_separator_grid_card_layout_width_medium).toInt()
                LibraryBookType.LINE -> -1
            }

            val height = when (type) {
                LibraryBookType.GRID_BIG -> context.resources.getDimension(R.dimen.book_grid_card_layout_height_big).toInt()
                LibraryBookType.SEPARATOR_BIG -> context.resources.getDimension(R.dimen.book_separator_grid_card_layout_height_big).toInt()
                LibraryBookType.GRID_MEDIUM -> context.resources.getDimension(if (mIsLandscape) R.dimen.book_grid_card_layout_height_landscape_medium else R.dimen.book_grid_card_layout_height_medium).toInt()
                LibraryBookType.SEPARATOR_MEDIUM -> context.resources.getDimension(if (mIsLandscape) R.dimen.book_separator_grid_card_layout_height_landscape_medium else R.dimen.book_separator_grid_card_layout_height_medium).toInt()
                LibraryBookType.LINE -> context.resources.getDimension(R.dimen.book_line_card_layout_height).toInt()
            }

            val size = Pair(width, height)
            mBookCardSize[type] = size
            return size
        }

        fun getMangaCardSize(context: Context, type: LibraryMangaType, isLandscape: Boolean) : Pair<Int, Int> {
            validLandscape(isLandscape)
            return if (mMangaCardSize.contains(type)) mMangaCardSize[type]!! else setMangaCardSize(context, type)
        }
        fun getBookCardSize(context: Context, type: LibraryBookType, isLandscape: Boolean) : Pair<Int, Int> {
            validLandscape(isLandscape)
            return if (mBookCardSize.contains(type)) mBookCardSize[type]!! else setBookCardSize(context, type)
        }

    }
}


class TouchUtil {
    companion object TouchUtils {

        fun setDefault(context: Context, type: Type) {
            val sharedPreferences = GeneralConsts.getSharedPreferences(context)
            with(sharedPreferences.edit()) {
                when (type) {
                    Type.MANGA -> {
                        this.putString(
                            GeneralConsts.KEYS.TOUCH.MANGA_TOP,
                            TouchScreen.TOUCH_SHARE_IMAGE.toString()
                        )

                        this.putString(
                            GeneralConsts.KEYS.TOUCH.MANGA_TOP_RIGHT,
                            TouchScreen.TOUCH_ASPECT_FIT.toString()
                        )

                        this.putString(
                            GeneralConsts.KEYS.TOUCH.MANGA_TOP_LEFT,
                            TouchScreen.TOUCH_FIT_WIDTH.toString()
                        )

                        this.putString(
                            GeneralConsts.KEYS.TOUCH.MANGA_LEFT,
                            TouchScreen.TOUCH_PREVIOUS_PAGE.toString()
                        )

                        this.putString(
                            GeneralConsts.KEYS.TOUCH.MANGA_RIGHT,
                            TouchScreen.TOUCH_NEXT_PAGE.toString()
                        )

                        this.putString(
                            GeneralConsts.KEYS.TOUCH.MANGA_BOTTOM,
                            TouchScreen.TOUCH_CHAPTER_LIST.toString()
                        )

                        this.putString(
                            GeneralConsts.KEYS.TOUCH.MANGA_BOTTOM_LEFT,
                            TouchScreen.TOUCH_PREVIOUS_FILE.toString()
                        )

                        this.putString(
                            GeneralConsts.KEYS.TOUCH.MANGA_BOTTOM_RIGHT,
                            TouchScreen.TOUCH_NEXT_FILE.toString()
                        )
                    }
                    Type.BOOK -> {
                        this.putString(
                            GeneralConsts.KEYS.TOUCH.BOOK_TOP,
                            TouchScreen.TOUCH_PAGE_MARK.toString()
                        )

                        this.putString(
                            GeneralConsts.KEYS.TOUCH.BOOK_TOP_RIGHT,
                            TouchScreen.TOUCH_NEXT_PAGE.toString()
                        )

                        this.putString(
                            GeneralConsts.KEYS.TOUCH.BOOK_TOP_LEFT,
                            TouchScreen.TOUCH_PREVIOUS_PAGE.toString()
                        )

                        this.putString(
                            GeneralConsts.KEYS.TOUCH.BOOK_LEFT,
                            TouchScreen.TOUCH_PREVIOUS_PAGE.toString()
                        )

                        this.putString(
                            GeneralConsts.KEYS.TOUCH.BOOK_RIGHT,
                            TouchScreen.TOUCH_NEXT_PAGE.toString()
                        )

                        this.putString(
                            GeneralConsts.KEYS.TOUCH.BOOK_BOTTOM,
                            TouchScreen.TOUCH_CHAPTER_LIST.toString()
                        )

                        this.putString(
                            GeneralConsts.KEYS.TOUCH.BOOK_BOTTOM_LEFT,
                            TouchScreen.TOUCH_PREVIOUS_FILE.toString()
                        )

                        this.putString(
                            GeneralConsts.KEYS.TOUCH.BOOK_BOTTOM_RIGHT,
                            TouchScreen.TOUCH_NEXT_FILE.toString()
                        )
                    }
                }

                this.commit()
            }
        }

        fun getTouch(context: Context, type: Type) : Map<Position, TouchScreen> {
            val touch = mutableMapOf<Position, TouchScreen>()
            val sharedPreferences = GeneralConsts.getSharedPreferences(context)
            when (type) {
                Type.MANGA -> {
                    touch[Position.TOP] = TouchScreen.valueOf(
                        sharedPreferences.getString(
                            GeneralConsts.KEYS.TOUCH.MANGA_TOP,
                            TouchScreen.TOUCH_SHARE_IMAGE.toString()
                        )!!
                    )
                    touch[Position.CORNER_TOP_RIGHT] = TouchScreen.valueOf(
                        sharedPreferences.getString(
                            GeneralConsts.KEYS.TOUCH.MANGA_TOP_RIGHT,
                            TouchScreen.TOUCH_ASPECT_FIT.toString()
                        )!!
                    )
                    touch[Position.CORNER_TOP_LEFT] = TouchScreen.valueOf(
                        sharedPreferences.getString(
                            GeneralConsts.KEYS.TOUCH.MANGA_TOP_LEFT,
                            TouchScreen.TOUCH_FIT_WIDTH.toString()
                        )!!
                    )
                    touch[Position.LEFT] = TouchScreen.valueOf(
                        sharedPreferences.getString(
                            GeneralConsts.KEYS.TOUCH.MANGA_LEFT,
                            TouchScreen.TOUCH_PREVIOUS_PAGE.toString()
                        )!!
                    )
                    touch[Position.RIGHT] = TouchScreen.valueOf(
                        sharedPreferences.getString(
                            GeneralConsts.KEYS.TOUCH.MANGA_RIGHT,
                            TouchScreen.TOUCH_NEXT_PAGE.toString()
                        )!!
                    )
                    touch[Position.BOTTOM] = TouchScreen.valueOf(
                        sharedPreferences.getString(
                            GeneralConsts.KEYS.TOUCH.MANGA_BOTTOM,
                            TouchScreen.TOUCH_CHAPTER_LIST.toString()
                        )!!
                    )
                    touch[Position.CORNER_BOTTOM_LEFT] = TouchScreen.valueOf(
                        sharedPreferences.getString(
                            GeneralConsts.KEYS.TOUCH.MANGA_BOTTOM_LEFT,
                            TouchScreen.TOUCH_PREVIOUS_FILE.toString()
                        )!!
                    )
                    touch[Position.CORNER_BOTTOM_RIGHT] = TouchScreen.valueOf(
                        sharedPreferences.getString(
                            GeneralConsts.KEYS.TOUCH.MANGA_BOTTOM_RIGHT,
                            TouchScreen.TOUCH_NEXT_FILE.toString()
                        )!!
                    )
                }
                Type.BOOK -> {
                    touch[Position.TOP] = TouchScreen.valueOf(
                        sharedPreferences.getString(
                            GeneralConsts.KEYS.TOUCH.BOOK_TOP,
                            TouchScreen.TOUCH_PAGE_MARK.toString()
                        )!!
                    )
                    touch[Position.CORNER_TOP_RIGHT] = TouchScreen.valueOf(
                        sharedPreferences.getString(
                            GeneralConsts.KEYS.TOUCH.BOOK_TOP_RIGHT,
                            TouchScreen.TOUCH_NOT_ASSIGNED.toString()
                        )!!
                    )
                    touch[Position.CORNER_TOP_LEFT] = TouchScreen.valueOf(
                        sharedPreferences.getString(
                            GeneralConsts.KEYS.TOUCH.BOOK_TOP_LEFT,
                            TouchScreen.TOUCH_NOT_ASSIGNED.toString()
                        )!!
                    )
                    touch[Position.LEFT] = TouchScreen.valueOf(
                        sharedPreferences.getString(
                            GeneralConsts.KEYS.TOUCH.BOOK_LEFT,
                            TouchScreen.TOUCH_PREVIOUS_PAGE.toString()
                        )!!
                    )
                    touch[Position.RIGHT] = TouchScreen.valueOf(
                        sharedPreferences.getString(
                            GeneralConsts.KEYS.TOUCH.BOOK_RIGHT,
                            TouchScreen.TOUCH_NEXT_PAGE.toString()
                        )!!
                    )
                    touch[Position.BOTTOM] = TouchScreen.valueOf(
                        sharedPreferences.getString(
                            GeneralConsts.KEYS.TOUCH.BOOK_BOTTOM,
                            TouchScreen.TOUCH_CHAPTER_LIST.toString()
                        )!!
                    )
                    touch[Position.CORNER_BOTTOM_LEFT] = TouchScreen.valueOf(
                        sharedPreferences.getString(
                            GeneralConsts.KEYS.TOUCH.BOOK_BOTTOM_LEFT,
                            TouchScreen.TOUCH_PREVIOUS_FILE.toString()
                        )!!
                    )
                    touch[Position.CORNER_BOTTOM_RIGHT] = TouchScreen.valueOf(
                        sharedPreferences.getString(
                            GeneralConsts.KEYS.TOUCH.BOOK_BOTTOM_RIGHT,
                            TouchScreen.TOUCH_NEXT_FILE.toString()
                        )!!
                    )
                }
            }

            touch[Position.CENTER] = TouchScreen.TOUCH_NOT_IMPLEMENTED
            return touch.toMap()
        }

    }
}