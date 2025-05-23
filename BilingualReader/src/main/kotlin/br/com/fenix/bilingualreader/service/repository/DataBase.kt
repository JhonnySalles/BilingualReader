package br.com.fenix.bilingualreader.service.repository

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.net.Uri
import android.widget.Toast
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.fenix.bilingualreader.MainActivity
import br.com.fenix.bilingualreader.R
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
import br.com.fenix.bilingualreader.model.entity.MangaAnnotation
import br.com.fenix.bilingualreader.model.entity.SubTitle
import br.com.fenix.bilingualreader.model.entity.Tags
import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.model.entity.VocabularyBook
import br.com.fenix.bilingualreader.model.entity.VocabularyManga
import br.com.fenix.bilingualreader.util.helpers.BackupError
import br.com.fenix.bilingualreader.util.helpers.Converters
import br.com.fenix.bilingualreader.util.helpers.ErrorRestoreDatabase
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File


@Database(
    version = 3, exportSchema = true,
    entities = [Manga::class, MangaAnnotation::class, Library::class, SubTitle::class, KanjiJLPT::class, Kanjax::class,
        LinkedFile::class, LinkedPage::class, Vocabulary::class, VocabularyManga::class, VocabularyBook::class,
        Book::class, BookAnnotation::class, BookConfiguration::class, BookSearch::class, Tags::class, History::class]
)
@TypeConverters(Converters::class)
abstract class DataBase : RoomDatabase() {

    abstract fun getMangaDao(): MangaDAO
    abstract fun getMangaAnnotation(): MangaAnnotationDAO
    abstract fun getBookDao(): BookDAO
    abstract fun getBookAnnotation(): BookAnnotationDAO
    abstract fun getBookSearch(): BookSearchDAO
    abstract fun getBookConfigurationDao(): BookConfigurationDAO
    abstract fun getSubTitleDao(): SubTitleDAO
    abstract fun getKanjiJLPTDao(): KanjiJLPTDAO
    abstract fun getKanjaxDao(): KanjaxDAO
    abstract fun getFileLinkDao(): FileLinkDAO
    abstract fun getPageLinkDao(): PageLinkDAO
    abstract fun getVocabularyDao(): VocabularyDAO
    abstract fun getLibrariesDao(): LibrariesDAO
    abstract fun getTagsDao(): TagsDAO
    abstract fun getHistoryDao(): HistoryDAO
    abstract fun getStatisticsDao(): StatisticsDAO

    // Singleton - One database initialize only
    companion object {
        private val mLOGGER = LoggerFactory.getLogger(DataBase::class.java)
        private const val DATABASE_NAME = "BilingualReader.db"

        lateinit var mAssets: AssetManager
        private lateinit var INSTANCE: DataBase

        fun getDataBase(context: Context): DataBase {
            if (!::INSTANCE.isInitialized)
                mAssets = context.assets
            synchronized(DataBase::class.java) { // Used for a two or many cores
                INSTANCE = Room.databaseBuilder(context, DataBase::class.java, DATABASE_NAME)
                    .addCallback(rdc)
                    .addMigrations(
                        Migrations.MIGRATION_1_2,
                        Migrations.MIGRATION_2_3,
                        Migrations.MIGRATION_3_4,
                        Migrations.MIGRATION_4_5,
                        Migrations.MIGRATION_5_6,
                        Migrations.MIGRATION_6_7,
                        Migrations.MIGRATION_7_8,
                        Migrations.MIGRATION_8_9,
                        Migrations.MIGRATION_9_10,
                        Migrations.MIGRATION_10_11,
                        Migrations.MIGRATION_11_12,
                        Migrations.MIGRATION_12_13,
                        Migrations.MIGRATION_13_14
                    )
                    .allowMainThreadQueries()
                    /*.setQueryCallback(object : QueryCallback { // Shows query
                        override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                            println("SQL Query: $sqlQuery SQL Args: $bindArgs")
                        }
                    }, Executors.newSingleThreadExecutor())*/
                    .build() // MainThread uses another thread in db conection
            }
            return INSTANCE
        }

        fun close() {
            if (::INSTANCE.isInitialized)
                INSTANCE.close()
        }

        private var rdc: Callback = object : Callback() {
            override fun onCreate(database: SupportSQLiteDatabase) {
                mLOGGER.info("Create initial database data....")

                val kanji = mAssets.open("kanji.sql").bufferedReader().use(BufferedReader::readText)
                database.execSQL(Migrations.SQLINITIAL.KANJI + kanji)

                val kanjax = mAssets.open("kanjax.sql").bufferedReader().use(BufferedReader::readText)
                database.execSQL(Migrations.SQLINITIAL.KANJAX + kanjax)

                val vocabulary = mAssets.open("vocabulary.sql").bufferedReader().use(BufferedReader::readText)
                database.execSQL(Migrations.SQLINITIAL.VOCABULARY + vocabulary)

                mLOGGER.info("Completed initial database data.")
            }
        }


        private lateinit var BACKUP : RoomBackup
        fun initializeBackup(activity: Activity) {
            if (!::BACKUP.isInitialized)
                BACKUP = RoomBackup(activity)
        }

        // Backup and restore
        fun backupDatabase(context: Context, file: File) {
            BACKUP.database(INSTANCE)
                .enableLogDebug(true)
                .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_FILE)
                .backupLocationCustomFile(File(file.path))
                .customBackupFileName(file.name)
                .backupIsEncrypted(false)
                .apply {
                    onCompleteListener { success, message, exitCode ->
                        mLOGGER.warn("Backup database. success: $success, msg: $message, code: $exitCode.")
                        if (success)
                            BACKUP.restartApp(Intent(context, MainActivity::class.java))
                        else {
                            mLOGGER.error("Error when backup database: $message.")
                            Firebase.crashlytics.apply {
                                setCustomKey("message", "Error when backup database: " + message)
                                recordException(BackupError(message))
                            }
                            throw BackupError("Error when backup database")
                        }
                    }
                }.backup()
        }

        fun autoBackupDatabase(context: Context, isRestart: Boolean = false) {
            mLOGGER.warn("Generate auto backup...")
            BACKUP.database(INSTANCE)
                .enableLogDebug(true)
                .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_INTERNAL)
                .maxFileCount(5)
                .backupIsEncrypted(false)
                .apply {
                    onCompleteListener { success, message, exitCode ->
                        mLOGGER.warn("Auto backup database. success: $success, msg: $message, code: $exitCode.")
                        if (success) {
                            if (isRestart) {
                                Toast.makeText(context, context.getString(R.string.config_database_backup_success), Toast.LENGTH_LONG).show()
                                BACKUP.restartApp(Intent(context, MainActivity::class.java))
                            }
                        } else {
                            mLOGGER.error("Error when auto backup database: $message.")
                            Firebase.crashlytics.apply {
                                setCustomKey("message", "Error when auto backup database: " + message)
                                recordException(BackupError(message))
                            }
                        }
                    }
                }.backup()
        }

        fun restoreDatabase(context: Context, file: File) {
            BACKUP.database(INSTANCE)
                .enableLogDebug(true)
                .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_FILE)
                .backupLocationCustomFile(File(file.path))
                .backupIsEncrypted(false)
                .apply {
                    onCompleteListener { success, message, exitCode ->
                        mLOGGER.error("Restore backup database. success: $success, msg: $message, code: $exitCode.")
                        if (success) {
                            Toast.makeText(context, context.getString(R.string.config_database_restore_success), Toast.LENGTH_LONG).show()
                            BACKUP.restartApp(Intent(context, MainActivity::class.java))
                        } else {
                            mLOGGER.error("Error when restore backup database: $message.")
                            Firebase.crashlytics.apply {
                                setCustomKey("message", "Error when restore backup database: " + message)
                                recordException(ErrorRestoreDatabase(message))
                            }
                            throw ErrorRestoreDatabase("Error when restore backup database file.")
                        }
                    }
                }.restore()
        }

        fun validDatabaseFile(context: Context, file: Uri): Boolean {
            val cr: ContentResolver = context.contentResolver
            val mime = cr.getType(file)
            return "application/octet-stream" == mime
        }
    }
}