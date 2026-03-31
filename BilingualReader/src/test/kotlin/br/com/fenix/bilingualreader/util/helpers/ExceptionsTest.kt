package br.com.fenix.bilingualreader.util.helpers

import org.junit.Assert.assertEquals
import org.junit.Test

class ExceptionsTest {
    @Test
    fun testExceptions() {
        val backupError = BackupError("backup error")
        assertEquals("backup error", backupError.message)
        
        val invalidDbFile = InvalidDbFile("invalid db file")
        assertEquals("invalid db file", invalidDbFile.message)
        
        val invalidDatabase = InvalidDatabase("invalid database")
        assertEquals("invalid database", invalidDatabase.message)
        
        val restoredNewDatabase = RestoredNewDatabase("restored new database")
        assertEquals("restored new database", restoredNewDatabase.message)
        
        val errorRestoreDatabase = ErrorRestoreDatabase("error restore database")
        assertEquals("error restore database", errorRestoreDatabase.message)
    }
}
