package br.com.ebook.extractor

import android.text.TextUtils
import br.com.ebook.core.BookContent
import br.com.ebook.core.BookExtractor
import br.com.ebook.core.BookMetadata
import br.com.ebook.core.EbookSettings
import br.com.ebook.foobnix.hypen.HypenUtils
import com.rtfparserkit.converter.text.StringTextConverter
import com.rtfparserkit.parser.RtfListenerAdaptor
import com.rtfparserkit.parser.RtfStreamSource
import com.rtfparserkit.parser.standard.StandardRtfParser
import com.rtfparserkit.rtf.Command
import com.rtfparserkit.utils.HexUtils
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.PrintWriter

object RtfBookExtractor : BookExtractor {
    private val LOGGER = LoggerFactory.getLogger(RtfBookExtractor::class.java)

    override val supportedFormats: Set<String> = setOf("rtf")

    override suspend fun extractMetadata(path: String): Result<BookMetadata> = runCatching {
        val file = File(path)
        BookMetadata(
            title = file.nameWithoutExtension,
            author = "",
            unzipPath = path
        )
    }

    override suspend fun extractCover(path: String): Result<ByteArray?> = runCatching {
        var coverBytes: ByteArray? = null
        try {
            FileInputStream(path).use { fis ->
                val source = RtfStreamSource(fis)
                val parser = StandardRtfParser()
                parser.parse(source, object : RtfListenerAdaptor() {
                    private var pict = false

                    override fun processString(string: String?) {
                        if (coverBytes == null && pict && string != null) {
                            coverBytes = HexUtils.parseHexString(string)
                        }
                    }

                    override fun processCommand(command: Command?, parameter: Int, hasParameter: Boolean, optional: Boolean) {
                        if (command == Command.pngblip || command == Command.jpegblip) {
                            pict = true
                        }
                    }
                })
            }
        } catch (e: Exception) {
            LOGGER.error("Error get image cover from RTF: {}", e.message, e)
        }
        coverBytes
    }

    override suspend fun extractOverview(path: String): Result<String> = runCatching {
        ""
    }

    override suspend fun extractFooterNotes(path: String): Result<Map<String, String>> = runCatching {
        emptyMap()
    }

    override suspend fun extractContent(path: String, outputDir: String): Result<BookContent> = runCatching {
        val hashCode = path.hashCode().toString()
        val fileName = "rtf_temp_$hashCode.html"
        val file = File(outputDir, fileName)

        try {
            PrintWriter(BufferedWriter(FileWriter(file))).use { writer ->
                writer.println("<!DOCTYPE html>")
                writer.println("<html>")
                writer.println("<body>")

                if (EbookSettings.isAutoHypens) {
                    HypenUtils.applyLanguage(EbookSettings.hypenLang)
                }

                FileInputStream(path).use { fis ->
                    val source = RtfStreamSource(fis)
                    val parser = StandardRtfParser()

                    parser.parse(source, object : StringTextConverter() {
                        private var isImage = false
                        private var isBR = false
                        private var format = "jpg"
                        private var counter = 0

                        override fun processExtractedText(text: String?) {
                            if (text == null) return
                            var htmlEncode = TextUtils.htmlEncode(text)
                            if (EbookSettings.isAutoHypens) {
                                htmlEncode = HypenUtils.applyHypnes(htmlEncode)
                            }
                            writer.println(htmlEncode)
                            isBR = false
                        }

                        override fun processString(string: String?) {
                            super.processString(string)
                            if (isImage && string != null) {
                                try {
                                    isImage = false
                                    val imageName = "${fileName}_${counter++}.rtf.$format"
                                    val imageFile = File(outputDir, imageName)
                                    FileOutputStream(imageFile).use { fos ->
                                        fos.write(HexUtils.parseHexString(string))
                                        fos.flush()
                                    }
                                    writer.write("<img src='$imageName' />")
                                } catch (e: Exception) {
                                    LOGGER.error("Error to process image string in RTF: {}", e.message, e)
                                }
                            }
                        }

                        override fun processCommand(command: Command?, parameter: Int, hasParameter: Boolean, optional: Boolean) {
                            super.processCommand(command, parameter, hasParameter, optional)
                            if (command == Command.cbpat || command == Command.par || command == Command.line) {
                                if (!isBR) {
                                    writer.write("<br/>")
                                    isBR = true
                                }
                            }
                            if (command == Command.pngblip) {
                                isImage = true
                                format = "png"
                            }
                            if (command == Command.jpegblip) {
                                isImage = true
                                format = "jpg"
                            }
                        }
                    })
                }

                writer.println("</body></html>")
            }
        } catch (e: Exception) {
            LOGGER.error("Error to extract RTF: {}", e.message, e)
            throw e
        }

        BookContent.HtmlFile(file.path, null)
    }
}
