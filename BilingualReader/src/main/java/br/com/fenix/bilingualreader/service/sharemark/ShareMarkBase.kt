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
import br.com.fenix.bilingualreader.model.exceptions.ShareMarkNotConnectCloudException
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import org.slf4j.LoggerFactory


abstract class ShareMarkBase(open var context: Context) : ShareMark {

    private val mLOGGER = LoggerFactory.getLogger(ShareMarkBase::class.java)

    companion object {
        const val INITIAL_SYNC_DATE_TIME = "2000-01-01T01:01:01.001-0300"

        fun getInstance(context: Context) : ShareMark {
            val prefs = GeneralConsts.getSharedPreferences(context)
            val type = ShareMarkCloud.valueOf(prefs.getString(GeneralConsts.KEYS.SYSTEM.SHARE_MARK_CLOUD, ShareMarkCloud.GOOGLE_DRIVE.toString())!!)

            return when (type) {
                ShareMarkCloud.GOOGLE_DRIVE -> ShareMarkGDriveController(context)
                ShareMarkCloud.FIRESTORE -> ShareMarkFirebaseController(context)
            }
        }

        fun clearLastSync(context: Context) {
            val prefs = GeneralConsts.getSharedPreferences(context)
            with(prefs.edit()) {
                this.remove(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_MANGA)
                this.remove(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_BOOK)
                this.commit()
            }
        }
    }

    abstract val mNotConnetErrorType : ShareMarkType

    @Throws(ShareMarkNotConnectCloudException::class)
    abstract fun initialize()

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
        return if (manga.lastAccess == null || item.lastAccess.after(manga.lastAccess!!)) {
            manga.bookMark = item.bookMark
            manga.lastAccess = item.lastAccess
            manga.favorite = item.favorite
            true
        } else {
            //Differ 10 seconds
            val diff: Long = item.lastAccess.time - manga.lastAccess!!.time
            if (diff > 10000 || diff < -10000)
                item.merge(manga)
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
        ShareMarkType.clear()
        if (!isOnline()) {
            ending(ShareMarkType.ERROR_NETWORK)
            return
        }

        try {
            initialize()
        } catch (e: Exception) {
            mLOGGER.error(e.message, e)
            ending(mNotConnetErrorType)
            return
        }

        try {
            processManga(update, ending)
        } catch (e: Exception) {
            mLOGGER.error(e.message, e)
            ending(ShareMarkType.ERROR)
        }
    }

    // --------------------------------------------------------- Book ---------------------------------------------------------
    protected fun compare(item: ShareItem, book: Book): Boolean {
        return if (book.lastAccess == null || item.lastAccess.after(book.lastAccess!!)) {
            book.bookMark = item.bookMark
            book.lastAccess = item.lastAccess
            book.favorite = item.favorite
            true
        } else {
            //Differ 10 seconds
            val diff: Long = item.lastAccess.time - book.lastAccess!!.time
            if (diff > 10000 || diff < -10000)
                item.merge(book)
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
        ShareMarkType.clear()
        if (!isOnline()) {
            ending(ShareMarkType.ERROR_NETWORK)
            return
        }

        try {
            initialize()
        } catch (e: Exception) {
            mLOGGER.error(e.message, e)
            ending(mNotConnetErrorType)
            return
        }

        try {
            processBook(update, ending)
        } catch (e: Exception) {
            mLOGGER.error(e.message, e)
            ending(ShareMarkType.ERROR)
        }
    }

}