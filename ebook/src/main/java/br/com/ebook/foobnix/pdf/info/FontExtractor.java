package br.com.ebook.foobnix.pdf.info;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import br.com.ebook.Config;
import br.com.ebook.foobnix.ext.EpubExtractor;
import br.com.ebook.foobnix.pdf.info.model.BookCSS;

public class FontExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FontExtractor.class);

    public static void extractFonts(final Context c) {
        if (c == null) {
            return;
        }
        new Thread() {
            @Override
            public void run() {
                extractInside(c, "fonts", BookCSS.FONTS_DIR);
            };
        }.start();

    }

    private static void extractInside(final Context c, String from, String to) {
        try {
            File fontsDir = getFontsDir(c, to);
            if (fontsDir.exists()) {
                if (Config.SHOW_LOG)
                    LOGGER.info("FontExtractor Dir exists: {}", fontsDir);
            } else {
                fontsDir.mkdirs();
            }
            String[] list = c.getAssets().list(from);
            for (String fontName : list) {
                File fontFile = new File(fontsDir, fontName);
                if (!fontFile.exists()) {
                    if (Config.SHOW_LOG)
                        LOGGER.info("FontExtractor Copy file {} to {}", fontName, fontFile);
                    InputStream open = c.getAssets().open(from + "/" + fontName);
                    EpubExtractor.writeToStream(open, new FileOutputStream(fontFile));
                    open.close();
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error extract inside: {}", e.getMessage(), e);
        }
    }

    public static File getFontsDir(final Context c, String to) {
        return new File(c.getExternalCacheDir(), to);
    }
}
