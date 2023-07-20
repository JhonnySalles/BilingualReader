package br.com.ebook.foobnix.ext;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import br.com.ebook.Config;
import br.com.ebook.foobnix.android.utils.LOG;

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
        if (Config.SHOW_LOG)
            LOG.d("PdfExtract", meta.getAuthor(), meta.getTitle(), unZipPath);
        openDocument.recycle();
        openDocument = null;
        return meta;
    }

}
