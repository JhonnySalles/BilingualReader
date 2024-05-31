package br.com.fenix.bilingualreader.service.controller

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Speech
import br.com.fenix.bilingualreader.model.enums.AudioStatus
import br.com.fenix.bilingualreader.model.enums.TextSpeech
import br.com.fenix.bilingualreader.model.exceptions.TTSException
import br.com.fenix.bilingualreader.service.listener.TTSListener
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.services.NotificationActionService
import br.com.fenix.bilingualreader.service.services.OnClearFromRecentService
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Notifications
import io.github.whitemagic2014.tts.TTS
import io.github.whitemagic2014.tts.TTSVoice
import io.github.whitemagic2014.tts.bean.Voice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors


class TextToSpeechController(val context: Context, listener: TTSListener?, book: Book,  parse: DocumentParse?, cover: Bitmap?) {

    companion object {
        const val mLIMITCACHE = 3

        private val thread = newSingleThreadContext("TTSAudio")
    }

    private var mListener: TTSListener? = listener

    private val mLOGGER = LoggerFactory.getLogger(TextToSpeechController::class.java)

    private val mCache: File = File(GeneralConsts.getCacheDir(context), GeneralConsts.CACHE_FOLDER.AUDIO)
    private var mCover: Bitmap? = cover
    private var mParse: DocumentParse? = parse

    private var mSequence: Int = 0
    private var mBook: String = book.title
    private var mFileName: String = book.fileName
    private var mDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    private lateinit var mVoice: Voice
    private var mVoiceRate = "+0%"
    private var mVoiceVolume = "+0%"

    fun test() {
        mPage = 0
        mLine = 0
        mLines.addAll(readHtml(0, mParse!!.getPage(0).pageHTMLWithImages))
    }

    fun setVoice(language: TextSpeech, rate: Float = 0f, volume: Int = 0) {
        mVoice = TTSVoice.provides().stream().filter { v: Voice -> v.shortName == language.getNameAzure() }.collect(Collectors.toList())[0]
        mVoiceRate = "+$rate%"
        mVoiceVolume = "+$volume%"
    }

    fun start(page: Int = 0, initial: String = "") {
        if (!::mVoice.isInitialized)
            throw TTSException("Not initialize a voice Type")

        execute(page, initial)
    }

    fun pause() {
        mPause = !mPause
    }

    fun stop() {
        mStop = false
    }

    private var mHandler = Handler(Looper.getMainLooper())
    private var mLastDelay: Runnable? = null

    private fun preparePlay(isPageChange: Boolean = false) {
        if (mLastDelay != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (mHandler.hasCallbacks(mLastDelay!!))
                    mHandler.removeCallbacks(mLastDelay!!)
            } else
                mHandler.removeCallbacks(mLastDelay!!)
        }

        mLastDelay = Runnable {
            mLastDelay = null
            play(mLines[mLine], isPageChange, isChange = true)
        }
        mHandler.postDelayed(mLastDelay!!, 2000)
    }

    fun back() {
        val pause = mPause
        try {
            mPause = true
            if (mLine > 0) {
                mLine--
                preparePlay()
            } else if (mPage > 0) {
                mPage--
                mLines.clear()
                mLines.addAll(readHtml(mPage, mParse!!.getPage(mPage).pageHTMLWithImages))
                mLine = (mLines.size -1)
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
            } else if (mPage < mParse!!.pageCount) {
                mPage++
                mLine = 0
                mLines.clear()
                mLines.addAll(readHtml(mPage, mParse!!.getPage(mPage).pageHTMLWithImages))
                preparePlay(true)
            }
        } finally {
            mPause = pause
        }
    }

    fun find(page: Int, text: String, isBefore: Boolean = false) {
        try {
            mPause = true

            if (mPage != page) {
                mPage = page
                mLine = 0
                mLines.clear()
                mLines.addAll(readHtml(mPage, mParse!!.getPage(mPage).pageHTMLWithImages))
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
        mListener?.status(status)
    }

    private lateinit var notificationManager : NotificationManagerCompat
    private lateinit var notification : Notification
    private fun createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager = NotificationManagerCompat.from(context)
            val mediaSessionCompat = MediaSessionCompat(context, "tag")

            context.registerReceiver(broadcastReceiver, IntentFilter(NotificationActionService.mIntentAction), Context.RECEIVER_NOT_EXPORTED)
            context.startService(Intent(context, OnClearFromRecentService::class.java))

            val intentPlay = Intent(context, NotificationActionService::class.java)
                .setAction(Notifications.TTS_ACTION_PLAY)
            val pendingIntentPlay = PendingIntent.getBroadcast(
                context, 0,
                intentPlay, PendingIntent.FLAG_UPDATE_CURRENT
            )

            val intentStop = Intent(context, NotificationActionService::class.java)
                .setAction(Notifications.TTS_ACTION_STOP)
            val pendingIntentStop = PendingIntent.getBroadcast(
                context, 0,
                intentStop, PendingIntent.FLAG_UPDATE_CURRENT
            )

            val pendingIntentPrevious: PendingIntent? = if (mPage == 0 && mLine == 0)
                null
            else {
                val intentPrevious = Intent(context, NotificationActionService::class.java)
                    .setAction(Notifications.TTS_ACTION_PREVIUOS)
                PendingIntent.getBroadcast(
                    context, 0,
                    intentPrevious, PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            val pendingIntentNext: PendingIntent? = if (mPage == mParse!!.pageCount && mLine == (mLines.size -1))
                null
            else {
                val intentNext = Intent(context, NotificationActionService::class.java)
                    .setAction(Notifications.TTS_ACTION_NEXT)
                PendingIntent.getBroadcast(
                    context, 0,
                    intentNext, PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            val notifyId = Notifications.getID()

            val notificationManager = NotificationManagerCompat.from(context)
            val notification = Notifications.getTTSNotification(context, mBook, mFileName, mCover, pendingIntentPlay, pendingIntentPrevious, pendingIntentNext, pendingIntentStop)
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                notificationManager.notify(notifyId, notification)
        }
    }

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.extras!!.getString(NotificationActionService.mIntentExtra)
            when (action) {
                Notifications.TTS_ACTION_PREVIUOS -> back()
                Notifications.TTS_ACTION_PLAY -> pause()
                Notifications.TTS_ACTION_NEXT -> next()
                Notifications.TTS_ACTION_STOP -> stop()
            }
        }
    }

    private fun processNotification(isPageChange: Boolean = false) {
        if ((mLine <= 1 && mPage == 0) || (mLine >= (mLines.size - 1) && mPage >= (mParse!!.pageCount - 1)))
            createNotification()

        if (isPageChange) {
            /*notification.setProgress(mParse.pageCount, mPage, false)
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                notificationManager.notify(notifyId, notification.build())*/
        }
    }

    private fun generateTss(speach: Speech, loaded: (Uri?) -> (Unit)) {
        if (speach.audio != null) {
            loaded(speach.audio)
            return
        }

        CoroutineScope(thread).launch {
            speach.audio = null
            try {
                val file = mBook + String.format("%05d", ++mSequence)
                val audio = TTS(mVoice, speach.text).fileName(file).formatMp3().voiceRate(mVoiceRate).voiceVolume(mVoiceVolume)
                    //.voicePitch()
                    .storage(mCache.absolutePath).trans()
                speach.audio = File(mCache, file).toUri()
            } catch (e: Exception) {
                mLOGGER.error("Generate TTS error", e)
            } finally {
                loaded(speach.audio)
            }
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
            processNotification(isPageChange)
            mMedia = MediaPlayer.create(context, speech.audio)
            mMedia!!.prepareAsync()
            mListener?.readingLine(speech)
            mMedia!!.start()
            mMedia!!.setOnCompletionListener {
                mPlayAudio = false
                mMedia = null
            }
        } catch (e: Exception) {
            mLOGGER.error("Error to reproduce audio: ${speech.audio}", e)
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

    private fun execute(page: Int, initial: String) {
        Thread {
            mPause = false
            mStop = false
            setStatus(AudioStatus.PREPARE)

            mLines.clear()

            try {
                mPage = page
                mLine = 0
                mLines.addAll(readHtml(page, mParse!!.getPage(page).pageHTMLWithImages))
                if (initial.isNotEmpty())
                    for ((index, line) in mLines.withIndex()) {
                        if (line.html.contains(initial)) {
                            mLine = index
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
                            mPage++
                            if (mPage >= mParse!!.pageCount) {
                                mStop = true
                                break
                            }

                            mLine = 0
                            mLines.clear()
                            mLines.addAll(readHtml(mPage, mParse!!.getPage(mPage).pageHTMLWithImages))
                            true
                        } else
                            false

                        val speech = mLines[mLine]
                        if (mReading.audio != null)
                            play(speech, isPageChange)
                        else {
                            mPrepare = true
                            generateTss(speech) { uri ->
                                mPrepare = false
                                play(speech, isPageChange)
                            }
                        }

                        val limit = if (mLines.size > mLIMITCACHE) mLIMITCACHE else mLines.size
                        if (limit > 1)
                            for (i in 1 until mLIMITCACHE)
                                generateTss(mLines[i]) { }
                    } finally {
                        Thread.sleep(1000)
                    }
                }
            } catch (e: Exception) {
                mLOGGER.error("Error to reading page on tts.", e)
            } finally {
                setStatus(AudioStatus.STOP)
            }
        }.start()
    }

    private fun readHtml(page: Int, html: String): MutableList<Speech> {
        val separator = if (html.contains("<end-line>")) "<end-line>" else "<br>"
        val newLine = "|||"
        var sequence = 0
        val lines = html.replace(separator, newLine).split(newLine).map { Speech(page, ++sequence, it.replace("<[^>]*>".toRegex(), ""), it) }
        return lines.toMutableList()
    }


    fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && ::notificationManager.isInitialized) {
            notificationManager.cancelAll()
            context.unregisterReceiver(broadcastReceiver)
        }

        mParse = null
        mStop = true
    }

}