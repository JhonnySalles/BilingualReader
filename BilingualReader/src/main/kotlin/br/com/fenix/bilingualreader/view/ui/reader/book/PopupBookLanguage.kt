package br.com.fenix.bilingualreader.view.ui.reader.book

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.TextSpeech
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout
import org.lucasr.twowayview.TwoWayView
import org.slf4j.LoggerFactory


class PopupBookLanguage : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(PopupBookLanguage::class.java)

    private val mViewModel: BookReaderViewModel by activityViewModels()

    private lateinit var mBookLanguage: TextInputLayout
    private lateinit var mBookLanguageAutoComplete: AutoCompleteTextView
    private lateinit var mBookReadingTTS: TextInputLayout
    private lateinit var mBookReadingTTSAutoComplete: AutoCompleteTextView
    private lateinit var mProcessJapaneseText: SwitchMaterial
    private lateinit var mTextWithFurigana: SwitchMaterial

    private var mMapLanguage: HashMap<String, Languages> = hashMapOf()
    private var mMapReadingTTS:  Map<String, TextSpeech> = hashMapOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_book_language, container, false)

        mBookLanguage = root.findViewById(R.id.popup_book_language_book_language)
        mBookLanguageAutoComplete = root.findViewById(R.id.popup_book_language_menu_autocomplete_book_language)
        mBookReadingTTS = root.findViewById(R.id.popup_book_language_tts_voice)
        mBookReadingTTSAutoComplete = root.findViewById(R.id.popup_book_language_menu_autocomplete_tts_voice)
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
                    TextSpeech.getDefault()

                mViewModel.changeTTSVoice(selected)
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


        val tts = TextSpeech.valueOf(preferences.getString(GeneralConsts.KEYS.READER.BOOK_READER_TTS, TextSpeech.getDefault().toString())!!)

        mBookReadingTTSAutoComplete.setText(mMapReadingTTS.filterValues { it == tts }.keys.first(), false)

        val language = mViewModel.book.value?.language ?: Languages.ENGLISH
        mBookLanguageAutoComplete.setText(mMapLanguage.filterValues { it == language }.keys.first(), false)

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
                mBookLanguageAutoComplete.setText(mMapLanguage.filterValues { lan -> lan == it.language }.keys.first(), false)
        }

        mViewModel.ttsVoice.observe(viewLifecycleOwner) {
            mBookReadingTTSAutoComplete.setText(mMapReadingTTS.filterValues { tts -> it == tts }.keys.first(), false)
        }

        mViewModel.language.observe(viewLifecycleOwner) {
            mBookLanguageAutoComplete.setText(mMapLanguage.filterValues { lang -> it == lang }.keys.first(), false)
            changeLanguage(it)
        }
    }

    private fun changeLanguage(lang: Languages) {
        if (lang == Languages.JAPANESE) {
            mProcessJapaneseText.visibility = View.VISIBLE
            mTextWithFurigana.visibility = View.VISIBLE
        } else {
            mProcessJapaneseText.visibility = View.GONE
            mTextWithFurigana.visibility = View.GONE
        }
    }
}