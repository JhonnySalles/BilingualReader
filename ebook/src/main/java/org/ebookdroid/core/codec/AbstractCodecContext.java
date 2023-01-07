package org.ebookdroid.core.codec;

import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfPasswordException;
import org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfPasswordRequiredException;
import org.ebookdroid.ui.viewer.VerticalViewActivity;

import br.com.ebook.foobnix.android.utils.LOG;
import br.com.ebook.foobnix.ext.CacheZipUtils;
import br.com.ebook.foobnix.ext.CacheZipUtils.CacheDir;
import br.com.ebook.foobnix.pdf.info.ExtUtils;
import br.com.ebook.foobnix.pdf.info.model.BookCSS;
import br.com.ebook.foobnix.sys.TempHolder;

import android.graphics.Bitmap;

public abstract class AbstractCodecContext implements CodecContext {

    private static final AtomicLong SEQ = new AtomicLong();

    private static Integer densityDPI;

    private long contextHandle;

    /**
     * Constructor.
     */
    protected AbstractCodecContext() {
        this(SEQ.incrementAndGet());
        TempHolder.get().loadingCancelled = false;
    }

    public abstract CodecDocument openDocumentInner(String fileName, String password);

    public CodecDocument openDocumentInnerCanceled(String fileName, String password) {
        CodecDocument openDocument = openDocumentInner(fileName, password);
        LOG.d("removeTempFiles1", TempHolder.get().loadingCancelled);
        if (TempHolder.get().loadingCancelled) {
            TempHolder.get().clear();
            removeTempFiles();
            return null;
        }
        return openDocument;
    }

    public void removeTempFiles() {
        LOG.d("removeTempFiles2", TempHolder.get().loadingCancelled);
        if (TempHolder.get().loadingCancelled) {
            recycle();
            CacheZipUtils.removeFiles(CacheZipUtils.CACHE_BOOK_DIR.listFiles());
        }
    }

    @Override
    public CodecDocument openDocument(String fileNameOriginal, String password) {
        LOG.d("Open-Document", fileNameOriginal);
        // TempHolder.get().loadingCancelled = false;
        if (ExtUtils.isZip(fileNameOriginal)) {
            LOG.d("Open-Document ZIP", fileNameOriginal);
            return openDocumentInnerCanceled(fileNameOriginal, password);
        }


        LOG.d("Open-Document 2 LANG:", BookCSS.get().hypenLang, fileNameOriginal);

        File cacheFileName = getCacheFileName(fileNameOriginal);
        CacheZipUtils.removeFiles(CacheZipUtils.CACHE_BOOK_DIR.listFiles(), cacheFileName);

        if (cacheFileName != null && cacheFileName.isFile()) {
            LOG.d("Open-Document from cache", fileNameOriginal);
            return openDocumentInnerCanceled(fileNameOriginal, password);
        }

        CacheZipUtils.cacheLock.lock();
        CacheZipUtils.createAllCacheDirs();
        try {
            String fileName = CacheZipUtils.extracIfNeed(fileNameOriginal, CacheDir.ZipApp).unZipPath;
            LOG.d("Open-Document extract", fileName);
            if (!ExtUtils.isValidFile(fileName)) {
                return null;
            }
            try {
                return openDocumentInnerCanceled(fileName, password);
            } catch (MuPdfPasswordException e) {
                throw new MuPdfPasswordRequiredException();
            } catch (Throwable e) {
                LOG.e(e);
                return null;
            }
        } finally {
            CacheZipUtils.cacheLock.unlock();
        }

    }

    public File getCacheFileName(String fileNameOriginal) {
        return null;
    }

    /**
     * Constructor.
     *
     * @param contextHandle
     *            contect handler
     */
    protected AbstractCodecContext(final long contextHandle) {
        this.contextHandle = contextHandle;
    }

    @Override
    protected final void finalize() throws Throwable {
        // recycle();
        super.finalize();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.codec.CodecContext#recycle()
     */
    @Override
    public final void recycle() {
        if (!isRecycled()) {
            freeContext();
            contextHandle = 0;
        }
    }

    protected void freeContext() {
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.codec.CodecContext#isRecycled()
     */
    @Override
    public final boolean isRecycled() {
        return contextHandle == 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.codec.CodecContext#getContextHandle()
     */
    @Override
    public final long getContextHandle() {
        return contextHandle;
    }

    @Override
    public boolean isPageSizeCacheable() {
        return true;
    }

    @Override
    public boolean isParallelPageAccessAvailable() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.codec.CodecContext#getBitmapConfig()
     */
    @Override
    public Bitmap.Config getBitmapConfig() {
        return Bitmap.Config.RGB_565;
    }

    private static int getDensityDPI() {
        if (densityDPI == null) {
            try {
                final Field f = VerticalViewActivity.DM.getClass().getDeclaredField("densityDpi");
                densityDPI = ((Integer) f.get(VerticalViewActivity.DM));
            } catch (final Throwable ex) {
                densityDPI = Integer.valueOf(120);
            }
        }
        return densityDPI.intValue();
    }
}
