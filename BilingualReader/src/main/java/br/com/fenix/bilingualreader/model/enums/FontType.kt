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
    RobotsReborn(R.font.robots_reborn, "robots_reborn.ttf", R.string.popup_reading_font_style_robots_reborn,false);

    open fun getFont() : Int = this.font
    open fun getName() : String = this.nameFile
    open fun getDescription() : Int = this.description
    open fun isJapanese() : Boolean = this.isJapanese

}