package br.com.fenix.bilingualreader.view.ui.tracker

import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.ComicInfo
import br.com.fenix.bilingualreader.model.entity.Track
import br.com.fenix.bilingualreader.model.enums.TrackStatus
import br.com.fenix.bilingualreader.service.repository.TrackRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import java.time.LocalDateTime

object TrackerConfigDialog {

    fun show(
        context: Context,
        libraryId: Long,
        fileName: String,
        comicInfo: ComicInfo? = null,
        onSaved: ((Track) -> Unit)? = null
    ) {
        TrackerLibraryPopup.show(context, libraryId, fileName, comicInfo) { track ->
            if (track != null) {
                onSaved?.invoke(track)
            }
        }
    }

    /**
     * Dialog disparado na última página do arquivo para confirmação do progresso de leitura.
     */
    fun showReadingConfirmation(
        context: Context,
        track: Track,
        inferredVolume: Int?,
        inferredChapter: Int?,
        onConfirmed: (Int, Int) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.popup_tracker_reading, null)
        val inputVolume = view.findViewById<TextInputEditText>(R.id.tracker_confirm_volume)
        val inputChapter = view.findViewById<TextInputEditText>(R.id.tracker_confirm_chapter)
        val inputStatus = view.findViewById<MaterialAutoCompleteTextView>(R.id.tracker_confirm_status)
        val inputScore = view.findViewById<TextInputEditText>(R.id.tracker_confirm_score)

        val targetVol = inferredVolume ?: (track.volumesRead + 1)
        val targetChap = inferredChapter ?: (track.chaptersRead + 1)

        inputVolume.setText(targetVol.toString())
        inputChapter.setText(targetChap.toString())

        val statusOptions = TrackStatus.entries.map { it.description }
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, statusOptions)
        inputStatus.setAdapter(adapter)
        inputStatus.setText(track.status.description, false)

        inputScore.setText(track.score?.toString() ?: "")

        MaterialAlertDialogBuilder(context, R.style.AppCompatAlertDialogStyle)
            .setTitle(R.string.tracker_confirm_title)
            .setView(view)
            .setPositiveButton(R.string.tracker_confirm_update) { _, _ ->
                val confirmedVol = inputVolume.text?.toString()?.toIntOrNull() ?: targetVol
                val confirmedChap = inputChapter.text?.toString()?.toIntOrNull() ?: targetChap
                val selectedStatusStr = inputStatus.text?.toString()
                val status = TrackStatus.fromString(selectedStatusStr)
                val score = inputScore.text?.toString()?.trim()?.toFloatOrNull()

                track.volumesRead = maxOf(track.volumesRead, confirmedVol)
                track.chaptersRead = maxOf(track.chaptersRead, confirmedChap)
                track.status = status
                if (score != track.score) {
                    track.score = score
                    track.scoreDate = if (score != null) LocalDateTime.now() else null
                }
                track.lastSyncDate = LocalDateTime.now()

                val trackRepository = TrackRepository(context)
                trackRepository.update(track)

                onConfirmed(confirmedVol, confirmedChap)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}
