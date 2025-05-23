package org.ebookdroid;

import android.content.Intent;

import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.droids.EpubContext;
import org.ebookdroid.droids.Fb2Context;
import org.ebookdroid.droids.HtmlContext;
import org.ebookdroid.droids.MobiContext;
import org.ebookdroid.droids.RtfContext;
import org.ebookdroid.droids.TxtContext;
import org.ebookdroid.droids.ZipContext;
import org.ebookdroid.droids.djvu.codec.DjvuContext;
import org.ebookdroid.droids.mupdf.codec.PdfContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import br.com.ebook.Config;

public enum BookType {
    PDF(PdfContext.class, Arrays.asList("pdf", "xps"), Arrays.asList("application/pdf")),
    TIFF(PdfContext.class, Arrays.asList("tiff", "tif"), Arrays.asList("image/tiff")),

    CBZ(PdfContext.class, Arrays.asList("cbz"), Arrays.asList("application/x-cbz")),

    EPUB(EpubContext.class, Arrays.asList("epub"), Arrays.asList("application/epub+zip")),

    FB2(Fb2Context.class, Arrays.asList("fb2"),
            Arrays.asList("application/fb2", "application/x-fictionbook", "application/x-fictionbook+xml", "application/x-fb2", "application/fb2+zip", "application/fb2.zip", "application/x-zip-compressed-fb2")),

    MOBI(MobiContext.class, Arrays.asList("mobi", "azw", "azw3", "azw4", "pdb", "prc"), Arrays.asList("application/x-mobipocket-ebook", "application/x-palm-database")),

    TXT(TxtContext.class, Arrays.asList("txt"), Arrays.asList("text/plain")),

    HTML(HtmlContext.class, Arrays.asList("html", "htm", "xhtml", "xhtm", "mht", "mhtml"), Arrays.asList("text/html", "text/xml")),

    RTF(RtfContext.class, Arrays.asList("rtf"), Arrays.asList("application/rtf", "application/x-rtf", "text/rtf", "text/richtext")),

    DJVU(DjvuContext.class, Arrays.asList("djvu"), Arrays.asList("image/vnd.djvu", "image/djvu", "image/x-djvu")),

    ZIP(ZipContext.class, Arrays.asList("zip"), Arrays.asList("application/zip", "application/x-compressed", "application/x-compressed-zip", "application/x-zip-compressed"));


    private static final Logger LOGGER = LoggerFactory.getLogger(BookType.class);

    private final static Map<String, BookType> extensionToActivity;

    private final static Map<String, BookType> mimeTypesToActivity;

    static {
        extensionToActivity = new HashMap<>();
        for (final BookType a : values()) {
            for (final String ext : a.extensions) {
                extensionToActivity.put(ext.toLowerCase(Locale.getDefault()), a);
            }
        }
        mimeTypesToActivity = new HashMap<>();
        for (final BookType a : values()) {
            for (final String type : a.mimeTypes) {
                mimeTypesToActivity.put(type.toLowerCase(Locale.getDefault()), a);
            }
        }
    }

    private final Class<? extends CodecContext> contextClass;

    private final List<String> extensions;

    private final List<String> mimeTypes;

    private BookType(final Class<? extends CodecContext> contextClass, final List<String> extensions, final List<String> mimeTypes) {
        this.contextClass = contextClass;
        this.extensions = extensions;
        this.mimeTypes = mimeTypes;
    }

    public boolean is(String path) {
        if (path == null) {
            return false;
        }

        path = path.toLowerCase(Locale.getDefault());
        for (final String ext : extensions) {
            if (path.endsWith(ext) || path.endsWith(ext + ".zip")) {
                return true;
            }
        }
        return false;
    }

    public String getExt() {
        return extensions.get(0);
    }

    public boolean is(Intent intent) {
        try {
            return is(intent.getData().getPath());
        } catch (Exception e) {
            return false;
        }
    }

    public static List<String> getAllSupportedExtensions() {
        List<String> list = new ArrayList<String>();

        for (final BookType a : values()) {
            for (final String ext : a.extensions) {
                list.add(ext);
            }
        }
        return list;
    }

    public static CodecContext getCodecContextByType(BookType activityType) {
        try {
            return activityType.contextClass.newInstance();
        } catch (Exception e) {
            LOGGER.error("Error get codec context by type: {}", e.getMessage(), e);
        }
        return null;
    }

    public static CodecContext getCodecContextByPath(String path) {
        CodecContext ctx = null;
        try {
            ctx = getByUri(path).contextClass.newInstance();
        } catch (Exception e) {
            if (Config.SHOW_LOG)
                LOGGER.error("Error get codec context by path: {}", e.getMessage(), e);
        }
        return ctx;
    }

    public String getFirstMimeTime() {
        return mimeTypes.get(0);
    }

    public static boolean isSupportedExtByPath(String path) {
        if (path == null) {
            return false;
        }
        path = path.toLowerCase(Locale.getDefault());

        for (final BookType a : values()) {
            for (final String ext : a.extensions) {
                if (path.endsWith(ext)) {
                    return true;
                }

            }
        }
        return false;
    }

    public static BookType getByUri(String uri) {
        if (uri == null) {
            return null;
        }

        uri = uri.toLowerCase(Locale.getDefault());

        for (final String ext : extensionToActivity.keySet()) {
            if (uri.endsWith("." + ext)) {
                return extensionToActivity.get(ext);
            }
        }
        return null;
    }

    public static BookType getByMimeType(final String type) {
        if (type == null) {
            return null;
        }
        return mimeTypesToActivity.get(type.toLowerCase());
    }

}
