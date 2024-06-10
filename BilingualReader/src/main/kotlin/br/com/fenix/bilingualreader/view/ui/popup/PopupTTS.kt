package br.com.fenix.bilingualreader.view.ui.popup

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AlertDialog
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.TextSpeech
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout

class PopupTTS(var context: Context) {

    private lateinit var mPopup: AlertDialog
    fun getPopupTTS(old: TextSpeech, onClose: (TextSpeech) -> (Unit)) {
        mNewTTS = old

        mPopup = MaterialAlertDialogBuilder(context, R.style.AppCompatMaterialAlertDialog)
            .setView(createPopup(context, LayoutInflater.from(context)))
            .setCancelable(true)
            .setNegativeButton(R.string.action_cancel) { _, _ -> onClose(old) }
            .setPositiveButton(R.string.action_confirm) { _, _ ->
                onClose(mNewTTS)
            }
            .create()

        mPopup.show()
    }

    private var mMapReadingTTS: Map<String, TextSpeech> = hashMapOf()
    private lateinit var mNewTTS: TextSpeech

    private fun createPopup(context: Context, inflater: LayoutInflater): View? {
        val root = inflater.inflate(R.layout.popup_tts, null, false)

        val readingTTS: TextInputLayout = root.findViewById(R.id.popup_tts_voice)
        val readingTTSAutoComplete: AutoCompleteTextView = root.findViewById(R.id.popup_menu_autocomplete_tts_voice)

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
                TextSpeech.getDefault()
        }

        return root
    }

}