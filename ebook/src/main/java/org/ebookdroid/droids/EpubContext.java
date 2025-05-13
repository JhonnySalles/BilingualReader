package org.ebookdroid.droids;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

import br.com.ebook.Config;
import br.com.ebook.foobnix.ext.CacheZipUtils;
import br.com.ebook.foobnix.ext.EpubExtractor;
import br.com.ebook.foobnix.pdf.info.ExtUtils;
import br.com.ebook.foobnix.pdf.info.JsonHelper;
import br.com.ebook.foobnix.pdf.info.model.BookCSS;
import br.com.ebook.foobnix.sys.TempHolder;

public class EpubContext extends PdfContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(EpubContext.class);

    private static final String TAG = "EpubContext";
    File cacheFile;

    @Override
    public File getCacheFileName(String fileNameOriginal) {
        if (Config.SHOW_LOG)
            LOGGER.info(TAG + " | getCacheFileName: {} - {}", fileNameOriginal, BookCSS.get().hypenLang);
        cacheFile = new File(CacheZipUtils.CACHE_BOOK_DIR, (fileNameOriginal + BookCSS.get().isAutoHypens + BookCSS.get().hypenLang).hashCode() + ".epub");
        return cacheFile;
    }

    @Override
    public CodecDocument openDocumentInner(final String fileName, String password) {
        if (Config.SHOW_LOG)
            LOGGER.info(TAG + ": {}", fileName);

        if (BookCSS.get().isAutoHypens && !cacheFile.isFile())
            EpubExtractor.proccessHypens(fileName, cacheFile.getPath());

        if (TempHolder.get().loadingCancelled) {
            removeTempFiles();
            return null;
        }

        String bookPath = BookCSS.get().isAutoHypens ? cacheFile.getPath() : fileName;
        final MuPdfDocument muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, fileName, password);

        final File jsonFile = new File(cacheFile.getParentFile(), ExtUtils.getFileNameWithoutExt(cacheFile.getName()) + ".json");
        if (jsonFile.isFile()) {
            muPdfDocument.setFootNotes(JsonHelper.fileToMap(jsonFile));
            if (Config.SHOW_LOG)
                LOGGER.info("Load notes from file: {}", jsonFile);
        }

        new Thread() {
            @Override
            public void run() {
                Map<String, String> notes = null;
                try {
                    muPdfDocument.setMediaAttachment(EpubExtractor.getAttachments(fileName));
                    if (!jsonFile.isFile()) {
                        notes = EpubExtractor.get().getFooterNotes(fileName);
                        muPdfDocument.setFootNotes(notes);

                        JsonHelper.mapToFile(jsonFile, notes);
                        if (Config.SHOW_LOG)
                            LOGGER.info("save notes to file: {}", jsonFile);
                    }
                    removeTempFiles();
                } catch (Exception e) {
                    LOGGER.error("Error open document inner: {}", e.getMessage(), e);
                }
            };
        }.start();

        return muPdfDocument;
    }

}
