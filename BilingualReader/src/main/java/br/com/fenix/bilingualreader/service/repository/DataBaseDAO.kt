package br.com.fenix.bilingualreader.service.repository

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteOpenHelper
import br.com.fenix.bilingualreader.model.entity.*
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts


interface DataBaseDAO<T> {

    @Insert
    fun save(obj: T): Long

    @Insert
    fun save(entities: List<T>)

    @Update
    fun update(obj: T): Int

    @Update
    fun update(entities: List<T>)

    @Delete
    fun delete(obj: T)

}


@Dao
abstract class MangaDAO : DataBaseDAO<Manga> {
    @Query("SELECT count(*) FROM " + DataBaseConsts.MANGA.TABLE_NAME)
    abstract fun getMangaCount(): Int

    @Query("SELECT * FROM " + DataBaseConsts.MANGA.TABLE_NAME + " WHERE " + DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY + " = :library AND " + DataBaseConsts.MANGA.COLUMNS.EXCLUDED + " = 0")
    abstract fun list(library: Long?): List<Manga>

    @Query("SELECT * FROM " + DataBaseConsts.MANGA.TABLE_NAME + " WHERE " + DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY + " = :library AND " + DataBaseConsts.MANGA.COLUMNS.EXCLUDED + " = 0 AND " + DataBaseConsts.MANGA.COLUMNS.LAST_ALTERATION + " >= datetime('now','-1 day')")
    abstract fun listRecentChange(library: Long?): List<Manga>

    @Query("SELECT * FROM " + DataBaseConsts.MANGA.TABLE_NAME + " WHERE " + DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY + " = :library AND " + DataBaseConsts.MANGA.COLUMNS.EXCLUDED + " = 1 AND " + DataBaseConsts.MANGA.COLUMNS.LAST_ALTERATION + " >= datetime('now','-1 day')")
    abstract fun listRecentDeleted(library: Long?): List<Manga>

    @Query(
        "SELECT * FROM ( " +
                " SELECT ${DataBaseConsts.MANGA.COLUMNS.ID}, ${DataBaseConsts.MANGA.COLUMNS.TITLE}, ${DataBaseConsts.MANGA.COLUMNS.FILE_PATH}, " +
                "        ${DataBaseConsts.MANGA.COLUMNS.FILE_FOLDER}, ${DataBaseConsts.MANGA.COLUMNS.FILE_NAME}, ${DataBaseConsts.MANGA.COLUMNS.FILE_SIZE}, ${DataBaseConsts.MANGA.COLUMNS.FILE_TYPE}, " +
                "        ${DataBaseConsts.MANGA.COLUMNS.PAGES}, ${DataBaseConsts.MANGA.COLUMNS.CHAPTERS}, ${DataBaseConsts.MANGA.COLUMNS.BOOK_MARK}, ${DataBaseConsts.MANGA.COLUMNS.FAVORITE}, " +
                "        ${DataBaseConsts.MANGA.COLUMNS.HAS_SUBTITLE}, ${DataBaseConsts.MANGA.COLUMNS.SERIES}, ${DataBaseConsts.MANGA.COLUMNS.PUBLISHER}, ${DataBaseConsts.MANGA.COLUMNS.VOLUME}, " +
                "        ${DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY}, ${DataBaseConsts.MANGA.COLUMNS.EXCLUDED}, ${DataBaseConsts.MANGA.COLUMNS.DATE_CREATE}, " +
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
                "        '' AS ${DataBaseConsts.MANGA.COLUMNS.SERIES}, '' AS ${DataBaseConsts.MANGA.COLUMNS.PUBLISHER}, '' AS ${DataBaseConsts.MANGA.COLUMNS.VOLUME}, " +
                "        -1 AS ${DataBaseConsts.MANGA.COLUMNS.FK_ID_LIBRARY}, 0 AS ${DataBaseConsts.MANGA.COLUMNS.EXCLUDED}, null AS ${DataBaseConsts.MANGA.COLUMNS.DATE_CREATE}, " +
                "        0 AS ${DataBaseConsts.MANGA.COLUMNS.FILE_ALTERATION}, null AS ${DataBaseConsts.MANGA.COLUMNS.LAST_VOCABULARY_IMPORT}, null AS ${DataBaseConsts.MANGA.COLUMNS.LAST_VERIFY}, " +
                "        ${DataBaseConsts.MANGA.COLUMNS.RELEASE}, null AS ${DataBaseConsts.MANGA.COLUMNS.LAST_ALTERATION}, " +
                "        Substr(${DataBaseConsts.MANGA.COLUMNS.LAST_ACCESS}, 0, 12) || '00:00:00.000' AS ${DataBaseConsts.MANGA.COLUMNS.LAST_ACCESS}, " +
                "        Substr(${DataBaseConsts.MANGA.COLUMNS.LAST_ACCESS}, 0, 12) || '25:60:60.000' AS ${DataBaseConsts.MANGA.COLUMNS.SORT} " +
                " FROM  " + DataBaseConsts.MANGA.TABLE_NAME +
                " WHERE " + DataBaseConsts.MANGA.COLUMNS.LAST_ACCESS + " is not null " +
                " GROUP BY Substr(${DataBaseConsts.MANGA.COLUMNS.LAST_ACCESS}, 0, 11)) " +
                "ORDER BY sort DESC "
    )
    abstract fun listHistory(): List<Manga>

    @Query("SELECT * FROM " + DataBaseConsts.MANGA.TABLE_NAME + " WHERE " + DataBaseConsts.MANGA.COLUMNS.EXCLUDED + " = 0 AND " + DataBaseConsts.MANGA.COLUMNS.ID + " = :id")
    abstract fun get(id: Long): Manga

    @Query("SELECT * FROM " + DataBaseConsts.MANGA.TABLE_NAME + " WHERE " + DataBaseConsts.MANGA.COLUMNS.EXCLUDED + " = 0 AND " + DataBaseConsts.MANGA.COLUMNS.FILE_NAME + " = :name")
    abstract fun get(name: String): Manga

    @Query("SELECT * FROM " + DataBaseConsts.MANGA.TABLE_NAME + " WHERE " + DataBaseConsts.MANGA.COLUMNS.EXCLUDED + " = 0 AND " + DataBaseConsts.MANGA.COLUMNS.FILE_PATH + " = :path")
    abstract fun getByPath(path: String): Manga

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

}


@Dao
abstract class BookDAO : DataBaseDAO<Book> {

    @Query("SELECT count(*) FROM " + DataBaseConsts.BOOK.TABLE_NAME)
    abstract fun getCount(): Int

    @Query("SELECT * FROM " + DataBaseConsts.BOOK.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK.COLUMNS.FK_ID_LIBRARY + " = :library AND " + DataBaseConsts.BOOK.COLUMNS.EXCLUDED + " = 0")
    abstract fun list(library: Long?): List<Book>

    @Query("SELECT * FROM " + DataBaseConsts.BOOK.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK.COLUMNS.FK_ID_LIBRARY + " = :library AND " + DataBaseConsts.BOOK.COLUMNS.EXCLUDED + " = 0 AND " + DataBaseConsts.BOOK.COLUMNS.LAST_ALTERATION + " >= datetime('now','-1 day')")
    abstract fun listRecentChange(library: Long?): List<Book>

    @Query("SELECT * FROM " + DataBaseConsts.BOOK.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK.COLUMNS.FK_ID_LIBRARY + " = :library AND " + DataBaseConsts.BOOK.COLUMNS.EXCLUDED + " = 1 AND " + DataBaseConsts.BOOK.COLUMNS.LAST_ALTERATION + " >= datetime('now','-1 day')")
    abstract fun listRecentDeleted(library: Long?): List<Book>

    @Query("SELECT * FROM " + DataBaseConsts.BOOK.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK.COLUMNS.EXCLUDED + " = 0 AND " + DataBaseConsts.BOOK.COLUMNS.ID + " = :id")
    abstract fun get(id: Long): Book

    @Query("SELECT * FROM " + DataBaseConsts.BOOK.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK.COLUMNS.EXCLUDED + " = 0 AND " + DataBaseConsts.BOOK.COLUMNS.FILE_NAME + " = :name")
    abstract fun get(name: String): Book

    @Query("SELECT * FROM " + DataBaseConsts.BOOK.TABLE_NAME + " WHERE " + DataBaseConsts.BOOK.COLUMNS.EXCLUDED + " = 0 AND " + DataBaseConsts.BOOK.COLUMNS.FILE_PATH + " = :path")
    abstract fun getByPath(path: String): Book

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

}


@Dao
abstract class SubTitleDAO : DataBaseDAO<SubTitle> {

    @Query("SELECT * FROM " + DataBaseConsts.SUBTITLES.TABLE_NAME + " WHERE " + DataBaseConsts.SUBTITLES.COLUMNS.FK_ID_MANGA + " = :idManga AND " + DataBaseConsts.SUBTITLES.COLUMNS.ID + " = :Id")
    abstract fun get(idManga: Long, Id: Long): SubTitle

    @Query("SELECT * FROM " + DataBaseConsts.SUBTITLES.TABLE_NAME + " WHERE " + DataBaseConsts.SUBTITLES.COLUMNS.FK_ID_MANGA + " = :idManga LIMIT 1")
    abstract fun findByIdManga(idManga: Long): SubTitle

    @Query("SELECT * FROM " + DataBaseConsts.SUBTITLES.TABLE_NAME + " WHERE " + DataBaseConsts.SUBTITLES.COLUMNS.FK_ID_MANGA + " = :idManga")
    abstract fun listByIdManga(idManga: Long): List<SubTitle>

    @Query("DELETE FROM " + DataBaseConsts.SUBTITLES.TABLE_NAME + " WHERE " + DataBaseConsts.SUBTITLES.COLUMNS.FK_ID_MANGA + " = :idManga")
    abstract fun deleteAll(idManga: Long)

    @Query("UPDATE " + DataBaseConsts.MANGA.TABLE_NAME + " SET " + DataBaseConsts.MANGA.COLUMNS.HAS_SUBTITLE + " = :hasSubtitle" + " WHERE " + DataBaseConsts.MANGA.COLUMNS.ID + " = :idManga")
    abstract fun updateHasSubtitle(idManga: Long, hasSubtitle: Boolean)

}


@Dao
abstract class KanjiJLPTDAO : DataBaseDAO<KanjiJLPT> {

    @Query("SELECT * FROM " + DataBaseConsts.JLPT.TABLE_NAME + " WHERE " + DataBaseConsts.JLPT.COLUMNS.ID + " = :id")
    abstract fun get(id: Long): KanjiJLPT

    @Query("SELECT * FROM " + DataBaseConsts.JLPT.TABLE_NAME)
    abstract fun list(): List<KanjiJLPT>

}


@Dao
abstract class KanjaxDAO : DataBaseDAO<Kanjax> {

    @Query("SELECT * FROM " + DataBaseConsts.KANJAX.TABLE_NAME + " WHERE " + DataBaseConsts.KANJAX.COLUMNS.ID + " = :id")
    abstract fun get(id: Long): Kanjax

    @Query("SELECT * FROM " + DataBaseConsts.KANJAX.TABLE_NAME + " WHERE " + DataBaseConsts.KANJAX.COLUMNS.KANJI + " = :kanji")
    abstract fun get(kanji: String): Kanjax

    @Query("SELECT * FROM " + DataBaseConsts.KANJAX.TABLE_NAME)
    abstract fun list(): List<Kanjax>

}


@Dao
abstract class FileLinkDAO : DataBaseDAO<FileLink> {

    @Query("SELECT * FROM " + DataBaseConsts.FILELINK.TABLE_NAME + " WHERE " + DataBaseConsts.FILELINK.COLUMNS.FK_ID_MANGA + " = :idManga AND " + DataBaseConsts.FILELINK.COLUMNS.FILE_NAME + " = :fileName AND " + DataBaseConsts.FILELINK.COLUMNS.PAGES + " = :pages")
    abstract fun get(idManga: Long, fileName: String, pages: Int): FileLink?

    @Query("SELECT * FROM " + DataBaseConsts.FILELINK.TABLE_NAME + " WHERE " + DataBaseConsts.FILELINK.COLUMNS.FK_ID_MANGA + " = :idManga")
    abstract fun get(idManga: Long): List<FileLink>?

    @Query("SELECT * FROM " + DataBaseConsts.FILELINK.TABLE_NAME + " WHERE " + DataBaseConsts.FILELINK.COLUMNS.FK_ID_MANGA + " = :idManga ORDER BY " + DataBaseConsts.FILELINK.COLUMNS.LAST_ACCESS + " DESC LIMIT 1")
    abstract fun getLastAccess(idManga: Long): FileLink?

    @Query("DELETE FROM " + DataBaseConsts.FILELINK.TABLE_NAME + " WHERE " + DataBaseConsts.FILELINK.COLUMNS.FK_ID_MANGA + " = :idManga AND " + DataBaseConsts.FILELINK.COLUMNS.FILE_NAME + " = :fileName")
    abstract fun delete(idManga: Long, fileName: String)

    @Query("DELETE FROM " + DataBaseConsts.FILELINK.TABLE_NAME + " WHERE " + DataBaseConsts.FILELINK.COLUMNS.FK_ID_MANGA + " = :idManga AND " + DataBaseConsts.FILELINK.COLUMNS.ID + " = :idFileLink")
    abstract fun delete(idManga: Long, idFileLink: Long)

    @Query("DELETE FROM " + DataBaseConsts.FILELINK.TABLE_NAME + " WHERE " + DataBaseConsts.FILELINK.COLUMNS.FK_ID_MANGA + " = :idManga")
    abstract fun deleteAllByManga(idManga: Long)

}


@Dao
abstract class PageLinkDAO : DataBaseDAO<PageLink> {

    @Query("SELECT * FROM " + DataBaseConsts.PAGESLINK.TABLE_NAME + " WHERE " + DataBaseConsts.PAGESLINK.COLUMNS.NOT_LINKED + " = 0 AND " + DataBaseConsts.PAGESLINK.COLUMNS.FK_ID_FILE + " = :idFile")
    abstract fun getPageLink(idFile: Long): List<PageLink>

    @Query("SELECT * FROM " + DataBaseConsts.PAGESLINK.TABLE_NAME + " WHERE " + DataBaseConsts.PAGESLINK.COLUMNS.NOT_LINKED + " = 1 AND " + DataBaseConsts.PAGESLINK.COLUMNS.FK_ID_FILE + " = :idFile")
    abstract fun getPageNotLink(idFile: Long): List<PageLink>

    @Query("DELETE FROM " + DataBaseConsts.PAGESLINK.TABLE_NAME + " WHERE " + DataBaseConsts.PAGESLINK.COLUMNS.FK_ID_FILE + " = :idFile")
    abstract fun deleteAll(idFile: Long)

    @Query("DELETE FROM " + DataBaseConsts.PAGESLINK.TABLE_NAME + " WHERE " + DataBaseConsts.PAGESLINK.COLUMNS.FK_ID_FILE + " in (SELECT " + DataBaseConsts.FILELINK.COLUMNS.ID + " FROM " + DataBaseConsts.FILELINK.TABLE_NAME + " WHERE " + DataBaseConsts.FILELINK.COLUMNS.FK_ID_MANGA + " = :idManga)")
    abstract fun deleteAllByManga(idManga: Long)

}


@Dao
abstract class VocabularyDAO : DataBaseDAO<Vocabulary> {

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
    abstract fun list(
        vocabulary: String,
        basicForm: String,
        favorite: Boolean,
        orderType: String,
        orderInverse: Boolean,
        padding: Int,
        size: Int
    ): List<Vocabulary>

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
    abstract fun listByManga(
        manga: String,
        vocabulary: String,
        basicForm: String,
        favorite: Boolean,
        orderType: String,
        orderInverse: Boolean,
        padding: Int,
        size: Int
    ): List<Vocabulary>

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
    abstract fun listByBook(
        book: String,
        vocabulary: String,
        basicForm: String,
        favorite: Boolean,
        orderType: String,
        orderInverse: Boolean,
        padding: Int,
        size: Int
    ): List<Vocabulary>

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

        if (isManga)
            database.execSQL(
                "INSERT OR REPLACE INTO " + DataBaseConsts.MANGA_VOCABULARY.TABLE_NAME +
                        " (" + DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_MANGA + ',' + DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_VOCABULARY + ',' + DataBaseConsts.MANGA_VOCABULARY.COLUMNS.APPEARS +
                        ") VALUES ($idMangaOrBook, $idVocabulary, $appears)"
            )
        else
            database.execSQL(
                "INSERT OR REPLACE INTO " + DataBaseConsts.BOOK_VOCABULARY.TABLE_NAME +
                        " (" + DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_BOOK + ',' + DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_VOCABULARY + ',' + DataBaseConsts.BOOK_VOCABULARY.COLUMNS.APPEARS +
                        ") VALUES ($idMangaOrBook, $idVocabulary, $appears)"
            )

        database.execSQL(
            "UPDATE " + DataBaseConsts.VOCABULARY.TABLE_NAME + " SET " + DataBaseConsts.VOCABULARY.COLUMNS.APPEARS + " = (" +
                    "    (SELECT SUM(" + DataBaseConsts.MANGA_VOCABULARY.COLUMNS.APPEARS + ") FROM " + DataBaseConsts.MANGA_VOCABULARY.TABLE_NAME +
                    "    WHERE " + DataBaseConsts.MANGA_VOCABULARY.COLUMNS.ID_VOCABULARY + " = $idVocabulary) +" +
                    "    (SELECT SUM(" + DataBaseConsts.BOOK_VOCABULARY.COLUMNS.APPEARS + ") FROM " + DataBaseConsts.BOOK_VOCABULARY.TABLE_NAME +
                    "    WHERE " + DataBaseConsts.BOOK_VOCABULARY.COLUMNS.ID_VOCABULARY + " = $idVocabulary) " +
                    ") WHERE " + DataBaseConsts.VOCABULARY.COLUMNS.ID + " = $idVocabulary"
        )
    }

}


@Dao
abstract class LibrariesDAO : DataBaseDAO<Library> {

    @Query("SELECT * FROM " + DataBaseConsts.LIBRARIES.TABLE_NAME + " WHERE " + DataBaseConsts.LIBRARIES.COLUMNS.EXCLUDED + " = 0")
    abstract fun list(): List<Library>

    @Query("SELECT * FROM " + DataBaseConsts.LIBRARIES.TABLE_NAME + " WHERE " + DataBaseConsts.LIBRARIES.COLUMNS.EXCLUDED + " = 0" + " AND " + DataBaseConsts.LIBRARIES.COLUMNS.ENABLED + " = 1")
    abstract fun listEnabled(): List<Library>

    @Query("SELECT * FROM " + DataBaseConsts.LIBRARIES.TABLE_NAME + " WHERE " + DataBaseConsts.LIBRARIES.COLUMNS.ID + " = :id")
    abstract fun get(id: Long): Library

    @Query("SELECT * FROM " + DataBaseConsts.LIBRARIES.TABLE_NAME + " WHERE " + DataBaseConsts.LIBRARIES.COLUMNS.TYPE + " = :type")
    abstract fun get(type: Libraries): Library

    @Query("UPDATE " + DataBaseConsts.LIBRARIES.TABLE_NAME + " SET " + DataBaseConsts.LIBRARIES.COLUMNS.EXCLUDED + " = 1 WHERE " + DataBaseConsts.LIBRARIES.COLUMNS.ID + " = :id")
    abstract fun delete(id: Long)

    @Query("UPDATE " + DataBaseConsts.LIBRARIES.TABLE_NAME + " SET " + DataBaseConsts.LIBRARIES.COLUMNS.EXCLUDED + " = 1 ")
    abstract fun deleteAll()

    @Query("SELECT * FROM " + DataBaseConsts.LIBRARIES.TABLE_NAME + " WHERE " + DataBaseConsts.LIBRARIES.COLUMNS.EXCLUDED + " = 1 AND " + DataBaseConsts.LIBRARIES.COLUMNS.PATH + " = :path")
    abstract fun findDeleted(path: String): Library?

    @Query("UPDATE " + DataBaseConsts.LIBRARIES.TABLE_NAME + " SET " + DataBaseConsts.LIBRARIES.COLUMNS.EXCLUDED + " = 1 WHERE " + DataBaseConsts.LIBRARIES.COLUMNS.PATH + " = :path")
    abstract fun removeDefault(path: String)

    @Query("SELECT * FROM " + DataBaseConsts.LIBRARIES.TABLE_NAME + " WHERE " + DataBaseConsts.LIBRARIES.COLUMNS.ID + " = :id")
    abstract fun getDefault(id: Long) : Library?

}


@Dao
abstract class BookConfigurationDAO : DataBaseDAO<BookConfiguration> {

    @Query("SELECT * FROM " + DataBaseConsts.BOOKCONFIGURATION.TABLE_NAME + " WHERE " + DataBaseConsts.BOOKCONFIGURATION.COLUMNS.FK_ID_BOOK + " = :idBook")
    abstract fun findConfiguration(idBook: Long): BookConfiguration?

}
