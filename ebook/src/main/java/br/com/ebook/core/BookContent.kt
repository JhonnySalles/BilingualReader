package br.com.ebook.core

sealed class BookContent {
    data class HtmlFile(val path: String, val notes: Map<String, String>? = null) : BookContent()
    data class EpubFile(val path: String) : BookContent()
}
