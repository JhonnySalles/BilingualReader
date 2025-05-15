package br.com.fenix.bilingualreader.service.repository

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.fenix.bilingualreader.model.enums.PaginationType
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import org.slf4j.LoggerFactory


class Migrations {
    object SQLINITIAL {
        const val KANJI: String = "INSERT INTO " + DataBaseConsts.JLPT.TABLE_NAME +
                " (" + DataBaseConsts.JLPT.COLUMNS.KANJI + ", " + DataBaseConsts.JLPT.COLUMNS.LEVEL + ") VALUES "

        const val KANJAX: String = "INSERT INTO " + DataBaseConsts.KANJAX.TABLE_NAME +
                " (" + DataBaseConsts.KANJAX.COLUMNS.ID + ", " + DataBaseConsts.KANJAX.COLUMNS.KANJI + ", " +
                DataBaseConsts.KANJAX.COLUMNS.KEYWORD + ", " + DataBaseConsts.KANJAX.COLUMNS.MEANING + ", " +
                DataBaseConsts.KANJAX.COLUMNS.KOOHII + ", " + DataBaseConsts.KANJAX.COLUMNS.KOOHII2 + ", " +
                DataBaseConsts.KANJAX.COLUMNS.ONYOMI + ", " + DataBaseConsts.KANJAX.COLUMNS.KUNYOMI + ", " +
                DataBaseConsts.KANJAX.COLUMNS.ONWORDS + ", " + DataBaseConsts.KANJAX.COLUMNS.KUNWORDS + ", " +
                DataBaseConsts.KANJAX.COLUMNS.JLPT + ", " + DataBaseConsts.KANJAX.COLUMNS.GRADE + ", " +
                DataBaseConsts.KANJAX.COLUMNS.FREQUENCE + ", " + DataBaseConsts.KANJAX.COLUMNS.STROKES + ", " +
                DataBaseConsts.KANJAX.COLUMNS.VARIANTS + ", " + DataBaseConsts.KANJAX.COLUMNS.RADICAL + ", " +
                DataBaseConsts.KANJAX.COLUMNS.PARTS + ", " + DataBaseConsts.KANJAX.COLUMNS.UTF8 + ", " +
                DataBaseConsts.KANJAX.COLUMNS.SJIS + ", " + DataBaseConsts.KANJAX.COLUMNS.KEYWORDS_PT + ", " +
                DataBaseConsts.KANJAX.COLUMNS.MEANING_PT + ") VALUES "

        const val VOCABULARY: String = "INSERT INTO " + DataBaseConsts.VOCABULARY.TABLE_NAME +
                " (" + DataBaseConsts.VOCABULARY.COLUMNS.WORD + ", " + DataBaseConsts.VOCABULARY.COLUMNS.BASIC_FORM + ", " +
                DataBaseConsts.VOCABULARY.COLUMNS.READING + ", " + DataBaseConsts.VOCABULARY.COLUMNS.ENGLISH + ", " +
                DataBaseConsts.VOCABULARY.COLUMNS.PORTUGUESE + ", " + DataBaseConsts.VOCABULARY.COLUMNS.JLPT + ", " +
                DataBaseConsts.VOCABULARY.COLUMNS.REVISED  + ") VALUES "

    }

    companion object {
        private val mLOGGER = LoggerFactory.getLogger(Migrations::class.java)

        // Migration version 2.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                mLOGGER.info("Start migration 1 - 2...")

                database.execSQL("CREATE TABLE IF NOT EXISTS " + DataBaseConsts.MANGA_ANNOTATION.TABLE_NAME + " (" +
                        DataBaseConsts.MANGA_ANNOTATION.COLUMNS.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + DataBaseConsts.MANGA_ANNOTATION.COLUMNS.FK_ID_MANGA + " INTEGER NOT NULL, " +
                        DataBaseConsts.MANGA_ANNOTATION.COLUMNS.PAGE + " INTEGER NOT NULL, " + DataBaseConsts.MANGA_ANNOTATION.COLUMNS.PAGES + " INTEGER NOT NULL, " +
                        DataBaseConsts.MANGA_ANNOTATION.COLUMNS.TYPE+ " TEXT NOT NULL, " + DataBaseConsts.MANGA_ANNOTATION.COLUMNS.CHAPTER + " TEXT NOT NULL, " +
                        DataBaseConsts.MANGA_ANNOTATION.COLUMNS.FOLDER + " TEXT NOT NULL, " + DataBaseConsts.MANGA_ANNOTATION.COLUMNS.ANNOTATION + " TEXT NOT NULL, " +
                        DataBaseConsts.MANGA_ANNOTATION.COLUMNS.ALTERATION + " TEXT NOT NULL, " + DataBaseConsts.MANGA_ANNOTATION.COLUMNS.CREATED + " TEXT NOT NULL)")

                database.execSQL("CREATE INDEX IF NOT EXISTS index_" + DataBaseConsts.MANGA_ANNOTATION.TABLE_NAME + "_" + DataBaseConsts.MANGA_ANNOTATION.COLUMNS.FK_ID_MANGA + "_" + DataBaseConsts.MANGA_ANNOTATION.COLUMNS.CHAPTER +
                        " ON " + DataBaseConsts.MANGA_ANNOTATION.TABLE_NAME + "(" + DataBaseConsts.MANGA_ANNOTATION.COLUMNS.FK_ID_MANGA + ", " + DataBaseConsts.MANGA_ANNOTATION.COLUMNS.CHAPTER + ")")

                try {
                    database.execSQL("ALTER TABLE " + DataBaseConsts.MANGA.TABLE_NAME + " ADD COLUMN " + DataBaseConsts.MANGA.COLUMNS.CHAPTERS_PAGES + " TEXT DEFAULT '' NOT NULL")
                } catch (e : Exception) {
                    mLOGGER.error("Error to alter table and create column chapters page: " + e.message, e)
                }

                mLOGGER.info("Completed migration 1 - 2.")
            }
        }

        // Migration version 3.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                mLOGGER.info("Start migration 2 - 3...")

                try {
                    database.execSQL("ALTER TABLE " + DataBaseConsts.BOOK_CONFIGURATION.TABLE_NAME + " ADD COLUMN " + DataBaseConsts.BOOK_CONFIGURATION.COLUMNS.PAGINATION + " TEXT DEFAULT '" + PaginationType.Default.name + "' NOT NULL")
                } catch (e : Exception) {
                    mLOGGER.error("Error to alter table and create column pagination: " + e.message, e)
                }

                mLOGGER.info("Completed migration 2 - 3.")
            }
        }

        // Migration version 4.
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                mLOGGER.info("Start migration 3 - 4...")

                mLOGGER.info("Completed migration 3 - 4.")
            }
        }

        // Migration version 5.
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                mLOGGER.info("Start migration 4 - 5...")

                mLOGGER.info("Completed migration 4 - 5.")
            }
        }

        // Migration version 6.
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                mLOGGER.info("Start migration 5 - 6...")

                mLOGGER.info("Completed migration 5 - 6.")
            }
        }

        // Migration version 7.
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                mLOGGER.info("Start migration 6 - 7...")

                mLOGGER.info("Completed migration 6 - 7.")
            }
        }

        // Migration version 8.
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                mLOGGER.info("Start migration 7 - 8...")

                mLOGGER.info("Completed migration 7 - 8.")
            }
        }

        // Migration version 9.
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                mLOGGER.info("Start migration 8 - 9...")

                mLOGGER.info("Completed migration 8 - 9.")
            }
        }

        // Migration version 9.
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                mLOGGER.info("Start migration 9 - 10...")

                mLOGGER.info("Completed migration 9 - 10.")
            }
        }

        // Migration version 10.
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                mLOGGER.info("Start migration 10 - 11...")

                mLOGGER.info("Completed migration 10 - 11.")
            }
        }

        // Migration version 11.
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                mLOGGER.info("Start migration 11 - 12...")


                mLOGGER.info("Completed migration 11 - 12.")
            }
        }

        // Migration version 12.
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                mLOGGER.info("Start migration 12 - 13...")


                mLOGGER.info("Completed migration 12 - 13.")
            }
        }

        // Migration version 13.
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                mLOGGER.info("Start migration 13 - 14...")

                mLOGGER.info("Completed migration 13 - 14.")
            }
        }

        // Migration version 14.
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                mLOGGER.info("Start migration 14 - 15...")

                mLOGGER.info("Completed migration 14 - 15.")
            }
        }
    }
}