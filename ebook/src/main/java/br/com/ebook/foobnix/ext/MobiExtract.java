package br.com.ebook.foobnix.ext;

import br.com.ebook.foobnix.android.utils.LOG;
import br.com.ebook.foobnix.android.utils.TxtUtils;
import com.foobnix.libmobi.LibMobi;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;
import br.com.ebook.foobnix.mobi.parser.MobiParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MobiExtract {

    private static byte[] toByteArray(InputStream input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            int nRead;
            byte[] data = new byte[16384];

            while ((nRead = input.read(data, 0, data.length)) != -1) {
                output.write(data, 0, nRead);
            }
        } catch (IOException e) {

        }
        return output.toByteArray();
    }

    public static FooterNote extract(String inputPath, final String outputDir, String hashCode) throws IOException {
        try {
            int sucess = LibMobi.convertToEpub(inputPath, new File(outputDir, hashCode + "").getPath());
            File result = new File(outputDir, hashCode + hashCode + ".epub");
            return new FooterNote(result.getPath(), null);
        } catch (Exception e) {
            LOG.e(e);
        }
        return new FooterNote("", null);
    }

    public static EbookMeta getBookMetaInformation(String path, boolean onlyTitle) throws IOException {
        File file = new File(path);
        try {
            byte[] raw = toByteArray(new FileInputStream(file));

            MobiParser parse = new MobiParser(raw);
            String title = parse.getTitle();
            String author = parse.getAuthor();
            String subject = parse.getSubject();
            String lang = parse.getLanguage();

            if (TxtUtils.isEmpty(title)) {
                title = file.getName();
            }
            byte[] decode = null;
            if (!onlyTitle) {
                decode = parse.getCoverOrThumb();
            }

            if (AppState.get().isFirstSurname) {
                author = TxtUtils.replaceLastFirstName(author);
            }

            EbookMeta ebookMeta = new EbookMeta(title, author, decode);
            ebookMeta.setGenre(subject);
            ebookMeta.setLang(lang);
            return ebookMeta;

        } catch (Throwable e) {
            LOG.e(e);
            return EbookMeta.Empty();
        }
    }

    public static String getBookOverview(String path) {
        String info = "";
        try {
            File file = new File(path);
            byte[] raw = toByteArray(new FileInputStream(file));

            MobiParser parse = new MobiParser(raw);
            info = parse.getDescription();

        } catch (Throwable e) {
            LOG.e(e);
        }
        return info;
    }

    public static byte[] getBookCover(String path) {
        try {
            File file = new File(path);
            FileInputStream fileInputStream = new FileInputStream(file);
            try {
                byte[] raw = toByteArray(fileInputStream);
                MobiParser parse = new MobiParser(raw);
                byte[] coverOrThumb = parse.getCoverOrThumb();
                parse = null;
                raw = null;
                return coverOrThumb;
            } finally {
                fileInputStream.close();
            }
        } catch (Throwable e) {
            LOG.e(e);
        }
        return null;
    }

}
