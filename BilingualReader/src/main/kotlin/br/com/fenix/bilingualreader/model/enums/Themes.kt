package br.com.fenix.bilingualreader.model.enums

import br.com.fenix.bilingualreader.R

enum class Themes(private val value: Int) {
    ORIGINAL(R.style.Theme_MangaReader),
    BLUE(R.style.Theme_MangaReader_Blue),
    GREEN(R.style.Theme_MangaReader_Green),
    PINK(R.style.Theme_MangaReader_Pink),
    RED(R.style.Theme_MangaReader_Red);

    open fun getValue() : Int = this.value
}