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

import br.com.ebook.foobnix.android.utils.Dips;
import br.com.ebook.foobnix.ext.CacheZipUtils;
import br.com.ebook.foobnix.pdf.info.ExtUtils;
import br.com.ebook.foobnix.pdf.info.model.BookCSS;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;
import br.com.ebook.foobnix.sys.TempHolder;

import java.io.File;

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

    private String originalFname;

    public void setOriginalFname(String originalFname) {
        this.originalFname = originalFname;
    }

    public String getOriginalFname() {
        return originalFname != null ? originalFname : fname;
    }

    @Override
    public List<OutlineLink> getOutline() {
        final MuPdfOutline ou = new MuPdfOutline();
        List<OutlineLink> list = ou.getOutline(documentHandle);
        boolean hasValid = false;
        if (list != null) {
            for (OutlineLink l : list) {
                if (l != null && l.getTitle() != null && !l.getTitle().trim().isEmpty()) {
                    hasValid = true;
                    break;
                }
            }
        }
        String bookPath = getOriginalFname();
        if (!hasValid && bookPath != null) {
            File f = new File(bookPath);
            if (f.isFile()) {
                String nameLow = f.getName().toLowerCase(java.util.Locale.getDefault());
                if (nameLow.endsWith(".epub") || nameLow.endsWith(".kepub")) {
                    List<OutlineLink> tocList = br.com.ebook.extractor.EpubBookExtractor.INSTANCE.extractTocOutline(bookPath);
                    if (!tocList.isEmpty()) {
                        for (OutlineLink l : tocList) {
                            if (l.getLink() != null && !l.getLink().matches("#\\d+")) {
                                try {
                                    int p = MuPdfLinks.getLinkPage(documentHandle, l.getLink());
                                    if (p < 0 && l.getLink().startsWith("OEBPS/")) {
                                        p = MuPdfLinks.getLinkPage(documentHandle, l.getLink().substring(6));
                                    }
                                    if (p < 0 && !l.getLink().startsWith("OEBPS/")) {
                                        p = MuPdfLinks.getLinkPage(documentHandle, "OEBPS/" + l.getLink());
                                    }
                                    if (p < 0 && l.getLink().contains("#")) {
                                        String base = l.getLink().substring(0, l.getLink().indexOf('#'));
                                        if (!base.isEmpty()) {
                                            int bp = MuPdfLinks.getLinkPage(documentHandle, base);
                                            if (bp < 0 && base.startsWith("OEBPS/")) {
                                                bp = MuPdfLinks.getLinkPage(documentHandle, base.substring(6));
                                            }
                                            if (bp < 0 && !base.startsWith("OEBPS/")) {
                                                bp = MuPdfLinks.getLinkPage(documentHandle, "OEBPS/" + base);
                                            }
                                            if (bp >= 0) {
                                                p = bp;
                                            }
                                        }
                                    }
                                    if (p >= 0) {
                                        l.setLink("#" + (p + 1));
                                    }
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                        tocList.add(new OutlineLink("", "", -1));
                        return tocList;
                    }
                } else if (nameLow.endsWith(".fb2")) {
                    List<OutlineLink> fb2List = br.com.ebook.extractor.Fb2BookExtractor.INSTANCE.extractOutline(bookPath);
                    if (!fb2List.isEmpty()) {
                        for (OutlineLink l : fb2List) {
                            if (l.getLink() != null && !l.getLink().matches("#\\d+")) {
                                try {
                                    int p = MuPdfLinks.getLinkPage(documentHandle, l.getLink());
                                    if (p >= 0) {
                                        l.setLink("#" + (p + 1));
                                    }
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                        fb2List.add(new OutlineLink("", "", -1));
                        return fb2List;
                    }
                } else if (nameLow.endsWith(".docx")) {
                    List<OutlineLink> docxList = br.com.ebook.extractor.DocxBookExtractor.INSTANCE.extractOutline(bookPath);
                    if (!docxList.isEmpty()) {
                        for (OutlineLink l : docxList) {
                            if (l.getLink() != null && !l.getLink().matches("#\\d+")) {
                                try {
                                    int p = MuPdfLinks.getLinkPage(documentHandle, l.getLink());
                                    if (p >= 0) {
                                        l.setLink("#" + (p + 1));
                                    }
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                        docxList.add(new OutlineLink("", "", -1));
                        return docxList;
                    }
                } else if (nameLow.endsWith(".txtz")) {
                    List<OutlineLink> txtzList = br.com.ebook.extractor.TxtzBookExtractor.INSTANCE.extractOutline(bookPath);
                    if (!txtzList.isEmpty()) {
                        for (OutlineLink l : txtzList) {
                            if (l.getLink() != null && !l.getLink().matches("#\\d+")) {
                                try {
                                    int p = MuPdfLinks.getLinkPage(documentHandle, l.getLink());
                                    if (p >= 0) {
                                        l.setLink("#" + (p + 1));
                                    }
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                        txtzList.add(new OutlineLink("", "", -1));
                        return txtzList;
                    }
                } else if (nameLow.endsWith(".rb")) {
                    List<OutlineLink> rbList = br.com.ebook.extractor.RbBookExtractor.INSTANCE.extractOutline(bookPath);
                    if (!rbList.isEmpty()) {
                        for (OutlineLink l : rbList) {
                            if (l.getLink() != null && !l.getLink().matches("#\\d+")) {
                                try {
                                    int p = MuPdfLinks.getLinkPage(documentHandle, l.getLink());
                                    if (p >= 0) {
                                        l.setLink("#" + (p + 1));
                                    }
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                        rbList.add(new OutlineLink("", "", -1));
                        return rbList;
                    }
                } else if (nameLow.endsWith(".snb")) {
                    List<OutlineLink> snbList = br.com.ebook.extractor.SnbBookExtractor.INSTANCE.extractOutline(bookPath);
                    if (!snbList.isEmpty()) {
                        for (OutlineLink l : snbList) {
                            if (l.getLink() != null && !l.getLink().matches("#\\d+")) {
                                try {
                                    int p = MuPdfLinks.getLinkPage(documentHandle, l.getLink());
                                    if (p >= 0) {
                                        l.setLink("#" + (p + 1));
                                    }
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                        snbList.add(new OutlineLink("", "", -1));
                        return snbList;
                    }
                } else if (nameLow.endsWith(".htmlz")) {
                    List<OutlineLink> htmlzList = br.com.ebook.extractor.HtmlzBookExtractor.INSTANCE.extractOutline(bookPath);
                    if (!htmlzList.isEmpty()) {
                        for (OutlineLink l : htmlzList) {
                            if (l.getLink() != null && !l.getLink().matches("#\\d+")) {
                                try {
                                    int p = MuPdfLinks.getLinkPage(documentHandle, l.getLink());
                                    if (p >= 0) {
                                        l.setLink("#" + (p + 1));
                                    }
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                        htmlzList.add(new OutlineLink("", "", -1));
                        return htmlzList;
                    }
                }
            }
        }
        return list;
    }

    @Override
    public CodecPage getPageInner(final int pageNumber) {
        try {
            TempHolder.lock.lock();
            if (isRecycled()) return null;
            MuPdfPage createPage = MuPdfPage.createPage(documentHandle, pageNumber + 1);
            return createPage;
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public int getPageCount() {
        try {
            TempHolder.lock.lock();
            if (isRecycled()) return 0;
            return getPageCountWithException(documentHandle, getW(), getH(), AppState.get().fontSizeSp);
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public CodecPageInfo getUnifiedPageInfo() {
        if (isEpub) {
            
            return new CodecPageInfo(getW(), getH());
        } else {
            return null;
        }
    }

    @Override
    public int getPageCount(int w, int h, int size) {
        this.w = w;
        this.h = h;
        try {
            TempHolder.lock.lock();
            if (isRecycled()) return 0;
            int pageCountWithException = getPageCountWithException(documentHandle, w, h, size);
            return pageCountWithException;
        } finally {
            TempHolder.lock.unlock();
        }
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
            if (isRecycled()) return null;
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

    private static final java.util.Set<Long> recycledDocHandles = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<Long, Boolean>());

    public static boolean isDocHandleRecycled(long handle) {
        return handle == 0 || recycledDocHandles.contains(handle);
    }

    @Override
    protected void freeDocument() {
        if (documentHandle != 0) {
            recycledDocHandles.add(documentHandle);
            free(documentHandle);
        }
        cacheHandle = -1;
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
            if (isRecycled()) return null;
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
            long maxAlloc = Runtime.getRuntime().maxMemory() / 4;
            int allocatedMemory = (int) Math.min((long) AppState.get().allocatedMemorySize * 1024 * 1024, Math.max(16 * 1024 * 1024, maxAlloc));

            int useDocStyle = BookCSS.get().documentStyle == BookCSS.STYLES_ONLY_USER ? 0 : 1;
            float imageScale = AppState.get().currentBookImageScale > 0 ? AppState.get().currentBookImageScale : AppState.get().defaultImageScale;
            if (imageScale <= 0f) {
                imageScale = 1.0f;
            }
            int antiAlias = AppState.get().antiAliasPdf;
            String accel = "";
            try {
                File fileObj = new File(fname);
                File parent = fileObj.getParentFile();
                if (parent != null && parent.canWrite() && !parent.equals(CacheZipUtils.CACHE_BOOK_DIR)) {
                    File accelFile = new File(parent, fileObj.getName() + ".accel");
                    accel = accelFile.getAbsolutePath();
                } else {
                    File accelDir = CacheZipUtils.CACHE_ACCEL_DIR != null ? CacheZipUtils.CACHE_ACCEL_DIR : CacheZipUtils.CACHE_BOOK_DIR;
                    if (accelDir == null || (!accelDir.exists() && !accelDir.mkdirs())) {
                        accelDir = (parent != null && parent.canWrite()) ? parent : new File("/data/local/tmp");
                    }
                    if (accelDir.exists() || accelDir.mkdirs()) {
                        String prefix = (parent != null && !parent.getName().isEmpty()) ? parent.getName() + "_" : "";
                        String bookFileName = prefix + fileObj.getName();
                        File accelFile = new File(accelDir, bookFileName + ".accel");
                        accel = accelFile.getAbsolutePath();
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Could not determine accel path for {}: {}", fname, e.getMessage());
            }

            int isImageScale = (imageScale != 1.0f) ? 1 : 0;

            final long open = open(allocatedMemory, format, fname, pwd != null ? pwd : "", css != null ? css : "", useDocStyle, imageScale, antiAlias, accel, isImageScale);

            if (open == 0)
                throw new RuntimeException("Document is corrupted or could not be opened: " + fname);

            recycledDocHandles.remove(open);

            // final int pages = getPageCountWithException(open);
            return open;
        } finally {
            TempHolder.lock.unlock();
        }
    }

    private static native long open(int storememory, int format, String fname, String pwd, String css, int useDocStyle,
                                    float scale, int antialias, String accel, int isImageScale);

    private static native void free(long handle);

    private synchronized int getPageCountWithException(final long handle) {
        if (handle == 0 || isRecycled()) return 0;
        final int count = getPageCountSafe(handle, Dips.screenWidth(), Dips.screenHeight(), Dips.spToPx(AppState.get().fontSizeSp));
        return Math.max(1, count);
    }

    private synchronized int getPageCountWithException(final long handle, int w, int h, int size) {
        if (handle == 0 || isRecycled()) return 0;
        final int count = getPageCountSafe(handle, w, h, Dips.spToPx(size));
        return Math.max(1, count);
    }

    private static long cacheHandle;
    private static int cacheWH;
    private static long cacheSize;
    private static int cacheCount;

    private int getPageCountSafe(long handle, int w, int h, int size) {
        if (handle == 0 || isRecycled()) return 0;

        if (handle == cacheHandle && size == cacheSize && w + h == cacheWH) {
            
            return cacheCount;
        }

        try {
            TempHolder.lock.lock();
            if (handle == 0 || isRecycled()) return 0;
            cacheHandle = handle;
            cacheSize = size;
            cacheWH = w + h;
            cacheCount = getPageCount(handle, w, h, size);
            
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
        
        TempHolder.lock.lock();
        try {
            saveInternal(documentHandle, path);
            
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
