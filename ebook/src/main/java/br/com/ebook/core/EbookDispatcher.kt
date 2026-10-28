package br.com.ebook.core

import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

object EbookDispatcher {
    val dispatcher = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors().coerceIn(4, 8)) { runnable ->
        Thread(runnable, "EbookExtractor-Thread").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY - 1
        }
    }.asCoroutineDispatcher()
}
