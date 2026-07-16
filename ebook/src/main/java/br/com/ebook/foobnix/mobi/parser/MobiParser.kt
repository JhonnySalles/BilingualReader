package br.com.ebook.foobnix.mobi.parser

import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.util.Arrays
import br.com.ebook.Config

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

        fun parse(raw: ByteBuffer): EXTH {
            val offset = indexOf(raw, "EXTH".toByteArray())
            identifier = asString(raw, offset, 4)
            len = asInt(raw, offset + 4, 4)
            count = asInt(raw, offset + 8, 4)

            var rOffset = offset + 12

            for (i in 0 until count) {
                val rType = asInt(raw, rOffset, 4)
                val rLen = asInt(raw, rOffset + 4, 4)
                val data = getBytes(raw, rOffset + 8, rLen - 8)
                rOffset += rLen
                headers[rType] = data
            }
            return this
        }
    }

    fun indexOf(input: ByteBuffer, search: ByteArray): Int {
        val limit = input.limit() - search.size + 1
        for (i in 0 until limit) {
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
        name = asString(raw, 0, 32)
        recordsCount = asInt(raw, 76, 2)

        var i = 78
        while (i < 78 + recordsCount * 8) {
            val recordOffset = byteArrayToInt(getBytes(raw, i, 4))
            recordsOffset.add(recordOffset)
            i += 8
        }
        val mobiOffset = recordsOffset[0]

        compression = asInt(raw, mobiOffset, 2)
        val encryption = asInt(raw, mobiOffset + 12, 2)
        if (Config.SHOW_LOG) {
            LOGGER.info("MobiParser -- encryption: {}", encryption)
        }

        mobiType = asInt(raw, mobiOffset + 24, 4)
        encoding = if (asInt(raw, mobiOffset + 28, 4) == 1252) "cp1251" else "UTF-8"

        val fullNameOffset = asInt(raw, mobiOffset + 84, 4)
        val fullNameLen = asInt(raw, mobiOffset + 88, 4)
        var nameTemp = asString(raw, mobiOffset + fullNameOffset, fullNameLen)
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
            exth = EXTH().parse(raw)
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

            var decoded: ByteArray? = null
            if (compression == COMPRESSION_PalmDOC) {
                decoded = lz77(coded)
            } else if (compression == COMPRESSION_NONE) {
                decoded = coded
            } else if (compression == COMPRESSION_HUFF) {
                try {
                    decoded = coded
                } catch (e: Exception) {
                    LOGGER.error("Error to export all: " + e.message, e)
                    decoded = "error".toByteArray()
                }
            } else {
                decoded = ("Compression not supported $compression").toByteArray()
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

    val title: String
        get() = fullName

    val author: String?
        get() {
            val bytes = exth.headers[100] ?: return null
            return String(bytes)
        }

    val subject: String?
        get() {
            val bytes = exth.headers[105] ?: return null
            return String(bytes)
        }

    val language: String?
        get() {
            val bytes = exth.headers[524] ?: return null
            return String(bytes)
        }

    val publisher: String?
        get() {
            val bytes = exth.headers[101] ?: return null
            return String(bytes)
        }

    val isbn: String?
        get() {
            val bytes = exth.headers[104] ?: return null
            return String(bytes)
        }

    val release: String?
        get() {
            val bytes = exth.headers[106] ?: return null
            return String(bytes)
        }

    val description: String?
        get() {
            val bytes = exth.headers[103] ?: return null
            return String(bytes)
        }

    val headers: Map<Int, ByteArray>
        get() = exth.headers

    fun getCoverOrThumb(): ByteArray? {
        var imgNumber = exth.headers[201]
        if (imgNumber == null) {
            imgNumber = exth.headers[202]
        }

        if (imgNumber != null) {
            val index = byteArrayToInt(imgNumber)
            return getRecordByIndex(index + firstImageIndex)
        } else {
            var i = firstImageIndex
            while (i < lastContentIndex) {
                val img = getRecordByIndex(i)
                if (img != null && img.size >= 2) {
                    if ((img[0].toInt() and 0xff) == 0xFF && (img[1].toInt() and 0xff) == 0xD8) {
                        return img
                    }
                }
                i++
            }
        }
        return null
    }

    fun getRecordByIndex(index: Int): ByteArray? {
        if (index >= recordsOffset.size) {
            return null
        }

        val from = recordsOffset[index]
        var to = raw.limit()
        if (index + 1 < recordsOffset.size) {
            to = recordsOffset[index + 1]
        }

        return getBytes(raw, from, to - from)
    }
}
