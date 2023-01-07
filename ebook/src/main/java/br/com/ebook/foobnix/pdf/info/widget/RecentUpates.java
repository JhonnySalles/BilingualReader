package br.com.ebook.foobnix.pdf.info.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;

import br.com.ebook.universalimageloader.core.ImageLoader;
import br.com.ebook.foobnix.android.utils.LOG;
import br.com.ebook.foobnix.android.utils.TxtUtils;
import br.com.ebook.foobnix.dao2.FileMeta;
import br.com.ebook.foobnix.pdf.info.IMG;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;
import br.com.ebook.R;
import br.com.ebook.foobnix.pdf.search.activity.HorizontalViewActivity;
import br.com.ebook.foobnix.sys.ImageExtractor;
import br.com.ebook.foobnix.tts.TTSActivity;
import br.com.ebook.foobnix.ui2.AppDB;

import org.ebookdroid.ui.viewer.VerticalViewActivity;

import java.io.File;
import java.util.Arrays;

public class RecentUpates {

    @TargetApi(25)
    public static void updateAll(final Context c) {
        if (c == null) {
            return;
        }

        LOG.d("RecentUpates", "MUPDF!", c.getClass());

        if (Build.VERSION.SDK_INT >= 25) {
            try {
                FileMeta recentLast = AppDB.get().getRecentLast();
                if (recentLast != null) {
                    ShortcutManager shortcutManager = c.getSystemService(ShortcutManager.class);
                    String url = IMG.toUrl(recentLast.getPath(), ImageExtractor.COVER_PAGE, IMG.getImageSize());
                    Bitmap image = ImageLoader.getInstance().loadImageSync(url, IMG.displayCacheMemoryDisc);

                    Intent lastBookIntent = new Intent(c, VerticalViewActivity.class);
                    if (AppState.get().isAlwaysOpenAsMagazine) {
                        lastBookIntent = new Intent(c, HorizontalViewActivity.class);
                    }
                    lastBookIntent.setAction(Intent.ACTION_VIEW);
                    lastBookIntent.setData(Uri.fromFile(new File(recentLast.getPath())));

                    ShortcutInfo shortcut = new ShortcutInfo.Builder(c, "last")//
                            .setShortLabel(recentLast.getTitle())//
                            .setLongLabel(TxtUtils.getFileMetaBookName(recentLast))//
                            .setIcon(Icon.createWithBitmap(image))//
                            .setIntent(lastBookIntent)//
                            .build();//

                    Intent tTSIntent = new Intent(c, TTSActivity.class);
                    tTSIntent.setData(Uri.fromFile(new File(recentLast.getPath())));
                    tTSIntent.setAction(Intent.ACTION_VIEW);

                    ShortcutInfo tts = new ShortcutInfo.Builder(c, "tts")//
                            .setShortLabel(c.getString(R.string.reading_out_loud))//
                            .setLongLabel(c.getString(R.string.reading_out_loud))//
                            .setIcon(Icon.createWithBitmap(image))//
                            .setIntent(tTSIntent)//
                            .build();//

                    shortcutManager.setDynamicShortcuts(Arrays.asList(tts, shortcut));
                    // shortcutManager.setDynamicShortcuts(Arrays.asList(shortcut));
                }
            } catch (Exception e) {
                LOG.e(e);
            }

        }
    }

}
