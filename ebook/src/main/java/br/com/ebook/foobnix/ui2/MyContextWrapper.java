package br.com.ebook.foobnix.ui2;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import br.com.ebook.foobnix.pdf.info.Urls;
import br.com.ebook.foobnix.android.utils.LOG;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;

import java.util.Locale;

public class MyContextWrapper extends ContextWrapper {

    public MyContextWrapper(Context base) {
        super(base);
    }


    @TargetApi(24)
    public static ContextWrapper wrap(Context context) {
        String language = AppState.get().appLang;
        final float scale = AppState.get().appFontScale;

        Resources res = context.getResources();
        Configuration configuration = res.getConfiguration();
        configuration.fontScale = scale;

        if (AppState.MY_SYSTEM_LANG.equals(AppState.get().appLang)) {
            language = Urls.getLangCode();
        }

        LOG.d("ContextWrapper language", language);

        Locale newLocale = new Locale(language);

        if (Build.VERSION.SDK_INT >= 24) {
            configuration.setLocale(newLocale);
            LocaleList localeList = new LocaleList(newLocale);
            LocaleList.setDefault(localeList);
            configuration.setLocales(localeList);

            context = context.createConfigurationContext(configuration);

        } else if (Build.VERSION.SDK_INT >= 17) {
            configuration.setLocale(newLocale);
            context = context.createConfigurationContext(configuration);
        } else {
            configuration.locale = newLocale;
            res.updateConfiguration(configuration, res.getDisplayMetrics());
        }

        return new ContextWrapper(context);
    }

    @SuppressWarnings("deprecation")
    public static Locale getSystemLocaleLegacy(Configuration config) {
        return config.locale;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static Locale getSystemLocale(Configuration config) {
        return config.getLocales().get(0);
    }

    @SuppressWarnings("deprecation")
    public static void setSystemLocaleLegacy(Configuration config, Locale locale) {
        config.locale = locale;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static void setSystemLocale(Configuration config, Locale locale) {
        config.setLocale(locale);
    }
}