package br.com.ebook.foobnix.ext;

import br.com.ebook.foobnix.android.utils.LOG;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

public class PdfExtract {

    public static EbookMeta getBookMetaInformation(String unZipPath) {
        PdfContext codecContex = new PdfContext();
        CodecDocument openDocument = null;
        try {
            openDocument = codecContex.openDocument(unZipPath, "");
        } catch (RuntimeException e) {
            LOG.e(e);
            return EbookMeta.Empty();
        }
        EbookMeta meta = new EbookMeta(openDocument.getBookTitle(), openDocument.getBookAuthor());
        LOG.d("PdfExtract", meta.getAuthor(), meta.getTitle(), unZipPath);
        openDocument.recycle();
        openDocument = null;
        return meta;
    }

}
