package br.com.fenix.bilingualreader.view.ui.reader.manga

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.service.controller.SubTitleController
import br.com.fenix.bilingualreader.service.repository.SubTitleRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Util
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream

class PopupMangaSubtitleConfiguration : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(PopupMangaSubtitleConfiguration::class.java)

    private lateinit var mPreferences: SharedPreferences
    private lateinit var mLoadExternalSubtitle: TextInputLayout
    private lateinit var mLoadExternalSubtitleAutoComplete: MaterialAutoCompleteTextView
    private lateinit var mSubtitleSelected: TextInputLayout
    private lateinit var mSubtitleSelectedAutoComplete: MaterialAutoCompleteTextView
    private lateinit var mSubtitleLanguage: TextInputLayout
    private lateinit var mSubtitleLanguageAutoComplete: MaterialAutoCompleteTextView
    private lateinit var mUsePageLinkInSearchTranslate: SwitchMaterial

    private lateinit var mSubTitleController: SubTitleController
    private lateinit var mMapLanguage: HashMap<String, Languages>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.popup_manga_subtitle_configuration, container, false)
        mPreferences = GeneralConsts.getSharedPreferences(requireContext())

        mLoadExternalSubtitle = root.findViewById(R.id.popup_manga_external_subtitle_select_path)
        mLoadExternalSubtitleAutoComplete =
            root.findViewById(R.id.popup_manga_menu_autocomplete_external_subtitle_select_path)
        mSubtitleSelected = root.findViewById(R.id.popup_manga_subtitle_selected)
        mSubtitleSelectedAutoComplete = root.findViewById(R.id.popup_manga_menu_autocomplete_subtitle_selected)

        mSubtitleLanguage = root.findViewById(R.id.popup_manga_subtitle_language)
        mSubtitleLanguageAutoComplete = root.findViewById(R.id.popup_manga_menu_autocomplete_subtitle_language)

        mUsePageLinkInSearchTranslate =
            root.findViewById(R.id.popup_manga_switch_use_page_linked_in_search_translate)

        mSubTitleController = SubTitleController.getInstance(requireContext())

        mUsePageLinkInSearchTranslate.isChecked = mPreferences.getBoolean(
            GeneralConsts.KEYS.PAGE_LINK.USE_IN_SEARCH_TRANSLATE,
            false
        )
        mUsePageLinkInSearchTranslate.setOnClickListener {
            mSubTitleController.setUseFileLink(mUsePageLinkInSearchTranslate.isChecked)
            mPreferences.edit().putBoolean(
                GeneralConsts.KEYS.PAGE_LINK.USE_IN_SEARCH_TRANSLATE,
                mUsePageLinkInSearchTranslate.isChecked
            ).apply()
        }

        observer()

        mSubtitleSelectedAutoComplete.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    mSubTitleController.clearSubtitlesSelected()
                }
            }

        mSubtitleSelectedAutoComplete.setOnClickListener {
            mSubtitleSelectedAutoComplete.setText("", false)
            mSubTitleController.clearSubtitlesSelected()
            if (mSubTitleController.chaptersKeys.value == null || mSubTitleController.chaptersKeys.value!!.isEmpty())
                Toast.makeText(
                    requireActivity(),
                    getString(R.string.popup_reading_manga_import_subtitle_is_empty),
                    Toast.LENGTH_SHORT
                ).show()
        }

        mSubtitleSelectedAutoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                mSubTitleController.selectedSubtitle(
                    parent.getItemAtPosition(position).toString()
                )
                MangaReaderActivity.selectTabReader()
            }

        mMapLanguage = Util.getLanguages(requireContext())

        mSubtitleLanguageAutoComplete.setAdapter(
            ArrayAdapter(
                requireContext(),
                R.layout.list_item,
                mMapLanguage.keys.toTypedArray()
            )
        )
        mSubtitleLanguageAutoComplete.setOnClickListener {
            mSubtitleLanguageAutoComplete.setText("", false)
            mSubTitleController.clearLanguage()
        }

        mSubtitleLanguageAutoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                if (parent.getItemAtPosition(position).toString().isNotEmpty() &&
                    mMapLanguage.containsKey(parent.getItemAtPosition(position).toString())
                )
                    mSubTitleController.selectedLanguage(
                        mMapLanguage[parent.getItemAtPosition(
                            position
                        ).toString()]!!
                    )
            }

        mLoadExternalSubtitleAutoComplete.setOnClickListener {
            val intent = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O)
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json|*/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            else
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json"))
                }
            startActivityForResult(intent, GeneralConsts.REQUEST.OPEN_JSON)
        }

        if (mSubTitleController.mManga != null && mSubTitleController.mManga!!.id != null) {
            val mSubtitleRepository = SubTitleRepository(requireContext())
            val lastSubtitle = mSubtitleRepository.findByIdManga(mSubTitleController.mManga!!.id!!)
            if (lastSubtitle != null) {
                mSubTitleController.initialize(lastSubtitle.chapterKey, lastSubtitle.pageKey)
            }
        }

        return root
    }

    private val externalTextWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable) {
            if (s.isEmpty())
                mSubTitleController.clearExternalSubtitlesSelected()
        }
    }

    override fun onResume() {
        super.onResume()
        mLoadExternalSubtitle.editText?.addTextChangedListener(externalTextWatcher)
    }

    override fun onPause() {
        super.onPause()
        mLoadExternalSubtitle.editText?.removeTextChangedListener(externalTextWatcher)
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int, resultData: Intent?
    ) {
        if (requestCode == GeneralConsts.REQUEST.OPEN_JSON) {
            if (resultCode == Activity.RESULT_OK) {
                resultData?.data?.also { uri ->
                    try {
                        val path = Util.normalizeFilePath(uri.path.toString())
                        val inputStream: InputStream = File(path).inputStream()
                        val inputString = inputStream.bufferedReader().use { it.readText() }
                        mLoadExternalSubtitleAutoComplete.setText(path)
                        mSubTitleController.getChapterFromJson(listOf(inputString), true)
                    } catch (e: Exception) {
                        mLOGGER.error("Error when open file: " + e.message, e)
                        Firebase.crashlytics.apply {
                            setCustomKey("message", "Error when open file: " + e.message)
                            recordException(e)
                        }
                    }
                }
            } else
                mLoadExternalSubtitleAutoComplete.setText("")
        }
    }

    private fun observer() {
        mSubTitleController.chaptersKeys.observe(viewLifecycleOwner) {
            mSubtitleSelectedAutoComplete.setAdapter(
                ArrayAdapter(
                    requireContext(),
                    R.layout.list_item,
                    it.sorted()
                )
            )
        }

        mSubTitleController.subTitleChapterSelected.observe(viewLifecycleOwner) {
            var text = ""
            if (it != null)
                text = mSubTitleController.getChapterKey(it)

            mSubtitleSelectedAutoComplete.setText(text, false)
        }

    }

}