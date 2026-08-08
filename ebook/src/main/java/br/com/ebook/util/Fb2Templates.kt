package br.com.ebook.util

object Fb2Templates {
    const val container_xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n" +
            "  <rootfiles>\n" +
            "    <rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>\n" +
            "  </rootfiles>\n" +
            "</container>"

    const val content_opf = "<?xml version=\"1.0\"?>\n" +
            "<package version=\"2.0\" unique-identifier=\"uid\" xmlns=\"http://www.idpf.org/2007/opf\">\n" +
            " <metadata xmlns:opf=\"http://www.idpf.org/2007/opf\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n" +
            "  <dc:title>%title%</dc:title>\n" +
            "  <dc:creator>%creator%</dc:creator>\n" +
            "  <meta name=\"cover\" content=\"bilingual-cover-image\" />\n" +
            " </metadata>\n" +
            "\n<manifest>\n" +
            "  <item id=\"idBookFb2\" href=\"fb2.fb2\" media-type=\"application/xhtml+xml\"/>\n" +
            "  <item id=\"idResourceFb2\" href=\"fb2.ncx\" media-type=\"application/x-dtbncx+xml\"/>\n" +
            "  %manifest%\n" +
            " </manifest>\n" +
            " \n<spine toc=\"idResourceFb2\">\n" +
            "  %spine%\n" +
            "  <itemref idref=\"idBookFb2\"/>\n" +
            "</spine>\n" +
            "</package>"

    const val NCX = "<?xml version=\"1.0\"?>\n" +
            "<ncx version=\"2005-1\" xml:lang=\"en\" xmlns=\"http://www.daisy.org/z3986/2005/ncx/\">\n" +
            " <head>\n" +
            " </head>\n" +
            " <docTitle>\n" +
            "  <text>title</text>\n" +
            " </docTitle>\n" +
            " <navMap>\n  \n%nav% \n   \n </navMap>\n" +
            "</ncx>"
}
