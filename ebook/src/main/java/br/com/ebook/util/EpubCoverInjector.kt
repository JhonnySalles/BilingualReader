package br.com.ebook.util

import br.com.ebook.util.IOUtils.copyTo
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object EpubCoverInjector {
    private val LOGGER = LoggerFactory.getLogger(EpubCoverInjector::class.java)

    private const val COVER_IMAGE_NAME = "bilingual-cover-image.jpg"
    private const val COVER_PAGE_NAME = "bilingual-cover-page.xhtml"

    fun injectCover(epubPath: String, coverBytes: ByteArray?) {
        if (coverBytes == null || coverBytes.isEmpty()) {
            LOGGER.info("No cover bytes to inject for: {}", epubPath)
            return
        }

        val epubFile = File(epubPath)
        if (!epubFile.exists()) {
            LOGGER.error("EPUB file does not exist: {}", epubPath)
            return
        }

        val tempFile = File(epubFile.parent, epubFile.name + ".tmp")
        try {
            ZipInputStream(BufferedInputStream(FileInputStream(epubFile))).use { zis ->
                ZipOutputStream(BufferedOutputStream(FileOutputStream(tempFile))).use { zos ->
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        zos.setLevel(0)
                    }
                    var entry: ZipEntry?
                    var opfPath = ""
                    var opfContent = ""

                    // Pass 1: Copy and find OPF
                    while (zis.nextEntry.also { entry = it } != null) {
                        val name = entry!!.name
                        if (name.endsWith(".opf", ignoreCase = true)) {
                            opfPath = name
                            opfContent = String(zis.readBytes(), Charsets.UTF_8)
                        } else {
                            zos.putNextEntry(ZipEntry(name))
                            zis.copyTo(zos)
                            zos.closeEntry()
                        }
                    }

                    if (opfPath.isNotEmpty() && opfContent.isNotEmpty()) {
                        // Check if already injected
                        if (!opfContent.contains("bilingual-cover-page")) {
                            val parentDir = if (opfPath.contains("/")) opfPath.substring(0, opfPath.lastIndexOf("/") + 1) else ""
                            
                            // 1. Inject cover.jpg
                            zos.putNextEntry(ZipEntry(parentDir + COVER_IMAGE_NAME))
                            zos.write(coverBytes)
                            zos.closeEntry()

                            // 2. Inject cover.xhtml
                            val xhtml = """
                                <?xml version="1.0" encoding="utf-8"?>
                                <!DOCTYPE html>
                                <html xmlns="http://www.w3.org/1999/xhtml">
                                <head>
                                  <title>Cover</title>
                                  <style type="text/css">
                                    body { margin: 0; padding: 0; text-align: center; background-color: #ffffff; }
                                    img { max-width: 100%; max-height: 100%; height: auto; width: auto; margin: 0 auto; display: block; }
                                  </style>
                                </head>
                                <body>
                                  <div>
                                    <img src="$COVER_IMAGE_NAME" alt="Cover" />
                                  </div>
                                </body>
                                </html>
                            """.trimIndent()
                            zos.putNextEntry(ZipEntry(parentDir + COVER_PAGE_NAME))
                            zos.write(xhtml.toByteArray(Charsets.UTF_8))
                            zos.closeEntry()

                            // 3. Modify OPF
                            var modifiedOpf = opfContent
                            val manifestTag = "<manifest>"
                            val manifestIdx = modifiedOpf.indexOf(manifestTag)
                            if (manifestIdx != -1) {
                                val insertPos = manifestIdx + manifestTag.length
                                val items = "\n    <item id=\"bilingual-cover-image\" href=\"$COVER_IMAGE_NAME\" media-type=\"image/jpeg\" />" +
                                            "\n    <item id=\"bilingual-cover-page\" href=\"$COVER_PAGE_NAME\" media-type=\"application/xhtml+xml\" />"
                                modifiedOpf = modifiedOpf.substring(0, insertPos) + items + modifiedOpf.substring(insertPos)
                            }

                            val spineRegex = Regex("<spine[^>]*>")
                            val match = spineRegex.find(modifiedOpf)
                            if (match != null) {
                                val insertPos = match.range.last + 1
                                val itemref = "\n    <itemref idref=\"bilingual-cover-page\" />"
                                modifiedOpf = modifiedOpf.substring(0, insertPos) + itemref + modifiedOpf.substring(insertPos)
                            }

                            // Write modified OPF
                            zos.putNextEntry(ZipEntry(opfPath))
                            zos.write(modifiedOpf.toByteArray(Charsets.UTF_8))
                            zos.closeEntry()
                            LOGGER.info("Successfully injected cover into: {}", epubPath)
                        } else {
                            // Write original OPF if already injected
                            zos.putNextEntry(ZipEntry(opfPath))
                            zos.write(opfContent.toByteArray(Charsets.UTF_8))
                            zos.closeEntry()
                            LOGGER.info("Cover already present in: {}", epubPath)
                        }
                    } else {
                        LOGGER.error("OPF file not found in EPUB: {}", epubPath)
                    }
                }
            }

            if (tempFile.exists()) {
                if (epubFile.delete()) {
                    if (!tempFile.renameTo(epubFile)) {
                        throw IOException("Could not rename temp file to ${epubFile.path}")
                    }
                } else {
                    throw IOException("Could not delete original file ${epubFile.path}")
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Error while injecting cover: {}", e.message, e)
            IOUtils.reportException(e, "Error while injecting cover: $epubPath")
            if (tempFile.exists()) tempFile.delete()
        }
    }
}
