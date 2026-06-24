package br.com.fenix.bilingualreader.util.secrets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PkceUtilTest {

    @Test
    fun `test generateCodeVerifier production`() {
        val verifier = PkceUtil.generateCodeVerifier()
        
        assertNotNull(verifier)
        // ByteArray(50) encoded in Base64 (NO_WRAP, NO_PADDING)
        // 50 bytes * 8 bits/byte = 400 bits
        // 400 bits / 6 bits/char = 66.666 -> 67 chars without padding
        assertEquals(67, verifier.length)
        
        // Should only contain Base64 URL safe characters [A-Za-z0-9-_]
        assertTrue(verifier.matches(Regex("^[A-Za-z0-9\\-_]+$")))
    }
}
