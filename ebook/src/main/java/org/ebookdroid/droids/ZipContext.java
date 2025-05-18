package org.ebookdroid.droids;

import androidx.core.util.Pair;

import org.ebookdroid.BookType;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import br.com.ebook.Config;
import br.com.ebook.foobnix.ext.CacheZipUtils;
import br.com.ebook.foobnix.ext.CacheZipUtils.CacheDir;

public class ZipContext extends PdfContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipContext.class);

    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {
        if (Config.SHOW_LOG)
            LOGGER.info("ZipContext begin: {}", fileName);

        Pair<Boolean, String> pack = CacheZipUtils.isSingleAndSupportEntryFile(new File(fileName));
        if (pack.first) {
            if (Config.SHOW_LOG)
                LOGGER.info("ZipContext: Singe archive entry");
            Fb2Context fb2Context = new Fb2Context();
            String etryPath = pack.second;
            File cacheFileName = fb2Context.getCacheFileName(new File(CacheDir.ZipApp.getDir(), etryPath).getPath());
            if (Config.SHOW_LOG)
                LOGGER.info("ZipContext: {} - {}", etryPath, cacheFileName.getName());
            if (cacheFileName.exists()) {
                if (Config.SHOW_LOG)
                    LOGGER.info("ZipContext: FB2 cache exists");
                return fb2Context.openDocumentInner(etryPath, password);
            }
        }

        String path = CacheZipUtils.extracIfNeed(fileName, CacheDir.ZipApp).unZipPath;
        if (path.endsWith("zip"))
            return null;

        CodecContext ctx = BookType.getCodecContextByPath(path);
        if (Config.SHOW_LOG)
            LOGGER.info("ZipContex: open {}", path);
        return ctx.openDocument(path, password);
    }

}
