package br.com.ebook.foobnix.ext;

import org.ebookdroid.BookType;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import br.com.ebook.foobnix.android.utils.LOG;
import br.com.ebook.foobnix.pdf.info.ExtUtils;
import junrar.Archive;
import junrar.rarfile.FileHeader;

public class CbzCbrExtractor {

    public static boolean isZip(String path) {
        try {
            byte[] buffer = new byte[2];
            FileInputStream is = new FileInputStream(path);
            is.read(buffer);
            is.close();
            String archType = new String(buffer);
            if ("pk".equalsIgnoreCase(archType)) {
                return true;
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return false;
    }

    public static byte[] getBookCover(String path) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            if (BookType.CBZ.is(path) || isZip(path)) {
                File file = new File(path);

                ZipFile zipFile = new ZipFile(file, StandardCharsets.UTF_8);
                ZipEntry nextEntry = null;
                Enumeration<? extends ZipEntry> entries = zipFile.entries();

                List<String> names = new ArrayList<String>();
                while ((nextEntry = entries.nextElement()) != null) {
                    String name = nextEntry.getName();
                    LOG.d("Name", name);
                    if (ExtUtils.isImagePath(name)) {
                        names.add(name);
                    }
                }
                zipFile.close();
                Collections.sort(names);

                zipFile = new ZipFile(file, StandardCharsets.UTF_8);
                nextEntry = null;

                String first = names.get(0);
                while ((nextEntry = entries.nextElement()) != null) {
                    if (nextEntry.getName().equals(first)) {
                        CacheZipUtils.writeToStream(zipFile.getInputStream(nextEntry), out);
                        break;
                    }
                }
                zipFile.close();

            } else if (BookType.CBR.is(path)) {
                FileInputStream is = new FileInputStream(new File(path));
                Archive archive = new Archive(new File(path));
                FileHeader fileHeader = archive.getFileHeaders().get(0);
                if (fileHeader.isDirectory()) {
                    fileHeader = archive.getFileHeaders().get(1);

                }
                LOG.d("EXtract CBR", fileHeader.getFileNameString());
                archive.extractFile(fileHeader, out);
                archive.close();
                is.close();
            }

        } catch (Exception e) {
            LOG.e(e);
        }
        return out.toByteArray();
    }

}
