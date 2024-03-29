/*******************************************************************************
 * Copyright 2011-2013 Sergey Tarasevich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.com.ebook.universalimageloader.cache.disc.naming

import br.com.ebook.universalimageloader.utils.L.e
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Names image file as MD5 hash of image URI
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.4.0
 */
class Md5FileNameGenerator : FileNameGenerator {
    override fun generate(imageUri: String?): String? {
        val md5 = getMD5(imageUri!!.toByteArray())
        val bi = BigInteger(md5).abs()
        return bi.toString(RADIX)
    }

    private fun getMD5(data: ByteArray): ByteArray? {
        var hash: ByteArray? = null
        try {
            val digest = MessageDigest.getInstance(HASH_ALGORITHM)
            digest.update(data)
            hash = digest.digest()
        } catch (e: NoSuchAlgorithmException) {
            e(e)
        }
        return hash
    }

    companion object {
        private const val HASH_ALGORITHM = "MD5"
        private const val RADIX = 10 + 26 // 10 digits + 26 letters
    }
}