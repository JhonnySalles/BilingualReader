package br.com.fenix.bilingualreader.service.sharemark

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.ShareItem
import br.com.fenix.bilingualreader.model.enums.ShareMarkType
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ShareMarkFirebaseController(override var context: Context) : ShareMarkFirebaseBase(context) {

    private val mLOGGER = LoggerFactory.getLogger(ShareMarkFirebaseController::class.java)

    companion object {
        const val DATABASE_NAME = "bilingualreader"
        const val MANGA = "manga"
        const val BOOK = "book"
    }

    private lateinit var mDB: FirebaseFirestore

    override fun initialize() {
        if (!::mDB.isInitialized) {
            try {
                FirebaseApp.initializeApp(context)
                mDB = FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                mLOGGER.error(e.message, e)
            }
        }
    }

    override val mNotConnetErrorType: ShareMarkType get() = ShareMarkType.NOT_CONNECT_FIREBASE

    private fun isInitialized(): Boolean = ::mDB.isInitialized

    // --------------------------------------------------------- Manga ---------------------------------------------------------
    override fun processManga(update: (manga: Manga) -> (Unit), ending: (processed: ShareMarkType) -> (Unit)) {
        CoroutineScope(Dispatchers.IO).launch {
            async {
                val prefs = GeneralConsts.getSharedPreferences(context)
                val simpleDate = SimpleDateFormat(GeneralConsts.SHARE_MARKS.PARSE_DATE_TIME, Locale.getDefault())
                val dateSync = prefs.getString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_MANGA, INITIAL_SYNC_DATE_TIME)!!
                val lastSync = simpleDate.parse(dateSync)!!

                val collection: CollectionReference
                val query: Task<QuerySnapshot>
                val querySnapshot: QuerySnapshot

                try {
                    collection = mDB.collection(DATABASE_NAME)
                    query = collection.whereEqualTo(MANGA, true).whereGreaterThan(ShareItem.FIELD_SYNC, dateSync).orderBy(ShareItem.FIELD_SYNC).get()
                    querySnapshot = query.await()
                } catch (e: Exception) {
                    mLOGGER.error(e.message, e)
                    withContext(Dispatchers.Main) {
                        ending(ShareMarkType.ERROR_DOWNLOAD)
                    }
                    return@async
                }

                val repository = MangaRepository(context)

                val share = mutableListOf<ShareItem>()

                val documents = querySnapshot.documents
                for (document in documents) {
                    for (key in document.data!!.keys)
                        document.get(key, ShareItem::class.java)?.let { share.add(it) }
                }

                repository.listSync(lastSync).apply {
                    for (manga in this)
                        share.find { it.file == manga.name }.also {
                            if (it != null) {
                                if (compare(it, manga)) {
                                    repository.update(manga)
                                    withContext(Dispatchers.Main) {
                                        update(manga)
                                    }
                                }
                            } else
                                share.add(ShareItem(manga))
                        }
                }

                share.filter { !it.processed }.forEach {
                    repository.findByFileName(it.file)?.let { manga ->
                        if (compare(it, manga)) {
                            repository.update(manga)
                            withContext(Dispatchers.Main) {
                                update(manga)
                            }
                        }
                    }
                }

                val isUpdate = if (share.isNotEmpty()) share.any { it.alter } else false

                val shared = if (isUpdate) {
                    try {
                        val data: MutableMap<String, Any> = mutableMapOf()

                        share.filter { it.alter }.forEach {
                            it.sync = Date()
                            data[it.file] = it
                        }

                        val result = collection.document(MANGA).set(data)
                        result.result
                        ShareMarkType.SUCCESS
                    } catch (e: Exception) {
                        mLOGGER.error(e.message, e)
                        ShareMarkType.ERROR_UPLOAD
                    }
                } else if (share.isNotEmpty())
                    ShareMarkType.SUCCESS
                else
                    ShareMarkType.NOT_ALTERATION

                ShareMarkType.send = isUpdate
                ShareMarkType.receive = share.isNotEmpty()

                prefs.edit().putString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_MANGA, simpleDate.format(Date())).apply()

                withContext(Dispatchers.Main) {
                    ending(shared)
                }
            }
        }
    }

    // --------------------------------------------------------- Book ---------------------------------------------------------
    override fun processBook(update: (book: Book) -> (Unit), ending: (processed: ShareMarkType) -> (Unit)) {
        CoroutineScope(Dispatchers.IO).launch {
            async {
                val prefs = GeneralConsts.getSharedPreferences(context)
                val simpleDate = SimpleDateFormat(GeneralConsts.SHARE_MARKS.PARSE_DATE_TIME, Locale.getDefault())
                val dateSync = prefs.getString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_BOOK, INITIAL_SYNC_DATE_TIME)!!
                val lastSync = simpleDate.parse(dateSync)!!

                val collection: CollectionReference
                val query: Task<QuerySnapshot>
                val querySnapshot: QuerySnapshot

                try {
                    collection = mDB.collection(DATABASE_NAME)
                    query = collection.whereEqualTo(BOOK, true).whereGreaterThan(ShareItem.FIELD_SYNC, dateSync).orderBy(ShareItem.FIELD_SYNC).get()
                    querySnapshot = query.await()
                } catch (e: Exception) {
                    mLOGGER.error(e.message, e)
                    withContext(Dispatchers.Main) {
                        ending(ShareMarkType.ERROR_DOWNLOAD)
                    }
                    return@async
                }

                val repository = BookRepository(context)

                val share = mutableListOf<ShareItem>()

                val documents = querySnapshot.documents
                for (document in documents) {
                    for (key in document.data!!.keys)
                        document.get(key, ShareItem::class.java)?.let { share.add(it) }
                }

                repository.listSync(lastSync).apply {
                    for (book in this)
                        share.parallelStream().filter { it.file == book.name }.findFirst().also {
                            if (it.isPresent) {
                                if (compare(it.get(), book)) {
                                    repository.update(book)
                                    withContext(Dispatchers.Main) {
                                        update(book)
                                    }
                                }
                            } else
                                share.add(ShareItem(book))
                        }
                }

                share.filter { !it.processed }.forEach {
                    repository.findByFileName(it.file)?.let { book ->
                        if (compare(it, book)) {
                            repository.update(book)
                            withContext(Dispatchers.Main) {
                                update(book)
                            }
                        }
                    }
                }

                val isUpdate = if (share.isNotEmpty()) share.any { it.alter } else false

                val shared = if (isUpdate) {
                    try {
                        val data: MutableMap<String, Any> = mutableMapOf()

                        share.filter { it.alter }.forEach {
                            it.sync = Date()
                            data[it.file] = it
                        }

                        val result = collection.document(BOOK).set(data)
                        result.result
                        ShareMarkType.SUCCESS
                    } catch (e: Exception) {
                        mLOGGER.error(e.message, e)
                        ShareMarkType.ERROR_UPLOAD
                    }
                } else if (share.isNotEmpty())
                    ShareMarkType.SUCCESS
                else
                    ShareMarkType.NOT_ALTERATION

                ShareMarkType.send = isUpdate
                ShareMarkType.receive = share.isNotEmpty()

                prefs.edit().putString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_BOOK, simpleDate.format(Date())).apply()

                withContext(Dispatchers.Main) {
                    ending(shared)
                }
            }
        }
    }

}