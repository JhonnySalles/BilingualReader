package br.com.ebook.foobnix.android.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Apps {

    private static final Logger LOGGER = LoggerFactory.getLogger(Apps.class);
    
    public static String getPackageName(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.packageName;
        } catch (Exception e) {
            LOGGER.error("Error get package name: {}", e.getMessage(), e);
        }
        return "";
    }

    public static void showDesctop(Context c) {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(startMain);
    }

}
