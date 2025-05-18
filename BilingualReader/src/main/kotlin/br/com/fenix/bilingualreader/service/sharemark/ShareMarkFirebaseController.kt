package br.com.fenix.bilingualreader.service.sharemark

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.entity.History
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.MangaAnnotation
import br.com.fenix.bilingualreader.model.entity.ShareItem
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.model.enums.ShareMarkType
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.BookAnnotationRepository
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.HistoryRepository
import br.com.fenix.bilingualreader.service.repository.MangaAnnotationRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Util
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
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
import java.time.LocalDateTime
import java.util.Date
import java.util.Locale


class ShareMarkFirebaseController(override var context: Context) : ShareMarkBase(context) {

    private val mLOGGER = LoggerFactory.getLogger(ShareMarkFirebaseController::class.java)

    companion object {
        const val DATABASE_NAME = "bilingualreader_%s_%s"
        const val MANGA = "manga"
        const val BOOK = "book"
    }

    private lateinit var mDB: FirebaseFirestore
    private var mUser = ""

    override fun initialize(ending: (access: ShareMarkType) -> (Unit)) {
        if (!::mDB.isInitialized) {
            try {
                val auth = FirebaseAuth.getInstance()
                val credential = GoogleSignIn.getLastSignedInAccount(context)
                mUser = credential!!.email?.substringBeforeLast("@") ?: ""
                val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                auth.signInWithCredential(firebaseCredential)

                FirebaseApp.initializeApp(context)
                mDB = FirebaseFirestore.getInstance()
                ending(ShareMarkType.SUCCESS)
            } catch (e: Exception) {
                mLOGGER.error(e.message, e)
                ending(ShareMarkType.ERROR)
            }
        } else
            ending(ShareMarkType.SUCCESS)
    }

    override val mNotConnectErrorType: ShareMarkType get() = ShareMarkType.NOT_CONNECT_FIREBASE

    private fun isInitialized(): Boolean = ::mDB.isInitialized

    // --------------------------------------------------------- Manga ---------------------------------------------------------
    override fun processManga(update: (manga: Manga) -> (Unit), ending: (processed: ShareMarkType) -> (Unit)) {
        CoroutineScope(Dispatchers.IO).launch {
            async {
                ShareMarkType.clear()
                val alteration = LocalDateTime.now().minusSeconds(1)
                val sync = Date()
                val prefs = GeneralConsts.getSharedPreferences(context)
                val simpleDate = SimpleDateFormat(GeneralConsts.SHARE_MARKS.PARSE_DATE_TIME, Locale.getDefault())
                val dateSync = prefs.getString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_MANGA, INITIAL_SYNC_DATE_TIME)!!
                val lastSync = simpleDate.parse(dateSync)!!

                val collection: CollectionReference
                val snapshot: QuerySnapshot
                val document: DocumentSnapshot

                try {
                    collection = mDB.collection(String.format(DATABASE_NAME, mUser, MANGA))
                    snapshot = collection.whereGreaterThan(ShareItem.FIELD_SYNC, lastSync).get().await()
                    document = collection.document("_" + MANGA).get().await()
                } catch (e: Exception) {
                    mLOGGER.error(e.message, e)
                    withContext(Dispatchers.Main) {
                        ending(ShareMarkType.ERROR_DOWNLOAD)
                    }
                    return@async
                }

                val repositoryManga = MangaRepository(context)
                val repositoryHistory = HistoryRepository(context)
                val repositoryAnnotation = MangaAnnotationRepository(context)
                val share = mutableListOf<ShareItem>()
                val mangas = mutableMapOf<String, Date>()

                try {
                    if (document.exists()) {
                        val item = document.data ?: mapOf()
                        mangas.putAll(item as Map<String, Date>)
                    }

                    snapshot.documents.forEach {
                        val item = it.data ?: mapOf()
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
                                if (mangas.containsKey(manga.name)) {
                                    val document = collection.document(manga.name).get().await()
                                    if (document.exists()) {
                                        val item = ShareItem(document.data as Map<String, *>)
                                        share.add(item)
                                        if (compare(item, manga)) {
                                            repositoryManga.update(manga, alteration)
                                            withContext(Dispatchers.Main) {
                                                update(manga)
                                            }
                                        }
                                    } else
                                        share.add(ShareItem(manga, repositoryHistory.find(manga.type, manga.fkLibrary!!, manga.id!!), repositoryAnnotation.findByManga(manga.id!!)))
                                } else if (manga.bookMark > 0)
                                    share.add(ShareItem(manga, repositoryHistory.find(manga.type, manga.fkLibrary!!, manga.id!!), repositoryAnnotation.findByManga(manga.id!!)))
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

                        it.annotation?.let { a ->
                            val annotations = repositoryAnnotation.findByManga(manga.id!!)
                            for (shared in  a.values) {
                                val created = GeneralConsts.dateToDateTime(shared.created)
                                val annotation = annotations.find { f -> f.created.compareTo(created) == 0 }

                                if (annotation != null) {
                                    annotation.chapter = shared.chapter
                                    annotation.folder = shared.text
                                    annotation.page = shared.page
                                    annotation.pages = shared.pages
                                    annotation.annotation = shared.annotation
                                    repositoryAnnotation.update(annotation)
                                } else
                                    repositoryAnnotation.save(
                                        MangaAnnotation(null, manga.id!!, shared.page, shared.pages, MarkType.valueOf(shared.type),
                                            shared.chapter, shared.text, shared.annotation, LocalDateTime.now(), created
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
                            mangas[it.file] = sync
                            it.sync = sync
                            it.refreshHistory(repositoryHistory.find(Type.MANGA, it.idLibrary, it.id))
                            it.refreshAnnotations(repositoryAnnotation.findByManga(it.id))
                            collection.document(it.file).set(it).await()
                        }

                        if (share.any { it.alter })
                            collection.document("_$MANGA").set(mangas).await()

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
                val alteration = LocalDateTime.now().minusSeconds(1)
                val sync = Date()
                val prefs = GeneralConsts.getSharedPreferences(context)
                val simpleDate = SimpleDateFormat(GeneralConsts.SHARE_MARKS.PARSE_DATE_TIME, Locale.getDefault())
                val dateSync = prefs.getString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_BOOK, INITIAL_SYNC_DATE_TIME)!!
                val lastSync = simpleDate.parse(dateSync)!!

                val collection: CollectionReference
                val snapshot: QuerySnapshot
                val document: DocumentSnapshot

                try {
                    collection = mDB.collection(String.format(DATABASE_NAME, mUser, BOOK))
                    snapshot = collection.whereGreaterThan(ShareItem.FIELD_SYNC, lastSync).get().await()
                    document = collection.document("_" + BOOK).get().await()
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
                val books = mutableMapOf<String, Date>()

                try {
                    if (document.exists()) {
                        val item = document.data ?: mapOf()
                        books.putAll(item as Map<String, Date>)
                    }

                    snapshot.documents.forEach {
                        val item = it.data ?: mapOf()
                        share.add(ShareItem(item))
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
                                if (books.containsKey(book.name)) {
                                    val document = collection.document(book.name).get().await()
                                    if (document.exists()) {
                                        val item = ShareItem(document.data as Map<String, *>)
                                        share.add(item)
                                        if (compare(item, book)) {
                                            repositoryBook.update(book, alteration)
                                            withContext(Dispatchers.Main) {
                                                update(book)
                                            }
                                        }
                                    } else
                                        share.add(ShareItem(book, repositoryHistory.find(book.type, book.fkLibrary!!, book.id!!), repositoryAnnotation.findByBook(book.id!!)))
                                } else if (book.bookMark > 0)
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
                            books[it.file] = sync
                            it.sync = sync
                            it.refreshHistory(repositoryHistory.find(Type.BOOK, it.idLibrary, it.id))
                            it.refreshAnnotations(repositoryAnnotation.findByBook(it.id))
                            collection.document(it.file).set(it).await()
                        }

                        if (share.any { it.alter })
                            collection.document("_$BOOK").set(books).await()

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