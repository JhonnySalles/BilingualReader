package br.com.ebook.foobnix.ext;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import br.com.ebook.BaseExtractor;
import br.com.ebook.foobnix.android.utils.TxtUtils;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;

public class CalirbeExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CalirbeExtractor.class);

    public static boolean isCalibre(String path) {
        File rootFolder = new File(path).getParentFile();
        File metadata = new File(rootFolder, "metadata.opf");
        return metadata.isFile();
    }

    public static String getBookOverview(String path) {
        try {

            File rootFolder = new File(path).getParentFile();
            File metadata = new File(rootFolder, "metadata.opf");
            if (!metadata.isFile())
                return null;

            XmlPullParser xpp = XmlParser.buildPullParser();
            xpp.setInput(new FileInputStream(metadata), "UTF-8");

            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if ("dc:description".equals(xpp.getName())) {
                        return Jsoup.clean(xpp.nextText(), Whitelist.simpleText());
                    }
                }
                eventType = xpp.next();
            }
        } catch (Exception e) {
            LOGGER.error("Error get book overview: {}", e.getMessage(), e);
        }
        return "";
    }

    public static EbookMeta getBookMetaInformation(String path) {
        EbookMeta meta = EbookMeta.Empty();
        try {

            File rootFolder = new File(path).getParentFile();
            File metadata = new File(rootFolder, "metadata.opf");
            if (!metadata.isFile())
                return null;

            XmlPullParser xpp = XmlParser.buildPullParser();
            xpp.setInput(new FileInputStream(metadata), "UTF-8");

            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if ("dc:title".equals(xpp.getName()))
                        meta.setTitle(xpp.nextText());

                    if ("dc:creator".equals(xpp.getName())) {
                        String author = xpp.nextText();
                        if (AppState.get().isFirstSurname) {
                            author = TxtUtils.replaceLastFirstName(author);
                        }
                        if (TxtUtils.isNotEmpty(meta.getAuthor())) {
                            meta.setAuthor(meta.getAuthor() + ", " + author);
                        } else {
                            meta.setAuthor(author);
                        }
                    }

                    if ("dc:description".equals(xpp.getName()))
                        meta.setAnnotation(xpp.nextText());

                    if ("dc:identifier".equals(xpp.getName())) {
                        String content = xpp.nextText();
                        if (content != null && content.toLowerCase().contains("isbn"))
                            meta.setIsbn(content.replaceAll("[\\D]", ""));
                    }

                    if ("dc:publisher".equals(xpp.getName()))
                        meta.setPublisher(xpp.nextText());

                    if ("dc:date".equals(xpp.getName())) {
                        String date = xpp.nextText();
                        Date release = null;
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
                        } catch (Exception e) {
                        }
                        meta.setRelease(release);
                    }

                    if ("meta".equals(xpp.getName())) {
                        String attrName = xpp.getAttributeValue(null, "name");
                        String attrContent = xpp.getAttributeValue(null, "content");
                        String attrProperty = xpp.getAttributeValue(null, "property");

                        if ("calibre:series".equals(attrName))
                            meta.setSequence(attrContent.replace(",", ""));

                        if ("calibre:series_index".equals(attrName))
                            meta.setsIndex(Integer.parseInt(attrContent));

                        if ("group-position".equals(attrProperty))
                            meta.setsIndex(Integer.parseInt(xpp.getText()));
                    }

                    if ("reference".equals(xpp.getName()) && "cover".equals(xpp.getAttributeValue(null, "type"))) {
                        String imgName = xpp.getAttributeValue(null, "href");
                        FileInputStream fileStream = new FileInputStream(new File(rootFolder, imgName));
                        meta.coverImage = BaseExtractor.getEntryAsByte(fileStream);
                        fileStream.close();
                    }

                }
                eventType = xpp.next();
            }
        } catch (Exception e) {
            LOGGER.error("Error get book meta information: {}", e.getMessage(), e);
        }

        return meta;

    }
}
