package org.ebookdroid.droids.mupdf.codec;

import android.graphics.RectF;

import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import br.com.ebook.Config;
import br.com.ebook.foobnix.android.utils.Dips;
import br.com.ebook.foobnix.pdf.info.ExtUtils;
import br.com.ebook.foobnix.pdf.info.model.BookCSS;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;
import br.com.ebook.foobnix.sys.TempHolder;

public class MuPdfDocument extends AbstractCodecDocument {

    private static final Logger LOGGER = LoggerFactory.getLogger(MuPdfDocument.class);

    public static final int FORMAT_PDF = 0;
    private boolean isEpub = false;

    private volatile Map<String, String> footNotes;
    private volatile List<String> mediaAttachment;

    private int pagesCount = -1;
    int w, h;
    private String fname;

    public MuPdfDocument(final MuPdfContext context, final int format, final String fname, final String pwd) {
        super(context, openFile(format, fname, pwd, BookCSS.get().toCssString()));
        this.fname = fname;
        isEpub = ExtUtils.isTextFomat(fname);
    }

    @Override
    public String documentToHtml() {
        StringBuilder out = new StringBuilder();
        int pages = getPageCount();
        for (int i = 0; i < pages; i++) {
            CodecPage pageCodec = getPage(i);
            String pageHTML = pageCodec.getPageHTML();
            out.append(pageHTML);
        }
        return out.toString();
    }

    @Override
    public Map<String, String> getFootNotes() {
        return footNotes;
    }

    @Override
    public List<OutlineLink> getOutline() {
        final MuPdfOutline ou = new MuPdfOutline();
        return ou.getOutline(documentHandle);
    }

    @Override
    public CodecPage getPageInner(final int pageNumber) {
        MuPdfPage createPage = MuPdfPage.createPage(documentHandle, pageNumber + 1);
        return createPage;
    }

    @Override
    public int getPageCount() {
        if (Config.SHOW_LOG)
            LOGGER.info("MuPdfDocument, getPageCount");
        return getPageCountWithException(documentHandle, getW(), getH(), AppState.get().fontSizeSp);
    }

    @Override
    public CodecPageInfo getUnifiedPageInfo() {
        if (isEpub) {
            if (Config.SHOW_LOG)
                LOGGER.info("MuPdfDocument, getUnifiedPageInfo");
            return new CodecPageInfo(getW(), getH());
        } else {
            return null;
        }
    }

    @Override
    public int getPageCount(int w, int h, int size) {
        this.w = w;
        this.h = h;
        int pageCountWithException = getPageCountWithException(documentHandle, w, h, size);
        if (Config.SHOW_LOG)
            LOGGER.info("MuPdfDocument, getPageCount: {}x{} - {} -- count {}", w, h, size, pageCountWithException);
        return pageCountWithException;
    }

    public int getW() {
        return w > 0 ? w : Dips.screenWidth();
    }

    public int getH() {
        return h > 0 ? h : Dips.screenHeight();
    }

    @Override
    public CodecPageInfo getPageInfo(final int pageNumber) {
        final CodecPageInfo info = new CodecPageInfo();

        try {
            TempHolder.lock.lock();
            final int res = getPageInfo(documentHandle, pageNumber + 1, info);
            if (res == -1) {
                return null;
            } else {
                // Check rotation
                info.rotation = (360 + info.rotation) % 360;
                return info;
            }
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    protected void freeDocument() {
        free(documentHandle);
        cacheHandle = -1;
        if (Config.SHOW_LOG)
            LOGGER.info("MUPDF! <<< recycle [document]: {} - {}", documentHandle, ExtUtils.getFileName(fname));
    }

    static void normalizeLinkTargetRect(final long docHandle, final int targetPage, final RectF targetRect, final int flags) {

        if ((flags & 0x0F) == 0) {
            targetRect.right = targetRect.left = 0;
            targetRect.bottom = targetRect.top = 0;
            return;
        }

        final CodecPageInfo cpi = new CodecPageInfo();
        try {
            TempHolder.lock.lock();
            MuPdfDocument.getPageInfo(docHandle, targetPage, cpi);
        } finally {
            TempHolder.lock.unlock();
        }

        final float left = targetRect.left;
        final float top = targetRect.top;

        if (((cpi.rotation / 90) % 2) != 0) {
            targetRect.right = targetRect.left = left / cpi.height;
            targetRect.bottom = targetRect.top = 1.0f - top / cpi.width;
        } else {
            targetRect.right = targetRect.left = left / cpi.width;
            targetRect.bottom = targetRect.top = 1.0f - top / cpi.height;
        }
    }

    native static int getPageInfo(long docHandle, int pageNumber, CodecPageInfo cpi);

    // 'info:Title'
    // 'info:Author'
    // 'info:Subject'
    // 'info:Keywords'
    // 'info:Creator'
    // 'info:Producer'
    // 'info:CreationDate'
    // 'info:ModDate'
    private native static String getMeta(long docHandle, String option);

    @Override
    public String getMeta(String option) {
        try {
            TempHolder.lock.lock();
            return getMeta(documentHandle, option);
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public String getBookTitle() {
        return getMeta("info:Title");
    }

    @Override
    public String getBookAuthor() {
        return getMeta("info:Author");
    }

    private static long openFile(final int format, String fname, final String pwd, String css) {

        try {
            TempHolder.lock.lock();
            int allocatedMemory = AppState.get().allocatedMemorySize * 1024 * 1024;

            if (Config.SHOW_LOG)
                LOGGER.info("allocatedMemory: {} MB {}", AppState.get().allocatedMemorySize, allocatedMemory);

            final long open = open(allocatedMemory, format, fname, pwd, css, BookCSS.get().documentStyle == BookCSS.STYLES_ONLY_USER ? 0 : 1);

            if (Config.SHOW_LOG) {
                LOGGER.info("Open document " + fname + " " + open);
                LOGGER.info("Open document css: {}", css);
                LOGGER.info("MUPDF! >>> open [document]: {} - {}", open, ExtUtils.getFileName(fname));
            }

            if (open == -1)
                throw new RuntimeException("Document is corrupted");

            // final int pages = getPageCountWithException(open);
            return open;
        } finally {
            TempHolder.lock.unlock();
        }
    }

    private static native long open(int storememory, int format, String fname, String pwd, String css, int useDocStyle);

    private static native void free(long handle);

    private static synchronized int getPageCountWithException(final long handle) {
        final int count = getPageCountSafe(handle, Dips.screenWidth(), Dips.screenHeight(), Dips.spToPx(AppState.get().fontSizeSp));
        if (count == 0)
            throw new RuntimeException("Document is corrupted");

        return count;
    }

    private static synchronized int getPageCountWithException(final long handle, int w, int h, int size) {
        final int count = getPageCountSafe(handle, w, h, Dips.spToPx(size));
        if (count == 0)
            throw new RuntimeException("Document is corrupted");

        return count;
    }

    private static long cacheHandle;
    private static int cacheWH;
    private static long cacheSize;
    private static int cacheCount;

    private static int getPageCountSafe(long handle, int w, int h, int size) {
        if (Config.SHOW_LOG)
            LOGGER.info("getPageCountSafe w h size: {}x{} - {}", w, h, size);

        if (handle == cacheHandle && size == cacheSize && w + h == cacheWH) {
            if (Config.SHOW_LOG)
                LOGGER.info("getPageCount from cache: {}", cacheCount);
            return cacheCount;
        }

        try {
            TempHolder.lock.lock();
            cacheHandle = handle;
            cacheSize = size;
            cacheWH = w + h;
            cacheCount = getPageCount(handle, w, h, size);
            if (Config.SHOW_LOG)
                LOGGER.info("getPageCount put to  cache: {}", cacheCount);
            return cacheCount;
        } finally {
            TempHolder.lock.unlock();
        }
    }

    private static native int getPageCount(long handle, int w, int h, int size);

    private native void saveInternal(long handle, String path);

    private native boolean hasChangesInternal(long handle);

    @Override
    public synchronized boolean hasChanges() {
        TempHolder.lock.lock();
        try {
            return hasChangesInternal(documentHandle);
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public synchronized void saveAnnotations(String path) {
        if (Config.SHOW_LOG)
            LOGGER.info("Save Annotations saveInternal 1");
        TempHolder.lock.lock();
        try {
            saveInternal(documentHandle, path);
            if (Config.SHOW_LOG)
                LOGGER.info("Save Annotations saveInternal 2");
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public List<RectF> searchText(final int pageNuber, final String pattern) throws DocSearchNotSupported {
        throw new DocSearchNotSupported();
    }

    @Override
    public synchronized void deleteAnnotation(long pageHandle, int index) {
        TempHolder.lock.lock();
        try {
            deleteAnnotationInternal(documentHandle, pageHandle, index);
        } finally {
            TempHolder.lock.unlock();
        }
    }

    private native void deleteAnnotationInternal(long docHandle, long pageHandle, int annot_index);

    public void setFootNotes(Map<String, String> footNotes) {
        this.footNotes = footNotes;
    }

    public void setMediaAttachment(List<String> mediaAttachment) {
        this.mediaAttachment = mediaAttachment;
    }

    @Override
    public List<String> getMediaAttachments() {
        return mediaAttachment;
    }

}
