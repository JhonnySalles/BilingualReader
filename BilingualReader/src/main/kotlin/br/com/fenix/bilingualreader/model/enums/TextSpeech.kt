package br.com.fenix.bilingualreader.model.enums

import android.content.Context
import br.com.fenix.bilingualreader.R


enum class TextSpeech(
    private val index: Int,
    private val nameAzure: String,
    private val language: Languages,
    private val voice: Int,
    private val region: Int,
    private val idiom: Int,
    private val gender: Int,
    private val isNeural: Boolean,
    private val isActive: Boolean
) {
    ARIA(
        0,
        "en-US-AriaNeural",
        Languages.ENGLISH,
        R.string.book_tts_voice_aria,
        R.string.book_tts_region_en,
        R.string.book_tts_idiom_en,
        R.string.book_tts_voice_gender_feminine,
        true,
        true
    ),
    DAVIS(
        1,
        "en-US-DavisNeural",
        Languages.ENGLISH,
        R.string.book_tts_voice_davis,
        R.string.book_tts_region_en,
        R.string.book_tts_idiom_en,
        R.string.book_tts_voice_gender_masculine,
        true,
        false
    ),
    GUY(
        2,
        "en-US-GuyNeural",
        Languages.ENGLISH,
        R.string.book_tts_voice_guy,
        R.string.book_tts_region_en,
        R.string.book_tts_idiom_en,
        R.string.book_tts_voice_gender_masculine,
        true,
        true
    ),
    JANE(
        3,
        "en-US-JaneNeural",
        Languages.ENGLISH,
        R.string.book_tts_voice_jane,
        R.string.book_tts_region_en,
        R.string.book_tts_idiom_en,
        R.string.book_tts_voice_gender_feminine,
        true,
        false
    ),
    JASON(
        4,
        "en-US-JasonNeural",
        Languages.ENGLISH,
        R.string.book_tts_voice_jason,
        R.string.book_tts_region_en,
        R.string.book_tts_idiom_en,
        R.string.book_tts_voice_gender_masculine,
        true,
        false
    ),
    JENNY(
        5,
        "en-US-JennyNeural",
        Languages.ENGLISH,
        R.string.book_tts_voice_jenny,
        R.string.book_tts_region_en,
        R.string.book_tts_idiom_en,
        R.string.book_tts_voice_gender_feminine,
        true,
        true
    ),
    NANCY(
        6,
        "en-US-NancyNeural",
        Languages.ENGLISH,
        R.string.book_tts_voice_nancy,
        R.string.book_tts_region_en,
        R.string.book_tts_idiom_en,
        R.string.book_tts_voice_gender_feminine,
        true,
        false
    ),
    SARA(
        7,
        "en-US-SaraNeural",
        Languages.ENGLISH,
        R.string.book_tts_voice_sara,
        R.string.book_tts_region_en,
        R.string.book_tts_idiom_en,
        R.string.book_tts_voice_gender_masculine,
        true,
        false
    ),
    TONY(
        8,
        "en-US-TonyNeural",
        Languages.ENGLISH,
        R.string.book_tts_voice_tony,
        R.string.book_tts_region_en,
        R.string.book_tts_idiom_en,
        R.string.book_tts_voice_gender_masculine,
        true,
        false
    ),
    NANAMI(
        9,
        "ja-JP-NanamiNeural",
        Languages.JAPANESE,
        R.string.book_tts_voice_nanami,
        R.string.book_tts_region_jp,
        R.string.book_tts_idiom_jp,
        R.string.book_tts_voice_gender_feminine,
        true, true
    ),
    KEITA(
        10,
        "ja-JP-KeitaNeural",
        Languages.JAPANESE,
        R.string.book_tts_voice_keita,
        R.string.book_tts_region_jp,
        R.string.book_tts_idiom_jp,
        R.string.book_tts_voice_gender_masculine,
        true,
        true
    ),
    FRANCISCA(
        11,
        "pt-BR-FranciscaNeural",
        Languages.PORTUGUESE,
        R.string.book_tts_voice_francisca,
        R.string.book_tts_region_pt,
        R.string.book_tts_idiom_pt,
        R.string.book_tts_voice_gender_feminine,
        true,
        true
    ),
    ANTONIO(
        12,
        "pt-BR-AntonioNeural",
        Languages.PORTUGUESE,
        R.string.book_tts_voice_antonio,
        R.string.book_tts_region_pt,
        R.string.book_tts_idiom_pt,
        R.string.book_tts_voice_gender_masculine,
        true, true
    ),
    THALITA(
        13,
        "pt-BR-ThalitaNeural1",
        Languages.PORTUGUESE,
        R.string.book_tts_voice_thalita,
        R.string.book_tts_region_pt,
        R.string.book_tts_idiom_pt,
        R.string.book_tts_voice_gender_feminine,
        true,
        false
    );

    open fun getDescription(context: Context): String {
        val id = if (this.isNeural)
            R.string.book_tts_description_neural
        else
            R.string.book_tts_description
        return context.getString(id, context.getString(this.voice), context.getString(this.idiom))
    }

    open fun getNameAzure(): String = this.nameAzure
    open fun isNeural(): Boolean = this.isNeural
    open fun isActive(): Boolean = this.isActive


    companion object {

        fun getDefault(isJapanese : Boolean): TextSpeech = if (isJapanese) NANAMI else FRANCISCA
        fun getByDescriptions(context: Context): Map<String, TextSpeech> {
            return TextSpeech.values().filter { it.isActive }.sortedWith(compareBy({ it.language }, { it.name })).associateBy { it.getDescription(context) }
        }

        fun getTextSpeech(context: Context, description: String, isJapanese: Boolean): TextSpeech {
            for (item in TextSpeech.values())
                if (item.getDescription(context).equals(description, ignoreCase = true))
                    return item

            return getDefault(isJapanese)
        }
    }


}