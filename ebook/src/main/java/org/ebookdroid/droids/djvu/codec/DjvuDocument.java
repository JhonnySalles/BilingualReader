package org.ebookdroid.droids.djvu.codec;

import android.graphics.RectF;

import org.ebookdroid.common.LengthUtils;
import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.core.codec.PageTextBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import br.com.ebook.Config;
import br.com.ebook.foobnix.sys.TempHolder;

public class DjvuDocument extends AbstractCodecDocument {

    private static final Logger LOGGER = LoggerFactory.getLogger(DjvuDocument.class);

    private String fileName;

    DjvuDocument(final DjvuContext djvuContext, final String fileName) {
        super(djvuContext, open(djvuContext.getContextHandle(), fileName));
        this.fileName = fileName;
        if (Config.SHOW_LOG)
            LOGGER.info("MUPDF! open document djvu: {} - {}", documentHandle, fileName);
    }

    @Override
    public List<OutlineLink> getOutline() {
        TempHolder.lock.lock();
        try {
            final DjvuOutline ou = new DjvuOutline();
            return ou.getOutline(documentHandle);
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public String getMeta(String option) {
        return "";
    }

    @Override
    public DjvuPage getPageInner(final int pageNumber) {
        TempHolder.lock.lock();
        try {
            if (Config.SHOW_LOG)
                LOGGER.info("DjvuPage_getPage: {}", pageNumber);
            return new DjvuPage(context.getContextHandle(), documentHandle, getPage(documentHandle, pageNumber), pageNumber);
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public String documentToHtml() {
        return "";
    }

    @Override
    public int getPageCount() {
        TempHolder.lock.lock();
        try {
            return getPageCount(documentHandle);
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public int getPageCount(int w, int h, int fsize) {
        return getPageCount();
    }

    @Override
    public CodecPageInfo getPageInfo(final int pageNumber) {
        final CodecPageInfo info = new CodecPageInfo();
        final int res = getPageInfo(documentHandle, pageNumber, context.getContextHandle(), info);
        if (res == -1)
            return null;
        else
            return info;
    }

    @Override
    protected void freeDocument() {
        free(documentHandle);
        if (Config.SHOW_LOG)
            LOGGER.info("MUPDF! recycle document djvu: {} - {}", documentHandle, fileName);
    }

    private native static int getPageInfo(long docHandle, int pageNumber, long contextHandle, CodecPageInfo cpi);

    private native static long open(long contextHandle, String fileName);

    private native static long getPage(long docHandle, int pageNumber);

    private native static int getPageCount(long docHandle);

    private native static void free(long pageHandle);

    @Override
    public List<? extends RectF> searchText(final int pageNuber, final String pattern) throws DocSearchNotSupported {
        final List<PageTextBox> list = DjvuPage.getPageText(documentHandle, pageNuber, context.getContextHandle(), pattern.toLowerCase());
        if (LengthUtils.isNotEmpty(list)) {
            CodecPageInfo cpi = getPageInfo(pageNuber);
            for (final PageTextBox ptb : list)
                DjvuPage.normalizeTextBox(ptb, cpi.width, cpi.height);
        }
        return list;
    }

    @Override
    public void saveAnnotations(String path) {

    }

    @Override
    public boolean hasChanges() {
        return false;
    }

    @Override
    public void deleteAnnotation(long pageNumber, int index) {
        // TODO Auto-generated method stub
    }

    @Override
    public List<String> getMediaAttachments() {
        return null;
    }
}
