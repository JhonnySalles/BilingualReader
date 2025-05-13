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
import br.com.ebook.foobnix.ext.Fb2Extractor;
import br.com.ebook.foobnix.pdf.info.ExtUtils;
import br.com.ebook.foobnix.pdf.info.JsonHelper;
import br.com.ebook.foobnix.pdf.info.model.BookCSS;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;

public class Fb2Context extends PdfContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(Fb2Context.class);

    File cacheFile, cacheFile1;

    @Override
    public File getCacheFileName(String fileNameOriginal) {
        fileNameOriginal = fileNameOriginal + BookCSS.get().isAutoHypens + BookCSS.get().hypenLang + AppState.get().isDouble;
        cacheFile = new File(CacheZipUtils.CACHE_BOOK_DIR, fileNameOriginal.hashCode() + ".epub");
        cacheFile1 = new File(CacheZipUtils.CACHE_BOOK_DIR, fileNameOriginal.hashCode() + ".epub.fb2");
        return cacheFile;
    }

    MuPdfDocument muPdfDocument;

    @Override
    public CodecDocument openDocumentInner(final String fileName, String password) {
        String outName = null;
        if (cacheFile.isFile()) {
            outName = cacheFile.getPath();
        } else if (cacheFile1.isFile()) {
            outName = cacheFile1.getPath();
        }

        if (outName == null) {
            outName = cacheFile.getPath();
            String file = ExtUtils.validNameFileCharacter(fileName, cacheFile.getParent(), "fb2_temp_" + fileName.hashCode());
            if (!Fb2Extractor.get().convert(file, outName))
                throw new RuntimeException("FB2 not converted");

            if (Config.SHOW_LOG)
                LOGGER.info("Fb2Context create: {} to {}", fileName, outName);
        }

        if (Config.SHOW_LOG)
            LOGGER.info("Fb2Context open: {}", outName);

        try {
            muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, outName, password);
        } catch (Exception e) {
            LOGGER.error("Error open document inner: {}", e.getMessage(), e);
            if (cacheFile.isFile())
                cacheFile.delete();

            outName = cacheFile1.getPath();
            String file = ExtUtils.validNameFileCharacter(fileName, cacheFile1.getParent(), "fb2_temp_" + fileName.hashCode());

            if (!Fb2Extractor.get().convertFB2(file, outName))
                throw new RuntimeException("FB2 not converted");

            muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, outName, password);
            if (Config.SHOW_LOG)
                LOGGER.info("Fb2Context create: {}", outName);
        }

        final File jsonFile = new File(cacheFile.getParentFile(), ExtUtils.getFileNameWithoutExt(cacheFile.getName()) + ".json");
        if (jsonFile.isFile()) {
            muPdfDocument.setFootNotes(JsonHelper.fileToMap(jsonFile));
            if (Config.SHOW_LOG)
                LOGGER.info("Load notes from file: {}", jsonFile);
        } else {

            new Thread() {
                @Override
                public void run() {
                    Map<String, String> notes = Fb2Extractor.get().getFooterNotes(fileName);
                    muPdfDocument.setFootNotes(notes);
                    JsonHelper.mapToFile(jsonFile, notes);
                    if (Config.SHOW_LOG)
                        LOGGER.info("save notes to file: {}", jsonFile);
                    removeTempFiles();
                };
            }.start();
        }

        return muPdfDocument;
    }

}
