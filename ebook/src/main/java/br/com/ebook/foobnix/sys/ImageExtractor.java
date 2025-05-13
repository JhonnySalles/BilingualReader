package br.com.ebook.foobnix.sys;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Base64;
import android.util.Pair;

import org.ebookdroid.BookType;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.common.bitmaps.RawBitmap;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.crop.PageCropper;
import org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfPasswordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import br.com.ebook.BaseExtractor;
import br.com.ebook.Config;
import br.com.ebook.foobnix.android.utils.Dips;
import br.com.ebook.foobnix.android.utils.Safe;
import br.com.ebook.foobnix.android.utils.TxtUtils;
import br.com.ebook.foobnix.entity.FileMeta;
import br.com.ebook.foobnix.entity.FileMetaCore;
import br.com.ebook.foobnix.ext.CacheZipUtils;
import br.com.ebook.foobnix.ext.CbzCbrExtractor;
import br.com.ebook.foobnix.ext.EbookMeta;
import br.com.ebook.foobnix.ext.EpubExtractor;
import br.com.ebook.foobnix.ext.Fb2Extractor;
import br.com.ebook.foobnix.ext.MobiExtract;
import br.com.ebook.foobnix.ext.RtfExtract;
import br.com.ebook.foobnix.pdf.info.ExtUtils;
import br.com.ebook.foobnix.pdf.info.IMG;
import br.com.ebook.foobnix.pdf.info.PageUrl;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;
import br.com.ebook.foobnix.pdf.info.wrapper.MagicHelper;
import br.com.ebook.universalimageloader.core.download.BaseImageDownloader;
import br.com.ebook.universalimageloader.core.download.ImageDownloader;

public class ImageExtractor implements ImageDownloader {

    static {
        System.loadLibrary("mypdf");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageExtractor.class);

    public static final int COVER_PAGE_WITH_EFFECT = -3;
    public static final int COVER_PAGE_NO_EFFECT = -2;
    public static final int COVER_PAGE = -1;
    private static ImageExtractor instance;
    private final BaseImageDownloader baseImage;
    private final Context c;

    public static SharedPreferences sp;

    public static synchronized ImageExtractor getInstance(final Context c) {
        if (instance == null)
            instance = new ImageExtractor(c);

        sp = c.getSharedPreferences("Errors", Context.MODE_PRIVATE);
        return instance;
    }

    public static void clearErrors() {
        if (sp != null) {
            sp.edit().clear().commit();
        }
    }

    private ImageExtractor(final Context c) {
        this.c = c;
        baseImage = new BaseImageDownloader(c);
    }

    public Bitmap proccessCoverPage(PageUrl pageUrl) {
        String path = pageUrl.getPath();
        if (pageUrl.getHeight() == 0) {
            pageUrl.setHeight((int) (pageUrl.getWidth() * 1.5));
        }

        FileMeta fileMeta = new FileMeta(path);
        EbookMeta ebookMeta = FileMetaCore.get().getEbookMeta(path, CacheZipUtils.CacheDir.ZipApp, false);

        FileMetaCore.get().upadteBasicMeta(fileMeta, new File(path));
        FileMetaCore.get().udpateFullMeta(fileMeta, ebookMeta);

        String unZipPath = ebookMeta.getUnzipPath();

        Bitmap cover = null;

        if (ebookMeta.coverImage != null)
            cover = BaseExtractor.arrayToBitmap(ebookMeta.coverImage, pageUrl.getWidth());
        else if (BookType.EPUB.is(unZipPath))
            cover = BaseExtractor.arrayToBitmap(EpubExtractor.get().getBookCover(unZipPath), pageUrl.getWidth());
        else if (BookType.FB2.is(unZipPath))
            cover = BaseExtractor.arrayToBitmap(Fb2Extractor.get().getBookCover(unZipPath), pageUrl.getWidth());
        else if (BookType.MOBI.is(unZipPath))
            cover = BaseExtractor.arrayToBitmap(MobiExtract.getBookCover(unZipPath), pageUrl.getWidth());
        else if (BookType.RTF.is(unZipPath))
            cover = BaseExtractor.arrayToBitmap(RtfExtract.getImageCover(unZipPath), pageUrl.getWidth());
        else if (BookType.PDF.is(unZipPath) || BookType.DJVU.is(unZipPath) || BookType.TIFF.is(unZipPath))
            cover = proccessOtherPage(pageUrl, fileMeta);
        else if (BookType.CBZ.is(unZipPath))
            cover = BaseExtractor.arrayToBitmap(CbzCbrExtractor.getBookCover(unZipPath), pageUrl.getWidth());
        else if (ExtUtils.isFileArchive(unZipPath)) {
            String ext = ExtUtils.getFileExtension(unZipPath);
            cover = BaseExtractor.getBookCoverWithTitle("...", "  [" + ext.toUpperCase(Locale.getDefault()) + "]", true);
            pageUrl.tempWithWatermakr = true;
        } else if (ExtUtils.isFontFile(unZipPath)) {
            cover = BaseExtractor.getBookCoverWithTitle("font", "", true);
            pageUrl.tempWithWatermakr = true;
        }

        if (cover == null) {
            cover = BaseExtractor.getBookCoverWithTitle(fileMeta.getAuthor(), fileMeta.getTitle(), true);
            pageUrl.tempWithWatermakr = true;
        }

        if (Config.SHOW_LOG)
            LOGGER.info("udpateFullMeta ImageExtractor: {}", fileMeta.getAuthor());
        //AppDB.get().update(fileMeta);

        return cover;
    }

    public InputStream generalCoverWithEffect(PageUrl pageUrl, Bitmap cover) {
        try {
            if (Config.SHOW_LOG)
                LOGGER.info("generalCoverWithEffect: {} - {} --- {} - {}", pageUrl.getWidth(), cover.getWidth(), pageUrl.getHeight(), cover.getHeight());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Bitmap res;
            if (AppState.get().isBookCoverEffect || pageUrl.getPage() == COVER_PAGE_WITH_EFFECT) {
                res = MagicHelper.scaleCenterCrop(cover, pageUrl.getHeight(), pageUrl.getWidth(), !pageUrl.tempWithWatermakr);
                res.compress(CompressFormat.PNG, 90, out);
            } else {
                res = cover;
                res.compress(CompressFormat.JPEG, 90, out);
            }

            byte[] byteArray = out.toByteArray();
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
            res.recycle();
            res = null;

            out.close();
            out = null;
            byteArray = null;
            return byteArrayInputStream;
        } catch (Exception e) {
            LOGGER.error("Error get cover with effect: {}", e.getMessage(), e);
            return null;
        }
    }

    public Bitmap proccessOtherPage(PageUrl pageUrl, FileMeta meta) {
        int page = pageUrl.getPage();
        String path = pageUrl.getPath();

        boolean isNeedDisableMagicInPDFDjvu = false;
        if (Config.SHOW_LOG)
            LOGGER.info("Page Number: {}", pageUrl.getPage());

        if (pageUrl.getPage() == COVER_PAGE || pageUrl.getPage() == COVER_PAGE_NO_EFFECT || pageUrl.getPage() == COVER_PAGE_WITH_EFFECT)
            isNeedDisableMagicInPDFDjvu = true;

        if (page < 0)
            page = 0;

        /*if (pageUrl.isCrop()) {
            isNeedDisableMagicInPDFDjvu = true;
        }*/

        CodecDocument codeCache = null;
        if (isNeedDisableMagicInPDFDjvu) {
            codeCache = singleCodecContext(path, "", pageUrl.getWidth(), pageUrl.getHeight());
            if (meta != null && codeCache != null && FileMetaCore.isNeedToExtractPDFMeta(path)) {
                String bookAuthor = codeCache.getBookAuthor();
                if (TxtUtils.isNotEmpty(bookAuthor))
                    meta.setAuthor(bookAuthor);

                String bookTitle = codeCache.getBookTitle();
                if (TxtUtils.isNotEmpty(bookTitle))
                    meta.setTitle(bookTitle);

                if (Config.SHOW_LOG)
                    LOGGER.info("PDF getBookAuthor: {} - {}", bookAuthor, bookTitle);
            }
        } else {
            codeCache = getNewCodecContext(path, "", pageUrl.getWidth(), pageUrl.getHeight(), AppState.get().fontSizeSp);
        }

        if (codeCache == null) {
            if (Config.SHOW_LOG)
                LOGGER.info("codecDocument == null - {}", path);
            return null;
        }

        final CodecPageInfo pageInfo = codeCache.getPageInfo(page);

        Bitmap bitmap = null;

        RectF rectF = new RectF(0, 0, 1f, 1f);
        final float k = (float) pageInfo.height / pageInfo.width;
        int width = pageUrl.getWidth();
        int height = (int) (width * k);

        if (Config.SHOW_LOG) {
            LOGGER.info("Bitmap: {} - {}", width, height);
            LOGGER.info("Bitmap pageInfo.height: {} - {}", pageInfo.width, pageInfo.height);
        }

        BitmapRef bitmapRef = null;
        CodecPage pageCodec = codeCache.getPage(page);

        if (pageUrl.getNumber() == 0) {
            rectF = new RectF(0, 0, 1f, 1f);
            if (isNeedDisableMagicInPDFDjvu)
                MagicHelper.isNeedMagic = false;

            bitmapRef = pageCodec.renderBitmap(width, height, rectF);

            if (isNeedDisableMagicInPDFDjvu)
                MagicHelper.isNeedMagic = true;

            bitmap = bitmapRef.getBitmap();

            if (pageUrl.isCrop()) {
                // bitmap = getCroppedPage(codecDocumentLocal, page,
                // bitmap).first;
                bitmap = cropBitmap(bitmap);
            }

        } else if (pageUrl.getNumber() == 1) {
            float right = (float) pageUrl.getCutp() / 100;
            rectF = new RectF(0, 0, right, 1f);
            bitmapRef = pageCodec.renderBitmap((int) (width * right), height, rectF);
            bitmap = bitmapRef.getBitmap();

            if (pageUrl.isCrop())
                bitmap = cropBitmap(bitmap);
        } else if (pageUrl.getNumber() == 2) {
            float right = (float) pageUrl.getCutp() / 100;
            rectF = new RectF(right, 0, 1f, 1f);
            bitmapRef = pageCodec.renderBitmap((int) (width * (1 - right)), height, rectF);
            bitmap = bitmapRef.getBitmap();

            if (pageUrl.isCrop())
                bitmap = cropBitmap(bitmap);
        }

        if (pageUrl.isInvert()) {
            final RawBitmap bmp = new RawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()));
            bmp.invert();
            bitmap.recycle();
            bitmap = bmp.toBitmap().getBitmap();
        }

        if (pageUrl.getRotate() > 0) {
            final Matrix matrix = new Matrix();
            matrix.postRotate(pageUrl.getRotate());
            final Bitmap bitmap1 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            bitmap = bitmap1;
        }

        if (!pageCodec.isRecycled())
            pageCodec.recycle();

        if (isNeedDisableMagicInPDFDjvu)
            codeCache.recycle();


        if (!isNeedDisableMagicInPDFDjvu && MagicHelper.isNeedBookBackgroundImage())
            bitmap = MagicHelper.updateWithBackground(bitmap);

        return bitmap;
    }

    public Bitmap cropBitmap(Bitmap bitmap) {
        final Rect rootRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF rectCrop = PageCropper.getCropBounds(bitmap, rootRect, new RectF(0, 0, 1f, 1f));
        int x = (int) (bitmap.getWidth() * rectCrop.left);
        int y = (int) (bitmap.getHeight() * rectCrop.top);
        int w = (int) (bitmap.getWidth() * rectCrop.width());
        int h = (int) (bitmap.getHeight() * rectCrop.height());
        Bitmap bitmap1 = Bitmap.createBitmap(bitmap, x, y, w, h);
        bitmap.recycle();
        return bitmap1;
    }

    @Deprecated
    public Pair<Bitmap, RectF> getCroppedPage(CodecDocument codecDocumentLocal, int page, Bitmap bitmap) {
        RectF rectF = new RectF(0, 0, 1f, 1f);
        final Rect rootRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        rectF = PageCropper.getCropBounds(bitmap, rootRect, rectF);

        float nWidth = bitmap.getWidth() - bitmap.getWidth() * (rectF.left + (1 - rectF.right));
        float nHeiht = bitmap.getHeight() - bitmap.getHeight() * (rectF.top + (1 - rectF.bottom));

        bitmap.recycle();
        codecDocumentLocal.getPage(page).recycle();
        Bitmap result = codecDocumentLocal.getPage(page).renderBitmap((int) nWidth, (int) nHeiht, rectF).getBitmap();
        return new Pair<>(result, rectF);
    }

    @Override
    public synchronized InputStream getStream(final String imageUri, final Object extra) throws IOException {
        return getStreamInner(imageUri);
    }

    public synchronized InputStream getStreamInner(final String imageUri) throws IOException {
        if (Config.SHOW_LOG)
            LOGGER.info("url: {}", imageUri);

        if (imageUri.startsWith(Safe.TXT_SAFE_RUN)) {
            if (Config.SHOW_LOG)
                LOGGER.info("MUPDF! {} {} {}", Safe.TXT_SAFE_RUN, "begin", imageUri);
            // try {
            // Thread.sleep(1500);
            // } catch (InterruptedException e) {
            // LOGGER.error("Error to export all: " + e.getMessage(), e);
            // }
            return baseImage.getStream("assets://opds/web111.png", null);
        }
        if (imageUri.startsWith("data:")) {
            String uri = imageUri;
            // uri = uri.replace("data:image/png;base64,", "");
            // uri = uri.replace("data:image/jpeg;base64,", "");
            // uri = uri.replace("data:image/jpg;base64,", "");
            // uri = uri.replace("data:image/gif;base64,", "");
            uri = uri.substring(uri.indexOf(",") + 1);
            if (Config.SHOW_LOG)
                LOGGER.info("Load image data: {}", uri);
            return new ByteArrayInputStream(Base64.decode(uri, Base64.DEFAULT));
        }

        if (!imageUri.startsWith("{"))
            return baseImage.getStream(imageUri, null);

        if (sp.contains("" + imageUri.hashCode())) {
            if (Config.SHOW_LOG)
                LOGGER.info("Error FILE: {}", imageUri);
            return messageFile("#crash", "");
        }

        final PageUrl pageUrl = PageUrl.fromString(imageUri);
        String path = pageUrl.getPath();

        File file = new File(path);
        try {

            if (ExtUtils.isImageFile(file)) {
                FileMeta fileMeta = new FileMeta(path);
                FileMetaCore.get().upadteBasicMeta(fileMeta, new File(path));
                //AppDB.get().update(fileMeta);

                return BaseExtractor.decodeImage(path, IMG.getImageSize());
            }

            if (path.endsWith("json")) {
                FileMeta fileMeta = new FileMeta(path);
                FileMetaCore.get().upadteBasicMeta(fileMeta, new File(path));
                //AppDB.get().update(fileMeta);
                return messageFile("#json", "");
            }

            if (!file.isFile()) {
                return messageFile("#no file", "");
            }
            sp.edit().putBoolean("" + imageUri.hashCode(), true).commit();

            int page = pageUrl.getPage();

            if (pageUrl.getHeight() == 0) {
                pageUrl.setHeight((int) (pageUrl.getWidth() * 1.5));
            }

            if (page == COVER_PAGE || page == COVER_PAGE_WITH_EFFECT) {
                try {
                    MagicHelper.isNeedBC = false;
                    Bitmap proccessCoverPage = proccessCoverPage(pageUrl);
                    return generalCoverWithEffect(pageUrl, proccessCoverPage);
                } finally {
                    MagicHelper.isNeedBC = true;
                }
            } else if (page == COVER_PAGE_NO_EFFECT) {
                ByteArrayInputStream bitmapToStream = bitmapToStream(proccessCoverPage(pageUrl));
                return bitmapToStream;
            } else {
                if (pageUrl.isDouble()) {
                    if (Config.SHOW_LOG)
                        LOGGER.error("isDouble: {} - {}", pageUrl.getHeight(), pageUrl.getWidth());

                    if (AppState.get().isDoubleCoverAlone)
                        pageUrl.setPage(pageUrl.getPage() - 1);

                    Bitmap bitmap1 = proccessOtherPage(pageUrl, null);
                    pageUrl.setPage(pageUrl.getPage() + 1);

                    Bitmap bitmap2 = null;
                    if (pageUrl.getPage() < pageCount)
                        bitmap2 = proccessOtherPage(pageUrl, null);
                    else {
                        bitmap2 = Bitmap.createBitmap(bitmap1);
                        Canvas canvas = new Canvas(bitmap2);
                        canvas.drawColor(Color.WHITE);
                    }

                    int maxH = Math.max(bitmap1.getHeight(), bitmap2.getHeight());
                    Bitmap bitmap = Bitmap.createBitmap(bitmap1.getWidth() + bitmap2.getWidth(), maxH, Bitmap.Config.RGB_565);
                    Canvas canvas = new Canvas(bitmap);
                    canvas.drawColor(MagicHelper.getBgColor());

                    if (AppState.get().isCutRTL) {
                        canvas.drawBitmap(bitmap2, 0, (maxH - bitmap2.getHeight()) / 2, null);
                        canvas.drawBitmap(bitmap1, bitmap2.getWidth(), (maxH - bitmap1.getHeight()) / 2, null);
                    } else {
                        canvas.drawBitmap(bitmap1, 0, (maxH - bitmap1.getHeight()) / 2, null);
                        canvas.drawBitmap(bitmap2, bitmap1.getWidth(), (maxH - bitmap2.getHeight()) / 2, null);
                    }

                    bitmap1.recycle();
                    bitmap2.recycle();
                    return bitmapToStreamRAW(bitmap);

                }

                return bitmapToStreamRAW(proccessOtherPage(pageUrl, null));
            }

        } catch (MuPdfPasswordException e) {
            return messageFile("#password", file.getName());
        } catch (final Exception e) {
            LOGGER.error("Error get stream inner: {}", e.getMessage(), e);
            return messageFile("#error", "");
        } catch (OutOfMemoryError e2) {
            AppState.get().pagesInMemory = 1;
            return messageFile("#error", "");
        } finally {
            sp.edit().remove("" + imageUri.hashCode()).commit();
        }
    }

    private ByteArrayInputStream bitmapToStream(Bitmap bitmap) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            boolean isJPG = AppState.get().imageFormat.equals(AppState.JPG);

            CompressFormat format = isJPG ? CompressFormat.JPEG : CompressFormat.PNG;
            int quality = isJPG ? 80 : 100;
            bitmap.compress(format, quality, os);

            byte[] byteArray = os.toByteArray();
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
            bitmap.recycle();
            bitmap = null;

            os.close();
            os = null;
            byteArray = null;

            return byteArrayInputStream;
        } catch (Exception e) {
            if (Config.SHOW_LOG)
                LOGGER.error("Error bitmap to stream: {}", e.getMessage(), e);
            return null;
        }
    }

    private InputStream bitmapToStreamRAW(Bitmap bitmap) {
        try {
            if (Config.SHOW_LOG)
                LOGGER.error("Rerurn bitmapToStreamRAW");
            return new InputStreamBitmap(bitmap);
            // return bitmapToStream(bitmap);
        } catch (Exception e) {
            if (Config.SHOW_LOG)
                LOGGER.error("Error bitmap to stream raw: {}", e.getMessage(), e);
            return null;
        }
    }

    static int pageCount = 0;

    public static volatile CodecDocument codeCache;
    public static volatile CodecContext codecContex;
    public static String pathCache;
    static int whCache;

    public static synchronized void clearCodeDocument() {
        if (codeCache != null) {
            codeCache.recycle();
            codeCache = null;
            pathCache = null;
            if (Config.SHOW_LOG)
                LOGGER.error("getNewCodecContext codeCache recycle");
        }
        if (codecContex != null) {
            codecContex.recycle();
            codecContex = null;
            if (Config.SHOW_LOG)
                LOGGER.error("getNewCodecContext codecContex recycle");
        }

        TempHolder.get().clear();

    }

    public static void init(CodecDocument codec, String path) {
        clearCodeDocument();
        codeCache = codec;
        pathCache = path;
    }

    public static synchronized CodecDocument singleCodecContext(final String path, String passw, int w, int h) {
        try {
            CodecContext codecContex = BookType.getCodecContextByPath(path);
            TempHolder.get().init(path);

            if (Config.SHOW_LOG)
                LOGGER.error("CodecContext: {}", codecContex);

            if (codecContex == null)
                return null;

            TempHolder.get().loadingCancelled = false;
            return codecContex.openDocument(path, passw);
        } catch (RuntimeException e) {
            LOGGER.error("Error get single codec context: {}", e.getMessage(), e);
            return null;
        }
    }

    public static synchronized CodecDocument getNewCodecContext(final String path, String passw, int w, int h, int font) {

        if (path.equals(pathCache) /* && whCache == h + w */ && codeCache != null && !codeCache.isRecycled()) {
            if (Config.SHOW_LOG)
                LOGGER.error("getNewCodecContext cache {} : {} - {}", path, w, h);
            return codeCache;
        }

        if (Config.SHOW_LOG)
            LOGGER.error("getNewCodecContext new {} : {} - {}", path, w, h);

        clearCodeDocument();

        pageCount = 0;
        pathCache = null;
        codeCache = null;
        whCache = -1;

        if (w <= 0 || h <= 0) {
            w = Dips.screenWidth();
            h = Dips.screenHeight();
        }

        if (Config.SHOW_LOG)
            LOGGER.error("getNewCodecContext after: {} - {}", w, h);

        codecContex = BookType.getCodecContextByPath(path);
        TempHolder.get().init(path);

        if (Config.SHOW_LOG)
            LOGGER.error("CodecContext: {}", codecContex);

        if (codecContex == null)
            return null;

        // CacheZipUtils.cacheLock.lock();
        // try {
        // String zipPath = CacheZipUtils.extracIfNeed(path, CacheDir.ZipApp).unZipPath;
        // if (Config.SHOW_LOG)
        //     LOGGER.error("getCodecContext: {}", zipPath);
        // codeCache = ctx.openDocument(zipPath, passw);
        // } finally {
        // CacheZipUtils.cacheLock.unlock();
        // }

        TempHolder.get().loadingCancelled = false;
        codeCache = codecContex.openDocument(path, passw);
        if (codeCache == null) {
            if (Config.SHOW_LOG)
                LOGGER.error("[Open doc is null 1: {}", path);
            return null;
        }

        pageCount = codeCache.getPageCount(w, h, font);
        pathCache = path;
        whCache = h + w;
        return codeCache;

    }

    private InputStream messageFile(String msg, String name) {
        return bitmapToStream(BaseExtractor.getBookCoverWithTitle(msg, name, true));
    }

}
