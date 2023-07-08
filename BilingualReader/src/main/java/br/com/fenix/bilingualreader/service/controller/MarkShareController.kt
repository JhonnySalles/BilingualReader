package br.com.fenix.bilingualreader.service.controller

import android.content.Context
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.ShareItem
import br.com.fenix.bilingualreader.model.entity.ShareMark
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.LibraryRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.LibraryUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.*


class MarkShareController(var context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(MarkShareController::class.java)

    private val mLibraryRepository = LibraryRepository(context)

    private var mIdFolder: String = ""
    private var mIdManga: String = ""
    private var mIdBook: String = ""


    private fun getFile(name: String): File {
        val file = File(GeneralConsts.getCacheDir(context), name)
        if (!file.exists())
            file.createNewFile()

        return file
    }

    private fun getDriveService(): Drive? {
        GoogleSignIn.getLastSignedInAccount(context)?.let { googleAccount ->
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = googleAccount.account!!
            return Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName(context.getString(R.string.app_name))
                .build()
        }
        return null
    }

    private fun downloadShareFile(drive: Drive, idFile: String): ByteArrayOutputStream? {
        return try {
            val outputStream = ByteArrayOutputStream()
            drive.files().get(idFile)
                .executeMediaAndDownloadTo(outputStream)
            outputStream
        } catch (e: GoogleJsonResponseException) {
            mLOGGER.error("Error download share file from drive.", e)
            null
        }
    }

    private fun uploadShareFile(drive: Drive, idFolder: String, name: String, file: File): String {
        return try {
            val gfile = com.google.api.services.drive.model.File()
            val fileContent = FileContent("application/json", file)
            gfile.name = name

            val parents: MutableList<String> = ArrayList(1)
            parents.add(idFolder)

            gfile.parents = parents
            val file = drive.Files().create(gfile, fileContent).setFields("id").execute()
            file.id
        } catch (e: GoogleJsonResponseException) {
            mLOGGER.error("Error upload share file from drive.", e)
            ""
        }
    }

    private fun createShareFiles(drive: Drive) {
        if (mIdFolder.isEmpty()) {
            val gFolder = com.google.api.services.drive.model.File()
            gFolder.name = GeneralConsts.SHARE_MARKS.FOLDER
            gFolder.mimeType = "application/vnd.google-apps.folder"
            mIdFolder = drive.Files().create(gFolder).setFields("id").execute().id
        }

        if (mIdManga.isEmpty())
            mIdManga = uploadShareFile(
                drive,
                mIdFolder,
                GeneralConsts.SHARE_MARKS.MANGA_FILE,
                getFile(GeneralConsts.SHARE_MARKS.MANGA_FILE)
            )

        if (mIdBook.isEmpty())
            mIdBook = uploadShareFile(
                drive,
                mIdFolder,
                GeneralConsts.SHARE_MARKS.BOOK_FILE,
                getFile(GeneralConsts.SHARE_MARKS.BOOK_FILE)
            )
    }

    private fun getShareFiles(ending: (access: Boolean) -> (Unit)) {
        getDriveService().let { service ->
            if (service != null)
                CoroutineScope(Dispatchers.IO).launch {
                    var pageToken: String? = null

                    mIdFolder = ""
                    mIdManga = ""
                    mIdBook = ""

                    do {
                        val result = service.files().list()
                            .setQ("mimeType='application/json' or mimeType='application/vnd.google-apps.folder'")
                            .setSpaces("drive")
                            .setFields("nextPageToken, items(id, title)")
                            .setPageToken(pageToken)
                            .execute()

                        for (file in result.files)
                            when (file.name) {
                                GeneralConsts.SHARE_MARKS.MANGA_FILE -> mIdManga = file.id
                                GeneralConsts.SHARE_MARKS.BOOK_FILE -> mIdBook = file.id
                                GeneralConsts.SHARE_MARKS.FOLDER -> mIdFolder = file.id
                            }
                    } while (pageToken != null)

                    if (mIdBook.isEmpty() || mIdManga.isEmpty())
                        createShareFiles(service)

                    downloadShareFile(service, mIdManga)?.let {
                        FileOutputStream(getFile(GeneralConsts.SHARE_MARKS.MANGA_FILE)).use { outputStream ->
                            it.writeTo(
                                outputStream
                            )
                        }
                    }

                    downloadShareFile(service, mIdBook)?.let {
                        FileOutputStream(getFile(GeneralConsts.SHARE_MARKS.BOOK_FILE)).use { outputStream ->
                            it.writeTo(
                                outputStream
                            )
                        }
                    }

                    ending(true)
                }
            else
                ending(false)
        }
    }

    private fun saveShareFile(share: ShareMark, name: String) {
        getDriveService()?.let { service ->
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.IO) {
                    val gson = Gson()
                    val file = File(GeneralConsts.getCacheDir(context), name)
                    gson.toJson(share, FileWriter(file))
                    uploadShareFile(service, mIdFolder, name, file)
                }
            }
        }
    }

    // --------------------------------------------------------- Manga ---------------------------------------------------------
    private fun compare(item: ShareItem, manga: Manga): Boolean {
        return if (manga.lastAccess == null || item.lastAccess.after(
                GeneralConsts.dateTimeToDate(
                    manga.lastAccess!!
                )
            )
        ) {
            manga.bookMark = item.bookMark
            manga.lastAccess = GeneralConsts.dateToDateTime(item.lastAccess)
            manga.favorite = item.favorite
            true
        } else {
            item.bookMark = manga.bookMark
            item.lastAccess = GeneralConsts.dateTimeToDate(manga.lastAccess!!)
            item.favorite = manga.favorite
            false
        }
    }

    private fun processManga(
        shares: Set<ShareItem>,
        mangas: List<Manga>,
        update: (manga: Manga) -> (Unit)
    ): Set<ShareItem> {
        val list = mutableSetOf<ShareItem>()
        list.addAll(shares)

        for (manga in mangas)
            shares.parallelStream().filter { it.file == manga.fileName }.findFirst().also {
                if (it.isPresent) {
                    if (compare(it.get(), manga))
                        update(manga)
                } else
                    list.add(ShareItem(manga))
            }

        return list.toSet()
    }

    fun mangaShareMark(update: (manga: Manga) -> (Unit), ending: (processed: Boolean) -> (Unit)) {
        getShareFiles { access ->
            try {
                if (access) {
                    val gson = Gson()
                    val repository = MangaRepository(context)

                    val reader =
                        JsonReader(FileReader(getFile(GeneralConsts.SHARE_MARKS.MANGA_FILE)))
                    val share: ShareMark = gson.fromJson(reader, ShareMark::class.java)

                    repository.list(LibraryUtil.getDefault(context, Type.MANGA))?.apply {
                        share.marks = processManga(share.marks, this) { manga ->
                            repository.save(manga)
                            update(manga)
                        }
                    }

                    val libs = mLibraryRepository.list(Type.MANGA)
                    for (lib in libs)
                        repository.list(lib)?.apply {
                            share.marks = processManga(share.marks, this) { manga ->
                                repository.save(manga)
                                update(manga)
                            }
                        }

                    if (share.marks.isNotEmpty())
                        saveShareFile(share, GeneralConsts.SHARE_MARKS.MANGA_FILE)
                }
            } finally {
                ending(access)
            }
        }
    }

    // --------------------------------------------------------- Book ---------------------------------------------------------
    private fun compare(item: ShareItem, book: Book): Boolean {
        return if (book.lastAccess == null || item.lastAccess.after(
                GeneralConsts.dateTimeToDate(
                    book.lastAccess!!
                )
            )
        ) {
            book.bookMark = item.bookMark
            book.lastAccess = GeneralConsts.dateToDateTime(item.lastAccess)
            book.favorite = item.favorite
            true
        } else {
            item.bookMark = book.bookMark
            item.lastAccess = GeneralConsts.dateTimeToDate(book.lastAccess!!)
            item.favorite = book.favorite
            false
        }
    }

    private fun processBook(
        shares: Set<ShareItem>,
        books: List<Book>,
        update: (book: Book) -> (Unit)
    ): Set<ShareItem> {
        val list = mutableSetOf<ShareItem>()
        list.addAll(shares)

        for (book in books)
            shares.parallelStream().filter { it.file == book.fileName }.findFirst().also {
                if (it.isPresent) {
                    if (compare(it.get(), book))
                        update(book)
                } else
                    list.add(ShareItem(book))
            }

        return list.toSet()
    }

    fun bookShareMark(update: (book: Book) -> (Unit), ending: (processed: Boolean) -> (Unit)) {
        getShareFiles { access ->
            try {
                if (access) {
                    val gson = Gson()
                    val repository = BookRepository(context)

                    val reader =
                        JsonReader(FileReader(getFile(GeneralConsts.SHARE_MARKS.BOOK_FILE)))
                    val share: ShareMark = gson.fromJson(reader, ShareMark::class.java)

                    repository.list(LibraryUtil.getDefault(context, Type.BOOK)).let {
                        if (it.isNotEmpty())
                            share.marks = processBook(share.marks, it) { book ->
                                repository.update(book)
                                update(book)
                            }
                    }

                    val libs = mLibraryRepository.list(Type.BOOK)
                    for (lib in libs)
                        repository.list(lib).let {
                            if (it.isNotEmpty())
                                share.marks = processBook(share.marks, it) { book ->
                                    repository.update(book)
                                    update(book)
                                }
                        }

                    if (share.marks.isNotEmpty())
                        saveShareFile(share, GeneralConsts.SHARE_MARKS.BOOK_FILE)
                }
            } finally {
                ending(access)
            }
        }
    }
}