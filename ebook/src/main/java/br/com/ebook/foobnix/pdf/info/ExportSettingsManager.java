package br.com.ebook.foobnix.pdf.info;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import br.com.ebook.Config;
import br.com.ebook.foobnix.android.utils.ResultResponse;
import br.com.ebook.foobnix.entity.FileMeta;
import br.com.ebook.foobnix.pdf.info.model.BookCSS;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;

public class ExportSettingsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportSettingsManager.class);

    public static final String PREFIX_BOOKMARKS_PREFERENCES = "ViewerPreferences";// DO
                                                                                  // NOT
                                                                                  // CHANGE!!!!
    public static final String PREFIX_BOOKS = "BOOKS";
    public static final String PREFIX_PDF = "pdf";
    public static final String PREFIX_RESULTS = "search_results_1";
    public static final String PREFIX_BOOK_CSS = "BookCSS";

    public static final String PREFIX_RECENT = "Recent";
    public static final String PREFIX_STARS_Books = "StarsBook";
    public static final String PREFIX_STARS_Folders = "StarsFolder";

    public static final String RECURCIVE = "recurcive";
    private static final String PATH2 = "PATH";

    SharedPreferences lastPathSP;
    SharedPreferences booksSP;
    SharedPreferences viewerSP;
    SharedPreferences pdfSP;
    SharedPreferences bookCSS;

    private static ExportSettingsManager instance;
    private Context c;

    private ExportSettingsManager(Context c) {
        this.c = c;
        booksSP = c.getSharedPreferences(PREFIX_BOOKS, Context.MODE_PRIVATE);
        viewerSP = c.getSharedPreferences(PREFIX_BOOKMARKS_PREFERENCES, Context.MODE_PRIVATE);
        pdfSP = c.getSharedPreferences(PREFIX_PDF, Context.MODE_PRIVATE);
        bookCSS = c.getSharedPreferences(PREFIX_BOOK_CSS, Context.MODE_PRIVATE);
    }

    public boolean exportAll(File toFile) {
        if (toFile == null)
            return false;

        if (Config.SHOW_LOG)
            LOGGER.info("Export all to {}", toFile.getPath());

        try {
            AppState.get().save(c);
            BookCSS.get().checkBeforeExport(c);

            JSONObject root = new JSONObject();
            root.put(PREFIX_PDF, exportToJSon(PREFIX_RESULTS, pdfSP, null));
            root.put(PREFIX_BOOKS, exportToJSon(PREFIX_BOOKS, booksSP, null));
            root.put(PREFIX_BOOKMARKS_PREFERENCES, exportToJSon(PREFIX_BOOKMARKS_PREFERENCES, viewerSP, null));
            root.put(PREFIX_BOOK_CSS, exportToJSon(PREFIX_BOOK_CSS, bookCSS, null));

            /*root.put(PREFIX_RECENT, fileMetaToJSON(AppDB.get().getRecent()));
            root.put(PREFIX_STARS_Books, fileMetaToJSON(AppDB.get().getStarsFiles()));
            root.put(PREFIX_STARS_Folders, fileMetaToJSON(AppDB.get().getStarsFolder()));*/

            String name = getSampleJsonConfigName(c, "export_all.json");
            File fileConfig = toFile;

            if (toFile.isDirectory()) {
                fileConfig = new File(toFile, name);
            }
            if (Config.SHOW_LOG)
                LOGGER.info("TEXT export to {}", name);

            FileWriter file = new FileWriter(fileConfig);
            if (Config.SHOW_LOG)
                LOGGER.info("TEXT export to {}", fileConfig.getPath());
            String string = root.toString(5);
            file.write(string);
            file.flush();
            file.close();
            return true;
        } catch (Exception e) {
            LOGGER.error("Error to export all: {}", e.getMessage(), e);
            Toast.makeText(c, e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return false;
    }

    public static String getSampleJsonConfigName(Context a, String ext) {
        try {
            final String format = Settings.System.getString(a.getContentResolver(), Settings.System.DATE_FORMAT);
            String time;
            if (TextUtils.isEmpty(format))
                time = DateFormat.getMediumDateFormat(a).format(System.currentTimeMillis());
            else
                time = new SimpleDateFormat(format).format(System.currentTimeMillis());

            String name = String.format("%s%s", time, ext);
            return name;
        } catch (Exception e) {
            return "pdf_reader" + ext;
        }
    }

    public boolean importAll(File file) {
        if (file == null)
            return false;

        if (Config.SHOW_LOG)
            LOGGER.info("Import all from {}", file.getPath());

        try {
            String json = new Scanner(file).useDelimiter("\\A").next();
            if (Config.SHOW_LOG)
                LOGGER.info("[IMPORT]: {}", json);
            JSONObject jsonObject = new JSONObject(json);

            importFromJSon(jsonObject.optJSONObject(PREFIX_PDF), pdfSP);
            importFromJSon(jsonObject.optJSONObject(PREFIX_BOOKS), booksSP);
            importFromJSon(jsonObject.optJSONObject(PREFIX_BOOKMARKS_PREFERENCES), viewerSP);
            importFromJSon(jsonObject.optJSONObject(PREFIX_BOOK_CSS), bookCSS);

            jsonToMeta(jsonObject.optJSONArray(PREFIX_RECENT), new ResultResponse<String>() {

                @Override
                public boolean onResultRecive(String result) {
                    //AppDB.get().addRecent(result);
                    return false;
                }
            });

            jsonToMeta(jsonObject.optJSONArray(PREFIX_STARS_Books), new ResultResponse<String>() {

                @Override
                public boolean onResultRecive(String result) {
                    //AppDB.get().addStarFile(result);
                    return false;
                }
            });

            jsonToMeta(jsonObject.optJSONArray(PREFIX_STARS_Folders), new ResultResponse<String>() {

                @Override
                public boolean onResultRecive(String result) {
                    //AppDB.get().addStarFolder(result);
                    return false;
                }
            });

            return true;
        } catch (Exception e) {
            LOGGER.error("Error import all: {}", e.getMessage(), e);
            Toast.makeText(c, e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return false;

    }

    public static JSONArray fileMetaToJSON(List<FileMeta> list) {
        JSONArray jsonObject = new JSONArray();
        if (list == null)
            return jsonObject;

        for (FileMeta value : list)
            jsonObject.put(value.getPath());

        return jsonObject;
    }

    public static void jsonToMeta(JSONArray jsonObject, ResultResponse<String> action) {
        if (jsonObject == null)
            return;

        try {
            for (int i = jsonObject.length() - 1; i >= 0; i--) {
                String path = jsonObject.getString(i);
                if (path != null)
                    action.onResultRecive(path);
            }
        } catch (Exception e) {
            LOGGER.error("Error join metadata: {}", e.getMessage(), e);
        }
    }

    public static JSONObject exportToJSon(String name, SharedPreferences sp, String exclude) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        Map<String, ?> all = sp.getAll();
        for (String key : all.keySet()) {
            if (exclude != null && key.startsWith(exclude))
                continue;

            Object value = all.get(key);
            jsonObject.put(key, value);
            if (Config.SHOW_LOG)
                LOGGER.info("export: {} - {}", key, value);
        }
        return jsonObject;
    }

    public static void importFromJSon(JSONObject jsonObject, SharedPreferences sp) throws JSONException {
        if (jsonObject == null) {
            if (Config.SHOW_LOG)
                LOGGER.info("import null");
            return;
        }
        if (Config.SHOW_LOG)
            LOGGER.info("importFromJSon", jsonObject);

        Iterator<String> keys = jsonObject.keys();
        Editor edit = sp.edit();
        while (keys.hasNext()) {
            String name = keys.next();
            Object res = jsonObject.get(name);
            if (Config.SHOW_LOG)
                LOGGER.info("import name{}type", name);

            if (res instanceof String)
                edit.putString(name, (String) res);
            else if (res instanceof Float)
                edit.putFloat(name, (Float) res);
            else if (res instanceof Double)
                edit.putFloat(name, ((Double) res).floatValue());
            else if (res instanceof Integer)
                edit.putInt(name, (Integer) res);
            else if (res instanceof Boolean)
                edit.putBoolean(name, (Boolean) res);
        }
        edit.commit();
    }

    public static ExportSettingsManager getInstance(Context c) {
        if (instance == null)
            instance = new ExportSettingsManager(c);

        return instance;
    }

    public String getLastPath() {
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        if (!externalStorageDirectory.exists()) {
            externalStorageDirectory = new File("/");
        }
        return lastPathSP.getString(PATH2, externalStorageDirectory.getPath());
    }

    public void saveLastPath(File file) {
        Editor edit = lastPathSP.edit();
        edit.putString(PATH2, file.getPath());
        edit.commit();
    }

}
