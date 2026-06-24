package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.ShareMark
import br.com.fenix.bilingualreader.model.enums.Type
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import java.util.Date

object ShareMarkMock {

    fun mockEntity(): ShareMark = ShareMark(
        origin = "Mock Origin",
        lastAlteration = Date(),
        type = Type.MANGA,
        marks = ShareItemMock.mockEntityList()
    )

    fun mockEntityList(): List<ShareMark> = listOf(mockEntity())

    fun asserts(expected: ShareMark?, actual: ShareMark?) {
        assertNotNull("Actual share mark should not be null", actual)
        expected?.let {
            assertEquals("Origin mismatch", it.origin, actual?.origin)
            assertEquals("Type mismatch", it.type, actual?.type)
        }
    }
}
