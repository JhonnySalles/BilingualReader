package br.com.fenix.bilingualreader.view.ui.configuration

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import br.com.fenix.bilingualreader.MainActivity
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.FontType
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.model.enums.PageMode
import br.com.fenix.bilingualreader.model.enums.ReaderMode
import br.com.fenix.bilingualreader.model.enums.ScrollingType
import br.com.fenix.bilingualreader.model.enums.ShareMarkCloud
import br.com.fenix.bilingualreader.model.enums.TextSpeech
import br.com.fenix.bilingualreader.model.enums.ThemeMode
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.listener.FontsListener
import br.com.fenix.bilingualreader.service.listener.ThemesListener
import br.com.fenix.bilingualreader.service.repository.DataBase
import br.com.fenix.bilingualreader.service.repository.HistoryRepository
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.service.sharemark.ShareMarkBase
import br.com.fenix.bilingualreader.service.update.UpdateApp
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.BackupError
import br.com.fenix.bilingualreader.util.helpers.ErrorRestoreDatabase
import br.com.fenix.bilingualreader.util.helpers.InvalidDatabase
import br.com.fenix.bilingualreader.util.helpers.LibraryUtil
import br.com.fenix.bilingualreader.util.helpers.MsgUtil
import br.com.fenix.bilingualreader.util.helpers.RestoredNewDatabase
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.util.secrets.Secrets
import br.com.fenix.bilingualreader.view.adapter.fonts.FontsCardAdapter
import br.com.fenix.bilingualreader.view.adapter.themes.ThemesCardAdapter
import br.com.fenix.bilingualreader.view.ui.library.book.BookLibraryViewModel
import br.com.fenix.bilingualreader.view.ui.library.manga.MangaLibraryViewModel
import br.com.fenix.bilingualreader.view.ui.menu.ConfigLibrariesViewModel
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.google.api.services.drive.DriveScopes
import org.lucasr.twowayview.TwoWayView
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale


class ConfigFragment : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(ConfigFragment::class.java)

    private val mViewModel: ConfigLibrariesViewModel by viewModels()

    // -------------------------------------------------------- System --------------------------------------------------------
    private lateinit var mConfigSystemThemeMode: TextInputLayout
    private lateinit var mConfigSystemThemeModeAutoComplete: MaterialAutoCompleteTextView
    private lateinit var mConfigSystemThemes: TwoWayView

    private lateinit var mConfigSystemFormatDate: TextInputLayout
    private lateinit var mConfigSystemFormatDateAutoComplete: MaterialAutoCompleteTextView

    private lateinit var mConfigSystemShareMarkEnabled: SwitchMaterial
    private lateinit var mConfigSystemShareMarkType: TextInputLayout
    private lateinit var mConfigSystemShareMarkTypeAutoComplete: MaterialAutoCompleteTextView
    private lateinit var mConfigSystemShareMarkAccount: MaterialButton
    private lateinit var mConfigSystemShareMarkSignIn: SignInButton
    private lateinit var mConfigSystemShareMarkMangaLastSync: MaterialButton
    private lateinit var mConfigSystemShareMarkBookLastSync: MaterialButton

    private lateinit var mConfigSystemBackup: MaterialButton
    private lateinit var mConfigSystemRestore: MaterialButton
    private lateinit var mConfigSystemLastBackup: TextView

    private lateinit var mConfigUpdateApp: MaterialButton

    private lateinit var mConfigCoversDelete: MaterialButton

    private lateinit var mConfigStatisticsDelete: MaterialButton

    private var mConfigSystemThemeModeSelect: ThemeMode = ThemeMode.SYSTEM
    private var mConfigSystemThemeSelect: Themes = Themes.ORIGINAL
    private var mConfigSystemDateSelect: String = GeneralConsts.CONFIG.DATA_FORMAT[0]
    private var mConfigSystemDateSmall: String = GeneralConsts.CONFIG.DATA_FORMAT_SMALL[0]
    private val mConfigSystemDatePattern = GeneralConsts.CONFIG.DATA_FORMAT
    private val mConfigSystemDateSmallPattern = GeneralConsts.CONFIG.DATA_FORMAT_SMALL

    private lateinit var mConfigSystemShareMarkCloudMap: HashMap<String, ShareMarkCloud>
    private var mConfigSystemShareMarkCloudSelect = ShareMarkCloud.GOOGLE_DRIVE

    // --------------------------------------------------------- Manga / Comic ---------------------------------------------------------
    private lateinit var mMangaLibraryPath: TextInputLayout
    private lateinit var mMangaLibraryPathAutoComplete: MaterialAutoCompleteTextView
    private lateinit var mMangaLibraryOrder: TextInputLayout
    private lateinit var mMangaLibraryOrderAutoComplete: MaterialAutoCompleteTextView
    private lateinit var mMangaLibrariesButton: MaterialButton

    private lateinit var mMangaDefaultSubtitleLanguage: TextInputLayout
    private lateinit var mMangaDefaultSubtitleLanguageAutoComplete: MaterialAutoCompleteTextView
    private lateinit var mMangaDefaultSubtitleTranslate: TextInputLayout
    private lateinit var mMangaSubtitleTranslateAutoComplete: MaterialAutoCompleteTextView

    private lateinit var mMangaReaderComicMode: TextInputLayout
    private lateinit var mMangaReaderComicModeAutoComplete: MaterialAutoCompleteTextView
    private lateinit var mMangaReaderPageMode: TextInputLayout
    private lateinit var mMangaPageModeAutoComplete: MaterialAutoCompleteTextView
    private lateinit var mMangaReaderScrollingMode: TextInputLayout
    private lateinit var mMangaScrollingModeAutoComplete: MaterialAutoCompleteTextView
    private lateinit var mMangaShowClockAndBattery: SwitchMaterial
    private lateinit var mMangaUseMagnifierType: SwitchMaterial
    private lateinit var mMangaKeepZoomBetweenPages: SwitchMaterial

    private lateinit var mMangaUseDualPageCalculate: SwitchMaterial
    private lateinit var mMangaUsePathNameForLinked: SwitchMaterial

    private var mMangaDefaultSubtitleLanguageSelect: Languages = Languages.JAPANESE
    private var mMangaDefaultSubtitleTranslateSelect: Languages = Languages.PORTUGUESE

    private lateinit var mMangaMapOrder: HashMap<String, Order>
    private lateinit var mMangaMapPageMode: HashMap<String, PageMode>
    private lateinit var mMangaMapReaderMode: HashMap<String, ReaderMode>
    private lateinit var mMangaMapLanguage: HashMap<String, Languages>
    private lateinit var mMangaMapThemeMode: HashMap<String, ThemeMode>
    private lateinit var mMangaMapThemes: HashMap<String, Themes>
    private lateinit var mMangaMapScrollingMode: HashMap<String, ScrollingType>

    private var mMangaPageModeSelectType: PageMode = PageMode.Comics
    private var mMangaReaderModeSelectType: ReaderMode = ReaderMode.FIT_WIDTH
    private var mMangaScrollingModeSelectType: ScrollingType = ScrollingType.Horizontal
    private var mMangaOrderSelect: Order = Order.Name

    // --------------------------------------------------------- Book ---------------------------------------------------------
    private lateinit var mBookLibrariesButton: MaterialButton
    private lateinit var mBookLibraryPath: TextInputLayout
    private lateinit var mBookLibraryPathAutoComplete: MaterialAutoCompleteTextView
    private lateinit var mBookLibraryOrder: TextInputLayout
    private lateinit var mBookLibraryOrderAutoComplete: MaterialAutoCompleteTextView

    private lateinit var mBookReadingTTS: TextInputLayout
    private lateinit var mBookReadingTTSAutoCompleteNormal: MaterialAutoCompleteTextView
    private lateinit var mBookReadingTTSAutoCompleteJapanese: MaterialAutoCompleteTextView
    private lateinit var mBookReadingSpeed: Slider

    private lateinit var mBookFontSize: Slider
    private lateinit var mBookFontTypeNormal: TwoWayView
    private lateinit var mBookFontTypeJapanese: TwoWayView
    private lateinit var mBookFontJapaneseStyle: SwitchMaterial

    private lateinit var mBookReaderScrollingMode: TextInputLayout
    private lateinit var mBookScrollingModeAutoComplete: MaterialAutoCompleteTextView
    private lateinit var mBookReaderProcessJapaneseText: SwitchMaterial
    private lateinit var mBookReaderTextWithFurigana: SwitchMaterial
    private lateinit var mBookReaderProcessVocabulary: SwitchMaterial

    private var mBookOrderSelect: Order = Order.Name
    private var mBookScrollingModeSelect: ScrollingType = ScrollingType.Pagination
    private var mBookReadingTTSSelectNormal: TextSpeech = TextSpeech.getDefault(false)
    private var mBookReadingTTSSelectJapanese: TextSpeech = TextSpeech.getDefault(true)

    private lateinit var mBookMapOrder: HashMap<String, Order>
    private lateinit var mBookMapScrollingMode: HashMap<String, ScrollingType>
    private lateinit var mBookMapReadingTTS: Map<String, TextSpeech>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mMangaLibraryPath = view.findViewById(R.id.config_manga_library_path)
        mMangaLibraryPathAutoComplete = view.findViewById(R.id.config_manga_menu_autocomplete_library_path)
        mMangaLibraryOrder = view.findViewById(R.id.config_manga_library_order)
        mMangaLibraryOrderAutoComplete = view.findViewById(R.id.config_manga_menu_autocomplete_library_order)
        mMangaLibrariesButton = view.findViewById(R.id.config_manga_libraries)

        mBookLibrariesButton = view.findViewById(R.id.config_book_libraries)
        mBookLibraryPath = view.findViewById(R.id.config_book_library_path)
        mBookLibraryPathAutoComplete = view.findViewById(R.id.config_book_menu_autocomplete_library_path)
        mBookLibraryOrder = view.findViewById(R.id.config_book_library_order)
        mBookLibraryOrderAutoComplete = view.findViewById(R.id.config_book_menu_autocomplete_library_order)

        mBookReadingTTS = view.findViewById(R.id.config_book_tts_voice_normal)
        mBookReadingTTSAutoCompleteNormal = view.findViewById(R.id.config_book_menu_autocomplete_tts_voice_normal)
        mBookReadingTTSAutoCompleteJapanese = view.findViewById(R.id.config_book_menu_autocomplete_tts_voice_japanese)
        mBookReadingSpeed = view.findViewById(R.id.config_book_tts_speed)

        mBookFontTypeNormal = view.findViewById(R.id.config_book_list_fonts_normal)
        mBookFontTypeJapanese = view.findViewById(R.id.config_book_list_fonts_japanese)
        mBookFontJapaneseStyle = view.findViewById(R.id.config_book_font_japanese_style)
        mBookFontSize = view.findViewById(R.id.config_book_font_size)

        mBookReaderScrollingMode = view.findViewById(R.id.config_book_reader_scrolling_mode)
        mBookScrollingModeAutoComplete = view.findViewById(R.id.config_book_menu_autocomplete_scrolling_mode)
        mBookReaderProcessJapaneseText = view.findViewById(R.id.config_book_reader_process_japanese_text)
        mBookReaderTextWithFurigana = view.findViewById(R.id.config_book_reader_text_with_furigana)
        mBookReaderProcessVocabulary = view.findViewById(R.id.config_book_reader_process_vocabulary)

        mConfigSystemThemeMode = view.findViewById(R.id.config_system_theme_mode)
        mConfigSystemThemeModeAutoComplete = view.findViewById(R.id.config_system_menu_autocomplete_theme_mode)
        mConfigSystemThemes = view.findViewById(R.id.config_system_list_themes)

        mMangaDefaultSubtitleLanguage = view.findViewById(R.id.config_manga_default_subtitle_language)
        mMangaDefaultSubtitleLanguageAutoComplete = view.findViewById(R.id.config_manga_menu_autocomplete_default_subtitle_language)
        mMangaDefaultSubtitleTranslate = view.findViewById(R.id.config_manga_default_subtitle_translate)
        mMangaSubtitleTranslateAutoComplete = view.findViewById(R.id.config_manga_menu_autocomplete_default_subtitle_translate)

        mMangaReaderComicMode = view.findViewById(R.id.config_manga_reader_comic_mode)
        mMangaReaderComicModeAutoComplete = view.findViewById(R.id.config_manga_menu_autocomplete_reader_comic_mode)
        mMangaReaderPageMode = view.findViewById(R.id.config_manga_reader_page_mode)
        mMangaPageModeAutoComplete = view.findViewById(R.id.config_manga_menu_autocomplete_page_mode)
        mMangaReaderScrollingMode = view.findViewById(R.id.config_manga_reader_scrolling_mode)
        mMangaScrollingModeAutoComplete  = view.findViewById(R.id.config_manga_menu_autocomplete_scrolling_mode)

        mMangaShowClockAndBattery = view.findViewById(R.id.config_manga_switch_show_clock_and_battery)
        mMangaUseMagnifierType = view.findViewById(R.id.config_manga_switch_use_magnifier_type)
        mMangaKeepZoomBetweenPages = view.findViewById(R.id.config_manga_switch_keep_zoom_between_pages)

        mConfigSystemFormatDate = view.findViewById(R.id.config_system_format_date)
        mConfigSystemFormatDateAutoComplete = view.findViewById(R.id.config_system_menu_autocomplete_format_date)
        mConfigSystemShareMarkEnabled = view.findViewById(R.id.config_system_share_mark_enabled)
        mConfigSystemShareMarkType = view.findViewById(R.id.config_system_share_mark_cloud)
        mConfigSystemShareMarkTypeAutoComplete = view.findViewById(R.id.config_system_menu_autocomplete_share_mark_type)
        mConfigSystemShareMarkAccount = view.findViewById(R.id.config_system_share_mark_signed_account)
        mConfigSystemShareMarkSignIn = view.findViewById(R.id.config_system_share_mark_sign_in_button)
        mConfigSystemShareMarkMangaLastSync = view.findViewById(R.id.config_system_share_mark_manga_last_sync)
        mConfigSystemShareMarkBookLastSync = view.findViewById(R.id.config_system_share_mark_book_last_sync)

        mMangaUseDualPageCalculate = view.findViewById(R.id.config_manga_switch_use_dual_page_calculate)
        mMangaUsePathNameForLinked = view.findViewById(R.id.config_manga_switch_use_path_name_for_linked)

        mConfigSystemBackup = view.findViewById(R.id.config_system_backup)
        mConfigSystemRestore = view.findViewById(R.id.config_system_restore)
        mConfigSystemLastBackup = view.findViewById(R.id.config_system_last_backup)

        mConfigUpdateApp = view.findViewById(R.id.config_update_app)
        mConfigCoversDelete = view.findViewById(R.id.config_covers_delete)
        mConfigStatisticsDelete = view.findViewById(R.id.config_statistics_delete)


        mMangaLibraryPathAutoComplete.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            startActivityForResult(intent, GeneralConsts.REQUEST.OPEN_MANGA_FOLDER)
        }

        mBookLibraryPathAutoComplete.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            startActivityForResult(intent, GeneralConsts.REQUEST.OPEN_BOOK_FOLDER)
        }

        mMangaLibrariesButton.setOnClickListener { openLibraries(Type.MANGA) }
        mBookLibrariesButton.setOnClickListener { openLibraries(Type.BOOK) }

        mMangaMapLanguage = Util.getLanguages(requireContext())

        mMangaMapOrder = hashMapOf(
            getString(R.string.option_order_name) to Order.Name,
            getString(R.string.option_order_date) to Order.Date,
            getString(R.string.option_order_access) to Order.LastAccess,
            getString(R.string.option_order_favorite) to Order.Favorite,
            getString(R.string.option_order_author) to Order.Author,
            getString(R.string.option_order_genre) to Order.Genre,
            getString(R.string.option_order_series) to Order.Series
        )

        mBookMapOrder = hashMapOf(
            getString(R.string.option_order_name) to Order.Name,
            getString(R.string.option_order_date) to Order.Date,
            getString(R.string.option_order_access) to Order.LastAccess,
            getString(R.string.option_order_favorite) to Order.Favorite,
            getString(R.string.option_order_author) to Order.Author,
            getString(R.string.option_order_genre) to Order.Genre,
            getString(R.string.option_order_series) to Order.Series
        )

        mMangaMapPageMode = hashMapOf(
            getString(R.string.menu_manga_reading_mode_comic) to PageMode.Comics,
            getString(R.string.menu_manga_reading_mode_manga) to PageMode.Manga
        )

        mMangaMapReaderMode = hashMapOf(
            getString(R.string.menu_manga_view_mode_aspect_fill) to ReaderMode.ASPECT_FILL,
            getString(R.string.menu_manga_view_mode_aspect_fit) to ReaderMode.ASPECT_FIT,
            getString(R.string.menu_manga_view_mode_fit_width) to ReaderMode.FIT_WIDTH
        )

        mMangaMapScrollingMode = hashMapOf(
            getString(R.string.menu_manga_scrolling_horizontal) to ScrollingType.Horizontal,
            getString(R.string.menu_manga_scrolling_vertical) to ScrollingType.Vertical
        )

        mBookMapScrollingMode = hashMapOf(
            getString(R.string.config_book_scrolling_infinity_scrolling) to ScrollingType.Scrolling,
            getString(R.string.config_book_scrolling_pagination) to ScrollingType.Pagination
        )

        mBookMapReadingTTS = TextSpeech.getByDescriptions(requireContext())

        val themeMode = requireContext().resources.getStringArray(R.array.theme_mode)
        mMangaMapThemeMode = hashMapOf(
            themeMode[0] to ThemeMode.SYSTEM,
            themeMode[1] to ThemeMode.LIGHT,
            themeMode[2] to ThemeMode.DARK
        )

        mMangaMapThemes = ThemeUtil.getThemes(requireContext())

        val adapterOrder = ArrayAdapter(requireContext(), R.layout.list_item, mMangaMapOrder.keys.toTypedArray())
        mMangaLibraryOrderAutoComplete.setAdapter(adapterOrder)
        mMangaLibraryOrderAutoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                mMangaOrderSelect =
                    if (parent.getItemAtPosition(position).toString().isNotEmpty() &&
                        mMangaMapOrder.containsKey(parent.getItemAtPosition(position).toString())
                    )
                        mMangaMapOrder[parent.getItemAtPosition(position).toString()]!!
                    else
                        Order.Name
            }

        val adapterOrderBook = ArrayAdapter(requireContext(), R.layout.list_item, mBookMapOrder.keys.toTypedArray())
        mBookLibraryOrderAutoComplete.setAdapter(adapterOrderBook)
        mBookLibraryOrderAutoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                mBookOrderSelect = if (parent.getItemAtPosition(position).toString().isNotEmpty() &&
                    mBookMapOrder.containsKey(parent.getItemAtPosition(position).toString())
                )
                    mBookMapOrder[parent.getItemAtPosition(position).toString()]!!
                else
                    Order.Name
            }

        val adapterLanguage = ArrayAdapter(requireContext(), R.layout.list_item, mMangaMapLanguage.keys.toTypedArray())
        mMangaDefaultSubtitleLanguageAutoComplete.setAdapter(adapterLanguage)
        mMangaDefaultSubtitleLanguageAutoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                mMangaDefaultSubtitleLanguageSelect = if (parent.getItemAtPosition(position).toString().isNotEmpty() &&
                        mMangaMapLanguage.containsKey(parent.getItemAtPosition(position).toString())
                    )
                        mMangaMapLanguage[parent.getItemAtPosition(position).toString()]!!
                    else
                        Languages.JAPANESE
            }

        mMangaSubtitleTranslateAutoComplete.setAdapter(adapterLanguage)
        mMangaSubtitleTranslateAutoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                mMangaDefaultSubtitleTranslateSelect =
                    if (parent.getItemAtPosition(position).toString().isNotEmpty() &&
                        mMangaMapLanguage.containsKey(parent.getItemAtPosition(position).toString())
                    )
                        mMangaMapLanguage[parent.getItemAtPosition(position).toString()]!!
                    else
                        Languages.PORTUGUESE
            }

        val adapterReaderMode = ArrayAdapter(requireContext(), R.layout.list_item, mMangaMapReaderMode.keys.toTypedArray())
        mMangaReaderComicModeAutoComplete.setAdapter(adapterReaderMode)
        mMangaReaderComicModeAutoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                mMangaReaderModeSelectType =
                    if (parent.getItemAtPosition(position).toString().isNotEmpty() &&
                        mMangaMapReaderMode.containsKey(
                            parent.getItemAtPosition(position).toString()
                        )
                    )
                        mMangaMapReaderMode[parent.getItemAtPosition(position).toString()]!!
                    else
                        ReaderMode.FIT_WIDTH
            }

        val adapterPageMode = ArrayAdapter(requireContext(), R.layout.list_item, mMangaMapPageMode.keys.toTypedArray())
        mMangaPageModeAutoComplete.setAdapter(adapterPageMode)
        mMangaPageModeAutoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                mMangaPageModeSelectType = if (parent.getItemAtPosition(position).toString().isNotEmpty() &&
                        mMangaMapPageMode.containsKey(parent.getItemAtPosition(position).toString())
                    )
                        mMangaMapPageMode[parent.getItemAtPosition(position).toString()]!!
                    else
                        PageMode.Comics
            }

        val adapterMangaScrollingMode = ArrayAdapter(requireContext(), R.layout.list_item, mMangaMapScrollingMode.keys.toTypedArray())
        mMangaScrollingModeAutoComplete.setAdapter(adapterMangaScrollingMode)
        mMangaScrollingModeAutoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                mMangaScrollingModeSelectType = if (parent.getItemAtPosition(position).toString().isNotEmpty() &&
                    mMangaMapScrollingMode.containsKey(parent.getItemAtPosition(position).toString())
                )
                    mMangaMapScrollingMode[parent.getItemAtPosition(position).toString()]!!
                else
                    ScrollingType.Horizontal
            }

        val adapterBookScrollingMode = ArrayAdapter(requireContext(), R.layout.list_item, mBookMapScrollingMode.keys.toTypedArray())
        mBookScrollingModeAutoComplete.setAdapter(adapterBookScrollingMode)
        mBookScrollingModeAutoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                mBookScrollingModeSelect = if (parent.getItemAtPosition(position).toString().isNotEmpty() &&
                    mBookMapScrollingMode.containsKey(parent.getItemAtPosition(position).toString())
                )
                    mBookMapScrollingMode[parent.getItemAtPosition(position).toString()]!!
                else
                    ScrollingType.Pagination
            }

        val adapterBookReadingTTS = ArrayAdapter(requireContext(), R.layout.list_item, mBookMapReadingTTS.keys.toTypedArray())
        mBookReadingTTSAutoCompleteNormal.setAdapter(adapterBookReadingTTS)
        mBookReadingTTSAutoCompleteNormal.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                mBookReadingTTSSelectNormal = if (parent.getItemAtPosition(position).toString().isNotEmpty() &&
                    mBookMapReadingTTS.containsKey(parent.getItemAtPosition(position).toString())
                )
                    mBookMapReadingTTS[parent.getItemAtPosition(position).toString()]!!
                else
                    TextSpeech.getDefault(false)
            }

        mBookReadingTTSAutoCompleteJapanese.setAdapter(adapterBookReadingTTS)
        mBookReadingTTSAutoCompleteJapanese.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                mBookReadingTTSSelectJapanese = if (parent.getItemAtPosition(position).toString().isNotEmpty() &&
                    mBookMapReadingTTS.containsKey(parent.getItemAtPosition(position).toString())
                )
                    mBookMapReadingTTS[parent.getItemAtPosition(position).toString()]!!
                else
                    TextSpeech.getDefault(false)
            }

        val themesMode = ArrayAdapter(requireContext(), R.layout.list_item, mMangaMapThemeMode.keys.toTypedArray())
        mConfigSystemThemeModeAutoComplete.setAdapter(themesMode)
        mConfigSystemThemeModeAutoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                mConfigSystemThemeModeSelect =
                    if (parent.getItemAtPosition(position).toString().isNotEmpty() &&
                        mMangaMapThemeMode.containsKey(
                            parent.getItemAtPosition(position).toString()
                        )
                    )
                        mMangaMapThemeMode[parent.getItemAtPosition(position).toString()]
                            ?: ThemeMode.SYSTEM
                    else
                        ThemeMode.SYSTEM

                saveTheme()

                when (mConfigSystemThemeModeSelect) {
                    ThemeMode.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    ThemeMode.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }

        val date0 = SimpleDateFormat(mConfigSystemDatePattern[0]).format(Date())
        val date1 = SimpleDateFormat(mConfigSystemDatePattern[1]).format(Date())
        val date2 = SimpleDateFormat(mConfigSystemDatePattern[2]).format(Date())
        val date3 = SimpleDateFormat(mConfigSystemDatePattern[3]).format(Date())

        val dataFormat = listOf(
            getString(R.string.config_option_date_time_format_0).format(date0),
            getString(R.string.config_option_date_time_format_1).format(date1),
            getString(R.string.config_option_date_time_format_2).format(date2),
            getString(R.string.config_option_date_time_format_3).format(date3)
        )
        val adapterDataFormat = ArrayAdapter(requireContext(), R.layout.list_item, dataFormat)
        mConfigSystemFormatDateAutoComplete.setAdapter(adapterDataFormat)
        mConfigSystemFormatDateAutoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                mConfigSystemDateSelect =
                    if (mConfigSystemDatePattern.size > position && position >= 0)
                        mConfigSystemDatePattern[position]
                    else
                        GeneralConsts.CONFIG.DATA_FORMAT[0]

                mConfigSystemDateSmall =
                    if (mConfigSystemDateSmallPattern.size > position && position >= 0)
                        mConfigSystemDateSmallPattern[position]
                    else
                        GeneralConsts.CONFIG.DATA_FORMAT_SMALL[0]
            }

        mConfigSystemBackup.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/*"
                putExtra(
                    Intent.EXTRA_MIME_TYPES, arrayOf(
                        "application/sqlite3"
                    )
                )

                val fileName: String = "BilingualReader_" + SimpleDateFormat(
                    GeneralConsts.PATTERNS.BACKUP_DATE_PATTERN,
                    Locale.getDefault()
                ).format(
                    Date()
                ) + ".sqlite3"
                putExtra(Intent.EXTRA_TITLE, fileName)
            }
            startActivityForResult(intent, GeneralConsts.REQUEST.GENERATE_BACKUP)
        }

        mConfigSystemRestore.setOnClickListener { choiceBackup() }

        mConfigUpdateApp.setOnClickListener {
            val update = UpdateApp(requireContext())
            val version = "33"
            MaterialAlertDialogBuilder(requireContext(), R.style.AppCompatMaterialAlertDialog)
                .setTitle(getString(R.string.config_update_app_title))
                .setMessage(getString(R.string.config_update_app_description, version))
                .setPositiveButton(R.string.action_confirm) { _, _ ->
                    try {
                        update.download("https://firebaseappdistribution.googleapis.com/app-binary-downloads/projects/269550539712/apps/1:269550539712:android:11dc80ed33aaa4b14dee25/releases/1jbdetlup0png/binaries/02a0bc537c83f162860d4b66326264265abcb9dd7b126329e58a9321becb4ce9/app.apk?token=AFb1MRwAAAAAZ_CMbBXnz5xwCNFZ6cLRJjKovnDDYRnZ3A31xyFnaGTmt-JBF_mFI9LBLy1zzakfe94nET9FAeR6iiy-3BoTCUTpRnB3lEVkajUOYdl5GZv0qoh-03pnMdK7IOKnH_9x1AXQrKstD8UEeKoy_mgq8AcDc8BJlDXTCBxciToA4TJlhtRD1GhCV9D0h3AR-_6IncuLNye2AySww-ptC0warl-Ex1ur81bvnl20ze4ab1i7PPmpC0U1cgVjVAA_Xfs_Z4gljrn0bMA-SlKjxcxl8qj4qE7PMwRTNFHQwlFd7dAJn0R82uE1T_EiROl9PRY6fOP76FQW0tHFYCHTTkENjsFlN1A")
                    } catch (e: Exception) {
                        mLOGGER.error("Error delete bitmap to cache: " + e.message, e)
                        Toast.makeText(requireContext(), getString(R.string.config_update_app_error), Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(R.string.action_cancel) { _, _ -> }
                .create().show()
        }

        mConfigCoversDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext(), R.style.AppCompatMaterialAlertDialog)
                .setTitle(getString(R.string.config_covers_delete_title))
                .setMessage(getString(R.string.config_covers_delete_description))
                .setPositiveButton(R.string.action_confirm) { _, _ ->
                    try {
                        val cacheManga = File(GeneralConsts.getCoverDir(requireContext()), GeneralConsts.CACHE_FOLDER.MANGA_COVERS)
                        if (cacheManga.exists())
                            cacheManga.listFiles()?.let {
                                for (f in it)
                                    f.delete()
                            }

                        val cacheBook = File(GeneralConsts.getCoverDir(requireContext()), GeneralConsts.CACHE_FOLDER.BOOK_COVERS)
                        if (cacheBook.exists())
                            cacheBook.listFiles()?.let {
                                for (f in it)
                                    f.delete()
                            }

                        Toast.makeText(requireContext(), getString(R.string.config_covers_delete_success), Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        mLOGGER.error("Error delete bitmap to cache: " + e.message, e)
                        Toast.makeText(requireContext(), getString(R.string.config_covers_delete_error), Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(R.string.action_cancel) { _, _ -> }
                .create().show()
        }

        mConfigStatisticsDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext(), R.style.AppCompatMaterialAlertDialog)
                .setTitle(getString(R.string.config_statistics_clear_title))
                .setMessage(getString(R.string.config_statistics_clear_description))
                .setPositiveButton(R.string.action_confirm) { _, _ ->
                    try {
                        HistoryRepository(requireContext()).clearAll()
                        Toast.makeText(requireContext(), getString(R.string.config_statistics_clear_success), Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        mLOGGER.error("Error delete statistics: " + e.message, e)
                        Toast.makeText(requireContext(), getString(R.string.config_statistics_clear_error), Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(R.string.action_cancel) { _, _ -> }
                .create().show()
        }

        mConfigSystemShareMarkCloudMap = hashMapOf(
            getString(R.string.share_mark_firestore) to ShareMarkCloud.FIRESTORE,
            getString(R.string.share_mark_google_drive) to ShareMarkCloud.GOOGLE_DRIVE
        )

        val adapterShareMark = ArrayAdapter(requireContext(), R.layout.list_item, mConfigSystemShareMarkCloudMap.keys.toTypedArray())
        mConfigSystemShareMarkTypeAutoComplete.setAdapter(adapterShareMark)
        mConfigSystemShareMarkTypeAutoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                mConfigSystemShareMarkCloudSelect = if (parent.getItemAtPosition(position).toString().isNotEmpty() &&
                    mConfigSystemShareMarkCloudMap.containsKey(parent.getItemAtPosition(position).toString()))
                    mConfigSystemShareMarkCloudMap[parent.getItemAtPosition(position).toString()]!!
                else
                    ShareMarkCloud.GOOGLE_DRIVE
            }

        prepareThemes()
        prepareFonts()
        loadConfig()

        mConfigSystemShareMarkAccount.setOnLongClickListener {
            MaterialAlertDialogBuilder(requireContext(), R.style.AppCompatMaterialAlertDialog)
                .setTitle(getString(R.string.config_system_share_mark_sign_out_title))
                .setMessage(getString(R.string.config_system_share_mark_sign_out))
                .setPositiveButton(R.string.action_confirm) { _, _ ->
                    GoogleSignIn.getLastSignedInAccount(requireContext())?.let {
                        getSignClient().signOut()
                    }
                    googleSigIn(null)
                }
                .setNegativeButton(R.string.action_cancel) { _, _ -> }
                .create().show()
            true
        }

        mConfigSystemShareMarkMangaLastSync.setOnLongClickListener {
            MaterialAlertDialogBuilder(requireContext(), R.style.AppCompatMaterialAlertDialog)
                .setTitle(getString(R.string.config_system_share_mark_clear_last_sync_title))
                .setMessage(getString(R.string.config_system_share_mark_clear_last_sync))
                .setPositiveButton(R.string.action_confirm) { _, _ ->
                    ShareMarkBase.clearLastSync(requireContext(), Type.MANGA)
                    mConfigSystemShareMarkMangaLastSync.visibility = View.GONE
                }
                .setNegativeButton(R.string.action_cancel) { _, _ -> }
                .create().show()
            true
        }

        mConfigSystemShareMarkBookLastSync.setOnLongClickListener {
            MaterialAlertDialogBuilder(requireContext(), R.style.AppCompatMaterialAlertDialog)
                .setTitle(getString(R.string.config_system_share_mark_clear_last_sync_title))
                .setMessage(getString(R.string.config_system_share_mark_clear_last_sync))
                .setPositiveButton(R.string.action_confirm) { _, _ ->
                    ShareMarkBase.clearLastSync(requireContext(), Type.BOOK)
                    mConfigSystemShareMarkBookLastSync.visibility = View.GONE
                }
                .setNegativeButton(R.string.action_cancel) { _, _ -> }
                .create().show()
            true
        }

        mConfigSystemShareMarkSignIn.setOnClickListener {
            val googleSignInClient = getSignClient()
            startActivityForResult(googleSignInClient.signInIntent, GeneralConsts.REQUEST.GOOGLE_SIGN_IN)
        }

        googleSigIn(GoogleSignIn.getLastSignedInAccount(requireContext()))

        mViewModel.loadLibrary(null)
    }

    override fun onStop() {
        saveConfig()
        super.onStop()
    }

    override fun onDestroyView() {
        mViewModel.removeLibraryDefault(mMangaLibraryPath.editText?.text.toString(), mBookLibraryPath.editText?.text.toString())
        ViewModelProvider(this)[MangaLibraryViewModel::class.java].setDefaultLibrary(LibraryUtil.getDefault(requireContext(), Type.MANGA))
        ViewModelProvider(this)[BookLibraryViewModel::class.java].setDefaultLibrary(LibraryUtil.getDefault(requireContext(), Type.BOOK))
        (requireActivity() as MainActivity).setLibraries(mViewModel.getListLibrary())

        super.onDestroyView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            GeneralConsts.REQUEST.OPEN_MANGA_FOLDER -> {
                var folder = ""
                if (data != null && resultCode == RESULT_OK) {
                    folder = Util.normalizeFilePath(data.data?.path.toString())

                    if (!Storage.isPermissionGranted(requireContext()))
                        Storage.takePermission(requireContext(), requireActivity())
                }

                mViewModel.saveDefault(Type.MANGA, folder)

                if (!folder.equals(mMangaLibraryPathAutoComplete.text.toString(), true)) {
                    mViewModel.deleteAllByPathDefault(Type.MANGA, mMangaLibraryPathAutoComplete.text.toString())
                    ViewModelProvider(requireActivity())[MangaLibraryViewModel::class.java].emptyList(LibraryUtil.getDefault(requireContext(), Type.MANGA).id!!)
                }

                mMangaLibraryPathAutoComplete.setText(folder)
            }

            GeneralConsts.REQUEST.OPEN_BOOK_FOLDER -> {
                var folder = ""
                if (data != null && resultCode == RESULT_OK) {
                    folder = Util.normalizeFilePath(data.data?.path.toString())

                    if (!Storage.isPermissionGranted(requireContext()))
                        Storage.takePermission(requireContext(), requireActivity())
                }

                mViewModel.saveDefault(Type.BOOK, folder)

                if (!folder.equals(mBookLibraryPathAutoComplete.text.toString(), true)) {
                    mViewModel.deleteAllByPathDefault(Type.BOOK, mBookLibraryPathAutoComplete.text.toString())
                    ViewModelProvider(requireActivity())[BookLibraryViewModel::class.java].emptyList(LibraryUtil.getDefault(requireContext(), Type.BOOK).id!!)
                }

                mBookLibraryPathAutoComplete.setText(folder)
            }

            GeneralConsts.REQUEST.CONFIG_LIBRARIES -> {
                val clear = data?.extras?.getBoolean(GeneralConsts.KEYS.LIBRARY.CLEAR_LIBRARY_LIST) ?: false
                mViewModel.loadLibrary(null)
                (requireActivity() as MainActivity).setLibraries(mViewModel.getListLibrary())

                if (clear) {
                    val extra = data?.extras
                    if (extra!!.containsKey(GeneralConsts.KEYS.LIBRARY.LIBRARY_TYPE)) {
                        val type = Type.valueOf(extra!!.getString(GeneralConsts.KEYS.LIBRARY.LIBRARY_TYPE)!!)
                        val libraries = extra.getLongArray(GeneralConsts.KEYS.LIBRARY.LIBRARY_ARRAY_ID)!!
                        for (library in libraries)
                            when(type) {
                                Type.BOOK -> ViewModelProvider(requireActivity())[BookLibraryViewModel::class.java].emptyList(library)
                                Type.MANGA -> ViewModelProvider(requireActivity())[MangaLibraryViewModel::class.java].emptyList(library)
                            }
                    }
                }
            }

            GeneralConsts.REQUEST.GENERATE_BACKUP -> {
                val fileUri: Uri? = data?.data
                try {
                    fileUri?.let {
                        DataBase.backupDatabase(
                            requireContext(),
                            File(Util.normalizeFilePath(it.path.toString()))
                        )
                    }
                } catch (e: BackupError) {
                    MsgUtil.error(
                        requireContext(),
                        getString(R.string.config_database_restore),
                        getString(R.string.config_database_error_backup)
                    ) { _, _ -> }
                } catch (e: Exception) {
                    mLOGGER.warn("Backup Generate Failed.", e)
                    MsgUtil.error(
                        requireContext(),
                        getString(R.string.config_database_restore),
                        getString(R.string.config_database_error_backup)
                    ) { _, _ -> }
                }
            }

            GeneralConsts.REQUEST.RESTORE_BACKUP -> {
                val fileUri: Uri? = data?.data
                try {
                    fileUri?.let {
                        val file = File(Util.normalizeFilePath(it.path.toString()))
                        if (DataBase.validDatabaseFile(requireContext(), it))
                            DataBase.restoreDatabase(requireContext(), file)
                        else
                            MsgUtil.alert(requireContext(), getString(R.string.config_database_restore), getString(R.string.config_database_invalid_file)) { _, _ -> }
                    }
                } catch (e: InvalidDatabase) {
                    MsgUtil.error(
                        requireContext(),
                        getString(R.string.config_database_restore),
                        getString(R.string.config_database_invalid_file)
                    ) { _, _ -> }
                } catch (e: RestoredNewDatabase) {
                    MsgUtil.error(
                        requireContext(),
                        getString(R.string.config_database_restore),
                        getString(R.string.config_database_error_new_database)
                    ) { _, _ -> }
                } catch (e: ErrorRestoreDatabase) {
                    MsgUtil.error(
                        requireContext(),
                        getString(R.string.config_database_restore),
                        getString(R.string.config_database_error_restore)
                    ) { _, _ -> }
                } catch (e: IOException) {
                    mLOGGER.warn("Backup Restore Failed.", e)
                    MsgUtil.error(
                        requireContext(),
                        getString(R.string.config_database_restore),
                        getString(R.string.config_database_error_read_file)
                    ) { _, _ -> }
                }
            }

            GeneralConsts.REQUEST.GOOGLE_SIGN_IN -> {
                val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                    googleSigIn(account)
                } catch (e: ApiException) {
                    mLOGGER.warn("SignIn failed code=" + e.statusCode, e)
                    googleSigIn(null)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.config_system_share_mark_sign_in_error),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == GeneralConsts.REQUEST.PERMISSION_FILES_ACCESS && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            MaterialAlertDialogBuilder(requireContext(), R.style.AppCompatAlertDialogStyle)
                .setTitle(requireContext().getString(R.string.alert_permission_files_access_denied_title))
                .setMessage(requireContext().getString(R.string.alert_permission_files_access_denied))
                .setPositiveButton(R.string.action_neutral) { _, _ -> }.create().show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_config, container, false)
    }

    private fun saveConfig() {
        mViewModel.saveDefault(Type.MANGA, mMangaLibraryPath.editText?.text.toString())
        mViewModel.saveDefault(Type.BOOK, mBookLibraryPath.editText?.text.toString())

        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
        with(sharedPreferences.edit()) {
            this.putString(
                GeneralConsts.KEYS.LIBRARY.MANGA_ORDER,
                mMangaOrderSelect.toString()
            )

            this.putString(
                GeneralConsts.KEYS.SUBTITLE.LANGUAGE,
                mMangaDefaultSubtitleLanguageSelect.toString()
            )

            this.putString(
                GeneralConsts.KEYS.SUBTITLE.TRANSLATE,
                mMangaDefaultSubtitleTranslateSelect.toString()
            )

            this.putString(
                GeneralConsts.KEYS.READER.MANGA_PAGE_MODE,
                mMangaPageModeSelectType.toString()
            )

            this.putString(
                GeneralConsts.KEYS.READER.MANGA_READER_MODE,
                mMangaReaderModeSelectType.toString()
            )

            this.putString(
                GeneralConsts.KEYS.READER.MANGA_PAGE_SCROLLING_MODE,
                mMangaScrollingModeSelectType.toString()
            )

            this.putBoolean(
                GeneralConsts.KEYS.READER.MANGA_SHOW_CLOCK_AND_BATTERY,
                mMangaShowClockAndBattery.isChecked
            )

            this.putBoolean(
                GeneralConsts.KEYS.READER.MANGA_USE_MAGNIFIER_TYPE,
                mMangaUseMagnifierType.isChecked
            )

            this.putBoolean(
                GeneralConsts.KEYS.READER.MANGA_KEEP_ZOOM_BETWEEN_PAGES,
                mMangaKeepZoomBetweenPages.isChecked
            )

            this.putBoolean(
                GeneralConsts.KEYS.PAGE_LINK.USE_DUAL_PAGE_CALCULATE,
                mMangaUseDualPageCalculate.isChecked
            )

            this.putBoolean(
                GeneralConsts.KEYS.PAGE_LINK.USE_PAGE_PATH_FOR_LINKED,
                mMangaUsePathNameForLinked.isChecked
            )

            this.putString(
                GeneralConsts.KEYS.LIBRARY.BOOK_ORDER,
                mBookOrderSelect.toString()
            )

            this.putString(
                GeneralConsts.KEYS.READER.BOOK_PAGE_SCROLLING_MODE,
                mBookScrollingModeSelect.toString()
            )

            this.putString(
                GeneralConsts.KEYS.READER.BOOK_READER_TTS_VOICE_NORMAL,
                mBookReadingTTSSelectNormal.toString()
            )

            this.putString(
                GeneralConsts.KEYS.READER.BOOK_READER_TTS_VOICE_JAPANESE,
                mBookReadingTTSSelectJapanese.toString()
            )

            this.putFloat(
                GeneralConsts.KEYS.READER.BOOK_READER_TTS_SPEED,
                mBookReadingSpeed.value
            )

            this.putBoolean(
                GeneralConsts.KEYS.READER.BOOK_PROCESS_JAPANESE_TEXT,
                mBookReaderProcessJapaneseText.isChecked
            )

            this.putBoolean(
                GeneralConsts.KEYS.READER.BOOK_GENERATE_FURIGANA_ON_TEXT,
                mBookReaderTextWithFurigana.isChecked
            )

            this.putBoolean(
                GeneralConsts.KEYS.READER.BOOK_FONT_JAPANESE_STYLE,
                mBookFontJapaneseStyle.isChecked
            )

            this.putBoolean(
                GeneralConsts.KEYS.READER.BOOK_PROCESS_VOCABULARY,
                mBookReaderProcessVocabulary.isChecked
            )

            this.putFloat(
                GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE,
                mBookFontSize.value
            )

            this.putBoolean(
                GeneralConsts.KEYS.SYSTEM.SHARE_MARK_ENABLED,
                mConfigSystemShareMarkEnabled.isChecked
            )

            val share = mConfigSystemShareMarkCloudSelect.toString()
            if (share != sharedPreferences.getString(GeneralConsts.KEYS.SYSTEM.SHARE_MARK_CLOUD, ShareMarkCloud.GOOGLE_DRIVE.toString())) {
                this.remove(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_MANGA)
                this.remove(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_BOOK)
            }

            this.putString(
                GeneralConsts.KEYS.SYSTEM.SHARE_MARK_CLOUD,
                share
            )

            this.putString(
                GeneralConsts.KEYS.SYSTEM.FORMAT_DATA,
                mConfigSystemDateSelect
            )

            this.putString(
                GeneralConsts.KEYS.SYSTEM.FORMAT_DATA_SMALL,
                mConfigSystemDateSmall
            )

            this.putString(
                GeneralConsts.KEYS.THEME.THEME_MODE,
                mConfigSystemThemeModeSelect.toString()
            )

            this.putString(
                GeneralConsts.KEYS.THEME.THEME_USED,
                mConfigSystemThemeSelect.toString()
            )

            this.commit()
        }

        mLOGGER.info(
            "Save prefer CONFIG:" + "\n[Library] Path " + mMangaLibraryPath.editText?.text +
                    " - Order " + mMangaLibraryOrder.editText?.text +
                    "\n[SubTitle] Language " + mMangaDefaultSubtitleLanguage.editText?.text +
                    " - Translate " + mMangaDefaultSubtitleTranslate.editText?.text +
                    "\n[System] Format Data " + mConfigSystemFormatDate.editText?.text +
                    "\n[Book] Path " + mBookLibraryPath.editText?.text +
                    " - Order " + mBookLibraryOrder.editText?.text
        )

    }

    private fun loadConfig() {
        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())

        mMangaLibraryPath.editText?.setText(mViewModel.getDefault(Type.MANGA))

        mMangaPageModeSelectType = PageMode.valueOf(
            sharedPreferences.getString(
                GeneralConsts.KEYS.READER.MANGA_PAGE_MODE,
                PageMode.Comics.toString()
            )!!
        )
        mMangaReaderModeSelectType = ReaderMode.valueOf(
            sharedPreferences.getString(
                GeneralConsts.KEYS.READER.MANGA_READER_MODE,
                ReaderMode.FIT_WIDTH.toString()
            )!!
        )
        mMangaScrollingModeSelectType = ScrollingType.valueOf(
            sharedPreferences.getString(
                GeneralConsts.KEYS.READER.MANGA_PAGE_SCROLLING_MODE,
                ScrollingType.Horizontal.toString()
            )!!
        )
        mMangaOrderSelect = Order.valueOf(
            sharedPreferences.getString(
                GeneralConsts.KEYS.LIBRARY.MANGA_ORDER,
                Order.Name.toString()
            )!!
        )
        mMangaDefaultSubtitleLanguageSelect = Languages.valueOf(
            sharedPreferences.getString(
                GeneralConsts.KEYS.SUBTITLE.LANGUAGE,
                Languages.JAPANESE.toString()
            )!!
        )
        mMangaDefaultSubtitleTranslateSelect = Languages.valueOf(
            sharedPreferences.getString(
                GeneralConsts.KEYS.SUBTITLE.TRANSLATE,
                Languages.PORTUGUESE.toString()
            )!!
        )
        mMangaLibraryOrderAutoComplete.setText(
            mMangaMapOrder.entries.first { it.value == mMangaOrderSelect }.key,
            false
        )
        mMangaDefaultSubtitleLanguageAutoComplete.setText(
            mMangaMapLanguage.entries.first { it.value == mMangaDefaultSubtitleLanguageSelect }.key,
            false
        )
        mMangaSubtitleTranslateAutoComplete.setText(
            mMangaMapLanguage.entries.first { it.value == mMangaDefaultSubtitleTranslateSelect }.key,
            false
        )
        mMangaReaderComicModeAutoComplete.setText(
            mMangaMapReaderMode.entries.first { it.value == mMangaReaderModeSelectType }.key,
            false
        )
        mMangaPageModeAutoComplete.setText(
            mMangaMapPageMode.entries.first { it.value == mMangaPageModeSelectType }.key,
            false
        )
        mMangaScrollingModeAutoComplete.setText(
            mMangaMapScrollingMode.entries.first { it.value == mMangaScrollingModeSelectType }.key,
            false
        )
        mMangaShowClockAndBattery.isChecked = sharedPreferences.getBoolean(
            GeneralConsts.KEYS.READER.MANGA_SHOW_CLOCK_AND_BATTERY,
            false
        )
        mMangaUseMagnifierType.isChecked = sharedPreferences.getBoolean(
            GeneralConsts.KEYS.READER.MANGA_USE_MAGNIFIER_TYPE,
            false
        )
        mMangaKeepZoomBetweenPages.isChecked = sharedPreferences.getBoolean(
            GeneralConsts.KEYS.READER.MANGA_KEEP_ZOOM_BETWEEN_PAGES,
            false
        )
        mMangaUseDualPageCalculate.isChecked = sharedPreferences.getBoolean(
            GeneralConsts.KEYS.PAGE_LINK.USE_DUAL_PAGE_CALCULATE,
            false
        )
        mMangaUsePathNameForLinked.isChecked = sharedPreferences.getBoolean(
            GeneralConsts.KEYS.PAGE_LINK.USE_PAGE_PATH_FOR_LINKED,
            false
        )

        mBookFontSize.value = sharedPreferences.getFloat(
            GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE,
            GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE_DEFAULT
        )
        mBookLibraryPath.editText?.setText(mViewModel.getDefault(Type.BOOK))

        mBookOrderSelect = Order.valueOf(
            sharedPreferences.getString(
                GeneralConsts.KEYS.LIBRARY.BOOK_ORDER,
                Order.Name.toString()
            )!!
        )

        mBookScrollingModeSelect = ScrollingType.valueOf(
            sharedPreferences.getString(
                GeneralConsts.KEYS.READER.BOOK_PAGE_SCROLLING_MODE,
                ScrollingType.Pagination.toString()
            )!!
        )

        mBookReadingTTSSelectNormal = TextSpeech.valueOf(
            sharedPreferences.getString(
                GeneralConsts.KEYS.READER.BOOK_READER_TTS_VOICE_NORMAL,
                TextSpeech.getDefault(false).toString()
            )!!
        )

        mBookReadingTTSSelectJapanese = TextSpeech.valueOf(
            sharedPreferences.getString(
                GeneralConsts.KEYS.READER.BOOK_READER_TTS_VOICE_JAPANESE,
                TextSpeech.getDefault(true).toString()
            )!!
        )

        mBookReadingSpeed.value = sharedPreferences.getFloat(GeneralConsts.KEYS.READER.BOOK_READER_TTS_SPEED, GeneralConsts.KEYS.READER.BOOK_READER_TTS_SPEED_DEFAULT)

        mBookLibraryOrderAutoComplete.setText(
            mBookMapOrder.entries.first { it.value == mBookOrderSelect }.key,
            false
        )

        mBookScrollingModeAutoComplete.setText(
            mBookMapScrollingMode.entries.first { it.value == mBookScrollingModeSelect }.key,
            false
        )

        mBookReadingTTSAutoCompleteNormal.setText(
            mBookMapReadingTTS.entries.first { it.value == mBookReadingTTSSelectNormal }.key,
            false
        )

        mBookReadingTTSAutoCompleteJapanese.setText(
            mBookMapReadingTTS.entries.first { it.value == mBookReadingTTSSelectJapanese }.key,
            false
        )

        mBookReaderProcessJapaneseText.isChecked = sharedPreferences.getBoolean(
            GeneralConsts.KEYS.READER.BOOK_PROCESS_JAPANESE_TEXT,
            true
        )

        mBookReaderTextWithFurigana.isChecked = sharedPreferences.getBoolean(
            GeneralConsts.KEYS.READER.BOOK_GENERATE_FURIGANA_ON_TEXT,
            true
        )

        mBookFontJapaneseStyle.isChecked = sharedPreferences.getBoolean(
            GeneralConsts.KEYS.READER.BOOK_FONT_JAPANESE_STYLE,
            false
        )

        mBookReaderProcessVocabulary.isChecked = sharedPreferences.getBoolean(
            GeneralConsts.KEYS.READER.BOOK_PROCESS_VOCABULARY,
            false
        )

        mConfigSystemDateSelect = sharedPreferences.getString(
            GeneralConsts.KEYS.SYSTEM.FORMAT_DATA,
            GeneralConsts.CONFIG.DATA_FORMAT[0]
        )!!
        mConfigSystemDateSmall = sharedPreferences.getString(
            GeneralConsts.KEYS.SYSTEM.FORMAT_DATA_SMALL,
            GeneralConsts.CONFIG.DATA_FORMAT_SMALL[0]
        )!!

        mConfigSystemShareMarkEnabled.isChecked = sharedPreferences.getBoolean(
            GeneralConsts.KEYS.SYSTEM.SHARE_MARK_ENABLED,
            false
        )

        mConfigSystemShareMarkCloudSelect = ShareMarkCloud.valueOf(
            sharedPreferences.getString(
                GeneralConsts.KEYS.SYSTEM.SHARE_MARK_CLOUD,
                ShareMarkCloud.GOOGLE_DRIVE.toString()
            )!!
        )

        mConfigSystemShareMarkTypeAutoComplete.setText(
            mConfigSystemShareMarkCloudMap.entries.first { it.value == mConfigSystemShareMarkCloudSelect }.key,
            false
        )

        mConfigSystemFormatDateAutoComplete.setText(
            "$mConfigSystemDateSelect (%s)".format(
                SimpleDateFormat(mConfigSystemDateSelect).format(
                    Date()
                )
            ), false
        )

        if (sharedPreferences.contains(GeneralConsts.KEYS.DATABASE.LAST_BACKUP)) {
            val backup = sharedPreferences.getString(
                GeneralConsts.KEYS.DATABASE.LAST_BACKUP,
                Date().toString()
            )?.let {
                SimpleDateFormat(
                    GeneralConsts.PATTERNS.DATE_TIME_PATTERN,
                    Locale.getDefault()
                ).parse(
                    it
                )
            }
            mConfigSystemLastBackup.text = getString(
                R.string.config_database_last_backup,
                backup?.let {
                    SimpleDateFormat(
                        mConfigSystemDateSelect + " " + GeneralConsts.PATTERNS.TIME_PATTERN,
                        Locale.getDefault()
                    ).format(
                        it
                    )
                }
            )
        }

        mConfigSystemThemeModeSelect = ThemeMode.valueOf(
            sharedPreferences.getString(
                GeneralConsts.KEYS.THEME.THEME_MODE,
                ThemeMode.SYSTEM.toString()
            )!!
        )
        mConfigSystemThemeSelect = Themes.valueOf(
            sharedPreferences.getString(
                GeneralConsts.KEYS.THEME.THEME_USED,
                Themes.ORIGINAL.toString()
            )!!
        )
        mConfigSystemThemeModeAutoComplete.setText(
            mMangaMapThemeMode.entries.first { it.value == mConfigSystemThemeModeSelect }.key,
            false
        )

        mConfigSystemShareMarkMangaLastSync.visibility = if (sharedPreferences.contains(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_MANGA)) {
            val sync = sharedPreferences.getString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_MANGA, Date().toString())
            val dateSync = SimpleDateFormat(GeneralConsts.SHARE_MARKS.PARSE_DATE_TIME, Locale.getDefault()).parse(sync)
            val lastSync = SimpleDateFormat(mConfigSystemDateSelect + " " + GeneralConsts.PATTERNS.TIME_PATTERN, Locale.getDefault()).format(dateSync)
            mConfigSystemShareMarkMangaLastSync.text = getString(R.string.config_system_share_mark_manga, lastSync)
            View.VISIBLE
        } else
            View.GONE

        mConfigSystemShareMarkBookLastSync.visibility = if (sharedPreferences.contains(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_BOOK)) {
            val sync = sharedPreferences.getString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_BOOK, Date().toString())
            val dateSync = SimpleDateFormat(GeneralConsts.SHARE_MARKS.PARSE_DATE_TIME, Locale.getDefault()).parse(sync)
            val lastSync = SimpleDateFormat(mConfigSystemDateSelect + " " + GeneralConsts.PATTERNS.TIME_PATTERN, Locale.getDefault()).format(dateSync)
            mConfigSystemShareMarkBookLastSync.text = getString(R.string.config_system_share_mark_book, lastSync)
            View.VISIBLE
        } else
            View.GONE
    }

    private fun openLibraries(type: Type) {
        val intent = Intent(requireContext(), MenuActivity::class.java)
        val bundle = Bundle()
        bundle.putInt(GeneralConsts.KEYS.FRAGMENT.ID, R.id.frame_config_libraries)
        bundle.putString(GeneralConsts.KEYS.LIBRARY.LIBRARY_TYPE, type.toString())
        intent.putExtras(bundle)
        requireActivity().overridePendingTransition(
            R.anim.fade_in_fragment_add_enter,
            R.anim.fade_out_fragment_remove_exit
        )
        startActivityForResult(intent, GeneralConsts.REQUEST.CONFIG_LIBRARIES, null)
    }

    private fun prepareThemes() {
        val listener = object : ThemesListener {
            override fun onClick(theme: Pair<Themes, Boolean>) {
                mConfigSystemThemeSelect = theme.first
                saveTheme()

                mViewModel.setEnableTheme(theme.first)
                requireActivity().setTheme(mConfigSystemThemeSelect.getValue())
                restartTheme()
            }
        }

        val theme = Themes.valueOf(
            GeneralConsts.getSharedPreferences(requireContext())
                .getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!
        )

        mViewModel.loadThemes(theme)
        val lineAdapter = ThemesCardAdapter(requireContext(), mViewModel.themes.value!!, listener)
        mConfigSystemThemes.adapter = lineAdapter

        mConfigSystemThemes.scrollBy(mViewModel.getSelectedThemeIndex())

        mViewModel.themes.observe(viewLifecycleOwner) {
            lineAdapter.updateList(it)
        }
    }

    private fun saveTheme() {
        with(GeneralConsts.getSharedPreferences(requireContext()).edit()) {
            this.putString(
                GeneralConsts.KEYS.THEME.THEME_MODE,
                mConfigSystemThemeModeSelect.toString()
            )

            this.putString(
                GeneralConsts.KEYS.THEME.THEME_USED,
                mConfigSystemThemeSelect.toString()
            )

            this.putBoolean(
                GeneralConsts.KEYS.THEME.THEME_CHANGE,
                true
            )

            this.commit()
        }
    }

    //to change the theme it is necessary to recreate the active, in this case it will signal to open the config
    private fun restartTheme() {
        with(GeneralConsts.getSharedPreferences(requireContext()).edit()) {
            this.putBoolean(
                GeneralConsts.KEYS.THEME.THEME_CHANGE,
                true
            )
            this.commit()
        }
        requireActivity().recreate()
    }

    private fun prepareFonts() {
        val listenerNormal = object : FontsListener {
            override fun onClick(font: Pair<FontType, Boolean>) {
                saveFont(font.first, false)
                mViewModel.setEnableFont(font.first, false)
            }
        }

        val fontNormal = FontType.valueOf(
            GeneralConsts.getSharedPreferences(requireContext())
                .getString(GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_TYPE_NORMAL, FontType.TimesNewRoman.toString())!!
        )

        mViewModel.loadFontsNormal(fontNormal)
        val lineAdapterNormal = FontsCardAdapter(requireContext(), mViewModel.fontsNormal.value!!, listenerNormal)
        mBookFontTypeNormal.adapter = lineAdapterNormal
        mBookFontTypeNormal.scrollBy(mViewModel.getSelectedFontTypeIndex(false))

        mViewModel.fontsNormal.observe(viewLifecycleOwner) {
            lineAdapterNormal.updateList(it)
        }

        val listenerJapanese = object : FontsListener {
            override fun onClick(font: Pair<FontType, Boolean>) {
                saveFont(font.first, true)
                mViewModel.setEnableFont(font.first, true)
            }
        }

        val fontJapanese = FontType.valueOf(
            GeneralConsts.getSharedPreferences(requireContext())
                .getString(GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_TYPE_JAPANESE, FontType.BabelStoneErjian1.toString())!!
        )

        mViewModel.loadFontsJapanese(fontJapanese)
        val lineAdapterJapanese = FontsCardAdapter(requireContext(), mViewModel.fontsJapanese.value!!, listenerJapanese)
        mBookFontTypeJapanese.adapter = lineAdapterJapanese
        mBookFontTypeJapanese.scrollBy(mViewModel.getSelectedFontTypeIndex(true))

        mViewModel.fontsJapanese.observe(viewLifecycleOwner) {
            lineAdapterJapanese.updateList(it)
        }
    }

    private fun saveFont(font: FontType, isJapanese : Boolean) {
        with(GeneralConsts.getSharedPreferences(requireContext()).edit()) {
            if (isJapanese)
                this.putString(
                    GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_TYPE_JAPANESE,
                    font.toString()
                )
            else
                this.putString(
                    GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_TYPE_NORMAL,
                    font.toString()
                )

            this.commit()
        }
    }

    private fun getSignClient() : GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(Secrets.getSecrets(requireContext()).getGoogleIdToken())
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE))
            .build()

        return GoogleSignIn.getClient(requireActivity(), signInOptions)
    }

    private fun googleSigIn(account: GoogleSignInAccount?) {
        if (account != null) {
            mConfigSystemShareMarkAccount.visibility = View.VISIBLE
            mConfigSystemShareMarkSignIn.visibility = View.GONE
            val display  = if (account.email != null) account.email else account.displayName
            mConfigSystemShareMarkAccount.text = requireContext().getString(R.string.config_system_share_mark_account, display)
        } else {
            mConfigSystemShareMarkAccount.visibility = View.GONE
            mConfigSystemShareMarkSignIn.visibility = View.VISIBLE
            mConfigSystemShareMarkAccount.text = ""
        }
    }

    private fun choiceBackup() {
        val selectDatabase = getString(R.string.config_database_restore_select_backup)
        val backups = mutableMapOf<String, String>()
        backups[selectDatabase] = selectDatabase

        val pattern = DateTimeFormatter.ofPattern(mConfigSystemDateSelect + " " + GeneralConsts.PATTERNS.TIME_PATTERN)
        val autoBackups = File(requireContext().filesDir, "/databasebackup")
        if (autoBackups.exists())
            autoBackups.listFiles()?.let {
                for (bkp in it.sortedDescending()) {
                    val name = bkp.name.replace("BilingualReader.db-", "").substringBeforeLast(".")
                    val date = LocalDateTime.parse(name.substring(0, 10) + "T" + name.substring(11))
                    backups[date.format(pattern)] = bkp.name
                    if (backups.size > 3)
                        break
                }
            }

        val items = backups.keys.toTypedArray()
        MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatMaterialAlertList)
            .setTitle(R.string.config_database_restore)
            .setItems(items) { _, selected ->
                val origin = items[selected]
                if (origin == selectDatabase) {
                    val i = Intent(Intent.ACTION_GET_CONTENT)
                    i.type = "*/*"
                    startActivityForResult(
                        Intent.createChooser(i, getString(R.string.config_database_select_file)),
                        GeneralConsts.REQUEST.RESTORE_BACKUP
                    )
                } else
                    DataBase.restoreDatabase(requireContext(), File(autoBackups, backups[origin]!!))
            }
            .show()
    }

}