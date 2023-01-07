package br.com.ebook.foobnix.tts;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.KeyEvent;

import androidx.media.session.MediaButtonReceiver;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.greenrobot.eventbus.EventBus;

import br.com.ebook.application.eBookApplication;
import br.com.ebook.foobnix.android.utils.TxtUtils;
import br.com.ebook.R;
import br.com.ebook.foobnix.android.utils.LOG;
import br.com.ebook.foobnix.android.utils.Vibro;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;
import br.com.ebook.foobnix.sys.ImageExtractor;
import br.com.ebook.foobnix.sys.TempHolder;

public class TTSService extends Service {

    public static final String EXTRA_PATH = "EXTRA_PATH";
    public static final String EXTRA_ANCHOR = "EXTRA_ANCHOR";
    public static final String EXTRA_INT = "INT";

    private static final String TAG = "TTSService";

    public static String ACTION_PLAY_CURRENT_PAGE = "ACTION_PLAY_CURRENT_PAGE";
    private WakeLock wakeLock;

    public TTSService() {
        LOG.d(TAG, "Create constructor");
    }

    int width;
    int height;

    AudioManager mAudioManager;
    MediaSessionCompat mMediaSessionCompat;
    boolean isActivated;

    @Override
    public void onCreate() {
        LOG.d(TAG, "Create");

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TTSService");

        AppState.get().load(getApplicationContext());
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(listener, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN);

        mMediaSessionCompat = new MediaSessionCompat(getApplicationContext(), "Tag");
        mMediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSessionCompat.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent intent) {
                LOG.d(TAG, "onMediaButtonEvent", isActivated, intent);
                if (isActivated) {
                    KeyEvent event = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
                    if (KeyEvent.ACTION_UP == event.getAction() && KeyEvent.KEYCODE_HEADSETHOOK == event.getKeyCode()) {
                        LOG.d(TAG, "onStartStop", "KEYCODE_HEADSETHOOK");
                        boolean isPlaying = TTSEngine.get().isPlaying();
                        if (isPlaying) {
                            TTSEngine.get().stop();
                        } else {
                            playPage("", AppState.get().lastBookPage, null);
                        }
                    }
                }
                return isActivated;
            }

        });

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        mMediaSessionCompat.setMediaButtonReceiver(pendingIntent);

        TTSEngine.get().getTTS();

    }

    boolean isPlaying;
    OnAudioFocusChangeListener listener = new OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(int focusChange) {
            LOG.d("onAudioFocusChange", focusChange);
            if (focusChange < 0) {
                isPlaying = TTSEngine.get().isPlaying();
                LOG.d("onAudioFocusChange", "Is playing", isPlaying);
                TTSEngine.get().stop();
            } else {
                if (isPlaying) {
                    playPage("", AppState.get().lastBookPage, null);
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void playLastBook() {
        playBookPage(AppState.get().lastBookPage, AppState.get().lastBookPath, "", AppState.get().lastBookWidth, AppState.get().lastBookHeight, AppState.get().lastFontSize);
    }

    public static void playBookPage(int page, String path, String anchor, int width, int height, int fontSize) {
        LOG.d(TAG, "playBookPage", page, path, width, height);
        TTSEngine.get().stop();

        AppState.get().lastBookWidth = width;
        AppState.get().lastBookHeight = height;
        AppState.get().lastFontSize = fontSize;

        Intent intent = playBookIntent(page, path, anchor);

        eBookApplication.context.startService(intent);

    }

    private static Intent playBookIntent(int page, String path, String anchor) {
        Intent intent = new Intent(eBookApplication.context, TTSService.class);
        intent.setAction(TTSService.ACTION_PLAY_CURRENT_PAGE);
        intent.putExtra(EXTRA_INT, page);
        intent.putExtra(EXTRA_PATH, path);
        intent.putExtra(EXTRA_ANCHOR, anchor);
        return intent;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent);
        LOG.d(TAG, "onStartCommand", intent);
        if (intent == null) {
            return START_STICKY;
        }
        LOG.d(TAG, "onStartCommand", intent.getAction());
        if (intent.getExtras() != null) {
            LOG.d(TAG, "onStartCommand", intent.getAction(), intent.getExtras());
            for (String key : intent.getExtras().keySet())
                LOG.d(TAG, key, "=>", intent.getExtras().get(key));
        }

        if (TTSNotification.TTS_STOP.equals(intent.getAction())) {
            TTSEngine.get().stop();
            savePage();
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }

        }
        if (TTSNotification.TTS_READ.equals(intent.getAction())) {
            TTSEngine.get().stop();
            playPage("", AppState.get().lastBookPage, null);
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        }
        if (TTSNotification.TTS_NEXT.equals(intent.getAction())) {
            TTSEngine.get().stop();
            playPage("", AppState.get().lastBookPage + 1, null);
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        }

        if (ACTION_PLAY_CURRENT_PAGE.equals(intent.getAction())) {
            mMediaSessionCompat.setActive(true);
            isActivated = true;
            int pageNumber = intent.getIntExtra(EXTRA_INT, -1);
            AppState.get().lastBookPath = intent.getStringExtra(EXTRA_PATH);
            String anchor = intent.getStringExtra(EXTRA_ANCHOR);

            if (pageNumber != -1) {
                playPage("", pageNumber, anchor);
            }
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }

        }

        return START_STICKY;
    }

    CodecDocument cache;
    String path;
    int wh;

    public CodecDocument getDC() {
        try {
            if (AppState.get().lastBookPath != null && AppState.get().lastBookPath.equals(path) && cache != null && wh == AppState.get().lastBookWidth + AppState.get().lastBookHeight) {
                LOG.d(TAG, "CodecDocument from cache", AppState.get().lastBookPath);
                return cache;
            }
            if (cache != null) {
                cache.recycle();
                cache = null;
            }
            path = AppState.get().lastBookPath;
            cache = ImageExtractor.singleCodecContext(AppState.get().lastBookPath, "", AppState.get().lastBookWidth, AppState.get().lastBookHeight);
            cache.getPageCount(AppState.get().lastBookWidth, AppState.get().lastBookHeight, AppState.get().fontSizeSp);
            wh = AppState.get().lastBookWidth + AppState.get().lastBookHeight;
            LOG.d(TAG, "CodecDocument new", AppState.get().lastBookPath, AppState.get().lastBookWidth, AppState.get().lastBookHeight);
            return cache;
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }
    }

    int emptyPageCount = 0;

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void playPage(String preText, int pageNumber, String anchor) {
        if (pageNumber != -1) {
            EventBus.getDefault().post(new MessagePageNumber(pageNumber));
            AppState.get().lastBookPage = pageNumber;
            CodecDocument dc = getDC();
            if (dc == null) {
                LOG.d(TAG, "CodecDocument", "is NULL");
                return;
            }

            int pageCount = dc.getPageCount();
            LOG.d(TAG, "CodecDocument PageCount", pageNumber, pageCount);
            if (pageNumber >= pageCount) {

                TempHolder.get().timerFinishTime = 0;

                Vibro.vibrate(1000);

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
                TTSEngine.get().getTTS().setOnUtteranceCompletedListener(null);
                TTSEngine.get().speek(eBookApplication.context.getString(R.string.the_book_is_over));

                EventBus.getDefault().post(new TtsStatus());
                return;
            }

            CodecPage page = dc.getPage(pageNumber);
            String pageHTML = page.getPageHTML();
            page.recycle();
            pageHTML = TxtUtils.replaceHTMLforTTS(pageHTML);

            if (TxtUtils.isNotEmpty(anchor)) {
                int indexOf = pageHTML.indexOf(anchor);
                if (indexOf > 0) {
                    pageHTML = pageHTML.substring(indexOf);
                    LOG.d("find anchor new text", pageHTML);
                }
            }

            LOG.d(TAG, pageHTML);

            if (TxtUtils.isEmpty(pageHTML)) {
                LOG.d("empty page play next one", emptyPageCount);
                emptyPageCount++;
                if (emptyPageCount < 3) {
                    playPage("", AppState.get().lastBookPage + 1, null);
                }
                return;
            }
            emptyPageCount = 0;

            String[] parts = TxtUtils.getParts(pageHTML);
            String firstPart = parts[0];
            final String secondPart = parts[1];

            if (TxtUtils.isNotEmpty(preText)) {
                preText = TxtUtils.replaceLast(preText, "-", "");
                firstPart = preText + firstPart;
            }

            if (Build.VERSION.SDK_INT >= 15) {
                TTSEngine.get().getTTS().setOnUtteranceProgressListener(new UtteranceProgressListener() {

                    @Override
                    public void onStart(String utteranceId) {

                    }

                    @Override
                    public void onError(String utteranceId) {
                        TTSEngine.get().stop();
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        LOG.d(TAG, "onUtteranceCompleted");
                        if (TempHolder.get().timerFinishTime != 0 && System.currentTimeMillis() > TempHolder.get().timerFinishTime) {
                            LOG.d(TAG, "Timer");
                            TempHolder.get().timerFinishTime = 0;
                            return;
                        }

                        playPage(secondPart, AppState.get().lastBookPage + 1, null);
                        SettingsManager.updateTempPage(AppState.get().lastBookPath, AppState.get().lastBookPage + 1);

                    }
                });
            } else {
                TTSEngine.get().getTTS().setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {

                    @Override
                    public void onUtteranceCompleted(String utteranceId) {
                        LOG.d(TAG, "onUtteranceCompleted");
                        if (TempHolder.get().timerFinishTime != 0 && System.currentTimeMillis() > TempHolder.get().timerFinishTime) {
                            LOG.d(TAG, "Timer");
                            TempHolder.get().timerFinishTime = 0;
                            return;
                        }
                        playPage(secondPart, AppState.get().lastBookPage + 1, null);
                        SettingsManager.updateTempPage(AppState.get().lastBookPath, AppState.get().lastBookPage + 1);

                    }

                });
            }

            TTSNotification.show(AppState.get().lastBookPath, pageNumber + 1);
            TTSEngine.get().speek(firstPart);
            EventBus.getDefault().post(new TtsStatus());

            savePage();

        }
    }

    public void savePage() {
        AppState.get().save(getApplicationContext());

        try {
            BookSettings bs = SettingsManager.getBookSettings(AppState.get().lastBookPath);
            bs.currentPageChanged(AppState.get().lastBookPage);
            bs.save();
            LOG.d(TAG, "currentPageChanged ", AppState.get().lastBookPage, AppState.get().lastBookPath);
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        isActivated = false;
        TempHolder.get().timerFinishTime = 0;
        mMediaSessionCompat.setActive(false);
        if (cache != null) {
            cache.recycle();
        }
        path = null;
        LOG.d(TAG, "onDestroy");
    }

}
