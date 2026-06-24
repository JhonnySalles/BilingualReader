package br.com.fenix.bilingualreader.service.parses.mock

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ParseMock {

    fun createZip(file: File, entries: Map<String, String>) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(file))).use { zos ->
            for ((name, content) in entries) {
                val entry = ZipEntry(name)
                zos.putNextEntry(entry)
                zos.write(content.toByteArray())
                zos.closeEntry()
            }
        }
    }

    fun createTar(file: File, entries: Map<String, String>) {
        TarArchiveOutputStream(BufferedOutputStream(FileOutputStream(file))).use { tos ->
            for ((name, content) in entries) {
                val bytes = content.toByteArray()
                val entry = TarArchiveEntry(name)
                entry.size = bytes.size.toLong()
                tos.putArchiveEntry(entry)
                tos.write(bytes)
                tos.closeArchiveEntry()
            }
        }
    }

    fun create7z(file: File, entries: Map<String, String>) {
        SevenZOutputFile(file).use { s7z ->
            for ((name, content) in entries) {
                val entry = s7z.createArchiveEntry(File(""), name)
                val bytes = content.toByteArray()
                entry.size = bytes.size.toLong()
                s7z.putArchiveEntry(entry)
                s7z.write(bytes)
                s7z.closeArchiveEntry()
            }
        }
    }

    fun createMockMangaEntries(): Map<String, String> {
        return mapOf(
            "page01.jpg" to "dummy image content 1",
            "page02.png" to "dummy image content 2",
            "chapter1/page03.jpg" to "dummy image content 3",
            "vocabulary.json" to "{\"word\": \"test\"}",
            "ComicInfo.xml" to """
                <?xml version="1.0"?>
                <ComicInfo xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
                  <Title>Mock Title</Title>
                  <Series>Mock Series</Series>
                  <Number>1</Number>
                  <Volume>1</Volume>
                </ComicInfo>
            """.trimIndent()
        )
    }

    fun createMockEpubEntries(): Map<String, String> {
        return mapOf(
            "mimetype" to "application/epub+zip",
            "META-INF/container.xml" to """
                <?xml version="1.0"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles>
                    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                  </rootfiles>
                </container>
            """.trimIndent(),
            "OEBPS/content.opf" to """
                <?xml version="1.0" encoding="UTF-8"?>
                <package xmlns="http://www.idpf.org/2007/opf" unique-identifier="pub-id" version="3.0">
                  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <dc:title>Mock Epub Title</dc:title>
                    <dc:creator>Mock Author</dc:creator>
                    <dc:language>en</dc:language>
                    <meta name="calibre:series" content="Mock Series"/>
                  </metadata>
                  <manifest>
                    <item id="page1" href="page1.jpg" media-type="image/jpeg"/>
                    <item id="page2" href="page2.jpg" media-type="image/jpeg"/>
                    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
                  </manifest>
                  <spine>
                    <itemref idref="page1"/>
                    <itemref idref="page2"/>
                  </spine>
                </package>
            """.trimIndent(),
            "OEBPS/page1.jpg" to "image 1 core",
            "OEBPS/page2.jpg" to "image 2 core",
            "OEBPS/nav.xhtml" to """
                <nav xmlns:epub="http://www.idpf.org/2007/ops" epub:type="toc">
                    <ol>
                        <li><a href="page1.jpg">Chapter 1</a></li>
                    </ol>
                </nav>
            """.trimIndent()
        )
    }
}
