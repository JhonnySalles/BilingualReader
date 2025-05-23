package br.com.fenix.bilingualreader.view.ui.popup

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.TextSpeech
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import io.github.whitemagic2014.tts.TTSVoice


class PopupTTS(var context: Context) {

    private lateinit var mPopup: AlertDialog
    fun getPopupTTS(oldVoice: TextSpeech, oldSpeed: Float, onClose: (TextSpeech, Float) -> (Unit)) {
        mNewTTS = oldVoice
        mNewSpeed = oldSpeed

        mPopup = MaterialAlertDialogBuilder(context, R.style.AppCompatMaterialAlertDialog)
            .setView(createPopup(context, LayoutInflater.from(context)))
            .setCancelable(true)
            .setNegativeButton(R.string.action_cancel) { _, _ -> onClose(oldVoice, oldSpeed) }
            .setPositiveButton(R.string.action_confirm, null)
            .create()

        mPopup.show()
        mPopup.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            if (validate()) {
                onClose(mNewTTS, mNewSpeed)
                mPopup.dismiss()
            }
        }
    }

    private var mMapReadingTTS: Map<String, TextSpeech> = hashMapOf()
    private lateinit var mNewTTS: TextSpeech
    private var mNewSpeed: Float = 0f
    private lateinit var mReadingTTS: TextInputLayout
    private lateinit var mSpeedTTS: Slider

    private fun createPopup(context: Context, inflater: LayoutInflater): View? {
        val root = inflater.inflate(R.layout.popup_tts, null, false)

        mReadingTTS = root.findViewById(R.id.popup_tts_voice)
        mSpeedTTS = root.findViewById(R.id.popup_tts_speed)
        val readingTTSAutoComplete: MaterialAutoCompleteTextView = root.findViewById(R.id.popup_menu_autocomplete_tts_voice)

        mMapReadingTTS = TextSpeech.getByDescriptions(context)

        val adapterBookReadingTTS = ArrayAdapter(context, R.layout.list_item, mMapReadingTTS.keys.toTypedArray())
        readingTTSAutoComplete.setAdapter(adapterBookReadingTTS)
        readingTTSAutoComplete.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            mNewTTS = if (parent.getItemAtPosition(position).toString().isNotEmpty() && mMapReadingTTS.containsKey(
                    parent.getItemAtPosition(position).toString()
                )
            )
                mMapReadingTTS[parent.getItemAtPosition(position).toString()]!!
            else
                TextSpeech.getDefault(false)
        }

        readingTTSAutoComplete.setText(mMapReadingTTS.entries.first { it.value == mNewTTS }.key, false)
        mSpeedTTS.value = mNewSpeed
        mSpeedTTS.addOnChangeListener { _, value, _ -> mNewSpeed = value }

        return root
    }

    private fun validate(): Boolean {
        val validated: Boolean

        mReadingTTS.isErrorEnabled = false
        mReadingTTS.error = ""

        if (mReadingTTS.editText?.text == null || mReadingTTS.editText?.text?.toString()?.isEmpty() == true) {
            validated = false
            mReadingTTS.isErrorEnabled = true
            mReadingTTS.error = context.getString(R.string.popup_reading_language_tts_voice_empty)
        } else {
            val voices = TTSVoice.provides()
            validated = voices.any { it.shortName.equals(mNewTTS.getNameAzure(), ignoreCase = true) }
            if (!validated) {
                mReadingTTS.isErrorEnabled = true
                mReadingTTS.error = context.getString(R.string.popup_reading_language_tts_voice_not_locate)
            }
        }

        return validated
    }

}