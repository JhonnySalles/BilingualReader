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
import android.graphics.Outline
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Choreographer
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
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
import br.com.fenix.bilingualreader.model.entity.Separator
import br.com.fenix.bilingualreader.model.enums.AlignmentLayoutType
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.Filter
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.HistoryType
import br.com.fenix.bilingualreader.model.enums.LibraryBookType
import br.com.fenix.bilingualreader.model.enums.LibraryMangaType
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.model.enums.Position
import br.com.fenix.bilingualreader.model.enums.ThemeMode
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.model.enums.TouchScreen
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.ocr.ImageProcess
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.GlassSetup
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur
import br.com.fenix.bilingualreader.view.components.GlassRenderScheduler
import android.os.ParcelFileDescriptor
import org.beyka.tiffbitmapfactory.TiffBitmapFactory
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

            return name.trim()
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

            val normalizedPath = path.trimEnd('/', '\\')
            var folder = if (normalizedPath.contains('/', true))
                normalizedPath.substringAfterLast('/')
            else if (normalizedPath.contains('\\', true))
                normalizedPath.substringAfterLast('\\')
            else
                normalizedPath

            folder = if (folder.contains("capitulo", true))
                folder.substringAfterLast("capitulo").replace("capitulo", "", true)
            else if (folder.contains("capítulo", true))
                folder.substringAfterLast("capítulo").replace("capítulo", "", true)
            else folder

            return folder.trim().toFloatOrNull() ?: -1f
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

        fun getOptimalViewCacheSize(context: Context): Int {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            return if (memoryInfo.lowMemory || memoryInfo.availMem < 1024 * 1024 * 500) {
                5
            } else {
                25
            }
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
                .matches(Regex(".*\\.(jpg|jpeg|bmp|gif|png|webp|avif|heic|heif|jxl|tiff|tif|pcx|jpf|jp2|j2k|jpx|pbm|pgm|ppm|pnm|iff)$"))
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

        fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
            var inSampleSize = 1
            if (reqWidth > 0 && reqHeight > 0 && (height > reqHeight || width > reqWidth)) {
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

        fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
            return calculateInSampleSize(options.outWidth, options.outHeight, reqWidth, reqHeight)
        }

        private fun isJp2(bytes: ByteArray): Boolean {
            if (bytes.size < 12) return false
            val isJp2Box = bytes[0] == 0x00.toByte() && bytes[1] == 0x00.toByte() &&
                           bytes[2] == 0x00.toByte() && bytes[3] == 0x0C.toByte() &&
                           bytes[4] == 0x6A.toByte() && bytes[5] == 0x50.toByte() &&
                           bytes[6] == 0x20.toByte() && bytes[7] == 0x20.toByte()
            val isJ2kStream = bytes[0] == 0xFF.toByte() && bytes[1] == 0x4F.toByte() &&
                              bytes[2] == 0xFF.toByte() && bytes[3] == 0x51.toByte()
            return isJp2Box || isJ2kStream
        }

        private fun isTiff(bytes: ByteArray): Boolean {
            if (bytes.size < 4) return false
            val le = bytes[0] == 0x49.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x2A.toByte() && bytes[3] == 0x00.toByte()
            val be = bytes[0] == 0x4D.toByte() && bytes[1] == 0x4D.toByte() && bytes[2] == 0x00.toByte() && bytes[3] == 0x2A.toByte()
            return le || be
        }

        /** Non-deprecated TIFF decode path (decodeFile is deprecated since Android Q). */
        private fun decodeTiffFile(file: File, options: TiffBitmapFactory.Options): Bitmap? {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                return TiffBitmapFactory.decodeFileDescriptor(pfd.fd, options)
            }
        }

        private fun isNativeFormat(bytes: ByteArray): Boolean {
            if (bytes.size < 4) return false
            val isJpeg = bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
            val isPng = bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
            val isGif = bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte()
            val isWebp = bytes.size >= 12 && bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
                         bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() && bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
            val isBmp = bytes[0] == 0x42.toByte() && bytes[1] == 0x4D.toByte()
            return isJpeg || isPng || isGif || isWebp || isBmp
        }

        fun decodeByteArray(
            bytes: ByteArray,
            reqWidth: Int = 0,
            reqHeight: Int = 0
        ): Bitmap? {
            if (bytes.isEmpty()) return null

            // 1. JXL
            try {
                if (com.awxkee.jxlcoder.JxlCoder.isJXL(bytes)) {
                    val jxlCoder = com.awxkee.jxlcoder.JxlCoder()
                    val w = if (reqWidth > 0) reqWidth else 0
                    val h = if (reqHeight > 0) reqHeight else 0
                    if (w > 0 && h > 0) {
                        return jxlCoder.decodeSampled(bytes, w, h)
                    }
                    return jxlCoder.decode(bytes)
                }
            } catch (ignored: Throwable) {}

            // 2. AVIF / HEIF
            try {
                val heifCoder = com.radzivon.bartoshyk.avif.coder.HeifCoder()
                if (heifCoder.isAvif(bytes) || heifCoder.isHeif(bytes)) {
                    return heifCoder.decode(bytes)
                }
            } catch (ignored: Throwable) {}

            // 3. JPEG 2000
            if (isJp2(bytes)) {
                try {
                    val jp2Bitmap = com.gemalto.jp2.JP2Decoder(bytes).decode()
                    if (jp2Bitmap != null) return jp2Bitmap
                } catch (ignored: Throwable) {}
            }

            // 4. TIFF
            if (isTiff(bytes)) {
                try {
                    val tempFile = File.createTempFile("tiff_", ".tif")
                    try {
                        tempFile.writeBytes(bytes)
                        val tiffOptions = TiffBitmapFactory.Options()
                        if (reqWidth > 0 && reqHeight > 0) {
                            tiffOptions.inJustDecodeBounds = true
                            decodeTiffFile(tempFile, tiffOptions)
                            if (tiffOptions.outWidth > 0 && tiffOptions.outHeight > 0) {
                                tiffOptions.inSampleSize = calculateInSampleSize(tiffOptions.outWidth, tiffOptions.outHeight, reqWidth, reqHeight)
                            }
                            tiffOptions.inJustDecodeBounds = false
                        }
                        val tiffBitmap = decodeTiffFile(tempFile, tiffOptions)
                        if (tiffBitmap != null) return tiffBitmap
                    } finally {
                        tempFile.delete()
                    }
                } catch (ignored: Throwable) {}
            }

            // 5. PCX
            val pcxBitmap = decodePcx(bytes)
            if (pcxBitmap != null) return pcxBitmap

            // 6. PBM / PGM / PPM
            val pbmBitmap = decodePbm(bytes)
            if (pbmBitmap != null) return pbmBitmap

            // 7. IFF ILBM
            val iffBitmap = decodeIff(bytes)
            if (iffBitmap != null) return iffBitmap

            // 8. Standard Android Decoders
            val options = BitmapFactory.Options()
            if (reqWidth > 0 && reqHeight > 0) {
                options.inJustDecodeBounds = true
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, reqWidth, reqHeight)
                options.inJustDecodeBounds = false
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            if (bitmap != null) return bitmap

            if (isNativeFormat(bytes) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val decoded = decodeBitmapNative(bytes, reqWidth, reqHeight)
                if (decoded != null) return decoded
            }

            return null
        }

        private fun decodePcx(bytes: ByteArray): Bitmap? {
            if (bytes.size < 128 || bytes[0] != 0x0A.toByte()) return null
            try {
                val bitsPerPixel = bytes[3].toInt() and 0xFF
                val xMin = (bytes[4].toInt() and 0xFF) or ((bytes[5].toInt() and 0xFF) shl 8)
                val yMin = (bytes[6].toInt() and 0xFF) or ((bytes[7].toInt() and 0xFF) shl 8)
                val xMax = (bytes[8].toInt() and 0xFF) or ((bytes[9].toInt() and 0xFF) shl 8)
                val yMax = (bytes[10].toInt() and 0xFF) or ((bytes[11].toInt() and 0xFF) shl 8)
                val width = xMax - xMin + 1
                val height = yMax - yMin + 1
                val numPlanes = bytes[65].toInt() and 0xFF
                val bytesPerLine = (bytes[66].toInt() and 0xFF) or ((bytes[67].toInt() and 0xFF) shl 8)

                if (width <= 0 || height <= 0 || width > 8192 || height > 8192) return null

                val palette = IntArray(256)
                if (bytes.size >= width * height && bytes[bytes.size - 769] == 0x0C.toByte()) {
                    val paletteOffset = bytes.size - 768
                    for (i in 0 until 256) {
                        val r = bytes[paletteOffset + i * 3].toInt() and 0xFF
                        val g = bytes[paletteOffset + i * 3 + 1].toInt() and 0xFF
                        val b = bytes[paletteOffset + i * 3 + 2].toInt() and 0xFF
                        palette[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                    }
                }

                val pixels = IntArray(width * height)
                var scanOffset = 128
                var row = 0
                val scanLine = ByteArray(bytesPerLine * numPlanes)

                while (row < height && scanOffset < bytes.size) {
                    var lineIdx = 0
                    while (lineIdx < scanLine.size && scanOffset < bytes.size) {
                        val b = bytes[scanOffset++].toInt() and 0xFF
                        if ((b and 0xC0) == 0xC0) {
                            val count = b and 0x3F
                            if (scanOffset >= bytes.size) break
                            val valByte = bytes[scanOffset++]
                            for (c in 0 until count) {
                                if (lineIdx < scanLine.size) scanLine[lineIdx++] = valByte
                            }
                        } else {
                            scanLine[lineIdx++] = b.toByte()
                        }
                    }

                    if (numPlanes == 1 && bitsPerPixel == 8) {
                        for (col in 0 until width) {
                            val colorIdx = scanLine[col].toInt() and 0xFF
                            pixels[row * width + col] = palette[colorIdx]
                        }
                    } else if (numPlanes == 3 && bitsPerPixel == 8) {
                        for (col in 0 until width) {
                            val r = scanLine[col].toInt() and 0xFF
                            val g = scanLine[bytesPerLine + col].toInt() and 0xFF
                            val b = scanLine[bytesPerLine * 2 + col].toInt() and 0xFF
                            pixels[row * width + col] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                        }
                    }
                    row++
                }
                return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
            } catch (e: Exception) {
                return null
            }
        }

        private fun decodePbm(bytes: ByteArray): Bitmap? {
            if (bytes.size < 10 || bytes[0] != 'P'.code.toByte()) return null
            val magic = bytes[1].toInt().toChar()
            if (magic !in listOf('1', '2', '3', '4', '5', '6')) return null

            try {
                var idx = 2
                fun skipWhitespaceAndComments() {
                    while (idx < bytes.size) {
                        val b = bytes[idx].toInt().toChar()
                        if (b == '#') {
                            while (idx < bytes.size && bytes[idx].toInt().toChar() != '\n') idx++
                        } else if (b.isWhitespace()) {
                            idx++
                        } else break
                    }
                }

                fun readToken(): String {
                    skipWhitespaceAndComments()
                    val start = idx
                    while (idx < bytes.size && !bytes[idx].toInt().toChar().isWhitespace() && bytes[idx].toInt().toChar() != '#') idx++
                    return String(bytes, start, idx - start)
                }

                val widthStr = readToken()
                val heightStr = readToken()
                if (widthStr.isEmpty() || heightStr.isEmpty()) return null
                val width = widthStr.toIntOrNull() ?: return null
                val height = heightStr.toIntOrNull() ?: return null
                if (width <= 0 || height <= 0 || width > 8192 || height > 8192) return null

                val maxVal = if (magic in listOf('2', '3', '5', '6')) readToken().toIntOrNull() ?: 255 else 1
                skipWhitespaceAndComments()

                val pixels = IntArray(width * height)

                if (magic == '4') { // Binary PBM (1 bit per pixel)
                    var pixelIdx = 0
                    while (pixelIdx < width * height && idx < bytes.size) {
                        val b = bytes[idx++].toInt() and 0xFF
                        for (bit in 7 downTo 0) {
                            if (pixelIdx < width * height) {
                                val isWhite = ((b shl (7 - bit)) and 0x80) == 0
                                pixels[pixelIdx++] = if (isWhite) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
                                if (pixelIdx % width == 0) break
                            }
                        }
                    }
                } else if (magic == '5') { // Binary PGM (Grayscale)
                    var pixelIdx = 0
                    while (pixelIdx < width * height && idx < bytes.size) {
                        val v = (bytes[idx++].toInt() and 0xFF) * 255 / maxVal
                        pixels[pixelIdx++] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
                    }
                } else if (magic == '6') { // Binary PPM (RGB)
                    var pixelIdx = 0
                    while (pixelIdx < width * height && idx + 2 < bytes.size) {
                        val r = (bytes[idx++].toInt() and 0xFF) * 255 / maxVal
                        val g = (bytes[idx++].toInt() and 0xFF) * 255 / maxVal
                        val b = (bytes[idx++].toInt() and 0xFF) * 255 / maxVal
                        pixels[pixelIdx++] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                    }
                }

                return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
            } catch (e: Exception) {
                return null
            }
        }

        private fun decodeIff(bytes: ByteArray): Bitmap? {
            if (bytes.size < 12) return null
            val form = String(bytes, 0, 4, Charsets.US_ASCII)
            val ilbm = String(bytes, 8, 4, Charsets.US_ASCII)
            if (form != "FORM" || ilbm != "ILBM") return null

            try {
                var offset = 12
                var width = 0
                var height = 0
                var numPlanes = 0
                var compression = 0
                val palette = IntArray(256)
                var bodyBytes: ByteArray? = null

                while (offset + 8 <= bytes.size) {
                    val chunkType = String(bytes, offset, 4, Charsets.US_ASCII)
                    val chunkSize = ((bytes[offset + 4].toInt() and 0xFF) shl 24) or
                            ((bytes[offset + 5].toInt() and 0xFF) shl 16) or
                            ((bytes[offset + 6].toInt() and 0xFF) shl 8) or
                            (bytes[offset + 7].toInt() and 0xFF)
                    val dataOffset = offset + 8
                    if (dataOffset + chunkSize > bytes.size) break

                    when (chunkType) {
                        "BMHD" -> {
                            width = ((bytes[dataOffset].toInt() and 0xFF) shl 8) or (bytes[dataOffset + 1].toInt() and 0xFF)
                            height = ((bytes[dataOffset + 2].toInt() and 0xFF) shl 8) or (bytes[dataOffset + 3].toInt() and 0xFF)
                            numPlanes = bytes[dataOffset + 8].toInt() and 0xFF
                            compression = bytes[dataOffset + 10].toInt() and 0xFF
                        }
                        "CMAP" -> {
                            val colorCount = chunkSize / 3
                            for (i in 0 until minOf(256, colorCount)) {
                                val r = bytes[dataOffset + i * 3].toInt() and 0xFF
                                val g = bytes[dataOffset + i * 3 + 1].toInt() and 0xFF
                                val b = bytes[dataOffset + i * 3 + 2].toInt() and 0xFF
                                palette[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                            }
                        }
                        "BODY" -> {
                            bodyBytes = bytes.copyOfRange(dataOffset, dataOffset + chunkSize)
                        }
                    }
                    offset += 8 + chunkSize + (chunkSize % 2)
                }

                if (width <= 0 || height <= 0 || bodyBytes == null) return null
                val pixels = IntArray(width * height)
                val rowBytes = (width + 15) / 16 * 2
                var bodyIdx = 0

                if (compression == 0) {
                    for (y in 0 until height) {
                        val planeData = Array(numPlanes) { ByteArray(rowBytes) }
                        for (p in 0 until numPlanes) {
                            if (bodyIdx + rowBytes <= bodyBytes.size) {
                                System.arraycopy(bodyBytes, bodyIdx, planeData[p], 0, rowBytes)
                                bodyIdx += rowBytes
                            }
                        }
                        for (x in 0 until width) {
                            var colorIdx = 0
                            val bytePos = x / 8
                            val bitPos = 7 - (x % 8)
                            for (p in 0 until numPlanes) {
                                val bit = (planeData[p][bytePos].toInt() shr bitPos) and 1
                                colorIdx = colorIdx or (bit shl p)
                            }
                            pixels[y * width + x] = palette[colorIdx]
                        }
                    }
                    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
                }
                return null
            } catch (e: Exception) {
                return null
            }
        }

        fun decodeInputStream(
            stream: InputStream,
            reqWidth: Int = 0,
            reqHeight: Int = 0
        ): Bitmap? {
            val bytes = Util.toByteArray(stream) ?: return null
            return decodeByteArray(bytes, reqWidth, reqHeight)
        }

        fun decodeFile(
            path: String,
            reqWidth: Int = 0,
            reqHeight: Int = 0
        ): Bitmap? {
            val file = File(path)
            if (!file.exists()) return null
            return try {
                val bytes = file.readBytes()
                decodeByteArray(bytes, reqWidth, reqHeight)
            } catch (e: Exception) {
                null
            }
        }

        fun decodeFile(
            file: File,
            reqWidth: Int = 0,
            reqHeight: Int = 0
        ): Bitmap? = decodeFile(file.absolutePath, reqWidth, reqHeight)

        @RequiresApi(Build.VERSION_CODES.P)
        fun decodeBitmapNative(bytes: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap? {
            return try {
                val source = android.graphics.ImageDecoder.createSource(java.nio.ByteBuffer.wrap(bytes))
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                    if (reqWidth > 0 && reqHeight > 0) {
                        val sample = ImageUtil.calculateInSampleSize(info.size.width, info.size.height, reqWidth, reqHeight)
                        if (sample > 1) decoder.setTargetSampleSize(sample)
                    }
                }
            } catch (ignored: Throwable) {
                null
            }
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

        fun decodeImageBase64(image: String, reqWidth: Int = 0, reqHeight: Int = 0): Bitmap? {
            return try {
                val imageBytes = android.util.Base64.decode(image, android.util.Base64.DEFAULT)
                if (imageBytes == null || imageBytes.isEmpty()) return null
                decodeByteArray(imageBytes, reqWidth, reqHeight)
            } catch (e: Exception) {
                null
            } catch (e: OutOfMemoryError) {
                null
            }
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
            val itemCover = ImageProcess.toGrayscale(image.copy(Bitmap.Config.ARGB_8888, true))
            val canvas = Canvas(itemCover)

            val effect = when(type) {
                Type.MANGA -> AppCompatResources.getDrawable(context, R.mipmap.book_not_found_effect)
                Type.BOOK -> AppCompatResources.getDrawable(context, R.mipmap.book_not_found_effect)
            }

            effect?.setBounds(0, 0, image.width, image.height)
            effect?.draw(canvas)
            return itemCover
        }

        fun combineImagesHorizontally(bitmaps: List<Bitmap>): Bitmap? {
            if (bitmaps.isEmpty()) return null
            if (bitmaps.size == 1) return bitmaps[0]

            var width = 0
            var height = 0
            for (bmp in bitmaps) {
                width += bmp.width
                if (bmp.height > height) {
                    height = bmp.height
                }
            }

            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            var currentX = 0f
            for (bmp in bitmaps) {
                canvas.drawBitmap(bmp, currentX, 0f, null)
                currentX += bmp.width
            }
            return result
        }

        fun combineImagesVertically(bitmaps: List<Bitmap>, alignment: AlignmentLayoutType = AlignmentLayoutType.Left): Bitmap? {
            if (bitmaps.isEmpty()) return null
            if (bitmaps.size == 1) return bitmaps[0]

            var width = 0
            var height = 0
            for (bmp in bitmaps) {
                height += bmp.height
                if (bmp.width > width) {
                    width = bmp.width
                }
            }

            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            var currentY = 0f
            for (bmp in bitmaps) {
                val offsetX = when (alignment) {
                    AlignmentLayoutType.Right -> (width - bmp.width).toFloat()
                    AlignmentLayoutType.Center -> ((width - bmp.width) / 2f)
                    else -> 0f // Left and Justify
                }
                canvas.drawBitmap(bmp, offsetX, currentY, null)
                currentY += bmp.height
            }
            return result
        }

    }
}

class MenuUtil {
    companion object MenuUtils {

        fun setupToolbar(activity: Activity, toolbar: View?, blurTop: BlurView?, barLayout: View?) {
            val sharedPreferences = GeneralConsts.getSharedPreferences(activity)
            val isGlass = sharedPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
            blurTop?.setBlurEnabled(isGlass)

            val themeColor = activity.getColorFromAttr(R.attr.colorSurface)
            val isNight = activity.resources.getBoolean(R.bool.isNight)
            val alpha = if (isNight) 0xA9 else 0x73
            val translucentColor = ((themeColor and 0x00FFFFFF) or (alpha shl 24)).toInt()
            val solidColor = ((themeColor and 0x00FFFFFF) or (0xFF shl 24)).toInt()
            val cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 28f, activity.resources.displayMetrics)

            val topBg = if (isGlass) {
                GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(translucentColor)
                    cornerRadii = floatArrayOf(
                        0f, 0f,
                        0f, 0f,
                        cornerRadius, cornerRadius,
                        cornerRadius, cornerRadius
                    )
                }
            } else {
                val middleColor = ((themeColor and 0x00FFFFFF) or (0xB3 shl 24)).toInt() // 70% opacity
                val transparentColor = (themeColor and 0x00FFFFFF).toInt() // 0% opacity
                GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(solidColor, solidColor, middleColor, transparentColor)).apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadii = floatArrayOf(
                        0f, 0f,
                        0f, 0f,
                        cornerRadius, cornerRadius,
                        cornerRadius, cornerRadius
                    )
                }
            }
            blurTop?.background = topBg
            toolbar?.background = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            barLayout?.background = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        }

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

        fun applyThemeMode(context: Context): Boolean {
            val preferences = GeneralConsts.getSharedPreferences(context)
            val themeMode = ThemeMode.valueOf(preferences.getString(GeneralConsts.KEYS.THEME.THEME_MODE, ThemeMode.SYSTEM.toString())!!)
            val mode = when (themeMode) {
                ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(mode)
            return when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> context.resources.getBoolean(R.bool.isNight)
            }
        }

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
            WindowCompat.setDecorFitsSystemWindows(window, false)

            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            if (statusBarDrawable != null || statusBarColor != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                val background = statusBarDrawable ?: ColorDrawable(statusBarColor!!)
                window.setBackgroundDrawable(background)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }

            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.isAppearanceLightStatusBars = isLightStatus
            controller.isAppearanceLightNavigationBars = !isDarkTheme
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

class NavigationUtil {
    companion object NavigationUtils {
        fun Activity.overrideActivityTransitionCompat(enterAnim: Int, exitAnim: Int, isOpen: Boolean = true) {
            if (Build.VERSION.SDK_INT >= 34) {
                overrideActivityTransition(
                    if (isOpen) Activity.OVERRIDE_TRANSITION_OPEN else Activity.OVERRIDE_TRANSITION_CLOSE,
                    enterAnim, exitAnim
                )
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(enterAnim, exitAnim)
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
            val itemColor = ColorUtil.getColor(color)
            return highlightWordInText(html, contain, itemColor)
        }

        fun highlightWordInText(html: String, contain: String, color: String): String = replaceHtmlTags(html).replace(contain, "<font color=$color>$contain</font>")

        fun clearHighlightWordInText(html: String): String = replaceHtmlTags(html)

        fun getImageFromTag(html: String) = html.substringAfter("<img src=\"").substringBefore("\" />")

        fun getImagesFromTag(html: String): List<String> {
            val list = mutableListOf<String>()
            val regex = "<img src=\"([^\"]+)\"\\s*/?>".toRegex()
            val matches = regex.findAll(html)
            for (match in matches) {
                list.add(match.groupValues[1])
            }
            return list
        }

        fun hasBrBetweenImages(html: String): Boolean {
            val imgRegex = "<img[^>]*>".toRegex()
            val brRegex = "<br\\s*/?>".toRegex()
            val matches = imgRegex.findAll(html).toList()
            if (matches.size < 2) return false
            
            for (i in 0 until matches.size - 1) {
                val start = matches[i].range.last
                val end = matches[i+1].range.first
                val substring = html.substring(start, end)
                if (brRegex.containsMatchIn(substring)) {
                    return true
                }
            }
            return false
        }

        fun isOnlyImageOnHtml(html: String): Boolean = html.contains("< ?(img)[^>]*>".toRegex()) && replaceHtmlTags(html).trim().isEmpty()
    }
}

class AnimationUtil {
    companion object AnimationUtils {
        const val PROPERTY_NO_ANIMATION = "NO_ANIMATION"

        const val duration = 200L
        fun animatePopupOpen(activity: Activity, frame: FrameLayout, isVertical: Boolean = true, navigationColor: Boolean = true, ending: () -> (Unit) = {}) {
            GlassRenderScheduler.suspendFor(duration + 50L, "popupOpen")
            frame.visibility = View.VISIBLE
            if (isVertical) {
                if (navigationColor)
                    PopupUtil.updateNavigationBarColor(activity, true)

                val positionInitial = frame.translationY
                frame.translationY = positionInitial + 200F
                frame.animate()
                    .setDuration(duration)
                    .translationY(positionInitial)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            GlassRenderScheduler.requestUpdateAll()
                            ending()
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
                            GlassRenderScheduler.requestUpdateAll()
                            ending()
                        }
                    })
            }
        }

        fun animatePopupClose(activity: Activity, frame: FrameLayout, isVertical: Boolean = true, navigationColor: Boolean = true) {
            GlassRenderScheduler.suspendFor(duration + 50L, "popupClose")
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
                            GlassRenderScheduler.requestUpdateAll()

                            if (navigationColor)
                                PopupUtil.updateNavigationBarColor(activity, false)
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
                            GlassRenderScheduler.requestUpdateAll()
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
                .setRegion( if(isPositionRight) bitmap.width - iconWidth else 0, 0, bitmap.width, iconHeight)
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
            for (swatch in palette.swatches) {
                if (mostPopulous == null || swatch.population > mostPopulous.population) {
                    mostPopulous = swatch
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
        fun updateNavigationBarColor(activity: Activity, isOpened: Boolean) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val window = activity.window ?: return
                if (isOpened) {
                    val themeColor = activity.getColorFromAttr(R.attr.colorSurfaceVariant)
                    window.navigationBarColor = themeColor
                    val isDark = ColorUtils.calculateLuminance(themeColor) < 0.5
                    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = !isDark
                } else {
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                }
            }
        }

        fun setupPopupBackgrounds( activity: Activity, popupBottom: View?, popupBackground: BlurView?, customRootView: ViewGroup? = null) {
            val sharedPreferences = GeneralConsts.getSharedPreferences(activity)
            val isGlass = sharedPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)
            val themeColor = activity.getColorFromAttr(R.attr.colorSurfaceVariant)
            val isNight = activity.resources.getBoolean(R.bool.isNight)
            val cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 28f, activity.resources.displayMetrics)

            val finalColor = if (isGlass) {
                val alpha = if (isNight) 0xA9 else 0x73
                (themeColor and 0x00FFFFFF) or (alpha shl 24)
            } else {
                themeColor
            }

            popupBottom?.let { pb ->
                pb.background = null
                val bottomSheetBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(finalColor)
                    cornerRadii = floatArrayOf(
                        cornerRadius, cornerRadius,
                        cornerRadius, cornerRadius,
                        0f, 0f,
                        0f, 0f
                    )
                }
                if (!isGlass) {
                    val topInset = activity.resources.getDimensionPixelSize(R.dimen.popup_background_size)
                    pb.background = android.graphics.drawable.InsetDrawable(bottomSheetBg, 0, topInset, 0, 0)
                } else {
                    pb.background = bottomSheetBg
                }
                
                pb.clipToOutline = true
                pb.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(
                            0,
                            0,
                            view.width,
                            view.height + cornerRadius.toInt(),
                            cornerRadius
                        )
                    }
                }
            }

            popupBackground?.let { bg ->
                bg.background = null

                val headerBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(if (isGlass) android.graphics.Color.TRANSPARENT else (themeColor and 0x00FFFFFF) or (0x80 shl 24))
                    cornerRadii = floatArrayOf(cornerRadius, cornerRadius, cornerRadius, cornerRadius, 0f, 0f, 0f, 0f)
                }
                bg.background = headerBg
                
                bg.clipToOutline = true
                bg.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height + cornerRadius.toInt(), cornerRadius)
                    }
                }

                if (isGlass) {
                    val decorView = activity.window.decorView
                    val background = decorView.background ?: android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK)
                    val blurAlgorithm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) RenderEffectBlur() else RenderScriptBlur(activity)
                    val rootView = customRootView ?: decorView.findViewById<ViewGroup>(android.R.id.content)
                    GlassSetup.setupGlass(bg, rootView, blurAlgorithm)
                        .setFrameClearDrawable(background)
                        .setBlurRadius(15f)
                    bg.setBlurEnabled(true)
                    bg.setBlurAutoUpdate(false)
                } else {
                    bg.setBlurEnabled(false)
                }
            }
        }

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

        fun onPopupTouch(activity: Activity, popup: FrameLayout, sheet: BottomSheetBehavior<FrameLayout>, centerButton: View, navigationColor: Boolean = true) {
            val gesture = GestureDetector(activity, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (sheet.state == BottomSheetBehavior.STATE_COLLAPSED)
                        sheet.state = BottomSheetBehavior.STATE_EXPANDED
                    else
                        sheet.state = BottomSheetBehavior.STATE_COLLAPSED
                    return super.onSingleTapConfirmed(e)
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    return super.onDoubleTap(e)
                }
                override fun onLongPress(e: MotionEvent) {
                    super.onLongPress(e)
                    AnimationUtil.animatePopupClose(activity, popup, navigationColor = navigationColor)
                }

                override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                    if (e1 != null)
                        if (abs(e1.y - e2.y) > 150) {
                            if (e2.y > e1.y)
                                AnimationUtil.animatePopupClose(activity, popup)
                            else if (e2.y < e1.y)
                                sheet.state = BottomSheetBehavior.STATE_EXPANDED
                            return true
                        }
                    return super.onFling(e1, e2, velocityX, velocityY)
                }
            })

            centerButton.setOnTouchListener { view, event ->
                view.performClick()
                gesture.onTouchEvent(event)
                popup.parent.requestDisallowInterceptTouchEvent(true)
                true
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
        private val mHistoryCardSize = mutableMapOf<HistoryType, Pair<Int, Int>>()

        private fun validLandscape(isLandscape: Boolean) {
            if (mIsLandscape != isLandscape) {
                mIsLandscape = isLandscape
                mMangaCardSize.clear()
                mBookCardSize.clear()
                mHistoryCardSize.clear()
            }
        }

        private fun setMangaCardSize(context: Context, type: LibraryMangaType) : Pair<Int, Int> {
            val width = when (type) {
                LibraryMangaType.GRID_SMALL -> context.resources.getDimension(R.dimen.manga_grid_card_layout_width_small).toInt()
                LibraryMangaType.GRID_BIG -> context.resources.getDimension(R.dimen.manga_grid_card_layout_width_big).toInt()
                LibraryMangaType.SEPARATOR_BIG -> context.resources.getDimension(R.dimen.manga_separator_grid_card_layout_width_big).toInt()
                LibraryMangaType.GRID_MEDIUM -> context.resources.getDimension(if (mIsLandscape) R.dimen.manga_grid_card_layout_width_landscape_medium else R.dimen.manga_grid_card_layout_width_medium).toInt()
                LibraryMangaType.SEPARATOR_MEDIUM -> context.resources.getDimension(if (mIsLandscape) R.dimen.manga_separator_grid_card_layout_width_landscape_medium else R.dimen.manga_separator_grid_card_layout_width_medium).toInt()
                LibraryMangaType.LINE,
                LibraryMangaType.SEPARATOR_LINE,
                LibraryMangaType.SEPARATOR_CAROUSEL -> -1
            }

            val height = when (type) {
                LibraryMangaType.GRID_SMALL -> context.resources.getDimension(R.dimen.manga_grid_card_layout_height_small).toInt()
                LibraryMangaType.GRID_BIG -> context.resources.getDimension(R.dimen.manga_grid_card_layout_height_big).toInt()
                LibraryMangaType.SEPARATOR_BIG -> context.resources.getDimension(R.dimen.manga_separator_grid_card_layout_height_big).toInt()
                LibraryMangaType.GRID_MEDIUM -> context.resources.getDimension(if (mIsLandscape) R.dimen.manga_grid_card_layout_height_landscape_medium else R.dimen.manga_grid_card_layout_height_medium).toInt()
                LibraryMangaType.SEPARATOR_MEDIUM -> context.resources.getDimension(if (mIsLandscape) R.dimen.manga_separator_grid_card_layout_height_landscape_medium else R.dimen.manga_separator_grid_card_layout_height_medium).toInt()
                LibraryMangaType.LINE,
                LibraryMangaType.SEPARATOR_LINE -> context.resources.getDimension(R.dimen.manga_line_card_layout_height).toInt()
                LibraryMangaType.SEPARATOR_CAROUSEL -> -1
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
                LibraryBookType.LINE,
                LibraryBookType.SEPARATOR_LINE,
                LibraryBookType.SEPARATOR_CAROUSEL -> -1
            }

            val height = when (type) {
                LibraryBookType.GRID_BIG -> context.resources.getDimension(R.dimen.book_grid_card_layout_height_big).toInt()
                LibraryBookType.SEPARATOR_BIG -> context.resources.getDimension(R.dimen.book_separator_grid_card_layout_height_big).toInt()
                LibraryBookType.GRID_MEDIUM -> context.resources.getDimension(if (mIsLandscape) R.dimen.book_grid_card_layout_height_landscape_medium else R.dimen.book_grid_card_layout_height_medium).toInt()
                LibraryBookType.SEPARATOR_MEDIUM -> context.resources.getDimension(if (mIsLandscape) R.dimen.book_separator_grid_card_layout_height_landscape_medium else R.dimen.book_separator_grid_card_layout_height_medium).toInt()
                LibraryBookType.LINE,
                LibraryBookType.SEPARATOR_LINE -> context.resources.getDimension(R.dimen.book_line_card_layout_height).toInt()
                LibraryBookType.SEPARATOR_CAROUSEL -> -1
            }

            val size = Pair(width, height)
            mBookCardSize[type] = size
            return size
        }

        fun getBookSeparator(context: Context, order: Order, book: Book): Separator {
            val favorite = context.getString(R.string.book_library_separator_favorite)
            val notFavorite = context.getString(R.string.book_library_separator_non_favorite)
            val title = when (order) {
                Order.Name -> book.title.substring(0, 1).uppercase()
                Order.Date -> GeneralConsts.formatCountDays(context, book.dateCreate)
                Order.LastAccess -> GeneralConsts.formatCountDays(context, book.lastAccess)
                Order.Author -> if (book.author.isEmpty()) "" else book.author.lowercase()
                Order.Genre -> if (book.genre.isEmpty()) "" else book.genre.lowercase()
                Order.Series -> if (book.series.isEmpty()) "" else book.series.lowercase()
                Order.Favorite -> if (book.favorite) favorite else notFavorite
                else -> ""
            }
            return Separator(title)
        }

        fun getMangaSeparator(context: Context, order: Order, manga: Manga): Separator {
            val favorite = context.getString(R.string.manga_library_separator_favorite)
            val notFavorite = context.getString(R.string.manga_library_separator_non_favorite)
            val title = when (order) {
                Order.Name -> manga.title.substring(0, 1).uppercase()
                Order.Date -> GeneralConsts.formatCountDays(context, manga.dateCreate)
                Order.LastAccess -> GeneralConsts.formatCountDays(context, manga.lastAccess)
                Order.Author -> if (manga.author.isEmpty()) "" else manga.author.lowercase()
                Order.Genre -> if (manga.genre.isEmpty()) "" else manga.genre.lowercase()
                Order.Series -> if (manga.series.isEmpty()) "" else manga.series.lowercase()
                Order.Favorite -> if (manga.favorite) favorite else notFavorite
                else -> ""
            }
            return Separator(title)
        }

        fun getMangaCardSize(context: Context, type: LibraryMangaType, isLandscape: Boolean) : Pair<Int, Int> {
            validLandscape(isLandscape)
            return if (mMangaCardSize.contains(type)) mMangaCardSize[type]!! else setMangaCardSize(context, type)
        }
        fun getBookCardSize(context: Context, type: LibraryBookType, isLandscape: Boolean) : Pair<Int, Int> {
            validLandscape(isLandscape)
            return if (mBookCardSize.contains(type)) mBookCardSize[type]!! else setBookCardSize(context, type)
        }

        private fun setHistoryCardSize(context: Context, type: HistoryType): Pair<Int, Int> {
            val width = when (type) {
                HistoryType.SEPARATOR_BIG -> context.resources.getDimension(R.dimen.history_separator_grid_card_layout_width_big).toInt()
                HistoryType.SEPARATOR_MEDIUM -> context.resources.getDimension(
                    if (mIsLandscape) R.dimen.history_separator_grid_card_layout_width_landscape_medium
                    else R.dimen.history_separator_grid_card_layout_width_medium
                ).toInt()
                else -> -1
            }
            val height = when (type) {
                HistoryType.SEPARATOR_BIG -> context.resources.getDimension(R.dimen.history_separator_grid_card_layout_height_big).toInt()
                HistoryType.SEPARATOR_MEDIUM -> context.resources.getDimension(
                    if (mIsLandscape) R.dimen.history_separator_grid_card_layout_height_landscape_medium
                    else R.dimen.history_separator_grid_card_layout_height_medium
                ).toInt()
                else -> -1
            }
            val size = Pair(width, height)
            mHistoryCardSize[type] = size
            return size
        }

        fun getHistoryCardSize(context: Context, type: HistoryType, isLandscape: Boolean): Pair<Int, Int> {
            validLandscape(isLandscape)
            return if (mHistoryCardSize.contains(type)) mHistoryCardSize[type]!! else setHistoryCardSize(context, type)
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

fun com.google.android.material.button.MaterialButton.executeWithAnimation(action: () -> Unit) {
    val avd = this.icon as? AnimatedVectorDrawable
    if (avd != null) {
        var isActionRun = false
        val runAction = {
            if (!isActionRun) {
                isActionRun = true
                action()
            }
        }
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable { runAction() }
        avd.clearAnimationCallbacks()
        avd.registerAnimationCallback(object : Animatable2.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                super.onAnimationEnd(drawable)
                handler.removeCallbacks(runnable)
                runAction()
            }
        })
        avd.reset()
        avd.start()
        handler.postDelayed(runnable, 400)
    } else {
        action()
    }
}

fun Button.executeWithAnimation(action: () -> Unit) {
    val iconDrawable = try {
        val method = this.javaClass.getMethod("getIcon")
        method.invoke(this) as? Drawable
    } catch (e: Exception) {
        null
    }
    val avd = iconDrawable as? AnimatedVectorDrawable
    if (avd != null) {
        var isActionRun = false
        val runAction = {
            if (!isActionRun) {
                isActionRun = true
                action()
            }
        }
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable { runAction() }
        avd.clearAnimationCallbacks()
        avd.registerAnimationCallback(object : Animatable2.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                super.onAnimationEnd(drawable)
                handler.removeCallbacks(runnable)
                runAction()
            }
        })
        avd.reset()
        avd.start()
        handler.postDelayed(runnable, 400) // Fallback timeout
    } else {
        action()
    }
}

fun BlurView.blurOnceDeferred(handler: Handler, delayMs: Long = 100L) {
    // delayMs kept for call-site compatibility; scheduler coalesces the one-shot update
    if (delayMs <= 0L) {
        GlassRenderScheduler.requestUpdate(this)
        return
    }
    Choreographer.getInstance().postFrameCallback {
        if (isAttachedToWindow) {
            GlassRenderScheduler.requestUpdate(this)
        }
    }
}