package org.ebookdroid.droids.mupdf.codec;

import org.ebookdroid.core.codec.OutlineLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import br.com.ebook.extractor.Fb2BookExtractor;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;
import br.com.ebook.foobnix.sys.TempHolder;

public class MuPdfOutline {

    private static final Logger LOGGER = LoggerFactory.getLogger(MuPdfOutline.class);

    private static final float[] temp = new float[4];

    private long docHandle;

    public List<OutlineLink> getOutline(final long dochandle) {
        final List<OutlineLink> ls = new ArrayList<OutlineLink>();
        docHandle = dochandle;
        try {
            TempHolder.lock.lock();
            final long outline = open(dochandle);
            ttOutline(ls, outline, 0);
            free(dochandle);

            ls.add(new OutlineLink("", "", -1));
        } finally {
            TempHolder.lock.unlock();
        }
        return ls;
    }

    private String getTitleString(long dochandle, long outline) {
        try {
            byte[] titleArray = getTitleArray(dochandle, outline);
            if (titleArray != null && titleArray.length > 0) {
                return new String(titleArray, java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Throwable ignored) {
        }
        try {
            return getTitle(dochandle, outline);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private void ttOutline(final List<OutlineLink> ls, long outline, final int level) {
        while (outline != -1 && outline != 0) {
            final String title = getTitleString(docHandle, outline);
            String link = null;
            try {
                String uri = getLinkUri(outline, docHandle);
                if (uri != null && !uri.trim().isEmpty()) {
                    uri = uri.trim();
                    if (uri.startsWith("#") && uri.matches("#\\d+")) {
                        link = uri;
                    } else {
                        int targetPage = -1;
                        try {
                            targetPage = MuPdfLinks.getLinkPage(docHandle, uri);
                        } catch (Throwable ignored) {
                        }
                        if (targetPage < 0 && !uri.startsWith("OEBPS/")) {
                            try {
                                targetPage = MuPdfLinks.getLinkPage(docHandle, "OEBPS/" + uri);
                            } catch (Throwable ignored) {
                            }
                        }
                        if (targetPage < 0 && uri.startsWith("OEBPS/")) {
                            try {
                                targetPage = MuPdfLinks.getLinkPage(docHandle, uri.substring(6));
                            } catch (Throwable ignored) {
                            }
                        }
                        if (targetPage < 0 && uri.contains("#")) {
                            String anchor = uri.substring(uri.indexOf('#'));
                            try {
                                targetPage = MuPdfLinks.getLinkPage(docHandle, anchor);
                            } catch (Throwable ignored) {
                            }
                        }
                        if (targetPage >= 0) {
                            link = "#" + (targetPage + 1);
                        } else if (uri.contains("#")) {
                            String base = uri.substring(0, uri.indexOf('#'));
                            if (!base.isEmpty()) {
                                int basePage = -1;
                                try {
                                    basePage = MuPdfLinks.getLinkPage(docHandle, base);
                                } catch (Throwable ignored) {
                                }
                                if (basePage < 0 && !base.startsWith("OEBPS/")) {
                                    try {
                                        basePage = MuPdfLinks.getLinkPage(docHandle, "OEBPS/" + base);
                                    } catch (Throwable ignored) {
                                    }
                                }
                                if (basePage >= 0) {
                                    link = "#" + (basePage + 1);
                                }
                            }
                        }
                        if (link == null) {
                            link = uri;
                        }
                    }
                }
            } catch (Throwable t) {
                LOGGER.warn("Error getting link URI: {}", t.getMessage());
            }

            if (link == null || link.equals("#0")) {
                try {
                    String fallback = getLink(outline, docHandle);
                    if (fallback != null && !fallback.equals("#0")) {
                        link = fallback;
                    }
                } catch (Throwable ignored) {
                }
            }

            if (link == null) {
                link = "#0";
            }

            if (title != null) {
                final OutlineLink outlineLink = new OutlineLink(title, link, level);

                boolean toAdd = true;
                if (AppState.get().outlineMode == AppState.OUTLINE_ONLY_HEADERS) {
                    if (outlineLink.getTitle().contains("[subtitle]")) {
                        toAdd = false;
                    }
                }
                outlineLink.setTitle(outlineLink.getTitle().replace("[title]", "").replace("[subtitle]", ""));

                if (outlineLink.getTitle().contains(Fb2BookExtractor.DIVIDER)) {
                    try {
                        String[] split = outlineLink.getTitle().split(Fb2BookExtractor.DIVIDER);
                        int level2 = Integer.parseInt(split[0]);
                        outlineLink.setLevel(level2);
                        outlineLink.setTitle(split[1]);
                    } catch (Exception e) {
                        LOGGER.error("Error ttoutline: {}", e.getMessage(), e);
                    }
                }

                if (toAdd)
                    ls.add(outlineLink);
            }

            final long child = getChild(outline);
            ttOutline(ls, child, level + 1);

            outline = getNext(outline);
        }
    }

    private static native String getTitle(long dochandle, long outlinehandle);

    private static native byte[] getTitleArray(long dochandle, long outlinehandle);

    private static native String getLink(long outlinehandle, long dochandle);

    private static native String getLinkUri(long outlinehandle, long dochandle);

    private static native int fillLinkTargetPoint(long outlinehandle, float[] point);

    private static native long getNext(long outlinehandle);

    private static native long getChild(long outlinehandle);

    private static native long open(long dochandle);

    private static native void free(long dochandle);
}
