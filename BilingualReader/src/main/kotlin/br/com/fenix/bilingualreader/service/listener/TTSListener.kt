package br.com.fenix.bilingualreader.service.listener

import br.com.fenix.bilingualreader.model.entity.Speech
import br.com.fenix.bilingualreader.model.enums.AudioStatus

interface TTSListener {
    fun statusTTS(status: AudioStatus)
    fun readingLine(line: Speech)
    fun changePageTTS(old:Int, new:Int)
    fun stopTTS()
}