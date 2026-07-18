package br.com.fenix.bilingualreader.service.parses.manga

import br.com.fenix.bilingualreader.model.enums.FileType
import com.github.junrar.exception.RarException
import com.github.junrar.exception.UnsupportedRarV5Exception
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.Locale

class ParseFactory {

    companion object Factory {
        private val mLOGGER = LoggerFactory.getLogger(ParseFactory::class.java)
        fun create(file: String): Parse? {
            return create(File(file))
        }

        fun create(file: File): Parse? {
            var parser: Parse? = null
            val fileName = file.absolutePath.lowercase(Locale.getDefault())
            if (file.isDirectory) {
                parser = DirectoryParse()
            } else {
                when {
                    FileType.CBZ.`is`(fileName) || FileType.ZIP.`is`(fileName) -> parser = ZipParse()
                    FileType.CBR.`is`(fileName) || FileType.RAR.`is`(fileName) -> parser = RarParse()
                    FileType.CBT.`is`(fileName) || FileType.TAR.`is`(fileName) -> parser = TarParse()
                    FileType.CB7.`is`(fileName) || FileType.SEVENZ.`is`(fileName) -> parser = SevenZipParse()
                    FileType.EPUB.`is`(fileName) || FileType.EPUB3.`is`(fileName) -> parser = EpubParse()
                }
            }

            return tryParse(parser, file)
        }

        private fun tryParse(parser: Parse?, file: File): Parse? {
            if (parser == null)
                return null

            var parsed = tryParseInternal(parser, file)
            if (parsed == null) {
                // Try fallbacks for potential misnamed extensions (e.g. .zip renamed to .cbr)
                if (parser !is ZipParse) {
                    parsed = tryParseInternal(ZipParse(), file)
                }
                if (parsed == null && parser !is RarParse) {
                    parsed = tryParseInternal(RarParse(), file)
                }
                if (parsed == null && parser !is SevenZipParse) {
                    parsed = tryParseInternal(SevenZipParse(), file)
                }
                if (parsed == null && parser !is TarParse) {
                    parsed = tryParseInternal(TarParse(), file)
                }
            }

            return parsed
        }

        private fun tryParseInternal(parser: Parse, file: File): Parse? {
            try {
                parser.parse(file)
                return if (parser is DirectoryParse && parser.numPages() < 4) {
                    parser.destroy(false)
                    null
                } else {
                    parser
                }
            } catch (e: UnsupportedRarV5Exception) {
                mLOGGER.warn("UnsupportedRarV5Exception: Error when parse: " + e.message + " - File: " + file.name)
            } catch (e: RarException) {
                if (e is com.github.junrar.exception.CorruptHeaderException) {
                    // Log only as warn message without passing the exception 'e' to avoid sending to Sentry
                    mLOGGER.warn("CorruptHeaderException (possibly renamed file): " + e.message + " - File: " + file.name)
                } else {
                    mLOGGER.warn("Error when parse: " + e.message + " - File: " + file.name, e)
                }
            } catch (e: IllegalArgumentException) {
                val cause = e.cause?.message ?: ""
                mLOGGER.warn("java.lang.IllegalArgumentException:" + cause + " Error when parse: " + e.message + " - File: " + file.name)
            } catch (e: IOException) {
                mLOGGER.warn("Error when parse: " + e.message + " - File: " + file.name, e)
            } catch (e: Exception) {
                mLOGGER.warn("Error when parse: " + e.message + " - File: " + file.name, e)
            }
            try {
                parser.destroy(false)
            } catch (ignored: Exception) {}
            return null
        }
    }
}