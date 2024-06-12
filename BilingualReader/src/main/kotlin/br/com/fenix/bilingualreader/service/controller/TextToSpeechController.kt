package br.com.fenix.bilingualreader.service.controller

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Speech
import br.com.fenix.bilingualreader.model.enums.AudioStatus
import br.com.fenix.bilingualreader.model.enums.TextSpeech
import br.com.fenix.bilingualreader.service.listener.TTSListener
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.services.NotificationBroadcastReceiver
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Notifications
import br.com.fenix.bilingualreader.util.helpers.TextUtil
import io.github.whitemagic2014.tts.TTS
import io.github.whitemagic2014.tts.TTSVoice
import io.github.whitemagic2014.tts.bean.Voice
import org.slf4j.LoggerFactory
import java.io.File
import java.util.stream.Collectors


class TextToSpeechController(val context: Context, book: Book, parse: DocumentParse?, cover: Bitmap?, val fontSize: Int) : MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    companion object {
        const val LIMIT_CACHE = 3
        const val SHOW_LOG = true
    }

    private var mListener = mutableListOf<TTSListener>()

    private val mLOGGER = LoggerFactory.getLogger(TextToSpeechController::class.java)

    private val mCache: File = File(GeneralConsts.getCacheDir(context), GeneralConsts.CACHE_FOLDER.AUDIO)
    private var mCover: Bitmap? = cover
    private var mParse: DocumentParse? = parse

    private var mBook: String = book.title.replace("[^\\w ]".toRegex(), "")
    private var mFileName: String = book.fileName
    private var mMainHandler = Handler(Looper.getMainLooper())

    private var mVoice: Voice
    private var mVoiceRate = "+0%"
    private var mVoiceVolume = "+0%"

    init {
        val sharedPreferences = GeneralConsts.getSharedPreferences(context)
        val language: TextSpeech = TextSpeech.valueOf(sharedPreferences.getString(GeneralConsts.KEYS.READER.BOOK_READER_TTS, TextSpeech.getDefault().toString())!!)
        val providers = TTSVoice.provides()

        mVoice = if (providers.any { it.shortName.equals(language.getNameAzure(), ignoreCase = true) })
            providers.stream().filter { v: Voice -> v.shortName == language.getNameAzure() }.collect(Collectors.toList())[0]
        else
            providers.first()
    }

    fun addListener(listener: TTSListener) = mListener.add(listener)

    fun removeListener(listener: TTSListener) = mListener.remove(listener)

    fun setVoice(language: TextSpeech, rate: Float = 0f, volume: Int = 0): Boolean {
        return try {
            val providers = TTSVoice.provides()

            if (providers.none { it.shortName.equals(language.getNameAzure(), ignoreCase = true) }) {
                mLOGGER.warn("Voice not found: ${language.name}")
                Toast.makeText(context, context.getString(R.string.tts_voice_not_found), Toast.LENGTH_SHORT).show()
                return false
            }

            val voice = providers.stream().filter { v: Voice -> v.shortName.equals(language.getNameAzure(), ignoreCase = true) }.collect(Collectors.toList())[0]
            val changeVoice = mVoice != voice
            mVoice = voice
            mVoiceRate = "+$rate%"
            mVoiceVolume = "+$volume%"

            if (changeVoice)
                generateCache()
            true
        } catch (e: Exception) {
            mLOGGER.error("Error to set voice.", e)
            false
        }
    }

    fun start(page: Int = 0, initial: String = "") = execute(page, initial)

    fun pause() {
        mPause = !mPause
    }

    fun stop() {
        mStop = true
        setStatus(AudioStatus.STOP)
    }

    private var mLastDelay: Runnable? = null

    private fun preparePlay(isPageChange: Boolean = false) {
        if (!::mThreadHandler.isInitialized)
            return

        if (mLastDelay != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (mThreadHandler.hasCallbacks(mLastDelay!!))
                    mThreadHandler.removeCallbacks(mLastDelay!!)
            } else
                mThreadHandler.removeCallbacks(mLastDelay!!)
        }

        mLastDelay = Runnable {
            mLastDelay = null
            play(mLines[mLine], isPageChange, isChange = true)
        }
        mThreadHandler.postDelayed(mLastDelay!!, 2000)
    }

    fun previous() {
        val pause = mPause
        try {
            mPause = true
            if (mLine > 0) {
                mLine--
                preparePlay()
            } else if (mPage > 0) {
                val old = mPage
                mPage--
                mLines.clear()
                mLines.addAll(formatHtml(mPage, mParse!!.getPage(mPage).pageHTMLWithImages))
                mLine = (mLines.size - 1)
                mMainHandler.post {
                    mListener.forEach { it.changePageTTS(old, mPage) }
                }
                preparePlay(true)
            }
        } finally {
            mPause = pause
        }
    }

    fun next() {
        val pause = mPause
        try {
            mPause = true
            if (mLine < mLines.size) {
                mLine++
                preparePlay()
            } else if (mPage < mParse!!.getPageCount(fontSize)) {
                val old = mPage
                mPage++
                mLine = 0
                mLines.clear()
                mLines.addAll(formatHtml(mPage, mParse!!.getPage(mPage).pageHTMLWithImages))
                mMainHandler.post {
                    mListener.forEach { it.changePageTTS(old, mPage) }
                }
                preparePlay(true)
            }
        } finally {
            mPause = pause
        }
    }

    fun find(page: Int, text: String) {
        try {
            mPause = true

            val oldPage = mPage

            val isBefore = page < mPage

            if (mPage != page) {
                mPage = page
                mLine = 0
                mLines.clear()
                mLines.addAll(formatHtml(mPage, mParse!!.getPage(mPage).pageHTMLWithImages))
            }

            mMainHandler.post {
                mListener.forEach { it.changePageTTS(oldPage, mPage) }
            }

            if (isBefore) {
                for (i in mLines.size downTo 0) {
                    val item = mLines[i]
                    if (page == item.page && item.html.contains(text)) {
                        mLine = i
                        break;
                    }
                }
            } else {
                for (i in 0 until mLines.size) {
                    val item = mLines[i]
                    if (page == item.page && item.html.contains(text)) {
                        mLine = i
                        break;
                    }
                }
            }

            play(mLines[mLine], isPageChange = true, isChange = true)
        } finally {
            mPause = false
        }
    }

    private var mStatus = AudioStatus.ENDING
    private fun setStatus(status: AudioStatus) {
        mStatus = status
        mMainHandler.post { mListener.forEach { it.statusTTS(status) }  }

        if (status == AudioStatus.PLAY || status == AudioStatus.PAUSE)
            changeNotification()
    }

    private lateinit var mNotificationManager: NotificationManagerCompat
    private lateinit var mNotification: NotificationCompat.Builder
    private val mNotifyId = Notifications.getID()

    private fun createNotification() {
        mNotificationManager = NotificationManagerCompat.from(context)
        changeNotification()
    }

    private fun changeNotification() {
        val hastPrevious = (mPage > 0 || mLine > 0)
        val pages = mParse!!.getPageCount(fontSize)
        val hastNext = !(mPage == pages && mLine == (mLines.size - 1))

        mNotification = Notifications.getTTSNotification(context, context.getString(R.string.tts_title), mFileName, mCover, hastPrevious, hastNext, mPause, broadcastReceiver)
        mNotification.setProgress(pages, mPage, false)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
            mNotificationManager.notify(mNotifyId, mNotification.build())
    }

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.extras!!.getString(NotificationBroadcastReceiver.mIntentExtra)
            when (action) {
                Notifications.TTS_ACTION_PREVIUOS -> previous()
                Notifications.TTS_ACTION_PLAY -> pause()
                Notifications.TTS_ACTION_NEXT -> next()
                Notifications.TTS_ACTION_STOP -> stop()
            }
        }
    }

    private fun processNotification(isPageChange: Boolean = false) {
        if ((mLine <= 1 && mPage == 0) || (mLine >= (mLines.size - 1) && mPage >= (mParse!!.getPageCount(fontSize) - 1)))
            createNotification()

        if (isPageChange) {
            mNotification.setProgress(mParse!!.getPageCount(fontSize), mPage, false)
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                mNotificationManager.notify(mNotifyId, mNotification.build())
        }
    }

    private fun generateTTS(speach: Speech, loaded: (Uri?) -> (Unit)) {
        if (speach.audio != null) {
            loaded(speach.audio)
            return
        }

        if (SHOW_LOG)
            mLOGGER.warn("Generate tts...")

        speach.audio = null
        try {
            val file = mBook.replace(" ", "_") + '_' + String.format("%03d", speach.page) + "__" + String.format("%05d", speach.sequence)

            val exists = File(mCache, file)
            if (exists.exists())
                exists.delete()

            val audio = TTS(mVoice, speach.text).fileName(file).formatMp3().voiceRate(mVoiceRate).voiceVolume(mVoiceVolume)
                //.voicePitch()
                .storage(mCache.absolutePath).trans()
            speach.audio = File(mCache, audio).toUri()
            if (SHOW_LOG)
                mLOGGER.warn("Audio tts generated finish. ${speach.audio}")
        } catch (e: Exception) {
            mLOGGER.error("Error to generate tts.", e)
        } finally {
            loaded(speach.audio)
        }
    }

    private fun clearLastCache() {
        mCache.listFiles()?.forEach { it.delete() }
    }

    private var mMedia: MediaPlayer? = null
    private fun play(speech: Speech, isPageChange: Boolean = false, isChange: Boolean = false) {
        if (speech.audio == null)
            return

        try {
            if (SHOW_LOG)
                mLOGGER.warn("Prepare to playing audio tts.")

            if (isChange) {
                mMedia?.stop()
                mMedia?.release()
            }

            mReading = speech
            speech.isRead = true
            mPlayAudio = true
            mMedia = MediaPlayer.create(context, speech.audio)
            mMainHandler.post {
                if (SHOW_LOG)
                    mLOGGER.warn("Playing tts -- Page: $mPage - Line: $mLine :: ${speech.text}.")

                processNotification(isPageChange)
                mListener.forEach { it.readingLine(speech) }
            }
            mMedia!!.isLooping = false
            mMedia!!.setOnPreparedListener(this)
            mMedia!!.setOnErrorListener(this)
            mMedia!!.setOnCompletionListener(this)
            mMedia!!.start()
        } catch (e: Exception) {
            mLOGGER.error("Error to playing audio tts: ${speech.audio}", e)
            mPlayAudio = false
        }
    }

    private var mPlayAudio = false
    private var mPrepare = false
    private var mPause = false
    private var mStop = false

    private var mPage: Int = 0
    private var mLine: Int = 0
    private lateinit var mReading: Speech
    private val mLines: MutableList<Speech> = mutableListOf()
    private lateinit var mThreadHandler : Handler

    private fun execute(page: Int, initial: String) {
        setStatus(AudioStatus.PREPARE)

        Thread {
            Looper.prepare()

            mThreadHandler = Handler(Looper.myLooper()!!)
            mPause = false
            mStop = false

            mLines.clear()
            clearLastCache()

            try {
                mPage = page
                mLine = -1
                mLines.addAll(formatHtml(page, mParse!!.getPage(page).pageHTMLWithImages))
                if (initial.isNotEmpty())
                    for ((index, line) in mLines.withIndex()) {
                        if (line.html.contains(initial)) {
                            mLine = index - 1
                            break
                        }
                    }

                createNotification()

                while (!mStop) {
                    if (mPause) {
                        if (mStatus != AudioStatus.PAUSE)
                            setStatus(AudioStatus.PAUSE)
                        Thread.sleep(500)
                        continue
                    }

                    if (mStatus != AudioStatus.PLAY)
                        setStatus(AudioStatus.PLAY)

                    //Need because on onCompletion not running
                    mPlayAudio = mMedia?.isPlaying ?: mPlayAudio

                    if (mPlayAudio || mPrepare) {
                        if (SHOW_LOG)
                            mLOGGER.warn("Audio tts playing or preparing")
                        Thread.sleep(250)
                        continue
                    }

                    try {
                        mLine++
                        val isPageChange = if (mLine >= mLines.size || mLines.isEmpty()) {
                            val old = mPage
                            mPage++
                            if (mPage >= mParse!!.getPageCount(fontSize)) {
                                mStop = true
                                break
                            }

                            if (SHOW_LOG)
                                mLOGGER.warn("Change to new page $mPage")

                            mMainHandler.post {
                                mListener.forEach { it.changePageTTS(old, mPage) }
                            }

                            mLine = 0
                            mLines.clear()
                            mLines.addAll(formatHtml(mPage, mParse!!.getPage(mPage).pageHTMLWithImages))
                            true
                        } else
                            false

                        if (mLines.isEmpty()) {
                            if (SHOW_LOG)
                                mLOGGER.warn("Page is empty.")
                            continue
                        }

                        mReading = mLines[mLine]
                        if (mReading.audio != null)
                            play(mReading, isPageChange)
                        else {
                            mPrepare = true
                            generateTTS(mReading) { uri ->
                                mPrepare = false
                                play(mReading, isPageChange)
                            }
                        }

                        if (SHOW_LOG)
                            mLOGGER.warn("Generate tss to cache...")

                        for (i in 1 until (LIMIT_CACHE + 1))
                            if (mLine + i < mLines.size)
                                generateTTS(mLines[mLine + i]) { }
                    } finally {
                        Thread.sleep(500)
                    }
                }
            } catch (e: Exception) {
                mLOGGER.error("Error to reading page on tts.", e)
            } finally {
                onDestroy()
                setStatus(AudioStatus.ENDING)
                mMainHandler.post {
                    mListener.forEach { it.stopTTS() }
                }
            }
        }.start()
    }

    private fun generateCache() {
        for (i in mLine until mLines.size)
            if (i < mLines.size)
                mLines[i].audio = null
    }

    private fun formatHtml(page: Int, html: String): MutableList<Speech> {
        val separator = "<br>"
        var sequence = 0
        val lines = TextUtil.replaceEndLine(html, separator).split(separator).map { Speech(page, ++sequence, TextUtil.replaceHtmlTTS(it), TextUtil.replaceHtmlTags(it).trim()) }
        return lines.filter { it.text.trim().isNotEmpty() }.toMutableList()
    }


    fun onDestroy() {
        if (::mNotificationManager.isInitialized) {
            try {
                context.unregisterReceiver(broadcastReceiver)
                mNotificationManager.cancel(mNotifyId)
            } catch (e: Exception) {
                mLOGGER.error("Error to cancel tts notification.", e)
            }
        }
        mMedia?.release()
        mStop = true
        mParse = null
        mMedia = null
    }

    override fun onPrepared(media: MediaPlayer?) {
        val media = media ?: mMedia

        if(media!!.isPlaying)
            media.pause()

        media.start()

        if (SHOW_LOG)
            mLOGGER.warn("Prepare is end,playing audio tts: ${mReading.audio}.")
    }

    override fun onError(p0: MediaPlayer?, error: Int, extra: Int): Boolean {
        if (SHOW_LOG)
            mLOGGER.warn("Error to playing audio tts: ${mReading.audio}.")

        when (error) {
            MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK -> mLOGGER.error("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK $extra")
            MediaPlayer.MEDIA_ERROR_SERVER_DIED -> mLOGGER.error("MediaPlayer Error", "MEDIA ERROR SERVER DIED $extra")
            MediaPlayer.MEDIA_ERROR_UNKNOWN -> mLOGGER.error("MediaPlayer Error", "MEDIA ERROR UNKNOWN $extra")
        }

        mPlayAudio = false
        mMedia!!.release()
        mMedia = null

        return true
    }

    override fun onCompletion(p0: MediaPlayer?) {
        mPlayAudio = false
        mMedia!!.release()
        mMedia = null

        if (SHOW_LOG)
            mLOGGER.warn("Audio tts reproduced finished.")
    }

}