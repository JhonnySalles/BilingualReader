package br.com.ebook.foobnix.android.utils;

import android.graphics.Bitmap;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import br.com.ebook.Config;
import br.com.ebook.foobnix.pdf.info.IMG;
import br.com.ebook.universalimageloader.core.ImageLoader;
import br.com.ebook.universalimageloader.core.listener.SimpleImageLoadingListener;

public class Safe {

    private static final Logger LOGGER = LoggerFactory.getLogger(Safe.class);

    public static final String TXT_SAFE_RUN = "SAFE_RUN-";

    static Random r = new Random();

    public static void run(final Runnable action) {
        ImageLoader.getInstance().clearAllTasks();

        ImageLoader.getInstance().loadImage(TXT_SAFE_RUN, IMG.noneOptions, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                if (Config.SHOW_LOG)
                    LOGGER.info(TXT_SAFE_RUN + " end {}", imageUri);
                if (action != null) {
                    ImageLoader.getInstance().clearAllTasks();
                    action.run();
                }
            }
        });

    }
}
