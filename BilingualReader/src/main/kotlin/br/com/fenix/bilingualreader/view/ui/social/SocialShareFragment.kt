package br.com.fenix.bilingualreader.view.ui.social

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.databinding.FragmentSocialShareBinding
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.SocialAspectRatio
import br.com.fenix.bilingualreader.model.enums.SocialBackgroundPattern
import br.com.fenix.bilingualreader.model.enums.SocialColorTheme
import br.com.fenix.bilingualreader.model.enums.SocialLayoutTemplate
import br.com.fenix.bilingualreader.model.enums.SocialMediaTypeFilter
import br.com.fenix.bilingualreader.model.enums.SocialPeriodFilter
import br.com.fenix.bilingualreader.model.enums.SocialTextColorMode
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.controller.SocialImageGenerator
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import eightbitlab.com.blurview.GlassSetup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class SocialShareFragment : Fragment() {

    private var _binding: FragmentSocialShareBinding? = null
    private val binding get() = _binding!!

    private val mViewModel: SocialShareViewModel by viewModels()
    private lateinit var mCurationAdapter: SocialShareCurationAdapter
    private lateinit var mPreviewAdapter: SocialSharePreviewAdapter

    private var mIsCurationExpanded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSocialShareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar()
        initArgs()
        initViews()
        observeViewModel()
    }

    private fun initToolbar() {
        (requireActivity() as? MenuActivity)?.let { activity ->
            activity.setActionBar(binding.socialToolbar)
            activity.supportActionBar?.setDisplayShowTitleEnabled(true)
            activity.supportActionBar?.setTitle(R.string.social_share_title)
        }
        binding.socialToolbar.title = getString(R.string.social_share_title)

        val context = requireContext()
        val sharedPreferences = GeneralConsts.getSharedPreferences(context)
        val isGlass = sharedPreferences.getBoolean(GeneralConsts.KEYS.THEME.THEME_GLASSMORPHISM, false)

        if (isGlass) {
            val background = ThemeUtil.getBlurFrameClearDrawable(context)
            GlassSetup.setupGlass(binding.socialBlurTop, binding.socialScroll)
                .setFrameClearDrawable(background)
                .setBlurRadius(10F)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.socialBlurTop) { v, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(v.paddingLeft, statusBarHeight, v.paddingRight, v.paddingBottom)
            insets
        }
    }

    private fun initArgs() {
        val args = arguments ?: activity?.intent?.extras
        if (args != null) {
            val typeStr = args.getString(GeneralConsts.KEYS.OBJECT.SOCIAL_SHARE_TYPE)
            val year = args.getInt(GeneralConsts.KEYS.OBJECT.SOCIAL_SHARE_YEAR, -1)
            val hasExplicitLibrary = args.containsKey(GeneralConsts.KEYS.OBJECT.SOCIAL_SHARE_LIBRARY)
            val libraryId = if (hasExplicitLibrary) args.getLong(GeneralConsts.KEYS.OBJECT.SOCIAL_SHARE_LIBRARY) else null

            val mediaType = when (typeStr) {
                Type.MANGA.name -> {
                    binding.socialChipMediaManga.isChecked = true
                    SocialMediaTypeFilter.MANGA
                }
                Type.BOOK.name -> {
                    binding.socialChipMediaBook.isChecked = true
                    SocialMediaTypeFilter.BOOK
                }
                else -> {
                    binding.socialChipMediaAll.isChecked = true
                    SocialMediaTypeFilter.ALL
                }
            }

            mViewModel.updateConfig {
                it.copy(
                    mediaType = mediaType,
                    libraryId = libraryId,
                    title = if (it.title.isBlank()) mViewModel.getDefaultTitle(requireContext()) else it.title,
                    subtitle = if (it.subtitle.isBlank()) mViewModel.getDefaultSubtitle() else it.subtitle
                )
            }

            if (year > 0) {
                binding.socialChipPeriodYear.isChecked = true
                mViewModel.setPeriodPreset(SocialPeriodFilter.THIS_YEAR, specificYear = year)
            }
        }
    }

    private fun initViews() {
        setupMediaTypeChipColors()

        // Curation Adapter
        mCurationAdapter = SocialShareCurationAdapter { key, isChecked ->
            mViewModel.toggleItemExclusion(key, isChecked)
        }
        binding.socialCurationRecycler.adapter = mCurationAdapter

        // Preview Adapter
        mPreviewAdapter = SocialSharePreviewAdapter { bitmap ->
            showFullScreenPreview(bitmap)
        }
        binding.socialPreviewRecycler.adapter = mPreviewAdapter

        // Curation collapse/expand toggle
        binding.socialCurationHeader.setOnClickListener {
            mIsCurationExpanded = !mIsCurationExpanded
            binding.socialCurationContent.visibility = if (mIsCurationExpanded) View.VISIBLE else View.GONE
            binding.socialCurationArrow.rotation = if (mIsCurationExpanded) 180f else 0f
        }

        binding.socialBtnSelectAll.setOnClickListener {
            mViewModel.selectAllItems(true)
        }

        binding.socialBtnDeselectAll.setOnClickListener {
            mViewModel.selectAllItems(false)
        }

        // Period filter chips
        binding.socialChipGroupPeriod.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.social_chip_period_week -> mViewModel.setPeriodPreset(SocialPeriodFilter.THIS_WEEK)
                R.id.social_chip_period_month -> mViewModel.setPeriodPreset(SocialPeriodFilter.THIS_MONTH)
                R.id.social_chip_period_year -> mViewModel.setPeriodPreset(SocialPeriodFilter.THIS_YEAR)
                R.id.social_chip_period_custom -> openDateRangePicker()
            }
        }

        // Media type chips
        binding.socialChipGroupMedia.setOnCheckedStateChangeListener { _, checkedIds ->
            val type = when (checkedIds.firstOrNull()) {
                R.id.social_chip_media_manga -> SocialMediaTypeFilter.MANGA
                R.id.social_chip_media_book -> SocialMediaTypeFilter.BOOK
                else -> SocialMediaTypeFilter.ALL
            }
            mViewModel.updateConfig { it.copy(mediaType = type) }
            populateLibraryChips(mViewModel.libraries.value ?: emptyList(), type)
        }

        // Library chips
        binding.socialChipGroupLibraries.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            if (checkedId == null || checkedId == R.id.social_chip_library_all) {
                mViewModel.updateConfig { it.copy(libraryId = null) }
            } else {
                val chip = group.findViewById<Chip>(checkedId)
                val libId = chip?.tag as? Long
                mViewModel.updateConfig { it.copy(libraryId = libId) }
            }
        }

        binding.socialSwitchOnlyCompleted.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.updateConfig { it.copy(onlyCompleted = isChecked) }
        }

        // Aspect ratio chips
        binding.socialChipGroupAspect.setOnCheckedStateChangeListener { _, checkedIds ->
            val aspect = when (checkedIds.firstOrNull()) {
                R.id.social_chip_aspect_9_16 -> SocialAspectRatio.STORY_9_16
                R.id.social_chip_aspect_1_1 -> SocialAspectRatio.SQUARE_1_1
                R.id.social_chip_aspect_16_9 -> SocialAspectRatio.TWITTER_16_9
                R.id.social_chip_aspect_2_3 -> SocialAspectRatio.TUMBLR_2_3
                R.id.social_chip_aspect_single_full -> SocialAspectRatio.SINGLE_FULL
                else -> SocialAspectRatio.POST_4_5
            }
            binding.socialInputLayoutCoversPerPage.isEnabled = (aspect != SocialAspectRatio.SINGLE_FULL)
            mViewModel.updateConfig { it.copy(aspectRatio = aspect) }
        }

        // Template chips
        binding.socialChipGroupTemplate.setOnCheckedStateChangeListener { _, checkedIds ->
            val tmpl = when (checkedIds.firstOrNull()) {
                R.id.social_chip_template_shelf -> SocialLayoutTemplate.PERSPECTIVE_SHELF
                R.id.social_chip_template_wrapped -> SocialLayoutTemplate.WRAPPED_STATS
                R.id.social_chip_template_list -> SocialLayoutTemplate.MINIMALIST_LIST
                R.id.social_chip_template_magazine -> SocialLayoutTemplate.MAGAZINE_HERO
                R.id.social_chip_template_polaroid -> SocialLayoutTemplate.POLAROID_GALLERY
                R.id.social_chip_template_podium -> SocialLayoutTemplate.PODIUM_RANKING
                R.id.social_chip_template_glass -> SocialLayoutTemplate.GLASS_CARDS
                else -> SocialLayoutTemplate.BENTO_GRID
            }
            mViewModel.updateConfig { it.copy(template = tmpl) }
        }

        // Theme chips
        binding.socialChipGroupTheme.setOnCheckedStateChangeListener { _, checkedIds ->
            val thm = when (checkedIds.firstOrNull()) {
                R.id.social_chip_theme_cover_blur -> SocialColorTheme.COVER_BLUR_MOSAIC
                R.id.social_chip_theme_app -> SocialColorTheme.APP_THEME
                R.id.social_chip_theme_dark_slate -> SocialColorTheme.DARK_SLATE
                R.id.social_chip_theme_oled -> SocialColorTheme.OLED_BLACK
                R.id.social_chip_theme_aurora -> SocialColorTheme.AURORA_BOREALIS
                R.id.social_chip_theme_sunset -> SocialColorTheme.SUNSET_SYNTH
                R.id.social_chip_theme_royal_gold -> SocialColorTheme.ROYAL_GOLD
                R.id.social_chip_theme_matcha -> SocialColorTheme.MATCHA_BOTANICAL
                R.id.social_chip_theme_manga_pop -> SocialColorTheme.MANGA_POP
                R.id.social_chip_theme_warm_paper -> SocialColorTheme.WARM_PAPER
                R.id.social_chip_theme_sakura -> SocialColorTheme.SAKURA_DREAM
                R.id.social_chip_theme_nordic -> SocialColorTheme.NORDIC_FROST
                R.id.social_chip_theme_custom -> SocialColorTheme.CUSTOM
                else -> SocialColorTheme.DYNAMIC_AMBIENT
            }
            binding.socialCustomThemeContainer.visibility = if (thm == SocialColorTheme.CUSTOM) View.VISIBLE else View.GONE
            mViewModel.updateConfig { it.copy(colorTheme = thm) }
        }

        // Pattern chips
        binding.socialChipGroupPattern.setOnCheckedStateChangeListener { _, checkedIds ->
            val pattern = when (checkedIds.firstOrNull()) {
                R.id.social_chip_pattern_grid -> SocialBackgroundPattern.GRID
                R.id.social_chip_pattern_dots -> SocialBackgroundPattern.DOTS
                R.id.social_chip_pattern_orbs -> SocialBackgroundPattern.ORBS
                else -> SocialBackgroundPattern.NONE
            }
            mViewModel.updateConfig { it.copy(backgroundPattern = pattern) }
        }

        // Text mode chips (Custom Theme)
        binding.socialChipGroupTextMode.setOnCheckedStateChangeListener { _, checkedIds ->
            val mode = when (checkedIds.firstOrNull()) {
                R.id.social_chip_text_light -> SocialTextColorMode.LIGHT
                R.id.social_chip_text_dark -> SocialTextColorMode.DARK
                else -> SocialTextColorMode.AUTO
            }
            mViewModel.updateConfig { it.copy(customTextColorMode = mode) }
        }

        // Custom Color Picker Buttons
        binding.socialBtnColorPrimary.setOnClickListener {
            val curr = mViewModel.config.value?.customPrimaryColor ?: Color.parseColor("#0F172A")
            showColorPickerDialog(getString(R.string.social_share_custom_color_primary), curr) { color ->
                mViewModel.updateConfig { it.copy(customPrimaryColor = color) }
            }
        }

        binding.socialBtnColorSecondary.setOnClickListener {
            val curr = mViewModel.config.value?.customSecondaryColor ?: Color.parseColor("#1E1B4B")
            showColorPickerDialog(getString(R.string.social_share_custom_color_secondary), curr) { color ->
                mViewModel.updateConfig { it.copy(customSecondaryColor = color) }
            }
        }

        binding.socialBtnColorAccent.setOnClickListener {
            val curr = mViewModel.config.value?.customAccentColor ?: Color.parseColor("#6366F1")
            showColorPickerDialog(getString(R.string.social_share_custom_color_accent), curr) { color ->
                mViewModel.updateConfig { it.copy(customAccentColor = color) }
            }
        }

        // Text inputs
        binding.socialInputTitle.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                mViewModel.updateConfig { it.copy(title = s?.toString() ?: "") }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.socialInputCoversPerPage.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val count = s?.toString()?.toIntOrNull()
                if (count != null && count > 0) {
                    mViewModel.updateConfig { it.copy(maxCovers = count.coerceIn(1, 100)) }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.socialInputSubtitle.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                mViewModel.updateConfig { it.copy(subtitle = s?.toString() ?: "") }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.socialInputHandle.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val handle = s?.toString() ?: ""
                mViewModel.saveUserHandle(handle)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Metrics toggles
        binding.socialChipMetricTime.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.updateConfig { it.copy(showReadingTime = isChecked) }
        }
        binding.socialChipMetricPages.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.updateConfig { it.copy(showPagesRead = isChecked) }
        }
        binding.socialChipMetricTitles.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.updateConfig { it.copy(showTitles = isChecked) }
        }
        binding.socialChipMetricProgress.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.updateConfig { it.copy(showProgressStatus = isChecked) }
        }

        binding.socialCheckboxCardBlur.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.updateConfig { it.copy(useCardTextBlur = isChecked) }
        }

        // Share button
        binding.socialBtnShare.setOnClickListener {
            exportAndShare()
        }

        // Save button
        binding.socialBtnSave.setOnClickListener {
            exportAndSave()
        }
    }

    private fun observeViewModel() {
        mViewModel.config.observe(viewLifecycleOwner) { config ->
            if (!binding.socialInputTitle.hasFocus() && binding.socialInputTitle.text.toString() != config.title && config.title.isNotBlank()) {
                binding.socialInputTitle.setText(config.title)
            }
            if (!binding.socialInputSubtitle.hasFocus() && binding.socialInputSubtitle.text.toString() != config.subtitle && config.subtitle.isNotBlank()) {
                binding.socialInputSubtitle.setText(config.subtitle)
            }
            if (!binding.socialInputHandle.hasFocus() && binding.socialInputHandle.text.toString() != config.userHandle && config.userHandle.isNotBlank()) {
                binding.socialInputHandle.setText(config.userHandle)
            }

            val all = mViewModel.allReadings.value ?: emptyList()
            val excluded = config.excludedItemKeys
            val includedCount = (all.size - excluded.size).coerceAtLeast(0)
            binding.socialCurationTitleText.text = getString(R.string.social_share_curation_title, includedCount, all.size)
            mCurationAdapter.updateExcludedKeys(excluded)

            val hexP = String.format("#%06X", 0xFFFFFF and config.customPrimaryColor)
            val hexS = String.format("#%06X", 0xFFFFFF and config.customSecondaryColor)
            val hexA = String.format("#%06X", 0xFFFFFF and config.customAccentColor)
            binding.socialBtnColorPrimary.text = "${getString(R.string.social_share_custom_color_primary)}\n$hexP"
            binding.socialBtnColorSecondary.text = "${getString(R.string.social_share_custom_color_secondary)}\n$hexS"
            binding.socialBtnColorAccent.text = "${getString(R.string.social_share_custom_color_accent)}\n$hexA"
        }

        mViewModel.allReadings.observe(viewLifecycleOwner) { all ->
            val config = mViewModel.config.value
            val excluded = config?.excludedItemKeys ?: emptySet()
            val includedCount = (all.size - excluded.size).coerceAtLeast(0)
            binding.socialCurationTitleText.text = getString(R.string.social_share_curation_title, includedCount, all.size)
            mCurationAdapter.updateData(all, excluded)

            val isEmpty = all.isEmpty()
            binding.socialPreviewEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.socialPreviewRecycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        mViewModel.libraries.observe(viewLifecycleOwner) { libs ->
            val currentMediaType = mViewModel.config.value?.mediaType ?: SocialMediaTypeFilter.ALL
            populateLibraryChips(libs, currentMediaType)
        }

        mViewModel.previewBitmaps.observe(viewLifecycleOwner) { bitmaps ->
            mPreviewAdapter.updateBitmaps(bitmaps)
            binding.socialPreviewSectionTitle.text = if (bitmaps.size > 1) {
                getString(R.string.social_share_preview_pages_count, bitmaps.size)
            } else {
                getString(R.string.social_share_section_preview)
            }
        }

        mViewModel.isRendering.observe(viewLifecycleOwner) { isRendering ->
            binding.socialPreviewProgress.visibility = if (isRendering) View.VISIBLE else View.GONE
        }
    }

    private fun setupMediaTypeChipColors() {
        val context = requireContext()
        val colorPrimary = MaterialColors.getColor(binding.root, androidx.appcompat.R.attr.colorPrimary)
        val colorOnPrimary = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnPrimary)
        val colorOutline = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOutline)
        val colorOnSurface = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnSurface)

        val indigo = ContextCompat.getColor(context, R.color.card_manga_indigo_base)
        val amber = ContextCompat.getColor(context, R.color.card_book_amber_base)

        fun applyChipColors(chip: Chip, checkedBgColor: Int, checkedTextColor: Int, uncheckedBorderColor: Int) {
            val bgStates = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(checkedBgColor, Color.TRANSPARENT)
            )
            val textStates = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(checkedTextColor, colorOnSurface)
            )
            val strokeStates = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(checkedBgColor, uncheckedBorderColor)
            )
            chip.chipBackgroundColor = bgStates
            chip.setTextColor(textStates)
            chip.chipStrokeColor = strokeStates
            chip.chipStrokeWidth = 1.2f * resources.displayMetrics.density
            chip.checkedIconTint = ColorStateList.valueOf(checkedTextColor)
        }

        applyChipColors(binding.socialChipMediaAll, colorPrimary, colorOnPrimary, colorOutline)
        applyChipColors(binding.socialChipMediaManga, indigo, Color.WHITE, ColorUtils.setAlphaComponent(indigo, 120))
        applyChipColors(binding.socialChipMediaBook, amber, Color.WHITE, ColorUtils.setAlphaComponent(amber, 120))
    }

    private fun populateLibraryChips(libraries: List<Library>, mediaType: SocialMediaTypeFilter) {
        val chipGroup = binding.socialChipGroupLibraries

        chipGroup.setOnCheckedStateChangeListener(null)

        while (chipGroup.childCount > 1) {
            chipGroup.removeViewAt(1)
        }

        val filteredLibraries = libraries.filter { lib ->
            when (mediaType) {
                SocialMediaTypeFilter.MANGA -> lib.type == Type.MANGA
                SocialMediaTypeFilter.BOOK -> lib.type == Type.BOOK
                SocialMediaTypeFilter.ALL -> true
            }
        }

        val currentSelectedLibraryId = mViewModel.config.value?.libraryId
        var selectedChipId: Int = R.id.social_chip_library_all

        for (lib in filteredLibraries) {
            val chip = layoutInflater.inflate(R.layout.social_share_chip_filter, chipGroup, false) as Chip
            chip.id = View.generateViewId()
            chip.text = lib.title
            chip.tag = lib.id
            chipGroup.addView(chip)

            if (currentSelectedLibraryId != null && lib.id == currentSelectedLibraryId) {
                selectedChipId = chip.id
            }
        }

        chipGroup.check(selectedChipId)

        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            if (checkedId == null || checkedId == R.id.social_chip_library_all) {
                mViewModel.updateConfig { it.copy(libraryId = null) }
            } else {
                val chip = group.findViewById<Chip>(checkedId)
                val libId = chip?.tag as? Long
                mViewModel.updateConfig { it.copy(libraryId = libId) }
            }
        }

        if (selectedChipId == R.id.social_chip_library_all && currentSelectedLibraryId != null) {
            mViewModel.updateConfig { it.copy(libraryId = null) }
        }
    }

    private fun openDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(R.string.social_share_period_custom)
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            if (selection != null && selection.first != null && selection.second != null) {
                val start = Instant.ofEpochMilli(selection.first).atZone(ZoneId.systemDefault()).toLocalDate()
                val end = Instant.ofEpochMilli(selection.second).atZone(ZoneId.systemDefault()).toLocalDate()

                mViewModel.updateConfig {
                    it.copy(
                        periodFilter = SocialPeriodFilter.CUSTOM_RANGE,
                        startDate = start,
                        endDate = end
                    )
                }
            }
        }
        picker.show(parentFragmentManager, "SocialDateRangePicker")
    }

    private fun showFullScreenPreview(bitmap: Bitmap) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val imageView = ImageView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener { dialog.dismiss() }
        }
        dialog.setContentView(imageView)
        dialog.show()
    }

    private fun exportAndShare() {
        lifecycleScope.launch {
            binding.socialPreviewProgress.visibility = View.VISIBLE
            val exportBitmaps = mViewModel.generateExportBitmaps()
            binding.socialPreviewProgress.visibility = View.GONE
            if (exportBitmaps.isNotEmpty()) {
                val fileName = "bilingual_recap_${System.currentTimeMillis()}"
                SocialImageGenerator.instance.shareImages(requireContext(), exportBitmaps, fileName)
            }
        }
    }

    private fun exportAndSave() {
        lifecycleScope.launch {
            binding.socialPreviewProgress.visibility = View.VISIBLE
            val exportBitmaps = mViewModel.generateExportBitmaps()
            binding.socialPreviewProgress.visibility = View.GONE
            if (exportBitmaps.isNotEmpty()) {
                val fileName = "bilingual_recap_${System.currentTimeMillis()}"
                val savedCount = SocialImageGenerator.instance.saveImagesToGallery(requireContext(), exportBitmaps, fileName)
                if (savedCount > 0) {
                    Toast.makeText(requireContext(), R.string.social_share_saved_success, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), R.string.social_share_saved_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showColorPickerDialog(title: String, initialColor: Int, onColorSelected: (Int) -> Unit) {
        val presetColors = intArrayOf(
            Color.parseColor("#0F172A"), Color.parseColor("#1E1B4B"), Color.parseColor("#064E3B"),
            Color.parseColor("#831843"), Color.parseColor("#090D16"), Color.parseColor("#14261C"),
            Color.parseColor("#000000"), Color.parseColor("#18181B"), Color.parseColor("#6366F1"),
            Color.parseColor("#38BDF8"), Color.parseColor("#34D399"), Color.parseColor("#F59E0B"),
            Color.parseColor("#FB923C"), Color.parseColor("#F43F5E"), Color.parseColor("#F8FAFC"),
            Color.parseColor("#FFF1F2"), Color.parseColor("#F0F9FF"), Color.parseColor("#FBF8F1")
        )
        val hexStr = String.format("#%06X", 0xFFFFFF and initialColor)
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        val input = EditText(context).apply {
            setText(hexStr)
            setSelection(hexStr.length)
            hint = "#RRGGBB"
        }
        val presetScrollView = android.widget.HorizontalScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (10 * resources.displayMetrics.density).toInt()
            }
        }
        val presetRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val swatchSize = (36 * resources.displayMetrics.density).toInt()
        val swatchMargin = (4 * resources.displayMetrics.density).toInt()
        for (col in presetColors) {
            val swatch = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(swatchSize, swatchSize).apply {
                    setMargins(swatchMargin, 0, swatchMargin, 0)
                }
                setBackgroundColor(col)
                setOnClickListener {
                    val code = String.format("#%06X", 0xFFFFFF and col)
                    input.setText(code)
                    input.setSelection(code.length)
                }
            }
            presetRow.addView(swatch)
        }
        presetScrollView.addView(presetRow)
        layout.addView(input)
        layout.addView(presetScrollView)

        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val text = input.text.toString().trim()
                try {
                    val parsed = Color.parseColor(if (text.startsWith("#")) text else "#$text")
                    onColorSelected(parsed)
                } catch (_: Exception) {
                    Toast.makeText(context, "Cor inválida", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

