package br.com.fenix.bilingualreader.service.repository

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.util.constants.GeneralConsts

class Storage(context: Context) {

    private val mRepository = MangaRepository(context)
    private val mBookRepository = BookRepository(context)

    // --------------------------------------------------------- Comic / Manga ---------------------------------------------------------
    fun getPrevManga(library: Library, manga: Manga): Manga? {
        var mangas = mRepository.findByFileFolder(manga.file.parent ?: "")
        var idx = mangas!!.indexOf(manga)
        var prev = if (idx > 0) mangas[idx - 1] else null

        if (prev == null) {
            mangas = mRepository.listOrderByTitle(library)
            idx = mangas!!.indexOf(manga)
            prev = if (idx > 0) mangas[idx - 1] else null
        }

        return prev
    }

    fun getNextManga(library: Library, manga: Manga): Manga? {
        var mangas = mRepository.findByFileFolder(manga.file.parent ?: "")
        var idx = mangas!!.indexOf(manga)
        var next = if (idx != mangas.size - 1) mangas[idx + 1] else null

        if (next == null) {
            mangas = mRepository.listOrderByTitle(library)
            idx = mangas!!.indexOf(manga)
            next = if (idx != mangas.size - 1) mangas[idx + 1] else null
        }

        return next
    }

    fun getManga(idManga: Long): Manga? =
        mRepository.get(idManga)

    fun findMangaByName(name: String): Manga? =
        mRepository.findByFileName(name)

    fun listMangas(library: Library): List<Manga>? =
        mRepository.list(library)

    fun listDeleted(library: Library): List<Manga>? =
        mRepository.listDeleted(library)

    fun delete(manga: Manga) {
        mRepository.delete(manga)
    }

    fun updateBookMark(manga: Manga) {
        mRepository.updateBookMark(manga)
    }

    fun save(manga: Manga): Long {
        return if (manga.id != null) {
            mRepository.update(manga)
            manga.id!!
        } else
            mRepository.save(manga)
    }

    // Used to get the cache images
    companion object Storage {
        private val EXTERNAL_PERMS = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
        )

        fun isPermissionGranted(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            // Valid permission on android 10 or above
                Environment.isExternalStorageManager()
            else {
                val readExternalStoragePermission: Int = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                readExternalStoragePermission == PackageManager.PERMISSION_GRANTED
            }
        }

        fun isPermissionWriteGranted(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            // Valid permission on android 10 or above
                Environment.isExternalStorageManager()
            else {
                val readExternalStoragePermission: Int = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )

                val writeExternalStoragePermission: Int = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                readExternalStoragePermission == PackageManager.PERMISSION_GRANTED && writeExternalStoragePermission == PackageManager.PERMISSION_GRANTED
            }
        }

        fun takePermission(context: Context, activity: Activity) =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) try {
                val intent =
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse(
                    String.format(
                        "package:%s",
                        context.packageName
                    )
                )
                activity.startActivity(intent)
            } catch (ex: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                activity.startActivity(intent)
            } else {
                ActivityCompat.requestPermissions(
                    activity,
                    EXTERNAL_PERMS,
                    GeneralConsts.REQUEST.PERMISSION_FILES_ACCESS
                )
            }
    }

    fun updateLastAccess(manga: Manga) {
        mRepository.updateLastAccess(manga)
    }


    // --------------------------------------------------------- Book ---------------------------------------------------------
    fun listBook(): List<Book>? =
        mBookRepository.list()

    fun listBookDeleted(): List<Book>? =
        mBookRepository.listDeleted()

    fun save(book: Book): Long {
        return if (book.id != null) {
            mBookRepository.update(book)
            book.id!!
        } else
            mBookRepository.save(book)
    }

    fun getPrevBook(book: Book): Book? {
        var books = mBookRepository.findByFileFolder(book.file.parent ?: "")
        var idx = books!!.indexOf(book)
        var prev = if (idx > 0) books[idx - 1] else null

        if (prev == null) {
            books = mBookRepository.listOrderByTitle()
            idx = books!!.indexOf(book)
            prev = if (idx > 0) books[idx - 1] else null
        }

        return prev
    }

    fun getNextBook(book: Book): Book? {
        var books = mBookRepository.findByFileFolder(book.file.parent ?: "")
        var idx = books!!.indexOf(book)
        var next = if (idx != books.size - 1) books[idx + 1] else null

        if (next == null) {
            books = mBookRepository.listOrderByTitle()
            idx = books!!.indexOf(book)
            next = if (idx != books.size - 1) books[idx + 1] else null
        }

        return next
    }

    fun getBook(idBook: Long): Book? =
        mBookRepository.get(idBook)

    fun findBookByName(name: String): Book? =
        mBookRepository.findByFileName(name)

    fun delete(book: Book) {
        mBookRepository.delete(book)
    }

    fun updateBookMark(book: Book) {
        mBookRepository.updateBookMark(book)
    }
}