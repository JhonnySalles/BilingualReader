package br.com.fenix.bilingualreader.model.enums

import br.com.fenix.bilingualreader.R

enum class FontType(private val font: Int, private val nameFile: String, private val description: Int, private val isJapanese: Boolean) {
    TimesNewRoman(R.font.times_new_roman, "times_new_roman.ttf", R.string.popup_reading_font_style_times_new_roman, false),
    Algerian(R.font.algerian, "algerian.ttf", R.string.popup_reading_font_style_algerian,false),
    Arial(R.font.arial, "arial.ttf", R.string.popup_reading_font_style_arial,false),
    AutumnFlowers(R.font.autumn_flowers, "autumn_flowers.ttf", R.string.popup_reading_font_style_autumn_flowers,false),
    BabelStoneErjian1(R.font.babel_stone_erjian1, "babel_stone_erjian1.ttf", R.string.popup_reading_font_style_babel_stone_erjian_1,true),
    BabelStoneErjian2(R.font.babel_stone_erjian2, "babel_stone_erjian2.ttf", R.string.popup_reading_font_style_babel_stone_erjian_2,true),
    BabelStoneHan(R.font.babel_stone_han, "babel_stone_han.ttf", R.string.popup_reading_font_style_babel_stone_han,true),
    Blackadder(R.font.blackadder, "blackadder.ttf", R.string.popup_reading_font_style_blackadder,false),
    ComicSans(R.font.comic_sans, "comic_sans.ttf", R.string.popup_reading_font_style_comic_sans,false),
    FrenchScript(R.font.french_script, "french_script.ttf", R.string.popup_reading_font_style_french_script, false),
    Comfortaa(R.font.comfortaa, "comforta.ttf", R.string.popup_reading_font_style_comfortaa,false),
    DroidSans(R.font.droid_sans, "droid_sans.ttf", R.string.popup_reading_font_style_droid_sans,false),
    DroidSerif(R.font.droid_serif, "droid_serif.ttf", R.string.popup_reading_font_style_droid_serif,false),
    OpenSans(R.font.open_sans, "open_sans.ttf", R.string.popup_reading_font_style_open_sans,false);

    open fun getFont() : Int = this.font
    open fun getName() : String = this.nameFile
    open fun getDescription() : Int = this.description
    open fun isJapanese() : Boolean = this.isJapanese

    companion object {
        fun getCssFont() = FontType.values().joinToString(separator = "") { " @font-face { font-family: ${it.name}; src: url(\"font/${it.nameFile}\"); } " }
    }

}