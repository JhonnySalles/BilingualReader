package br.com.ebook.core

sealed class BookContent {
    data class HtmlFile(val path: String, val notes: Map<String, String>?) : BookContent()
    data class EpubFile(val path: String) : BookContent()
}
