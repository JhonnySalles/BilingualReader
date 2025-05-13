package br.com.ebook.foobnix.pdf.info;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.text.TextUtilsCompat;
import androidx.core.view.ViewCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import br.com.ebook.Config;

public class Urls {

    private static final Logger LOGGER = LoggerFactory.getLogger(Urls.class);

    public static String encode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            if (Config.SHOW_LOG)
                LOGGER.error("Error encode url: {}", e.getMessage(), e);
            return URLEncoder.encode(string);
        }
    }

    public static void open(final Context a, String url) {
        if (a == null || url == null) {
            return;
        }
        if (Config.SHOW_LOG)
            LOGGER.info(">>> open: {}", url);
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
