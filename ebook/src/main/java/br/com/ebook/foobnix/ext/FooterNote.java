package br.com.ebook.foobnix.ext;

import java.util.Map;

import br.com.ebook.Config;
import br.com.ebook.foobnix.android.utils.LOG;

public class FooterNote {

    public String path;
    public Map<String, String> notes;

    public FooterNote(String path, Map<String, String> notes) {
        this.path = path;
        this.notes = notes;
    }

    public void debugPrint() {
        if (Config.SHOW_LOG)
            LOG.d("debugPrint", path);
        if (notes == null) {
            if (Config.SHOW_LOG)
                LOG.d("Notes is null");
            return;
        }
        for (String key : notes.keySet()) {
            if (Config.SHOW_LOG)
                LOG.d(key, " = ", notes.get(key));
        }
    }

}
