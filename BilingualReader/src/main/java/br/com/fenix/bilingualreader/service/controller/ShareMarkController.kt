package br.com.fenix.bilingualreader.service.controller

import android.content.Context
import android.os.Build
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.ShareItem
import br.com.fenix.bilingualreader.model.entity.ShareMark
import br.com.fenix.bilingualreader.model.enums.ShareMarkType
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.model.exceptions.DriveDownloadException
import br.com.fenix.bilingualreader.model.exceptions.DriveUploadException
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.io.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class ShareMarkController(var context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(ShareMarkController::class.java)

    companion object {
        const val INITIAL_SYNC_DATE_TIME = "2000-01-01T01:01:01.001-0300"
    }

    private var mIdFolder: String = ""
    private var mIdManga: String = ""
    private var mIdBook: String = ""

    private fun getDeviceName(): String {
        val manufacturer: String = Build.MANUFACTURER
        val model: String = Build.MODEL
        return if (model.startsWith(manufacturer))
            model
        else
            "$manufacturer $model"
    }


    private fun getFile(name: String, isDelete: Boolean = false): File {
        val file = File(GeneralConsts.getCacheDir(context), name)
        if (!file.exists())
            file.createNewFile()
        else {
            if (isDelete) {
                file.delete()
                file.createNewFile()
            }
        }

        return file
    }

    private fun getFile(share: ShareMark, name: String): File {
        val file = File(GeneralConsts.getCacheDir(context), name)
        if (file.exists())
            file.delete()

        file.createNewFile()
        share.lastAlteration = Date()
        share.origin = getDeviceName()

        val gson = GsonBuilder()
            .setDateFormat(GeneralConsts.SHARE_MARKS.PARSE_DATE_TIME)
            .excludeFieldsWithoutExposeAnnotation()
            .create()
        val writer = FileWriter(file)
        gson.toJson(share, writer)
        writer.flush()
        writer.close()

        return file
    }

    private fun ajusts(share: ShareMark, type: Type) {
        if (share.type == null)
            share.type = type

        if (share.marks == null)
            share.marks = mutableSetOf()
        else if (share.marks!!.any { it == null })
            share.marks = share.marks!!.filter { it != null }.toMutableSet()
    }

    private fun getDriveService(): Drive? {
        GoogleSignIn.getLastSignedInAccount(context)?.let { googleAccount ->
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = googleAccount.account
            return Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
            ).setApplicationName(context.getString(R.string.app_name))
                .build()
        }
        return null
    }

    private fun deleteShareFile(drive: Drive, idFile: String) {
        try {
            drive.files().delete(idFile).execute()
        } catch (e: GoogleJsonResponseException) {
            mLOGGER.error("Error deleted share file from drive.", e)
        } catch (e: Exception) {
            mLOGGER.error("Error deleted share file from drive.", e)
        }
    }

    private fun downloadShareFile(drive: Drive, idFile: String): ByteArrayOutputStream? {
        return try {
            val outputStream = ByteArrayOutputStream()
            drive.files().get(idFile).executeMediaAndDownloadTo(outputStream)
            outputStream
        } catch (e: GoogleJsonResponseException) {
            mLOGGER.error("Error download share file from drive.", e)
            throw DriveDownloadException("")
        } catch (e: Exception) {
            mLOGGER.error("Error download share file from drive.", e)
            throw DriveDownloadException("")
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
            mLOGGER.error("Error create share file from drive.", e)
            throw DriveUploadException("")
        } catch (e: Exception) {
            mLOGGER.error("Error create share file from drive.", e)
            throw DriveUploadException("")
        }
    }

    private fun uploadShareFile(
        drive: Drive,
        idFolder: String,
        idFile: String,
        nameWithoutExtension: String,
        file: File
    ): String {
        return try {
            val gfile = com.google.api.services.drive.model.File()
            gfile.name = nameWithoutExtension + "_" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern(GeneralConsts.SHARE_MARKS.DATE_TIME)) + GeneralConsts.SHARE_MARKS.FILE_EXTENSION
            drive.Files().update(idFile, gfile).execute()

            uploadShareFile(
                drive,
                idFolder,
                nameWithoutExtension + GeneralConsts.SHARE_MARKS.FILE_EXTENSION,
                file
            )
        } catch (e: GoogleJsonResponseException) {
            mLOGGER.error("Error upload share file from drive.", e)
            throw DriveUploadException("")
        } catch (e: Exception) {
            mLOGGER.error("Error upload share file from drive.", e)
            throw DriveUploadException("")
        }
    }

    private fun createShareFiles(drive: Drive) {
        try {
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
                    GeneralConsts.SHARE_MARKS.MANGA_FILE_WITH_EXTENSION,
                    getFile(
                        ShareMark(Type.MANGA),
                        GeneralConsts.SHARE_MARKS.MANGA_FILE_WITH_EXTENSION
                    )
                )

            if (mIdBook.isEmpty())
                mIdBook = uploadShareFile(
                    drive,
                    mIdFolder,
                    GeneralConsts.SHARE_MARKS.BOOK_FILE_WITH_EXTENSION,
                    getFile(
                        ShareMark(Type.BOOK),
                        GeneralConsts.SHARE_MARKS.BOOK_FILE_WITH_EXTENSION
                    )
                )
        } catch (e: Exception) {
            mLOGGER.error("Error to create share file to drive.", e)
            throw DriveUploadException("")
        }
    }

    private fun getShareFiles(ending: (access: ShareMarkType) -> (Unit)) {
        getDriveService().let { service ->
            if (service != null)
                CoroutineScope(Dispatchers.IO).launch {
                    async {
                        try {
                            var pageToken: String? = null

                            mIdFolder = ""
                            mIdManga = ""
                            mIdBook = ""

                            val limit = 3
                            var books = 0
                            var mangas = 0

                            do {
                                val query =
                                    "(name contains '" + GeneralConsts.SHARE_MARKS.MANGA_FILE + "' or " +
                                            " name contains '" + GeneralConsts.SHARE_MARKS.BOOK_FILE + "' or " +
                                            " name contains '" + GeneralConsts.SHARE_MARKS.FOLDER + "') and " +
                                            "(mimeType='application/json' or mimeType='application/vnd.google-apps.folder')"
                                val result = service.files().list()
                                    .setQ(query)
                                    .setSpaces("drive")
                                    .setFields("nextPageToken, files(id, name)")
                                    .setOrderBy("name desc")
                                    .setPageToken(pageToken)
                                    .execute()

                                for (file in result.files)
                                    when (file.name) {
                                        GeneralConsts.SHARE_MARKS.MANGA_FILE_WITH_EXTENSION -> mIdManga = file.id
                                        GeneralConsts.SHARE_MARKS.BOOK_FILE_WITH_EXTENSION -> mIdBook = file.id
                                        GeneralConsts.SHARE_MARKS.FOLDER -> mIdFolder = file.id
                                        else -> {
                                            if (file.name.contains(GeneralConsts.SHARE_MARKS.MANGA_FILE,true))
                                                mangas++

                                            if (mangas > limit) {
                                                deleteShareFile(service, file.id)
                                                mangas = 0
                                            }

                                            if (file.name.contains(GeneralConsts.SHARE_MARKS.BOOK_FILE,true))
                                                books++

                                            if (books > limit) {
                                                deleteShareFile(service, file.id)
                                                books = 0
                                            }
                                        }
                                    }
                            } while (pageToken != null)

                            if (mIdBook.isEmpty() || mIdManga.isEmpty())
                                createShareFiles(service)

                            downloadShareFile(service, mIdManga)?.let {
                                FileOutputStream(
                                    getFile(
                                        GeneralConsts.SHARE_MARKS.MANGA_FILE_WITH_EXTENSION,
                                        true
                                    )
                                ).use { outputStream ->
                                    it.writeTo(
                                        outputStream
                                    )
                                }
                            }

                            downloadShareFile(service, mIdBook)?.let {
                                FileOutputStream(
                                    getFile(
                                        GeneralConsts.SHARE_MARKS.BOOK_FILE_WITH_EXTENSION,
                                        true
                                    )
                                ).use { outputStream ->
                                    it.writeTo(
                                        outputStream
                                    )
                                }
                            }

                            withContext(Dispatchers.Main) {
                                ending(ShareMarkType.SUCCESS)
                            }
                        } catch (e: UserRecoverableAuthIOException) {
                            withContext(Dispatchers.Main) {
                                val result = ShareMarkType.NEED_PERMISSION_DRIVE
                                result.intent = e.intent
                                ending(result)
                            }
                        } catch (e: DriveDownloadException) {
                            withContext(Dispatchers.Main) {
                                ending(ShareMarkType.ERROR_DOWNLOAD)
                            }
                        } catch (e: Exception) {
                            mLOGGER.error(e.message, e)
                            withContext(Dispatchers.Main) {
                                ending(ShareMarkType.NOT_CONNECT_DRIVE)
                            }
                        }
                    }
                }
            else
                ending(ShareMarkType.NOT_SIGN_IN)
        }
    }

    private fun saveShareFile(
        ending: (processed: ShareMarkType) -> (Unit),
        idFolder: String,
        idFile: String,
        name: String,
        file: File
    ) {
        getDriveService()?.let { service ->
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.IO) {
                    try {
                        uploadShareFile(service, idFolder, idFile, name, file)
                        withContext(Dispatchers.Main) {
                            ending(ShareMarkType.SUCCESS)
                        }
                    } catch (e: DriveUploadException) {
                        withContext(Dispatchers.Main) {
                            ending(ShareMarkType.ERROR_UPLOAD)
                        }
                    } catch (e: Exception) {
                        mLOGGER.error("Error save share file from drive.", e)
                        withContext(Dispatchers.Main) {
                            ending(ShareMarkType.ERROR)
                        }
                    }
                }
            }
        }
    }

    // --------------------------------------------------------- Manga ---------------------------------------------------------
    private fun compare(item: ShareItem, manga: Manga): Boolean {
        return if (manga.lastAccess == null || item.lastAccess.after(GeneralConsts.dateTimeToDate(manga.lastAccess!!))) {
            manga.bookMark = item.bookMark
            manga.lastAccess = GeneralConsts.dateToDateTime(item.lastAccess)
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

    private fun processManga(
        shares: MutableSet<ShareItem>,
        process: List<ShareItem>,
        mangas: List<Manga>,
        update: (manga: Manga) -> (Unit)
    ) {
        for (manga in mangas)
            process.parallelStream().filter { it.file == manga.fileName }.findFirst().also {
                if (it.isPresent) {
                    if (compare(it.get(), manga))
                        update(manga)
                } else
                    shares.add(ShareItem(manga))
            }
    }

    /**
     * @param update  Function call when object is updated
     * @param ending  Function call when process finish, parameter make a process results
     *
     * @throws UserRecoverableAuthIOException if can't be authorized connect on drive
     */
    fun mangaShareMark(
        update: (manga: Manga) -> (Unit),
        ending: (processed: ShareMarkType) -> (Unit)
    ) {
        try {
            getShareFiles { access ->
                if (access == ShareMarkType.SUCCESS) {
                    val gson = GsonBuilder()
                        .setDateFormat(GeneralConsts.SHARE_MARKS.PARSE_DATE_TIME)
                        .excludeFieldsWithoutExposeAnnotation()
                        .create()
                    val repository = MangaRepository(context)

                    val reader = JsonReader(FileReader(getFile(GeneralConsts.SHARE_MARKS.MANGA_FILE_WITH_EXTENSION)))
                    val share: ShareMark = gson.fromJson(reader, ShareMark::class.java)
                    ajusts(share, Type.MANGA)

                    val prefs = GeneralConsts.getSharedPreferences(context)
                    val simpleDate = SimpleDateFormat(GeneralConsts.SHARE_MARKS.PARSE_DATE_TIME, Locale.getDefault())
                    val lastSync = simpleDate.parse(prefs.getString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_MANGA, INITIAL_SYNC_DATE_TIME)!!)!!

                    val list = share.marks!!.filter { it.sync.after(lastSync) }

                    repository.listSync(lastSync).apply {
                            processManga(share.marks!!, list, this) { manga ->
                                repository.update(manga)
                                update(manga)
                            }
                    }

                    list.filter { !it.processed }.forEach {
                        repository.findByFileName(it.file)?.let { manga ->
                            if (compare(it, manga)) {
                                repository.update(manga)
                                update(manga)
                            }
                        }
                    }

                    val isUpdate = if (share.marks!!.isNotEmpty()) share.marks!!.any { it.alter } else false

                    if (isUpdate)
                        saveShareFile(
                            ending,
                            mIdFolder,
                            mIdManga,
                            GeneralConsts.SHARE_MARKS.MANGA_FILE,
                            getFile(share, GeneralConsts.SHARE_MARKS.MANGA_FILE_WITH_EXTENSION)
                        )
                    else
                        ending(ShareMarkType.NOT_ALTERATION)

                    prefs.edit().putString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_MANGA, simpleDate.format(Date())).apply()
                } else
                    ending(access)
            }
        } catch (e: Exception) {
            mLOGGER.error(e.message, e)
            ending(ShareMarkType.ERROR)
        }
    }

    // --------------------------------------------------------- Book ---------------------------------------------------------
    private fun compare(item: ShareItem, book: Book): Boolean {
        return if (book.lastAccess == null || item.lastAccess.after(GeneralConsts.dateTimeToDate(book.lastAccess!!))) {
            book.bookMark = item.bookMark
            book.lastAccess = GeneralConsts.dateToDateTime(item.lastAccess)
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

    private fun processBook(
        shares: MutableSet<ShareItem>,
        process: List<ShareItem>,
        books: List<Book>,
        update: (book: Book) -> (Unit)
    ) {
        for (book in books)
            process.parallelStream().filter { it.file == book.fileName }.findFirst().also {
                if (it.isPresent) {
                    if (compare(it.get(), book))
                        update(book)
                } else
                    shares.add(ShareItem(book))
            }
    }

    /**
     * @param update  Function call when object is updated
     * @param ending  Function call when process finish, parameter is true if can processed list
     *
     * @throws UserRecoverableAuthIOException if can't be authorized to connect on drive
     */
    fun bookShareMark(
        update: (book: Book) -> (Unit),
        ending: (processed: ShareMarkType) -> (Unit)
    ) {
        try {
            getShareFiles { access ->
                if (access == ShareMarkType.SUCCESS) {
                    val gson = GsonBuilder()
                        .setDateFormat(GeneralConsts.SHARE_MARKS.PARSE_DATE_TIME)
                        .excludeFieldsWithoutExposeAnnotation()
                        .create()
                    val repository = BookRepository(context)

                    val reader = JsonReader(FileReader(getFile(GeneralConsts.SHARE_MARKS.BOOK_FILE_WITH_EXTENSION)))
                    val share: ShareMark = gson.fromJson(reader, ShareMark::class.java)
                    ajusts(share, Type.BOOK)

                    val sync = Date()
                    val prefs = GeneralConsts.getSharedPreferences(context)
                    val simpleDate = SimpleDateFormat(GeneralConsts.SHARE_MARKS.PARSE_DATE_TIME, Locale.getDefault())
                    val lastSync = simpleDate.parse(prefs.getString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_BOOK, INITIAL_SYNC_DATE_TIME)!!)!!

                    val list = share.marks!!.filter { it.sync.after(lastSync) }

                    repository.listSync(lastSync).let {
                        if (it.isNotEmpty())
                            processBook(share.marks!!, list, it) { book ->
                                repository.update(book)
                                update(book)
                            }
                    }

                    list.filter { !it.processed }.forEach {
                        repository.findByFileName(it.file)?.let { book ->
                            if (compare(it, book))
                                update(book)
                        }
                    }

                    val isUpdate = if (share.marks!!.isNotEmpty()) share.marks!!.any { it.alter } else false

                    if (isUpdate)
                        saveShareFile(
                            ending,
                            mIdFolder,
                            mIdBook,
                            GeneralConsts.SHARE_MARKS.BOOK_FILE,
                            getFile(share, GeneralConsts.SHARE_MARKS.BOOK_FILE_WITH_EXTENSION)
                        )
                    else
                        ending(ShareMarkType.NOT_ALTERATION)

                    prefs.edit().putString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_BOOK, simpleDate.format(sync)).apply()
                } else
                    ending(access)
            }
        } catch (e: Exception) {
            mLOGGER.error(e.message, e)
            ending(ShareMarkType.ERROR)
        }
    }

}