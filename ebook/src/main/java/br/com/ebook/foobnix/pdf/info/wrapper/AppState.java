package br.com.ebook.foobnix.pdf.info.wrapper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.view.KeyEvent;

import br.com.ebook.foobnix.android.utils.LOG;
import br.com.ebook.foobnix.android.utils.MemoryUtils;
import br.com.ebook.foobnix.pdf.info.ExportSettingsManager;
import br.com.ebook.foobnix.pdf.info.Urls;
import br.com.ebook.foobnix.pdf.info.view.DragingPopup;
import br.com.ebook.foobnix.ui2.AppDB;
import br.com.ebook.foobnix.android.utils.Dips;
import br.com.ebook.foobnix.android.utils.Objects;
import br.com.ebook.foobnix.android.utils.Objects.IgnoreHashCode;
import br.com.ebook.foobnix.pdf.info.model.BookCSS;
import br.com.ebook.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class AppState {

    public static final String PROXY_HTTP = "HTTP";
    public static final String PROXY_SOCKS = "SOCKS";

    public static final int TEXT_COLOR_DAY = Color.parseColor("#5b5b5b");
    public static final int TEXT_COLOR_NIGHT = Color.parseColor("#8e8e8e");

    public static final long APP_CLOSE_AUTOMATIC = TimeUnit.MINUTES.toMillis(500);// SECONDS, MINUTES
    public static final long APP_UPDATE_TIME_IN_UI = TimeUnit.SECONDS.toMillis(10);
    // public static final long APP_CLOSE_AUTOMATIC =
    // TimeUnit.SECONDS.toMillis(5);
    public static final int DAY_TRANSPARENCY = 200;
    public static final int NIGHT_TRANSPARENCY = 160;
    public static Map<String, String[]> CONVERTERS = new LinkedHashMap<String, String[]>();

    static {
        CONVERTERS.put("PDF", "https://cloudconvert.com/anything-to-pdf, http://topdf.com".split(", "));
        CONVERTERS.put("PDF Rotate", "https://www.pdfrotate.com, https://smallpdf.com/rotate-pdf, http://www.rotatepdf.net".split(", "));
        CONVERTERS.put("EPUB", "https://cloudconvert.com/anything-to-epub, http://toepub.com".split(", "));
        CONVERTERS.put("MOBI", "https://cloudconvert.com/anything-to-mobi, http://toepub.com".split(", "));
        CONVERTERS.put("AZW3", "https://cloudconvert.com/anything-to-azw3, http://toepub.com".split(", "));
        CONVERTERS.put("DOCX", "https://cloudconvert.com/anything-to-docx, http://document.online-convert.com/convert-to-docx, http://pdf2docx.com/".split(", "));

    }

    public static final String PNG = "PNG";
    public static final String JPG = "JPG";

    public static final String[] LIBRE_EXT = ".odt, .odp, .docx, .doc, .pptx, .ppt".split(", ");
    public static final String[] OTHER_BOOK_MEDIA = ".wav, mp3".split(", ");
    public static final String[] OTHER_BOOK_EXT = ".abw, .docm, .lwp, .md, .pages, .rst, .sdw, .tex, .wpd, .wps, .zabw, .cbc, .chm, .lit, .lrf, .oeb, .pml, .rb, .snb, .tcr, .txtz, .azw1, .tpz".split(", ");
    public static final String[] OTHER_ARCH_EXT = ".img, .zip, .rar, .7z, .arj, .bz2, .bzip2, .tbz2, .tbz, .txz, .cab, .gz, .gzip, .tgz, .iso, .lzh, .lha, .lzma, .tar, .xar, .z, .taz, .xz, .dmg".split(", ");

    public static int COLOR_WHITE = Color.WHITE;
    // public static int COLOR_BLACK = Color.parseColor("#030303");
    public static int COLOR_BLACK = Color.BLACK;

    public static int WIDGET_LIST = 1;
    public static int WIDGET_GRID = 2;

    public static int EDIT_NONE = 0;
    public static int EDIT_PEN = 1;
    public static int EDIT_DELETE = 2;

    public static int TAP_NEXT_PAGE = 0;
    public static int TAP_PREV_PAGE = 1;
    public static int TAP_DO_NOTHING = 2;

    public static int BLUE_FILTER_DEFAULT_COLOR = Color.BLACK;
    public static String MY_SYSTEM_LANG = "my";

    public boolean isUseTypeFace = false;

    public static List<Integer> NEXT_KEYS = Arrays.asList(//
            KeyEvent.KEYCODE_VOLUME_UP, //
            KeyEvent.KEYCODE_PAGE_UP, //
            // KeyEvent.KEYCODE_DPAD_UP,//
            KeyEvent.KEYCODE_DPAD_RIGHT, //
            94, //
            105 //
            // KeyEvent.KEYCODE_DEL//
    );

    public static List<Integer> PREV_KEYS = Arrays.asList(//
            KeyEvent.KEYCODE_VOLUME_DOWN, //
            KeyEvent.KEYCODE_PAGE_DOWN, //
            // KeyEvent.KEYCODE_DPAD_DOWN, //
            KeyEvent.KEYCODE_DPAD_LEFT, //
            95, //
            106 //
            // KeyEvent.KEYCODE_ENTER //

    );

    public List<String> COLORS = Arrays.asList(//
            "#000001", //
            "#000002", //
            "#0000FF", //
            "#00FF00", //
            "#808000", //
            "#FFFF00", //
            "#FF0000", //
            "#00FFFF", //
            "#000000", //
            "#FF00FF", //
            "#808080", //
            "#008000", //
            "#800000", //
            "#000080", //
            "#800080", //
            "#008080", //
            "#C0C0C0", //
            "#FFFFFF", //
            "#CDDC39"//
    );

    public static final List<String> STYLE_COLORS = Arrays.asList(//
            "#3949AB", //
            // "#2a56c6", //
            // "#E6A639", //
            // "#395B9C", //
            "#EA5964", //
            "#00897B", //
            "#000000" //

    );

    public boolean opdsLargeCovers = true;

    public final static String READ_COLORS_DEAFAUL =
            // (name),(bg),(text),(0-day 1-nigth)
            "" + //
                    "1,#ffffff,#000000,0;" + //
                    "2,#f2f0e9,#383226,0;" + //
                    "3,#f9f5e8,#333333,0;" + //
                    //
                    "A,#000000,#ffffff,1;" + //
                    "B,#000000,#8cffb5,1;" + //
                    "C,#3a3a3a,#c8c8c8,1;"; //

    public String readColors = READ_COLORS_DEAFAUL;

    public static String DEFAULTS_TABS_ORDER = "0#1,1#1,2#1,3#1,4#1,5#1,6#0";
    public String tabsOrder = DEFAULTS_TABS_ORDER;

    public int tintColor = Color.parseColor(STYLE_COLORS.get(0));

    public int statusBarColorDay = TEXT_COLOR_DAY;
    public int statusBarColorNight = TEXT_COLOR_NIGHT;
    // public int tintColor =
    // Color.parseColor(STYLE_COLORS.get(STYLE_COLORS.size() - 2));
    public int userColor = Color.MAGENTA;

    final public static List<Integer> WIDGET_SIZE = Arrays.asList(0, 70, 100, 150, 200, 250);

    public final static int MAX_SPEED = 149;

    public final static int MODE_GRID = 1;
    public final static int MODE_LIST = 2;
    public final static int MODE_COVERS = 3;
    public final static int MODE_AUTHORS = 4;
    public final static int MODE_GENRE = 5;
    public final static int MODE_SERIES = 6;
    public final static int MODE_LIST_COMPACT = 7;

    public final static int BOOKMARK_MODE_BY_DATE = 1;
    public final static int BOOKMARK_MODE_BY_BOOK = 2;

    public final static int DOUBLE_CLICK_AUTOSCROLL = 0;
    public final static int DOUBLE_CLICK_ADJUST_PAGE = 1;
    public final static int DOUBLE_CLICK_NOTHING = 2;
    public final static int DOUBLE_CLICK_ZOOM_IN_OUT = 3;
    public final static int DOUBLE_CLICK_CENTER_HORIZONTAL = 4;
    public final static int DOUBLE_CLICK_CLOSE_BOOK = 5;
    public final static int DOUBLE_CLICK_CLOSE_BOOK_AND_APP = 6;
    public final static int DOUBLE_CLICK_CLOSE_HIDE_APP = 7;

    public final static int BR_SORT_BY_PATH = 0;

    public final static int NEXT_SCREEN_SCROLL_BY_PAGES = 0;

    public final static int OUTLINE_HEADERS_AND_SUBHEADERES = 0;
    public final static int OUTLINE_ONLY_HEADERS = 1;

    public final static int READING_PROGRESS_NUMBERS = 0;
    public final static int READING_PROGRESS_PERCENT = 1;
    public final static int READING_PROGRESS_PERCENT_NUMBERS = 2;

    public final static int AUTO_BRIGTNESS = -1000;

    @IgnoreHashCode
    public int doubleClickAction1 = DOUBLE_CLICK_ADJUST_PAGE;

    @IgnoreHashCode
    public int inactivityTime = 2;
    @IgnoreHashCode
    public int remindRestTime = 60;

    public int flippingInterval = 10;
    public int ttsTimer = 60;

    @IgnoreHashCode
    public int readingProgress = READING_PROGRESS_NUMBERS;

    public int outlineMode = OUTLINE_ONLY_HEADERS;

    public boolean longTapEnable = true;

    public boolean isEditMode = true;
    public boolean isFullScreen = true;
    public boolean isFullScreenMain = false;
    public boolean isAutoFit = false;
    public boolean notificationOngoing = false;

    public boolean isShowImages = true;
    public boolean isShowToolBar = true;
    public boolean isShowReadingProgress = true;
    public boolean isShowChaptersOnProgress = true;

    public int nextScreenScrollBy = NEXT_SCREEN_SCROLL_BY_PAGES;// 0 by
    // pages,
    // 25 - 25%
    // persent

    public int nextScreenScrollMyValue = 15;

    public boolean isWhiteTheme = true;
    public boolean isOpenLastBook = false;

    public boolean isSortAsc = true;
    public int sortBy = AppDB.SORT_BY.PATH.ordinal();
    public int sortByBrowse = BR_SORT_BY_PATH;
    public boolean sortByReverse = false;

    @IgnoreHashCode
    public boolean isBrighrnessEnable = true;

    @IgnoreHashCode
    public boolean isRewindEnable = true;

    public int contrastImage = 0;
    public int brigtnessImage = 0;
    public boolean bolderTextOnImage = false;
    public boolean isEnableBC = false;

    @IgnoreHashCode
    public int appBrightness = AUTO_BRIGTNESS;

    public float cropTolerance = 0.5f;

    public float ttsSpeed = 1.0f;
    public float ttsPitch = 1.0f;

    public List<Integer> nextKeys = NEXT_KEYS;
    public List<Integer> prevKeys = PREV_KEYS;

    @IgnoreHashCode
    public boolean isUseVolumeKeys = true;

    @IgnoreHashCode
    public boolean isReverseKeys = Dips.isSmallScreen();

    public boolean isMusicianMode = false;
    public String musicText = "Musician";

    public boolean isCrop = false;
    public boolean isCut = false;
    public boolean isDouble = false;
    public boolean isDoubleCoverAlone = false;

    public boolean isDayNotInvert = true;

    public int cpTextLight = Color.BLACK;
    public int cpBGLight = Color.WHITE;
    public int cpTextBlack = Color.WHITE;
    public int cpBGBlack = Color.BLACK;

    public boolean isUseBGImageDay = false;
    public boolean isUseBGImageNight = false;
    public String bgImageDayPath = MagicHelper.IMAGE_BG_1;
    public String bgImageNightPath = MagicHelper.IMAGE_BG_1;
    public int bgImageDayTransparency = DAY_TRANSPARENCY;
    public int bgImageNightTransparency = NIGHT_TRANSPARENCY;

    public String appLang = AppState.MY_SYSTEM_LANG;
    public float appFontScale = 1.0f;

    public boolean isLocked = false;
    public boolean isLoopAutoplay = false;
    public boolean isBookCoverEffect = false;

    public int editWith = EDIT_PEN;
    public String annotationDrawColor = "";
    public String annotationTextColor = COLORS.get(2);
    public int editAlphaColor = 100;
    public float editLineWidth = 3;

    public boolean isAlwaysOpenAsMagazine = false;
    public boolean isRememberMode = false;
    public boolean isInkMode = true;

    public volatile boolean isAutoScroll = false;
    public int autoScrollSpeed = 120;

    @IgnoreHashCode
    public boolean isScrollSpeedByVolumeKeys = false;

    @IgnoreHashCode
    public int mouseWheelSpeed = 70;

    public String selectedText;

    // public int widgetHeigth = 100;
    public int widgetType = WIDGET_LIST;
    public int widgetItemsCount = 4;

    public int widgetSize = WIDGET_SIZE.get(1);

    @IgnoreHashCode
    public String rememberDict = "web:Google Translate";

    @IgnoreHashCode
    public boolean isRememberDictionary;

    public String fromLang = "en";
    public String toLang = Urls.getLangCode();

    @IgnoreHashCode
    public int orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;

    private static AppState instance = new AppState();
    private SharedPreferences sp;

    public int libraryMode = MODE_GRID;
    public int broseMode = MODE_LIST;
    public int recentMode = MODE_LIST;
    public int bookmarksMode = BOOKMARK_MODE_BY_DATE;
    public int starsMode = MODE_LIST;

    public boolean isBrowseGrid = false;

    public String downlodsPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Librera").getPath();
    public String ttsSpeakPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Librera/TTS").getPath();

    public String fileToDelete;

    public String lastBookPath;
    public int lastBookPage = 0;
    public int lastBookWidth = 0;
    public int lastBookHeight = 0;
    public int lastFontSize = 0;

    public int colorDayText = COLOR_BLACK;
    public int colorDayBg = COLOR_WHITE;

    public int colorNigthText = COLOR_WHITE;
    public int colorNigthBg = COLOR_BLACK;

    public boolean supportPDF = true;
    public boolean supportXPS = false;
    public boolean supportDJVU = true;
    public boolean supportEPUB = true;
    public boolean supportFB2 = true;
    public boolean supportRTF = false;
    public boolean supportMOBI = true;
    public boolean supportCBZ = false;
    public boolean supportZIP = false;
    public boolean supportOther = false;

    public boolean supportTXT = false;
    public boolean isPreText = false;
    public boolean isLineBreaksText = false;
    public boolean isIgnoreAnnotatations = false;
    public boolean isSaveAnnotatationsAutomatically = false;
    public boolean isShowCloseAppDialog = true;

    public boolean isFirstSurname = false;

    public boolean isOLED = false;

    public int cutP = 50;

    public volatile int fontSizeSp = Dips.isXLargeScreen() ? 32 : 24;
    public volatile int statusBarTextSizeAdv = Dips.isXLargeScreen() ? 16 : 14;
    public volatile int statusBarTextSizeEasy = Dips.isXLargeScreen() ? 16 : 12;
    public volatile int progressLineHeight = Dips.isXLargeScreen() ? 8 : 4;

    public String lastClosedActivity;
    public String lastMode;
    public String dirLastPath;

    public String versionNew = "";

    public boolean isRTL = Urls.isRtl();
    public boolean isCutRTL = Urls.isRtl();

    // perofrmance
    public int pagesInMemory = 3;
    public float pageQuality = 1.2f;
    public int rotate = 0;
    public int rotateViewPager = 0;

    @IgnoreHashCode
    public int tapzoneSize = Dips.isXLargeScreen() ? 15 : 25;

    public int allocatedMemorySize = (int) MemoryUtils.RECOMENDED_MEMORY_SIZE;

    @IgnoreHashCode
    public boolean isScrollAnimation = true;
    public String imageFormat = PNG;
    public boolean isCustomizeBgAndColors = false;
    public boolean isVibration = true;

    @IgnoreHashCode
    public boolean isLockPDF = false;

    @IgnoreHashCode
    public boolean isCropPDF = false;

    public boolean selectingByLetters = Arrays.asList("ja", "zh", "ko", "vi").contains(Urls.getLangCode());

    public long installationDate = System.currentTimeMillis();
    public long searchDate = 0;

    public boolean isFirstTimeVertical = true;
    public boolean isFirstTimeHorizontal = true;

    @IgnoreHashCode
    public boolean isShowLongBackDialog = false;

    public String customConfigColors = "";

    public boolean isStarsInWidget = false;

    public boolean isCropBookCovers = true;
    public boolean isBorderAndShadow = true;

    public boolean isBrowseImages = false;

    public int coverBigSize = (int) (((Dips.screenWidthDP() / (Dips.screenWidthDP() / 120)) - 8) * (Dips.isXLargeScreen() ? 1.5f : 1));
    public int coverSmallSize = 80;

    @IgnoreHashCode
    public int tapZoneTop = TAP_PREV_PAGE;
    @IgnoreHashCode
    public int tapZoneBottom = TAP_NEXT_PAGE;
    @IgnoreHashCode
    public int tapZoneLeft = TAP_PREV_PAGE;
    @IgnoreHashCode
    public int tapZoneRight = TAP_NEXT_PAGE;

    @IgnoreHashCode
    public int blueLightColor = BLUE_FILTER_DEFAULT_COLOR;

    @IgnoreHashCode
    public int blueLightAlpha = 30;

    @IgnoreHashCode
    public boolean isEnableBlueFilter = false;

    public boolean proxyEnable = false;
    public String proxyServer = "";
    public int proxyPort = 0;
    public String proxyUser = "";
    public String proxyPassword = "";
    public String proxyType = PROXY_HTTP;

    public String nameVerticalMode = "";
    public String nameHorizontalMode = "";
    public String nameMusicianMode = "";

    public boolean isAutomaticExport = true;
    public boolean isDisplayAllFilesInFolder = false;

    public Set<String> myAutoComplete = new HashSet<String>();

    @IgnoreHashCode
    public int hashCode = 0;

    public List<Integer> getNextKeys() {
        return isReverseKeys ? prevKeys : nextKeys;
    }

    public List<Integer> getPrevKeys() {
        return isReverseKeys ? nextKeys : prevKeys;
    }


    public static Map<String, String> getDictionaries(String input) {
        final Map<String, String> providers = new LinkedHashMap<String, String>();
        String ln = AppState.get().toLang;
        String from = AppState.get().fromLang;
        String text = Uri.encode(input);
        providers.put("Google Translate", String.format("https://translate.google.com/#%s/%s/%s", from, ln, text));
        providers.put("Lingvo", String.format("http://www.lingvo-online.ru/en/Translate/%s-%s/%s", from, ln, text));

        providers.put("Dictionary.com", "http://dictionary.reference.com/browse/" + text);

        providers.put("Oxford", "http://www.oxforddictionaries.com/definition/english/" + text);
        providers.put("Longman", "http://www.ldoceonline.com/search/?q=" + text);
        providers.put("Cambridge", "http://dictionary.cambridge.org/dictionary/american-english/" + text);
        providers.put("Macmillan", "http://www.macmillandictionary.com/dictionary/british/" + text);
        providers.put("Collins", "http://www.collinsdictionary.com/dictionary/english/" + text);
        providers.put("Merriam-Webster", "http://www.merriam-webster.com/dictionary/" + text);
        providers.put("1tudien", "http://www.1tudien.com/?w=" + text);
        providers.put("Vdict", String.format("http://vdict.com/%s,1,0,0.html", text));
        providers.put("Google Search", String.format("http://www.google.com/search?q=%s", text));
        providers.put("Wikipedia", String.format("https://%s.m.wikipedia.org/wiki/%s", from, text));
        providers.put("Wiktionary", String.format("https://%s.m.wiktionary.org/wiki/%s", from, text));
        return providers;
    }


    public final static List<String> appDictionariesKeys = Arrays.asList(//
            "search", //
            "lingvo", //
            "dict", //
            "livio", //
            "tran", //
            "promt", //
            "fora", //
            "aard", //
            "web", //
            "woordenboek"// https://play.google.com/store/apps/details?id=com.prisma.woordenboek.englesxl

            //
    );

    public static synchronized AppState get() {
        return instance;
    }

    private boolean isLoaded = false;

    public void defaults(Context c) {
        musicText = c.getString(R.string.musician);

        if (Dips.isEInk(c)) {
            AppState.get().isInkMode = true;
            AppState.get().isDayNotInvert = true;
            AppState.get().isEditMode = true;
            AppState.get().isRememberMode = false;
            AppState.get().isReverseKeys = true;
            AppState.get().isScrollAnimation = false;
            AppState.get().tintColor = Color.BLACK;

        }
    }

    public void load(final Context a) {
        try {
            if (!isLoaded) {
                AppState.get().isInkMode = Dips.isEInk(a);
                AppState.get().bolderTextOnImage = Dips.isEInk(a);
                AppState.get().isEnableBC = Dips.isEInk(a);
                nameVerticalMode = a.getString(R.string.mode_vertical);
                nameHorizontalMode = a.getString(R.string.mode_horizontally);
                nameMusicianMode = a.getString(R.string.mode_musician);
                defaults(a);
                loadIn(a);
                BookCSS.get().load(a);
                DragingPopup.loadCache(a);
                PasswordState.get().load(a);
                LOG.d("AppState Load lasta", lastClosedActivity);
            } else {
                LOG.d("AppState is Loaded", lastClosedActivity);
            }
            isLoaded = true;
        } catch (Exception e) {
            LOG.e(e);
        }
    }


    public void loadIn(final Context a) {
        if (a == null) {
            return;
        }
        sp = a.getSharedPreferences(ExportSettingsManager.PREFIX_PDF, Context.MODE_PRIVATE);
        Objects.loadFromSp(this, sp);
    }

    public static String keyToString(final List<Integer> list) {
        Collections.sort(list);
        final StringBuilder line = new StringBuilder();
        for (final int value : list) {
            line.append(value);
            line.append(",");
        }
        return line.toString();
    }

    public static List<Integer> stringToKyes(final String list) {
        final List<Integer> res = new ArrayList<Integer>();

        for (final String value : list.split(",")) {
            if (value != null && !value.trim().equals("")) {
                res.add(new Integer(value.trim()));
            }
        }
        Collections.sort(res);
        return res;
    }

    public synchronized void save(final Context a) {
        try {
            saveIn(a);
            BookCSS.get().save(a);
            DragingPopup.saveCache(a);
            PasswordState.get().save(a);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public void saveIn(final Context a) {
        if (a == null) {
            return;
        }
        int currentHash = Objects.hashCode(AppState.get(), false);
        if (currentHash == hashCode) {
            LOG.d("Objects", "Ignore save hashCode the same");
            return;
        }
        sp = a.getSharedPreferences(ExportSettingsManager.PREFIX_PDF, Context.MODE_PRIVATE);
        hashCode = currentHash;
        Objects.saveToSP(AppState.get(), sp);
    }

}