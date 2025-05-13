package br.com.ebook.foobnix.ext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import br.com.ebook.Config;

public class FooterNote {

    private static final Logger LOGGER = LoggerFactory.getLogger(FooterNote.class);

    public String path;
    public Map<String, String> notes;

    public FooterNote(String path, Map<String, String> notes) {
        this.path = path;
        this.notes = notes;
    }

    public void debugPrint() {
        if (Config.SHOW_LOG)
            LOGGER.info("debugPrint: {}", path);
        if (notes == null) {
            if (Config.SHOW_LOG)
                LOGGER.info("Notes is null");
            return;
        }
        for (String key : notes.keySet()) {
            if (Config.SHOW_LOG)
                LOGGER.info("{} = {}", key, notes.get(key));
        }
    }

}
