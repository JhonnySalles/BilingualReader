package br.com.fenix.bilingualreader.service.controller

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
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
import io.github.whitemagic2014.tts.TTS
import io.github.whitemagic2014.tts.TTSVoice
import io.github.whitemagic2014.tts.bean.Voice
import org.slf4j.LoggerFactory
import java.io.File
import java.util.stream.Collectors


class TextToSpeechController(val context: Context, book: Book, parse: DocumentParse?, cover: Bitmap?, val fontSize: Int) {

    companion object {
        const val mLIMITCACHE = 3
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
        val language: TextSpeech =
            TextSpeech.valueOf(sharedPreferences.getString(GeneralConsts.KEYS.READER.BOOK_READER_TTS, TextSpeech.getDefault().toString())!!)
        mVoice = TTSVoice.provides().stream().filter { v: Voice -> v.shortName == language.getNameAzure() }.collect(Collectors.toList())[0]
    }

    fun addListener(listener: TTSListener) = mListener.add(listener)

    fun removeListener(listener: TTSListener) = mListener.remove(listener)

    fun setVoice(language: TextSpeech, rate: Float = 0f, volume: Int = 0) {
        mVoice = TTSVoice.provides().stream().filter { v: Voice -> v.shortName == language.getNameAzure() }.collect(Collectors.toList())[0]
        mVoiceRate = "+$rate%"
        mVoiceVolume = "+$volume%"
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
                mLines.addAll(readHtml(mPage, mParse!!.getPage(mPage).pageHTMLWithImages))
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
                mLines.addAll(readHtml(mPage, mParse!!.getPage(mPage).pageHTMLWithImages))
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
                mLines.addAll(readHtml(mPage, mParse!!.getPage(mPage).pageHTMLWithImages))
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

        speach.audio = null
        try {
            val file = mBook.replace(" ", "_") + '_' + String.format("%03d", speach.page) + "__" + String.format("%05d", speach.sequence)
            val audio = TTS(mVoice, speach.text).fileName(file).formatMp3().voiceRate(mVoiceRate).voiceVolume(mVoiceVolume)
                //.voicePitch()
                .storage(mCache.absolutePath).trans()
            speach.audio = File(mCache, audio).toUri()
        } catch (e: Exception) {
            mLOGGER.error("Error to generate TTS.", e)
        } finally {
            loaded(speach.audio)
        }
    }

    private var mMedia: MediaPlayer? = null
    private fun play(speech: Speech, isPageChange: Boolean = false, isChange: Boolean = false) {
        if (speech.audio == null)
            return

        try {
            if (isChange)
                mMedia?.stop()

            mReading = speech
            speech.isRead = true
            mPlayAudio = true
            mMedia = MediaPlayer.create(context, speech.audio)
            mMainHandler.post {
                processNotification(isPageChange)
                mListener.forEach { it.readingLine(speech) }
            }
            mMedia!!.isLooping = false
            mMedia!!.start()
            mMedia!!.setOnCompletionListener {
                mPlayAudio = false
                mMedia = null
            }
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

            try {
                mPage = page
                mLine = -1
                mLines.addAll(readHtml(page, mParse!!.getPage(page).pageHTMLWithImages))
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

                    try {
                        if (mPlayAudio || mPrepare)
                            continue

                        mLine++
                        val isPageChange = if (mLine >= mLines.size || mLines.isEmpty()) {
                            val old = mPage
                            mPage++
                            if (mPage >= mParse!!.getPageCount(fontSize)) {
                                mStop = true
                                break
                            }

                            mMainHandler.post {
                                mListener.forEach { it.changePageTTS(old, mPage) }
                            }

                            mLine = 0
                            mLines.clear()
                            mLines.addAll(readHtml(mPage, mParse!!.getPage(mPage).pageHTMLWithImages))
                            true
                        } else
                            false

                        if (mLines.isEmpty()) {
                            Thread.sleep(500)
                            continue
                        }

                        mReading = mLines[mLine]
                        mLOGGER.error("Page $mPage - Line $mLine - size ${mLines.size}:: " + mReading.text)

                        mMainHandler.post {
                            processNotification(false)
                            mListener.forEach { it.readingLine(mReading) }
                        }

                        Thread.sleep(2000)
                        if (true)
                            continue

                        if (mReading.audio != null)
                            play(mReading, isPageChange)
                        else {
                            mPrepare = true
                            generateTTS(mReading) { uri ->
                                mPrepare = false
                                play(mReading, isPageChange)
                            }
                        }

                        val limit = if (mLines.size > mLIMITCACHE) mLIMITCACHE else mLines.size
                        if (limit > 1)
                            for (i in 1 until mLIMITCACHE)
                                generateTTS(mLines[i]) { }
                    } finally {
                        Thread.sleep(1000)
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

    private fun readHtml(page: Int, html: String): MutableList<Speech> {
        val separator = if (html.contains("<end-line>")) "<end-line>" else "<br>"

        val html = if (html.contains("<image-begin>image"))
            html.replace("<image-begin>", "<img src=\"data:").replace("<image-end>", "\" />")
        else
            html

        val newLine = "|||"
        var sequence = 0
        val lines = html.replace(separator, newLine).split(newLine).map { Speech(page, ++sequence, it.replace("<[^>]*>".toRegex(), ""), it) }
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

        mStop = true
        mParse = null
    }

}