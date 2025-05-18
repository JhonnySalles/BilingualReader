package org.ebookdroid.droids;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import br.com.ebook.Config;
import br.com.ebook.foobnix.ext.CacheZipUtils;
import br.com.ebook.foobnix.ext.FooterNote;
import br.com.ebook.foobnix.ext.HtmlExtractor;

public class HtmlContext extends PdfContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlContext.class);

    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {

        Map<String, String> notes = null;
        try {
            FooterNote extract = HtmlExtractor.extract(fileName, CacheZipUtils.CACHE_BOOK_DIR.getPath());
            fileName = extract.path;
            notes = extract.notes;
            if (Config.SHOW_LOG)
                LOGGER.info("new file name: {}", fileName);
        } catch (Exception e) {
            LOGGER.error("Error open document inner: {}", e.getMessage(), e);
        }

        MuPdfDocument muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, fileName, password);
        muPdfDocument.setFootNotes(notes);
        return muPdfDocument;
    }

}
