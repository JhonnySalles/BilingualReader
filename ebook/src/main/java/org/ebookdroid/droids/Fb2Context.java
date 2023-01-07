package org.ebookdroid.droids;

import java.io.File;
import java.util.Map;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import br.com.ebook.foobnix.android.utils.LOG;
import br.com.ebook.foobnix.ext.CacheZipUtils;
import br.com.ebook.foobnix.ext.Fb2Extractor;
import br.com.ebook.foobnix.pdf.info.JsonHelper;
import br.com.ebook.foobnix.pdf.info.model.BookCSS;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;

public class Fb2Context extends PdfContext {

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
            Fb2Extractor.get().convert(fileName, outName);
            LOG.d("Fb2Context create", fileName, "to", outName);
        }

        LOG.d("Fb2Context open", outName);

        try {
            muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, outName, password);
        } catch (Exception e) {
            LOG.e(e);
            if (cacheFile.isFile()) {
                cacheFile.delete();
            }
            outName = cacheFile1.getPath();
            Fb2Extractor.get().convertFB2(fileName, outName);
            muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, outName, password);
            LOG.d("Fb2Context create", outName);
        }

        final File jsonFile = new File(cacheFile + ".json");
        if (jsonFile.isFile()) {
            muPdfDocument.setFootNotes(JsonHelper.fileToMap(jsonFile));
            LOG.d("Load notes from file", jsonFile);
        } else {

            new Thread() {
                @Override
                public void run() {
                    Map<String, String> notes = Fb2Extractor.get().getFooterNotes(fileName);
                    muPdfDocument.setFootNotes(notes);
                    JsonHelper.mapToFile(jsonFile, notes);
                    LOG.d("save notes to file", jsonFile);
                    removeTempFiles();
                };
            }.start();
        }


        return muPdfDocument;
    }

}
