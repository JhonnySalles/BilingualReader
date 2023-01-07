package br.com.ebook.foobnix.android.utils;

import android.graphics.Bitmap;
import android.view.View;

import br.com.ebook.universalimageloader.core.ImageLoader;
import br.com.ebook.universalimageloader.core.listener.SimpleImageLoadingListener;
import br.com.ebook.foobnix.pdf.info.IMG;

import java.util.Random;

public class Safe {

    public static final String TXT_SAFE_RUN = "SAFE_RUN-";

    static Random r = new Random();

    public static void run(final Runnable action) {
        ImageLoader.getInstance().clearAllTasks();

        ImageLoader.getInstance().loadImage(TXT_SAFE_RUN, IMG.noneOptions, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                LOG.d(TXT_SAFE_RUN, "end", imageUri);
                if (action != null) {
                    ImageLoader.getInstance().clearAllTasks();
                    action.run();
                }
            }
        });

    }
}
