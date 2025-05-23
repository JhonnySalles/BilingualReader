package org.ebookdroid.droids.djvu.codec;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;

import org.ebookdroid.common.LengthUtils;
import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.AbstractCodecPage;
import org.ebookdroid.core.codec.Annotation;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.core.codec.PageTextBox;
import org.ebookdroid.droids.mupdf.codec.TextWord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.com.ebook.Config;
import br.com.ebook.foobnix.pdf.info.model.AnnotationType;
import br.com.ebook.foobnix.pdf.info.wrapper.MagicHelper;
import br.com.ebook.foobnix.sys.TempHolder;

public class DjvuPage extends AbstractCodecPage {

    private static final Logger LOGGER = LoggerFactory.getLogger(DjvuPage.class);
    private final long contextHandle;
    private final long docHandle;
    private final int pageNo;
    private long pageHandle;
    private int w = 0;
    private int h = 0;

    DjvuPage(final long contextHandle, final long docHandle, final long pageHandle, final int pageNo) {
        this.contextHandle = contextHandle;
        this.docHandle = docHandle;
        this.pageHandle = pageHandle;
        this.pageNo = pageNo;
    }

    @Override
    public List<Annotation> getAnnotations() {
        return new ArrayList<Annotation>();
    }

    @Override
    public int getWidth() {
        if (w == 0)
            w = getWidth(pageHandle);

        return w;
    }

    @Override
    public int getCharCount() {
        return 0;
    }

    @Override
    public int getHeight() {
        if (h == 0)
            h = getHeight(pageHandle);

        return h;
    }

    @Override
    public String getPageHTML() {
        return "";
    }

    @Override
    public String getPageHTMLWithImages() {
        return "";
    }

    @Override
    public BitmapRef renderBitmap(final int width, final int height, final RectF pageSliceBounds) {
        if (Config.SHOW_LOG)
            LOGGER.info("Render DJVU Page: {}x{} - {}", width, height, pageSliceBounds);
        final int renderMode = 0;// 0-color,1-black,2 color only, 3 mask, 4 backgroud, 5 foreground
        BitmapRef bmp = null;
        if (width > 0 && height > 0) {
            bmp = BitmapManager.getBitmap("Djvu page", width, height, Bitmap.Config.RGB_565);
            final int[] buffer = new int[width * height];
            renderPageWrapper(pageHandle, contextHandle, width, height, pageSliceBounds.left, pageSliceBounds.top, pageSliceBounds.width(), pageSliceBounds.height(), buffer, renderMode);

            if (MagicHelper.isNeedBC)
                MagicHelper.applyQuickContrastAndBrightness(buffer, width, height);

            MagicHelper.udpateColorsMagic(buffer);
            bmp.getBitmap().setPixels(buffer, 0, width, 0, 0, width, height);
            return bmp;
        }

        if (bmp == null)
            bmp = BitmapManager.getBitmap("Djvu page", 100, 100, Bitmap.Config.RGB_565);

        // final Canvas c = new Canvas(bmp.getBitmap());
        // final Paint paint = new Paint();
        // paint.setColor(Color.GRAY);
        // c.drawRect(new Rect(0, 0, width, height), paint);
        return bmp;
    }

    @Override
    public void addMarkupAnnotation(PointF[] quadPoints, AnnotationType type, float[] color) {
    }

    @Override
    public Bitmap renderThumbnail(int width) {
        return renderThumbnail(width, getWidth(), getHeight());
    }

    @Override
    public void addAnnotation(float[] color, PointF[][] points, float width, float alpha) {
        // TODO Auto-generated method stub
    }

    @Override
    public Bitmap renderThumbnail(int width, int originW, int originH) {
        if (originW < 0 || originH <= 0) {
            originW = width;
            originH = (int) (width * 1.3);
        }

        RectF rectF = new RectF(0, 0, 1f, 1f);
        float k = (float) originH / originW;
        if (Config.SHOW_LOG)
            LOGGER.info("Render! w{} H {} {} {}", originW, originH, k, width * k);
        BitmapRef renderBitmap = renderBitmap(width, (int) (width * k), rectF);
        return renderBitmap.getBitmap();
    }

    @Override
    protected void finalize() throws Throwable {
        // recycle();
        super.finalize();
    }

    @Override
    public synchronized void recycle() {
        TempHolder.lock.lock();
        try {
            if (pageHandle == 0) {
                return;
            }

            if (Config.SHOW_LOG)
                LOGGER.info("MUPDF! recycle page: {} - {}", docHandle, pageHandle);
            long p = pageHandle;
            pageHandle = 0;
            free(p);
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public synchronized boolean isRecycled() {
        return pageHandle == 0;
    }

    @Override
    public List<PageLink> getPageLinks() {
        return Collections.emptyList();
    }

    public List<PageLink> getPageLinks1() {
        TempHolder.lock.lock();
        try {
            final List<PageLink> links = getPageLinks(docHandle, pageNo);
            if (links != null) {
                final float width = getWidth();
                final float height = getHeight();
                for (final PageLink link : links) {
                    normalize(link.sourceRect, width, height);

                    if (link.url != null && link.url.startsWith("#")) {
                        try {
                            link.targetPage = Integer.parseInt(link.url.substring(1)) - 1;
                            link.url = null;
                        } catch (final NumberFormatException ex) {
                            LOGGER.error("Error open document inner: {}", ex.getMessage(), ex);
                        }
                    }
                }

                return links;
            }
        } finally {
            TempHolder.lock.unlock();
        }
        return Collections.emptyList();
    }


    public List<PageTextBox> getPageText1() {
        final List<PageTextBox> list = getPageText(docHandle, pageNo, contextHandle, null);
        if (LengthUtils.isNotEmpty(list)) {
            final float width = getWidth();
            final float height = getHeight();
            for (final PageTextBox ptb : list)
                normalizeTextBox(ptb, width, height);
        }
        return list;
    }


    static void normalize(final RectF r, final float width, final float height) {
        r.left = r.left / width;
        r.right = r.right / width;
        r.top = r.top / height;
        r.bottom = r.bottom / height;
    }

    static void normalizeTextBox(final PageTextBox r, final float width, final float height) {
        final float left = r.left / width;
        final float right = r.right / width;
        final float top = 1 - r.top / height;
        final float bottom = 1 - r.bottom / height;
        r.left = Math.min(left, right);
        r.right = Math.max(left, right);
        r.top = Math.min(top, bottom);
        r.bottom = Math.max(top, bottom);
    }

    private static synchronized native int getWidth(long pageHandle);

    private static synchronized native int getHeight(long pageHandle);

    private static native boolean isDecodingDone(long pageHandle);

    private boolean renderPageWrapper(long pageHandle, long contextHandle, int targetWidth, int targetHeight, float pageSliceX, float pageSliceY, float pageSliceWidth, float pageSliceHeight, int[] buffer,
            int renderMode) {
        try {
            TempHolder.lock.lock();
            return renderPage(pageHandle, contextHandle, targetWidth, targetHeight, pageSliceX, pageSliceY, pageSliceWidth, pageSliceHeight, buffer, renderMode);
        } finally {
            TempHolder.lock.unlock();
        }
    }

    private boolean renderPageBitmapWrapper(long pageHandle, long contextHandle, int targetWidth, int targetHeight, float pageSliceX, float pageSliceY, float pageSliceWidth, float pageSliceHeight, Bitmap bitmap,
            int renderMode) {
        try {
            TempHolder.lock.lock();
            return renderPageBitmap(pageHandle, contextHandle, targetWidth, targetHeight, pageSliceX, pageSliceY, pageSliceWidth, pageSliceHeight, bitmap, renderMode);
        } finally {
            TempHolder.lock.unlock();
        }
    }

    private static native boolean renderPage(long pageHandle, long contextHandle, int targetWidth, int targetHeight, float pageSliceX, float pageSliceY, float pageSliceWidth, float pageSliceHeight, int[] buffer,
            int renderMode);

    private static native boolean renderPageBitmap(long pageHandle, long contextHandle, int targetWidth, int targetHeight, float pageSliceX, float pageSliceY, float pageSliceWidth, float pageSliceHeight, Bitmap bitmap,
            int renderMode);

    private static native void free(long pageHandle);

    private native static ArrayList<PageLink> getPageLinks(long docHandle, int pageNo);

    native static List<PageTextBox> getPageText(long docHandle, int pageNo, long contextHandle, String pattern);

    @Override
    public TextWord[][] getText() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getPageHandle() {
        return 0;
    }

}
