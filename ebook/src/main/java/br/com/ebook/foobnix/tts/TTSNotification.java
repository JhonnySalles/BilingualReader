package br.com.ebook.foobnix.tts;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.core.app.NotificationCompat;

import org.ebookdroid.ui.viewer.VerticalViewActivity;

import java.io.File;

import br.com.ebook.application.eBookApplication;
import br.com.ebook.universalimageloader.core.ImageLoader;
import br.com.ebook.R;
import br.com.ebook.foobnix.android.utils.LOG;
import br.com.ebook.foobnix.dao2.FileMeta;
import br.com.ebook.foobnix.pdf.info.IMG;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;
import br.com.ebook.foobnix.pdf.search.activity.HorizontalViewActivity;
import br.com.ebook.foobnix.sys.ImageExtractor;
import br.com.ebook.foobnix.ui2.AppDB;

public class TTSNotification {

    public static final String ACTION_TTS = "TTSNotification_TTS";

    public static final String TTS_READ = "TTS_READ";
    public static final String TTS_STOP = "TTS_STOP";
    public static final String TTS_NEXT = "TTS_NEXT";

    private static final int NOT_ID = 123123;

    static String bookPath1;
    static int page1;

    public static void show(String bookPath, int page) {
        Context c = eBookApplication.context;
        bookPath1 = bookPath;
        page1 = page;
        try {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(c);

            FileMeta fileMeta = AppDB.get().getOrCreate(bookPath);

            Intent intent = new Intent(c, HorizontalViewActivity.class.getSimpleName().equals(AppState.get().lastMode) ? HorizontalViewActivity.class : VerticalViewActivity.class);
            intent.setAction(ACTION_TTS);
            intent.setData(Uri.fromFile(new File(bookPath)));
            if (page > 0) {
                intent.putExtra("page", page - 1);
            }

            PendingIntent contentIntent = PendingIntent.getActivity(c, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            PendingIntent stop = PendingIntent.getService(c, 0, new Intent(TTS_STOP, null, c, TTSService.class), PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent read = PendingIntent.getService(c, 0, new Intent(TTS_READ, null, c, TTSService.class), PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent next = PendingIntent.getService(c, 0, new Intent(TTS_NEXT, null, c, TTSService.class), PendingIntent.FLAG_UPDATE_CURRENT);

            builder.setContentIntent(contentIntent) //
                    .setSmallIcon(R.drawable.glyphicons_185_volume_up) //
                    .setLargeIcon(getBookImage(bookPath)) //
                    .setTicker(c.getString(R.string.app_name)) //
                    .setWhen(System.currentTimeMillis()) //
                    .setOngoing(AppState.get().notificationOngoing)//
                    .addAction(R.drawable.glyphicons_175_pause, c.getString(R.string.to_stop), stop)//
                    .addAction(R.drawable.glyphicons_174_play, c.getString(R.string.to_read), read)//
                    .addAction(R.drawable.glyphicons_177_forward, c.getString(R.string.next), next)//
                    .setContentTitle(fileMeta.getTitle() + " – " + fileMeta.getAuthor()) //
                    .setContentText(c.getString(R.string.page) + " " + page); ///

            Notification n = builder.build(); //
            nm.notify(NOT_ID, n);
        } catch (Exception e) {
            LOG.e(e);
            return;
        }
    }

    public static void hideNotification() {
        NotificationManager nm = (NotificationManager) eBookApplication.context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOT_ID);
    }

    public static void showLast() {
        show(bookPath1, page1);
    }

    public static Bitmap getBookImage(String path) {
        String url = IMG.toUrl(path, ImageExtractor.COVER_PAGE_WITH_EFFECT, IMG.getImageSize());
        return ImageLoader.getInstance().loadImageSync(url, IMG.displayCacheMemoryDisc);
    }
}
