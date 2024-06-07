package br.com.fenix.bilingualreader.view.ui.popup

import android.content.Context
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.entity.Tags
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.service.repository.BookAnnotationRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import org.slf4j.LoggerFactory


class PopupAnnotations(var context: Context) {

    companion object {
        private val mLOGGER = LoggerFactory.getLogger(PopupAnnotations::class.java)

        fun generateClick(context: Context, annotation: BookAnnotation, color: Int, onDelete: (BookAnnotation) -> (Unit), onClose: (Boolean) -> (Unit)): ClickableSpan {
            return object : ClickableSpan() {
                override fun onClick(p0: View) {
                    PopupAnnotations(context).popup(annotation, onDelete, onClose)
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                    ds.color = color
                }
            }
        }
    }

    private val mRepository = BookAnnotationRepository(context)
    private var mTags = mutableListOf<Tags>()
    private lateinit var mEdit: TextInputEditText
    private lateinit var mPopup: AlertDialog
    private lateinit var mAnnotation: BookAnnotation

    fun popup(annotation: BookAnnotation, onDelete: (BookAnnotation) -> (Unit), onClose: (Boolean) -> (Unit)) {
        mAnnotation = BookAnnotation(annotation)
        mPopup = MaterialAlertDialogBuilder(context, R.style.AppCompatMaterialAlertDialog)
            .setView(createPopup(context, LayoutInflater.from(context), annotation))
            .setNeutralButton(R.string.book_annotation_popup_delete) { _, _ ->
                mRepository.delete(annotation)
                mPopup.dismiss()
                onDelete(mAnnotation)
            }
            .setNegativeButton(R.string.book_annotation_popup_cancel) { _, _ -> onClose(false) }
            .setPositiveButton(R.string.book_annotation_popup_confirm) { _, _ ->
                mAnnotation.annotation = mEdit.text.toString()
                annotation.update(mAnnotation)
                if (annotation.id == null)
                    mRepository.save(annotation)
                else
                    mRepository.update(annotation)
                mPopup.dismiss()
                onClose(true)
            }
            .create()
        mPopup.show()
    }

    private fun createPopup(context: Context, inflater: LayoutInflater, annotation: BookAnnotation): View? {
        val root = inflater.inflate(R.layout.popup_annotations, null, false)
        root.findViewById<TextView>(R.id.popup_annotation_title).text = context.getString(R.string.book_annotation_popup_title, annotation.page, context.getString(annotation.type.getDescription()))
        mEdit = root.findViewById(R.id.popup_annotation_edit)
        mEdit.setText(annotation.annotation)

        val yellow = root.findViewById<MaterialButton>(R.id.popup_annotation_yellow)
        val green = root.findViewById<MaterialButton>(R.id.popup_annotation_green)
        val blue = root.findViewById<MaterialButton>(R.id.popup_annotation_blue)
        val red = root.findViewById<MaterialButton>(R.id.popup_annotation_red)

        val buttons = mutableListOf<MaterialButton>()
        buttons.add(yellow)
        buttons.add(blue)
        buttons.add(red)
        buttons.add(green)

        val changeColor = {
            for (btn in buttons)
                btn.icon = when (btn.id) {
                    R.id.popup_annotation_yellow -> AppCompatResources.getDrawable(
                        context,
                        if (mAnnotation.color == Color.Yellow) R.drawable.ic_text_view_select_yellow_selected else R.drawable.ic_text_view_select_yellow
                    )

                    R.id.popup_annotation_green -> AppCompatResources.getDrawable(
                        context,
                        if (mAnnotation.color == Color.Green) R.drawable.ic_text_view_select_green_selected else R.drawable.ic_text_view_select_green
                    )

                    R.id.popup_annotation_blue -> AppCompatResources.getDrawable(
                        context,
                        if (mAnnotation.color == Color.Blue) R.drawable.ic_text_view_select_blue_selected else R.drawable.ic_text_view_select_blue
                    )

                    R.id.popup_annotation_red -> AppCompatResources.getDrawable(
                        context,
                        if (mAnnotation.color == Color.Red) R.drawable.ic_text_view_select_red_selected else R.drawable.ic_text_view_select_red
                    )

                    else -> btn.icon
                }
        }

        yellow.setOnClickListener {
            mAnnotation.color = Color.Yellow
            changeColor()
        }
        green.setOnClickListener {
            mAnnotation.color = Color.Green
            changeColor()
        }
        blue.setOnClickListener {
            mAnnotation.color = Color.Blue
            changeColor()
        }
        red.setOnClickListener {
            mAnnotation.color = Color.Red
            changeColor()
        }
        changeColor()
        return root
    }

}