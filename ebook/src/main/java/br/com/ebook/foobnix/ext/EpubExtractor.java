package br.com.ebook.foobnix.ext;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import br.com.ebook.BaseExtractor;
import br.com.ebook.Config;
import br.com.ebook.foobnix.android.utils.TxtUtils;
import br.com.ebook.foobnix.pdf.info.ExtUtils;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;
import br.com.ebook.foobnix.sys.TempHolder;

public class EpubExtractor extends BaseExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EpubExtractor.class);

    final static EpubExtractor inst = new EpubExtractor();

    private EpubExtractor() {

    }

    public static EpubExtractor get() {
        return inst;
    }

    public static void proccessHypens(String input, String output) {
        try {
            if (Config.SHOW_LOG)
                LOGGER.info("proccessHypens: {} || {}", input, output);

            File file = new File(input);
            ZipFile zipFile = new ZipFile(file, StandardCharsets.UTF_8);
            ZipEntry nextEntry = null;
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(output)));
            zos.setLevel(0);

            while (entries.hasMoreElements() && (nextEntry = entries.nextElement()) != null) {
                if (TempHolder.get().loadingCancelled) {
                    break;
                }
                String name = nextEntry.getName();
                String nameLow = name.toLowerCase();

                if (!name.endsWith("container.xml") && (nameLow.endsWith("html") || nameLow.endsWith("htm") || nameLow.endsWith("xml"))) {
                    if (Config.SHOW_LOG)
                        LOGGER.info("nextEntry HTML cancell: {} -- {}", TempHolder.get().loadingCancelled, name);
                    ByteArrayOutputStream hStream = Fb2Extractor.generateHyphenFile(new InputStreamReader(zipFile.getInputStream(nextEntry)));
                    Fb2Extractor.writeToZipNoClose(zos, name, new ByteArrayInputStream(hStream.toByteArray()));
                } else {
                    if (Config.SHOW_LOG)
                        LOGGER.info("nextEntry cancell: {} -- {}", TempHolder.get().loadingCancelled, name);
                    Fb2Extractor.writeToZipNoClose(zos, name, zipFile.getInputStream(nextEntry));
                }

            }
            zipFile.close();

            zos.close();
        } catch (Exception e) {
            LOGGER.error("Error process hypens: {}", e.getMessage(), e);
        }
    }

    @Override
    public String getBookOverview(String path) {
        String info = "";
        try {
            final File file = new File(path);
            ZipFile zipFile = new ZipFile(file, StandardCharsets.UTF_8);
            ZipEntry nextEntry = null;
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements() && (nextEntry = entries.nextElement()) != null) {
                String name = nextEntry.getName().toLowerCase();
                if (name.endsWith(".opf")) {

                    XmlPullParser xpp = XmlParser.buildPullParser();
                    xpp.setInput(zipFile.getInputStream(nextEntry), "utf-8");

                    int eventType = xpp.getEventType();

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            if ("dc:description".equals(xpp.getName()) || "dcns:description".equals(xpp.getName())) {
                                info = xpp.nextText();
                                break;
                            }
                        }
                        if (eventType == XmlPullParser.END_TAG) {
                            if ("metadata".equals(xpp.getName())) {
                                break;
                            }
                        }
                        eventType = xpp.next();
                    }
                }
            }
            zipFile.close();
        } catch (Exception e) {
            LOGGER.error("Error get book overview: {}", e.getMessage(), e);
        }
        return info;
    }

    @Override
    public EbookMeta getBookMetaInformation(String path) {
        final File file = new File(path);
        try {
            ZipFile zipFile = new ZipFile(file, StandardCharsets.UTF_8);
            ZipEntry nextEntry = null;
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            String title = null;
            String author = null;
            String subject = "";
            String series = null;
            String number = null;
            String lang = null;
            String isbn = null;
            String publisher = null;
            Date release = null;

            while (entries.hasMoreElements() && (nextEntry = entries.nextElement()) != null) {
                String name = nextEntry.getName().toLowerCase();
                if (name.endsWith(".opf")) {

                    XmlPullParser xpp = XmlParser.buildPullParser();
                    xpp.setInput(zipFile.getInputStream(nextEntry), "utf-8");

                    int eventType = xpp.getEventType();

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            if ("dc:title".equals(xpp.getName()) || "dcns:title".equals(xpp.getName()))
                                title = xpp.nextText();

                            if ("dc:creator".equals(xpp.getName()) || "dcns:creator".equals(xpp.getName()))
                                author = xpp.nextText();

                            if ("dc:subject".equals(xpp.getName()) || "dcns:subject".equals(xpp.getName()))
                                subject = xpp.nextText() + "," + subject;

                            if (lang == null && ("dc:language".equals(xpp.getName()) || "dcns:language".equals(xpp.getName())))
                                lang = xpp.nextText();

                            if (isbn == null && ("dc:identifier".equals(xpp.getName()) || "dcns:identifier".equals(xpp.getName()))) {
                                String content = xpp.nextText();
                                if (content != null && content.toLowerCase().contains("isbn"))
                                    isbn = content.replaceAll("[\\D]", "");
                            }

                            if (publisher == null && ("dc:publisher".equals(xpp.getName()) || "dcns:publisher".equals(xpp.getName())))
                                publisher = xpp.nextText();

                            if (release == null && ("dc:date".equals(xpp.getName()) || "dcns:date".equals(xpp.getName()))) {
                                String date = xpp.nextText();
                                try {
                                    if (date.contains("T"))
                                        release = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(date);
                                    else {
                                        try {
                                            release = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(date);
                                        } catch (Exception e) {
                                            release = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).parse(date);
                                        }
                                    }
                                } catch (Exception ignored) {
                                }
                            }

                            if ("meta".equals(xpp.getName())) {
                                String nameAttr = xpp.getAttributeValue(null, "name");
                                if ("calibre:series".equals(nameAttr)) {
                                    series = xpp.getAttributeValue(null, "content");
                                } else if ("calibre:series_index".equals(nameAttr)) {
                                    number = xpp.getAttributeValue(null, "content");
                                    if (number != null) {
                                        number = number.replace(".0", "");
                                    }
                                } else {
                                    String propertyAttr = xpp.getAttributeValue(null, "property");
                                    if ("group-position".equals(propertyAttr)) {
                                        number = xpp.getText();
                                    }
                                }
                            }
                        }
                        if (eventType == XmlPullParser.END_TAG) {
                            if ("metadata".equals(xpp.getName()))
                                break;
                        }
                        eventType = xpp.next();
                    }
                }
            }
            zipFile.close();

            if (AppState.get().isFirstSurname)
                author = TxtUtils.replaceLastFirstName(author);

            EbookMeta ebookMeta = new EbookMeta(title, author, series, subject.replaceAll(",$", ""), isbn, publisher, release);
            try {
                if (number != null)
                    ebookMeta.setsIndex(Integer.parseInt(number));
            } catch (Exception e) {
                if (Config.SHOW_LOG)
                    LOGGER.warn("Error to set index: {}", e.getMessage(), e);
            }
            ebookMeta.setLang(lang);
            return ebookMeta;
        } catch (Exception e) {
            LOGGER.error("Error get book meta information: {}", e.getMessage(), e);
            return EbookMeta.Empty();
        }
    }

    @Override
    public byte[] getBookCover(String path) {
        byte[] cover = null;
        try {
            ZipFile zipFile = new ZipFile(new File(path), StandardCharsets.UTF_8);
            ZipEntry nextEntry = null;
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            String coverName = null;
            String coverResource = null;

            while (coverName == null && entries.hasMoreElements() && (nextEntry = entries.nextElement()) != null) {
                String name = nextEntry.getName().toLowerCase();
                if (name.endsWith(".opf")) {
                    XmlPullParser xpp = XmlParser.buildPullParser();
                    xpp.setInput(zipFile.getInputStream(nextEntry), "utf-8");

                    int eventType = xpp.getEventType();

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            if (("meta".equals(xpp.getName()) || (xpp.getName() != null && xpp.getName().endsWith(":meta"))) &&
                                    "cover".equals(xpp.getAttributeValue(null, "name"))) {
                                coverResource = xpp.getAttributeValue(null, "content");
                            }

                            if (coverResource != null && ("item".equals(xpp.getName()) || (xpp.getName() != null && xpp.getName().endsWith(":item"))) &&
                                    (coverResource.equals(xpp.getAttributeValue(null, "id")) || coverResource.equals(xpp.getAttributeValue(null, "properties")))) {
                                coverName = xpp.getAttributeValue(null, "href");
                                if (coverName != null && coverName.endsWith(".svg")) {
                                    coverName = null;
                                }
                                break;
                            }

                            if (coverResource == null && "item".equals(xpp.getName()) && xpp.getAttributeValue(null, "properties") != null &&
                                    xpp.getAttributeValue(null, "properties").toLowerCase().contains("cover")) {
                                coverName = xpp.getAttributeValue(null, "href");
                                if (coverName != null && coverName.endsWith(".svg")) {
                                    coverName = null;
                                }
                                break;
                            }
                        }
                        eventType = xpp.next();
                    }
                }
            }

            if (coverName != null) {
                zipFile.close();
                zipFile = new ZipFile(new File(path), StandardCharsets.UTF_8);
                nextEntry = null;
                entries = zipFile.entries();

                while (entries.hasMoreElements() && (nextEntry = entries.nextElement()) != null) {
                    String name = nextEntry.getName();
                    if (name.contains(coverName)) {
                        cover = BaseExtractor.getEntryAsByte(zipFile.getInputStream(nextEntry));
                        break;
                    }
                }
            }

            if (cover == null) {
                zipFile.close();
                zipFile = new ZipFile(new File(path), StandardCharsets.UTF_8);
                nextEntry = null;
                entries = zipFile.entries();
                byte[] coverAux = null;

                while (entries.hasMoreElements() && (nextEntry = entries.nextElement()) != null) {
                    String name = nextEntry.getName().toLowerCase(Locale.getDefault());

                    if (name.contains("\\"))
                        name = name.substring(name.lastIndexOf("\\") + 1);

                    if (name.contains("/"))
                        name = name.substring(name.lastIndexOf("/") + 1);

                    if (name.endsWith(".jpeg") || name.endsWith(".jpg") || name.endsWith(".png")) {
                        if (name.startsWith("cover")) {
                            cover = BaseExtractor.getEntryAsByte(zipFile.getInputStream(nextEntry));
                            break;
                        }

                        if (name.contains("cover"))
                            coverAux = BaseExtractor.getEntryAsByte(zipFile.getInputStream(nextEntry));
                    }
                }

                if (cover == null && coverAux != null)
                    cover = coverAux;
            }

            if (cover == null) {
                zipFile.close();
                zipFile = new ZipFile(new File(path), StandardCharsets.UTF_8);
                nextEntry = null;
                entries = zipFile.entries();

                while (entries.hasMoreElements() && (nextEntry = entries.nextElement()) != null) {
                    String name = nextEntry.getName().toLowerCase(Locale.getDefault());
                    if (name.endsWith(".jpeg") || name.endsWith(".jpg") || name.endsWith(".png")) {
                        cover = BaseExtractor.getEntryAsByte(zipFile.getInputStream(nextEntry));
                        break;
                    }
                }
            }

            zipFile.close();

        } catch (Exception e) {
            LOGGER.error("Error get book cover: {}", e.getMessage(), e);
        }
        return cover;
    }

    public static File extractAttachment(File bookPath, String attachmentName) {
        if (Config.SHOW_LOG)
            LOGGER.info("Begin extractAttachment: {} -- {}", bookPath.getPath(), attachmentName);
        try {

            InputStream in = new FileInputStream(bookPath);
            ZipInputStream zipInputStream = new ZipInputStream(in);

            ZipEntry nextEntry = null;
            while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                if (TempHolder.get().loadingCancelled)
                    break;

                if (nextEntry.getName().equals(attachmentName)) {
                    if (attachmentName.contains("/"))
                        attachmentName = attachmentName.substring(attachmentName.lastIndexOf("/") + 1);

                    File extractMedia = new File(CacheZipUtils.ATTACHMENTS_CACHE_DIR, attachmentName);

                    if (Config.SHOW_LOG)
                        LOGGER.info("Begin extractAttachment extract: {}", extractMedia.getPath());

                    FileOutputStream fileOutputStream = new FileOutputStream(extractMedia);
                    OutputStream out = new BufferedOutputStream(fileOutputStream);
                    writeToStream(zipInputStream, out);
                    return extractMedia;
                }
                // zipInputStream.closeEntry();
            }

            return null;
        } catch (Exception e) {
            LOGGER.error("Error extract attachment: {}", e.getMessage(), e);
            return null;
        }
    }

    public static void writeToStream(InputStream zipInputStream, OutputStream out) throws IOException {

        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipInputStream.read(bytesIn)) != -1) {
            out.write(bytesIn, 0, read);
        }
        out.close();
    }

    public static List<String> getAttachments(String inputPath) throws IOException {
        List<String> attachments = new ArrayList<String>();
        try {
            InputStream in = new FileInputStream(new File(inputPath));
            ZipEntry nextEntry = null;
            ZipInputStream zipInputStream = new ZipInputStream(in);
            while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                if (TempHolder.get().loadingCancelled)
                    break;

                String name = nextEntry.getName();
                if (Config.SHOW_LOG)
                    LOGGER.info("getAttachments: {}", name);
                if (ExtUtils.isMediaContent(name)) {
                    if (nextEntry.getSize() > 0)
                        name = name + "," + nextEntry.getSize();
                    else if (nextEntry.getCompressedSize() > 0)
                        name = name + "," + nextEntry.getCompressedSize();
                    else
                        name = name + "," + 0;

                    attachments.add(name);
                }
            }
            zipInputStream.close();
        } catch (Exception e) {
            LOGGER.error("Error get attachments: {}", e.getMessage(), e);
        }
        return attachments;
    }

    @Override
    public Map<String, String> getFooterNotes(String inputPath) {
        Map<String, String> notes = new HashMap<String, String>();
        try {
            InputStream in = new FileInputStream(new File(inputPath));
            ZipInputStream zipInputStream = new ZipInputStream(in);

            ZipEntry nextEntry = null;
            Map<String, String> textLink = new HashMap<String, String>();
            Set<String> files = new HashSet<String>();

            try {
                CacheZipUtils.removeFiles(CacheZipUtils.ATTACHMENTS_CACHE_DIR.listFiles());

                while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                    if (TempHolder.get().loadingCancelled)
                        break;

                    String name = nextEntry.getName();
                    String nameLow = name.toLowerCase();
                    if (nameLow.endsWith("html") || nameLow.endsWith("htm") || nameLow.endsWith("xml")) {
                        // System.out.println("- " + nameLow + " -");
                        Document parse = Jsoup.parse(zipInputStream, null, "", Parser.xmlParser());
                        Elements select = parse.select("a[href]");
                        for (int i = 0; i < select.size(); i++) {
                            Element item = select.get(i);
                            String text = item.text();
                            if (item.attr("href").contains("#")) {
                                String attr = item.attr("href");
                                String file = attr.substring(0, attr.indexOf("#"));
                                // System.out.println(text + " -> " + attr + "
                                // [" +
                                // file);
                                if (attr.startsWith("#")) {
                                    attr = name + attr;
                                }
                                if (!TxtUtils.isFooterNote(text)) {
                                    if (Config.SHOW_LOG)
                                        LOGGER.info("Skip text: {}", text);
                                    continue;
                                }

                                textLink.put(attr, text);

                                if (Config.SHOW_LOG)
                                    LOGGER.info("Extract file: {}", file);

                                if (TxtUtils.isEmpty(file))
                                    file = name;

                                if (file.endsWith("html") || file.endsWith("htm") || nameLow.endsWith("xml"))
                                    files.add(file);
                            }
                        }

                    }
                    zipInputStream.closeEntry();
                }

                in = new FileInputStream(new File(inputPath));
                zipInputStream = new ZipInputStream(in);

                while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                    if (TempHolder.get().loadingCancelled)
                        break;

                    String name = nextEntry.getName();
                    for (String fileName : files) {

                        if (name.endsWith(fileName)) {
                            if (Config.SHOW_LOG)
                                LOGGER.info("PARSE FILE NAME: {}", name);
                            // System.out.println("file: " + name);
                            Parser xmlParser = Parser.xmlParser();
                            Document parse = Jsoup.parse(zipInputStream, null, "", xmlParser);
                            Elements ids = parse.select("[id]");
                            for (int i = 0; i < ids.size(); i++) {
                                Element item = ids.get(i);
                                String id = item.attr("id");
                                String value = item.text();

                                if (value.trim().length() < 4)
                                    value = value + " " + parse.select("[id=" + id + "]+*").text();

                                if (value.trim().length() < 4)
                                    value = value + " " + parse.select("[id=" + id + "]+*+*").text();

                                try {
                                    if (value.trim().length() < 4)
                                        value = value + " " + parse.select("[id=" + id + "]").parents().get(0).text();
                                } catch (Exception e) {
                                    if (Config.SHOW_LOG)
                                        LOGGER.error("Error trim value: {}", e.getMessage(), e);
                                }

                                // System.out.println("id:" + id + " value:"
                                // +
                                // value);
                                String fileKey = fileName + "#" + id;

                                String textKey = textLink.get(fileKey);

                                if (Config.SHOW_LOG)
                                    LOGGER.info("{} {}", textKey, value);
                                notes.put(textKey, value);

                            }
                        }

                    }
                    zipInputStream.closeEntry();
                }
                zipInputStream.close();
                in.close();

            } catch (Exception e) {
                if (Config.SHOW_LOG)
                    LOGGER.error("Error get footer notes: {}", e.getMessage(), e);
            }

            return notes;
        } catch (Throwable e) {
            LOGGER.error("Error get footer notes: {}", e.getMessage(), e);
            return notes;
        }
    }

    @Override
    public boolean convert(String path, String to) {
        return false;
    }

}
