package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.LinkedFile
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.LinkedPage
import java.time.LocalDateTime

class FileLinkRepository(context: Context) {

    private var mDataBase = DataBase.getDataBase(context).getFileLinkDao()
    private var mDataBasePage = DataBase.getDataBase(context).getPageLinkDao()

    fun save(obj: LinkedFile): Long {
        delete(obj.manga!!)
        obj.lastAlteration = LocalDateTime.now()
        val id = mDataBase.save(obj)
        if (obj.pagesLink != null) {
            mDataBasePage.deleteAll(id)
            save(id, obj.pagesLink!!)
            save(id, obj.pagesNotLink!!)
        }

        return id
    }

    private fun save(idFile: Long, pages: List<LinkedPage>) {
        for (page in pages) {
            page.idFile = idFile
            page.id = mDataBasePage.save(page)
        }
    }

    fun update(obj: LinkedFile) {
        obj.lastAlteration = LocalDateTime.now()
        mDataBase.update(obj)
        if (obj.pagesLink != null) {
            mDataBasePage.deleteAll(obj.id!!)
            save(obj.id!!, obj.pagesLink!!)
            save(obj.id!!, obj.pagesNotLink!!)
        }
    }

    fun delete(obj: LinkedFile) {
        if (obj.id != null) {
            mDataBasePage.deleteAll(obj.id!!)
            mDataBase.delete(obj)
        }
    }

    fun delete(obj: Manga) {
        if (obj.id != null) {
            mDataBasePage.deleteAllByManga(obj.id!!)
            mDataBase.deleteAllByManga(obj.id!!)
        }
    }

    fun get(obj: Manga): LinkedFile? {
        val fileLink = if (obj.id != null && obj.id != 0L) mDataBase.getLastAccess(obj.id!!) else null
        if (fileLink != null) {
            fileLink.manga = obj
            fileLink.pagesLink = findPagesLink(fileLink.id!!)
            fileLink.pagesNotLink = findPagesNotLink(fileLink.id!!)
        }
        return fileLink
    }

    fun findByFileName(idManga: Long, name: String, pages: Int): LinkedFile? {
        val fileLink = mDataBase.get(idManga, name, pages)
        if (fileLink != null) {
            fileLink.pagesLink = findPagesLink(fileLink.id!!)
            fileLink.pagesNotLink = findPagesNotLink(fileLink.id!!)
        }
        return fileLink
    }

    private fun findPagesLink(idFileLink: Long): List<LinkedPage> {
        return mDataBasePage.getPageLink(idFileLink)
    }

    private fun findPagesNotLink(idFileLink: Long): List<LinkedPage> {
        return mDataBasePage.getPageNotLink(idFileLink)
    }

    fun findAllByManga(idManga: Long): List<LinkedFile>? =
        mDataBase.get(idManga)

}