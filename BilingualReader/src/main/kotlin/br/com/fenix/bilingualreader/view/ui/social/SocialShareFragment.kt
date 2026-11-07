package br.com.fenix.bilingualreader.view.ui.social

import android.app.Dialog
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.databinding.FragmentSocialShareBinding
import br.com.fenix.bilingualreader.model.enums.SocialAspectRatio
import br.com.fenix.bilingualreader.model.enums.SocialColorTheme
import br.com.fenix.bilingualreader.model.enums.SocialLayoutTemplate
import br.com.fenix.bilingualreader.model.enums.SocialMediaTypeFilter
import br.com.fenix.bilingualreader.model.enums.SocialPeriodFilter
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.controller.SocialImageGenerator
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import eightbitlab.com.blurview.GlassSetup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SocialShareFragment : Fragment() {

    private var _binding: FragmentSocialShareBinding? = null
    private val binding get() = _binding!!

    private val mViewModel: SocialShareViewModel by viewModels()
    private lateinit var mCurationAdapter: SocialShareCurationAdapter

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
        (requireActivity() as? MenuActivity)?.setActionBar(binding.socialToolbar)

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
            val libraryId = args.getLong(GeneralConsts.KEYS.OBJECT.SOCIAL_SHARE_LIBRARY, -1L)

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
                    libraryId = if (libraryId > 0) libraryId else null
                )
            }

            if (year > 0) {
                binding.socialChipPeriodYear.isChecked = true
                mViewModel.setPeriodPreset(SocialPeriodFilter.THIS_YEAR, specificYear = year)
            }
        }
    }

    private fun initViews() {
        // Curation Adapter
        mCurationAdapter = SocialShareCurationAdapter { key, isChecked ->
            mViewModel.toggleItemExclusion(key, isChecked)
        }
        binding.socialCurationRecycler.adapter = mCurationAdapter

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
                else -> SocialAspectRatio.POST_4_5
            }
            mViewModel.updateConfig { it.copy(aspectRatio = aspect) }
        }

        // Template chips
        binding.socialChipGroupTemplate.setOnCheckedStateChangeListener { _, checkedIds ->
            val tmpl = when (checkedIds.firstOrNull()) {
                R.id.social_chip_template_shelf -> SocialLayoutTemplate.PERSPECTIVE_SHELF
                R.id.social_chip_template_wrapped -> SocialLayoutTemplate.WRAPPED_STATS
                R.id.social_chip_template_list -> SocialLayoutTemplate.MINIMALIST_LIST
                else -> SocialLayoutTemplate.BENTO_GRID
            }
            mViewModel.updateConfig { it.copy(template = tmpl) }
        }

        // Theme chips
        binding.socialChipGroupTheme.setOnCheckedStateChangeListener { _, checkedIds ->
            val thm = when (checkedIds.firstOrNull()) {
                R.id.social_chip_theme_app -> SocialColorTheme.APP_THEME
                R.id.social_chip_theme_dark_slate -> SocialColorTheme.DARK_SLATE
                R.id.social_chip_theme_oled -> SocialColorTheme.OLED_BLACK
                R.id.social_chip_theme_warm_paper -> SocialColorTheme.WARM_PAPER
                R.id.social_chip_theme_manga_pop -> SocialColorTheme.MANGA_POP
                else -> SocialColorTheme.DYNAMIC_AMBIENT
            }
            mViewModel.updateConfig { it.copy(colorTheme = thm) }
        }

        // Text inputs
        binding.socialInputTitle.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                mViewModel.updateConfig { it.copy(title = s?.toString() ?: "") }
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

        // Tap to expand preview in full screen
        binding.socialPreviewCard.setOnClickListener {
            val bitmap = mViewModel.previewBitmap.value
            if (bitmap != null) {
                showFullScreenPreview(bitmap)
            }
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
        }

        mViewModel.allReadings.observe(viewLifecycleOwner) { all ->
            val config = mViewModel.config.value
            val excluded = config?.excludedItemKeys ?: emptySet()
            val includedCount = all.size - excluded.size
            binding.socialCurationTitleText.text = getString(R.string.social_share_curation_title, includedCount, all.size)
            mCurationAdapter.updateData(all, excluded)

            val isEmpty = all.isEmpty()
            binding.socialPreviewEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.socialPreviewImage.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        mViewModel.previewBitmap.observe(viewLifecycleOwner) { bmp ->
            if (bmp != null) {
                binding.socialPreviewImage.setImageBitmap(bmp)
            }
        }

        mViewModel.isRendering.observe(viewLifecycleOwner) { isRendering ->
            binding.socialPreviewProgress.visibility = if (isRendering) View.VISIBLE else View.GONE
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
            val exportBmp = mViewModel.generateExportBitmap()
            binding.socialPreviewProgress.visibility = View.GONE
            if (exportBmp != null) {
                val fileName = "bilingual_recap_${System.currentTimeMillis()}"
                SocialImageGenerator.instance.shareImage(requireContext(), exportBmp, fileName)
            }
        }
    }

    private fun exportAndSave() {
        lifecycleScope.launch {
            binding.socialPreviewProgress.visibility = View.VISIBLE
            val exportBmp = mViewModel.generateExportBitmap()
            binding.socialPreviewProgress.visibility = View.GONE
            if (exportBmp != null) {
                val fileName = "bilingual_recap_${System.currentTimeMillis()}"
                val uri = SocialImageGenerator.instance.saveImageToGallery(requireContext(), exportBmp, fileName)
                if (uri != null) {
                    Toast.makeText(requireContext(), R.string.social_share_saved_success, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), R.string.social_share_saved_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

