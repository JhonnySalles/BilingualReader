package br.com.fenix.bilingualreader.view.ui.reader.book

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.TextSpeech
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import org.slf4j.LoggerFactory


class PopupBookLanguage : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(PopupBookLanguage::class.java)

    private val mViewModel: BookReaderViewModel by activityViewModels()

    private lateinit var mBookLanguage: TextInputLayout
    private lateinit var mBookLanguageAutoComplete: MaterialAutoCompleteTextView
    private lateinit var mBookReadingTTS: TextInputLayout
    private lateinit var mBookReadingTTSAutoComplete: MaterialAutoCompleteTextView
    private lateinit var mBookReadingSpeed: Slider
    private lateinit var mProcessJapaneseText: SwitchMaterial
    private lateinit var mTextWithFurigana: SwitchMaterial

    private var mMapLanguage: HashMap<String, Languages> = hashMapOf()
    private var mMapReadingTTS:  Map<String, TextSpeech> = hashMapOf()

    private var mIsJapanese : Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_book_language, container, false)

        mBookLanguage = root.findViewById(R.id.popup_book_language_book_language)
        mBookLanguageAutoComplete = root.findViewById(R.id.popup_book_language_menu_autocomplete_book_language)
        mBookReadingTTS = root.findViewById(R.id.popup_book_language_tts_voice)
        mBookReadingTTSAutoComplete = root.findViewById(R.id.popup_book_language_menu_autocomplete_tts_voice)
        mBookReadingSpeed = root.findViewById(R.id.popup_book_language_tts_speed)
        mProcessJapaneseText = root.findViewById(R.id.popup_book_language_process_japanese_text)
        mTextWithFurigana = root.findViewById(R.id.popup_book_language_text_with_furigana)

        val languages = resources.getStringArray(R.array.languages)
        mMapLanguage = hashMapOf(
            languages[1] to Languages.ENGLISH,
            languages[2] to Languages.JAPANESE,
            languages[0] to Languages.PORTUGUESE
        )

        mBookLanguageAutoComplete.setAdapter(ArrayAdapter(requireContext(), R.layout.list_item, mMapLanguage.keys.toTypedArray()))
        mBookLanguageAutoComplete.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
                val lang = if (parent.getItemAtPosition(position).toString().isNotEmpty() &&
                    mMapLanguage.containsKey(parent.getItemAtPosition(position).toString())
                )
                    mMapLanguage[parent.getItemAtPosition(position).toString()]!!
                else
                    mViewModel.book.value?.language ?: Languages.JAPANESE

                mViewModel.changeLanguage(lang)
            }

        mMapReadingTTS = TextSpeech.getByDescriptions(requireContext())

        val adapterBookReadingTTS = ArrayAdapter(requireContext(), R.layout.list_item, mMapReadingTTS.keys.toTypedArray())
        mBookReadingTTSAutoComplete.setAdapter(adapterBookReadingTTS)
        mBookReadingTTSAutoComplete.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
                val selected = if (parent.getItemAtPosition(position).toString().isNotEmpty() && mMapReadingTTS.containsKey(parent.getItemAtPosition(position).toString()))
                    mMapReadingTTS[parent.getItemAtPosition(position).toString()]!!
                else
                    TextSpeech.getDefault(mIsJapanese)

                mViewModel.changeTTSVoice(selected, mBookReadingSpeed.value)
            }

        val preferences = GeneralConsts.getSharedPreferences(requireContext())

        mProcessJapaneseText.setOnClickListener {
            with(preferences.edit()) {
                this.putBoolean(
                    GeneralConsts.KEYS.READER.BOOK_PROCESS_JAPANESE_TEXT,
                    mProcessJapaneseText.isChecked
                )
                this.commit()
            }

            mViewModel.changeJapanese(mProcessJapaneseText.isChecked, mTextWithFurigana.isChecked)
        }

        mTextWithFurigana.setOnClickListener {
            with(preferences.edit()) {
                this.putBoolean(
                    GeneralConsts.KEYS.READER.BOOK_GENERATE_FURIGANA_ON_TEXT,
                    mTextWithFurigana.isChecked
                )
                this.commit()
            }

            mViewModel.changeJapanese(mProcessJapaneseText.isChecked, mTextWithFurigana.isChecked)
        }

        val key = if (mIsJapanese) GeneralConsts.KEYS.READER.BOOK_READER_TTS_VOICE_JAPANESE else GeneralConsts.KEYS.READER.BOOK_READER_TTS_VOICE_NORMAL
        val voice = TextSpeech.valueOf(preferences.getString(key, TextSpeech.getDefault(mIsJapanese).toString())!!)

        mBookReadingTTSAutoComplete.setText(mMapReadingTTS.entries.first { it.value == voice }.key, false)
        mBookReadingSpeed.value = preferences.getFloat(GeneralConsts.KEYS.READER.BOOK_READER_TTS_SPEED, GeneralConsts.KEYS.READER.BOOK_READER_TTS_SPEED_DEFAULT)

        val language = mViewModel.book.value?.language ?: Languages.ENGLISH
        mBookLanguageAutoComplete.setText(mMapLanguage.entries.first { it.value == language }.key, false)

        mProcessJapaneseText.isChecked = preferences.getBoolean(GeneralConsts.KEYS.READER.BOOK_GENERATE_FURIGANA_ON_TEXT, true)
        mTextWithFurigana.isChecked = preferences.getBoolean(GeneralConsts.KEYS.READER.BOOK_PROCESS_JAPANESE_TEXT, true)

        observer()
        return root
    }

    private fun observer() {
        mViewModel.book.observe(viewLifecycleOwner) {
            if (it == null)
                mBookLanguageAutoComplete.setText("", false)
            else
                mBookLanguageAutoComplete.setText(mMapLanguage.entries.first { e -> e.value == it.language }.key, false)
        }

        mViewModel.ttsVoice.observe(viewLifecycleOwner) {
            mBookReadingTTSAutoComplete.setText(mMapReadingTTS.entries.first { tts -> it == tts.value }.key, false)
        }

        mViewModel.language.observe(viewLifecycleOwner) {
            mBookLanguageAutoComplete.setText(mMapLanguage.entries.first { e -> e.value == it }.key, false)
            changeLanguage(it)
        }
    }

    private fun changeLanguage(lang: Languages) {
        mIsJapanese = lang == Languages.JAPANESE
        if (mIsJapanese) {
            mProcessJapaneseText.visibility = View.VISIBLE
            mTextWithFurigana.visibility = View.VISIBLE
        } else {
            mProcessJapaneseText.visibility = View.GONE
            mTextWithFurigana.visibility = View.GONE
        }
    }
}