package br.com.fenix.bilingualreader.model.enums

import android.content.Context
import br.com.fenix.bilingualreader.R

enum class TextSpeech(private val index: Int, private val nameAzure: String, private val language: Languages,  private val voice : Int, private val region : Int, private val idiom : Int, private val gender: Int, private val isNeural: Boolean) {
    ARIA( 0,"en-US-AriaNeural", Languages.ENGLISH, R.string.book_tts_voice_aria, R.string.book_tts_region_en, R.string.book_tts_idiom_en, R.string.book_tts_voice_gender_masculine, true ),
    DAVIS( 1,"en-US-DavisNeural", Languages.ENGLISH, R.string.book_tts_voice_aria, R.string.book_tts_region_en, R.string.book_tts_idiom_en, R.string.book_tts_voice_gender_masculine, true),
    GUY( 2,"en-US-GuyNeural",Languages.ENGLISH, R.string.book_tts_voice_aria, R.string.book_tts_region_en, R.string.book_tts_idiom_en, R.string.book_tts_voice_gender_masculine, true ),
    JANE( 3,"en-US-JaneNeural",Languages.ENGLISH, R.string.book_tts_voice_aria, R.string.book_tts_region_en, R.string.book_tts_idiom_en, R.string.book_tts_voice_gender_masculine, true ),
    JASON(  4,"en-US-JasonNeural",Languages.ENGLISH, R.string.book_tts_voice_aria, R.string.book_tts_region_en, R.string.book_tts_idiom_en, R.string.book_tts_voice_gender_masculine, true ),
    JENNY(  5,"en-US-JennyNeural",Languages.ENGLISH, R.string.book_tts_voice_aria, R.string.book_tts_region_en, R.string.book_tts_idiom_en, R.string.book_tts_voice_gender_masculine, true ),
    NANCY( 6,"en-US-NancyNeural",Languages.ENGLISH, R.string.book_tts_voice_aria, R.string.book_tts_region_en, R.string.book_tts_idiom_en, R.string.book_tts_voice_gender_masculine, true ),
    SARA( 7,"en-US-SaraNeural",Languages.ENGLISH, R.string.book_tts_voice_aria, R.string.book_tts_region_en, R.string.book_tts_idiom_en, R.string.book_tts_voice_gender_masculine, true ),
    TONY(  8,"en-US-TonyNeural",Languages.ENGLISH, R.string.book_tts_voice_aria, R.string.book_tts_region_en, R.string.book_tts_idiom_en, R.string.book_tts_voice_gender_masculine, true ),
    NANAMI(  9,"ja-JP-NanamiNeural",Languages.ENGLISH, R.string.book_tts_voice_aria, R.string.book_tts_region_en, R.string.book_tts_idiom_en, R.string.book_tts_voice_gender_masculine, true ),
    FRANCISCA(10, "pt-BR-FranciscaNeural",Languages.ENGLISH, R.string.book_tts_voice_aria, R.string.book_tts_region_en, R.string.book_tts_idiom_en, R.string.book_tts_voice_gender_masculine, true );

    open fun getDescription(context: Context) : String {
        val id =  if (this.isNeural)
            R.string.book_tts_description_neural
        else
            R.string.book_tts_description
        return context.getString(id, this.voice, this.idiom)
    }
    open fun getNameAzure() : String = this.nameAzure
    open fun isNeural() : Boolean = this.isNeural


}