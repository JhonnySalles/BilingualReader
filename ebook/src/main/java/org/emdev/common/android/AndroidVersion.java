package org.emdev.common.android;

public class AndroidVersion {

    public static final int VERSION = getVersion();

    public static final boolean is41x = 16 <= VERSION;

    private static int getVersion() {
        try {
            return Integer.parseInt(android.os.Build.VERSION.SDK);
        } catch (Throwable th) {
            return 3;
        }
    }

}
