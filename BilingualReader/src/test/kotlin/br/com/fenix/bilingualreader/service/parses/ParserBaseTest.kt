package br.com.fenix.bilingualreader.service.parses
 
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
abstract class ParserBaseTest {
 
    @get:Rule
    val tempFolder = TemporaryFolder()
 
    protected lateinit var testDir: File
 
    @Before
    open fun setUp() {
        testDir = tempFolder.newFolder("parser_tests")
    }
 
    @After
    open fun tearDown() {
        unmockkAll()
        // TemporaryFolder Rule automatically deletes the folder
    }
 
    protected fun createSampleFile(fileName: String, content: String = "dummy content"): File {
        val file = File(testDir, fileName)
        file.writeText(content)
        return file
    }
}
