package br.com.ebook.foobnix.pdf.info;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import br.com.ebook.foobnix.android.utils.LOG;
import br.com.ebook.foobnix.pdf.info.wrapper.AppBookmark;
import br.com.ebook.foobnix.sys.TempHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppSharedPreferences {
    public static final String BOOKMARK_ = "bookmark_";
    public static final String RECENT_ = "recent:";
    public static final String STAR_ = "star:";

    final static AppSharedPreferences instance = new AppSharedPreferences();

    private SharedPreferences bookmarkPreferences;

    public static AppSharedPreferences get() {
        return instance;
    }

    public void init(Context context) {
        bookmarkPreferences = context.getSharedPreferences(ExportSettingsManager.PREFIX_BOOKMARKS_PREFERENCES, Context.MODE_PRIVATE);
    }

    public void addStart(String path1) {
        Uri uri = Uri.fromFile(new File(path1));
        LOG.d("ADD_PREF STAR", path1);
        SharedPreferences.Editor editor = bookmarkPreferences.edit();
        editor.remove(STAR_ + path1);
        editor.putString(STAR_ + path1, uri.toString() + "\n" + System.currentTimeMillis());
        editor.commit();
        TempHolder.listHash++;
    }


    public void removeStar(String path) {
        if (path == null) {
            return;
        }
        SharedPreferences.Editor editor = bookmarkPreferences.edit();
        editor.remove(STAR_ + path);
        editor.commit();
        TempHolder.listHash++;
    }

    public void addBookMark(AppBookmark bookmark) {
        SharedPreferences.Editor editor = bookmarkPreferences.edit();

        editor.putString(BOOKMARK_ + bookmark.getDate(), AppBookmark.decode(bookmark));
        editor.commit();
        TempHolder.listHash++;
        bookmarks.clear();
    }

    public void removeBookmark(AppBookmark bookmark) {
        SharedPreferences.Editor editor = bookmarkPreferences.edit();
        editor.remove(BOOKMARK_ + bookmark.getDate());
        editor.commit();
        TempHolder.listHash++;
        bookmarks.clear();
    }

    public List<AppBookmark> getBookmarksByBook(File book) {
        List<AppBookmark> filter = new ArrayList<AppBookmark>();
        if (book == null) {
            return filter;
        }

        for (AppBookmark current : getBookmarks()) {
            if (current != null && current.getPath() != null && book.getPath() != null && current.getPath().equals(book.getPath())) {
                filter.add(current);
            }
        }
        Collections.sort(filter, COMPARE_BY_PAGE);
        return filter;

    }

    public static Comparator<AppBookmark> COMPARE_BY_PAGE = new Comparator<AppBookmark>() {
        @Override
        public int compare(AppBookmark lhs, AppBookmark rhs) {
            return lhs.getPage() - rhs.getPage();
        }
    };

    List<AppBookmark> bookmarks = new ArrayList<AppBookmark>();

    public List<AppBookmark> getBookmarks() {
        if (!bookmarks.isEmpty()) {
            return bookmarks;
        }
        for (String key : bookmarkPreferences.getAll().keySet()) {
            if (key.startsWith(BOOKMARK_)) {
                String bookString = bookmarkPreferences.getString(key, null);
                AppBookmark encode = AppBookmark.encode(bookString);
                if (encode != null) {
                    bookmarks.add(encode);
                }
            }
        }
        Collections.sort(bookmarks, BY_DATE);
        return bookmarks;
    }

    public Map<String, List<AppBookmark>> getBookmarksMap() {

        Map<String, List<AppBookmark>> map = new HashMap<String, List<AppBookmark>>();

        List<AppBookmark> bookmarks = new ArrayList<AppBookmark>();
        for (String key : bookmarkPreferences.getAll().keySet()) {
            if (key.startsWith(BOOKMARK_)) {
                String bookString = bookmarkPreferences.getString(key, null);
                AppBookmark encode = AppBookmark.encode(bookString);
                if (encode != null) {
                    bookmarks.add(encode);
                    String path = encode.getPath();
                    List<AppBookmark> list = map.get(path);
                    if (list == null) {
                        list = new ArrayList<AppBookmark>();
                    }
                    list.add(encode);
                    map.put(path, list);
                }

            }

        }
        Collections.sort(bookmarks, BY_DATE);
        return map;
    }

    private final static Comparator<AppBookmark> BY_DATE = new Comparator<AppBookmark>() {
        @Override
        public int compare(AppBookmark lhs, AppBookmark rhs) {
            return lhs.getDate() - rhs.getDate() > 0 ? -1 : 1;
        }
    };

    public boolean isStar(String path) {
        return bookmarkPreferences.getString(STAR_ + path, null) != null;
    }

    public boolean changeIsStar(String path) {
        boolean isStar = isStar(path);
        if (isStar) {
            removeStar(path);
        } else {
            addStart(path);
        }
        // FileMetaOld info = MetaCache.get().getByPath(path);
        // if (info != null) {
        // info.setStar(!isStar);
        // }
        // MetaCache.get().updateStartsCache();
        return !isStar;
    }

}
