package br.com.ebook.foobnix.ext;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.ebook.Config;

public class PdfExtract {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfExtract.class);

    public static EbookMeta getBookMetaInformation(String unZipPath) {
        PdfContext codecContex = new PdfContext();
        CodecDocument openDocument = null;
        try {
            openDocument = codecContex.openDocument(unZipPath, "");
        } catch (RuntimeException e) {
            LOGGER.error("Error get meta information: {}", e.getMessage(), e);
            return EbookMeta.Empty();
        }
        EbookMeta meta = new EbookMeta(openDocument.getBookTitle(), openDocument.getBookAuthor());
        if (Config.SHOW_LOG)
            LOGGER.info("PdfExtract: {} - {} - {}", meta.getAuthor(), meta.getTitle(), unZipPath);
        openDocument.recycle();
        openDocument = null;
        return meta;
    }

}
