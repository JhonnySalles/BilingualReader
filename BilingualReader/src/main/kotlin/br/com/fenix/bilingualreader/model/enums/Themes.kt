package br.com.fenix.bilingualreader.model.enums

import br.com.fenix.bilingualreader.R

enum class Themes(private val value: Int) {
    ORIGINAL(R.style.Theme_MangaReader),
    BLUE(R.style.Theme_MangaReader_Blue),
    OCEAN_BLUE(R.style.Theme_MangaReader_OceanBlue),
    GREEN(R.style.Theme_MangaReader_Green),
    FOREST_GREEN(R.style.Theme_MangaReader_ForestGreen),
    PINK(R.style.Theme_MangaReader_Pink),
    RED(R.style.Theme_MangaReader_Red),
    BLOOD_RED(R.style.Theme_MangaReader_BloodRed);

    open fun getValue() : Int = this.value
}