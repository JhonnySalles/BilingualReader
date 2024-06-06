package br.com.fenix.bilingualreader.service.listener

import br.com.fenix.bilingualreader.model.enums.Color

interface TextSelectCallbackListener {
    fun textSelectReadingFrom(page: Int, text: String)
    fun textSelectAddMark(page: Int, text: String, color: Color, start: Int, end: Int)
    fun textSelectRemoveMark(page: Int, start: Int, end: Int)
    fun textSelectTranslate(text: String, page: Int)
    fun textSelectFind(text: String, page: Int)
}