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
import br.com.ebook.foobnix.ext.FooterNote;
import br.com.ebook.foobnix.ext.MobiExtract;
import br.com.ebook.foobnix.pdf.info.JsonHelper;
import br.com.ebook.foobnix.pdf.info.model.BookCSS;

public class MobiContext extends PdfContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(MobiContext.class);

    String fileNameEpub = null;

    public int originalHashCode;
    File cacheFile;

    @Override
    public File getCacheFileName(String fileName) {
        originalHashCode = (fileName + BookCSS.get().isAutoHypens + BookCSS.get().hypenLang).hashCode();
        cacheFile = new File(CacheZipUtils.CACHE_BOOK_DIR, originalHashCode + "" + originalHashCode + ".epub");
        return cacheFile;
    }

    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {
        if (Config.SHOW_LOG)
            LOGGER.info("Context: MobiContext - {}", fileName);

        try {

            if (cacheFile.isFile()) {
                fileNameEpub = cacheFile.getPath();
            } else {
                try {
                    int outName = BookCSS.get().isAutoHypens ? "temp".hashCode() : originalHashCode;
                    FooterNote extract = MobiExtract.extract(fileName, CacheZipUtils.CACHE_BOOK_DIR.getPath(), outName + "");
                    fileNameEpub = extract.path;
                    if (BookCSS.get().isAutoHypens) {

                        EpubExtractor.proccessHypens(fileNameEpub, cacheFile.getPath());
                        fileNameEpub = cacheFile.getPath();
                    }
                    if (Config.SHOW_LOG)
                        LOGGER.info("new file name: {}", fileName);
                } catch (Exception e) {
                    LOGGER.error("Error open document inner: {}", e.getMessage(), e);
                }
            }

            final MuPdfDocument muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, fileNameEpub, password);

            final File jsonFile = new File(cacheFile + ".json");
            if (jsonFile.isFile()) {
                muPdfDocument.setFootNotes(JsonHelper.fileToMap(jsonFile));
                if (Config.SHOW_LOG)
                    LOGGER.info("Load notes from file: {}", jsonFile);
            } else {

                new Thread() {
                    @Override
                    public void run() {
                        Map<String, String> notes = null;
                        try {
                            notes = EpubExtractor.get().getFooterNotes(fileNameEpub);
                            if (Config.SHOW_LOG)
                                LOGGER.info("new file name: {}", fileNameEpub);
                            muPdfDocument.setFootNotes(notes);

                            JsonHelper.mapToFile(jsonFile, notes);
                            if (Config.SHOW_LOG)
                                LOGGER.info("save notes to file: {}", jsonFile);

                            removeTempFiles();

                        } catch (Exception e) {
                            LOGGER.error("Error open document inner: {}", e.getMessage(), e);
                        }
                    }

                    ;
                }.start();
            }

            return muPdfDocument;
        } catch (Exception e) {
            if (cacheFile.exists())
                cacheFile.delete();

            throw e;
        }
    }

}
