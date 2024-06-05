package br.com.fenix.bilingualreader.service.listener

import br.com.fenix.bilingualreader.model.entity.Speech
import br.com.fenix.bilingualreader.model.enums.AudioStatus

interface TTSListener {
    fun status(status: AudioStatus)
    fun readingLine(line: Speech)
    fun stop()
}