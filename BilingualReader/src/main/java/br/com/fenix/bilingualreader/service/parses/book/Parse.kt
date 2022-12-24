package br.com.fenix.bilingualreader.service.parses.book

import java.io.File
import java.io.InputStream

interface Parse {
    fun parse(file: File?)
    fun destroy(isClearCache: Boolean = true)
    fun getType(): String

    fun getPage(num: Int): InputStream
    fun numPages(): Int

    fun getCover(): ByteArray

    fun getTitle(): String
    fun getAuthor(): String
    fun getSequence(): String
    fun getGenre(): String
    fun getAnnotation(): String
    fun getUnzipPath(): String
    fun getSIndex(): Integer
    fun getLang(): String
    fun getKeywords(): String
    fun getYear(): String
    fun getPublisher(): String
    fun getIsbn(): String
}