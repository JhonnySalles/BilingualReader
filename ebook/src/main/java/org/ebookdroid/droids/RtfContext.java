package org.ebookdroid.droids;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import br.com.ebook.foobnix.ext.CacheZipUtils;
import br.com.ebook.foobnix.ext.RtfExtract;
import br.com.ebook.foobnix.pdf.info.model.BookCSS;

public class RtfContext extends PdfContext {

	private static final Logger LOGGER = LoggerFactory.getLogger(RtfContext.class);

	File cacheFile;

	@Override
	public File getCacheFileName(String fileNameOriginal) {
        fileNameOriginal = fileNameOriginal + BookCSS.get().isAutoHypens + BookCSS.get().hypenLang;
		cacheFile = new File(CacheZipUtils.CACHE_BOOK_DIR, fileNameOriginal.hashCode() + ".html");
		return cacheFile;
	}

	@Override
	public CodecDocument openDocumentInner(String fileName, String password) {
		if (!cacheFile.isFile()) {
			try {
				RtfExtract.extract(fileName, CacheZipUtils.CACHE_BOOK_DIR.getPath(), cacheFile.getName());
			} catch (Exception e) {
				LOGGER.error("Error open document inner: {}", e.getMessage(), e);
			}
		}

		MuPdfDocument muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, cacheFile.getPath(), password);
		return muPdfDocument;
	}
}
