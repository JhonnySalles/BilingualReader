package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config()
class DataBaseTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        DataBase.close()
    }

    @Test
    fun testSingleton() {
        val db1 = DataBase.getDataBase(context)
        val db2 = DataBase.getDataBase(context)
        
        assertSame(db1, db2)
        db1.openHelper.writableDatabase
        assertTrue(db1.isOpen)
        
        DataBase.close()
        // Após fechar, o próximo deve ser uma nova instância (ou pelo menos null/reset)
        val db3 = DataBase.getDataBase(context)
        assertNotSame(db1, db3)
    }

    @Test
    fun testSetTestingInstance() {
        val mockDb = mockk<DataBase>(relaxed = true)
        DataBase.setTestingInstance(mockDb)
        
        val current = DataBase.getDataBase(context)
        assertSame(mockDb, current)
    }
}
