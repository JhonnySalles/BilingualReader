package org.ebookdroid.common.cache;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import org.ebookdroid.BookType;
import org.ebookdroid.common.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import br.com.ebook.Config;
import br.com.ebook.foobnix.pdf.info.ExtUtils;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;

public class CacheManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheManager.class);
    private static Context s_context;

    public static String getFilePathFromAttachmentIfNeed(Activity activity) {
        try {
            if (activity != null && activity.getIntent() != null && "content".equals(activity.getIntent().getScheme())) {

                String fileName = getFileName(activity.getIntent().getData());
                if (fileName != null) {
                    final File tempFile = CacheManager.createTempFile(activity.getIntent().getData(), fileName);
                    return tempFile.getAbsolutePath();
                } else {
                    String mime = activity.getIntent().getType();
                    if (mime == null) {
                        mime = ExtUtils.getMimeTypeByUri(activity.getIntent().getData());
                    }
                    if (mime != null) {
                        BookType bookType = BookType.getByMimeType(mime);
                        if (bookType != null) {
                            final File tempFile = CacheManager.createTempFile(activity.getIntent().getData(), "book." + bookType.getExt());
                            return tempFile.getAbsolutePath();
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (Config.SHOW_LOG)
                LOGGER.error("Error get file path from attacment if need: {}", e.getMessage(), e);
        }
        return "";
    }

    public static String getFileName(Uri uri) {
        String result = uri.getPath();
        int cut = result.lastIndexOf('/');
        if (cut != -1) {
            result = result.substring(cut + 1);
        }

        return result;
    }

    public static PageCacheFile getPageFile(final String path, int pages) {
        long lastModified = new File(path).lastModified();
        final String md5 = StringUtils.md5(path + lastModified + pages + AppState.get().isFullScreen);
        if (Config.SHOW_LOG)
            LOGGER.info("LAST{}", md5);
        final File cacheDir = s_context.getFilesDir();
        return new PageCacheFile(cacheDir, md5 + ".cache");
    }

    public static File createTempFile(final Uri uri, String ext) throws IOException {
        if (Config.SHOW_LOG)
            LOGGER.info("createTempFile: {}", uri);

        final File cacheDir = s_context.getFilesDir();
        // final File tempfile = File.createTempFile("temp", ext, cacheDir);
        final File tempfile = new File(cacheDir, ExtUtils.getFileName(ext));
        if (Config.SHOW_LOG)
            LOGGER.info("FILE: {}", tempfile);
        tempfile.deleteOnExit();

        final InputStream source = s_context.getContentResolver().openInputStream(uri);
        copy(source, new FileOutputStream(tempfile));

        return tempfile;
    }

    public static void init(Context eBookDroidApp) {
        s_context = eBookDroidApp;
    }

    public static void copy(final InputStream source, final OutputStream target) throws IOException {
        ReadableByteChannel in = null;
        WritableByteChannel out = null;
        try {
            in = Channels.newChannel(source);
            out = Channels.newChannel(target);
            final ByteBuffer buf = ByteBuffer.allocateDirect(512 * 1024);
            while (in.read(buf) > 0) {
                buf.flip();
                out.write(buf);
                buf.flip();
            }
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (final IOException ex) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (final IOException ex) {
                }
            }
        }
    }

}
