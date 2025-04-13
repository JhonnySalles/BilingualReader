package br.com.fenix.bilingualreader.view.ui.help

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import br.com.fenix.bilingualreader.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class HelpFragment : Fragment() {

    private lateinit var mScrollView: ScrollView
    private lateinit var mScrollUp: FloatingActionButton
    private lateinit var mLibraryContent: TextView
    private lateinit var mLibraryTitle: TextView
    private lateinit var mReaderContent: TextView
    private lateinit var mReaderTitle: TextView
    private lateinit var mReaderMangaContent: TextView
    private lateinit var mReaderMangaTitle: TextView
    private lateinit var mReaderBookContent: TextView
    private lateinit var mReaderBookTitle: TextView
    private lateinit var mSubtitleContent: TextView
    private lateinit var mSubtitleTitle: TextView
    private lateinit var mSubtitleImportContent: TextView
    private lateinit var mSubtitleImportTitle: TextView
    private lateinit var mSubtitleDataContent: TextView
    private lateinit var mSubtitleDataTitle: TextView
    private lateinit var mSubtitleVocabularyContent: TextView
    private lateinit var mSubtitleVocabularyTitle: TextView
    private lateinit var mVocabularyContent: TextView
    private lateinit var mVocabularyTitle: TextView
    private lateinit var mKanjiContent: TextView
    private lateinit var mKanjiTitle: TextView
    private lateinit var mFloatingPopupContent: TextView
    private lateinit var mFloatingPopupTitle: TextView
    private lateinit var mLanguageSupportContent: TextView
    private lateinit var mLanguageSupportTitle: TextView
    private lateinit var mStatisticsContent: TextView
    private lateinit var mStatisticsTitle: TextView
    private lateinit var mThemesContent: TextView
    private lateinit var mThemesTitle: TextView
    private lateinit var mShareContent: TextView
    private lateinit var mShareTitle: TextView

    private val mDismissUpButton = Runnable { mScrollUp.hide() }
    private val mHandler = Handler(Looper.getMainLooper())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_help, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mScrollView = view.findViewById(R.id.help_scroll_view)
        mScrollUp = view.findViewById(R.id.help_scroll_up)

        mScrollUp.setOnClickListener {
            (mScrollUp.drawable as AnimatedVectorDrawable).start()
            mScrollView.smoothScrollTo(0, 0)
        }
        mScrollUp.visibility = View.GONE
        mScrollView.setOnScrollChangeListener { _, _, yNew, _, yOld ->
            if ((yNew - yOld) < -150) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (mHandler.hasCallbacks(mDismissUpButton))
                        mHandler.removeCallbacks(mDismissUpButton)
                } else
                    mHandler.removeCallbacks(mDismissUpButton)

                mHandler.postDelayed(mDismissUpButton, 3000)

                mScrollUp.show()
            } else if ((yNew - yOld) > 1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (mHandler.hasCallbacks(mDismissUpButton))
                        mHandler.removeCallbacks(mDismissUpButton)
                } else
                    mHandler.removeCallbacks(mDismissUpButton)

                
                mScrollUp.hide()
            }
        }

        mLibraryContent = view.findViewById(R.id.help_library_content)
        mLibraryTitle = view.findViewById(R.id.help_library_title)
        mReaderContent = view.findViewById(R.id.help_reader_content)
        mReaderTitle = view.findViewById(R.id.help_reader_title)
        mReaderMangaContent = view.findViewById(R.id.help_reader_manga_content)
        mReaderMangaTitle = view.findViewById(R.id.help_reader_manga_title)
        mReaderBookContent = view.findViewById(R.id.help_reader_book_content)
        mReaderBookTitle = view.findViewById(R.id.help_reader_book_title)
        mSubtitleContent = view.findViewById(R.id.help_subtitle_content)
        mSubtitleTitle = view.findViewById(R.id.help_subtitle_title)
        mSubtitleImportContent = view.findViewById(R.id.help_subtitle_import_content)
        mSubtitleImportTitle = view.findViewById(R.id.help_subtitle_import_title)
        mSubtitleDataContent = view.findViewById(R.id.help_subtitle_data_content)
        mSubtitleDataTitle = view.findViewById(R.id.help_subtitle_data_title)
        mSubtitleVocabularyContent = view.findViewById(R.id.help_subtitle_vocabulary_content)
        mSubtitleVocabularyTitle = view.findViewById(R.id.help_subtitle_vocabulary_title)
        mVocabularyContent = view.findViewById(R.id.help_vocabulary_content)
        mVocabularyTitle = view.findViewById(R.id.help_vocabulary_title)
        mKanjiContent = view.findViewById(R.id.help_kanjis_content)
        mKanjiTitle = view.findViewById(R.id.help_kanjis_title)
        mFloatingPopupContent = view.findViewById(R.id.help_floating_popup_content)
        mFloatingPopupTitle = view.findViewById(R.id.help_floating_popup_title)
        mLanguageSupportContent = view.findViewById(R.id.help_language_support_content)
        mLanguageSupportTitle = view.findViewById(R.id.help_language_support_title)
        mStatisticsContent = view.findViewById(R.id.help_statistics_content)
        mStatisticsTitle = view.findViewById(R.id.help_statistics_title)
        mThemesContent = view.findViewById(R.id.help_themes_content)
        mThemesTitle = view.findViewById(R.id.help_themes_title)
        mShareContent = view.findViewById(R.id.help_share_content)
        mShareTitle = view.findViewById(R.id.help_share_title)

        mLibraryContent.setOnClickListener { mScrollView.smoothScrollTo(0, mLibraryTitle.top) }
        mReaderContent.setOnClickListener { mScrollView.smoothScrollTo(0, mReaderTitle.top) }
        mReaderMangaContent.setOnClickListener { mScrollView.smoothScrollTo(0, mReaderMangaTitle.top) }
        mReaderBookContent.setOnClickListener { mScrollView.smoothScrollTo(0, mReaderBookTitle.top) }
        mSubtitleContent.setOnClickListener { mScrollView.smoothScrollTo(0, mSubtitleTitle.top) }
        mSubtitleImportContent.setOnClickListener { mScrollView.smoothScrollTo(0, mSubtitleImportTitle.top) }
        mSubtitleDataContent.setOnClickListener { mScrollView.smoothScrollTo(0, mSubtitleDataTitle.top) }
        mSubtitleVocabularyContent.setOnClickListener { mScrollView.smoothScrollTo(0, mSubtitleVocabularyTitle.top) }
        mVocabularyContent.setOnClickListener { mScrollView.smoothScrollTo(0, mVocabularyTitle.top) }
        mKanjiContent.setOnClickListener { mScrollView.smoothScrollTo(0, mKanjiTitle.top) }
        mFloatingPopupContent.setOnClickListener { mScrollView.smoothScrollTo(0, mFloatingPopupTitle.top) }
        mLanguageSupportContent.setOnClickListener { mScrollView.smoothScrollTo(0, mLanguageSupportTitle.top) }
        mStatisticsContent.setOnClickListener { mScrollView.smoothScrollTo(0, mStatisticsTitle.top) }
        mThemesContent.setOnClickListener { mScrollView.smoothScrollTo(0, mThemesTitle.top) }
        mShareContent.setOnClickListener { mScrollView.smoothScrollTo(0, mShareTitle.top) }
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mHandler.hasCallbacks(mDismissUpButton))
                mHandler.removeCallbacks(mDismissUpButton)
        } else
            mHandler.removeCallbacks(mDismissUpButton)

        super.onDestroy()
    }
}