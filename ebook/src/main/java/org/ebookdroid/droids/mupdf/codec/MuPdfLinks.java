package org.ebookdroid.droids.mupdf.codec;

import android.graphics.RectF;

import org.ebookdroid.core.codec.PageLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import br.com.ebook.Config;

public class MuPdfLinks {

    private static final Logger LOGGER = LoggerFactory.getLogger(MuPdfLinks.class);

    // private static final int FZ_LINK_NONE = 0;
    // private static final int FZ_LINK_GOTO = 1;
    // private static final int FZ_LINK_URI = 2;
    // private static final int FZ_LINK_LAUNCH = 3;
    // private static final int FZ_LINK_NAMED = 4;
    // private static final int FZ_LINK_GOTOR = 5;

    private static final float[] temp = new float[4];

    static List<PageLink> getPageLinks(final long docHandle, final long pageHandle, final RectF pageBounds) {
        final List<PageLink> links = new ArrayList<PageLink>();

        for (long linkHandle = getFirstPageLink(docHandle, pageHandle); linkHandle != 0; linkHandle = getNextPageLink(linkHandle)) {

            final PageLink link = new PageLink();
            if (Config.SHOW_LOG)
                LOGGER.info("LINK GET: {} - {}", docHandle, linkHandle);
            final int type = getPageLinkType(docHandle, linkHandle);
            if (type == 1) {// external
                link.url = getPageLinkUrl(linkHandle);
                if (fillPageLinkSourceRect(linkHandle, temp)) {
                    link.sourceRect = new RectF();
                    link.sourceRect.left = (temp[0] - pageBounds.left) / pageBounds.width();
                    link.sourceRect.top = (temp[1] - pageBounds.top) / pageBounds.height();
                    link.sourceRect.right = (temp[2] - pageBounds.left) / pageBounds.width();
                    link.sourceRect.bottom = (temp[3] - pageBounds.top) / pageBounds.height();
                }
                links.add(link);
            } else if (type == 0) {// internal
                if (fillPageLinkSourceRect(linkHandle, temp)) {
                    link.sourceRect = new RectF();
                    link.sourceRect.left = (temp[0] - pageBounds.left) / pageBounds.width();
                    link.sourceRect.top = (temp[1] - pageBounds.top) / pageBounds.height();
                    link.sourceRect.right = (temp[2] - pageBounds.left) / pageBounds.width();
                    link.sourceRect.bottom = (temp[3] - pageBounds.top) / pageBounds.height();
                }

                try {
                    link.targetPage = getPageLinkTargetPage(docHandle, linkHandle);
                    // link.targetPage--;
                } catch (Exception e) {
                    link.targetPage = -1;
                }

                links.add(link);
            }

            if (Config.SHOW_LOG)
                LOGGER.info("LINK DROP: {} - {}", docHandle, linkHandle);
            // dropLink(docHandle, linkHandle);
        }
        return links;
    }

    private static native void dropLink(long dochandle, long linkhandle);

    private static native long getFirstPageLink(long dochandle, long pagehandle);

    private static native long getNextPageLink(long linkhandle);

    private static native int getPageLinkType(long dochandle, long linkhandle);

    private static native String getPageLinkUrl(long linkhandle);

    private static native boolean fillPageLinkSourceRect(long linkhandle, float[] bounds);

    private static native int getPageLinkTargetPage(long dochandle, long linkhandle);

    private static native int fillPageLinkTargetPoint(long linkhandle, float[] point);

}
