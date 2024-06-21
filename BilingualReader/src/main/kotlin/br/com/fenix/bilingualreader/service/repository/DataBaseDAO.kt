package br.com.fenix.bilingualreader.service.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomWarnings
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteQuery
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.entity.BookConfiguration
import br.com.fenix.bilingualreader.model.entity.BookSearch
import br.com.fenix.bilingualreader.model.entity.History
import br.com.fenix.bilingualreader.model.entity.Kanjax
import br.com.fenix.bilingualreader.model.entity.KanjiJLPT
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.LinkedFile
import br.com.fenix.bilingualreader.model.entity.LinkedPage
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.Statistics
import br.com.fenix.bilingualreader.model.entity.SubTitle
import br.com.fenix.bilingualreader.model.entity.Tags
import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.model.entity.VocabularyBook
import br.com.fenix.bilingualreader.model.entity.VocabularyManga
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import java.time.LocalDateTime
import java.util.Date


interface DataBaseDAO<T> {

    @Insert
    fun save(obj: T): Long

    @Insert
    fun save(entities: List<T>)

    @Insert
    fun saveAll(vararg entities: T)

    @Update
    fun update(obj: T): Int

    @Update
    fun update(entities: List<T>)

    @Update
    fun update(vararg entities: T)

    @Delete
    fun delete(obj: T)


    @RawQuery
    fun list(query: SupportSQLiteQuery): List<T>

    @RawQuery
    fun query(query: SupportSQLiteQuery): T?

}

@Dao
abstract class BaseDAO<T, ID>(private val mTABLE: String, private val mCOLUMN_ID : String) : DataBaseDAO<T> {

    open fun findAll() : List<T> = list(SimpleSQLiteQuery("SELECT * FROM $mTABLE "))

    open fun exist(id: ID): Boolean = query(SimpleSQLiteQuery("SELECT * FROM $mTABLE WHERE $mCOLUMN_ID = ${id.toString()}")) != null

    open fun find(id: ID): T? = query(SimpleSQLiteQuery("SELECT * FROM $mTABLE WHERE $mCOLUMN_ID = ${id.toString()}"))

    open fun findAll(vararg id : ID) : List<T> = findAll(id.toList())

    open fun findAll(ids: List<ID>): List<T> {
        val where = StringBuilder()
        for ((index, id)  in ids.withIndex()) {
            if (index != 0)
                where.append(",")

            where.append("'").append(id).append("'")
        }
        return list(SimpleSQLiteQuery("SELECT * FROM $mTABLE WHERE $mCOLUMN_ID IN ($where)"))
    }

}


@Dao
abstract class MangaDAO : BaseDAO<Manga, Long>(DataBaseConsts.MANGA.TABLE_NAME, DataBaseConsts.MANGA.COLUMNS.ID) {
    @Query("SELECT count(*) FROM " + DataBaseConsts.MANGA.TABLE_NAME)
    abstract fun getMangaCount(): Int

    @Query("SELECT * FROM " + DataBaseConsts.MANGA.TABLE_NAME + " WHERE " + DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY + " = :library AND " + DataBaseConsts.MANGA.COLUMNS.EXCLUDED + " = 0")
    abstract fun list(library: Long?): List<Manga>

    @Query("SELECT * FROM " + DataBaseConsts.MANGA.TABLE_NAME + " WHERE " + DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY + " = :library AND " + DataBaseConsts.MANGA.COLUMNS.EXCLUDED + " = 0 AND " + DataBaseConsts.MANGA.COLUMNS.LAST_ALTERATION + " >= datetime('now','-5 hour')")
    abstract fun listRecentChange(library: Long?): List<Manga>

    @Query("SELECT * FROM " + DataBaseConsts.MANGA.TABLE_NAME + " WHERE " + DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY + " = :library AND " + DataBaseConsts.MANGA.COLUMNS.EXCLUDED + " = 1 AND " + DataBaseConsts.MANGA.COLUMNS.LAST_ALTERATION + " >= datetime('now','-5 hour')")
    abstract fun listRecentDeleted(library: Long?): List<Manga>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        "SELECT * FROM ( " +
                " SELECT ${DataBaseConsts.MANGA.COLUMNS.ID}, ${DataBaseConsts.MANGA.COLUMNS.TITLE}, ${DataBaseConsts.MANGA.COLUMNS.FILE_PATH}, " +
                "        ${DataBaseConsts.MANGA.COLUMNS.FILE_FOLDER}, ${DataBaseConsts.MANGA.COLUMNS.FILE_NAME}, ${DataBaseConsts.MANGA.COLUMNS.FILE_SIZE}, ${DataBaseConsts.MANGA.COLUMNS.FILE_TYPE}, " +
                "        ${DataBaseConsts.MANGA.COLUMNS.PAGES}, ${DataBaseConsts.MANGA.COLUMNS.CHAPTERS}, ${DataBaseConsts.MANGA.COLUMNS.BOOK_MARK}, ${DataBaseConsts.MANGA.COLUMNS.FAVORITE}, " +
                "        ${DataBaseConsts.MANGA.COLUMNS.HAS_SUBTITLE}, ${DataBaseConsts.MANGA.COLUMNS.AUTHOR}, ${DataBaseConsts.MANGA.COLUMNS.SERIES}, ${DataBaseConsts.MANGA.COLUMNS.PUBLISHER}, " +
                "        ${DataBaseConsts.MANGA.COLUMNS.VOLUME}, ${DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY}, ${DataBaseConsts.MANGA.COLUMNS.EXCLUDED}, ${DataBaseConsts.MANGA.COLUMNS.DATE_CREATE}, " +
                "        ${DataBaseConsts.MANGA.COLUMNS.FILE_ALTERATION}, ${DataBaseConsts.MANGA.COLUMNS.LAST_VOCABULARY_IMPORT}, ${DataBaseConsts.MANGA.COLUMNS.LAST_VERIFY}, " +
                "        ${DataBaseConsts.MANGA.COLUMNS.RELEASE}, ${DataBaseConsts.MANGA.COLUMNS.LAST_ALTERATION}, ${DataBaseConsts.MANGA.COLUMNS.LAST_ACCESS}, " +
                "        ${DataBaseConsts.MANGA.COLUMNS.LAST_ACCESS} AS ${DataBaseConsts.MANGA.COLUMNS.SORT}  " +
                " FROM " + DataBaseConsts.MANGA.TABLE_NAME +
                " WHERE " + DataBaseConsts.MANGA.COLUMNS.LAST_ACCESS + " is not null " +
                "UNION" +
                " SELECT null AS ${DataBaseConsts.MANGA.COLUMNS.ID}, '' AS ${DataBaseConsts.MANGA.COLUMNS.TITLE}, '' AS ${DataBaseConsts.MANGA.COLUMNS.FILE_PATH}, " +
                "        '' AS ${DataBaseConsts.MANGA.COLUMNS.FILE_FOLDER}, '' AS ${DataBaseConsts.MANGA.COLUMNS.FILE_NAME}, 0 AS ${DataBaseConsts.MANGA.COLUMNS.FILE_SIZE}, " +
                "        'UNKNOWN' AS ${DataBaseConsts.MANGA.COLUMNS.FILE_TYPE}, 0 AS ${DataBaseConsts.MANGA.COLUMNS.PAGES}, '' AS ${DataBaseConsts.MANGA.COLUMNS.CHAPTERS}, " +
                "        0 AS ${DataBaseConsts.MANGA.COLUMNS.BOOK_MARK}, 0 AS ${DataBaseConsts.MANGA.COLUMNS.FAVORITE}, 1 AS ${DataBaseConsts.MANGA.COLUMNS.HAS_SUBTITLE}, " +
                "        '' AS ${DataBaseConsts.MANGA.COLUMNS.AUTHOR}, '' AS ${DataBaseConsts.MANGA.COLUMNS.SERIES}, '' AS ${DataBaseConsts.MANGA.COLUMNS.PUBLISHER}, " +
                "        '' AS ${DataBaseConsts.MANGA.COLUMNS.VOLUME}, -1 AS ${DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY}, 0 AS ${DataBaseConsts.MANGA.COLUMNS.EXCLUDED}, " +
                "        null AS ${DataBaseConsts.MANGA.COLUMNS.DATE_CREATE}, 0 AS ${DataBaseConsts.MANGA.COLUMNS.FILE_ALTERATION}, null AS ${DataBaseConsts.MANGA.COLUMNS.LAST_VOCABULARY_IMPORT}, " +
                "        null AS ${DataBaseConsts.MANGA.COLUMNS.LAST_VERIFY}, ${DataBaseConsts.MANGA.COLUMNS.RELEASE}, null AS ${DataBaseConsts.MANGA.COLUMNS.LAST_ALTERATION}, " +
                "        Substr(${DataBaseConsts.MANGA.COLUMNS.LAST_ACCESS}, 0, 12) || '23:59:59.999' AS ${DataBaseConsts.MANGA.COLUMNS.LAST_ACCESS}, " +
                "        Substr(${DataBaseConsts.MANGA.COLUMNS.LAST_ACCESS}, 0, 12) || '25:60:60.000' AS ${DataBaseConsts.MANGA.COLUMNS.SORT} " +
                " FROM  " + DataBaseConsts.MANGA.TABLE_NAME +
                " WHERE " + DataBaseConsts.MANGA.COLUMNS.LAST_ACCESS + " is not null " +
                " GROUP BY Substr(${DataBaseConsts.MANGA.COLUMNS.LAST_ACCESS}, 0, 11)) " +
                "ORDER BY sort DESC "
    )
    abstract fun listHistory(): List<Manga>

    @Query("SELECT * FROM " + DataBaseConsts.MANGA.TABLE_NAME + " WHERE " + DataBaseConsts.MANGA.COLUMNS.EXCLUDED + " = 0 AND " + DataBaseConsts.MANGA.COLUMNS.ID + " = :id")
    abstract fun get(id: Long): Manga?

    @Query("SELECT * FROM " + DataBaseConsts.MANGA.TABLE_NAME + " WHERE " + DataBaseConsts.MANGA.COLUMNS.EXCLUDED + " = 0 AND UPPER(" + DataBaseConsts.MANGA.COLUMNS.FILE_NAME + ") = UPPER(:name)")
    abstract fun getByFileName(name: String): Manga?

    @Query("SELECT * FROM " + DataBaseConsts.MANGA.TABLE_NAME + " WHERE " + DataBaseConsts.MANGA.COLUMNS.EXCLUDED + " = 0 AND " + DataBaseConsts.MANGA.COLUMNS.FILE_PATH + " = :path")
    abstract fun getByPath(path: String): Manga?

    @Query("SELECT * FROM " + DataBaseConsts.MANGA.TABLE_NAME + " WHERE " + DataBaseConsts.MANGA.COLUMNS.EXCLUDED + " = 0 AND " + DataBaseConsts.MANGA.COLUMNS.FILE_FOLDER + " = :folder ORDER BY " + DataBaseConsts.MANGA.COLUMNS.TITLE)
    abstract fun listByFolder(folder: String): List<Manga>

    @Query("SELECT * FROM " + DataBaseConsts.MANGA.TABLE_NAME + " WHERE " + DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY + " = :library AND " + DataBaseConsts.MANGA.COLUMNS.EXCLUDED + " = 0 ORDER BY " + DataBaseConsts.MANGA.COLUMNS.TITLE)
    abstract fun listOrderByTitle(library: Long?): List<Manga>

    @Query("UPDATE " + DataBaseConsts.MANGA.TABLE_NAME + " SET " + DataBaseConsts.MANGA.COLUMNS.BOOK_MARK + " = :marker " + " WHERE " + DataBaseConsts.MANGA.COLUMNS.ID + " = :id ")
    abstract fun updateBookMark(id: Long, marker: Int)

    @Query("UPDATE " + DataBaseConsts.MANGA.TABLE_NAME + " SET " + DataBaseConsts.MANGA.COLUMNS.EXCLUDED + " = 1 WHERE " + DataBaseConsts.MANGA.COLUMNS.ID + " = :id")
    abstract fun delete(id: Long)

    @Query("SELECT * FROM " + DataBaseConsts.MANGA.TABLE_NAME + " WHERE " + DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY + " = :library AND " + DataBaseConsts.MANGA.COLUMNS.EXCLUDED + " = 1")
    abstract fun listDeleted(library: Long?): List<Manga>

    @Query("UPDATE " + DataBaseConsts.MANGA.TABLE_NAME + " SET " + DataBaseConsts.MANGA.COLUMNS.EXCLUDED + " = 1 WHERE " + DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY + " = :library")
    abstract fun deleteLibrary(library: Long?)

    @Query("UPDATE " + DataBaseConsts.MANGA.TABLE_NAME + " SET " + DataBaseConsts.MANGA.COLUMNS.EXCLUDED + " = 0 WHERE " + DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY + " = :library AND " + DataBaseConsts.MANGA.COLUMNS.ID + " = :id")
    abstract fun clearDelete(library: Long?, id: Long)

    @Query("SELECT * FROM " + DataBaseConsts.MANGA.TABLE_NAME + " WHERE " + DataBaseConsts.MANGA.COLUMNS.EXCLUDED + " = 0 ORDER BY " + DataBaseConsts.MANGA.COLUMNS.LAST_ACCESS + " DESC LIMIT 2")
    abstract fun getLastOpen(): List<Manga>?

    @Query("SELECT * FROM " + DataBaseConsts.MANGA.TABLE_NAME + " WHERE " + DataBaseConsts.MANGA.COLUMNS.LAST_ACCESS + " >= :date ORDER BY " + DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY + ", " + DataBaseConsts.MANGA.COLUMNS.FILE_NAME)
    abstract fun listSync(date: String): List<Manga>

}


@Dao
abstract class BookDAO : BaseDAO<Book, Long>(DataBaseConsts.BOOK.TABLE_NAME, DataBaseConsts.BOOK.COLUMNS.ID) {

    @Query("SELECT count(*) FROM " + DataBaseConsts.BOOK.TABLE_NAME)
    abstract fun getCount(): Int

    @Query("SELECT * FROM " + DataBaseConsts.BOOK.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK.COLUMNS.FK_ID_LIBRARY + " = :library AND " + DataBaseConsts.BOOK.COLUMNS.EXCLUDED + " = 0")
    abstract fun list(library: Long?): List<Book>

    @Query("SELECT * FROM " + DataBaseConsts.BOOK.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK.COLUMNS.FK_ID_LIBRARY + " = :library AND " + DataBaseConsts.BOOK.COLUMNS.EXCLUDED + " = 0 AND " + DataBaseConsts.BOOK.COLUMNS.LAST_ALTERATION + " >= datetime('now','-5 hour')")
    abstract fun listRecentChange(library: Long?): List<Book>

    @Query("SELECT * FROM " + DataBaseConsts.BOOK.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK.COLUMNS.FK_ID_LIBRARY + " = :library AND " + DataBaseConsts.BOOK.COLUMNS.EXCLUDED + " = 1 AND " + DataBaseConsts.BOOK.COLUMNS.LAST_ALTERATION + " >= datetime('now','-5 hour')")
    abstract fun listRecentDeleted(library: Long?): List<Book>

    @Query("SELECT * FROM " + DataBaseConsts.BOOK.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK.COLUMNS.EXCLUDED + " = 0 AND " + DataBaseConsts.BOOK.COLUMNS.ID + " = :id")
    abstract fun get(id: Long): Book?

    @Query("SELECT * FROM " + DataBaseConsts.BOOK.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK.COLUMNS.EXCLUDED + " = 0 AND " + DataBaseConsts.BOOK.COLUMNS.FILE_NAME + " = :name")
    abstract fun getByFileName(name: String): Book?

    @Query("SELECT * FROM " + DataBaseConsts.BOOK.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK.COLUMNS.EXCLUDED + " = 0 AND " + DataBaseConsts.BOOK.COLUMNS.FILE_PATH + " = :path")
    abstract fun getByPath(path: String): Book?

    @Query("SELECT * FROM " + DataBaseConsts.BOOK.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK.COLUMNS.EXCLUDED + " = 0 AND " + DataBaseConsts.BOOK.COLUMNS.FILE_FOLDER + " = :folder ORDER BY " + DataBaseConsts.BOOK.COLUMNS.TITLE)
    abstract fun listByFolder(folder: String): List<Book>

    @Query("SELECT * FROM " + DataBaseConsts.BOOK.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK.COLUMNS.FK_ID_LIBRARY + " = :library AND " + DataBaseConsts.BOOK.COLUMNS.EXCLUDED + " = 0 ORDER BY " + DataBaseConsts.BOOK.COLUMNS.TITLE)
    abstract fun listOrderByTitle(library: Long?): List<Book>

    @Query("UPDATE " + DataBaseConsts.BOOK.TABLE_NAME + " SET " + DataBaseConsts.BOOK.COLUMNS.BOOK_MARK + " = :marker " + " WHERE " + DataBaseConsts.BOOK.COLUMNS.ID + " = :id ")
    abstract fun updateBookMark(id: Long, marker: Int)

    @Query("UPDATE " + DataBaseConsts.BOOK.TABLE_NAME + " SET " + DataBaseConsts.BOOK.COLUMNS.EXCLUDED + " = 1 WHERE " + DataBaseConsts.BOOK.COLUMNS.ID + " = :id")
    abstract fun delete(id: Long)

    @Query("SELECT * FROM " + DataBaseConsts.BOOK.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK.COLUMNS.FK_ID_LIBRARY + " = :library AND " + DataBaseConsts.BOOK.COLUMNS.EXCLUDED + " = 1")
    abstract fun listDeleted(library: Long?): List<Book>

    @Query("UPDATE " + DataBaseConsts.BOOK.TABLE_NAME + " SET " + DataBaseConsts.BOOK.COLUMNS.EXCLUDED + " = 0 WHERE " + DataBaseConsts.BOOK.COLUMNS.ID + " = :id")
    abstract fun clearDelete(id: Long)

    @Query("SELECT * FROM " + DataBaseConsts.BOOK.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK.COLUMNS.EXCLUDED + " = 0 ORDER BY " + DataBaseConsts.BOOK.COLUMNS.LAST_ACCESS + " DESC LIMIT 2")
    abstract fun getLastOpen(): List<Book>?

    @Query("SELECT * FROM " + DataBaseConsts.BOOK.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK.COLUMNS.LAST_ACCESS + " >= :date ORDER BY " + DataBaseConsts.BOOK.COLUMNS.FK_ID_LIBRARY + ", " + DataBaseConsts.BOOK.COLUMNS.FILE_NAME)
    abstract fun listSync(date: Date): List<Book>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        "SELECT * FROM ( " +
                " SELECT ${DataBaseConsts.BOOK.COLUMNS.ID}, ${DataBaseConsts.BOOK.COLUMNS.TITLE}, ${DataBaseConsts.BOOK.COLUMNS.AUTHOR}, " +
                "        ${DataBaseConsts.BOOK.COLUMNS.PASSWORD}, ${DataBaseConsts.BOOK.COLUMNS.ANNOTATION}, ${DataBaseConsts.BOOK.COLUMNS.YEAR}, " +
                "        ${DataBaseConsts.BOOK.COLUMNS.GENRE}, ${DataBaseConsts.BOOK.COLUMNS.PUBLISHER}, ${DataBaseConsts.BOOK.COLUMNS.ISBN}, " +
                "        ${DataBaseConsts.BOOK.COLUMNS.PAGES}, ${DataBaseConsts.BOOK.COLUMNS.CHAPTER}, ${DataBaseConsts.BOOK.COLUMNS.CHAPTER_DESCRIPTION}, " +
                "        ${DataBaseConsts.BOOK.COLUMNS.BOOK_MARK}, ${DataBaseConsts.BOOK.COLUMNS.LANGUAGE}, ${DataBaseConsts.BOOK.COLUMNS.FILE_PATH}, " +
                "        ${DataBaseConsts.BOOK.COLUMNS.FILE_NAME}, ${DataBaseConsts.BOOK.COLUMNS.FILE_TYPE}, ${DataBaseConsts.BOOK.COLUMNS.FILE_FOLDER}, " +
                "        ${DataBaseConsts.BOOK.COLUMNS.FILE_SIZE}, ${DataBaseConsts.BOOK.COLUMNS.FAVORITE}, ${DataBaseConsts.BOOK.COLUMNS.DATE_CREATE}, " +
                "        ${DataBaseConsts.BOOK.COLUMNS.FK_ID_LIBRARY}, ${DataBaseConsts.BOOK.COLUMNS.TAGS}, ${DataBaseConsts.BOOK.COLUMNS.EXCLUDED}, " +
                "        ${DataBaseConsts.BOOK.COLUMNS.LAST_ALTERATION}, ${DataBaseConsts.BOOK.COLUMNS.FILE_ALTERATION}, ${DataBaseConsts.BOOK.COLUMNS.LAST_VOCABULARY_IMPORT}, " +
                "        ${DataBaseConsts.BOOK.COLUMNS.LAST_VERIFY}, ${DataBaseConsts.BOOK.COLUMNS.LAST_ACCESS}, " +
                "        ${DataBaseConsts.BOOK.COLUMNS.LAST_ACCESS} AS ${DataBaseConsts.BOOK.COLUMNS.SORT}  " +
                " FROM " + DataBaseConsts.BOOK.TABLE_NAME +
                " WHERE " + DataBaseConsts.BOOK.COLUMNS.LAST_ACCESS + " is not null " +
                "UNION" +
                " SELECT null AS ${DataBaseConsts.BOOK.COLUMNS.ID}, '' AS ${DataBaseConsts.BOOK.COLUMNS.TITLE}, '' AS ${DataBaseConsts.BOOK.COLUMNS.AUTHOR}, " +
                "        '' AS ${DataBaseConsts.BOOK.COLUMNS.PASSWORD}, '' AS ${DataBaseConsts.BOOK.COLUMNS.ANNOTATION}, ${DataBaseConsts.BOOK.COLUMNS.YEAR}, " +
                "        '' AS ${DataBaseConsts.BOOK.COLUMNS.GENRE}, '' AS ${DataBaseConsts.BOOK.COLUMNS.PUBLISHER}, '' AS ${DataBaseConsts.BOOK.COLUMNS.ISBN}, " +
                "        0 AS ${DataBaseConsts.BOOK.COLUMNS.PAGES}, '' AS ${DataBaseConsts.BOOK.COLUMNS.CHAPTER}, '' AS ${DataBaseConsts.BOOK.COLUMNS.CHAPTER_DESCRIPTION}, " +
                "        0 AS ${DataBaseConsts.BOOK.COLUMNS.BOOK_MARK}, ${DataBaseConsts.BOOK.COLUMNS.LANGUAGE}, '' AS ${DataBaseConsts.BOOK.COLUMNS.FILE_PATH}, " +
                "        '' AS ${DataBaseConsts.BOOK.COLUMNS.FILE_NAME}, ${DataBaseConsts.BOOK.COLUMNS.FILE_TYPE}, '' AS ${DataBaseConsts.BOOK.COLUMNS.FILE_FOLDER}, " +
                "        0 AS ${DataBaseConsts.BOOK.COLUMNS.FILE_SIZE}, false AS ${DataBaseConsts.BOOK.COLUMNS.FAVORITE}, null AS ${DataBaseConsts.BOOK.COLUMNS.DATE_CREATE}, " +
                "        -1 AS ${DataBaseConsts.BOOK.COLUMNS.FK_ID_LIBRARY}, '' AS ${DataBaseConsts.BOOK.COLUMNS.TAGS}, false AS ${DataBaseConsts.BOOK.COLUMNS.EXCLUDED}, " +
                "        null AS ${DataBaseConsts.BOOK.COLUMNS.LAST_ALTERATION}, false AS ${DataBaseConsts.BOOK.COLUMNS.FILE_ALTERATION}, " +
                "        null AS ${DataBaseConsts.BOOK.COLUMNS.LAST_VOCABULARY_IMPORT}, null AS ${DataBaseConsts.BOOK.COLUMNS.LAST_VERIFY}, " +
                "        Substr(${DataBaseConsts.MANGA.COLUMNS.LAST_ACCESS}, 0, 12) || '23:59:59.999' AS ${DataBaseConsts.MANGA.COLUMNS.LAST_ACCESS}, " +
                "        Substr(${DataBaseConsts.MANGA.COLUMNS.LAST_ACCESS}, 0, 12) || '25:60:60.000' AS ${DataBaseConsts.MANGA.COLUMNS.SORT} " +
                " FROM  " + DataBaseConsts.BOOK.TABLE_NAME +
                " WHERE " + DataBaseConsts.BOOK.COLUMNS.LAST_ACCESS + " is not null " +
                " GROUP BY Substr(${DataBaseConsts.BOOK.COLUMNS.LAST_ACCESS}, 0, 11)) " +
                "ORDER BY sort DESC "
    )
    abstract fun listHistory(): List<Book>

}


@Dao
abstract class SubTitleDAO : BaseDAO<SubTitle, Long>(DataBaseConsts.SUBTITLES.TABLE_NAME, DataBaseConsts.SUBTITLES.COLUMNS.ID) {

    @Query("SELECT * FROM " + DataBaseConsts.SUBTITLES.TABLE_NAME + " WHERE " + DataBaseConsts.SUBTITLES.COLUMNS.FK_ID_MANGA + " = :idManga AND " + DataBaseConsts.SUBTITLES.COLUMNS.ID + " = :Id")
    abstract fun get(idManga: Long, Id: Long): SubTitle

    @Query("SELECT * FROM " + DataBaseConsts.SUBTITLES.TABLE_NAME + " WHERE " + DataBaseConsts.SUBTITLES.COLUMNS.FK_ID_MANGA + " = :idManga LIMIT 1")
    abstract fun findByIdManga(idManga: Long): SubTitle

    @Query("SELECT * FROM " + DataBaseConsts.SUBTITLES.TABLE_NAME + " WHERE " + DataBaseConsts.SUBTITLES.COLUMNS.FK_ID_MANGA + " = :idManga")
    abstract fun listByIdManga(idManga: Long): List<SubTitle>

    @Query("DELETE FROM " + DataBaseConsts.SUBTITLES.TABLE_NAME + " WHERE " + DataBaseConsts.SUBTITLES.COLUMNS.FK_ID_MANGA + " = :idManga")
    abstract fun deleteAll(idManga: Long)

    @Query("UPDATE " + DataBaseConsts.MANGA.TABLE_NAME + " SET " + DataBaseConsts.MANGA.COLUMNS.HAS_SUBTITLE + " = :hasSubtitle, " + DataBaseConsts.MANGA.COLUMNS.LAST_ALTERATION + " = :lastAlteration, " +
            DataBaseConsts.MANGA.COLUMNS.LAST_VOCABULARY_IMPORT + " = null WHERE " + DataBaseConsts.MANGA.COLUMNS.ID + " = :idManga")
    abstract fun updateHasSubtitle(idManga: Long, hasSubtitle: Boolean, lastAlteration : LocalDateTime = LocalDateTime.now())

}


@Dao
abstract class KanjiJLPTDAO : BaseDAO<KanjiJLPT, Long>(DataBaseConsts.JLPT.TABLE_NAME, DataBaseConsts.JLPT.COLUMNS.ID) {

    @Query("SELECT * FROM " + DataBaseConsts.JLPT.TABLE_NAME + " WHERE " + DataBaseConsts.JLPT.COLUMNS.ID + " = :id")
    abstract fun get(id: Long): KanjiJLPT

    @Query("SELECT * FROM " + DataBaseConsts.JLPT.TABLE_NAME)
    abstract fun list(): List<KanjiJLPT>

}


@Dao
abstract class KanjaxDAO : BaseDAO<Kanjax, Long>(DataBaseConsts.KANJAX.TABLE_NAME, DataBaseConsts.KANJAX.COLUMNS.ID) {

    @Query("SELECT * FROM " + DataBaseConsts.KANJAX.TABLE_NAME + " WHERE " + DataBaseConsts.KANJAX.COLUMNS.ID + " = :id")
    abstract fun get(id: Long): Kanjax

    @Query("SELECT * FROM " + DataBaseConsts.KANJAX.TABLE_NAME + " WHERE " + DataBaseConsts.KANJAX.COLUMNS.KANJI + " = :kanji")
    abstract fun get(kanji: String): Kanjax

    @Query("SELECT * FROM " + DataBaseConsts.KANJAX.TABLE_NAME)
    abstract fun list(): List<Kanjax>

}


@Dao
abstract class FileLinkDAO : BaseDAO<LinkedFile, Long>(DataBaseConsts.FILELINK.TABLE_NAME, DataBaseConsts.FILELINK.COLUMNS.ID) {

    @Query("SELECT * FROM " + DataBaseConsts.FILELINK.TABLE_NAME + " WHERE " + DataBaseConsts.FILELINK.COLUMNS.FK_ID_MANGA + " = :idManga AND " + DataBaseConsts.FILELINK.COLUMNS.FILE_NAME + " = :fileName AND " + DataBaseConsts.FILELINK.COLUMNS.PAGES + " = :pages")
    abstract fun get(idManga: Long, fileName: String, pages: Int): LinkedFile?

    @Query("SELECT * FROM " + DataBaseConsts.FILELINK.TABLE_NAME + " WHERE " + DataBaseConsts.FILELINK.COLUMNS.FK_ID_MANGA + " = :idManga")
    abstract fun get(idManga: Long): List<LinkedFile>?

    @Query("SELECT * FROM " + DataBaseConsts.FILELINK.TABLE_NAME + " WHERE " + DataBaseConsts.FILELINK.COLUMNS.FK_ID_MANGA + " = :idManga ORDER BY " + DataBaseConsts.FILELINK.COLUMNS.LAST_ACCESS + " DESC LIMIT 1")
    abstract fun getLastAccess(idManga: Long): LinkedFile?

    @Query("DELETE FROM " + DataBaseConsts.FILELINK.TABLE_NAME + " WHERE " + DataBaseConsts.FILELINK.COLUMNS.FK_ID_MANGA + " = :idManga AND " + DataBaseConsts.FILELINK.COLUMNS.FILE_NAME + " = :fileName")
    abstract fun delete(idManga: Long, fileName: String)

    @Query("DELETE FROM " + DataBaseConsts.FILELINK.TABLE_NAME + " WHERE " + DataBaseConsts.FILELINK.COLUMNS.FK_ID_MANGA + " = :idManga AND " + DataBaseConsts.FILELINK.COLUMNS.ID + " = :idFileLink")
    abstract fun delete(idManga: Long, idFileLink: Long)

    @Query("DELETE FROM " + DataBaseConsts.FILELINK.TABLE_NAME + " WHERE " + DataBaseConsts.FILELINK.COLUMNS.FK_ID_MANGA + " = :idManga")
    abstract fun deleteAllByManga(idManga: Long)

}


@Dao
abstract class PageLinkDAO : BaseDAO<LinkedPage, Long>(DataBaseConsts.PAGESLINK.TABLE_NAME, DataBaseConsts.PAGESLINK.COLUMNS.ID) {

    @Query("SELECT * FROM " + DataBaseConsts.PAGESLINK.TABLE_NAME + " WHERE " + DataBaseConsts.PAGESLINK.COLUMNS.NOT_LINKED + " = 0 AND " + DataBaseConsts.PAGESLINK.COLUMNS.FK_ID_FILE + " = :idFile")
    abstract fun getPageLink(idFile: Long): List<LinkedPage>

    @Query("SELECT * FROM " + DataBaseConsts.PAGESLINK.TABLE_NAME + " WHERE " + DataBaseConsts.PAGESLINK.COLUMNS.NOT_LINKED + " = 1 AND " + DataBaseConsts.PAGESLINK.COLUMNS.FK_ID_FILE + " = :idFile")
    abstract fun getPageNotLink(idFile: Long): List<LinkedPage>

    @Query("DELETE FROM " + DataBaseConsts.PAGESLINK.TABLE_NAME + " WHERE " + DataBaseConsts.PAGESLINK.COLUMNS.FK_ID_FILE + " = :idFile")
    abstract fun deleteAll(idFile: Long)

    @Query("DELETE FROM " + DataBaseConsts.PAGESLINK.TABLE_NAME + " WHERE " + DataBaseConsts.PAGESLINK.COLUMNS.FK_ID_FILE + " in (SELECT " + DataBaseConsts.FILELINK.COLUMNS.ID + " FROM " + DataBaseConsts.FILELINK.TABLE_NAME + " WHERE " + DataBaseConsts.FILELINK.COLUMNS.FK_ID_MANGA + " = :idManga)")
    abstract fun deleteAllByManga(idManga: Long)

}


@Dao
abstract class VocabularyDAO : BaseDAO<Vocabulary, Long>(DataBaseConsts.VOCABULARY.TABLE_NAME, DataBaseConsts.VOCABULARY.COLUMNS.ID) {

    @Query(
        "SELECT V.* " +
                " FROM " + DataBaseConsts.VOCABULARY.TABLE_NAME + " V " +
                " WHERE CASE :favorite WHEN 1 THEN " + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE + " = :favorite ELSE 1 > 0 END " +
                " GROUP BY " + DataBaseConsts.VOCABULARY.COLUMNS.WORD +
                " ORDER BY " +
                " CASE :orderInverse WHEN 0 THEN " +
                "     CASE :orderType WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.WORD + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.WORD +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE +
                "     ELSE '' END  " +
                " ELSE '' END ASC," +
                " CASE :orderInverse WHEN 1 THEN " +
                "     CASE :orderType WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.WORD + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.WORD +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE +
                "     ELSE '' END  " +
                " ELSE '' END DESC," + DataBaseConsts.VOCABULARY.COLUMNS.WORD + " LIMIT :size OFFSET :padding"
    )
    abstract fun list(favorite: Boolean, orderType: String, orderInverse: Boolean, padding: Int, size: Int): List<Vocabulary>

    @Query(
        "SELECT V.* " +
                " FROM " + DataBaseConsts.VOCABULARY.TABLE_NAME + " V " +
                " WHERE (" + DataBaseConsts.VOCABULARY.COLUMNS.WORD + " LIKE '%' || :vocabulary || '%' OR " + DataBaseConsts.VOCABULARY.COLUMNS.BASIC_FORM + " LIKE '%' || :basicForm || '%' )" +
                " AND CASE :favorite WHEN 1 THEN " + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE + " = :favorite ELSE 1 > 0 END " +
                " GROUP BY " + DataBaseConsts.VOCABULARY.COLUMNS.WORD +
                " ORDER BY" +
                " CASE :orderInverse WHEN 0 THEN " +
                "     CASE :orderType WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.WORD + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.WORD +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE +
                "     ELSE '' END  " +
                " ELSE '' END ASC," +
                " CASE :orderInverse WHEN 1 THEN " +
                "     CASE :orderType WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.WORD + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.WORD +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE +
                "     ELSE '' END  " +
                " ELSE '' END DESC," + DataBaseConsts.VOCABULARY.COLUMNS.WORD + " LIMIT :size OFFSET :padding"
    )
    abstract fun list(vocabulary: String, basicForm: String, favorite: Boolean, orderType: String, orderInverse: Boolean, padding: Int, size: Int): List<Vocabulary>

    @Query("SELECT * FROM " + DataBaseConsts.VOCABULARY.TABLE_NAME + " WHERE " + DataBaseConsts.VOCABULARY.COLUMNS.ID + " = :id")
    abstract fun get(id: Long): Vocabulary

    @Query("SELECT * FROM " + DataBaseConsts.VOCABULARY.TABLE_NAME + " WHERE " + DataBaseConsts.VOCABULARY.COLUMNS.WORD + " = :vocabulary AND " + DataBaseConsts.VOCABULARY.COLUMNS.BASIC_FORM + " = :basicForm LIMIT 1")
    abstract fun find(vocabulary: String, basicForm: String): Vocabulary?

    @Query("SELECT * FROM " + DataBaseConsts.VOCABULARY.TABLE_NAME + " WHERE " + DataBaseConsts.VOCABULARY.COLUMNS.WORD + " = :vocabulary OR " + DataBaseConsts.VOCABULARY.COLUMNS.BASIC_FORM + " = :vocabulary LIMIT 1")
    abstract fun find(vocabulary: String): Vocabulary?

    @Query("SELECT * FROM " + DataBaseConsts.VOCABULARY.TABLE_NAME + " WHERE " + DataBaseConsts.VOCABULARY.COLUMNS.WORD + " = :vocabulary OR " + DataBaseConsts.VOCABULARY.COLUMNS.BASIC_FORM + " = :vocabulary")
    abstract fun findAll(vocabulary: String): List<Vocabulary>

    @Query(
        "SELECT * FROM " + DataBaseConsts.VOCABULARY.TABLE_NAME + " WHERE " + DataBaseConsts.VOCABULARY.COLUMNS.WORD + " = :vocabulary " +
                " AND CASE WHEN LENGTH(:basicForm) = 0 THEN " + DataBaseConsts.VOCABULARY.COLUMNS.BASIC_FORM + " IS NULL ELSE " + DataBaseConsts.VOCABULARY.COLUMNS.BASIC_FORM + " = :basicForm END LIMIT 1"
    )
    abstract fun exists(vocabulary: String, basicForm: String): Vocabulary?

    @Query("UPDATE " + DataBaseConsts.MANGA.TABLE_NAME + " SET " + DataBaseConsts.MANGA.COLUMNS.LAST_VOCABULARY_IMPORT + " = :lastImport, " + DataBaseConsts.MANGA.COLUMNS.FILE_ALTERATION + " = :alteration WHERE " + DataBaseConsts.MANGA.COLUMNS.ID + " = :id")
    abstract fun updateMangaImport(id: Long, lastImport: LocalDateTime, alteration : Date)

    @Query("UPDATE " + DataBaseConsts.BOOK.TABLE_NAME + " SET " + DataBaseConsts.BOOK.COLUMNS.LAST_VOCABULARY_IMPORT + " = :lastImport, " + DataBaseConsts.BOOK.COLUMNS.FILE_ALTERATION + " = :alteration WHERE " + DataBaseConsts.BOOK.COLUMNS.ID + " = :id")
    abstract fun updateBookImport(id: Long, lastImport: LocalDateTime, alteration : Date)

    // --------------------------------------------------------- Comic / Manga ---------------------------------------------------------
    @Query(
        "SELECT V.*" +
                " FROM " + DataBaseConsts.VOCABULARY.TABLE_NAME + " V " +
                " INNER JOIN " + DataBaseConsts.MANGA_VOCABULARY.TABLE_NAME + " MGV ON MGV." + DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_VOCABULARY + " = V." + DataBaseConsts.VOCABULARY.COLUMNS.ID +
                " INNER JOIN " + DataBaseConsts.MANGA.TABLE_NAME + " MG ON MGV." + DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_MANGA + " = MG." + DataBaseConsts.MANGA.COLUMNS.ID +
                " WHERE CASE WHEN LENGTH(:manga) <> 0 THEN MG." + DataBaseConsts.MANGA.COLUMNS.TITLE + " LIKE '%' || :manga || '%' ELSE 1 > 0 END " +
                " AND CASE WHEN 1 = :favorite THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE + " = :favorite ELSE 1 > 0 END " +
                " GROUP BY " + DataBaseConsts.VOCABULARY.COLUMNS.WORD +
                " ORDER BY " +
                " CASE :orderInverse WHEN 0 THEN " +
                "     CASE :orderType WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.WORD + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.WORD +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE +
                "     ELSE '' END  " +
                " ELSE '' END ASC," +
                " CASE :orderInverse WHEN 1 THEN " +
                "     CASE :orderType WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.WORD + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.WORD +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE +
                "     ELSE '' END  " +
                " ELSE '' END DESC," + DataBaseConsts.VOCABULARY.COLUMNS.WORD + " LIMIT :size OFFSET :padding"
    )
    abstract fun listByManga(manga: String, favorite: Boolean, orderType: String, orderInverse: Boolean, padding: Int, size: Int): List<Vocabulary>

    @Query(
        "SELECT V.* " +
                " FROM " + DataBaseConsts.VOCABULARY.TABLE_NAME + " V " +
                " INNER JOIN " + DataBaseConsts.MANGA_VOCABULARY.TABLE_NAME + " MGV ON MGV." + DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_VOCABULARY + " = V." + DataBaseConsts.VOCABULARY.COLUMNS.ID +
                " INNER JOIN " + DataBaseConsts.MANGA.TABLE_NAME + " MG ON MGV." + DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_MANGA + " = MG." + DataBaseConsts.MANGA.COLUMNS.ID +
                " WHERE CASE WHEN LENGTH(:manga) <> 0 THEN MG." + DataBaseConsts.MANGA.COLUMNS.TITLE + " LIKE '%' || :manga || '%' ELSE 1 > 0 END " +
                " AND (V." + DataBaseConsts.VOCABULARY.COLUMNS.WORD + " LIKE '%' || :vocabulary || '%' OR V." + DataBaseConsts.VOCABULARY.COLUMNS.BASIC_FORM + " LIKE '%' || :basicForm || '%' )" +
                " AND CASE WHEN 1 = :favorite THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE + " = :favorite ELSE 1 > 0 END " +
                " GROUP BY " + DataBaseConsts.VOCABULARY.COLUMNS.WORD +
                " ORDER BY " +
                " CASE :orderInverse WHEN 0 THEN " +
                "     CASE :orderType WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.WORD + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.WORD +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE +
                "     ELSE '' END  " +
                " ELSE '' END ASC," +
                " CASE :orderInverse WHEN 1 THEN " +
                "     CASE :orderType WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.WORD + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.WORD +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE +
                "     ELSE '' END  " +
                " ELSE '' END DESC," + DataBaseConsts.VOCABULARY.COLUMNS.WORD + " LIMIT :size OFFSET :padding"
    )
    abstract fun listByManga(manga: String, vocabulary: String, basicForm: String, favorite: Boolean, orderType: String, orderInverse: Boolean, padding: Int, size: Int): List<Vocabulary>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        "SELECT V.*, (CASE WHEN LENGTH(:mangaName) <> 0 THEN M." + DataBaseConsts.MANGA.COLUMNS.TITLE + " LIKE :mangaName || '%' ELSE 0 END) AS Ord " +
                " FROM " + DataBaseConsts.MANGA_VOCABULARY.TABLE_NAME + " V " +
                " INNER JOIN " + DataBaseConsts.MANGA.TABLE_NAME + " M ON M." + DataBaseConsts.MANGA.COLUMNS.ID + " = V." + DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_MANGA +
                " WHERE " + DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_VOCABULARY + " = :idVocabulary " +
                " GROUP BY " + DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_MANGA +
                " ORDER BY Ord DESC, M." + DataBaseConsts.MANGA.COLUMNS.TITLE + " ASC"
    )
    abstract fun findMangaByVocabulary(mangaName: String, idVocabulary: Long): List<VocabularyManga>

    @Query("SELECT * FROM " + DataBaseConsts.MANGA_VOCABULARY.TABLE_NAME + " WHERE " + DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_MANGA + " = :idManga GROUP BY " + DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_VOCABULARY)
    abstract fun findByManga(idManga: Long): List<VocabularyManga>

    @Query("SELECT * FROM " + DataBaseConsts.MANGA.TABLE_NAME + " WHERE " + DataBaseConsts.MANGA.COLUMNS.ID + " = :id")
    abstract fun getManga(id: Long): Manga

    // --------------------------------------------------------- Book ---------------------------------------------------------
    @Query(
        "SELECT V.*" +
                " FROM " + DataBaseConsts.VOCABULARY.TABLE_NAME + " V " +
                " INNER JOIN " + DataBaseConsts.BOOK_VOCABULARY.TABLE_NAME + " MBV ON MBV." + DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_VOCABULARY + " = V." + DataBaseConsts.VOCABULARY.COLUMNS.ID +
                " INNER JOIN " + DataBaseConsts.BOOK.TABLE_NAME + " MB ON MBV." + DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_BOOK + " = MB." + DataBaseConsts.BOOK.COLUMNS.ID +
                " WHERE CASE WHEN LENGTH(:book) <> 0 THEN MB." + DataBaseConsts.BOOK.COLUMNS.TITLE + " LIKE '%' || :book || '%' ELSE 1 > 0 END " +
                " AND CASE WHEN 1 = :favorite THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE + " = :favorite ELSE 1 > 0 END " +
                " GROUP BY " + DataBaseConsts.VOCABULARY.COLUMNS.WORD +
                " ORDER BY " +
                " CASE :orderInverse WHEN 0 THEN " +
                "     CASE :orderType WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.WORD + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.WORD +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE +
                "     ELSE '' END  " +
                " ELSE '' END ASC," +
                " CASE :orderInverse WHEN 1 THEN " +
                "     CASE :orderType WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.WORD + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.WORD +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE +
                "     ELSE '' END  " +
                " ELSE '' END DESC," + DataBaseConsts.VOCABULARY.COLUMNS.WORD + " LIMIT :size OFFSET :padding"
    )
    abstract fun listByBook(book: String, favorite: Boolean, orderType: String, orderInverse: Boolean, padding: Int, size: Int): List<Vocabulary>

    @Query(
        "SELECT V.* " +
                " FROM " + DataBaseConsts.VOCABULARY.TABLE_NAME + " V " +
                " INNER JOIN " + DataBaseConsts.BOOK_VOCABULARY.TABLE_NAME + " MBV ON MBV." + DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_VOCABULARY + " = V." + DataBaseConsts.VOCABULARY.COLUMNS.ID +
                " INNER JOIN " + DataBaseConsts.BOOK.TABLE_NAME + " MB ON MBV." + DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_BOOK + " = MB." + DataBaseConsts.BOOK.COLUMNS.ID +
                " WHERE CASE WHEN LENGTH(:book) <> 0 THEN MB." + DataBaseConsts.BOOK.COLUMNS.TITLE + " LIKE '%' || :book || '%' ELSE 1 > 0 END " +
                " AND (V." + DataBaseConsts.VOCABULARY.COLUMNS.WORD + " LIKE '%' || :vocabulary || '%' OR V." + DataBaseConsts.VOCABULARY.COLUMNS.BASIC_FORM + " LIKE '%' || :basicForm || '%' )" +
                " AND CASE WHEN 1 = :favorite THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE + " = :favorite ELSE 1 > 0 END " +
                " GROUP BY " + DataBaseConsts.VOCABULARY.COLUMNS.WORD +
                " ORDER BY " +
                " CASE :orderInverse WHEN 0 THEN " +
                "     CASE :orderType WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.WORD + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.WORD +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE +
                "     ELSE '' END  " +
                " ELSE '' END ASC," +
                " CASE :orderInverse WHEN 1 THEN " +
                "     CASE :orderType WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.WORD + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.WORD +
                "          WHEN '" + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE + "' THEN V." + DataBaseConsts.VOCABULARY.COLUMNS.FAVORITE +
                "     ELSE '' END  " +
                " ELSE '' END DESC," + DataBaseConsts.VOCABULARY.COLUMNS.WORD + " LIMIT :size OFFSET :padding"
    )
    abstract fun listByBook(book: String, vocabulary: String, basicForm: String, favorite: Boolean, orderType: String, orderInverse: Boolean, padding: Int, size: Int): List<Vocabulary>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        "SELECT V.*, (CASE WHEN LENGTH(:bookName) <> 0 THEN M." + DataBaseConsts.BOOK.COLUMNS.TITLE + " LIKE :bookName || '%' ELSE 0 END) AS Ord " +
                " FROM " + DataBaseConsts.BOOK_VOCABULARY.TABLE_NAME + " V " +
                " INNER JOIN " + DataBaseConsts.BOOK.TABLE_NAME + " M ON M." + DataBaseConsts.BOOK.COLUMNS.ID + " = V." + DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_BOOK +
                " WHERE " + DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_VOCABULARY + " = :idVocabulary " +
                " GROUP BY " + DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_BOOK +
                " ORDER BY Ord DESC, M." + DataBaseConsts.BOOK.COLUMNS.TITLE + " ASC"
    )
    abstract fun findBookByVocabulary(bookName: String, idVocabulary: Long): List<VocabularyBook>

    @Query("SELECT * FROM " + DataBaseConsts.BOOK_VOCABULARY.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_BOOK + " = :idBOOK GROUP BY " + DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_VOCABULARY)
    abstract fun findByBook(idBOOK: Long): List<VocabularyBook>

    @Query("SELECT * FROM " + DataBaseConsts.BOOK.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK.COLUMNS.ID + " = :id")
    abstract fun getBook(id: Long): Book

    // --------------------------------------------------------- Subtitle ---------------------------------------------------------
    fun insert(dbHelper: SupportSQLiteOpenHelper, idMangaOrBook: Long, idVocabulary: Long, appears: Int, isManga: Boolean = true) {
        val database = dbHelper.readableDatabase

        val sql = if (isManga)
            "INSERT OR REPLACE INTO " + DataBaseConsts.MANGA_VOCABULARY.TABLE_NAME +
                    " (" + DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID + ',' + DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_MANGA + ',' + DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_VOCABULARY + ',' + DataBaseConsts.MANGA_VOCABULARY.COLUMNS.APPEARS +
                    ") VALUES ((SELECT ID FROM " + DataBaseConsts.MANGA_VOCABULARY.TABLE_NAME +
                " WHERE " + DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_MANGA + " = $idMangaOrBook" +
                " AND " + DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_VOCABULARY + " = $idVocabulary), $idMangaOrBook, $idVocabulary, $appears)"
        else
            "INSERT OR REPLACE INTO " + DataBaseConsts.BOOK_VOCABULARY.TABLE_NAME +
                    " (" + DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID + ',' + DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_BOOK + ',' + DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_VOCABULARY + ',' + DataBaseConsts.BOOK_VOCABULARY.COLUMNS.APPEARS +
                    ") VALUES ((SELECT ID FROM " + DataBaseConsts.BOOK_VOCABULARY.TABLE_NAME +
                    " WHERE " + DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_BOOK + " = $idMangaOrBook" +
                    " AND " + DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_VOCABULARY + " = $idVocabulary), $idMangaOrBook, $idVocabulary, $appears)"

        database.execSQL(sql)

        database.execSQL(
            "UPDATE " + DataBaseConsts.VOCABULARY.TABLE_NAME + " SET " + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS + " = (" +
                    "    IFNULL((SELECT SUM(" + DataBaseConsts.MANGA_VOCABULARY.COLUMNS.APPEARS + ") FROM " + DataBaseConsts.MANGA_VOCABULARY.TABLE_NAME +
                    "    WHERE " + DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_VOCABULARY + " = $idVocabulary), 0) +" +
                    "    IFNULL((SELECT SUM(" + DataBaseConsts.BOOK_VOCABULARY.COLUMNS.APPEARS + ") FROM " + DataBaseConsts.BOOK_VOCABULARY.TABLE_NAME +
                    "    WHERE " + DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_VOCABULARY + " = $idVocabulary), 0) " +
                    ") WHERE " + DataBaseConsts.VOCABULARY.COLUMNS.ID + " = $idVocabulary"
        )
    }

}


@Dao
abstract class LibrariesDAO : BaseDAO<Library, Long>(DataBaseConsts.LIBRARIES.TABLE_NAME, DataBaseConsts.LIBRARIES.COLUMNS.ID) {

    @Query("SELECT * FROM " + DataBaseConsts.LIBRARIES.TABLE_NAME + " WHERE " + DataBaseConsts.LIBRARIES.COLUMNS.EXCLUDED + " = 0")
    abstract fun list(): List<Library>

    @Query("SELECT * FROM " + DataBaseConsts.LIBRARIES.TABLE_NAME + " WHERE " + DataBaseConsts.LIBRARIES.COLUMNS.EXCLUDED + " = 0" + " AND " + DataBaseConsts.LIBRARIES.COLUMNS.ENABLED + " = 1")
    abstract fun listEnabled(): List<Library>

    @Query("SELECT * FROM " + DataBaseConsts.LIBRARIES.TABLE_NAME + " WHERE " + DataBaseConsts.LIBRARIES.COLUMNS.ID + " = :id")
    abstract fun get(id: Long): Library

    @Query("SELECT * FROM " + DataBaseConsts.LIBRARIES.TABLE_NAME + " WHERE " + DataBaseConsts.LIBRARIES.COLUMNS.TYPE + " = :type AND " + DataBaseConsts.LIBRARIES.COLUMNS.LANGUAGE + " = :language")
    abstract fun get(type: Type, language: Libraries): Library

    @Query("UPDATE " + DataBaseConsts.LIBRARIES.TABLE_NAME + " SET " + DataBaseConsts.LIBRARIES.COLUMNS.EXCLUDED + " = 1 WHERE " + DataBaseConsts.LIBRARIES.COLUMNS.ID + " = :id")
    abstract fun delete(id: Long)

    @Query("UPDATE " + DataBaseConsts.LIBRARIES.TABLE_NAME + " SET " + DataBaseConsts.LIBRARIES.COLUMNS.EXCLUDED + " = 1 ")
    abstract fun deleteAll()

    @Query("SELECT * FROM " + DataBaseConsts.LIBRARIES.TABLE_NAME + " WHERE " + DataBaseConsts.LIBRARIES.COLUMNS.EXCLUDED + " = 1 AND " + DataBaseConsts.LIBRARIES.COLUMNS.PATH + " = :path")
    abstract fun findDeleted(path: String): Library?

    @Query("UPDATE " + DataBaseConsts.LIBRARIES.TABLE_NAME + " SET " + DataBaseConsts.LIBRARIES.COLUMNS.EXCLUDED + " = 1 WHERE " + DataBaseConsts.LIBRARIES.COLUMNS.PATH + " = :path")
    abstract fun removeDefault(path: String)

    @Query("SELECT * FROM " + DataBaseConsts.LIBRARIES.TABLE_NAME + " WHERE " + DataBaseConsts.LIBRARIES.COLUMNS.ID + " = :id")
    abstract fun getDefault(id: Long): Library?

    @Query("UPDATE " + DataBaseConsts.MANGA.TABLE_NAME + " SET " + DataBaseConsts.MANGA.COLUMNS.EXCLUDED + " = 1 WHERE " + DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY + " = :idLibrary AND " + DataBaseConsts.MANGA.COLUMNS.FILE_FOLDER + " = :path")
    abstract fun deleteMangaByPath(idLibrary: Long, path: String)

    @Query("UPDATE " + DataBaseConsts.BOOK.TABLE_NAME + " SET " + DataBaseConsts.BOOK.COLUMNS.EXCLUDED + " = 1 WHERE " + DataBaseConsts.BOOK.COLUMNS.FK_ID_LIBRARY + " = :idLibrary AND " + DataBaseConsts.BOOK.COLUMNS.FILE_FOLDER + " = :path")
    abstract fun deleteBookByPath(idLibrary: Long, path: String)

}


@Dao
abstract class BookAnnotationDAO : BaseDAO<BookAnnotation, Long>(DataBaseConsts.BOOK_ANNOTATION.TABLE_NAME, DataBaseConsts.BOOK_ANNOTATION.COLUMNS.ID) {

    @Query(
        "SELECT * FROM " + DataBaseConsts.BOOK_ANNOTATION.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK_CONFIGURATION.COLUMNS.FK_ID_BOOK + " = :idBook " +
        "ORDER BY ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.ALTERATION} DESC"
    )
    abstract fun findAllByBook(idBook: Long): List<BookAnnotation>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        "SELECT * FROM ( " +
                " SELECT ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.ID}, ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.FK_ID_BOOK}, ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.PAGE}, " +
                "        ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.PAGES}, ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.FONT_SIZE}, ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.TYPE}, " +
                "        ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.CHAPTER_NUMBER}, ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.CHAPTER}, ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.TEXT}, " +
                "        ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.ANNOTATION}, ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.FAVORITE}, ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.COLOR}, " +
                "        ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.RANGE}, ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.ALTERATION}, ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.CREATED}, " +
                "        0 AS ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.COUNT} " +
                " FROM " + DataBaseConsts.BOOK_ANNOTATION.TABLE_NAME +
                " WHERE " + DataBaseConsts.BOOK_ANNOTATION.COLUMNS.FK_ID_BOOK + " = :idBook " +
                "UNION" +
                " SELECT null AS ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.ID}, 0 AS ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.FK_ID_BOOK}, 0 AS ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.PAGE}, " +
                "        0 AS ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.PAGES}, ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.FONT_SIZE}, 'BookMark' AS ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.TYPE}, " +
                "        mark.${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.CHAPTER_NUMBER}, mark.${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.CHAPTER}, '' AS ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.TEXT}, " +
                "        '' AS ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.ANNOTATION}, false AS ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.FAVORITE}, 'None' AS ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.COLOR}, " +
                "        '' AS ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.RANGE}, '2000-01-01T00:00:00.000' AS ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.ALTERATION}, '2000-01-01T00:00:00.000' AS ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.CREATED}, " +
                "        (SELECT Count(*) FROM " + DataBaseConsts.BOOK_ANNOTATION.TABLE_NAME + " aux WHERE aux." + DataBaseConsts.BOOK_ANNOTATION.COLUMNS.FK_ID_BOOK + " = mark." + DataBaseConsts.BOOK_ANNOTATION.COLUMNS.FK_ID_BOOK + "" +
                "         AND aux." + DataBaseConsts.BOOK_ANNOTATION.COLUMNS.CHAPTER + " = mark." + DataBaseConsts.BOOK_ANNOTATION.COLUMNS.CHAPTER + ") AS ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.COUNT} " +
                " FROM  " + DataBaseConsts.BOOK_ANNOTATION.TABLE_NAME + " mark " +
                " WHERE mark." + DataBaseConsts.BOOK_ANNOTATION.COLUMNS.FK_ID_BOOK + " = :idBook " +
                " GROUP BY ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.CHAPTER_NUMBER}) " +
                "ORDER BY ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.CHAPTER_NUMBER} ASC, ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.COUNT} DESC "
    )
    abstract fun list(idBook: Long): List<BookAnnotation>

    @Query(
        "SELECT * FROM " + DataBaseConsts.BOOK_ANNOTATION.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK_CONFIGURATION.COLUMNS.FK_ID_BOOK + " = :idBook " +
        "ORDER BY ${DataBaseConsts.BOOK_ANNOTATION.COLUMNS.ALTERATION} DESC"
    )
    abstract fun findByBook(idBook: Long): List<BookAnnotation>

    @Query("SELECT * FROM " + DataBaseConsts.BOOK_ANNOTATION.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK_CONFIGURATION.COLUMNS.FK_ID_BOOK + " = :idBook AND " + DataBaseConsts.BOOK_ANNOTATION.COLUMNS.PAGE + " = :page")
    abstract fun findByPage(idBook: Long, page: Int): List<BookAnnotation>

}


@Dao
abstract class BookConfigurationDAO : BaseDAO<BookConfiguration, Long>(DataBaseConsts.BOOK_CONFIGURATION.TABLE_NAME, DataBaseConsts.BOOK_CONFIGURATION.COLUMNS.ID)  {

    @Query("SELECT * FROM " + DataBaseConsts.BOOK_CONFIGURATION.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK_CONFIGURATION.COLUMNS.FK_ID_BOOK + " = :idBook")
    abstract fun findByBook(idBook: Long): BookConfiguration?

}

@Dao
abstract class BookSearchDAO : BaseDAO<BookSearch, Long>(DataBaseConsts.BOOK_SEARCH_HISTORY.TABLE_NAME, DataBaseConsts.BOOK_SEARCH_HISTORY.COLUMNS.ID) {

    @Query("SELECT * FROM " + DataBaseConsts.BOOK_SEARCH_HISTORY.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK_SEARCH_HISTORY.COLUMNS.FK_ID_BOOK + " = :idBook ORDER BY " + DataBaseConsts.BOOK_SEARCH_HISTORY.COLUMNS.DATE + " DESC")
    abstract fun findAllByBook(idBook: Long): List<BookSearch>

}


@Dao
abstract class TagsDAO : BaseDAO<Tags, Long>(DataBaseConsts.TAGS.TABLE_NAME, DataBaseConsts.TAGS.COLUMNS.ID) {
    @Query("SELECT * FROM " + DataBaseConsts.TAGS.TABLE_NAME + " WHERE " + DataBaseConsts.TAGS.COLUMNS.EXCLUDED + " = 0 AND " + DataBaseConsts.TAGS.COLUMNS.ID + " = :id")
    abstract fun get(id: Long): Tags?

    @Query("SELECT * FROM " + DataBaseConsts.TAGS.TABLE_NAME + " WHERE " + DataBaseConsts.TAGS.COLUMNS.EXCLUDED + " = 0 AND " + DataBaseConsts.TAGS.COLUMNS.NAME + " = :name")
    abstract fun get(name: String): Tags?

    @Query("SELECT * FROM " + DataBaseConsts.TAGS.TABLE_NAME + " WHERE " + DataBaseConsts.TAGS.COLUMNS.EXCLUDED + " = 0 AND " + DataBaseConsts.TAGS.COLUMNS.NAME + " LIKE :name")
    abstract fun valid(name: String): Tags?

    @Query("SELECT * FROM " + DataBaseConsts.TAGS.TABLE_NAME + " WHERE " + DataBaseConsts.TAGS.COLUMNS.EXCLUDED + " = 0 ORDER BY " + DataBaseConsts.TAGS.COLUMNS.NAME)
    abstract fun list(): MutableList<Tags>?
}


@Dao
abstract class HistoryDAO :  BaseDAO<History, Long>(DataBaseConsts.HISTORY.TABLE_NAME, DataBaseConsts.HISTORY.COLUMNS.ID) {

    @Query("SELECT * FROM " + DataBaseConsts.HISTORY.TABLE_NAME + " WHERE " + DataBaseConsts.HISTORY.COLUMNS.TYPE + " = :type AND " + DataBaseConsts.HISTORY.COLUMNS.FK_ID_LIBRARY + " = :idLibrary AND " + DataBaseConsts.HISTORY.COLUMNS.FK_ID_REFERENCE + " = :idReference ORDER BY " + DataBaseConsts.HISTORY.COLUMNS.ID + " DESC LIMIT 1")
    abstract fun last(type: Type, idLibrary: Long, idReference: Long): History?

    @Query("UPDATE " + DataBaseConsts.HISTORY.TABLE_NAME + " SET " + DataBaseConsts.HISTORY.COLUMNS.NOTIFIED + " = 1 WHERE " + DataBaseConsts.HISTORY.COLUMNS.TYPE + " = :type AND " + DataBaseConsts.HISTORY.COLUMNS.FK_ID_LIBRARY + " = :idLibrary AND " + DataBaseConsts.HISTORY.COLUMNS.FK_ID_REFERENCE + " = :idReference")
    abstract fun notify(type: Type, idLibrary: Long, idReference: Long)

    @Query("UPDATE " + DataBaseConsts.HISTORY.TABLE_NAME + " SET " + DataBaseConsts.HISTORY.COLUMNS.NOTIFIED + " = 1 WHERE " + DataBaseConsts.HISTORY.COLUMNS.ID + " = :id")
    abstract fun notify(id: Long)

    @Query("SELECT * FROM " + DataBaseConsts.HISTORY.TABLE_NAME + " WHERE " + DataBaseConsts.HISTORY.COLUMNS.TYPE + " = :type AND " + DataBaseConsts.HISTORY.COLUMNS.FK_ID_LIBRARY + " = :idLibrary AND " + DataBaseConsts.HISTORY.COLUMNS.FK_ID_REFERENCE + " = :idReference ORDER BY " + DataBaseConsts.HISTORY.COLUMNS.ID)
    abstract fun find(type: Type, idLibrary: Long, idReference: Long) : List<History>

}


@Dao
abstract class StatisticsDAO {
    companion object {
        const val SELECT_BOOK = " SELECT CASE WHEN B." + DataBaseConsts.BOOK.COLUMNS.BOOK_MARK + " > 0 AND B." + DataBaseConsts.BOOK.COLUMNS.BOOK_MARK + " < B." + DataBaseConsts.BOOK.COLUMNS.PAGES + " THEN 1 ELSE 0 END AS " + DataBaseConsts.STATISTICS.COLUMNS.READING + ", " +
                "          CASE WHEN B." + DataBaseConsts.BOOK.COLUMNS.BOOK_MARK + " <= 0 THEN 1 ELSE 0 END AS " + DataBaseConsts.STATISTICS.COLUMNS.TO_READ + ", " +
                "          CASE WHEN B." + DataBaseConsts.BOOK.COLUMNS.EXCLUDED + " = 0 THEN 1 ELSE 0 END AS " + DataBaseConsts.STATISTICS.COLUMNS.LIBRARY + "," +
                "          CASE WHEN B." + DataBaseConsts.BOOK.COLUMNS.BOOK_MARK + " > 0 AND B." + DataBaseConsts.BOOK.COLUMNS.BOOK_MARK + " >= B." + DataBaseConsts.BOOK.COLUMNS.PAGES + " THEN 1 ELSE 0 END AS " + DataBaseConsts.STATISTICS.COLUMNS.READ + ", " +
                "          SUM(COALESCE(CASE WHEN B." + DataBaseConsts.BOOK.COLUMNS.BOOK_MARK + " > 0 AND B." + DataBaseConsts.BOOK.COLUMNS.BOOK_MARK + " >= B." + DataBaseConsts.BOOK.COLUMNS.PAGES + " THEN (H." + DataBaseConsts.HISTORY.COLUMNS.PAGE_END + " - H." + DataBaseConsts.HISTORY.COLUMNS.PAGE_START + ") ELSE 0 END, 0)) AS " + DataBaseConsts.STATISTICS.COLUMNS.COMPLETE_READING_PAGES + ", " +
                "          SUM(COALESCE(CASE WHEN B." + DataBaseConsts.BOOK.COLUMNS.BOOK_MARK + " > 0 AND B." + DataBaseConsts.BOOK.COLUMNS.BOOK_MARK + " >= B." + DataBaseConsts.BOOK.COLUMNS.PAGES + " THEN H." + DataBaseConsts.HISTORY.COLUMNS.SECONDS_READ + " ELSE 0 END, 0)) AS " + DataBaseConsts.STATISTICS.COLUMNS.COMPLETE_READING_SECONDS + ", " +
                "          SUM(COALESCE(CASE WHEN B." + DataBaseConsts.BOOK.COLUMNS.BOOK_MARK + " > 0 AND B." + DataBaseConsts.BOOK.COLUMNS.BOOK_MARK + " < B." + DataBaseConsts.BOOK.COLUMNS.PAGES + " THEN (H." + DataBaseConsts.HISTORY.COLUMNS.PAGE_END + " - H." + DataBaseConsts.HISTORY.COLUMNS.PAGE_START + ") ELSE 0 END, 0)) AS " + DataBaseConsts.STATISTICS.COLUMNS.CURRENT_READING_PAGES + ", " +
                "          SUM(COALESCE(CASE WHEN B." + DataBaseConsts.BOOK.COLUMNS.BOOK_MARK + " > 0 AND B." + DataBaseConsts.BOOK.COLUMNS.BOOK_MARK + " < B." + DataBaseConsts.BOOK.COLUMNS.PAGES + " THEN H." + DataBaseConsts.HISTORY.COLUMNS.SECONDS_READ + " ELSE 0 END, 0)) AS " + DataBaseConsts.STATISTICS.COLUMNS.CURRENT_READING_SECONDS + "," +
                "          SUM(COALESCE(H." + DataBaseConsts.HISTORY.COLUMNS.PAGE_END + " - H." + DataBaseConsts.HISTORY.COLUMNS.PAGE_START + ", 0)) AS " + DataBaseConsts.STATISTICS.COLUMNS.TOTAL_READ_PAGES + ", SUM(COALESCE(H." + DataBaseConsts.HISTORY.COLUMNS.SECONDS_READ + ", 0)) AS " + DataBaseConsts.STATISTICS.COLUMNS.TOTAL_READ_SECONDS + ", " +
                "          H." + DataBaseConsts.HISTORY.COLUMNS.DATE_TIME_START +
                "   FROM " + DataBaseConsts.BOOK.TABLE_NAME + " B " +
                "   LEFT JOIN " + DataBaseConsts.HISTORY.TABLE_NAME + " H ON H." + DataBaseConsts.HISTORY.COLUMNS.TYPE + " = 'BOOK' " +
                "     AND H." + DataBaseConsts.HISTORY.COLUMNS.FK_ID_LIBRARY + " = B." + DataBaseConsts.BOOK.COLUMNS.FK_ID_LIBRARY +
                "     AND H." + DataBaseConsts.HISTORY.COLUMNS.FK_ID_REFERENCE + " = B." + DataBaseConsts.BOOK.COLUMNS.ID + "  " +
                "  GROUP BY B." + DataBaseConsts.BOOK.COLUMNS.ID + ", B." + DataBaseConsts.BOOK.COLUMNS.FK_ID_LIBRARY

        const val SELECT_MANGA = " SELECT CASE WHEN M." + DataBaseConsts.MANGA.COLUMNS.BOOK_MARK + " > 0 AND M." + DataBaseConsts.MANGA.COLUMNS.BOOK_MARK + " < M." + DataBaseConsts.MANGA.COLUMNS.PAGES + " THEN 1 ELSE 0 END AS " + DataBaseConsts.STATISTICS.COLUMNS.READING + ", " +
                "          CASE WHEN M." + DataBaseConsts.MANGA.COLUMNS.BOOK_MARK + " <= 0 THEN 1 ELSE 0 END AS " + DataBaseConsts.STATISTICS.COLUMNS.TO_READ + ", " +
                "          CASE WHEN M." + DataBaseConsts.MANGA.COLUMNS.EXCLUDED + " = 0 THEN 1 ELSE 0 END AS " + DataBaseConsts.STATISTICS.COLUMNS.LIBRARY + "," +
                "          CASE WHEN M." + DataBaseConsts.MANGA.COLUMNS.BOOK_MARK + " > 0 AND M." + DataBaseConsts.MANGA.COLUMNS.BOOK_MARK + " >= M." + DataBaseConsts.MANGA.COLUMNS.PAGES + " THEN 1 ELSE 0 END AS " + DataBaseConsts.STATISTICS.COLUMNS.READ + ", " +
                "          SUM(COALESCE(CASE WHEN M." + DataBaseConsts.MANGA.COLUMNS.BOOK_MARK + " > 0 AND M." + DataBaseConsts.MANGA.COLUMNS.BOOK_MARK + " >= M." + DataBaseConsts.MANGA.COLUMNS.PAGES + " THEN (H." + DataBaseConsts.HISTORY.COLUMNS.PAGE_END + " - H." + DataBaseConsts.HISTORY.COLUMNS.PAGE_START + ") ELSE 0 END, 0)) AS " + DataBaseConsts.STATISTICS.COLUMNS.COMPLETE_READING_PAGES + ", " +
                "          SUM(COALESCE(CASE WHEN M." + DataBaseConsts.MANGA.COLUMNS.BOOK_MARK + " > 0 AND M." + DataBaseConsts.MANGA.COLUMNS.BOOK_MARK + " >= M." + DataBaseConsts.MANGA.COLUMNS.PAGES + " THEN H." + DataBaseConsts.HISTORY.COLUMNS.SECONDS_READ + " ELSE 0 END, 0)) AS " + DataBaseConsts.STATISTICS.COLUMNS.COMPLETE_READING_SECONDS + ", " +
                "          SUM(COALESCE(CASE WHEN M." + DataBaseConsts.MANGA.COLUMNS.BOOK_MARK + " > 0 AND M." + DataBaseConsts.MANGA.COLUMNS.BOOK_MARK + " < M." + DataBaseConsts.MANGA.COLUMNS.PAGES + " THEN (H." + DataBaseConsts.HISTORY.COLUMNS.PAGE_END + " - H." + DataBaseConsts.HISTORY.COLUMNS.PAGE_START + ") ELSE 0 END, 0)) AS " + DataBaseConsts.STATISTICS.COLUMNS.CURRENT_READING_PAGES + ", " +
                "          SUM(COALESCE(CASE WHEN M." + DataBaseConsts.MANGA.COLUMNS.BOOK_MARK + " > 0 AND M." + DataBaseConsts.MANGA.COLUMNS.BOOK_MARK + " < M." + DataBaseConsts.MANGA.COLUMNS.PAGES + " THEN H." + DataBaseConsts.HISTORY.COLUMNS.SECONDS_READ + " ELSE 0 END, 0)) AS " + DataBaseConsts.STATISTICS.COLUMNS.CURRENT_READING_SECONDS + "," +
                "          SUM(COALESCE(H." + DataBaseConsts.HISTORY.COLUMNS.PAGE_END + " - H." + DataBaseConsts.HISTORY.COLUMNS.PAGE_START + ", 0)) AS " + DataBaseConsts.STATISTICS.COLUMNS.TOTAL_READ_PAGES + ", SUM(COALESCE(H." + DataBaseConsts.HISTORY.COLUMNS.SECONDS_READ + ", 0)) AS " + DataBaseConsts.STATISTICS.COLUMNS.TOTAL_READ_SECONDS + ", " +
                "          H." + DataBaseConsts.HISTORY.COLUMNS.DATE_TIME_START +
                "   FROM " + DataBaseConsts.MANGA.TABLE_NAME + " M " +
                "   LEFT JOIN " + DataBaseConsts.HISTORY.TABLE_NAME + " H ON H." + DataBaseConsts.HISTORY.COLUMNS.TYPE + " = 'MANGA' " +
                "     AND H." + DataBaseConsts.HISTORY.COLUMNS.FK_ID_LIBRARY + " = M." + DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY +
                "     AND H." + DataBaseConsts.HISTORY.COLUMNS.FK_ID_REFERENCE + " = M." + DataBaseConsts.MANGA.COLUMNS.ID + "  " +
                " GROUP BY M." + DataBaseConsts.MANGA.COLUMNS.ID + ", M." + DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY

        private const val HAVING_NOT_NULL = " HAVING " + DataBaseConsts.STATISTICS.COLUMNS.TYPE + " IS NOT NULL"

        private const val SELECT_FIELDS = " SUM(" + DataBaseConsts.STATISTICS.COLUMNS.READING + ") AS " + DataBaseConsts.STATISTICS.COLUMNS.READING + ", " +
                "      SUM(" + DataBaseConsts.STATISTICS.COLUMNS.TO_READ + ") AS " + DataBaseConsts.STATISTICS.COLUMNS.TO_READ + ", " +
                "      SUM(" + DataBaseConsts.STATISTICS.COLUMNS.LIBRARY + ") AS " + DataBaseConsts.STATISTICS.COLUMNS.LIBRARY + ", " +
                "      SUM(" + DataBaseConsts.STATISTICS.COLUMNS.READ + ") AS " + DataBaseConsts.STATISTICS.COLUMNS.READ + ", " +
                "      SUM(" + DataBaseConsts.STATISTICS.COLUMNS.COMPLETE_READING_PAGES + ") AS " + DataBaseConsts.STATISTICS.COLUMNS.COMPLETE_READING_PAGES + "," +
                "      SUM(" + DataBaseConsts.STATISTICS.COLUMNS.COMPLETE_READING_SECONDS + ") AS " + DataBaseConsts.STATISTICS.COLUMNS.COMPLETE_READING_SECONDS + ", " +
                "      SUM(" + DataBaseConsts.STATISTICS.COLUMNS.CURRENT_READING_PAGES + ") AS " + DataBaseConsts.STATISTICS.COLUMNS.CURRENT_READING_PAGES + ", " +
                "      SUM(" + DataBaseConsts.STATISTICS.COLUMNS.CURRENT_READING_SECONDS + ") AS " + DataBaseConsts.STATISTICS.COLUMNS.CURRENT_READING_SECONDS + "," +
                "      SUM(" + DataBaseConsts.STATISTICS.COLUMNS.TOTAL_READ_PAGES + ") AS " + DataBaseConsts.STATISTICS.COLUMNS.TOTAL_READ_PAGES + ", " +
                "      SUM(" + DataBaseConsts.STATISTICS.COLUMNS.TOTAL_READ_SECONDS + ") AS " + DataBaseConsts.STATISTICS.COLUMNS.TOTAL_READ_SECONDS + ", " +
                "  " + DataBaseConsts.HISTORY.COLUMNS.DATE_TIME_START + " AS " + DataBaseConsts.STATISTICS.COLUMNS.DATE_TIME

        const val SELECT = "SELECT " + SELECT_FIELDS +  ", 'MANGA' AS " + DataBaseConsts.STATISTICS.COLUMNS.TYPE + " FROM (" + SELECT_MANGA +  ")" +
                "   UNION ALL   " +
                "  SELECT " + SELECT_FIELDS + ", 'BOOK' AS " + DataBaseConsts.STATISTICS.COLUMNS.TYPE + " FROM (" + SELECT_BOOK +  ")"

        private const val GROUP_BY_YEAR = " WHERE " + DataBaseConsts.STATISTICS.COLUMNS.TYPE + " = :type AND " + DataBaseConsts.HISTORY.COLUMNS.DATE_TIME_START + " >= :dateStart" +
                "   AND " + DataBaseConsts.HISTORY.COLUMNS.DATE_TIME_START + " <= :dateEnd GROUP BY SUBSTR(" + DataBaseConsts.HISTORY.COLUMNS.DATE_TIME_START + ", 6,7) "

        const val SELECT_YEAR = "SELECT " + SELECT_FIELDS + ", 'MANGA' AS " + DataBaseConsts.STATISTICS.COLUMNS.TYPE + " FROM (" + SELECT_MANGA +  ") " + GROUP_BY_YEAR +
                "   UNION ALL   " +
                "  SELECT " + SELECT_FIELDS + ", 'BOOK' AS " + DataBaseConsts.STATISTICS.COLUMNS.TYPE + " FROM (" + SELECT_BOOK +  ") " + GROUP_BY_YEAR
    }

    @Query(SELECT)
    abstract fun statistics(): List<Statistics>

    @Query(SELECT_YEAR)
    abstract fun statistics(type: Type, dateStart: LocalDateTime, dateEnd: LocalDateTime): List<Statistics>

    @Query("SELECT SUBSTR(" + DataBaseConsts.HISTORY.COLUMNS.DATE_TIME_START + ", 1, 4) AS YEAR FROM " + DataBaseConsts.HISTORY.TABLE_NAME + " GROUP BY YEAR")
    abstract fun listYears(): MutableList<Int>

}

