package br.com.ebook.util

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

object PalmDocDecompressor {

    fun decompress(file: File): ByteArray {
        RandomAccessFile(file, "r").use { raf ->
            val header = ByteArray(78)
            raf.readFully(header)
            
            val numRecords = ((header[76].toInt() and 0xFF) shl 8) or (header[77].toInt() and 0xFF)
            val recordOffsets = IntArray(numRecords)
            for (i in 0 until numRecords) {
                val offset = raf.readInt()
                raf.readInt() // Attributes (1 byte) + Unique ID (3 bytes)
                recordOffsets[i] = offset
            }
            
            if (numRecords <= 1) {
                throw IOException("PDB sem registros de texto.")
            }
            
            // Record 0 contains PalmDOC header info
            raf.seek(recordOffsets[0].toLong())
            val compression = raf.readUnsignedShort()
            raf.readUnsignedShort() // Unused
            val textLength = raf.readInt()
            val numTextRecs = raf.readUnsignedShort()
            raf.readUnsignedShort() // recordSize (unused)
            
            val out = ByteArrayOutputStream(textLength)
            
            for (i in 1..numTextRecs) {
                if (i >= numRecords) break
                val startOffset = recordOffsets[i].toLong()
                val endOffset = if (i + 1 < numRecords) recordOffsets[i + 1].toLong() else raf.length()
                val compressedSize = (endOffset - startOffset).toInt()
                
                val compressedData = ByteArray(compressedSize)
                raf.seek(startOffset)
                raf.readFully(compressedData)
                
                if (compression == 2) {
                    decompressChunk(compressedData, out)
                } else {
                    out.write(compressedData)
                }
            }
            
            return out.toByteArray()
        }
    }
    
    private fun decompressChunk(data: ByteArray, out: ByteArrayOutputStream) {
        var i = 0
        val n = data.size
        // Buffer local para o chunk descompactado (tamanho máximo de record costuma ser 4096, deixamos folga)
        val chunkBuf = ByteArray(8192)
        var chunkLen = 0
        
        while (i < n) {
            val c = data[i].toInt() and 0xFF
            i++
            if (c == 0) {
                chunkBuf[chunkLen++] = 0
            } else if (c in 1..8) {
                if (i + c <= n) {
                    System.arraycopy(data, i, chunkBuf, chunkLen, c)
                    chunkLen += c
                    i += c
                } else {
                    break
                }
            } else if (c in 9..0x7F) {
                chunkBuf[chunkLen++] = c.toByte()
            } else if (c in 0x80..0xBF) {
                if (i < n) {
                    val c2 = data[i].toInt() and 0xFF
                    i++
                    val m = ((c and 0x3F) shl 8) or c2
                    val offset = m shr 3
                    val length = (m and 0x07) + 3
                    
                    var start = chunkLen - offset
                    if (start >= 0) {
                        for (idx in 0 until length) {
                            chunkBuf[chunkLen++] = chunkBuf[start++]
                        }
                    }
                }
            } else { // 0xC0..0xFF
                chunkBuf[chunkLen++] = 32
                chunkBuf[chunkLen++] = (c xor 0x80).toByte()
            }
        }
        out.write(chunkBuf, 0, chunkLen)
    }
}
