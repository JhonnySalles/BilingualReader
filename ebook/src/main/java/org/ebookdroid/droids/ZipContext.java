package org.ebookdroid.droids;

import androidx.core.util.Pair;

import org.ebookdroid.BookType;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import java.io.File;

import br.com.ebook.foobnix.android.utils.LOG;
import br.com.ebook.foobnix.ext.CacheZipUtils;
import br.com.ebook.foobnix.ext.CacheZipUtils.CacheDir;

public class ZipContext extends PdfContext {

    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {
        LOG.d("ZipContext begin", fileName);

        Pair<Boolean, String> pack = CacheZipUtils.isSingleAndSupportEntryFile(new File(fileName));
        if (pack.first) {
            LOG.d("ZipContext", "Singe archive entry");
            Fb2Context fb2Context = new Fb2Context();
            String etryPath = pack.second;
            File cacheFileName = fb2Context.getCacheFileName(new File(CacheDir.ZipApp.getDir(), etryPath).getPath());
            LOG.d("ZipContext", etryPath, cacheFileName.getName());
            if (cacheFileName.exists()) {
                LOG.d("ZipContext", "FB2 cache exists");
                return fb2Context.openDocumentInner(etryPath, password);
            }
        }


        String path = CacheZipUtils.extracIfNeed(fileName, CacheDir.ZipApp).unZipPath;
        if (path.endsWith("zip")) {
            return null;
        }

        CodecContext ctx = BookType.getCodecContextByPath(path);
        LOG.d("ZipContext", "open", path);
        return ctx.openDocument(path, password);
    }

}
