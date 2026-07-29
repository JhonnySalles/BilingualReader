package br.com.fenix.bilingualreader.service.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DataBaseTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        DataBase.close()
        DataBase.setTestingInstance(null)
        DataBase.isTesting = false
    }

    @After
    fun tearDown() {
        DataBase.close()
        DataBase.setTestingInstance(null)
        DataBase.isTesting = false
    }

    @Test
    fun testSingleton() {
        val db1 = DataBase.getDataBase(context)
        assertNotNull(db1)
        db1.openHelper.writableDatabase
        assertTrue(db1.isOpen)
    }

    @Test
    fun testSetTestingInstance() {
        val mockDb = mockk<DataBase>(relaxed = true)
        every { mockDb.isOpen } returns true
        DataBase.setTestingInstance(mockDb)
        
        val current = DataBase.getDataBase(context)
        assertSame(mockDb, current)
    }
}
