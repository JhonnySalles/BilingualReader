package br.com.ebook.util

import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object EpubPackager {

    data class GalleryItem(
        val imageFileName: String, // e.g. "images/Art_insert2.jpeg" or "media/Art_insert2.jpeg"
        val title: String = "Illustration"
    )

    data class ChapterItem(
        val title: String,
        val contentHtml: String,
        val fileName: String = "chapter.xhtml",
        val anchorId: String? = null,
        val cssLinks: List<String> = emptyList()
    )

    fun createEpub(
        outputFile: File,
        title: String,
        author: String = "",
        coverBytes: ByteArray? = null,
        coverFileName: String = "cover.jpg",
        galleryItems: List<GalleryItem> = emptyList(),
        chapters: List<ChapterItem>,
        resourceFiles: Map<String, ByteArray> = emptyMap(), // Relative path inside OEBPS/ (e.g. "media/cover.jpeg" or "cover.jpg") -> bytes
        cssStyles: String = """
body { margin: 0; padding: 0; }
p { margin: 1em 0; line-height: 1.5; }
h1, h2, h3 { text-align: center; margin: 1.2em 0; }
img { max-width: 100%; height: auto; }
.br-full-page { text-align: center; margin: 0; padding: 0; page-break-inside: avoid; }
.br-full-page img { max-width: 100%; max-height: 100%; width: auto; height: auto; display: block; margin: 0 auto; }
.br-inline { display: inline; vertical-align: middle; }
.br-gap { margin: 1.5em 0; }
"""
    ) {
        outputFile.parentFile?.mkdirs()
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val bookId = "urn:uuid:" + UUID.nameUUIDFromBytes(outputFile.name.toByteArray())

        val writtenPaths = mutableSetOf<String>()

        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zos ->
            // 1. mimetype (MUST be first, uncompressed)
            writeMimetype(zos)
            writtenPaths.add("mimetype")

            // 2. META-INF/container.xml
            writeEntry(zos, "META-INF/container.xml", Fb2Templates.container_xml.toByteArray(Charsets.UTF_8), writtenPaths)

            // 3. OEBPS/reader-base.css
            writeEntry(zos, "OEBPS/reader-base.css", cssStyles.toByteArray(Charsets.UTF_8), writtenPaths)

            val manifest = StringBuilder()
            val spine = StringBuilder()
            val navMap = StringBuilder()
            var playOrder = 1

            manifest.append("    <item id=\"reader-base-css\" href=\"reader-base.css\" media-type=\"text/css\" />\n")

            // Check if style.css exists in resourceFiles; if so write it, otherwise provide a fallback style.css
            val hasCustomStyleCss = resourceFiles.keys.any { it.trimStart('/').equals("style.css", ignoreCase = true) }
            if (!hasCustomStyleCss) {
                writeEntry(zos, "OEBPS/style.css", cssStyles.toByteArray(Charsets.UTF_8), writtenPaths)
                manifest.append("    <item id=\"style\" href=\"style.css\" media-type=\"text/css\" />\n")
            }

            // 4. Cover (Spine Item 1)
            val hasCover = coverBytes != null && coverBytes.isNotEmpty()
            if (hasCover) {
                val coverMime = getMimeType(coverFileName)
                writeEntry(zos, "OEBPS/$coverFileName", coverBytes!!, writtenPaths)
                manifest.append("    <item id=\"cover-image\" href=\"$coverFileName\" media-type=\"$coverMime\" />\n")

                val coverXhtml = """<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>Cover</title>
  <link rel="stylesheet" href="reader-base.css" type="text/css" />
  <style type="text/css">
    body { margin: 0; padding: 0; text-align: center; }
    img { max-width: 100%; max-height: 100%; width: auto; height: auto; display: block; margin: 0 auto; }
  </style>
</head>
<body>
  <div style="text-align: center; margin: 0; padding: 0;">
    <img src="$coverFileName" alt="Cover" />
  </div>
</body>
</html>"""
                writeEntry(zos, "OEBPS/cover.xhtml", coverXhtml.toByteArray(Charsets.UTF_8), writtenPaths)
                manifest.append("    <item id=\"cover-page\" href=\"cover.xhtml\" media-type=\"application/xhtml+xml\" />\n")
                spine.append("    <itemref idref=\"cover-page\" />\n")
            }

            // 5. Resources (images, fonts, custom style.css, etc.)
            for ((resPath, resBytes) in resourceFiles) {
                val cleanResPath = resPath.trimStart('/')
                // If the cover was already written to OEBPS/$coverFileName, don't duplicate
                if (hasCover && cleanResPath == coverFileName) continue
                if (writtenPaths.contains("OEBPS/$cleanResPath")) continue

                val mime = getMimeType(cleanResPath)
                val resId = "res_" + cleanResPath.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                writeEntry(zos, "OEBPS/$cleanResPath", resBytes, writtenPaths)
                manifest.append("    <item id=\"$resId\" href=\"$cleanResPath\" media-type=\"$mime\" />\n")
            }

            // 6. Chapters / Content (Main Spine Items)
            for ((index, chap) in chapters.withIndex()) {
                val chapId = "chap_$index"
                val chapFile = if (chap.fileName.isNotBlank()) chap.fileName else "chapter_$index.xhtml"
                val extraCssLinks = chap.cssLinks.joinToString("\n") {
                    "  <link rel=\"stylesheet\" href=\"$it\" type=\"text/css\" />"
                }

                val xhtml = if (chap.contentHtml.contains("<?xml") || chap.contentHtml.contains("<html")) {
                    chap.contentHtml
                } else {
                    """<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>${escapeXml(chap.title)}</title>
  <link rel="stylesheet" href="reader-base.css" type="text/css" />
$extraCssLinks
</head>
<body>
${chap.contentHtml}
</body>
</html>"""
                }

                writeEntry(zos, "OEBPS/$chapFile", xhtml.toByteArray(Charsets.UTF_8), writtenPaths)
                manifest.append("    <item id=\"$chapId\" href=\"$chapFile\" media-type=\"application/xhtml+xml\" />\n")
                spine.append("    <itemref idref=\"$chapId\" />\n")

                if (chap.title.isNotBlank()) {
                    val anchorSuffix = if (!chap.anchorId.isNullOrEmpty()) "#${chap.anchorId}" else ""
                    navMap.append("""    <navPoint id="np_$playOrder" playOrder="$playOrder">
      <navLabel><text>${escapeXml(chap.title)}</text></navLabel>
      <content src="$chapFile$anchorSuffix" />
    </navPoint>
""")
                    playOrder++
                }
            }

            // 7. Gallery Images (End of Book Spine Pages, e.g. for TXTZ or leftover standalone images)
            for ((index, item) in galleryItems.withIndex()) {
                val pageId = "gallery_page_$index"
                val pageFile = "gallery_$index.xhtml"
                val cleanImgSrc = item.imageFileName.trimStart('/')
                val galleryXhtml = """<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>${escapeXml(item.title)}</title>
  <link rel="stylesheet" href="reader-base.css" type="text/css" />
  <style type="text/css">
    body { margin: 0; padding: 0; text-align: center; }
    img { max-width: 100%; max-height: 100%; width: auto; height: auto; display: block; margin: 0 auto; }
  </style>
</head>
<body>
  <div class="br-full-page">
    <img src="$cleanImgSrc" alt="${escapeXml(item.title)}" />
  </div>
</body>
</html>"""
                writeEntry(zos, "OEBPS/$pageFile", galleryXhtml.toByteArray(Charsets.UTF_8), writtenPaths)
                manifest.append("    <item id=\"$pageId\" href=\"$pageFile\" media-type=\"application/xhtml+xml\" />\n")
                spine.append("    <itemref idref=\"$pageId\" />\n")

                val galleryTitle = item.title.ifBlank { "Galeria ${index + 1}" }
                navMap.append("""    <navPoint id="np_$playOrder" playOrder="$playOrder">
      <navLabel><text>${escapeXml(galleryTitle)}</text></navLabel>
      <content src="$pageFile" />
    </navPoint>
""")
                playOrder++
            }

            // 8. OEBPS/toc.ncx
            val ncx = """<?xml version="1.0" encoding="UTF-8"?>
<ncx version="2005-1" xmlns="http://www.daisy.org/z3986/2005/ncx/">
  <head>
    <meta name="dtb:uid" content="$bookId" />
    <meta name="dtb:depth" content="1" />
    <meta name="dtb:totalPageCount" content="0" />
    <meta name="dtb:maxPageNumber" content="0" />
  </head>
  <docTitle><text>${escapeXml(title)}</text></docTitle>
  <navMap>
$navMap  </navMap>
</ncx>"""
            writeEntry(zos, "OEBPS/toc.ncx", ncx.toByteArray(Charsets.UTF_8), writtenPaths)

            // 9. OEBPS/content.opf
            val coverMeta = if (hasCover) "    <meta name=\"cover\" content=\"cover-image\" />\n" else ""
            val opf = """<?xml version="1.0" encoding="UTF-8"?>
<package version="2.0" unique-identifier="BookId" xmlns="http://www.idpf.org/2007/opf">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
    <dc:identifier id="BookId">$bookId</dc:identifier>
    <dc:title>${escapeXml(title)}</dc:title>
    <dc:creator>${escapeXml(author)}</dc:creator>
    <dc:language>pt</dc:language>
$coverMeta  </metadata>
  <manifest>
    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml" />
$manifest  </manifest>
  <spine toc="ncx">
$spine  </spine>
</package>"""
            writeEntry(zos, "OEBPS/content.opf", opf.toByteArray(Charsets.UTF_8), writtenPaths)
        }
    }

    private fun writeMimetype(zos: ZipOutputStream) {
        val bytes = "application/epub+zip".toByteArray(Charsets.US_ASCII)
        val entry = ZipEntry("mimetype").apply {
            method = ZipEntry.STORED
            size = bytes.size.toLong()
            compressedSize = bytes.size.toLong()
            crc = CRC32().apply { update(bytes) }.value
        }
        zos.putNextEntry(entry)
        zos.write(bytes)
        zos.closeEntry()
    }

    private fun writeEntry(zos: ZipOutputStream, path: String, bytes: ByteArray, writtenPaths: MutableSet<String>? = null) {
        if (writtenPaths != null && !writtenPaths.add(path)) {
            return
        }
        val entry = ZipEntry(path)
        zos.putNextEntry(entry)
        zos.write(bytes)
        zos.closeEntry()
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    fun getMimeType(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".gif") -> "image/gif"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".svg") -> "image/svg+xml"
            lower.endsWith(".xhtml") || lower.endsWith(".html") || lower.endsWith(".htm") -> "application/xhtml+xml"
            lower.endsWith(".css") -> "text/css"
            lower.endsWith(".ncx") -> "application/x-dtbncx+xml"
            lower.endsWith(".opf") -> "application/oebps-package+xml"
            else -> "application/octet-stream"
        }
    }
}
