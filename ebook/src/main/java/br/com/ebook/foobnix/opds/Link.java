package br.com.ebook.foobnix.opds;

import br.com.ebook.foobnix.pdf.info.ExtUtils;

import java.util.HashMap;
import java.util.Map;

public class Link {

    public static final String APPLICATION_ATOM_XML = "application/atom+xml";
    public static final String WEB_LINK = "text/html";
    public static final String DISABLED = "disabled/";
    public static final String TYPE_LOGO = "MY_LOGO";
    public String href;
    public String type = "";
    public String rel;
    public String title;

    public String filePath;

    static Map<String, String> map = new HashMap<String, String>();

    {
        map.put("text/html", "web");
        map.put("text/download", "txt");
        map.put("application/rtf", "rtf");
        map.put("application/msword", "doc");
        map.put("application/doc", "doc");
        map.put("application/docx", "docx");
        map.put("application/pdf", "pdf");
        map.put("application/pdb", "pdb");
        map.put("application/djvu", "djvu");
        map.put("application/epub+zip", "epub");
        map.put("application/fb-ebook", "fb2");
        map.put("application/fb2+xml", "fb2");
        map.put("application/fb-ebook+zip", "fb2.zip");
        map.put("application/x-sony-bbeb", "lrf");
        map.put("application/x-mobipocket-ebook", "mobi");
    }

    public Link(String href) {
        type = APPLICATION_ATOM_XML;
        this.href = href;
    }

    public Link(String href, String type) {
        this.type = type;
        this.href = href;
    }

    public Link(String href, String type, String title) {
        this.type = type;
        this.href = href;
        this.title = title;
    }

    public boolean isThumbnail() {
        return rel != null && rel.contains("thumbnail") && ExtUtils.isImageMime(type);
    }

    public boolean isSearchLink() {
        return "search".equals(rel) && APPLICATION_ATOM_XML.equals(type);
    }

    public boolean isDisabled() {
        return type.startsWith(DISABLED) || type.equals("image/");
    }

    public boolean isImageLink() {
        return ExtUtils.isImageMime(type);
    }

    public boolean isWebLink() {
        return WEB_LINK.equals(type);
    }

    public String getDownloadDisplayFormat() {
        if (type == null) {
            return null;
        }
        for (String item : map.keySet()) {
            if (item.equals(type)) {
                return map.get(item);
            }
        }

        if (type.contains("+zip") || type.contains("+rar")) {
            return type.replace("application/", "").replace("+", ".");
        }

        return null;

    }

}
