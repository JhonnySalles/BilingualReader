package br.com.ebook.foobnix.hypen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public enum HyphenPattern {
    en("en", 2, 3),
    es("es", 2, 2),
    da("da", 2, 2),
    ga("ga", 2, 3),
    hy("hy", 2, 2),
    gu("gu", 2, 2),
    uk("uk", 2, 2),
    bn("bn", 2, 2),
    hi("hi", 2, 2),
    sl("sl", 2, 2),
    pl("pl", 2, 2),
    eo("eo", 2, 2),
    et("et", 2, 3),
    it("it", 2, 2),
    cs("cs", 2, 3),
    fr("fr", 2, 3),
    pt("pt", 2, 3),
    lt("lt", 2, 2),
    el("el", 2, 2),
    ca("ca", 2, 2),
    kn("kn", 2, 2),
    ml("ml", 2, 2),
    tr("tr", 2, 2),
    fi("fi", 2, 2),
    ta("ta", 2, 2),
    sk("sk", 2, 3),
    te("te", 2, 2),
    de("de", 2, 2),
    ro("ro", 2, 2),
    sr("sr", 2, 2),
    lv("lv", 2, 2),
    ru("ru", 2, 2),
    be("be", 2, 2),
    sv("sv", 2, 2),
    la("la", 2, 2),
    nl("nl", 2, 2);

    private static final Logger LOGGER = LoggerFactory.getLogger(HyphenPattern.class);

    public final String lang;
    public final int leftMin;
    public final int rightMin;
    public final Map<Integer, String> patternObject;

    HyphenPattern(String lang, int leftMin, int rightMin) {
        this.lang = lang;
        this.leftMin = leftMin;
        this.rightMin = rightMin;
        this.patternObject = loadPatterns();
    }

    private Map<Integer, String> loadPatterns() {
        Map<Integer, String> map = new HashMap<>();
        String resourcePath = "/hyphen/" + this.name() + ".txt";
        try (InputStream is = HyphenPattern.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                LOGGER.error("Resource not found: {}", resourcePath);
                return map;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int eqIndex = line.indexOf('=');
                    if (eqIndex > 0) {
                        try {
                            int key = Integer.parseInt(line.substring(0, eqIndex));
                            String value = line.substring(eqIndex + 1);
                            map.put(key, value);
                        } catch (NumberFormatException e) {
                            LOGGER.error("Error parsing key on line: {}", line);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error loading patterns for {}: {}", this.name(), e.getMessage(), e);
        }
        return map;
    }
}
