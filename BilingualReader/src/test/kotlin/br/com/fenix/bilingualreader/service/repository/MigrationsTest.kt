package br.com.fenix.bilingualreader.service.repository

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class MigrationsTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DataBase::class.java.canonicalName ?: "br.com.fenix.bilingualreader.service.repository.DataBase",
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        println("Working Dir: " + System.getProperty("user.dir"))
        var db = helper.createDatabase(TEST_DB, 1)

        // Database has schema version 1. Insert some data using SQL queries.
        // You cannot use DAO classes because they expect the latest schema.
        db.execSQL("INSERT INTO Manga (title, pages, chapters, book_mark, completed, path, size, name, type, folder, favorite, has_subtitle, author, series, genre, publisher, volume, excluded, file_alteration) " +
                   "VALUES ('Test', 100, 'Chapter 1', 0, 0, 'path', 1024, 'Test Name', 'epub', 'root', 0, 0, 'Author', 'Series', 'Genre', 'Publisher', '1', 0, 0)")

        // Prepare for the next version.
        db.close()

        // Re-open the database with version 2 and provide MIGRATION_1_2 as the migration object.
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, Migrations.MIGRATION_1_2)

        // MigrationTestHelper automatically verifies the schema changes, but you may want to validate data.
        val cursor = db.query("SELECT * FROM " + DataBaseConsts.MANGA.TABLE_NAME)
        assert(cursor.moveToFirst())
        val columnIndex = cursor.getColumnIndex(DataBaseConsts.MANGA.COLUMNS.CHAPTERS_PAGES)
        assert(columnIndex != -1)
        cursor.close()
    }
}
