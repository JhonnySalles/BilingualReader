package br.com.fenix.bilingualreader.view.ui.detail.book

import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Information
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.service.controller.ImageController
import br.com.fenix.bilingualreader.service.listener.InformationCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.ColorUtil
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.LibraryUtil
import br.com.fenix.bilingualreader.util.helpers.ListUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.view.adapter.detail.TagsCardAdapter
import br.com.fenix.bilingualreader.view.adapter.detail.manga.InformationRelatedCardAdapter
import br.com.fenix.bilingualreader.view.ui.detail.DetailActivity
import br.com.fenix.bilingualreader.view.ui.popup.PopupBookMark
import br.com.fenix.bilingualreader.view.ui.popup.PopupTags
import br.com.fenix.bilingualreader.view.ui.reader.book.BookReaderActivity
import br.com.fenix.bilingualreader.view.ui.vocabulary.VocabularyActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import org.lucasr.twowayview.TwoWayView
import org.slf4j.LoggerFactory


class BookDetailFragment : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(BookDetailFragment::class.java)

    private val mViewModel: BookDetailViewModel by viewModels()

    private lateinit var mRootScroll: NestedScrollView
    private lateinit var mBackgroundImage: ImageView
    private lateinit var mImage: ImageView
    private lateinit var mTitle: TextView
    private lateinit var mAuthor: TextView
    private lateinit var mChapter: TextView
    private lateinit var mFolder: TextView
    private lateinit var mFileType: TextView
    private lateinit var mLastAccess: TextView
    private lateinit var mDeleted: TextView
    private lateinit var mBookMark: TextView
    private lateinit var mProgress: ProgressBar
    private lateinit var mButtonsContent: LinearLayout
    private lateinit var mFavoriteButton: MaterialButton
    private lateinit var mMakReadButton: MaterialButton
    private lateinit var mBookMakButton: MaterialButton
    private lateinit var mAddTagButton: MaterialButton
    private lateinit var mClearHistoryButton: MaterialButton
    private lateinit var mDeleteButton: MaterialButton
    private lateinit var mVocabularyButton: MaterialButton
    private lateinit var mChaptersList: ListView
    private lateinit var mFileLinkContent: LinearLayout
    private lateinit var mFileLinksList: ListView

    private lateinit var mInformationContent: LinearLayout
    private lateinit var mInformationImage: ImageView
    private lateinit var mInformationSynopsis: TextView
    private lateinit var mInformationTitle: TextView
    private lateinit var mInformationAuthor: TextView
    private lateinit var mInformationAnnotation: TextView
    private lateinit var mInformationPublish: TextView
    private lateinit var mInformationRelease: TextView
    private lateinit var mInformationLanguage: TextView
    private lateinit var mInformationVolume: TextView
    private lateinit var mInformationIsbn: TextView
    private lateinit var mInformationGenres: TextView
    private lateinit var mInformationFile: TextView
    private lateinit var mInformationTags: TwoWayView
    private lateinit var mTagsList: ListView

    private lateinit var mWebInformationContent: LinearLayout
    private lateinit var mWebInformationImage: ImageView
    private lateinit var mWebInformationSynopsis: TextView
    private lateinit var mWebInformationAlternativeTitles: TextView
    private lateinit var mWebInformationStatus: TextView
    private lateinit var mWebInformationPublish: TextView
    private lateinit var mWebInformationVolumes: TextView
    private lateinit var mWebInformationAuthors: TextView
    private lateinit var mWebInformationGenres: TextView
    private lateinit var mWebInformationOrigin: TextView
    private lateinit var mWebInfoRelatedContent: LinearLayout
    private lateinit var mWebInfoRelatedRelatedList: RecyclerView
    private lateinit var mWebInfoRelatedOrigin: TextView
    private lateinit var mWebInfoListener: InformationCardListener

    private lateinit var mBookLanguage: TextInputLayout
    private lateinit var mBookLanguageAutoComplete: MaterialAutoCompleteTextView

    private lateinit var mListener: InformationCardListener
    private lateinit var mPopupTag: PopupTags

    private var mSubtitles: MutableList<String> = mutableListOf()
    private var mChapters: MutableList<String> = mutableListOf()
    private var mFileLinks: MutableList<String> = mutableListOf()
    private var mTags: MutableList<String> = mutableListOf()
    private var mMapLanguage: HashMap<String, Languages> = hashMapOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_book_detail, container, false)

        mRootScroll = root.findViewById(R.id.book_detail_scroll)
        mBackgroundImage = root.findViewById(R.id.book_detail_background_image)
        mImage = root.findViewById(R.id.book_detail_book_image)

        mBookLanguage = root.findViewById(R.id.book_detail_information_book_language)
        mBookLanguageAutoComplete = root.findViewById(R.id.book_detail_information_menu_autocomplete_language)

        mTitle = root.findViewById(R.id.book_detail_title)
        mAuthor = root.findViewById(R.id.book_detail_author)
        mChapter = root.findViewById(R.id.book_detail_chapter)
        mFolder = root.findViewById(R.id.book_detail_folder)
        mFileType = root.findViewById(R.id.book_detail_file_type)
        mLastAccess = root.findViewById(R.id.book_detail_last_access)
        mDeleted = root.findViewById(R.id.book_detail_deleted)
        mBookMark = root.findViewById(R.id.book_detail_book_mark)
        mProgress = root.findViewById(R.id.book_detail_progress)

        mButtonsContent = root.findViewById(R.id.book_detail_buttons)
        mFavoriteButton = root.findViewById(R.id.book_detail_button_favorite)
        mClearHistoryButton = root.findViewById(R.id.book_detail_button_clear_history)
        mMakReadButton = root.findViewById(R.id.book_detail_button_mark_read)
        mBookMakButton = root.findViewById(R.id.book_detail_button_book_mark)
        mAddTagButton = root.findViewById(R.id.book_detail_button_add_tag)
        mDeleteButton = root.findViewById(R.id.book_detail_button_delete)
        mVocabularyButton = root.findViewById(R.id.book_detail_button_vocabulary)

        mChaptersList = root.findViewById(R.id.book_detail_chapters_list)
        mFileLinkContent = root.findViewById(R.id.book_detail_files_link_detail)
        mFileLinksList = root.findViewById(R.id.book_detail_files_links_list)

        mInformationContent = root.findViewById(R.id.book_detail_information)
        mInformationImage = root.findViewById(R.id.book_detail_information_image)
        mInformationSynopsis = root.findViewById(R.id.book_detail_information_synopsis)
        mInformationTitle = root.findViewById(R.id.book_detail_information_title)
        mInformationAuthor = root.findViewById(R.id.book_detail_information_author)
        mInformationAnnotation = root.findViewById(R.id.book_detail_information_annotation)
        mInformationPublish = root.findViewById(R.id.book_detail_information_publish)
        mInformationLanguage = root.findViewById(R.id.book_detail_information_language)
        mInformationRelease = root.findViewById(R.id.book_detail_information_release)
        mInformationVolume = root.findViewById(R.id.book_detail_information_volume)
        mInformationIsbn = root.findViewById(R.id.book_detail_information_isbn)
        mInformationGenres = root.findViewById(R.id.book_detail_information_genres)
        mInformationFile = root.findViewById(R.id.book_detail_information_file)
        mInformationTags = root.findViewById(R.id.book_detail_information_tags)
        mTagsList = root.findViewById(R.id.book_detail_information_tags_list)

        mWebInformationContent = root.findViewById(R.id.book_detail_web_information)
        mWebInformationImage = root.findViewById(R.id.book_detail_web_information_image)
        mWebInformationSynopsis = root.findViewById(R.id.book_detail_web_information_synopsis)
        mWebInformationAlternativeTitles = root.findViewById(R.id.book_detail_web_information_alternative_titles)
        mWebInformationStatus = root.findViewById(R.id.book_detail_web_information_status)
        mWebInformationPublish = root.findViewById(R.id.book_detail_web_information_publish)
        mWebInformationVolumes = root.findViewById(R.id.book_detail_web_information_volumes_chapters)
        mWebInformationAuthors = root.findViewById(R.id.book_detail_web_information_author)
        mWebInformationGenres = root.findViewById(R.id.book_detail_web_information_genres)
        mWebInformationOrigin = root.findViewById(R.id.book_detail_web_information_origin)
        mWebInfoRelatedContent = root.findViewById(R.id.book_detail_web_information_relations)
        mWebInfoRelatedRelatedList = root.findViewById(R.id.book_detail_relations_lists)
        mWebInfoRelatedOrigin = root.findViewById(R.id.book_detail_relations_origin)

        val languages = resources.getStringArray(R.array.languages)
        mMapLanguage = hashMapOf(
            languages[1] to Languages.ENGLISH,
            languages[2] to Languages.JAPANESE,
            languages[0] to Languages.PORTUGUESE
        )

        mBookLanguageAutoComplete.setAdapter(ArrayAdapter(requireContext(), R.layout.list_item, mMapLanguage.keys.toTypedArray()))
        mBookLanguageAutoComplete.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
                val lang = if (parent.getItemAtPosition(position).toString().isNotEmpty() && mMapLanguage.containsKey(parent.getItemAtPosition(position).toString()))
                        mMapLanguage[parent.getItemAtPosition(position).toString()]!!
                    else
                        mViewModel.book.value?.language ?: Languages.JAPANESE

                mViewModel.changeLanguage(lang)
            }

        mFavoriteButton.setOnClickListener { favorite() }
        mMakReadButton.setOnClickListener {
            (mMakReadButton.icon as AnimatedVectorDrawable).start()
            markRead()
        }
        mAddTagButton.setOnClickListener {
            (mAddTagButton.icon as AnimatedVectorDrawable).start()
            mViewModel.book.value?.run {
                mPopupTag.getPopupTags(this) {
                    mViewModel.loadTags()
                }
            }
        }
        mBookMakButton.setOnClickListener {
            (mBookMakButton.icon as AnimatedVectorDrawable).start()
            openBookMark()
        }
        mDeleteButton.setOnClickListener {
            (mDeleteButton.icon as AnimatedVectorDrawable).start()
            deleteFile()
        }
        mClearHistoryButton.setOnClickListener {
            (mClearHistoryButton.icon as AnimatedVectorDrawable).start()
            clearHistory()
        }
        mVocabularyButton.setOnClickListener {
            (mVocabularyButton.icon as AnimatedVectorDrawable).start()
            openVocabulary()
        }

        mFileLinksList.adapter = ArrayAdapter(requireContext(), R.layout.list_item_detail, mFileLinks)
        mChaptersList.adapter = ArrayAdapter(requireContext(), R.layout.list_item_detail, mChapters)
        mTagsList.adapter = ArrayAdapter(requireContext(), R.layout.list_item_detail, mTags)
        mInformationTags.adapter = TagsCardAdapter(requireContext(), mutableListOf())

        mWebInfoRelatedRelatedList.adapter = InformationRelatedCardAdapter()
        mWebInfoRelatedRelatedList.layoutManager = LinearLayoutManager(requireContext())

        mPopupTag = PopupTags(requireContext())

        mTitle.setOnLongClickListener {
            mViewModel.book.value?.let { bk -> FileUtil(requireContext()).copyName(bk) }
            true
        }

        mChaptersList.setOnItemClickListener { _, _, index, _ ->
            mViewModel.book.value?.let {
                if (index >= 0 && mChapters.size > index) {
                    val chapter = mChapters[index]
                    val page = mViewModel.getPage(chapter)
                    it.bookMark = page
                    mViewModel.save(it)
                    val intent = Intent(context, BookReaderActivity::class.java)
                    val bundle = Bundle()
                    bundle.putSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY, mViewModel.library)
                    bundle.putString(GeneralConsts.KEYS.BOOK.NAME, it.title)
                    bundle.putInt(GeneralConsts.KEYS.BOOK.MARK, it.bookMark)
                    bundle.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK, it)
                    intent.putExtras(bundle)
                    context?.startActivity(intent)
                }
            }
        }

        mInformationContent.setOnLongClickListener {
            if (mViewModel.information.value != null)
                openUrl(mViewModel.information.value!!.link)
            true
        }

        mListener = object : InformationCardListener {
            override fun onClickLong(url: String) {
                openUrl(url)
            }
        }

        mWebInformationContent.setOnLongClickListener {
            if (mViewModel.webInformation.value != null)
                openUrl(mViewModel.webInformation.value!!.link)
            true
        }

        mWebInfoListener = object : InformationCardListener {
            override fun onClickLong(url: String) {
                openUrl(url)
            }
        }

        (mWebInfoRelatedRelatedList.adapter as InformationRelatedCardAdapter).attachListener(
            mWebInfoListener
        )

        observer()

        arguments?.let {
            mViewModel.library = if (it.containsKey(GeneralConsts.KEYS.OBJECT.LIBRARY))
                it[GeneralConsts.KEYS.OBJECT.LIBRARY] as Library
            else
                LibraryUtil.getDefault(requireContext(), Type.BOOK)

            if (it.containsKey(GeneralConsts.KEYS.OBJECT.BOOK))
                mViewModel.setBook(requireContext(), it[GeneralConsts.KEYS.OBJECT.BOOK] as Book)
        }

        return root
    }

    private fun observer() {
        mViewModel.cover.observe(viewLifecycleOwner) {
            val isDark = resources.getBoolean(R.bool.isNight)
            ThemeUtil.changeStatusColorFromListener(requireActivity().window, mRootScroll, false, isDark)
            if (it != null) {
                mBackgroundImage.setImageBitmap(it)
                mImage.setImageBitmap(it)
                ColorUtil.isDarkColor(it) { l ->
                    ThemeUtil.changeStatusColorFromListener(requireActivity().window, mRootScroll, l, isDark)
                }
            }
        }

        mViewModel.book.observe(viewLifecycleOwner) {
            if (it != null) {
                mBookLanguageAutoComplete.setText(
                    mMapLanguage.entries.first { lan -> lan.value == it.language }.key,
                    false
                )

                mTitle.text = it.title
                mFolder.text = it.path
                mFileType.text = it.fileType.toString()
                mAuthor.text = it.author
                mChapter.text = ""

                val percent: Float = if (it.bookMark > 0) ((it.bookMark.toFloat() / it.pages) * 100) else 0f
                mBookMark.text = "${it.bookMark} / ${it.pages} (${Util.formatDecimal(percent)} %)"
                mLastAccess.text = if (it.lastAccess == null) "" else GeneralConsts.formatterDate(
                    requireContext(),
                    it.lastAccess!!
                )
                mProgress.max = it.pages
                mProgress.setProgress(it.bookMark, false)

                if (mFavoriteButton.tag != it.favorite) {
                    mFavoriteButton.setIconResource(if (it.favorite) R.drawable.ico_animated_favorited_marked else R.drawable.ico_animated_favorited_unmarked)
                    (mFavoriteButton.icon as AnimatedVectorDrawable).start()
                }
                mFavoriteButton.tag = it.favorite

                if (it.excluded) {
                    mDeleted.text = getString(R.string.book_detail_book_deleted)
                    mDeleted.visibility = View.VISIBLE
                } else {
                    mDeleted.text = ""
                    mDeleted.visibility = View.GONE
                }
            } else {
                mBookLanguageAutoComplete.setText("", false)

                mTitle.text = ""
                mFolder.text = ""
                mFileType.text = ""
                mAuthor.text = ""
                mChapter.text = ""
                mLastAccess.text = ""
                mDeleted.text = ""
                mBookMark.text = ""
                mProgress.max = 1
                mProgress.setProgress(0, false)
                mFavoriteButton.setIconResource(R.drawable.ico_favorite_unmark)
            }
        }

        mViewModel.listChapters.observe(viewLifecycleOwner) {
            mChapters.clear()
            mChapters.addAll(it)
            (mChaptersList.adapter as ArrayAdapter<*>).notifyDataSetChanged()
        }

        mViewModel.listLinkedFileLinks.observe(viewLifecycleOwner) { fileLinks ->
            val list = fileLinks?.map { it.path }?.toMutableList() ?: mutableListOf()
            mFileLinks.clear()
            mFileLinks.addAll(list)
            (mFileLinksList.adapter as ArrayAdapter<*>).notifyDataSetChanged()
            mFileLinkContent.visibility = if (mFileLinks.isNotEmpty())
                View.VISIBLE
            else
                View.GONE
        }

        mViewModel.tags.observe(viewLifecycleOwner) {
            mTags.clear()
            mTags.addAll(it.map { t -> t.name })
            (mTagsList.adapter as ArrayAdapter<*>).notifyDataSetChanged()
        }

        mViewModel.information.observe(viewLifecycleOwner) {
            if (it != null) {
                mInformationContent.visibility = View.VISIBLE
                mInformationSynopsis.text = it.synopsis.replace("\\n", "\n")

                mInformationImage.visibility = View.GONE
                mInformationImage.setImageBitmap(null)

                mInformationAnnotation.text = HtmlCompat.fromHtml(requireContext().getString(R.string.book_detail_information_annotation, it.annotation), HtmlCompat.FROM_HTML_MODE_COMPACT)
                mInformationPublish.text = HtmlCompat.fromHtml(requireContext().getString(R.string.book_detail_information_publisher, it.publisher), HtmlCompat.FROM_HTML_MODE_COMPACT)
                mInformationAuthor.text = HtmlCompat.fromHtml(requireContext().getString(R.string.book_detail_information_author, it.authors), HtmlCompat.FROM_HTML_MODE_COMPACT)
                mInformationTitle.text = HtmlCompat.fromHtml(requireContext().getString(R.string.book_detail_information_title, it.title), HtmlCompat.FROM_HTML_MODE_COMPACT)
                mInformationRelease.text = HtmlCompat.fromHtml(requireContext().getString(R.string.book_detail_information_release, it.release), HtmlCompat.FROM_HTML_MODE_COMPACT)
                mInformationVolume.text = HtmlCompat.fromHtml(requireContext().getString(R.string.book_detail_information_volume, it.volumes), HtmlCompat.FROM_HTML_MODE_COMPACT)
                mInformationLanguage.text = HtmlCompat.fromHtml(requireContext().getString(R.string.book_detail_information_language, it.language), HtmlCompat.FROM_HTML_MODE_COMPACT)
                mInformationIsbn.text = HtmlCompat.fromHtml(requireContext().getString(R.string.book_detail_information_isbn, it.isbn), HtmlCompat.FROM_HTML_MODE_COMPACT)
                mInformationGenres.text = HtmlCompat.fromHtml(requireContext().getString(R.string.book_detail_information_genres, it.genres), HtmlCompat.FROM_HTML_MODE_COMPACT)
                mInformationFile.text = HtmlCompat.fromHtml(requireContext().getString(R.string.book_detail_information_file, it.file), HtmlCompat.FROM_HTML_MODE_COMPACT)

                val genres = ListUtil.listFromString(it.genres)
                if (genres.isNotEmpty())
                    (mInformationTags.adapter as TagsCardAdapter).updateList(genres.toMutableList())
                else
                    (mInformationTags.adapter as TagsCardAdapter).clearList()

            } else {
                mInformationContent.visibility = View.GONE
                mInformationImage.visibility = View.GONE

                mInformationSynopsis.text = ""
                mInformationAnnotation.text = HtmlCompat.fromHtml(requireContext().getString(R.string.book_detail_information_annotation, ""), HtmlCompat.FROM_HTML_MODE_COMPACT)
                mInformationPublish.text = HtmlCompat.fromHtml(requireContext().getString(R.string.book_detail_information_publisher, ""), HtmlCompat.FROM_HTML_MODE_COMPACT)
                mInformationAuthor.text = HtmlCompat.fromHtml(requireContext().getString(R.string.book_detail_information_author, ""), HtmlCompat.FROM_HTML_MODE_COMPACT)
                mInformationTitle.text = HtmlCompat.fromHtml(requireContext().getString(R.string.book_detail_information_title, ""), HtmlCompat.FROM_HTML_MODE_COMPACT)
                mInformationRelease.text = HtmlCompat.fromHtml(requireContext().getString(R.string.book_detail_information_release, ""), HtmlCompat.FROM_HTML_MODE_COMPACT)
                mInformationLanguage.text = HtmlCompat.fromHtml(requireContext().getString(R.string.book_detail_information_language, ""), HtmlCompat.FROM_HTML_MODE_COMPACT)
                mInformationVolume.text = HtmlCompat.fromHtml(requireContext().getString(R.string.book_detail_information_volume, ""), HtmlCompat.FROM_HTML_MODE_COMPACT)
                mInformationIsbn.text = HtmlCompat.fromHtml(requireContext().getString(R.string.book_detail_information_isbn, ""), HtmlCompat.FROM_HTML_MODE_COMPACT)
                mInformationGenres.text = HtmlCompat.fromHtml(requireContext().getString(R.string.book_detail_information_genres, ""), HtmlCompat.FROM_HTML_MODE_COMPACT)
                mInformationFile.text = HtmlCompat.fromHtml(requireContext().getString(R.string.book_detail_information_file, ""), HtmlCompat.FROM_HTML_MODE_COMPACT)

                (mInformationTags.adapter as TagsCardAdapter).clearList()
            }
        }

        mViewModel.webInformation.observe(viewLifecycleOwner) {
            if (it != null) {
                mWebInformationContent.visibility = View.VISIBLE
                mWebInformationSynopsis.text = it.synopsis.replace("\\n", "\n")

                mWebInformationImage.visibility = View.GONE
                mWebInformationImage.setImageBitmap(null)

                if (it.imageLink != null)
                    ImageController.instance.setImageAsync(requireContext(), it.imageLink!!, mWebInformationImage)

                mWebInformationAlternativeTitles.text = HtmlCompat.fromHtml(it.alternativeTitles, HtmlCompat.FROM_HTML_MODE_COMPACT)
                mWebInformationStatus.text = HtmlCompat.fromHtml(it.status, HtmlCompat.FROM_HTML_MODE_COMPACT)
                mWebInformationPublish.text = HtmlCompat.fromHtml(it.release, HtmlCompat.FROM_HTML_MODE_COMPACT)
                mWebInformationVolumes.text = HtmlCompat.fromHtml(it.volumes + ", " + it.chapters, HtmlCompat.FROM_HTML_MODE_COMPACT)
                mWebInformationAuthors.text = HtmlCompat.fromHtml(it.authors, HtmlCompat.FROM_HTML_MODE_COMPACT)
                mWebInformationGenres.text = HtmlCompat.fromHtml(it.genres, HtmlCompat.FROM_HTML_MODE_COMPACT)
                mWebInformationOrigin.text = it.origin
            } else {
                mWebInformationContent.visibility = View.GONE
                mWebInformationImage.visibility = View.GONE

                mWebInformationSynopsis.text = ""
                mWebInformationAlternativeTitles.text = ""
                mWebInformationStatus.text = ""
                mWebInformationPublish.text = ""
                mWebInformationVolumes.text = ""
                mWebInformationAuthors.text = ""
                mWebInformationGenres.text = ""
                mWebInformationOrigin.text = ""
            }
        }

        mViewModel.webInformationRelations.observe(viewLifecycleOwner) {
            mWebInfoRelatedContent.visibility = if (it != null && it.isNotEmpty()) View.VISIBLE else View.GONE
            updateRelatedList(it)
        }
    }

    override fun onResume() {
        super.onResume()
        mViewModel.getInformation()
    }

    private fun deleteFile() {
        val book = mViewModel.book.value ?: return
        MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
            .setTitle(getString(R.string.book_library_menu_delete))
            .setMessage(getString(R.string.book_library_menu_delete_description) + "\n" + book.file.name)
            .setPositiveButton(
                R.string.action_positive
            ) { _, _ ->
                mViewModel.delete()
                if (book.file.exists()) {
                    val isDeleted = book.file.delete()
                    mLOGGER.info("File deleted ${book.name}: $isDeleted")
                }
                (requireActivity() as DetailActivity).onBackPressed()
            }
            .setNegativeButton(
                R.string.action_negative
            ) { _, _ -> }
            .create().show()
    }

    private fun clearHistory() {
        mViewModel.clearHistory()
    }

    private fun markRead() {
        mViewModel.markRead()
    }

    private fun openBookMark() {
        val book = mViewModel.book.value ?: return
        val onUpdate: (History) -> (Unit) = { mViewModel.save(book) }
        PopupBookMark(requireActivity(), requireActivity().supportFragmentManager)
            .getPopupBookMark(book, onUpdate) { change, book ->
                if (change)
                    onUpdate(book)
            }
    }

    private fun favorite() {
        val book = mViewModel.book.value ?: return
        book.favorite = !book.favorite
        mViewModel.save(book)
    }

    private fun updateRelatedList(list: MutableList<Information>?) {
        (mWebInfoRelatedRelatedList.adapter as InformationRelatedCardAdapter).updateList(list)
        mWebInfoRelatedOrigin.text = if (list.isNullOrEmpty()) "" else list[0].origin
    }

    private fun openUrl(url: String) {
        if (url.isEmpty()) return

        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )
        )
    }

    private fun openVocabulary() {
        mViewModel.book.value?.let {
            val intent = Intent(requireContext(), VocabularyActivity::class.java)
            val bundle = Bundle()
            bundle.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK, it)
            bundle.putSerializable(GeneralConsts.KEYS.VOCABULARY.TYPE, Type.BOOK)
            intent.putExtras(bundle)
            requireActivity().overridePendingTransition(
                R.anim.fade_in_fragment_add_enter,
                R.anim.fade_out_fragment_remove_exit
            )
            startActivity(intent)
        }
    }
}