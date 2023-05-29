package br.com.ebook.foobnix.pdf.info;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.text.TextUtilsCompat;
import androidx.core.view.ViewCompat;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import br.com.ebook.BuildConfig;
import br.com.ebook.Config;
import br.com.ebook.foobnix.android.utils.LOG;

public class Urls {

    public static String encode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.e(e);
            return URLEncoder.encode(string);
        }
    }

    public static void open(final Context a, String url) {
        if (a == null || url == null) {
            return;
        }
        if (Config.SHOW_LOG)
            LOG.d(">>> open", url);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        a.startActivity(browserIntent);
    }
    
    public static void openTTS(Context a) {
        try {
            open(a, "market://details?id=com.google.android.tts");
        } catch (Exception e) {
            // android 1.6
            open(a, "https://play.google.com/store/apps/details?id=com.google.android.tts");
        }
    }

    public static String getLangCode() {
        try {
            return Locale.getDefault().getLanguage();
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isRtl() {
        try {
            return TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_RTL;
        } catch (Exception e) {
            return false;
        }
    }

}
