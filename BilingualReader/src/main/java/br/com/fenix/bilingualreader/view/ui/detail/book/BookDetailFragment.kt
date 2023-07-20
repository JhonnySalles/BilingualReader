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
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import br.com.fenix.bilingualreader.service.listener.InformationCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.LibraryUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.view.ui.detail.DetailActivity
import br.com.fenix.bilingualreader.view.ui.reader.manga.MangaReaderActivity
import br.com.fenix.bilingualreader.view.ui.vocabulary.VocabularyActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import org.slf4j.LoggerFactory


class BookDetailFragment : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(BookDetailFragment::class.java)

    private val mViewModel: BookDetailViewModel by viewModels()

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
    private lateinit var mClearHistoryButton: MaterialButton
    private lateinit var mDeleteButton: MaterialButton
    private lateinit var mVocabularyButton: MaterialButton
    private lateinit var mChaptersList: ListView
    private lateinit var mFileLinkContent: LinearLayout
    private lateinit var mFileLinksList: ListView

    private lateinit var mInformationContent: LinearLayout
    private lateinit var mInformationImage: ImageView
    private lateinit var mInformationSynopsis: TextView
    private lateinit var mInformationAnnotation: TextView
    private lateinit var mInformationPublish: TextView
    private lateinit var mInformationYear: TextView
    private lateinit var mInformationIsbn: TextView
    private lateinit var mInformationGenres: TextView
    private lateinit var mInformationFile: TextView

    private lateinit var mBookLanguage: TextInputLayout
    private lateinit var mBookLanguageAutoComplete: AutoCompleteTextView

    private lateinit var mListener: InformationCardListener

    private var mSubtitles: MutableList<String> = mutableListOf()
    private var mChapters: MutableList<String> = mutableListOf()
    private var mFileLinks: MutableList<String> = mutableListOf()
    private var mMapLanguage: HashMap<String, Languages> = hashMapOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_book_detail, container, false)

        mBackgroundImage = root.findViewById(R.id.book_detail_background_image)
        mImage = root.findViewById(R.id.book_detail_book_image)

        mBookLanguage = root.findViewById(R.id.book_detail_information_language)
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
        mDeleteButton = root.findViewById(R.id.book_detail_button_delete)
        mVocabularyButton = root.findViewById(R.id.book_detail_button_vocabulary)

        mChaptersList = root.findViewById(R.id.book_detail_chapters_list)
        mFileLinkContent = root.findViewById(R.id.book_detail_files_link_detail)
        mFileLinksList = root.findViewById(R.id.book_detail_files_links_list)

        mInformationContent = root.findViewById(R.id.book_detail_information)
        mInformationImage = root.findViewById(R.id.book_detail_information_image)
        mInformationSynopsis = root.findViewById(R.id.book_detail_information_synopsis)
        mInformationAnnotation =
            root.findViewById(R.id.book_detail_information_annotation)
        mInformationPublish = root.findViewById(R.id.book_detail_information_publish)
        mInformationYear = root.findViewById(R.id.book_detail_information_year)
        mInformationIsbn = root.findViewById(R.id.book_detail_information_isbn)
        mInformationGenres = root.findViewById(R.id.book_detail_information_genres)
        mInformationFile = root.findViewById(R.id.book_detail_information_file)

        val languages = resources.getStringArray(R.array.languages)
        mMapLanguage = hashMapOf(
            languages[1] to Languages.ENGLISH,
            languages[2] to Languages.JAPANESE,
            languages[0] to Languages.PORTUGUESE
        )

        mBookLanguageAutoComplete.setAdapter(ArrayAdapter(requireContext(), R.layout.list_item, mMapLanguage.keys.toTypedArray()))
        mBookLanguageAutoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val lang = if (parent.getItemAtPosition(position).toString().isNotEmpty() &&
                        mMapLanguage.containsKey(parent.getItemAtPosition(position).toString())
                    )
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

        mFileLinksList.adapter =
            ArrayAdapter(requireContext(), R.layout.list_item_all_text, mFileLinks)
        mChaptersList.adapter =
            ArrayAdapter(requireContext(), R.layout.list_item_all_text, mChapters)

        mTitle.setOnLongClickListener {
            mViewModel.book.value?.let { bk -> FileUtil(requireContext()).copyName(bk) }
            true
        }

        mChaptersList.setOnItemClickListener { _, _, index, _ ->
            mViewModel.book.value?.let {
                if (index >= 0 && mChapters.size > index) {
                    val folder = mChapters[index]
                    val page = mViewModel.getPage(folder)
                    it.bookMark = page
                    mViewModel.save(it)
                    val intent = Intent(context, MangaReaderActivity::class.java)
                    val bundle = Bundle()
                    bundle.putSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY, mViewModel.library)
                    bundle.putString(GeneralConsts.KEYS.MANGA.NAME, it.title)
                    bundle.putInt(GeneralConsts.KEYS.MANGA.MARK, it.bookMark)
                    bundle.putSerializable(GeneralConsts.KEYS.OBJECT.MANGA, it)
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
        mViewModel.book.observe(viewLifecycleOwner) {
            if (it != null) {
                BookImageCoverController.instance.setImageCoverAsync(
                    requireContext(),
                    it,
                    arrayListOf(mBackgroundImage, mImage),
                    false
                )

                mBookLanguageAutoComplete.setText(
                    mMapLanguage.filterValues { lan -> lan == it.language }.keys.first(),
                    false
                )

                mTitle.text = it.title
                mFolder.text = it.path
                mFileType.text = it.type.toString()
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
                    mDeleted.text = getString(R.string.book_detail_manga_deleted)
                    mDeleted.visibility = View.VISIBLE
                } else {
                    mDeleted.text = ""
                    mDeleted.visibility = View.GONE
                }
            } else {
                mBackgroundImage.setImageBitmap(null)
                mImage.setImageBitmap(null)

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

        mViewModel.information.observe(viewLifecycleOwner) {
            if (it != null) {
                mInformationContent.visibility = View.VISIBLE
                mInformationSynopsis.text = it.synopsis.replace("\\n", "\n")

                mInformationImage.visibility = View.GONE
                mInformationImage.setImageBitmap(null)

                mInformationAnnotation.text = requireContext().getString(R.string.book_detail_information_annotation, it.annotation)
                mInformationPublish.text = requireContext().getString(R.string.book_detail_information_publisher, it.publisher)
                mInformationYear.text = requireContext().getString(R.string.book_detail_information_year, it.year)
                mInformationIsbn.text = requireContext().getString(R.string.book_detail_information_isbn, it.isbn)
                mInformationGenres.text = requireContext().getString(R.string.book_detail_information_genres, it.genres)
                mInformationFile.text = requireContext().getString(R.string.book_detail_information_file, it.file)
            } else {
                mInformationContent.visibility = View.GONE
                mInformationImage.visibility = View.GONE

                mInformationSynopsis.text = ""
                mInformationAnnotation.text = ""
                mInformationPublish.text = ""
                mInformationYear.text = ""
                mInformationIsbn.text = ""
                mInformationGenres.text = ""
                mInformationFile.text = ""
            }
        }
    }

    private fun deleteFile() {
        val book = mViewModel.book.value ?: return
        MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
            .setTitle(getString(R.string.manga_library_menu_delete))
            .setMessage(getString(R.string.manga_library_menu_delete_description) + "\n" + book.file.name)
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

    private fun favorite() {
        val book = mViewModel.book.value ?: return
        book.favorite = !book.favorite
        mViewModel.save(book)
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