package br.com.ebook.extractor

import br.com.ebook.foobnix.mobi.parser.MobiParser
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.ByteBuffer

class MobiParserTest {

    @Test
    fun testParsePalmDocPdbWithoutMobiHeader() {
        val pdbFile = File("f:/Projetos/BilingualReader/teste/01-86--EIGHTY-SIX, Vol. 01.pdb")
        if (!pdbFile.exists()) {
            return
        }

        val bytes = pdbFile.readBytes()
        val buffer = ByteBuffer.wrap(bytes)

        val parser = MobiParser(buffer)
        assertFalse(parser.isMobi, "PDB PalmDOC file must not be identified as MOBI")
        assertNull(parser.getCoverOrThumb(), "PDB PalmDOC file has no embedded image cover")
        assertNotNull(parser.title)

        runBlocking {
            val metadataResult = MobiBookExtractor.extractMetadata(pdbFile.path)
            val metadata = metadataResult.getOrNull()
            assertNotNull(metadata)
            assertFalse(metadata!!.title.isBlank())

            val coverResult = MobiBookExtractor.extractCover(pdbFile.path)
            assertNull(coverResult.getOrNull())
        }
    }

    @Test
    fun testMockPalmDocBufferWithExthFlagDoesNotLoop() {
        // Simula cabeçalho de PalmDOC com 2 records (16 bytes rec0) e bit 0x40 ativo no offset 128
        val buf = ByteBuffer.allocate(1024)
        // Nome do banco
        val nameBytes = "Test PalmDoc Book".toByteArray()
        buf.position(0)
        buf.put(nameBytes)
        // Número de records: 2
        buf.position(76)
        buf.putShort(2)
        // Offset record 0: 96
        buf.position(78)
        buf.putInt(96)
        buf.putInt(0) // uid
        // Offset record 1: 112
        buf.position(86)
        buf.putInt(112)
        buf.putInt(1) // uid
        // Record 0 (PalmDOC: 16 bytes: 96..111)
        buf.position(96)
        buf.putShort(2) // compression PalmDOC
        buf.putShort(0)
        buf.putInt(100) // text length
        buf.putShort(1) // num recs
        buf.putShort(4096)
        // Offset 96 + 128 = 224: coloca bit 0x40 para simular a flag antiga que causava travamento
        buf.position(224)
        buf.put(0x40.toByte())

        val parser = MobiParser(buf)
        assertFalse(parser.isMobi)
        assertNull(parser.getCoverOrThumb())
    }
}
