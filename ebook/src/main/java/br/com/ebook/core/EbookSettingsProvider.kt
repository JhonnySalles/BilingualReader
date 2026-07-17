package br.com.ebook.core

import br.com.ebook.foobnix.pdf.info.model.BookCSS
import br.com.ebook.foobnix.pdf.info.wrapper.AppState

interface EbookSettingsProvider {
    val isPreText: Boolean
    val isLineBreaksText: Boolean
    val isFirstSurname: Boolean
    val isDouble: Boolean
    val isAutoHypens: Boolean
    val hypenLang: String
}

object EbookSettings : EbookSettingsProvider {
    var provider: EbookSettingsProvider = DefaultEbookSettingsProvider

    override val isPreText: Boolean get() = provider.isPreText
    override val isLineBreaksText: Boolean get() = provider.isLineBreaksText
    override val isFirstSurname: Boolean get() = provider.isFirstSurname
    override val isDouble: Boolean get() = provider.isDouble
    override val isAutoHypens: Boolean get() = provider.isAutoHypens
    override val hypenLang: String get() = provider.hypenLang
}

private object DefaultEbookSettingsProvider : EbookSettingsProvider {
    override val isPreText: Boolean get() = try { AppState.get()?.isPreText ?: false } catch (e: Throwable) { false }
    override val isLineBreaksText: Boolean get() = try { AppState.get()?.isLineBreaksText ?: false } catch (e: Throwable) { false }
    override val isFirstSurname: Boolean get() = try { AppState.get()?.isFirstSurname ?: false } catch (e: Throwable) { false }
    override val isDouble: Boolean get() = try { AppState.get()?.isDouble ?: false } catch (e: Throwable) { false }
    override val isAutoHypens: Boolean get() = try { BookCSS.get()?.isAutoHypens ?: false } catch (e: Throwable) { false }
    override val hypenLang: String get() = try { BookCSS.get()?.hypenLang ?: "en" } catch (e: Throwable) { "en" }
}
