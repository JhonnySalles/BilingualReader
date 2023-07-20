package br.com.ebook.foobnix.android.utils;

import android.annotation.TargetApi;

import br.com.ebook.Config;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;

public class Vibro {

    public static void vibrate() {
        vibrate(100);
    }

    @TargetApi(26)
    public static void vibrate(long time) {
        if (AppState.get().isVibration) {
            /*if (Build.VERSION.SDK_INT >= 26) {
                ((Vibrator) eBookApplication.context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                ((Vibrator) eBookApplication.context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(time);
            }*/
        }
        if (Config.SHOW_LOG)
            LOG.d("Vibro", "vibrate", time);
    }

}
