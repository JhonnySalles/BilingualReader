package br.com.ebook.foobnix.android.utils;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import br.com.ebook.Config;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;

public class Objects {

    private static final Logger LOGGER = LoggerFactory.getLogger(Objects.class);
    static String TAG = "Objects";

    @Retention(RetentionPolicy.RUNTIME)
    public @interface IgnoreHashCode {

    }

    public static void saveToSP(Object obj, SharedPreferences sp) {
        if (Config.SHOW_LOG)
            LOGGER.info("{} - saveToSP", TAG);

        Editor edit = sp.edit();
        for (final Field f : obj.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isPrivate(f.getModifiers()))
                continue;

            try {
                if (Config.SHOW_LOG)
                    LOGGER.info("{} - saveToSP: {} {} {}", TAG, f.getType(), f.getName(), f.get(obj));

                if (f.getType().equals(int.class)) {
                    edit.putInt(f.getName(), f.getInt(obj));
                } else

                if (f.getType().equals(String.class)) {
                    Object object = f.get(obj);
                    edit.putString(f.getName(), object != null ? object.toString() : null);
                } else

                if (f.getType().equals(float.class)) {
                    edit.putFloat(f.getName(), f.getFloat(obj));
                } else

                if (f.getType().equals(long.class)) {
                    edit.putLong(f.getName(), f.getLong(obj));
                } else

                if (f.getType().equals(boolean.class)) {
                    edit.putBoolean(f.getName(), f.getBoolean(obj));
                } else

                if (f.getType().equals(java.util.Set.class)) {
                    edit.putStringSet(f.getName(), (Set<String>) f.get(obj));
                }

            } catch (Exception e) {
                LOGGER.error("Error save to sp: {} - {}", e.getMessage(), f.getName(), e);
            }
        }
        edit.commit();
    }

    public static void loadFromSp(Object obj, SharedPreferences sp) {

        for (final Field f : obj.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isPrivate(f.getModifiers())) {
                continue;
            }

            try {
                if (f.getType().equals(int.class)) {
                    f.setInt(obj, sp.getInt(f.getName(), f.getInt(obj)));
                } else

                if (f.getType().equals(String.class)) {
                    Object object = f.get(obj);
                    f.set(obj, sp.getString(f.getName(), object != null ? "" + object : null));
                } else

                if (f.getType().equals(float.class)) {
                    f.setFloat(obj, sp.getFloat(f.getName(), f.getFloat(obj)));
                } else

                if (f.getType().equals(long.class)) {
                    f.setLong(obj, sp.getLong(f.getName(), f.getLong(obj)));
                } else if (f.getType().equals(boolean.class)) {
                    f.setBoolean(obj, sp.getBoolean(f.getName(), f.getBoolean(obj)));
                } else if (f.getType().equals(java.util.Set.class)) {
                    f.set(obj, sp.getStringSet(f.getName(), new HashSet<String>()));
                }

                if (Config.SHOW_LOG)
                    LOGGER.info("{} - loadFromSp: {} {} {}", TAG, f.getType(), f.getName(), f.get(obj));

            } catch (Exception e) {
                LOGGER.error("Error get load from sp: {}", e.getMessage(), e);
            }
        }

    }

    public static int hashCode(Object o) {
        return hashCode(o, true);
    }

    static String hashStringID = "hashCode";

    public static int hashCode(Object obj, boolean ignoreSomeHash) {
        StringBuilder res = new StringBuilder();

        for (Field f : obj.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isPrivate(f.getModifiers())) {
                continue;
            }
            if (ignoreSomeHash && f.isAnnotationPresent(IgnoreHashCode.class)) {
                continue;
            }
            if (f.getName().equals(hashStringID)) {
                continue;
            }

            try {
                res.append(f.get(obj));
            } catch (Exception e) {
                LOGGER.error("Error hash code: {}", e.getMessage(), e);
            }

        }
        int hashCode = res.toString().hashCode();
        if (Config.SHOW_LOG)
            LOGGER.info("{} - hashCode: {}", TAG, hashCode);
        return hashCode;
    }

    public static void compareObjects(Object obj1, AppState obj2) {
        for (Field f : obj1.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isPrivate(f.getModifiers()))
                continue;

            try {
                String value1 = "" + f.get(obj1);
                String value2 = "" + f.get(obj2);
                if (!value1.equals(value2)) {
                    if (Config.SHOW_LOG)
                        LOGGER.info("{} - compareObjects not same: {} {} {}", TAG, f.getName(), value1, value2);
                }

            } catch (Exception e) {
                LOGGER.error("Error get compare objects: {}", e.getMessage(), e);
            }
        }

    }

}
