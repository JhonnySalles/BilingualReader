package br.com.ebook.foobnix.ui2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.query.QueryBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import br.com.ebook.foobnix.dao2.DaoMaster;
import br.com.ebook.R;
import br.com.ebook.foobnix.android.utils.LOG;
import br.com.ebook.foobnix.dao2.DaoSession;
import br.com.ebook.foobnix.dao2.FileMeta;
import br.com.ebook.foobnix.dao2.FileMetaDao;
import br.com.ebook.foobnix.pdf.info.ExtUtils;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;
import br.com.ebook.foobnix.ui2.adapter.FileMetaAdapter;

public class AppDB {

    private static final String DB_NAME = "all-5";

    private final static AppDB in = new AppDB();

    public enum SEARCH_IN {
        //
        PATH(FileMetaDao.Properties.Path, -1), //
        SERIES(FileMetaDao.Properties.Sequence, AppState.MODE_SERIES), //
        GENRE(FileMetaDao.Properties.Genre, AppState.MODE_GENRE), //
        AUTHOR(FileMetaDao.Properties.Author, AppState.MODE_AUTHORS);
        //
        private final Property property;
        private final int mode;

        private SEARCH_IN(Property property, int mode) {
            this.property = property;
            this.mode = mode;
        }

        public Property getProperty() {
            return property;
        }

        public String getDotPrefix() {
            return "@" + name().toLowerCase(Locale.US);
        }

        public static SEARCH_IN getByMode(int index) {
            for (SEARCH_IN sortBy : values()) {
                if (sortBy.getMode() == index) {
                    return sortBy;
                }
            }
            return SEARCH_IN.AUTHOR;
        }

        public static SEARCH_IN getByPrefix(String string) {
            for (SEARCH_IN sortBy : values()) {
                if (string.startsWith(sortBy.getDotPrefix())) {
                    return sortBy;
                }
            }
            return SEARCH_IN.PATH;
        }

        public int getMode() {
            return mode;
        }
    }

    public enum SORT_BY {
        //
        PATH(0, R.string.by_path, FileMetaDao.Properties.Path), //
        FILE_NAME(1, R.string.by_file_name, FileMetaDao.Properties.PathTxt), //
        SIZE(2, R.string.by_size, FileMetaDao.Properties.Size), //
        DATA(3, R.string.by_date, FileMetaDao.Properties.Date), //
        TITLE(4, R.string.by_title, FileMetaDao.Properties.Title), //
        AUTHOR(5, R.string.by_author, FileMetaDao.Properties.Author), //
        SERIES(6, R.string.by_series, FileMetaDao.Properties.Sequence), //
        SERIES_INDEX(7, R.string.by_number, FileMetaDao.Properties.SIndex);//

        private final int index;
        private final int resName;
        private final Property property;

        private SORT_BY(int index, int resName, Property property) {
            this.index = index;
            this.resName = resName;
            this.property = property;
        }

        public static SORT_BY getByID(int index) {
            for (SORT_BY sortBy : values()) {
                if (sortBy.getIndex() == index) {
                    return sortBy;
                }
            }
            return SORT_BY.PATH;

        }

        public int getIndex() {
            return index;
        }

    }

    public static AppDB get() {
        return in;
    }

    private FileMetaDao fileMetaDao;
    private DaoSession daoSession;

    public void open(Context c) {
        DatabaseUpgradeHelper helper = new DatabaseUpgradeHelper(c, DB_NAME);

        SQLiteDatabase writableDatabase = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(writableDatabase);

        daoSession = daoMaster.newSession();

        fileMetaDao = daoSession.getFileMetaDao();
    }

    public void delete(FileMeta meta) {
        fileMetaDao.delete(meta);
    }

    public List<FileMeta> getRecent() {
        List<FileMeta> list = fileMetaDao.queryBuilder().where(FileMetaDao.Properties.IsRecent.eq(1)).orderDesc(FileMetaDao.Properties.IsRecentTime).list();
        return removeNotExist(list);
    }

    public static List<FileMeta> removeNotExist(List<FileMeta> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<FileMeta>();
        }
        Iterator<FileMeta> iterator = items.iterator();
        while (iterator.hasNext()) {
            FileMeta next = iterator.next();
            if (!new File(next.getPath()).isFile()) {
                iterator.remove();
            }
        }
        return items;
    }

    public FileMeta getRecentLast() {
        List<FileMeta> list = fileMetaDao.queryBuilder().where(FileMetaDao.Properties.IsRecent.eq(1)).orderDesc(FileMetaDao.Properties.IsRecentTime).limit(1).list();
        removeNotExist(list);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    public void addRecent(String path) {
        if (!new File(path).isFile()) {
            LOG.d("Can't add to recent, it's not a file", path);
            return;
        }
        LOG.d("Add Recent", path);
        FileMeta load = getOrCreate(path);
        load.setIsRecent(true);
        load.setIsRecentTime(System.currentTimeMillis());
        fileMetaDao.update(load);
    }

    public void addStarFile(String path) {
        if (!new File(path).isFile()) {
            LOG.d("Can't add to recent, it's not a file", path);
            return;
        }
        LOG.d("addStarFile", path);
        FileMeta load = getOrCreate(path);
        load.setIsStar(true);
        load.setIsStarTime(System.currentTimeMillis());
        load.setCusType(FileMetaAdapter.DISPLAY_TYPE_FILE);
        fileMetaDao.update(load);
    }

    public void addStarFolder(String path) {
        if (!new File(path).isDirectory()) {
            LOG.d("Can't add to recent, it's not a file", path);
            return;
        }
        LOG.d("addStarFile", path);
        FileMeta load = getOrCreate(path);
        load.setPathTxt(ExtUtils.getFileName(path));
        load.setIsStar(true);
        load.setIsStarTime(System.currentTimeMillis());
        load.setCusType(FileMetaAdapter.DISPLAY_TYPE_DIRECTORY);
        fileMetaDao.update(load);
    }

    public void save(FileMeta meta) {
        fileMetaDao.insertOrReplace(meta);
    }

    public long getCount() {
        try {
            return fileMetaDao.queryBuilder().count();
        } catch (Exception e) {
            return 0;
        }
    }

    public List<FileMeta> getAll() {
        try {
            return fileMetaDao.queryBuilder().list();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public FileMeta load(String path) {
        return fileMetaDao.load(path);
    }

    public FileMeta getOrCreate(String path) {
        FileMeta load = fileMetaDao.load(path);
        try {
            if (load == null) {
                load = new FileMeta(path);
                fileMetaDao.insert(load);
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return load;
    }

    public void update(FileMeta meta) {
        try {
            fileMetaDao.update(meta);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public List<FileMeta> getStarsFiles() {
        QueryBuilder<FileMeta> where = fileMetaDao.queryBuilder();
        List<FileMeta> list = where.where(FileMetaDao.Properties.IsStar.eq(1), where.or(FileMetaDao.Properties.CusType.isNull(), FileMetaDao.Properties.CusType.eq(FileMetaAdapter.DISPLAY_TYPE_FILE)))
                .orderDesc(FileMetaDao.Properties.IsStarTime).list();
        return removeNotExist(list);
    }

    public List<FileMeta> getStarsFolder() {
        return fileMetaDao.queryBuilder().where(FileMetaDao.Properties.IsStar.eq(1), FileMetaDao.Properties.CusType.eq(FileMetaAdapter.DISPLAY_TYPE_DIRECTORY)).orderAsc(FileMetaDao.Properties.PathTxt).list();
    }

    public boolean isStarFolder(String path) {
        try {
            FileMeta load = fileMetaDao.load(path);
            if (load == null) {
                return false;
            }
            return load != null && load.getIsStar();
        } catch (Exception e) {
            return false;
        }
    }

}
