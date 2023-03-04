package br.com.fenix.bilingualreader.view.ui.detail.manga

import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Information
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.controller.MangaImageController
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController
import br.com.fenix.bilingualreader.service.listener.InformationCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.LibraryUtil
import br.com.fenix.bilingualreader.view.adapter.detail.manga.InformationRelatedCardAdapter
import br.com.fenix.bilingualreader.view.ui.detail.DetailActivity
import br.com.fenix.bilingualreader.view.ui.reader.manga.MangaReaderActivity
import br.com.fenix.bilingualreader.view.ui.vocabulary.VocabularyActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.slf4j.LoggerFactory


class MangaDetailFragment : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(MangaDetailFragment::class.java)

    private val mViewModel: MangaDetailViewModel by viewModels()

    private lateinit var mBackgroundImage: ImageView
    private lateinit var mImage: ImageView
    private lateinit var mTitle: TextView
    private lateinit var mFolder: TextView
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
    private lateinit var mSubtitlesContent: LinearLayout
    private lateinit var mSubtitlesList: ListView

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

    private lateinit var mLocalInformationContent: LinearLayout
    private lateinit var mLocalInformationSeries: TextView
    private lateinit var mLocalInformationAuthors: TextView

    private lateinit var mLocalInformationVolumeReleasePublisherContent: LinearLayout
    private lateinit var mLocalInformationVolume: TextView
    private lateinit var mLocalInformationRelease: TextView
    private lateinit var mLocalInformationPublisher: TextView

    private lateinit var mLocalInformationComicInfo: LinearLayout

    private lateinit var mLocalInformationComicInfoTitleLanguageContent: LinearLayout
    private lateinit var mLocalInformationComicInfoTitle: TextView
    private lateinit var mLocalInformationComicInfoLanguage: TextView
    private lateinit var mLocalInformationComicInfoStoryArch: TextView
    private lateinit var mLocalInformationComicInfoGenre: TextView
    private lateinit var mLocalInformationComicInfoCharacters: TextView
    private lateinit var mLocalInformationComicInfoTeams: TextView
    private lateinit var mLocalInformationComicInfoLocations: TextView

    private lateinit var mLocalInformationComicInfoBookMarksContent: LinearLayout
    private lateinit var mLocalInformationComicInfoBookMarks: ListView

    private var mSubtitles: MutableList<String> = mutableListOf()
    private var mChapters: MutableList<String> = mutableListOf()
    private var mFileLinks: MutableList<String> = mutableListOf()
    private var mBookMarks: MutableList<String> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_manga_detail, container, false)

        mBackgroundImage = root.findViewById(R.id.manga_detail_background_image)
        mImage = root.findViewById(R.id.manga_detail_manga_image)
        mTitle = root.findViewById(R.id.manga_detail_title)
        mFolder = root.findViewById(R.id.manga_detail_folder)
        mLastAccess = root.findViewById(R.id.manga_detail_last_access)
        mDeleted = root.findViewById(R.id.manga_detail_deleted)
        mBookMark = root.findViewById(R.id.manga_detail_book_mark)
        mProgress = root.findViewById(R.id.manga_detail_progress)
        mButtonsContent = root.findViewById(R.id.manga_detail_buttons)
        mFavoriteButton = root.findViewById(R.id.manga_detail_button_favorite)
        mClearHistoryButton = root.findViewById(R.id.manga_detail_button_clear_history)
        mMakReadButton = root.findViewById(R.id.manga_detail_button_mark_read)
        mDeleteButton = root.findViewById(R.id.manga_detail_button_delete)
        mVocabularyButton = root.findViewById(R.id.manga_detail_button_vocabulary)
        mChaptersList = root.findViewById(R.id.manga_detail_chapters_list)
        mFileLinkContent = root.findViewById(R.id.manga_detail_files_link_detail)
        mFileLinksList = root.findViewById(R.id.manga_detail_files_links_list)
        mSubtitlesContent = root.findViewById(R.id.manga_detail_subtitle_content)
        mSubtitlesList = root.findViewById(R.id.manga_detail_subtitles_list)

        mLocalInformationContent = root.findViewById(R.id.manga_detail_local_information)
        mLocalInformationSeries = root.findViewById(R.id.manga_detail_local_information_series)
        mLocalInformationAuthors = root.findViewById(R.id.manga_detail_local_information_authors)

        mLocalInformationVolumeReleasePublisherContent =
            root.findViewById(R.id.manga_detail_local_information_volume_release_publisher)
        mLocalInformationVolume = root.findViewById(R.id.manga_detail_local_information_volume)
        mLocalInformationRelease = root.findViewById(R.id.manga_detail_local_information_release)
        mLocalInformationPublisher =
            root.findViewById(R.id.manga_detail_local_information_publisher)

        mLocalInformationComicInfo =
            root.findViewById(R.id.manga_detail_local_information_comic_info)

        mLocalInformationComicInfoTitleLanguageContent =
            root.findViewById(R.id.manga_detail_local_information_comic_info_title_language)
        mLocalInformationComicInfoTitle =
            root.findViewById(R.id.manga_detail_local_information_comic_info_title)
        mLocalInformationComicInfoLanguage =
            root.findViewById(R.id.manga_detail_local_information_comic_info_language)
        mLocalInformationComicInfoStoryArch =
            root.findViewById(R.id.manga_detail_local_information_comic_info_story_arch)
        mLocalInformationComicInfoGenre =
            root.findViewById(R.id.manga_detail_local_information_comic_info_genre)
        mLocalInformationComicInfoCharacters =
            root.findViewById(R.id.manga_detail_local_information_comic_info_characters)
        mLocalInformationComicInfoTeams =
            root.findViewById(R.id.manga_detail_local_information_comic_info_teams)
        mLocalInformationComicInfoLocations =
            root.findViewById(R.id.manga_detail_local_information_comic_info_locations)

        mLocalInformationComicInfoBookMarksContent =
            root.findViewById(R.id.manga_detail_local_information_comic_info_book_marks_content)
        mLocalInformationComicInfoBookMarks =
            root.findViewById(R.id.manga_detail_local_information_comic_info_book_marks)

        mWebInformationContent = root.findViewById(R.id.manga_detail_web_information)
        mWebInformationImage = root.findViewById(R.id.manga_detail_web_information_image)
        mWebInformationSynopsis = root.findViewById(R.id.manga_detail_web_information_synopsis)
        mWebInformationAlternativeTitles =
            root.findViewById(R.id.manga_detail_web_information_alternative_titles)
        mWebInformationStatus = root.findViewById(R.id.manga_detail_web_information_status)
        mWebInformationPublish = root.findViewById(R.id.manga_detail_web_information_publish)
        mWebInformationVolumes =
            root.findViewById(R.id.manga_detail_web_information_volumes_chapters)
        mWebInformationAuthors = root.findViewById(R.id.manga_detail_web_information_author)
        mWebInformationGenres = root.findViewById(R.id.manga_detail_web_information_genres)
        mWebInformationOrigin = root.findViewById(R.id.manga_detail_web_information_origin)
        mWebInfoRelatedContent = root.findViewById(R.id.manga_detail_web_information_relations)
        mWebInfoRelatedRelatedList = root.findViewById(R.id.manga_detail_relations_lists)
        mWebInfoRelatedOrigin = root.findViewById(R.id.manga_detail_relations_origin)

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

        mSubtitlesList.adapter =
            ArrayAdapter(requireContext(), R.layout.list_item_all_text, mSubtitles)
        mFileLinksList.adapter =
            ArrayAdapter(requireContext(), R.layout.list_item_all_text, mFileLinks)
        mChaptersList.adapter =
            ArrayAdapter(requireContext(), R.layout.list_item_all_text, mChapters)
        mLocalInformationComicInfoBookMarks.adapter =
            ArrayAdapter(requireContext(), R.layout.list_item_all_text, mBookMarks)

        mWebInfoRelatedRelatedList.adapter = InformationRelatedCardAdapter()
        mWebInfoRelatedRelatedList.layoutManager = LinearLayoutManager(requireContext())

        mTitle.setOnLongClickListener {
            mViewModel.manga.value?.let { mg -> FileUtil(requireContext()).copyName(mg) }
            true
        }

        mChaptersList.setOnItemClickListener { _, _, index, _ ->
            mViewModel.manga.value?.let {
                if (index >= 0 && mChapters.size > index) {
                    val folder = mChapters[index]
                    val page = mViewModel.getPage(folder)
                    it.bookMark = page
                    mViewModel.save(it)
                    openManga(it)
                }
            }
        }

        mLocalInformationComicInfoBookMarks.setOnItemClickListener { _, _, index, _ ->
            mViewModel.localInformation.value?.let {
                if (index >= 0 && mBookMarks.size > index && mViewModel.manga.value != null) {
                    it.bookMarks.first { b -> b.second == mBookMarks[index] }.let { b ->
                        val manga = mViewModel.manga.value
                        manga!!.bookMark = b.first + 1
                        mViewModel.save(manga)
                        openManga(manga)
                    }
                }
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
                LibraryUtil.getDefault(requireContext(), Type.MANGA)

            if (it.containsKey(GeneralConsts.KEYS.OBJECT.MANGA))
                mViewModel.setManga(it[GeneralConsts.KEYS.OBJECT.MANGA] as Manga)
        }

        return root
    }

    override fun onResume() {
        super.onResume()

        mViewModel.getInformation()
    }

    private fun observer() {
        mViewModel.manga.observe(viewLifecycleOwner) {
            if (it != null) {
                MangaImageCoverController.instance.setImageCoverAsync(
                    requireContext(),
                    it,
                    arrayListOf(mBackgroundImage, mImage),
                    false
                )
                mTitle.text = it.name
                mFolder.text = it.path
                val folder = mViewModel.getChapterFolder(it.bookMark)
                mBookMark.text =
                    "${it.bookMark} / ${it.pages}" + if (folder.isNotEmpty()) " - $folder" else ""
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

                if (it.author.isNotEmpty())
                    mLocalInformationAuthors.text = HtmlCompat.fromHtml(
                        requireContext().getString(
                            R.string.manga_detail_local_information_volume,
                            it.author
                        ), HtmlCompat.FROM_HTML_MODE_COMPACT
                    )
                else
                    mLocalInformationAuthors.visibility = View.GONE

                if (it.series.isNotEmpty())
                    mLocalInformationSeries.text = HtmlCompat.fromHtml(
                        requireContext().getString(
                            R.string.manga_detail_local_information_volume,
                            it.author
                        ), HtmlCompat.FROM_HTML_MODE_COMPACT
                    )
                else
                    mLocalInformationSeries.visibility = View.GONE

                if (it.volume.isNotEmpty())
                    mLocalInformationVolume.text = HtmlCompat.fromHtml(
                        requireContext().getString(
                            R.string.manga_detail_local_information_volume,
                            it.volume
                        ), HtmlCompat.FROM_HTML_MODE_COMPACT
                    )
                else
                    mLocalInformationVolume.visibility = View.GONE

                if (it.publisher.isNotEmpty())
                    mLocalInformationPublisher.text = HtmlCompat.fromHtml(
                        requireContext().getString(
                            R.string.manga_detail_local_information_publisher,
                            it.publisher
                        ), HtmlCompat.FROM_HTML_MODE_COMPACT
                    )
                else
                    mLocalInformationPublisher.visibility = View.GONE

                if (it.release != null)
                    mLocalInformationRelease.text = HtmlCompat.fromHtml(
                        requireContext().getString(
                            R.string.manga_detail_local_information_release,
                            GeneralConsts.formatterDate(requireContext(), it.release!!)
                        ), HtmlCompat.FROM_HTML_MODE_COMPACT
                    )
                else
                    mLocalInformationRelease.visibility = View.GONE

                mLocalInformationContent.visibility =
                    if (it.author.isNotEmpty() || it.series.isNotEmpty() || it.volume.isNotEmpty() || it.publisher.isNotEmpty() || it.release != null)
                        View.VISIBLE
                    else
                        View.GONE

                if (it.excluded) {
                    mDeleted.text = getString(R.string.manga_detail_manga_deleted)
                    mDeleted.visibility = View.VISIBLE
                } else {
                    mDeleted.text = ""
                    mDeleted.visibility = View.GONE
                }

            } else {
                mBackgroundImage.setImageBitmap(null)
                mImage.setImageBitmap(null)

                mTitle.text = ""
                mFolder.text = ""
                mLastAccess.text = ""
                mDeleted.text = ""
                mBookMark.text = ""
                mProgress.max = 1
                mProgress.setProgress(0, false)
                mFavoriteButton.setIconResource(R.drawable.ico_favorite_unmark)

                mLocalInformationRelease.text = ""
                mLocalInformationPublisher.text = ""
                mLocalInformationVolume.text = ""
                mLocalInformationSeries.text = ""
                mLocalInformationAuthors.text = ""
            }
        }

        mViewModel.listChapters.observe(viewLifecycleOwner) {
            mChapters.clear()
            mChapters.addAll(it)
            (mChaptersList.adapter as ArrayAdapter<*>).notifyDataSetChanged()

            val manga = mViewModel.manga.value
            if (manga != null)
                mBookMark.text =
                    "${manga.bookMark} / ${manga.pages} - " + mViewModel.getChapterFolder(manga.bookMark)
        }

        mViewModel.listSubtitles.observe(viewLifecycleOwner) {
            mSubtitles.clear()
            mSubtitles.addAll(it)
            (mSubtitlesList.adapter as ArrayAdapter<*>).notifyDataSetChanged()
            mSubtitlesContent.visibility = if (it.isNotEmpty())
                View.VISIBLE
            else
                View.GONE
        }

        mViewModel.listFileLinks.observe(viewLifecycleOwner) { fileLinks ->
            val list = fileLinks?.map { it.path }?.toMutableList() ?: mutableListOf()
            mFileLinks.clear()
            mFileLinks.addAll(list)
            (mFileLinksList.adapter as ArrayAdapter<*>).notifyDataSetChanged()
            mFileLinkContent.visibility = if (mFileLinks.isNotEmpty())
                View.VISIBLE
            else
                View.GONE
        }

        mViewModel.localInformation.observe(viewLifecycleOwner) {
            mBookMarks.clear()
            if (it != null) {
                mLocalInformationVolume.text =
                    HtmlCompat.fromHtml(it.volumes, HtmlCompat.FROM_HTML_MODE_COMPACT)
                mLocalInformationPublisher.text =
                    HtmlCompat.fromHtml(it.publisher, HtmlCompat.FROM_HTML_MODE_COMPACT)
                mLocalInformationRelease.text =
                    HtmlCompat.fromHtml(it.release, HtmlCompat.FROM_HTML_MODE_COMPACT)

                mLocalInformationVolumeReleasePublisherContent.visibility =
                    if (it.volumes.isNotEmpty() || it.publisher.isNotEmpty() || it.release.isNotEmpty())
                        View.VISIBLE
                    else
                        View.GONE

                mLocalInformationComicInfoTitle.text =
                    HtmlCompat.fromHtml(it.title, HtmlCompat.FROM_HTML_MODE_COMPACT)
                mLocalInformationComicInfoTitle.visibility =
                    if (it.title.isNotEmpty()) View.VISIBLE else View.GONE
                mLocalInformationComicInfoLanguage.text =
                    HtmlCompat.fromHtml(it.languageDescription, HtmlCompat.FROM_HTML_MODE_COMPACT)
                mLocalInformationComicInfoLanguage.visibility =
                    if (it.languageDescription.isNotEmpty()) View.VISIBLE else View.GONE

                mLocalInformationComicInfoTitleLanguageContent.visibility =
                    if (mLocalInformationComicInfoTitle.visibility == View.VISIBLE ||
                        mLocalInformationComicInfoLanguage.visibility == View.VISIBLE
                    )
                        View.VISIBLE
                    else
                        View.GONE

                mLocalInformationComicInfoStoryArch.text =
                    HtmlCompat.fromHtml(it.storyArch, HtmlCompat.FROM_HTML_MODE_COMPACT)
                mLocalInformationComicInfoStoryArch.visibility =
                    if (it.storyArch.isNotEmpty()) View.VISIBLE else View.GONE
                mLocalInformationComicInfoGenre.text =
                    HtmlCompat.fromHtml(it.genres, HtmlCompat.FROM_HTML_MODE_COMPACT)
                mLocalInformationComicInfoGenre.visibility =
                    if (it.genres.isNotEmpty()) View.VISIBLE else View.GONE
                mLocalInformationComicInfoCharacters.text =
                    HtmlCompat.fromHtml(it.characters, HtmlCompat.FROM_HTML_MODE_COMPACT)
                mLocalInformationComicInfoCharacters.visibility =
                    if (it.characters.isNotEmpty()) View.VISIBLE else View.GONE
                mLocalInformationComicInfoTeams.text =
                    HtmlCompat.fromHtml(it.teams, HtmlCompat.FROM_HTML_MODE_COMPACT)
                mLocalInformationComicInfoTeams.visibility =
                    if (it.teams.isNotEmpty()) View.VISIBLE else View.GONE
                mLocalInformationComicInfoLocations.text =
                    HtmlCompat.fromHtml(it.locations, HtmlCompat.FROM_HTML_MODE_COMPACT)
                mLocalInformationComicInfoLocations.visibility =
                    if (it.locations.isNotEmpty()) View.VISIBLE else View.GONE

                if (it.bookMarks.isNotEmpty()) {
                    mLocalInformationComicInfoBookMarksContent.visibility = View.VISIBLE
                    mBookMarks.addAll(it.bookMarks.map { b -> b.second })
                } else
                    mLocalInformationComicInfoBookMarksContent.visibility = View.GONE

                mLocalInformationComicInfo.visibility =
                    if (mLocalInformationComicInfoStoryArch.visibility == View.VISIBLE ||
                        mLocalInformationComicInfoGenre.visibility == View.VISIBLE || mLocalInformationComicInfoCharacters.visibility == View.VISIBLE ||
                        mLocalInformationComicInfoTeams.visibility == View.VISIBLE || mLocalInformationComicInfoLocations.visibility == View.VISIBLE ||
                        mLocalInformationComicInfoBookMarksContent.visibility == View.VISIBLE
                    )
                        View.VISIBLE
                    else
                        View.GONE
            } else {
                mLocalInformationVolume.text = ""
                mLocalInformationRelease.text = ""
                mLocalInformationPublisher.text = ""

                mLocalInformationComicInfoTitle.text = ""
                mLocalInformationComicInfoLanguage.text = ""

                mLocalInformationComicInfoGenre.text = ""
                mLocalInformationComicInfoCharacters.text = ""
                mLocalInformationComicInfoTeams.text = ""
                mLocalInformationComicInfoLocations.text = ""

                mLocalInformationVolumeReleasePublisherContent.visibility = View.GONE
                mLocalInformationComicInfoTitleLanguageContent.visibility = View.GONE
                mLocalInformationComicInfo.visibility = View.GONE
                mLocalInformationComicInfoBookMarksContent.visibility = View.GONE
            }
        }

        mViewModel.webInformation.observe(viewLifecycleOwner) {
            if (it != null) {
                mWebInformationContent.visibility = View.VISIBLE
                mWebInformationSynopsis.text = it.synopsis.replace("\\n", "\n")

                mWebInformationImage.visibility = View.GONE
                mWebInformationImage.setImageBitmap(null)

                if (it.imageLink != null)
                    MangaImageController.instance.setImageAsync(
                        requireContext(),
                        it.imageLink!!,
                        mWebInformationImage
                    )

                mWebInformationAlternativeTitles.text =
                    HtmlCompat.fromHtml(it.alternativeTitles, HtmlCompat.FROM_HTML_MODE_COMPACT)
                mWebInformationStatus.text =
                    HtmlCompat.fromHtml(it.status, HtmlCompat.FROM_HTML_MODE_COMPACT)
                mWebInformationPublish.text =
                    HtmlCompat.fromHtml(it.release, HtmlCompat.FROM_HTML_MODE_COMPACT)
                mWebInformationVolumes.text = HtmlCompat.fromHtml(
                    it.volumes + ", " + it.chapters,
                    HtmlCompat.FROM_HTML_MODE_COMPACT
                )
                mWebInformationAuthors.text =
                    HtmlCompat.fromHtml(it.authors, HtmlCompat.FROM_HTML_MODE_COMPACT)
                mWebInformationGenres.text =
                    HtmlCompat.fromHtml(it.genres, HtmlCompat.FROM_HTML_MODE_COMPACT)
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
            mWebInfoRelatedContent.visibility =
                if (it != null && it.isNotEmpty()) View.VISIBLE else View.GONE
            updateRelatedList(it)
        }

    }

    private fun deleteFile() {
        val manga = mViewModel.manga.value ?: return
        MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
            .setTitle(getString(R.string.manga_library_menu_delete))
            .setMessage(getString(R.string.manga_library_menu_delete_description) + "\n" + manga.file.name)
            .setPositiveButton(
                R.string.action_positive
            ) { _, _ ->
                mViewModel.delete()
                if (manga.file.exists()) {
                    val isDeleted = manga.file.delete()
                    mLOGGER.info("File deleted ${manga.name}: $isDeleted")
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
        val manga = mViewModel.manga.value ?: return
        manga.favorite = !manga.favorite
        mViewModel.save(manga)
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
        mViewModel.manga.value?.let {
            val intent = Intent(requireContext(), VocabularyActivity::class.java)
            val bundle = Bundle()
            bundle.putSerializable(GeneralConsts.KEYS.OBJECT.MANGA, it)
            bundle.putSerializable(GeneralConsts.KEYS.VOCABULARY.TYPE, Type.MANGA)
            intent.putExtras(bundle)
            requireActivity().overridePendingTransition(
                R.anim.fade_in_fragment_add_enter,
                R.anim.fade_out_fragment_remove_exit
            )
            startActivity(intent)
        }
    }

    private fun openManga(manga: Manga) {
        val intent = Intent(context, MangaReaderActivity::class.java)
        val bundle = Bundle()
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.LIBRARY, mViewModel.library)
        bundle.putString(GeneralConsts.KEYS.MANGA.NAME, manga.title)
        bundle.putInt(GeneralConsts.KEYS.MANGA.MARK, manga.bookMark)
        bundle.putSerializable(GeneralConsts.KEYS.OBJECT.MANGA, manga)
        intent.putExtras(bundle)
        context?.startActivity(intent)
    }
}