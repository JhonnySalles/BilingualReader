package br.com.ebook.foobnix.ext;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import br.com.ebook.Config;
import br.com.ebook.foobnix.hypen.HypenUtils;
import br.com.ebook.foobnix.pdf.info.ExtUtils;
import br.com.ebook.foobnix.pdf.info.model.BookCSS;

public class HtmlExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlExtractor.class);

    public static final String OUT_FB2_XML = "temp.html";

    public static FooterNote extract(String inputPath, final String outputDir) throws IOException {
        // File file = new File(new File(inputPath).getParent(), OUT_FB2_XML);
        File file = new File(outputDir, OUT_FB2_XML);

        try {
            String encoding = ExtUtils.determineEncoding(new FileInputStream(inputPath));

            if (Config.SHOW_LOG)
                LOGGER.info("HtmlExtractor encoding: {}", encoding);
            BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(inputPath), encoding));

            StringBuilder html = new StringBuilder();
            String line;

            if (BookCSS.get().isAutoHypens)
                HypenUtils.applyLanguage(BookCSS.get().hypenLang);

            boolean isBody = false;
            while ((line = input.readLine()) != null) {

                if (Config.SHOW_LOG)
                    LOGGER.info(line);

                if (line.toLowerCase(Locale.getDefault()).contains("<body"))
                    isBody = true;

                if (isBody)
                    html.append(line);

                if (line.toLowerCase(Locale.getDefault()).contains("</html>"))
                    break;
            }
            input.close();

            FileOutputStream out = new FileOutputStream(file);


            String string = Jsoup.clean(html.toString(), Whitelist.relaxed().removeTags("img"));


            if (BookCSS.get().isAutoHypens) {
                string = HypenUtils.applyHypnes(string);
                string = Jsoup.clean(string, Whitelist.relaxed());
            }
            // String string = html.toString();
            string = "<html><head></head><body style='text-align:justify;'><br/>" + string + "</body></html>";
            // string = string.replace("\">", "\"/>");
            string = string.replace("<br>", "<br/>");
            // string = string.replace("http://example.com/", "");

            out.write(string.getBytes());
            out.flush();
            out.close();
        } catch (Exception e) {
            LOGGER.error("Error to extract: {}", e.getMessage(), e);
        }

        return new FooterNote(file.getPath(), null);
    }

}
