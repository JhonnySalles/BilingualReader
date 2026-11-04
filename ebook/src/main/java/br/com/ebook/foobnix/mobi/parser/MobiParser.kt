package br.com.ebook.foobnix.mobi.parser

import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.util.Arrays

class MobiParser @Throws(IOException::class) constructor(private val raw: ByteBuffer) {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MobiParser::class.java)

        const val COMPRESSION_NONE = 0
        const val COMPRESSION_PalmDOC = 2
        const val COMPRESSION_HUFF = 17480

        @JvmStatic
        fun byteArrayToInt(buffer: ByteArray): Int {
            var total = 0
            for (b in buffer) {
                total = (total shl 8) + (b.toInt() and 0xff)
            }
            return total
        }

        @JvmStatic
        fun asString(raw: ByteBuffer, offset: Int, len: Int): String {
            return String(getBytes(raw, offset, len))
        }

        @JvmStatic
        fun asInt(raw: ByteBuffer, offset: Int, len: Int): Int {
            val range = getBytes(raw, offset, len)
            return byteArrayToInt(range)
        }

        @JvmStatic
        fun toInt(bytes: ByteArray, pos: Int): Int {
            if (bytes.size < 10) {
                return -1
            }
            return (bytes[bytes.size - pos].toInt() and 0xff)
        }

        @JvmStatic
        fun getBytes(buffer: ByteBuffer, offset: Int, length: Int): ByteArray {
            if (length <= 0 || offset < 0 || offset + length > buffer.capacity()) {
                return ByteArray(0)
            }
            val bytes = ByteArray(length)
            val originalPosition = buffer.position()
            buffer.position(offset)
            buffer.get(bytes)
            buffer.position(originalPosition)
            return bytes
        }

        @JvmStatic
        fun lz77(bytes: ByteArray): ByteArray {
            val outputStream = ByteArrayBuffer(bytes.size)
            var i = 0
            while (i < bytes.size - 4) {
                val b = bytes[i++].toInt() and 0x00FF
                try {
                    if (b == 0x0) {
                        outputStream.write(b)
                    } else if (b <= 0x08) {
                        for (j in 0 until b) {
                            outputStream.write(bytes[i + j].toInt())
                        }
                        i += b
                    } else if (b <= 0x7f) {
                        outputStream.write(b)
                    } else if (b <= 0xbf) {
                        val next = bytes[i++].toInt() and 0xFF
                        val bFull = (b shl 8) or next
                        val length = (bFull and 0x0007) + 3
                        val location = (bFull shr 3) and 0x7FF
                        for (j in 0 until length) {
                            outputStream.write(outputStream.rawData[outputStream.size() - location].toInt())
                        }
                    } else {
                        outputStream.write(' '.code)
                        outputStream.write(b xor 0x80)
                    }
                } catch (e: Exception) {
                    LOGGER.error("Error to export all: {}", e.message, e)
                }
            }
            return outputStream.rawData
        }

        @JvmStatic
        fun byteArrayToString(buffer: ByteArray, encoding: String?): String {
            val len = buffer.size
            var zeroIndex = -1
            for (i in 0 until len) {
                if (buffer[i].toInt() == 0) {
                    zeroIndex = i
                    break
                }
            }

            if (encoding != null) {
                try {
                    return if (zeroIndex == -1) {
                        String(buffer, charset(encoding))
                    } else {
                        String(buffer, 0, zeroIndex, charset(encoding))
                    }
                } catch (e: Exception) {
                    // fall through
                }
            }

            return if (zeroIndex == -1) {
                String(buffer)
            } else {
                String(buffer, 0, zeroIndex)
            }
        }
    }

    var isMobi: Boolean = false
        private set
    var name: String
    var recordsCount: Int
    private val recordsOffset = ArrayList<Int>()
    var mobiType: Int = 0
    var encoding: String
    var fullName: String
    var locale: String
    var firstImageIndex: Int = 0
    var lastContentIndex: Int = 0
    var exth = EXTH()
    private var firstContentIndex: Int = 0
    private var compression: Int = 0

    inner class EXTH {
        var identifier: String = ""
        var len: Int = 0
        var count: Int = 0
        val headers = HashMap<Int, ByteArray>()
        val headersMulti = HashMap<Int, MutableList<ByteArray>>()

        fun parse(raw: ByteBuffer, mobiOffset: Int, rec0Len: Int): EXTH {
            val maxSearchLen = kotlin.math.min(rec0Len, 100_000)
            val offset = indexOf(raw, "EXTH".toByteArray(), mobiOffset, maxSearchLen)
            if (offset < 0 || offset + 12 > raw.capacity()) {
                return this
            }
            identifier = asString(raw, offset, 4)
            len = asInt(raw, offset + 4, 4)
            count = asInt(raw, offset + 8, 4)

            if (count <= 0 || count > 300 || offset + len > raw.capacity()) {
                return this
            }

            var rOffset = offset + 12

            for (i in 0 until count) {
                if (rOffset + 8 > raw.capacity()) break
                val rType = asInt(raw, rOffset, 4)
                val rLen = asInt(raw, rOffset + 4, 4)
                if (rLen < 8 || rOffset + rLen > raw.capacity()) break
                val data = getBytes(raw, rOffset + 8, rLen - 8)
                rOffset += rLen
                headers[rType] = data
                headersMulti.computeIfAbsent(rType) { mutableListOf() }.add(data)
            }
            return this
        }
    }

    fun indexOf(input: ByteBuffer, search: ByteArray, startOffset: Int = 0, maxLen: Int = -1): Int {
        val start = kotlin.math.max(0, startOffset)
        val end = if (maxLen > 0) kotlin.math.min(input.limit(), start + maxLen) else input.limit()
        val limit = end - search.size + 1
        for (i in start until limit) {
            var found = true
            for (j in search.indices) {
                if (input.get(i + j) != search[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }

    init {
        name = asString(raw, 0, 32).trimEnd('\u0000', ' ')
        recordsCount = asInt(raw, 76, 2)

        if (recordsCount > 0) {
            var i = 78
            while (i < 78 + recordsCount * 8 && i + 4 <= raw.capacity()) {
                val recordOffset = byteArrayToInt(getBytes(raw, i, 4))
                recordsOffset.add(recordOffset)
                i += 8
            }
        }

        if (recordsOffset.isNotEmpty()) {
            val mobiOffset = recordsOffset[0]
            compression = asInt(raw, mobiOffset, 2)

            val rec0Len = if (recordsOffset.size > 1) recordsOffset[1] - mobiOffset else raw.capacity() - mobiOffset
            val magic = if (rec0Len >= 20 && mobiOffset + 20 <= raw.capacity()) asString(raw, mobiOffset + 16, 4) else ""
            isMobi = (magic == "MOBI" || magic == "BOOK")

            if (isMobi) {
                val encryption = asInt(raw, mobiOffset + 12, 2)

                mobiType = asInt(raw, mobiOffset + 24, 4)
                encoding = if (asInt(raw, mobiOffset + 28, 4) == 1252) "cp1251" else "UTF-8"

                val fullNameOffset = asInt(raw, mobiOffset + 84, 4)
                val fullNameLen = asInt(raw, mobiOffset + 88, 4)
                var nameTemp = if (fullNameLen in 1..4096 && mobiOffset + fullNameOffset + fullNameLen <= raw.capacity()) {
                    asString(raw, mobiOffset + fullNameOffset, fullNameLen)
                } else {
                    name
                }
                if (encryption != 0) {
                    nameTemp = "(DRM) $nameTemp"
                }
                fullName = nameTemp

                locale = asString(raw, mobiOffset + 92, 4)

                firstImageIndex = asInt(raw, mobiOffset + 108, 4)
                val isEXTHFlag = (asInt(raw, mobiOffset + 128, 4) and 0x40) != 0
                firstContentIndex = asInt(raw, mobiOffset + 192, 2)
                lastContentIndex = asInt(raw, mobiOffset + 194, 2)

                if (isEXTHFlag) {
                    exth = EXTH().parse(raw, mobiOffset, rec0Len)
                }
            } else {
                // Formato PalmDOC / PDB sem cabeçalho MOBI
                fullName = name
                encoding = "UTF-8"
                locale = ""
                firstContentIndex = 1
                lastContentIndex = recordsCount
                firstImageIndex = recordsCount
            }
        } else {
            fullName = name
            encoding = "UTF-8"
            locale = ""
        }
    }

    private val INDX = "INDX".toByteArray()

    fun getTextContent(): String {
        val outputStream = ByteArrayOutputStream()
        if (firstContentIndex == 0) {
            firstContentIndex = 1
        }
        var i = firstContentIndex
        while (i < lastContentIndex - 1 && i < firstImageIndex) {
            val start = recordsOffset[i]
            val end = recordsOffset[i + 1]
            val coded = getBytes(raw, start, end - start)

            val decoded: ByteArray? = if (compression == COMPRESSION_PalmDOC) {
                lz77(coded)
            } else if (compression == COMPRESSION_NONE) {
                coded
            } else if (compression == COMPRESSION_HUFF) {
                try {
                    coded
                } catch (e: Exception) {
                    LOGGER.error("Error to export all: " + e.message, e)
                    "error".toByteArray()
                }
            } else {
                ("Compression not supported $compression").toByteArray()
            }

            if (decoded != null && decoded.size >= 4) {
                val header = Arrays.copyOfRange(decoded, 0, 4)
                if (Arrays.equals(INDX, header)) {
                    i++
                    continue
                }
            }

            if (decoded != null) {
                for (n in decoded.indices) {
                    if (decoded[n].toInt() != 0x00) {
                        outputStream.write(decoded[n].toInt())
                    }
                }
            }
            i++
        }
        return try {
            outputStream.toString(encoding)
        } catch (e: UnsupportedEncodingException) {
            outputStream.toString()
        }
    }

    fun getSampleText(): String {
        return try {
            val sb = java.lang.StringBuilder()
            if (recordsOffset.size > 1) {
                // Record 1 (primeiro registro de texto com dados de capa/copyright inicial)
                val start1 = recordsOffset[1]
                val end1 = if (recordsOffset.size > 2) recordsOffset[2] else raw.capacity()
                val coded1 = getBytes(raw, start1, end1 - start1)
                val decoded1 = if (compression == COMPRESSION_PalmDOC) lz77(coded1) else coded1
                sb.append(String(decoded1, Charsets.UTF_8)).append("\n")

                // Último registro de texto (frequentemente contém colofão e editora final)
                val lastIdx = recordsOffset.size - 1
                if (lastIdx > 1) {
                    val startLast = recordsOffset[lastIdx]
                    val endLast = raw.capacity()
                    val codedLast = getBytes(raw, startLast, endLast - startLast)
                    val decodedLast = if (compression == COMPRESSION_PalmDOC) lz77(codedLast) else codedLast
                    sb.append(String(decodedLast, Charsets.UTF_8))
                }
            }
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }

    val title: String
        get() = fullName

    val author: String?
        get() {
            val list = exth.headersMulti[100]
            if (!list.isNullOrEmpty()) {
                val authors = list.map { String(it).trim() }.filter { it.isNotEmpty() }
                if (authors.isNotEmpty()) {
                    return authors.joinToString(", ")
                }
            }
            val bytes = exth.headers[100] ?: return null
            return String(bytes).trim()
        }

    val subject: String?
        get() {
            val list = exth.headersMulti[105]
            val raw = if (!list.isNullOrEmpty()) {
                list.map { String(it).trim() }.filter { it.isNotEmpty() }.joinToString(", ")
            } else {
                val bytes = exth.headers[105] ?: return null
                String(bytes).trim()
            }
            return raw.split(Regex("""[;,]""")).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ")
        }

    val language: String?
        get() {
            val bytes = exth.headers[524] ?: return null
            return String(bytes).trim()
        }

    val publisher: String?
        get() {
            val bytes = exth.headers[101] ?: return null
            return String(bytes).trim()
        }

    val isbn: String?
        get() {
            val bytes = exth.headers[104] ?: return null
            return String(bytes).trim()
        }

    val release: String?
        get() {
            val bytes = exth.headers[106] ?: return null
            return String(bytes).trim()
        }

    val description: String?
        get() {
            val bytes = exth.headers[103] ?: return null
            return String(bytes).trim()
        }

    val headers: Map<Int, ByteArray>
        get() = exth.headers

    fun getCoverOrThumb(): ByteArray? {
        if (!isMobi) {
            return null
        }
        var imgNumber = exth.headers[201]
        if (imgNumber == null) {
            imgNumber = exth.headers[202]
        }
        if (imgNumber != null) {
            val coverIndex = firstImageIndex + byteArrayToInt(imgNumber)
            if (coverIndex in recordsOffset.indices) {
                val start = recordsOffset[coverIndex]
                val end = if (coverIndex + 1 < recordsOffset.size) recordsOffset[coverIndex + 1] else raw.capacity()
                return getBytes(raw, start, end - start)
            }
        }
        return null
    }

}
