package br.com.ebook.foobnix.ext;

import org.ebookdroid.BookType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import br.com.ebook.Config;
import br.com.ebook.foobnix.pdf.info.ExtUtils;

public class CbzCbrExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CbzCbrExtractor.class);

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
            LOGGER.error("Error to extract: {}", e.getMessage(), e);
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
                while (entries.hasMoreElements() && (nextEntry = entries.nextElement()) != null) {
                    String name = nextEntry.getName();
                    if (Config.SHOW_LOG)
                        LOGGER.info("Name: {}", name);
                    if (ExtUtils.isImagePath(name)) {
                        names.add(name);
                    }
                }
                zipFile.close();
                Collections.sort(names);

                zipFile = new ZipFile(file, StandardCharsets.UTF_8);
                nextEntry = null;

                String first = names.get(0);
                while (entries.hasMoreElements() && (nextEntry = entries.nextElement()) != null) {
                    if (nextEntry.getName().equals(first)) {
                        CacheZipUtils.writeToStream(zipFile.getInputStream(nextEntry), out);
                        break;
                    }
                }
                zipFile.close();

            }

        } catch (Exception e) {
            LOGGER.error("Error get book cover: {}", e.getMessage(), e);
        }
        return out.toByteArray();
    }

}
