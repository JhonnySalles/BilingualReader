package org.ebookdroid.droids;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import java.util.Map;

import br.com.ebook.foobnix.android.utils.LOG;
import br.com.ebook.foobnix.ext.CacheZipUtils;
import br.com.ebook.foobnix.ext.FooterNote;
import br.com.ebook.foobnix.ext.HtmlExtractor;

public class HtmlContext extends PdfContext {

    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {

        Map<String, String> notes = null;
        try {
            FooterNote extract = HtmlExtractor.extract(fileName, CacheZipUtils.CACHE_BOOK_DIR.getPath());
            fileName = extract.path;
            notes = extract.notes;
            LOG.d("new file name", fileName);
        } catch (Exception e) {
            LOG.e(e);
        }

        MuPdfDocument muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, fileName, password);
        muPdfDocument.setFootNotes(notes);
        return muPdfDocument;
    }

}
