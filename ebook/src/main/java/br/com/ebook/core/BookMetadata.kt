package br.com.ebook.core

import java.time.LocalDate

data class BookMetadata(
    val title: String,
    val author: String,
    val series: String = "",
    val genre: String = "",
    val language: String = "",
    val isbn: String = "",
    val publisher: String = "",
    val releaseDate: LocalDate? = null,
    val seriesIndex: Int = 0,
    val annotation: String = "",
    val coverImage: ByteArray? = null,
    val unzipPath: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BookMetadata

        if (title != other.title) return false
        if (author != other.author) return false
        if (isbn != other.isbn) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + isbn.hashCode()
        return result
    }
}
