package br.com.ebook.util

object ImageSizeUtils {

    enum class ImageRole {
        FULL_PAGE,
        INLINE
    }

    const val DEFAULT_SCREEN_WIDTH = 1080
    const val DEFAULT_SCREEN_HEIGHT = 1920
    const val DEFAULT_MIN_SIDE = 64

    /**
     * Reads the dimensions (width, height) of PNG, JPEG, GIF, or WEBP image bytes.
     * Pure Kotlin implementation with no Android SDK dependencies, suitable for JVM unit tests.
     */
    fun readDimensions(bytes: ByteArray?): Pair<Int, Int>? {
        if (bytes == null || bytes.size < 8) return null

        // 1. PNG: 89 50 4E 47 0D 0A 1A 0A
        if (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() &&
            bytes[4] == 0x0D.toByte() && bytes[5] == 0x0A.toByte() &&
            bytes[6] == 0x1A.toByte() && bytes[7] == 0x0A.toByte()
        ) {
            if (bytes.size >= 24) {
                val w = ((bytes[16].toInt() and 0xFF) shl 24) or
                        ((bytes[17].toInt() and 0xFF) shl 16) or
                        ((bytes[18].toInt() and 0xFF) shl 8) or
                        (bytes[19].toInt() and 0xFF)
                val h = ((bytes[20].toInt() and 0xFF) shl 24) or
                        ((bytes[21].toInt() and 0xFF) shl 16) or
                        ((bytes[22].toInt() and 0xFF) shl 8) or
                        (bytes[23].toInt() and 0xFF)
                if (w > 0 && h > 0) return Pair(w, h)
            }
            return null
        }

        // 2. GIF: GIF87a or GIF89a
        if (bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte()) {
            if (bytes.size >= 10) {
                val w = (bytes[6].toInt() and 0xFF) or ((bytes[7].toInt() and 0xFF) shl 8)
                val h = (bytes[8].toInt() and 0xFF) or ((bytes[9].toInt() and 0xFF) shl 8)
                if (w > 0 && h > 0) return Pair(w, h)
            }
            return null
        }

        // 3. WEBP: RIFF....WEBP
        if (bytes.size >= 16 &&
            bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        ) {
            val chunk = String(bytes, 12, 4, Charsets.US_ASCII)
            when (chunk) {
                "VP8 " -> {
                    if (bytes.size >= 30) {
                        val w = ((bytes[26].toInt() and 0xFF) or ((bytes[27].toInt() and 0xFF) shl 8)) and 0x3FFF
                        val h = ((bytes[28].toInt() and 0xFF) or ((bytes[29].toInt() and 0xFF) shl 8)) and 0x3FFF
                        if (w > 0 && h > 0) return Pair(w, h)
                    }
                }
                "VP8L" -> {
                    if (bytes.size >= 25 && bytes[20] == 0x2F.toByte()) {
                        val b21 = bytes[21].toInt() and 0xFF
                        val b22 = bytes[22].toInt() and 0xFF
                        val b23 = bytes[23].toInt() and 0xFF
                        val b24 = bytes[24].toInt() and 0xFF
                        val w = 1 + (b21 or ((b22 and 0x3F) shl 8))
                        val h = 1 + (((b22 shr 6) or (b23 shl 2) or ((b24 and 0x0F) shl 10)))
                        if (w > 0 && h > 0) return Pair(w, h)
                    }
                }
                "VP8X" -> {
                    if (bytes.size >= 30) {
                        val w = 1 + ((bytes[24].toInt() and 0xFF) or
                                ((bytes[25].toInt() and 0xFF) shl 8) or
                                ((bytes[26].toInt() and 0xFF) shl 16))
                        val h = 1 + ((bytes[27].toInt() and 0xFF) or
                                ((bytes[28].toInt() and 0xFF) shl 8) or
                                ((bytes[29].toInt() and 0xFF) shl 16))
                        if (w > 0 && h > 0) return Pair(w, h)
                    }
                }
            }
            return null
        }

        // 4. JPEG: FF D8
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) {
            var offset = 2
            while (offset < bytes.size - 8) {
                if ((bytes[offset].toInt() and 0xFF) != 0xFF) {
                    offset++
                    continue
                }
                while (offset < bytes.size && (bytes[offset].toInt() and 0xFF) == 0xFF) {
                    offset++
                }
                if (offset >= bytes.size) break
                val marker = bytes[offset].toInt() and 0xFF
                offset++

                // SOF markers: Baseline (C0), Extended Sequential (C1), Progressive (C2), Lossless (C3),
                // Differential (C5-C7), Extended (C9-CB), etc. Excluding DHT (C4) and DAC (CC).
                if (marker in listOf(0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF)) {
                    if (offset + 7 <= bytes.size) {
                        // Length: 2 bytes at offset, Precision: 1 byte at offset+2
                        // Height: 2 bytes at offset+3, Width: 2 bytes at offset+5
                        val h = ((bytes[offset + 3].toInt() and 0xFF) shl 8) or (bytes[offset + 4].toInt() and 0xFF)
                        val w = ((bytes[offset + 5].toInt() and 0xFF) shl 8) or (bytes[offset + 6].toInt() and 0xFF)
                        if (w > 0 && h > 0) return Pair(w, h)
                    }
                    break
                } else if (marker == 0xDA || marker == 0xD9) {
                    // SOS (Start of Scan) or EOI (End of Image)
                    break
                } else if (marker !in 0xD0..0xD7 && marker != 0x01) {
                    // Marker with length payload
                    if (offset + 2 <= bytes.size) {
                        val length = ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
                        offset += length
                    } else {
                        break
                    }
                }
            }
            return null
        }

        return null
    }

    /**
     * Classifies an image into FULL_PAGE or INLINE based on its dimensions and screen size.
     * Rule: FULL_PAGE if (w > screenW / 3 || h > screenH / 3) && min(w, h) >= minSide.
     * Otherwise INLINE.
     */
    fun classify(
        dims: Pair<Int, Int>?,
        screenW: Int = DEFAULT_SCREEN_WIDTH,
        screenH: Int = DEFAULT_SCREEN_HEIGHT,
        minSide: Int = DEFAULT_MIN_SIDE
    ): ImageRole {
        if (dims == null) return ImageRole.INLINE
        val (w, h) = dims
        val minDim = Math.min(w, h)
        val isLargeEnough = (w > screenW / 3 || h > screenH / 3)
        val hasSufficientThickness = minDim >= minSide

        return if (isLargeEnough && hasSufficientThickness) {
            ImageRole.FULL_PAGE
        } else {
            ImageRole.INLINE
        }
    }

    fun classify(
        bytes: ByteArray?,
        screenW: Int = DEFAULT_SCREEN_WIDTH,
        screenH: Int = DEFAULT_SCREEN_HEIGHT,
        minSide: Int = DEFAULT_MIN_SIDE
    ): ImageRole {
        if (bytes == null) return ImageRole.INLINE
        val dims = readDimensions(bytes)
        return classify(dims, screenW, screenH, minSide)
    }
}
