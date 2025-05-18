package br.com.fenix.bilingualreader.view.ui.reader.manga

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.service.controller.SubTitleController
import br.com.fenix.bilingualreader.service.japanese.Formatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout


class PopupMangaSubtitleReader : Fragment() {

    private lateinit var mSubtitlePage: TextInputLayout
    private lateinit var mSubtitlePageAutoComplete: MaterialAutoCompleteTextView
    private lateinit var mSubtitleTitle: TextView
    private lateinit var mSubtitleContent: TextView
    private lateinit var mNavBeforeText: MaterialButton
    private lateinit var mNavNextText: MaterialButton
    private lateinit var mRefresh: MaterialButton
    private lateinit var mDraw: MaterialButton
    private lateinit var mChangeLanguage: MaterialButton
    private lateinit var mLabelChapter: String
    private lateinit var mLabelText: String

    private lateinit var mSubTitleController: SubTitleController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_manga_subtitle_reader, container, false)

        mSubtitlePageAutoComplete = root.findViewById(R.id.popup_manga_subtitle_menu_autocomplete_page_selected)
        mSubtitlePage = root.findViewById(R.id.popup_manga_subtitle_page_selected)
        mSubtitleTitle = root.findViewById(R.id.popup_manga_subtitle_subtitle_title)
        mSubtitleContent = root.findViewById(R.id.popup_manga_subtitle_text_content)
        mNavBeforeText = root.findViewById(R.id.popup_manga_subtitle_before_text)
        mNavNextText = root.findViewById(R.id.popup_manga_subtitle_next_text)
        mRefresh = root.findViewById(R.id.popup_manga_subtitle_refresh)
        mDraw = root.findViewById(R.id.popup_manga_subtitle_draw_text)
        mChangeLanguage = root.findViewById(R.id.popup_manga_subtitle_change_language)

        mSubtitleContent.movementMethod = LinkMovementMethod.getInstance()

        mLabelChapter = getString(R.string.popup_reading_manga_subtitle_chapter)
        mLabelText = getString(R.string.popup_reading_manga_subtitle_text)

        mSubtitleContent.setOnLongClickListener {
            if (mSubtitleContent.text.isNotEmpty()) {
                val clipboard =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Copied Text", mSubtitleContent.text)
                clipboard.setPrimaryClip(clip)

                Toast.makeText(
                    requireActivity(),
                    getString(R.string.action_copy, mSubtitleContent.text),
                    Toast.LENGTH_SHORT
                ).show()
            }
            true
        }

        mSubTitleController = SubTitleController.getInstance(requireContext())

        mNavBeforeText.setOnClickListener {
            (mNavBeforeText.icon as AnimatedVectorDrawable).start()
            mSubTitleController.getBeforeText()
        }
        mNavNextText.setOnClickListener {
            (mNavNextText.icon as AnimatedVectorDrawable).start()
            mSubTitleController.getNextText()
        }

        mRefresh.setOnClickListener {
            (mRefresh.icon as AnimatedVectorDrawable).start()
            mSubTitleController.findSubtitle()
        }
        mDraw.setOnClickListener {
            (mDraw.icon as AnimatedVectorDrawable).start()
            mSubTitleController.drawSelectedText()
        }

        mChangeLanguage.setOnClickListener {
            (mChangeLanguage.icon as AnimatedVectorDrawable).start()
            mSubTitleController.changeLanguage()
        }

        mSubtitlePageAutoComplete.setOnClickListener {
            mSubtitlePageAutoComplete.setText("", false)

            if (mSubTitleController.pagesKeys.value == null || mSubTitleController.pagesKeys.value!!.isEmpty())
                Toast.makeText(
                    requireActivity(),
                    getString(R.string.popup_reading_manga_import_subtitle_page_is_empty),
                    Toast.LENGTH_SHORT
                ).show()
        }

        mSubtitlePageAutoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                mSubTitleController.selectedPage(
                    parent.getItemAtPosition(position).toString()
                )
            }

        observer()
        return root
    }

    private fun observer() {
        mSubTitleController.pagesKeys.observe(viewLifecycleOwner) {
            mSubtitlePageAutoComplete.setAdapter(
                ArrayAdapter(
                    requireContext(),
                    R.layout.list_item,
                    it.sorted()
                )
            )
        }

        mSubTitleController.subTitlePageSelected.observe(viewLifecycleOwner) {
            var key = ""
            if (it != null)
                key = mSubTitleController.getPageKey(it)

            mSubtitlePageAutoComplete.setText(key, false)
        }

        mSubTitleController.subTitleTextSelected.observe(viewLifecycleOwner) {
            var title = ""
            mSubtitleContent.text = ""
            if (it != null) {
                val index =
                    mSubTitleController.subTitlePageSelected.value?.subTitleTexts?.indexOf(mSubTitleController.subTitleTextSelected.value)
                        ?.plus(1)
                title =
                    "$mLabelChapter ${mSubTitleController.subTitleChapterSelected.value?.chapter.toString()} - $mLabelText $index/${mSubTitleController.subTitlePageSelected.value?.subTitleTexts?.size}"

                Formatter.generateKanjiColor(requireContext(), SpannableString(it.text)) { kanji ->
                    mSubtitleContent.text = kanji
                }
            } else if (mSubTitleController.subTitleChapterSelected.value != null && mSubTitleController.subTitlePageSelected.value != null)
                title =
                    "$mLabelChapter ${mSubTitleController.subTitleChapterSelected.value?.chapter.toString()} - $mLabelText 0/${if (mSubTitleController.subTitlePageSelected.value?.subTitleTexts == null) 0 else mSubTitleController.subTitlePageSelected.value?.subTitleTexts?.size}"

            mSubtitleTitle.text = title
        }
    }

}