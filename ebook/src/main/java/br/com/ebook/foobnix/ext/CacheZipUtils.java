package br.com.ebook.foobnix.ext;

import android.content.Context;

import androidx.core.util.Pair;

import org.ebookdroid.BookType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import br.com.ebook.Config;

public class CacheZipUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheZipUtils.class);
    private static final int BUFFER_SIZE = 16 * 1024;

    public enum CacheDir {
        ZipApp("ZipApp"), //
        ZipService("ZipService"); //

        private final String type;

        private CacheDir(String type) {
            this.type = type;
        }

        public static File parent;

        public String getType() {
            return type;
        }

        public static void createCacheDirs() {
            for (CacheDir folder : values()) {
                File root = new File(parent, folder.getType());
                if (!root.exists()) {
                    root.mkdirs();
                }
            }
        }


        public void removeCacheContent() {
            try {
                removeFiles(getDir().listFiles());
            } catch (Exception e) {
                LOGGER.error("Error to remove cache: {}", e.getMessage(), e);
            }
        }

        public File getDir() {
            return new File(parent, type);
        }

    }

    public static File CACHE_UN_ZIP_DIR;
    public static File CACHE_BOOK_DIR;
    public static File CACHE_WEB;
    public static File ATTACHMENTS_CACHE_DIR;
    public static final Lock cacheLock = new ReentrantLock();

    public static void init(Context c, File dir) {
        CACHE_BOOK_DIR = new File(dir, "Book");
        CACHE_UN_ZIP_DIR = new File(dir, "UnZip");
        ATTACHMENTS_CACHE_DIR = new File(dir, "Attachments");
        CACHE_WEB = new File(dir, "Web");

        CacheZipUtils.createAllCacheDirs();
        CacheDir.createCacheDirs();
    }

    public static void createAllCacheDirs() {
        if (!CACHE_BOOK_DIR.exists()) {
            CACHE_BOOK_DIR.mkdirs();
        }
        if (!ATTACHMENTS_CACHE_DIR.exists()) {
            ATTACHMENTS_CACHE_DIR.mkdirs();
        }
        if (!CACHE_WEB.exists()) {
            CACHE_WEB.mkdirs();
        }
    }

    public static void removeFiles(File[] files) {
        try {
            if (files == null) {
                return;
            }
            for (File file : files) {
                if (file != null) {
                    file.delete();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error to remove files: {}", e.getMessage(), e);
        }
    }


    public static void removeFiles(File[] files, File exept) {
        try {
            if (files == null || exept == null) {
                return;
            }
            for (File file : files) {
                if (file != null && !file.getName().startsWith(exept.getName())) {
                    file.delete();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error to remove files: {}", e.getMessage(), e);
        }
    }

    public static Pair<Boolean, String> isSingleAndSupportEntryFile(File file) {
        try {
            return isSingleAndSupportEntry(file);
        } catch (Exception e) {
            return new Pair<>(false, "");
        }
    }

    public static Pair<Boolean, String> isSingleAndSupportEntry(File is) {
        if (is == null) {
            return new Pair<>(false, "");
        }
        String name = "";
        try {
            ZipFile zipFile = new ZipFile(is, StandardCharsets.UTF_8);
            boolean find = false;
            ZipEntry nextEntry = null;

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements() && (nextEntry = entries.nextElement()) != null) {
                name = nextEntry.getName();
                if (find) {
                    zipFile.close();
                    return new Pair<>(false, "");
                }
                find = true;
            }
            zipFile.close();
        } catch (Exception e) {
            LOGGER.error("Error to valide is suport a single entry: {}", e.getMessage(), e);
        }
        return new Pair<>(BookType.isSupportedExtByPath(name), name);
    }

    public static class UnZipRes {
        public String originalPath;
        public String unZipPath;
        public String entryName;

        public UnZipRes(String originalPath, String unZipPath, String entryName) {
            this.originalPath = originalPath;
            this.unZipPath = unZipPath;
            this.entryName = entryName;
        }
    }

    public static UnZipRes extracIfNeed(String path, CacheDir folder) {
        if (!path.endsWith(".zip"))
            return new UnZipRes(path, path, null);

        folder.removeCacheContent();

        try {
            File file = new File(path);
            if (!isSingleAndSupportEntry(file).first)
                return new UnZipRes(path, path, null);

            ZipFile zipFile = new ZipFile(file, StandardCharsets.UTF_8);

            ZipEntry nextEntry = null;
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements() && (nextEntry = entries.nextElement()) != null) {
                if (BookType.isSupportedExtByPath(nextEntry.getName())) {
                    File out = new File(folder.getDir(), nextEntry.getName());
                    BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(out));
                    writeToStream(zipFile.getInputStream(nextEntry), fileOutputStream);
                    if (Config.SHOW_LOG)
                        LOGGER.info("Unpack archive: {}", file.getPath());

                    zipFile.close();
                    return new UnZipRes(path, file.getPath(), nextEntry.getName());
                }
            }
            zipFile.close();
        } catch (Exception e) {
            LOGGER.error("Error to extract: {}", e.getMessage(), e);
        }
        return new UnZipRes(path, path, null);
    }

    public static void writeToStream(InputStream zipInputStream, OutputStream out) throws IOException {

        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipInputStream.read(bytesIn)) != -1) {
            out.write(bytesIn, 0, read);
        }
        out.close();
    }

    static public void zipFolder(String srcFolder, String destZipFile) throws Exception {
        ZipOutputStream zip = null;
        FileOutputStream fileWriter = null;

        fileWriter = new FileOutputStream(destZipFile);
        zip = new ZipOutputStream(fileWriter);
        zip.setLevel(0);

        addFolderToZip("", srcFolder, zip);
        zip.flush();
        zip.close();
    }

    static private void addFileToZip(String path, String srcFile, ZipOutputStream zip) throws Exception {

        File folder = new File(srcFile);
        if (folder.isDirectory()) {
            addFolderToZip(path, srcFile, zip);
        } else {
            byte[] buf = new byte[BUFFER_SIZE];
            int len;
            FileInputStream in = new FileInputStream(srcFile);
            zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
            while ((len = in.read(buf)) > 0) {
                zip.write(buf, 0, len);
            }
            in.close();
        }
    }

    static private void addFolderToZip(String path, String srcFolder, ZipOutputStream zip) throws Exception {
        File folder = new File(srcFolder);

        for (String fileName : folder.list()) {
            if (path.equals(""))
                addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip);
            else
                addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip);
        }
    }

    public static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }

    public static void copyFile(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }
}
