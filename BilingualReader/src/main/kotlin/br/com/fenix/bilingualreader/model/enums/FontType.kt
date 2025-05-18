package br.com.fenix.bilingualreader.model.enums

import br.com.fenix.bilingualreader.R

enum class FontType(private val font: Int, private val fontRotate: Int?, private val nameFile: String, private val description: Int, private val isJapanese: Boolean) {
    TimesNewRoman(R.font.times_new_roman, null, "times_new_roman.ttf", R.string.popup_reading_font_style_times_new_roman, false),
    Algerian(R.font.algerian, null,"algerian.ttf", R.string.popup_reading_font_style_algerian,false),
    Arial(R.font.arial, null,"arial.ttf", R.string.popup_reading_font_style_arial,false),
    AutumnFlowers(R.font.autumn_flowers, null,"autumn_flowers.ttf", R.string.popup_reading_font_style_autumn_flowers,false),
    BabelStoneErjian1(R.font.babel_stone_erjian1, R.font.babel_stone_erjian_rotated1,"babel_stone_erjian1.ttf", R.string.popup_reading_font_style_babel_stone_erjian_1,true),
    BabelStoneErjian2(R.font.babel_stone_erjian2, R.font.babel_stone_erjian_rotated2,"babel_stone_erjian2.ttf", R.string.popup_reading_font_style_babel_stone_erjian_2,true),
    BabelStoneHan(R.font.babel_stone_han, R.font.babel_stone_han_rotated,"babel_stone_han.ttf", R.string.popup_reading_font_style_babel_stone_han,true),
    Blackadder(R.font.blackadder,null,"blackadder.ttf", R.string.popup_reading_font_style_blackadder,false),
    ComicSans(R.font.comic_sans,null,"comic_sans.ttf", R.string.popup_reading_font_style_comic_sans,false),
    FrenchScript(R.font.french_script, null,"french_script.ttf", R.string.popup_reading_font_style_french_script, false),
    Comfortaa(R.font.comfortaa,null,"comforta.ttf", R.string.popup_reading_font_style_comfortaa,false),
    DroidSans(R.font.droid_sans,null,"droid_sans.ttf", R.string.popup_reading_font_style_droid_sans,false),
    DroidSerif(R.font.droid_serif,null,"droid_serif.ttf", R.string.popup_reading_font_style_droid_serif,false),
    OpenSans(R.font.open_sans,null,"open_sans.ttf", R.string.popup_reading_font_style_open_sans,false);

    open fun getFont() : Int = this.font
    open fun getFontRotate() : Int = this.fontRotate ?: this.font
    open fun getName() : String = this.nameFile
    open fun getDescription() : Int = this.description
    open fun isJapanese() : Boolean = this.isJapanese

    companion object {
        fun getCssFont() = FontType.values().joinToString(separator = "") { " @font-face { font-family: ${it.name}; src: url(\"font/${it.nameFile}\"); } " }
    }

}