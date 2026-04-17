package br.com.fenix.bilingualreader.service.parses
 
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Path

@Config(sdk = [33])
abstract class ParserBaseTest {
 
    @TempDir
    lateinit var tempDir: Path
 
    protected lateinit var testDir: File
 
    @BeforeEach
    open fun setUp() {
        testDir = tempDir.resolve("parser_tests").toFile()
        testDir.mkdirs()
    }
 
    @AfterEach
    open fun tearDown() {
        unmockkAll()
    }
 
    protected fun createSampleFile(fileName: String, content: String = "dummy content"): File {
        val file = File(testDir, fileName)
        file.writeText(content)
        return file
    }
}
