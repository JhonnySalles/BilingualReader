package br.com.fenix.bilingualreader.model.dao

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.service.repository.Migrations
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedReader

@RunWith(AndroidJUnit4::class)
class VocabularyDAOLoadTest {

    private lateinit var db: DataBase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        DataBase.mAssets = context.assets

        val rdc = object : RoomDatabase.Callback() {
            override fun onCreate(database: SupportSQLiteDatabase) {
                val kanji = context.assets.open("kanji.sql").bufferedReader().use(BufferedReader::readText)
                database.execSQL(Migrations.SQLINITIAL.KANJI + kanji)

                val kanjax = context.assets.open("kanjax.sql").bufferedReader().use(BufferedReader::readText)
                database.execSQL(Migrations.SQLINITIAL.KANJAX + kanjax)

                val vocabulary = context.assets.open("vocabulary.sql").bufferedReader().use(BufferedReader::readText)
                database.execSQL(Migrations.SQLINITIAL.VOCABULARY + vocabulary)
            }
        }

        db = Room.inMemoryDatabaseBuilder(context, DataBase::class.java)
            .addCallback(rdc)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun verifyInitialDataLoaded() {
        val voucherCount = db.getVocabularyDao().list(false, "word", false, 0, 100).size
        val kanjiCount = db.getKanjiJLPTDao().list().size
        val kanjaxCount = db.getKanjaxDao().list().size

        assertTrue("Vocabulary should be loaded from assets", voucherCount > 0)
        assertTrue("Kanji should be loaded from assets", kanjiCount > 0)
        assertTrue("Kanjax should be loaded from assets", kanjaxCount > 0)
    }
}
