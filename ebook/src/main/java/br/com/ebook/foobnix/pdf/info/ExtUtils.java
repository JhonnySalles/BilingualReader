package br.com.ebook.foobnix.pdf.info;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.webkit.MimeTypeMap;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.ebookdroid.BookType;
import org.ebookdroid.common.cache.CacheManager;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.com.ebook.foobnix.android.utils.Safe;
import br.com.ebook.foobnix.android.utils.TxtUtils;
import br.com.ebook.foobnix.ext.CacheZipUtils;
import br.com.ebook.universalimageloader.core.ImageLoader;
import br.com.ebook.R;
import br.com.ebook.foobnix.android.utils.Apps;
import br.com.ebook.foobnix.android.utils.Dips;
import br.com.ebook.foobnix.android.utils.LOG;
import br.com.ebook.foobnix.pdf.info.model.BookCSS;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;

public class ExtUtils {

    private static final String IMAGE_PNG_BASE64 = "image/png;base64,";
    private static final String IMAGE_JPEG_BASE64 = "image/jpeg;base64,";
    public static final String REFLOW_EPUB = "-reflow.epub";
    public static final String REFLOW_HTML = "-reflow.html";
    private static final String IMAGE_BEGIN = "<image-begin>";
    private static final String IMAGE_END = "<image-end>";

    public static ExecutorService ES = Executors.newFixedThreadPool(4);

    public final static List<String> otherExts = Arrays.asList(AppState.OTHER_BOOK_EXT);
    public final static List<String> lirbeExt = Arrays.asList(AppState.LIBRE_EXT);
    public final static List<String> imageExts = Arrays.asList(".png", ".jpg", ".jpeg", ".gif");
    public final static List<String> imageMimes = Arrays.asList("image/png", "image/jpg", "image/jpeg", "image/gif");
    public final static List<String> archiveExts = Arrays.asList(AppState.OTHER_ARCH_EXT);
    public final static List<String> browseExts = BookType.getAllSupportedExtensions();
    public static Map<String, String> mimeCache = new HashMap<String, String>();

    static {
        browseExts.addAll(otherExts);
        browseExts.addAll(archiveExts);
        browseExts.addAll(imageExts);
        browseExts.addAll(lirbeExt);
        browseExts.add(".json");
        browseExts.addAll(BookCSS.fontExts);
        browseExts.addAll(Arrays.asList(AppState.OTHER_BOOK_MEDIA));

        mimeCache.put(".tpz", "application/x-topaz-ebook");
        mimeCache.put(".azw1", "application/x-topaz-ebook");

        mimeCache.put(".pgn", " application/x-chess-pgn");

        mimeCache.put(".jpeg", "image/jpeg");
        mimeCache.put(".jpg", "image/jpeg");
        mimeCache.put(".png", "image/png");

        mimeCache.put(".chm", "application/x-chm");
        mimeCache.put(".xps", "application/vnd.ms-xpsdocument");
        mimeCache.put(".chm", "application/x-chm");
        mimeCache.put(".lit", "application/x-ms-reader");

        mimeCache.put(".doc", "application/msword");
        mimeCache.put(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        mimeCache.put(".ppt", "application/vnd.ms-powerpoint");
        mimeCache.put(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");

        mimeCache.put(".odt", "application/vnd.oasis.opendocument.text");
        mimeCache.put(".odp", "application/vnd.oasis.opendocument.presentation");

        mimeCache.put(".gz", "application/x-gzip");
        mimeCache.put(".zip", "application/x-compressed-zip");
        mimeCache.put(".rar", "application/x-rar-compressed");

        mimeCache.put(".cbr", "application/x-cbr");
        mimeCache.put(".cbt", "application/x-cbr");
        mimeCache.put(".cb7", "application/x-cbr");

        mimeCache.put(".mp3", "audio/mpeg");
        mimeCache.put(".mp4", "audio/mp4");
        mimeCache.put(".wav", "audio/vnd.wav");
        mimeCache.put(".ogg", "audio/ogg");
        mimeCache.put(".m4a", "audio/m4a");

        mimeCache.put(".m3u8", "application/x-mpegURL");
        mimeCache.put(".ts", "video/MP2T");

        mimeCache.put(".flv", "video/x-flv");
        mimeCache.put(".mp4", "video/mp4");
        mimeCache.put(".m4v", "video/x-m4v");
        mimeCache.put(".3gp", "video/3gpp");
        mimeCache.put(".mov", "video/quicktime");
        mimeCache.put(".avi", "video/x-msvideo");
        mimeCache.put(".wmv", "video/x-ms-wmv");
        mimeCache.put(".mp4", "video/mp4");
        mimeCache.put(".webm", "video/webm");
    }

    static List<String> audio = Arrays.asList(".mp3", ".mp4", ".wav", ".ogg", ".m4a");
    static List<String> video = Arrays.asList(".webm", ".m3u8", ".ts", ".flv", ".mp4", ".3gp", ".mov", ".avi", ".wmv", ".mp4", ".m4v");

    public static void openFile(Activity a, File file) {
        if (ExtUtils.doifFileExists(a, file)) {

            if (ExtUtils.isZip(file)) {

                if (CacheZipUtils.isSingleAndSupportEntryFile(file).first) {
                    ExtUtils.showDocument(a, file);
                }
            } else if (ExtUtils.isNotSupportedFile(file)) {
                ExtUtils.openWith(a, file);
            } else {
                ExtUtils.showDocument(a, file);
            }
        }
    }

    public static boolean isMediaContent(String path) {
        if (TxtUtils.isEmpty(path)) {
            return false;
        }
        path = path.trim().toLowerCase();

        for (String ext : audio) {
            if (path.endsWith(ext)) {
                return true;
            }
        }
        for (String ext : video) {
            if (path.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public static String upperCaseFirst(String text) {
        if (text.length() >= 1) {
            text = text.trim();
            text = text.substring(0, 1).toUpperCase() + text.substring(1);
        }
        return text;
    }

    public static boolean isNotSupportedFile(File file) {
        return !BookType.isSupportedExtByPath(file.getPath());
    }

    public static boolean isImageOrEpub(File file) {
        return ExtUtils.isImageFile(file) || ExtUtils.isFileArchive(file) || BookType.EPUB.is(file.getPath());
    }

    public static boolean isNoTextLayerForamt(String name) {
        return BookType.DJVU.is(name) || BookType.CBR.is(name) || BookType.CBZ.is(name) || BookType.TIFF.is(name);
    }

    public static String getMimeTypeByUri(Uri uri) {
        String mimeType = null;

        try {
            if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT) && context != null) {
                ContentResolver cr = context.getContentResolver();
                mimeType = cr.getType(uri);
            }
            if (mimeType == null) {
                String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.getPath());
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
            }
        } catch (Exception e) {
            LOG.e(e);
        }

        return mimeType;
    }

    public static boolean isImageFile(File file) {
        if (file != null && file.isFile()) {
            return isImagePath(file.getName());
        }
        return false;
    }

    public static boolean isImagePath(String path) {
        if (path == null) {
            return false;
        }
        String name = path.toLowerCase(Locale.US);
        for (String ext : imageExts) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isImageMime(String mime) {
        if (mime == null) {
            return false;
        }
        mime = mime.toLowerCase(Locale.US);
        for (String ext : imageMimes) {
            if (ext.equals(mime)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLibreFile(File file) {
        if (file != null && file.isFile()) {
            String name = file.getName().toLowerCase();
            for (String ext : lirbeExt) {
                if (name.endsWith(ext)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isOtherFile(File file) {
        if (file != null && file.isFile()) {
            String name = file.getName().toLowerCase();
            for (String ext : otherExts) {
                if (name.endsWith(ext)) {
                    return true;
                }
            }
        }
        return false;

    }

    public static boolean isFileArchive(String name) {
        if (name == null) {
            return false;
        }
        name = name.toLowerCase(Locale.US);
        for (String ext : archiveExts) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFileArchive(File file) {
        if (file != null && file.isFile()) {
            String name = file.getName().toLowerCase();
            for (String ext : archiveExts) {
                if (name.endsWith(ext)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isFontFile(String name) {
        name = name.toLowerCase(Locale.US);
        for (String ext : BookCSS.fontExts) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> seachExts = new ArrayList<String>();

    private static java.text.DateFormat dateFormat;

    public static void init(Context c) {
        context = c;

        dateFormat = DateFormat.getDateFormat(c);
        updateSearchExts();
    }

    public static String getFileExtension(File file) {
        return getFileExtension(file.getName());
    }

    public static String getFileExtension(String name) {
        LOG.d("getFileExtension 1", name);
        if (name == null) {
            return "";
        }
        if (name.contains("/")) {
            name = name.substring(name.lastIndexOf("/") + 1);
        }
        if (!name.contains(".")) {
            return "";
        }

        try {
            return name.substring(name.lastIndexOf(".") + 1);
        } catch (Exception e) {
            return "";
        }
    }

    public static String getFileNameWithoutExt(String name) {
        if (!name.contains(".")) {
            return name;
        }
        return name.substring(0, name.lastIndexOf("."));
    }

    public static String getFileName(String name) {
        if (!name.contains("/")) {
            return name;
        }
        try {
            return name.substring(name.lastIndexOf("/") + 1);
        } catch (Exception e) {
            return name;
        }
    }

    public static void updateSearchExts() {
        List<String> result = new ArrayList<String>();
        seachExts.clear();

        if (AppState.get().supportPDF) {
            result.add(".pdf");
        }
        if (AppState.get().supportXPS) {
            result.add(".xps");
        }

        if (AppState.get().supportEPUB) {
            result.add(".epub");
        }

        if (AppState.get().supportDJVU) {
            result.add(".djvu");
        }
        if (AppState.get().supportFB2) {
            result.add(".fb2");
            if (!AppState.get().supportZIP) {
                result.add(".fb2.zip");
            }
        }
        if (AppState.get().supportTXT) {
            result.add(".txt");
            result.add(".html");
            result.add(".xhtml");
            if (!AppState.get().supportZIP) {
                result.add(".txt.zip");
            }
        }
        if (AppState.get().supportRTF) {
            result.add(".rtf");
            if (!AppState.get().supportZIP) {
                result.add(".rtf.zip");
            }
        }
        if (AppState.get().supportMOBI) {
            result.add(".mobi");
            result.add(".azw");
            result.add(".azw3");
        }
        if (AppState.get().supportCBZ) {
            result.add(".cbz");
            result.add(".cbr");
        }
        if (AppState.get().supportZIP) {
            result.addAll(archiveExts);
        }
        if (AppState.get().supportOther) {
            result.addAll(otherExts);
            result.addAll(lirbeExt);
        }

        for (String ext : result) {
            seachExts.add(ext);
            // seachExts.add(ext.toUpperCase(Locale.US));
        }

    }

    public static FileFilter getFileFilter() {
        return filter;
    }

    private static Context context;

    public static boolean doifFileExists(Context c, File file) {
        if (file != null && file.isFile()) {
            return true;
        }
        if (c != null) {
            Toast.makeText(c, "Arquivo não encontrado" + " " + file.getPath(), Toast.LENGTH_LONG).show();
        }
        return false;

    }

    public static boolean doifFileExists(Context c, String path) {
        return doifFileExists(c, new File(path));
    }

    public static boolean isTextFomat(Intent intent) {
        if (intent == null || intent.getData() == null || intent.getData().getPath() == null) {
            LOG.d("isTextFomat", "intent or data or path is null");
            return false;
        }
        return isTextFomat(intent.getData().getPath());
    }

    public static synchronized boolean isTextFomat(String path) {
        if (path == null) {
            return false;
        }
        return BookType.ZIP.is(path) || BookType.EPUB.is(path) || BookType.FB2.is(path) || BookType.TXT.is(path) || BookType.RTF.is(path) || BookType.HTML.is(path) || BookType.MOBI.is(path);
    }

    public static synchronized boolean isZip(File path) {
        return isZip(path.getPath());
    }

    public static synchronized boolean isZip(String path) {
        if (path == null) {
            return false;
        }
        return path.toLowerCase(Locale.US).endsWith(".zip");
    }

    public static synchronized boolean isNoMetaFomat(String path) {
        if (path == null) {
            return false;
        }
        return BookType.TXT.is(path) || BookType.RTF.is(path) || BookType.HTML.is(path) || BookType.PDF.is(path) || BookType.DJVU.is(path) || BookType.CBZ.is(path);
    }

    public static String getDateFormat(File file) {
        return dateFormat.format(file.lastModified());
    }

    public static String readableFileSize(long size) {
        if (size <= 0)
            return "0";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0").format(size / Math.pow(1024, digitGroups)) + "" + units[digitGroups];
    }

    private static FileFilter filter = new FileFilter() {
        @Override
        public boolean accept(final File pathname) {
            for (final String s : browseExts) {
                if (pathname.getName().endsWith(s)) {
                    return true;
                }
            }
            return pathname.isDirectory();
        }
    };

    public static boolean isNotValidFile(final File file) {
        return !isValidFile(file);
    }

    public static boolean isValidFile(final File file) {
        return file != null && file.isFile();
    }

    public static boolean isValidFile(final String path) {
        return path != null && isValidFile(new File(path));
    }

    public static boolean isValidFile(final Uri uri) {
        return uri != null && isValidFile(uri.getPath());
    }

    public static boolean showDocument(final Context c, final File file) {
        return showDocument(c, file, -1);
    }

    public static boolean showDocument(final Context c, final File file, final int page) {

        ImageLoader.getInstance().clearAllTasks();

        if (AppState.get().isRememberMode) {
            showDocumentWithoutDialog(c, file, page);
            return true;
        }

        /*View view = LayoutInflater.from(c).inflate(R.layout.choose_mode_dialog, null, false);

        final TextView vertical = (TextView) view.findViewById(R.id.vertical);
        final TextView horizontal = (TextView) view.findViewById(R.id.horizontal);
        final TextView music = (TextView) view.findViewById(R.id.music);

        final EditText verticalEdit = (EditText) view.findViewById(R.id.verticalEdit);
        final EditText horizontalEdit = (EditText) view.findViewById(R.id.horizontalEdit);
        final EditText musicEdit = (EditText) view.findViewById(R.id.musicEdit);

        verticalEdit.setText(AppState.get().nameVerticalMode);
        horizontalEdit.setText(AppState.get().nameHorizontalMode);
        musicEdit.setText(AppState.get().nameMusicianMode);

        vertical.setText(AppState.get().nameVerticalMode);
        horizontal.setText(AppState.get().nameHorizontalMode);
        music.setText(AppState.get().nameMusicianMode);

        Views.gone(verticalEdit, horizontalEdit, musicEdit);

        final TextView editNames = (TextView) view.findViewById(R.id.editNames);
        TxtUtils.underlineTextView(editNames);

        editNames.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                AppState.get().nameVerticalMode = c.getString(R.string.mode_vertical);
                AppState.get().nameHorizontalMode = c.getString(R.string.mode_horizontally);
                AppState.get().nameMusicianMode = c.getString(R.string.mode_musician);

                verticalEdit.setText(AppState.get().nameVerticalMode);
                horizontalEdit.setText(AppState.get().nameHorizontalMode);
                musicEdit.setText(AppState.get().nameMusicianMode);

                vertical.setText(AppState.get().nameVerticalMode);
                horizontal.setText(AppState.get().nameHorizontalMode);
                music.setText(AppState.get().nameMusicianMode);

                AppState.get().save(c);

                return true;
            }
        });

        editNames.setOnClickListener(new View.OnClickListener() {
            boolean isEdit = true;

            @Override
            public void onClick(View v) {

                String vText = verticalEdit.getText().toString().trim();
                String hText = horizontalEdit.getText().toString().trim();
                String mText = musicEdit.getText().toString().trim();

                if (TxtUtils.isEmpty(vText)) {
                    verticalEdit.setSelected(true);
                    Toast.makeText(c, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TxtUtils.isEmpty(hText)) {
                    horizontalEdit.setSelected(true);
                    Toast.makeText(c, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TxtUtils.isEmpty(mText)) {
                    musicEdit.setSelected(true);
                    Toast.makeText(c, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isEdit) { // edit
                    editNames.setText(R.string.save);
                    Views.visible(verticalEdit, horizontalEdit, musicEdit);
                    Views.gone(vertical, horizontal, music);

                    AppState.get().save(c);

                } else { // text view
                    editNames.setText(R.string.edit_names);
                    Views.visible(vertical, horizontal, music);
                    Views.gone(verticalEdit, horizontalEdit, musicEdit);
                }

                AppState.get().nameVerticalMode = vText;
                AppState.get().nameHorizontalMode = hText;
                AppState.get().nameMusicianMode = mText;

                //Keyboards.close(v);

                verticalEdit.setText(AppState.get().nameVerticalMode);
                horizontalEdit.setText(AppState.get().nameHorizontalMode);
                musicEdit.setText(AppState.get().nameMusicianMode);

                vertical.setText(AppState.get().nameVerticalMode);
                horizontal.setText(AppState.get().nameHorizontalMode);
                music.setText(AppState.get().nameMusicianMode);

                TxtUtils.underlineTextView(editNames);
                isEdit = !isEdit;
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(R.string.select_the_reading_mode);
        builder.setView(view);
        builder.setCancelable(true);
        final AlertDialog dialog = builder.show();

        vertical.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
                AppState.get().isAlwaysOpenAsMagazine = false;
                AppState.get().isMusicianMode = false;
                showDocumentWithoutDialog(c, file, page);
            }
        });
        horizontal.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
                AppState.get().isAlwaysOpenAsMagazine = true;
                AppState.get().isMusicianMode = false;
                showDocumentWithoutDialog(c, file, page);
            }
        });

        music.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
                AppState.get().isAlwaysOpenAsMagazine = false;
                AppState.get().isMusicianMode = true;
                showDocumentWithoutDialog(c, file, page);
            }
        });

        if (Dips.isEInk(c)) {
            view.findViewById(R.id.music).setVisibility(View.GONE);
        }
        CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkBoxRemember);
        checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AppState.get().isRememberMode = isChecked;
            }
        });*/

        return true;

    }

    public static void showDocumentWithoutDialog(final Context c, final File file, final int page) {
        showDocument(c, Uri.fromFile(file), page);
    }

    public static boolean showDocument(final Activity c, final Uri uri) {
        String filePath = CacheManager.getFilePathFromAttachmentIfNeed(c);
        if (TxtUtils.isEmpty(filePath) && uri != null && uri.getPath() != null) {
            filePath = uri.getPath();
        }
        // MetaCache.get().getOrCreateByPath(filePath);
        return showDocument(c, new File(filePath), -1);
    }

    public static void showDocument(final Context c, final Uri uri, final int page) {
        Safe.run(new Runnable() {

            @Override
            public void run() {
                showDocumentInner(c, uri, page);
            }
        });

    }

    public static void showDocumentInner(final Context c, final Uri uri, final int page) {
        if (!isValidFile(uri)) {
            Toast.makeText(c, "Arquivo não encontrado", Toast.LENGTH_LONG).show();
            return;
        }
        LOG.d("showDocument", uri.getPath());

        if (AppState.get().isAlwaysOpenAsMagazine) {
            openHorizontalView(c, new File(uri.getPath()), page - 1);
            return;
        }

    }

    private static void openHorizontalView(final Context c, final File file, final int page) {
        if (file == null) {
            Toast.makeText(c, "Arquivo não encontrado", Toast.LENGTH_LONG).show();
            return;
        }
        if (!isValidFile(file.getPath())) {
            Toast.makeText(c, "Arquivo não encontrado", Toast.LENGTH_LONG).show();
            return;
        }

    }

    public static Intent createOpenFileIntent(Context context, File file) {
        String extension = extensionFromName(file.getName());
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (mimeType == null) {
            // If android doesn't know extension we can check our own list.
            mimeType = getMimeType(file);
        }

        Intent openIntent = new Intent();
        openIntent.setAction(android.content.Intent.ACTION_VIEW);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // openIntent.setDataAndType(getUriProvider(context, file), mimeType);
        openIntent.setDataAndType(getUriProvider(context, file), mimeType);
        // LOG.d("getUriProvider2", getUriProvider(context, file));
        // LOG.d("getUriProvider2", Uri.fromFile(file));

        // 1. Check if there is a default app opener for this type of content.
        final PackageManager packageManager = context.getPackageManager();
        ResolveInfo defaultAppInfo = packageManager.resolveActivity(openIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (!defaultAppInfo.activityInfo.name.endsWith("ResolverActivity")) {
            return openIntent;
        }

        // 2. Retrieve all apps for our intent. If there are no apps - return usual
        // already created intent.
        List<Intent> targetedOpenIntents = new ArrayList<Intent>();
        List<ResolveInfo> appInfoList = packageManager.queryIntentActivities(openIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (appInfoList.isEmpty()) {
            return openIntent;
        }

        // 3. Sort in alphabetical order, filter itself and create intent with the rest
        // of the apps.
        Collections.sort(appInfoList, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo first, ResolveInfo second) {
                String firstName = packageManager.getApplicationLabel(first.activityInfo.applicationInfo).toString();
                String secondName = packageManager.getApplicationLabel(second.activityInfo.applicationInfo).toString();
                return firstName.compareToIgnoreCase(secondName);
            }
        });
        for (ResolveInfo appInfo : appInfoList) {
            String packageName = appInfo.activityInfo.packageName;
            if (packageName.equals(context.getPackageName())) {
                continue;
            }

            Intent targetedOpenIntent = new Intent(android.content.Intent.ACTION_VIEW);
            targetedOpenIntent.setDataAndType(getUriProvider(context, file), mimeType);
            targetedOpenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            targetedOpenIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            targetedOpenIntent.setPackage(packageName);

            targetedOpenIntents.add(targetedOpenIntent);
        }
        Intent remove = targetedOpenIntents.remove(targetedOpenIntents.size() - 1);
        Intent createChooser = Intent.createChooser(remove, "Selecione");
        Intent chooserIntent = createChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedOpenIntents.toArray(new Parcelable[]{}));

        return chooserIntent;
    }

    public static String extensionFromName(String fileName) {
        int dotPosition = fileName.lastIndexOf('.');

        // If extension not present or empty
        if (dotPosition == -1 || dotPosition == fileName.length() - 1) {
            return "";
        } else {
            return fileName.substring(dotPosition + 1).toLowerCase(Locale.getDefault());
        }
    }

    public static void openWith(final Context a, final File file) {
        a.startActivity(createOpenFileIntent(a, file));
    }

    public static Uri getUriProvider(Context a, File file) {
        Uri uriForFile = null;
        // if (Apps.getTargetSdkVersion(a) >= 24) {
        // if (Apps.getTargetSdkVersion(a) >= 24) {
        if (Build.VERSION.SDK_INT >= 24) {
            uriForFile = FileProvider.getUriForFile(a, Apps.getPackageName(a) + ".provider", file);
        } else {
            uriForFile = Uri.fromFile(file);
        }
        LOG.d("getUriProvider", uriForFile);
        return uriForFile;
    }

    public static String getMimeType(File file) {
        String mime = "";
        try {
            String name = file.getName().toLowerCase();
            String ext = getFileExtension(name);

            String mimeType = mimeCache.get("." + ext);
            if (mimeType != null) {
                mime = mimeType;
            } else {
                BookType codecType = BookType.getByUri(name);
                mime = codecType.getFirstMimeTime();
            }
        } catch (Exception e) {
            mime = "application/" + ExtUtils.getFileExtension(file);
        }
        LOG.d("getMimeType", mime);
        return mime;
    }

    public static String determineEncoding(InputStream fis) {
        String encoding = null;
        try {
            UniversalDetector detector = new UniversalDetector(null);

            int nread;
            byte[] buf = new byte[1024];
            while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, nread);
            }
            detector.dataEnd();

            encoding = detector.getDetectedCharset();
            detector.reset();
            fis.close();

            LOG.d("File Encoding", encoding);

        } catch (Exception e) {
            LOG.e(e);
        }
        return encoding == null ? "UTF-8" : encoding;
    }

}
