package br.com.ebook.foobnix.pdf.info.wrapper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.Toast;

import org.ebookdroid.core.codec.PageLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import br.com.ebook.Config;
import br.com.ebook.foobnix.android.utils.ResultResponse;
import br.com.ebook.foobnix.android.utils.Safe;
import br.com.ebook.foobnix.android.utils.TxtUtils;
import br.com.ebook.foobnix.entity.FileMeta;
import br.com.ebook.foobnix.pdf.info.IMG;
import br.com.ebook.foobnix.pdf.info.PageUrl;
import br.com.ebook.foobnix.pdf.info.model.AnnotationType;
import br.com.ebook.foobnix.pdf.info.model.OutlineLinkWrapper;
import br.com.ebook.foobnix.sys.ImageExtractor;
import br.com.ebook.universalimageloader.core.ImageLoader;

@SuppressLint("NewApi")
public abstract class DocumentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentController.class);

    public final static List<Integer> orientationIds = Arrays.asList(//
            ActivityInfo.SCREEN_ORIENTATION_SENSOR, //
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, //
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, //
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, //
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT//
    );


    protected final Activity activity;
    public Handler handler;

    public long readTimeStart;

    public DocumentController(final Activity activity) {
        this.activity = activity;
        readTimeStart = System.currentTimeMillis();
    }

    public void initHandler() {
        handler = new Handler();
    }

    private File currentBook;
    private String title;

    private final LinkedList<Integer> linkHistory = new LinkedList<Integer>();

    public abstract void onGoToPage(int page);

    public abstract void onSrollLeft();

    public abstract void onSrollRight();

    public abstract void onScrollUp();

    public abstract void onScrollDown();

    public abstract void onNextPage(boolean animate);

    public abstract void onPrevPage(boolean animate);

    public abstract void onNextScreen(boolean animate);

    public abstract void onPrevScreen(boolean animate);

    public abstract void onZoomInc();

    public abstract void onZoomDec();

    public abstract void onZoomInOut(int x, int y);

    public abstract void onNightMode();

    public abstract void onCrop();

    public abstract void onFullScreen();

    public abstract int getCurentPage();

    public abstract int getCurentPageFirst1();

    public abstract int getPageCount();

    public abstract void onScrollY(int value);

    public abstract void onAutoScroll();

    public abstract void clearSelectedText();

    public abstract void saveChanges(List<PointF> points, int color);

    public abstract void deleteAnnotation(long pageHander, int page, int index);

    public abstract void underlineText(int color, float width, AnnotationType type);

    public abstract void getOutline(ResultResponse<List<OutlineLinkWrapper>> outline, boolean forse);

    public abstract String getFootNote(String text);

    public abstract List<String> getMediaAttachments();

    public abstract PageUrl getPageUrl(int page);

    public abstract void saveAnnotationsToFile();

    public abstract int getBookWidth();

    public abstract int getBookHeight();

    public void saveSettings() {

    }

    public void updateRendering() {

    }

    public abstract void cleanImageMatrix();

    public void checkReadingTimer() {
        long timeout = System.currentTimeMillis() - readTimeStart;
        if (AppState.get().remindRestTime != -1 && timeout >= TimeUnit.MINUTES.toMillis(AppState.get().remindRestTime)) {
            /*AlertDialogs.showOkDialog(activity, getString(R.string.remind_msg), new Runnable() {

                @Override
                public void run() {
                    readTimeStart = System.currentTimeMillis();
                }
            });*/
        }
    }

    public boolean isEasyMode() {
        return AppState.get().isAlwaysOpenAsMagazine;
    }

    public void onResume() {
        readTimeStart = System.currentTimeMillis();
    }

    public Bitmap getBookImage() {
        String url = IMG.toUrl(getCurrentBook().getPath(), ImageExtractor.COVER_PAGE_WITH_EFFECT, IMG.getImageSize());
        return ImageLoader.getInstance().loadImageSync(url, IMG.displayCacheMemoryDisc);
    }

    public FileMeta getBookFileMeta() {
        return new FileMeta(getCurrentBook().getPath());
    }

    public String getBookFileMetaName() {
        return TxtUtils.getFileMetaBookName(getBookFileMeta());
    }

    public void loadOutline(final ResultResponse<List<OutlineLinkWrapper>> resultTop) {
        getOutline(new ResultResponse<List<OutlineLinkWrapper>>() {

            @Override
            public boolean onResultRecive(List<OutlineLinkWrapper> result) {
                outline = result;
                if (resultTop != null) {
                    resultTop.onResultRecive(result);
                }
                return false;
            }
        }, false);
    }

    List<OutlineLinkWrapper> outline;

    public void setOutline(List<OutlineLinkWrapper> outline) {
        this.outline = outline;
    }

    public abstract boolean isCropCurrentBook();

    public float getOffsetX() {
        return -1;
    }

    public float getOffsetY() {
        return -1;
    }

    public void onGoToPage(final int page, final float offsetX, final float offsetY) {

    }

    public void addRecent(final Uri uri) {
        //AppDB.get().addRecent(uri.getPath());
        // AppSharedPreferences.get().addRecent(uri);
    }

    public void onClickTop() {

    }

    public String getString(int resId) {
        return activity.getString(resId);
    }

    public void onLinkHistory() {
        if (!getLinkHistory().isEmpty()) {
            final int last = getLinkHistory().removeLast();
            onScrollY(last);
            if (Config.SHOW_LOG)
                LOGGER.info("onLinkHistory: {}", last);

        }
    }

    public static void runFullScreen(final Activity a) {
        try {
            a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
//            Keyboards.hideNavigation(a);
        } catch (Exception e) {
            LOGGER.error("Error ren full screen: {}", e.getMessage(), e);
        }
    }

    public static void runNormalScreen(final Activity a) {
        try {
            a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            final View decorView = a.getWindow().getDecorView();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }
        } catch (Exception e) {
            LOGGER.error("Error ren normal screen: {}", e.getMessage(), e);
        }
    }

    public static void chooseFullScreen(final Activity a, final boolean isFullscren) {
        if (isFullscren) {
            runFullScreen(a);
        } else {
            runNormalScreen(a);
        }
    }

    public void restartActivity() {

        IMG.clearMemoryCache();
        saveAppState();

        Safe.run(new Runnable() {

            @Override
            public void run() {
                ImageExtractor.clearCodeDocument();
                activity.finish();
                activity.startActivity(activity.getIntent());
            }
        });
    }

    public void saveAppState() {
        AppState.get().save(activity);
    }

    public abstract String getTextForPage(int page);

    public abstract List<PageLink> getLinksForPage(int page);
    public abstract void doSearch(String text, ResultResponse<Integer> result);

    public Activity getActivity() {
        return activity;
    }

    public void toast(final String text) {
        Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
    }

    public boolean showContent(final ListView contentList) {
        return false;
    }

    public File getCurrentBook() {
        return currentBook;
    }

    public void setCurrentBook(final File currentBook) {
        this.currentBook = currentBook;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public LinkedList<Integer> getLinkHistory() {
        return linkHistory;
    }

    public void toPageDialog() {

    }

    public abstract void alignDocument();

    public abstract void centerHorizontal();

    public void recyclePage(int page) {

    }

}
