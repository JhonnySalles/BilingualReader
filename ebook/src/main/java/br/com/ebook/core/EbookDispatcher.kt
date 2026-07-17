package br.com.ebook.core

import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

object EbookDispatcher {
    val dispatcher = Executors.newFixedThreadPool(2) { runnable ->
        Thread(runnable, "EbookExtractor-Thread").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY - 1
        }
    }.asCoroutineDispatcher()
}
