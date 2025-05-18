package br.com.fenix.bilingualreader.service.sharemark

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.ShareItem
import br.com.fenix.bilingualreader.model.enums.ShareMarkCloud
import br.com.fenix.bilingualreader.model.enums.ShareMarkType
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.model.exceptions.ShareMarkNotConnectCloudException
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt


abstract class ShareMarkBase(open var context: Context) : ShareMark {

    private val mLOGGER = LoggerFactory.getLogger(ShareMarkBase::class.java)

    companion object {
        const val INITIAL_SYNC_DATE_TIME = "2000-01-01T01:01:01.001-0300"
        var IN_SYNC = false

        fun getInstance(context: Context) : ShareMark {
            val prefs = GeneralConsts.getSharedPreferences(context)
            val type = ShareMarkCloud.valueOf(prefs.getString(GeneralConsts.KEYS.SYSTEM.SHARE_MARK_CLOUD, ShareMarkCloud.GOOGLE_DRIVE.toString())!!)

            return when (type) {
                ShareMarkCloud.GOOGLE_DRIVE -> ShareMarkGDriveController(context)
                ShareMarkCloud.FIRESTORE -> ShareMarkFirebaseController(context)
            }
        }

        fun clearLastSync(context: Context, type: Type) {
            val prefs = GeneralConsts.getSharedPreferences(context)
            with(prefs.edit()) {
                when (type) {
                    Type.MANGA -> this.remove(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_MANGA)
                    Type.BOOK -> this.remove(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_BOOK)
                }
                this.commit()
            }
        }
    }

    abstract val mNotConnectErrorType : ShareMarkType

    @Throws(ShareMarkNotConnectCloudException::class)
    abstract fun initialize(ending: (access: ShareMarkType) -> (Unit))

    protected fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        if (connectivityManager != null) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            }
        }
        return false
    }

    protected fun getDeviceName(): String {
        val manufacturer: String = Build.MANUFACTURER
        val model: String = Build.MODEL
        return if (model.startsWith(manufacturer))
            model
        else
            "$manufacturer $model"
    }

    // --------------------------------------------------------- Manga ---------------------------------------------------------
    protected fun compare(item: ShareItem, manga: Manga): Boolean {
        return if ((manga.lastAccess == null && manga.lastAlteration == null) ||
            (manga.lastAlteration != null && GeneralConsts.dateTimeToDate(manga.lastAlteration!!).before(item.sync)) ||
            (manga.lastAccess != null && item.lastAccess.after(GeneralConsts.dateTimeToDate(manga.lastAccess!!)))) {

            manga.bookMark = item.bookMark
            manga.lastAccess = GeneralConsts.dateToDateTime(item.lastAccess)
            manga.favorite = item.favorite
            manga.completed = item.completed

            item.processed = true
            item.received = true
            true
        } else {
            if (manga.lastAccess == null)
                item.merge(manga)
            else {
                //Differ 10 seconds
                val diff: Long = item.lastAccess.time - GeneralConsts.dateTimeToDate(manga.lastAccess!!).time
                if (diff > 5000 || diff < -5000)
                    item.merge(manga)
            }
            false
        }
    }

    protected abstract fun processManga(update: (manga: Manga) -> (Unit), ending: (processed: ShareMarkType) -> (Unit))

    /**
     * @param update  Function call when object is updated
     * @param ending  Function call when process finish, parameter make a process results
     *
     */
    override fun mangaShareMark(update: (manga: Manga) -> (Unit), ending: (processed: ShareMarkType) -> (Unit)) {
        if (IN_SYNC) {
            ending(ShareMarkType.SYNC_IN_PROGRESS)
            return
        }

        ShareMarkType.clear()
        if (!isOnline()) {
            ending(ShareMarkType.ERROR_NETWORK)
            return
        }

        IN_SYNC = true

        try {
            initialize() {
                if (it == ShareMarkType.SUCCESS) {
                    try {
                        processManga(update) {
                            IN_SYNC = false
                            ending(it)
                        }
                    } catch (e: Exception) {
                        IN_SYNC = false
                        mLOGGER.error(e.message, e)
                        ending(ShareMarkType.ERROR)
                    }
                } else {
                    ending(it)
                    IN_SYNC = false
                }
            }
        } catch (e: Exception) {
            IN_SYNC = false
            mLOGGER.error(e.message, e)
            ending(mNotConnectErrorType)
            return
        }
    }

    // --------------------------------------------------------- Book ---------------------------------------------------------
    protected fun compare(item: ShareItem, book: Book): Boolean {
        return if ((book.lastAccess == null && book.lastAlteration == null) ||
            (book.lastAlteration != null && GeneralConsts.dateTimeToDate(book.lastAlteration!!).before(item.sync)) ||
            (book.lastAccess != null && item.lastAccess.after(GeneralConsts.dateTimeToDate(book.lastAccess!!)))) {

            if (book.pages <= 1 && item.pages > 1) {
                book.bookMark = item.bookMark
                book.pages = item.pages
            } else if (item.completed || item.bookMark >= item.pages)
                book.bookMark = book.pages
            else {
                val percent = item.bookMark.toFloat() / item.pages
                book.bookMark = (book.pages * percent).roundToInt()

                if (book.bookMark < 0)
                    book.bookMark = 0
                else if (book.bookMark > book.pages)
                    book.bookMark = book.pages
            }

            book.completed = item.completed
            book.lastAccess = GeneralConsts.dateToDateTime(item.lastAccess)
            book.favorite = item.favorite
            item.processed = true
            item.received = true
            true
        } else {
            if (book.lastAccess == null)
                item.merge(book)
            else {
                //Differ 10 seconds
                val diff: Long = item.lastAccess.time - GeneralConsts.dateTimeToDate(book.lastAccess!!).time
                if (diff > 5000 || diff < -5000)
                    item.merge(book)
            }
            false
        }
    }

    protected abstract fun processBook(update: (book: Book) -> (Unit), ending: (processed: ShareMarkType) -> (Unit))

    /**
     * @param update  Function call when object is updated
     * @param ending  Function call when process finish, parameter is true if can processed list
     *
     */
    override fun bookShareMark(update: (book: Book) -> (Unit), ending: (processed: ShareMarkType) -> (Unit)) {
        if (IN_SYNC) {
            ending(ShareMarkType.SYNC_IN_PROGRESS)
            return
        }

        ShareMarkType.clear()
        if (!isOnline()) {
            ending(ShareMarkType.ERROR_NETWORK)
            return
        }

        IN_SYNC = true

        try {
            initialize() {
                if (it == ShareMarkType.SUCCESS) {
                    try {
                        processBook(update) {
                            IN_SYNC = false
                            ending(it)
                        }
                    } catch (e: Exception) {
                        IN_SYNC = false
                        mLOGGER.error(e.message, e)
                        ending(ShareMarkType.ERROR)
                    }
                } else {
                    IN_SYNC = false
                    ending(it)
                }
            }
        } catch (e: Exception) {
            IN_SYNC = false
            mLOGGER.error(e.message, e)
            ending(mNotConnectErrorType)
            return
        }
    }

}