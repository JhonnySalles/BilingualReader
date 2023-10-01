package br.com.fenix.bilingualreader.service.controller

import android.accounts.NetworkErrorException
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ShareMarkController(var context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(ShareMarkController::class.java)

    companion object {
        const val INITIAL_SYNC_DATE_TIME = "2000-01-01T01:01:01.001-0300"
    }

    private var mIdFolder: String = ""
    private var mIdManga: String = ""
    private var mIdBook: String = ""

    private fun isOnline(): Boolean {
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

    fun clearLastSync() {
        val prefs = GeneralConsts.getSharedPreferences(context)
        with(prefs.edit()) {
            this.remove(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_MANGA)
            this.remove(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_BOOK)
            this.commit()
        }
    }

    // --------------------------------------------------------- Google Drive ---------------------------------------------------------
    private fun ajusts(share: ShareMark, type: Type) {
        if (share.type == null)
            share.type = type

        if (share.marks == null)
            share.marks = mutableSetOf()
        else if (share.marks!!.any { it == null })
            share.marks = share.marks!!.filter { it != null }.toMutableSet()
    }

    private fun setHttpTimeout(requestInitializer : HttpRequestInitializer) : HttpRequestInitializer {
        return HttpRequestInitializer { request ->
            requestInitializer.initialize(request);
            request?.connectTimeout = 5 * 60000; // 5 minutes
            request?.readTimeout = 3 * 60000; // 3 minutes
        }
    }

    private fun getDriveService(): Drive? {
        GoogleSignIn.getLastSignedInAccount(context)?.let { googleAccount ->
            val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_FILE))
            credential.selectedAccount = googleAccount.account
            return Drive.Builder(AndroidHttp.newCompatibleTransport(), JacksonFactory.getDefaultInstance(), setHttpTimeout(credential))
                .setApplicationName(context.getString(R.string.app_name))
                .build()
        }
        return null
    }

    private fun deleteShareFile(drive: Drive, idFile: String) {
        try {
            drive.files().delete(idFile).execute()
        } catch (e: GoogleJsonResponseException) {
            mLOGGER.warn("Error deleted share file from drive.", e)
        } catch (e: Exception) {
            mLOGGER.warn("Error deleted share file from drive.", e)
        }
    }

    private fun downloadShareFile(drive: Drive, idFile: String): ByteArrayOutputStream? {
        return try {
            val outputStream = ByteArrayOutputStream()
            drive.files().get(idFile).executeMediaAndDownloadTo(outputStream)
            outputStream
        } catch (e: GoogleJsonResponseException) {
            mLOGGER.warn("Error download share file from drive.", e)
            throw DriveDownloadException("")
        } catch (e: Exception) {
            mLOGGER.warn("Error download share file from drive.", e)
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
            mLOGGER.warn("Error create share file from drive.", e)
            throw DriveUploadException("")
        } catch (e: Exception) {
            mLOGGER.warn("Error create share file from drive.", e)
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
            gfile.name = nameWithoutExtension + "_" + SimpleDateFormat(GeneralConsts.SHARE_MARKS.DATE_TIME, Locale.getDefault()).format(Date()) + GeneralConsts.SHARE_MARKS.FILE_EXTENSION
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
                        } catch (e: Exception) {
                            mLOGGER.error(e.message, e)
                            withContext(Dispatchers.Main) {
                                when (e) {
                                    is UnknownHostException,
                                    is NetworkErrorException -> ending(ShareMarkType.ERROR_NETWORK)
                                    is DriveDownloadException -> ending(ShareMarkType.ERROR_DOWNLOAD)
                                    is UserRecoverableAuthIOException -> {
                                        val result = ShareMarkType.NEED_PERMISSION_DRIVE
                                        result.intent = e.intent
                                        ending(result)
                                    }
                                    else -> ending(ShareMarkType.NOT_CONNECT_DRIVE)
                                }
                            }
                        }
                    }
                }
            else
                ending(ShareMarkType.NOT_SIGN_IN)
        }
    }

    private fun saveShareFile(idFolder: String, idFile: String, name: String, file: File) : ShareMarkType {
        var ending = ShareMarkType.ERROR_UPLOAD
        getDriveService()?.let { service ->
            ending = try {
                uploadShareFile(service, idFolder, idFile, name, file)
                ShareMarkType.SUCCESS
            } catch (e: DriveUploadException) {
                ShareMarkType.ERROR_UPLOAD
            } catch (e: Exception) {
                mLOGGER.error("Error save share file from drive.", e)
                ShareMarkType.ERROR
            }
        }
        return ending
    }

    // --------------------------------------------------------- Manga ---------------------------------------------------------
    private fun compare(item: ShareItem, manga: Manga): Boolean {
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

    private fun processManga(update: (manga: Manga) -> (Unit), ending: (processed: ShareMarkType) -> (Unit)) {
        CoroutineScope(Dispatchers.IO).launch {
            async {
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
                val lastSync = if (share.marks!!.isEmpty())
                    simpleDate.parse(INITIAL_SYNC_DATE_TIME)!!
                else
                    simpleDate.parse(prefs.getString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_MANGA, INITIAL_SYNC_DATE_TIME)!!)!!

                val list = share.marks!!.filter { it.sync.after(lastSync) }

                repository.listSync(lastSync).apply {
                    for (manga in this)
                        share.marks!!.find { it.file == manga.name }.also {
                            if (it != null) {
                                if (compare(it, manga)) {
                                    repository.update(manga)
                                    withContext(Dispatchers.Main) {
                                        update(manga)
                                    }
                                }
                            } else
                                share.marks!!.add(ShareItem(manga))
                        }
                }

                list.filter { !it.processed }.forEach {
                    repository.findByFileName(it.file)?.let { manga ->
                        if (compare(it, manga)) {
                            repository.update(manga)
                            withContext(Dispatchers.Main) {
                                update(manga)
                            }
                        }
                    }
                }

                val isUpdate = if (share.marks!!.isNotEmpty()) share.marks!!.any { it.alter } else false

                val shared = if (isUpdate)
                    saveShareFile(mIdFolder, mIdManga, GeneralConsts.SHARE_MARKS.MANGA_FILE, getFile(share, GeneralConsts.SHARE_MARKS.MANGA_FILE_WITH_EXTENSION))
                else if (list.isNotEmpty())
                    ShareMarkType.SUCCESS
                else
                    ShareMarkType.NOT_ALTERATION

                ShareMarkType.send = isUpdate
                ShareMarkType.receive = list.isNotEmpty()

                prefs.edit().putString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_MANGA, simpleDate.format(Date())).apply()

                withContext(Dispatchers.Main) {
                    ending(shared)
                }
            }
        }
    }

    /**
     * @param update  Function call when object is updated
     * @param ending  Function call when process finish, parameter make a process results
     *
     */
    fun mangaShareMark(update: (manga: Manga) -> (Unit), ending: (processed: ShareMarkType) -> (Unit)) {
        ShareMarkType.clear()
        if (!isOnline()) {
            ending(ShareMarkType.ERROR_NETWORK)
            return
        }

        try {
            getShareFiles { access ->
                if (access == ShareMarkType.SUCCESS)
                    processManga(update, ending)
                else
                    ending(access)
            }
        } catch (e: Exception) {
            mLOGGER.error(e.message, e)
            ending(ShareMarkType.ERROR)
        }
    }

    // --------------------------------------------------------- Book ---------------------------------------------------------
    private fun compare(item: ShareItem, book: Book): Boolean {
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

    private fun processBook(update: (book: Book) -> (Unit), ending: (processed: ShareMarkType) -> (Unit)) {
        CoroutineScope(Dispatchers.IO).launch {
            async {

                val gson = GsonBuilder()
                    .setDateFormat(GeneralConsts.SHARE_MARKS.PARSE_DATE_TIME)
                    .excludeFieldsWithoutExposeAnnotation()
                    .create()
                val repository = BookRepository(context)

                val reader = JsonReader(FileReader(getFile(GeneralConsts.SHARE_MARKS.BOOK_FILE_WITH_EXTENSION)))
                val share: ShareMark = gson.fromJson(reader, ShareMark::class.java)
                ajusts(share, Type.BOOK)

                val prefs = GeneralConsts.getSharedPreferences(context)
                val simpleDate = SimpleDateFormat(GeneralConsts.SHARE_MARKS.PARSE_DATE_TIME, Locale.getDefault())
                val lastSync = simpleDate.parse(prefs.getString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_BOOK, INITIAL_SYNC_DATE_TIME)!!)!!

                val list = share.marks!!.filter { it.sync.after(lastSync) }

                repository.listSync(lastSync).apply {
                    for (book in this)
                        list.parallelStream().filter { it.file == book.name }.findFirst().also {
                            if (it.isPresent) {
                                if (compare(it.get(), book)) {
                                    repository.update(book)
                                    withContext(Dispatchers.Main) {
                                        update(book)
                                    }
                                }
                            } else
                                share.marks!!.add(ShareItem(book))
                        }
                }

                list.filter { !it.processed }.forEach {
                    repository.findByFileName(it.file)?.let { book ->
                        if (compare(it, book)) {
                            repository.update(book)
                            withContext(Dispatchers.Main) {
                                update(book)
                            }
                        }
                    }
                }

                val isUpdate = if (share.marks!!.isNotEmpty()) share.marks!!.any { it.alter } else false

                val shared = if (isUpdate)
                    saveShareFile(mIdFolder, mIdBook, GeneralConsts.SHARE_MARKS.BOOK_FILE, getFile(share, GeneralConsts.SHARE_MARKS.BOOK_FILE_WITH_EXTENSION))
                else
                    ShareMarkType.NOT_ALTERATION

                withContext(Dispatchers.Main) {
                    ending(shared)
                }

                prefs.edit().putString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_BOOK, simpleDate.format(Date())).apply()
            }
        }
    }

    /**
     * @param update  Function call when object is updated
     * @param ending  Function call when process finish, parameter is true if can processed list
     *
     */
    fun bookShareMark(update: (book: Book) -> (Unit), ending: (processed: ShareMarkType) -> (Unit)) {
        ShareMarkType.clear()
        if (!isOnline()) {
            ending(ShareMarkType.ERROR_NETWORK)
            return
        }

        try {
            getShareFiles { access ->
                if (access == ShareMarkType.SUCCESS)
                    processBook(update, ending)
                else
                    ending(access)
            }
        } catch (e: Exception) {
            mLOGGER.error(e.message, e)
            ending(ShareMarkType.ERROR)
        }
    }

}