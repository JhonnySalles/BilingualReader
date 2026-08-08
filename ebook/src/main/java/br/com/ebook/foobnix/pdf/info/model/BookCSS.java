package br.com.ebook.foobnix.pdf.info.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.os.Build;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import br.com.ebook.foobnix.android.utils.TxtUtils;
import br.com.ebook.foobnix.entity.FileMeta;
import br.com.ebook.foobnix.pdf.info.ExportSettingsManager;
import br.com.ebook.foobnix.pdf.info.ExtUtils;
import br.com.ebook.foobnix.pdf.info.FontExtractor;
import br.com.ebook.foobnix.pdf.info.Urls;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;
import br.com.ebook.foobnix.pdf.info.wrapper.MagicHelper;

public class BookCSS {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookCSS.class);

    public static final String LINK_COLOR_NIGHT = "#876f52";
    public static final String LINK_COLOR_DAY = "#8c1908";
    public static final String LINK_COLOR_UNIVERSAL = "#0066cc";

    public static final String FONTS_DIR = "Fonts";

    public static final int TEXT_ALIGN_JUSTIFY = 0;
    public static final int TEXT_ALIGN_LEFT = 1;
    public static final int TEXT_ALIGN_RIGHT = 2;
    public static final int TEXT_ALIGN_CENTER = 3;

    public static int STYLES_DOC_AND_USER = 0;
    public static int STYLES_ONLY_DOC = 1;
    public static int STYLES_ONLY_USER = 2;

    public static final String COURIER = "Courier";
    private static final String DEFAULT_FONT = "Times New Roman";

    public static List<String> fontExts = Arrays.asList(".ttf", ".otf");

    private static List<String> FONT_NAMES = Arrays.asList(//
            "Arial", //
            // "Noto Serif", "Noto Sans", //
            // "Charis SIL", //
            COURIER, //
            // "Helvetica", //
            DEFAULT_FONT);
    static {
        Collections.sort(FONT_NAMES);
    }
    public int documentStyle = STYLES_DOC_AND_USER;
    public int marginTop;
    public int marginRight;
    public int marginBottom;
    public int marginLeft;

    public int emptyLine;

    public int lineHeight;
    public int textIndent;
    public int fontWeight;
    public String customCSS1;

    public int textAlign;

    public String fontFolder;

    public String normalFont;
    public String boldFont;
    public String boldItalicFont;
    public String italicFont;
    public String headersFont;

    public boolean isAutoHypens;
    public String hypenLang;
    public String linkColorDay;
    public String linkColorNight;

    public void load(Context c) {
        if (c == null) {
            return;
        }
        resetToDefault(c);

        SharedPreferences sp = c.getSharedPreferences(ExportSettingsManager.PREFIX_BOOK_CSS, Context.MODE_PRIVATE);
        fontFolder = sp.getString("fontFolder1", fontFolder);

        normalFont = sp.getString("normalFont", normalFont);
        boldFont = sp.getString("boldFont", boldFont);
        boldItalicFont = sp.getString("boldItalicFont", boldItalicFont);
        italicFont = sp.getString("italicFont", italicFont);
        headersFont = sp.getString("headersFont", headersFont);

        textAlign = sp.getInt("textAlign", textAlign);
        marginTop = sp.getInt("marginTop", marginTop);
        marginRight = sp.getInt("marginRight", marginRight);
        marginBottom = sp.getInt("marginBottom", marginBottom);
        marginLeft = sp.getInt("marginLeft", marginLeft);
        emptyLine = sp.getInt("emptyLine", emptyLine);
        customCSS1 = sp.getString("customCSS1", customCSS1);

        lineHeight = sp.getInt("lineHeight", lineHeight);
        textIndent = sp.getInt("textIndent", textIndent);
        fontWeight = sp.getInt("fontWeight", fontWeight);
        documentStyle = sp.getInt("documentStyle", documentStyle);

        isAutoHypens = sp.getBoolean("isAutoHypens1", isAutoHypens);
        hypenLang = sp.getString("hypenLang", hypenLang);
        linkColorDay = sp.getString("linkColorDay", linkColorDay);
        linkColorNight = sp.getString("linkColorNight", linkColorNight);

    }

    public int position(String fontName) {
        try {
            List<String> allFonts = getAllFonts();
            return allFonts.indexOf(fontName);
        } catch (Exception e) {
            return 0;
        }

    }

    public void resetToDefault(Context c) {
        textAlign = TEXT_ALIGN_JUSTIFY;

        marginTop = 9;
        marginRight = 8;
        marginBottom = 6;
        marginLeft = 8;

        emptyLine = 5;

        lineHeight = 13;
        textIndent = 10;
        fontWeight = 400;

        fontFolder = DEFAULT_FOLDER(c);
        normalFont = DEFAULT_FONT;
        boldFont = DEFAULT_FONT;
        italicFont = DEFAULT_FONT;
        boldItalicFont = DEFAULT_FONT;
        headersFont = DEFAULT_FONT;

        documentStyle = STYLES_DOC_AND_USER;
        isAutoHypens = true;
        hypenLang = "ru";

        linkColorDay = LINK_COLOR_UNIVERSAL;
        linkColorNight = LINK_COLOR_UNIVERSAL;
        customCSS1 = "pre{white-space:pre;font-size:0.7em;}\n" + //
                "svg{display:block;}\n" + //
                "figure{font-size:0.7em;}";

        

    }

    private String DEFAULT_FOLDER(Context c) {
        return FontExtractor.getFontsDir(c, FONTS_DIR).getPath();
    }

    public void checkBeforeExport(Context c) {
        if (fontFolder != null && fontFolder.equals(DEFAULT_FOLDER(c))) {
            fontFolder = null;
            save(c);
            fontFolder = DEFAULT_FOLDER(c);
        }

    }

    public void allFonts(String fontName) {
        normalFont = fontName;
    }

    public void save(Context c) {
        if (c == null) {
            return;
        }
        SharedPreferences sp = c.getSharedPreferences(ExportSettingsManager.PREFIX_BOOK_CSS, Context.MODE_PRIVATE);
        Editor edit = sp.edit();
        edit.putString("fontFolder1", fontFolder);

        edit.putString("normalFont", normalFont);
        edit.putString("boldFont", boldFont);
        edit.putString("italicFont", italicFont);
        edit.putString("boldItalicFont", boldItalicFont);
        edit.putString("headersFont", headersFont);

        edit.putInt("textAlign", textAlign);
        edit.putInt("marginTop", marginTop);
        edit.putInt("marginRight", marginRight);
        edit.putInt("marginBottom", marginBottom);
        edit.putInt("marginLeft", marginLeft);
        edit.putInt("emptyLine", emptyLine);
        edit.putString("customCSS1", customCSS1);

        edit.putInt("lineHeight", lineHeight);
        edit.putInt("textIndent", textIndent);
        edit.putInt("fontWeight", fontWeight);
        edit.putInt("documentStyle", documentStyle);
        edit.putBoolean("isAutoHypens1", isAutoHypens);
        edit.putString("hypenLang", hypenLang);
        edit.putString("linkColorDay", linkColorDay);
        edit.putString("linkColorNight", linkColorNight);

        edit.commit();
    }

    public void resetAll(String initFontName) {
        if (FONT_NAMES.contains(initFontName)) {
            normalFont = initFontName;
            boldFont = initFontName;
            italicFont = initFontName;
            boldItalicFont = initFontName;
            headersFont = initFontName;
            return;
        }
        normalFont = DEFAULT_FONT;
        boldFont = DEFAULT_FONT;
        italicFont = DEFAULT_FONT;
        boldItalicFont = DEFAULT_FONT;
        headersFont = DEFAULT_FONT;

        initFontName = initFontName.replace(".ttf", "").replace(".otf", "").trim().toLowerCase(Locale.getDefault());

        List<String> allFonts = getAllFonts();
        for (String fullName : allFonts) {
            String fontName = fullName.replace(".ttf", "").replace(".otf", "").trim().toLowerCase(Locale.getDefault());

            if (fontName.startsWith(initFontName) || fontName.equals(initFontName)) {

                if (fontName.equals(initFontName) || fontName.endsWith("regular") || fontName.endsWith("normal") || fontName.endsWith("light") || fontName.endsWith("medium") || fontName.endsWith("me")) {
                    normalFont = fullName;

                } else if (fontName.endsWith("bolditalic") || fontName.endsWith("boldit") || fontName.endsWith("boit") || fontName.endsWith("bold it") || fontName.endsWith("bold italic")) {
                    boldItalicFont = fullName;

                } else if (fontName.endsWith("bold") || fontName.endsWith("bo") || fontName.endsWith("bd") || fontName.endsWith("bolt")) {
                    headersFont = boldFont = fullName;

                } else if (fontName.endsWith("italic") || fontName.endsWith("it")) {
                    italicFont = fullName;

                }
            }
        }

    }

    public List<String> getAllFonts() {
        List<String> all = new ArrayList<String>();
        all.addAll(FONT_NAMES);
        all.addAll(getAllFontsFromFolder(fontFolder));
        return all;
    }

    public List<String> getAllFontsFiltered() {
        List<String> all = new ArrayList<String>();
        all.addAll(FONT_NAMES);
        all.addAll(getAllFontsFiltered(fontFolder));
        return all;
    }

    private Collection<String> getAllFontsFiltered(String path) {
        if (TxtUtils.isNotEmpty(path) && new File(path).isDirectory()) {
            File file = new File(path);
            String[] list = file.list(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    name = name.toLowerCase(Locale.getDefault());
                    for (String ext : fontExts) {
                        if (name.endsWith(ext)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
            if (list != null && list.length >= 1) {
                List<String> filtered = new ArrayList<String>();

                for (String fontName : list) {
                    fontName = filterFontName(fontName);

                    if (!filtered.contains(fontName)) {
                        filtered.add(fontName);
                    }
                }

                Collections.sort(filtered, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return o1.toLowerCase().compareTo(o2.toLowerCase());
                    }

                });
                return filtered;
            }
        }
        return Collections.EMPTY_LIST;
    }

    public static String filterFontName(String fontName) {
        if (!fontName.contains(".")) {
            return fontName;
        }
        String ext = ExtUtils.getFileExtension(fontName);
        if (fontName.contains("-")) {
            fontName = fontName.substring(0, fontName.lastIndexOf("-")) + "." + ext;
        } else if (fontName.contains("_")) {
            fontName = fontName.substring(0, fontName.lastIndexOf("_")) + "." + ext;
        } else if (fontName.contains(" ")) {
            fontName = fontName.substring(0, fontName.lastIndexOf(" ")) + "." + ext;
        }
        return fontName;
    }

    private Collection<String> getAllFontsFromFolder(String path) {
        if (TxtUtils.isNotEmpty(path) && new File(path).isDirectory()) {
            File file = new File(path);
            String[] list = file.list(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    name = name.toLowerCase(Locale.getDefault());
                    for (String ext : fontExts) {
                        if (name.endsWith(ext)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
            if (list != null && list.length >= 1) {
                List<String> filtered = new ArrayList<String>();

                for (String fontName : list) {
                    filtered.add(fontName);
                }

                Collections.sort(filtered, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return o1.toLowerCase().compareTo(o2.toLowerCase());
                    }

                });
                return filtered;
            }
        }
        return Collections.EMPTY_LIST;
    }

    public boolean isFontFileName(String name) {
        if (TxtUtils.isEmpty(name)) {
            return false;
        }
        name = name.toLowerCase(Locale.getDefault());
        for (String ext : fontExts) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public String em(int value) {
        if (value == 0) {
            return "0px";
        }
        float em = (float) value / 10;
        return "" + em + "em";
    }

    private static BookCSS inst = new BookCSS();

    public static BookCSS get() {

        return inst;
    }

    public String getTextAlignConst(int id) {
        if (id == TEXT_ALIGN_JUSTIFY) {
            return "justify";
        }
        if (id == TEXT_ALIGN_LEFT) {
            return "left";
        }
        if (id == TEXT_ALIGN_RIGHT) {
            return "right";
        }
        if (id == TEXT_ALIGN_CENTER) {
            return "center";
        }
        return "initial";
    }

    /** Android 8–12 (API <= 31): MuPDF is more sensitive to invalid / complex CSS. */
    static boolean isLegacyCssDevice() {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.S;
    }

    static String quoteFontFamily(String name) {
        if (TxtUtils.isEmpty(name)) {
            return "'serif'";
        }
        String safe = name.replace("'", "\\'");
        return "'" + safe + "'";
    }

    /**
     * Builds a CSS url(...) value for @font-face. Returns null if the path cannot be made safe.
     */
    static String cssUrl(String path) {
        if (TxtUtils.isEmpty(path)) {
            return null;
        }
        try {
            String normalized = path.replace('\\', '/');
            StringBuilder encoded = new StringBuilder();
            for (int i = 0; i < normalized.length(); i++) {
                char c = normalized.charAt(i);
                if (c == ' ') {
                    encoded.append("%20");
                } else if (c == '\'') {
                    encoded.append("%27");
                } else if (c == '"') {
                    encoded.append("%22");
                } else if (c == '\n' || c == '\r' || c == '\t') {
                    continue;
                } else {
                    encoded.append(c);
                }
            }
            String result = encoded.toString();
            if (result.isEmpty() || result.contains("*/")) {
                return null;
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Basic validation/sanitization of user custom CSS before injecting into MuPDF.
     * Returns empty string if braces are unbalanced or content looks unsafe.
     */
    static String sanitizeCustomCss(String css, boolean compat) {
        if (TxtUtils.isEmpty(css)) {
            return "";
        }
        String cleaned = css.replace("\n", " ").replace("\r", " ").trim();
        if (cleaned.contains("*/") && !cleaned.contains("/*")) {
            return "";
        }
        int open = 0;
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (c == '{') {
                open++;
            } else if (c == '}') {
                open--;
                if (open < 0) {
                    return "";
                }
            }
        }
        if (open != 0) {
            return "";
        }
        if (compat) {
            // Drop rules that use fragile child/universal selectors on older MuPDF builds.
            cleaned = cleaned.replaceAll("[^{}]*[>*][^{}]*\\{[^{}]*\\}", "");
            cleaned = cleaned.trim();
            open = 0;
            for (int i = 0; i < cleaned.length(); i++) {
                char c = cleaned.charAt(i);
                if (c == '{') {
                    open++;
                } else if (c == '}') {
                    open--;
                    if (open < 0) {
                        return "";
                    }
                }
            }
            if (open != 0) {
                return "";
            }
        }
        return cleaned;
    }

    private void appendFontFace(StringBuilder builder, String family, String fontName, String weight, String style) {
        if (!isFontFileName(fontName)) {
            return;
        }
        String url = cssUrl(getFontPath(fontName));
        if (url == null) {
            return;
        }
        builder.append("@font-face{font-family:").append(family).append(";src:url('").append(url)
                .append("') format('truetype');");
        if (weight != null) {
            builder.append("font-weight:").append(weight).append(";");
        }
        if (style != null) {
            builder.append("font-style:").append(style).append(";");
        }
        builder.append("}");
    }

    private void appendPageAndColors(StringBuilder builder) {
        String backgroundColor = MagicHelper.colorToString(MagicHelper.getBgColor());
        String textColor = MagicHelper.colorToString(MagicHelper.getTextColor());

        builder.append("@page{");
        builder.append(String.format("margin-top:%s !important;", em(marginTop + 1)));
        builder.append(String.format("margin-right:%s !important;", em(marginRight)));
        builder.append(String.format("margin-bottom:%s !important;", em(marginBottom - 1)));
        builder.append(String.format("margin-left:%s !important;", em(marginLeft)));
        builder.append("}");

        builder.append("p,div,body{");
        builder.append(String.format("background-color:%s !important;", backgroundColor));
        builder.append(String.format("color:%s !important;", textColor));
        builder.append(String.format("line-height:%s !important;", em(lineHeight)));
        builder.append("}");

        builder.append("body{padding:0 !important;margin:0 !important;}");
    }

    private void appendFb2Rules(StringBuilder builder, boolean compat) {
        builder.append(String.format("empty-line{padding-top:%s;}", em(emptyLine)));
        builder.append("epigraph{text-align:right;margin-left:2em;font-style:italic;}");
        builder.append("text-author{font-style:italic;font-weight:bold;}");
        builder.append("image{text-align:center;text-indent:0px;display:block;}");
        builder.append("subtitle{text-align:center !important;text-indent:0px !important;}");
        builder.append("title{text-align:center !important;text-indent:0px !important;}");

        if (compat) {
            // Avoid child/sibling selectors that older MuPDF CSS parsers reject.
            builder.append("title p{text-align:center !important;text-indent:0px !important;}");
        } else {
            builder.append("section>title{page-break-before:avoid;}");
            builder.append("section>title>p{text-align:center !important;text-indent:0px !important;}");
            builder.append("title>p{text-align:center !important;text-indent:0px !important;}");
            builder.append("section+section>title{page-break-before:always;}");
            builder.append("p>image{display:block;}");
        }
    }

    private void appendUserFontsAndBody(StringBuilder builder, boolean compat) {
        if (AppState.get().isDayNotInvert) {
            builder.append("a{color:").append(linkColorDay).append(" !important;}");
        } else {
            builder.append("a{color:").append(linkColorNight).append(" !important;}");
        }

        appendFontFace(builder, "my", normalFont, "normal", "normal");
        appendFontFace(builder, "my", boldFont, "bold", "normal");
        appendFontFace(builder, "my", italicFont, "normal", "italic");
        appendFontFace(builder, "my", boldItalicFont, "bold", "italic");

        if (isFontFileName(headersFont) && cssUrl(getFontPath(headersFont)) != null) {
            appendFontFace(builder, "myHeader", headersFont, null, null);
            builder.append("h1{font-size:1.50em;text-align:center;font-weight:normal;font-family:myHeader;}");
            builder.append("h2{font-size:1.30em;text-align:center;font-weight:normal;font-family:myHeader;}");
            builder.append("h3{font-size:1.15em;text-align:center;font-weight:normal;font-family:myHeader;}");
            builder.append("h4{font-size:1.00em;text-align:center;font-weight:normal;font-family:myHeader;}");
            builder.append("h5{font-size:0.80em;text-align:center;font-weight:normal;font-family:myHeader;}");
            builder.append("h6{font-size:0.60em;text-align:center;font-weight:normal;font-family:myHeader;}");
            if (compat) {
                builder.append("title{font-size:1.2em;font-weight:normal;font-family:myHeader;}");
                builder.append("subtitle{font-size:1.0em;font-weight:normal;font-family:myHeader;}");
            } else {
                builder.append("title,title>p,title>p>strong{font-size:1.2em;font-weight:normal;font-family:myHeader;}");
                builder.append("subtitle{font-size:1.0em;font-weight:normal;font-family:myHeader;}");
            }
        } else {
            String headerFamily = quoteFontFamily(headersFont);
            builder.append("h1{font-size:1.50em;text-align:center;font-weight:bold;font-family:").append(headerFamily).append(";}");
            builder.append("h2{font-size:1.30em;text-align:center;font-weight:bold;font-family:").append(headerFamily).append(";}");
            builder.append("h3{font-size:1.15em;text-align:center;font-weight:bold;font-family:").append(headerFamily).append(";}");
            builder.append("h4{font-size:1.00em;text-align:center;font-weight:bold;font-family:").append(headerFamily).append(";}");
            builder.append("h5{font-size:0.80em;text-align:center;font-weight:bold;font-family:").append(headerFamily).append(";}");
            builder.append("h6{font-size:0.60em;text-align:center;font-weight:bold;font-family:").append(headerFamily).append(";}");
            builder.append("title{font-size:1.2em;font-weight:bold;font-family:").append(headerFamily).append(";}");
            builder.append("subtitle{font-size:1.0em;font-weight:bold;font-family:").append(headerFamily).append(";}");
        }

        builder.append("h1,h2,h3,h4,h5,h6,img{text-indent:0px !important;text-align:center;}");

        builder.append("body,p{");
        if (isFontFileName(normalFont) && cssUrl(getFontPath(normalFont)) != null) {
            builder.append("font-family:my !important;");
        } else {
            builder.append("font-family:").append(quoteFontFamily(normalFont)).append(" !important;font-weight:normal;");
        }
        builder.append(String.format("text-indent:%s;", em(textIndent)));
        builder.append(String.format("text-align:%s !important;", getTextAlignConst(textAlign)));
        builder.append("}");

        if (!compat) {
            builder.append(String.format("p+p{text-indent:%s;}", em(textIndent)));
        }

        if (!isFontFileName(boldFont)) {
            builder.append("b{font-family:").append(quoteFontFamily(boldFont)).append(";font-weight:bold;}");
        }

        if (!isFontFileName(italicFont)) {
            builder.append("i{font-family:").append(quoteFontFamily(italicFont)).append(";font-style:italic;}");
        }

        builder.append("body,p,b,i,em{font-size:medium !important;}");

        String custom = sanitizeCustomCss(customCSS1, compat);
        if (!TxtUtils.isEmpty(custom)) {
            builder.append(custom);
        }
    }

    public String toCssStringCompat() {
        StringBuilder builder = new StringBuilder();
        builder.append("/* documentStyle").append(documentStyle).append(" cssProfile=compat */\n");
        builder.append("/* isAutoHypens1").append(isAutoHypens).append(hypenLang).append(" */\n");
        appendPageAndColors(builder);
        appendFb2Rules(builder, true);
        if (documentStyle == STYLES_DOC_AND_USER || documentStyle == STYLES_ONLY_USER) {
            appendUserFontsAndBody(builder, true);
        }
        return builder.toString();
    }

    public String toCssStringModern() {
        StringBuilder builder = new StringBuilder();
        builder.append("/* documentStyle").append(documentStyle).append(" cssProfile=modern */\n");
        builder.append("/* isAutoHypens1").append(isAutoHypens).append(hypenLang).append(" */\n");
        appendPageAndColors(builder);
        appendFb2Rules(builder, false);
        if (documentStyle == STYLES_DOC_AND_USER || documentStyle == STYLES_ONLY_USER) {
            appendUserFontsAndBody(builder, false);
        }
        return builder.toString();
    }

    public String toCssString() {
        boolean legacy = isLegacyCssDevice();
        String result = legacy ? toCssStringCompat() : toCssStringModern();
        LOGGER.debug("BookCSS profile={} length={}", legacy ? "compat" : "modern", result.length());
        LOGGER.debug("Injected CSS Content:\n{}", result);
        return result;
    }

    public String getFontWeight(String fontName) {
        return isFontFileName(fontName) ? "bold" : "bold";
    }

    public String getHeaderFontFamily(String fontName) {
        return isFontFileName(fontName) ? "myHeader" : fontName;
    }

    public static Typeface getTypeFaceForFont(String fontName) {
        try {
            if (FONT_NAMES.contains(fontName)) {
                return Typeface.DEFAULT;
            }
            return Typeface.createFromFile(BookCSS.get().getFontPath(fontName));
        } catch (Exception e) {
            return Typeface.DEFAULT;
        }
    }

    public static Typeface getNormalTypeFace() {
        return getTypeFaceForFont(BookCSS.get().normalFont);
    }

    public String getFontPath(String name) {
        return fontFolder + "/" + name;
    }

    public void detectLang(String bookPath) {
        FileMeta meta = null;
        if (meta != null) {
            BookCSS.get().hypenLang = meta.getLang();
        }
        if (TxtUtils.isEmpty(BookCSS.get().hypenLang)) {
            BookCSS.get().hypenLang = Urls.getLangCode();
        }
    }

}
