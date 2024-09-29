package br.com.fenix.bilingualreader.service.sharemark

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.entity.History
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.ShareItem
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.model.enums.ShareMarkType
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.BookAnnotationRepository
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.HistoryRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Util
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Date
import java.util.Locale


class ShareMarkFirebaseController(override var context: Context) : ShareMarkBase(context) {

    private val mLOGGER = LoggerFactory.getLogger(ShareMarkFirebaseController::class.java)

    companion object {
        const val DATABASE_NAME = "bilingualreader"
        const val MANGA = "_manga"
        const val BOOK = "_book"
    }

    private lateinit var mDB: FirebaseFirestore
    private var mUser = ""

    override fun initialize() {
        if (!::mDB.isInitialized) {
            try {
                val auth = FirebaseAuth.getInstance()
                val credential = GoogleSignIn.getLastSignedInAccount(context)
                mUser = credential!!.email?.substringBeforeLast("@") ?: ""
                val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                auth.signInWithCredential(firebaseCredential)

                FirebaseApp.initializeApp(context)
                mDB = FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                mLOGGER.error(e.message, e)
            }
        }
    }

    override val mNotConnectErrorType: ShareMarkType get() = ShareMarkType.NOT_CONNECT_FIREBASE

    private fun isInitialized(): Boolean = ::mDB.isInitialized

    // --------------------------------------------------------- Manga ---------------------------------------------------------
    override fun processManga(update: (manga: Manga) -> (Unit), ending: (processed: ShareMarkType) -> (Unit)) {
        CoroutineScope(Dispatchers.IO).launch {
            async {
                ShareMarkType.clear()
                val sync = Date()
                val alteration = LocalDateTime.now()
                val prefs = GeneralConsts.getSharedPreferences(context)
                val simpleDate = SimpleDateFormat(GeneralConsts.SHARE_MARKS.PARSE_DATE_TIME, Locale.getDefault())
                val dateSync = prefs.getString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_MANGA, INITIAL_SYNC_DATE_TIME)!!
                val lastSync = simpleDate.parse(dateSync)!!

                val collection: CollectionReference
                val query: Task<DocumentSnapshot>
                val snapshot: DocumentSnapshot

                try {
                    collection = mDB.collection(DATABASE_NAME)
                    query = collection.document(mUser + MANGA).get()
                    snapshot = query.await()
                } catch (e: Exception) {
                    mLOGGER.error(e.message, e)
                    withContext(Dispatchers.Main) {
                        ending(ShareMarkType.ERROR_DOWNLOAD)
                    }
                    return@async
                }

                val repositoryManga = MangaRepository(context)
                val repositoryHistory = HistoryRepository(context)
                val share = mutableListOf<ShareItem>()
                val cloud: MutableMap<String, Any> = mutableMapOf()

                try {
                    val documents = snapshot.data ?: mapOf()
                    for (key in documents.keys) {
                        val item = documents[key] as Map<String, *>
                        cloud[key] = item
                        if (lastSync.before((item[ShareItem.FIELD_SYNC] as Timestamp).toDate()))
                            share.add(ShareItem(item))
                    }
                } catch (e: Exception) {
                    mLOGGER.error(e.message, e)
                    withContext(Dispatchers.Main) {
                        ending(ShareMarkType.ERROR)
                    }
                    return@async
                }

                repositoryManga.listSync(lastSync).apply {
                    for (manga in this)
                        share.find { it.file == manga.name }.also {
                            if (it != null) {
                                if (compare(it, manga)) {
                                    repositoryManga.update(manga, alteration)
                                    withContext(Dispatchers.Main) {
                                        update(manga)
                                    }
                                }
                            } else {
                                val alphabet = manga.name.substring(0, 2).lowercase().trim()
                                if (cloud.containsKey(alphabet) && (cloud[alphabet]as Map<String, Any>).containsKey(manga.name)) {
                                    val item = ShareItem((cloud[alphabet] as Map<String, Any>)[manga.name] as Map<String, *>)
                                    share.add(item)
                                    if (compare(item, manga)) {
                                        repositoryManga.update(manga, alteration)
                                        withContext(Dispatchers.Main) {
                                            update(manga)
                                        }
                                    }
                                } else
                                    share.add(ShareItem(manga, repositoryHistory.find(manga.type, manga.fkLibrary!!, manga.id!!)))
                            }
                        }
                }

                share.filter { !it.processed }.forEach {
                    repositoryManga.findByFileName(it.file)?.let { manga ->
                        if (compare(it, manga)) {
                            repositoryManga.update(manga, alteration)
                            withContext(Dispatchers.Main) {
                                update(manga)
                            }
                        }
                    }
                }

                share.parallelStream().forEach {
                    repositoryManga.findByFileName(it.file)?.let { manga ->
                        it.history?.let { h ->
                            val histories = repositoryHistory.find(manga.type, manga.fkLibrary!!, manga.id!!).map { h -> GeneralConsts.dateTimeToDate(h.start) }
                            val list = h.values.filter { f -> histories.none { s -> f.start.compareTo(s) == 0 } }
                            if (list.isNotEmpty())
                                for (shared in list)
                                    repositoryHistory.save(
                                        History(null, manga.fkLibrary!!, manga.id!!, manga.type, shared.pageStart, shared.pageEnd, shared.pages, shared.completed,
                                            shared.volume, shared.chaptersRead, GeneralConsts.dateToDateTime(shared.start), GeneralConsts.dateToDateTime(shared.end),
                                            shared.secondsRead.toLong(), shared.averageTimeByPage.toLong(), shared.useTTS, isNotify = false
                                        )
                                    )
                        }
                    }
                }

                val isUpdate = if (share.isNotEmpty()) share.any { it.alter } else false

                val shared = if (isUpdate) {
                    try {
                        share.filter { it.alter }.forEach {
                            it.sync = sync
                            val position = it.file.substring(0, 2).lowercase().trim()
                            val item = if (cloud.containsKey(position)) cloud[position] as MutableMap<String, Any> else mutableMapOf()
                            item[it.file] = it
                            cloud[position] = item
                            it.refreshHistory(repositoryHistory.find(Type.MANGA, it.idLibrary, it.id))
                        }

                        val result = collection.document(mUser + MANGA).set(cloud)
                        result.await()
                        ShareMarkType.SUCCESS
                    } catch (e: Exception) {
                        mLOGGER.error(e.message, e)
                        ShareMarkType.ERROR_UPLOAD
                    }
                } else if (share.isNotEmpty())
                    ShareMarkType.SUCCESS
                else
                    ShareMarkType.NOT_ALTERATION

                ShareMarkType.send = share.count { it.alter }
                ShareMarkType.receive = share.count { it.received }

                if (shared != ShareMarkType.ERROR_UPLOAD)
                    prefs.edit().putString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_MANGA, simpleDate.format(Date(sync.time + 1000))).apply()

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
                ShareMarkType.clear()
                val sync = Date()
                val alteration = LocalDateTime.now()
                val prefs = GeneralConsts.getSharedPreferences(context)
                val simpleDate = SimpleDateFormat(GeneralConsts.SHARE_MARKS.PARSE_DATE_TIME, Locale.getDefault())
                val dateSync = prefs.getString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_BOOK, INITIAL_SYNC_DATE_TIME)!!
                val lastSync = simpleDate.parse(dateSync)!!

                val collection: CollectionReference
                val query: Task<DocumentSnapshot>
                val snapshot: DocumentSnapshot

                try {
                    collection = mDB.collection(DATABASE_NAME)
                    query = collection.document(mUser + BOOK).get()
                    snapshot = query.await()
                } catch (e: Exception) {
                    mLOGGER.error(e.message, e)
                    withContext(Dispatchers.Main) {
                        ending(ShareMarkType.ERROR_DOWNLOAD)
                    }
                    return@async
                }

                val repositoryBook = BookRepository(context)
                val repositoryHistory = HistoryRepository(context)
                val repositoryAnnotation = BookAnnotationRepository(context)
                val share = mutableListOf<ShareItem>()
                val cloud: MutableMap<String, Any> = mutableMapOf()

                try {
                    val documents = snapshot.data ?: mapOf()
                    for (key in documents.keys) {
                        val alphabet = documents[key] as Map<String, *>
                        cloud[key] = alphabet
                        for (itm in alphabet.keys) {
                            val item = alphabet[itm] as Map<String, *>
                            if (lastSync.before((item[ShareItem.FIELD_SYNC] as Timestamp).toDate()))
                                share.add(ShareItem(item))
                        }
                    }
                } catch (e: Exception) {
                    mLOGGER.error(e.message, e)
                    withContext(Dispatchers.Main) {
                        ending(ShareMarkType.ERROR)
                    }
                    return@async
                }

                repositoryBook.listSync(lastSync).apply {
                    for (book in this)
                        share.parallelStream().filter { it.file == book.name }.findFirst().also {
                            if (it.isPresent) {
                                if (compare(it.get(), book)) {
                                    repositoryBook.update(book, alteration)
                                    withContext(Dispatchers.Main) {
                                        update(book)
                                    }
                                }
                            } else {
                                val alphabet = book.name.substring(0, 2).lowercase().trim()
                                if (cloud.containsKey(alphabet) && (cloud[alphabet]as Map<String, Any>).containsKey(book.name)) {
                                    val item = ShareItem((cloud[alphabet] as Map<String, Any>)[book.name] as Map<String, *>)
                                    share.add(item)
                                    if (compare(item, book)) {
                                        repositoryBook.update(book, alteration)
                                        withContext(Dispatchers.Main) {
                                            update(book)
                                        }
                                    }
                                } else
                                    share.add(ShareItem(book, repositoryHistory.find(book.type, book.fkLibrary!!, book.id!!), repositoryAnnotation.findByBook(book.id!!)))
                            }
                        }
                }

                share.filter { !it.processed }.forEach {
                    repositoryBook.findByFileName(it.file)?.let { book ->
                        if (compare(it, book)) {
                            repositoryBook.update(book, alteration)
                            withContext(Dispatchers.Main) {
                                update(book)
                            }
                        }
                    }
                }

                share.parallelStream().forEach {
                    repositoryBook.findByFileName(it.file)?.let { book ->
                        it.history?.let { h ->
                            val histories = repositoryHistory.find(book.type, book.fkLibrary!!, book.id!!).map { h -> GeneralConsts.dateTimeToDate(h.start) }
                            val list = h.values.filter { f -> histories.none { s -> f.start.compareTo(s) == 0 } }
                            if (list.isNotEmpty())
                                for (shared in list)
                                    repositoryHistory.save(
                                        History(null, book.fkLibrary!!, book.id!!, book.type, shared.pageStart, shared.pageEnd, shared.pages, shared.completed,
                                            shared.volume, shared.chaptersRead, GeneralConsts.dateToDateTime(shared.start), GeneralConsts.dateToDateTime(shared.end),
                                            shared.secondsRead.toLong(), shared.averageTimeByPage.toLong(), shared.useTTS, isNotify = false
                                        )
                                    )
                        }

                        it.annotation?.let { a ->
                            val annotations = repositoryAnnotation.findByBook(book.id!!)
                            for (shared in  a.values) {
                                val created = GeneralConsts.dateToDateTime(shared.created)
                                val annotation = annotations.find { f -> f.created.compareTo(created) == 0 }

                                if (annotation != null) {
                                    annotation.text = shared.text
                                    annotation.page = shared.page
                                    annotation.pages = shared.pages
                                    annotation.fontSize = shared.fontSize
                                    annotation.annotation = shared.annotation
                                    annotation.range = Util.stringToIntArray(shared.range)
                                    annotation.favorite = shared.favorite
                                    annotation.color = Color.valueOf(shared.color)
                                    repositoryAnnotation.update(annotation)
                                } else
                                    repositoryAnnotation.save(
                                        BookAnnotation(null, book.id!!, shared.page, shared.pages, shared.fontSize, MarkType.valueOf(shared.type), shared.chapterNumber,
                                            shared.chapter, shared.text, Util.stringToIntArray(shared.range), shared.annotation, shared.favorite, Color.valueOf(shared.color),
                                            LocalDateTime.now(), created
                                        )
                                    )
                            }
                        }
                    }
                }

                val isUpdate = if (share.isNotEmpty()) share.any { it.alter } else false

                val shared = if (isUpdate) {
                    try {
                        share.filter { it.alter }.forEach {
                            it.sync = sync
                            val alphabet = it.file.substring(0, 2).lowercase().trim()
                            val item = if (cloud.containsKey(alphabet)) cloud[alphabet] as MutableMap<String, Any> else mutableMapOf()
                            item[it.file] = it
                            cloud[alphabet] = item
                            it.refreshHistory(repositoryHistory.find(Type.BOOK, it.idLibrary, it.id))
                            it.refreshAnnotations(repositoryAnnotation.findByBook(it.id))
                        }

                        val result = collection.document(mUser + BOOK).set(cloud)
                        result.await()
                        ShareMarkType.SUCCESS
                    } catch (e: Exception) {
                        mLOGGER.error(e.message, e)
                        ShareMarkType.ERROR_UPLOAD
                    }
                } else if (share.isNotEmpty())
                    ShareMarkType.SUCCESS
                else
                    ShareMarkType.NOT_ALTERATION

                ShareMarkType.send = share.count { it.alter }
                ShareMarkType.receive = share.count { it.received }

                if (shared != ShareMarkType.ERROR_UPLOAD)
                    prefs.edit().putString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_BOOK, simpleDate.format(Date(sync.time + 1000))).apply()

                withContext(Dispatchers.Main) {
                    ending(shared)
                }
            }
        }
    }

}