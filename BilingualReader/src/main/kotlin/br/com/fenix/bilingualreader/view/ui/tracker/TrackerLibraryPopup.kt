package br.com.fenix.bilingualreader.view.ui.tracker

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.ComicInfo
import br.com.fenix.bilingualreader.model.entity.Track
import br.com.fenix.bilingualreader.service.repository.TrackRepository
import br.com.fenix.bilingualreader.service.tracker.TrackerMatcher
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText

object TrackerLibraryPopup {

    fun show(
        context: Context,
        libraryId: Long,
        fileName: String,
        comicInfo: ComicInfo? = null,
        onLinked: ((Track?) -> Unit)? = null
    ) {
        val trackRepository = TrackRepository(context)
        val tracks = trackRepository.listByLibrary(libraryId)
        val matchedTrack = TrackerMatcher.matchTrack(tracks, comicInfo, fileName)

        val parsed = TrackerMatcher.parseFileName(fileName)
        val malId = TrackerMatcher.extractMalIdFromComicInfo(comicInfo)
        val defaultTitle = comicInfo?.series?.ifBlank { null }
            ?: comicInfo?.title?.ifBlank { null }
            ?: parsed.cleanTitle.ifBlank { fileName }
        val defaultRegex = Regex.escape(defaultTitle)

        val view = LayoutInflater.from(context).inflate(R.layout.popup_tracker, null)
        val textWorkTitle = view.findViewById<TextView>(R.id.popup_tracker_work_title)
        val selectDropdown = view.findViewById<MaterialAutoCompleteTextView>(R.id.popup_tracker_select)
        val btnNew = view.findViewById<MaterialButton>(R.id.popup_tracker_btn_new)
        val inputsLayout = view.findViewById<LinearLayout>(R.id.popup_tracker_inputs_layout)
        val inputVol = view.findViewById<TextInputEditText>(R.id.popup_tracker_input_vol)
        val inputChap = view.findViewById<TextInputEditText>(R.id.popup_tracker_input_chap)

        textWorkTitle.text = fileName

        val trackTitles = tracks.map { it.title.ifBlank { it.titleRegex } }
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, trackTitles)
        selectDropdown.setAdapter(adapter)

        var selectedTrack: Track? = matchedTrack
        var alertDialog: androidx.appcompat.app.AlertDialog? = null

        fun updateTrackSelection(track: Track?) {
            selectedTrack = track
            if (track != null) {
                inputsLayout.visibility = View.VISIBLE
                val volVal = if (track.volumesRead > 0) track.volumesRead else (parsed.volume ?: 0)
                val chapVal = if (track.chaptersRead > 0) track.chaptersRead else (parsed.chapter?.toInt() ?: 0)
                inputVol.setText(if (volVal > 0) volVal.toString() else "")
                inputChap.setText(if (chapVal > 0) chapVal.toString() else "")
                alertDialog?.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL)?.visibility = View.VISIBLE
            } else {
                inputsLayout.visibility = View.GONE
                alertDialog?.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL)?.visibility = View.GONE
            }
        }

        selectDropdown.setOnItemClickListener { _, _, position, _ ->
            if (position in tracks.indices) {
                updateTrackSelection(tracks[position])
            }
        }

        btnNew.setOnClickListener {
            alertDialog?.dismiss()
            TrackerActivity.start(
                context = context,
                libraryId = libraryId,
                prefillTitle = defaultTitle,
                prefillRegex = defaultRegex,
                prefillMalId = malId,
                prefillVol = parsed.volume,
                prefillChap = parsed.chapter
            )
        }

        fun saveAndFinish(sync: Boolean) {
            val track = selectedTrack
            if (track != null) {
                val vol = inputVol.text?.toString()?.toIntOrNull()
                val chap = inputChap.text?.toString()?.toIntOrNull()
                if (vol != null) track.volumesRead = vol
                if (chap != null) track.chaptersRead = chap
                trackRepository.update(track)

                if (sync) {
                    Toast.makeText(context, R.string.tracker_sync, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, R.string.tracker_save_success, Toast.LENGTH_SHORT).show()
                }
                onLinked?.invoke(track)
            } else {
                onLinked?.invoke(null)
            }
        }

        alertDialog = MaterialAlertDialogBuilder(context, R.style.AppCompatAlertDialogStyle)
            .setTitle(R.string.tracker_title)
            .setView(view)
            .setPositiveButton(R.string.action_save) { _, _ ->
                saveAndFinish(sync = false)
            }
            .setNeutralButton(R.string.tracker_sync) { _, _ ->
                saveAndFinish(sync = true)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .create()

        alertDialog.show()

        if (matchedTrack != null) {
            val idx = tracks.indexOfFirst { it.id == matchedTrack.id }
            if (idx != -1) {
                selectDropdown.setText(trackTitles[idx], false)
            }
            updateTrackSelection(matchedTrack)
        } else {
            updateTrackSelection(null)
        }
    }
}
