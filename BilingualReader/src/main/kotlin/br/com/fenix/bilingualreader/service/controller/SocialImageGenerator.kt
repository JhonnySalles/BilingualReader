package br.com.fenix.bilingualreader.service.controller

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.palette.graphics.Palette
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.HistoryStatistics
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.SocialShareConfig
import br.com.fenix.bilingualreader.model.enums.SocialAspectRatio
import br.com.fenix.bilingualreader.model.enums.SocialColorTheme
import br.com.fenix.bilingualreader.model.enums.SocialLayoutTemplate
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class SocialImageGenerator private constructor() {

    private val mLOGGER = LoggerFactory.getLogger(SocialImageGenerator::class.java)

    companion object {
        val instance by lazy { SocialImageGenerator() }
    }

    private data class ItemRenderData(
        val item: HistoryStatistics,
        val cover: Bitmap?
    )

    private data class ThemeColors(
        val bgPrimary: Int,
        val bgSecondary: Int,
        val accent: Int,
        val cardBg: Int,
        val cardBorder: Int,
        val textPrimary: Int,
        val textSecondary: Int,
        val isDark: Boolean
    )

    suspend fun generateImage(
        context: Context,
        config: SocialShareConfig,
        items: List<HistoryStatistics>,
        scale: Float = 1.0f
    ): Bitmap = withContext(Dispatchers.Default) {
        val baseWidth = config.aspectRatio.width
        val baseHeight = config.aspectRatio.height

        val targetWidth = max(100, (baseWidth * scale).roundToInt())
        val targetHeight = max(100, (baseHeight * scale).roundToInt())

        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (scale != 1.0f) {
            canvas.scale(scale, scale)
        }

        // Limit items to maxCovers
        val visibleItems = items.take(config.maxCovers)

        // Load covers in IO context
        val renderItems = withContext(Dispatchers.IO) {
            visibleItems.map { stat ->
                val base = stat.base
                val coverBmp = try {
                    if (base is Manga) {
                        MangaImageCoverController.instance.getMangaCover(context, base, isCoverSize = false)
                    } else if (base is Book) {
                        BookImageCoverController.instance.getBookCover(context, base, isCoverSize = false)
                    } else null
                } catch (e: Exception) {
                    mLOGGER.warn("Failed to load cover for item: ${base.title}", e)
                    null
                }
                ItemRenderData(stat, coverBmp)
            }
        }

        // Resolve Theme Colors
        val themeColors = resolveThemeColors(context, config.colorTheme, renderItems.mapNotNull { it.cover })

        // Draw Background
        drawBackground(canvas, baseWidth.toFloat(), baseHeight.toFloat(), config.colorTheme, themeColors)

        // Calculate layout areas
        val padding = baseWidth * 0.05f
        val headerHeight = baseHeight * 0.15f
        val footerHeight = baseHeight * 0.11f

        val headerRect = RectF(padding, padding, baseWidth - padding, padding + headerHeight)
        val footerRect = RectF(padding, baseHeight - padding - footerHeight, baseWidth - padding, baseHeight - padding)
        val contentRect = RectF(padding, headerRect.bottom + (padding * 0.5f), baseWidth - padding, footerRect.top - (padding * 0.5f))

        // Draw Header
        drawHeader(context, canvas, headerRect, config, items, themeColors)

        // Draw Template Content
        when (config.template) {
            SocialLayoutTemplate.BENTO_GRID -> drawBentoGrid(canvas, contentRect, renderItems, config, themeColors)
            SocialLayoutTemplate.PERSPECTIVE_SHELF -> drawPerspectiveShelf(canvas, contentRect, renderItems, config, themeColors)
            SocialLayoutTemplate.WRAPPED_STATS -> drawWrappedStats(canvas, contentRect, renderItems, config, themeColors, items)
            SocialLayoutTemplate.MINIMALIST_LIST -> drawMinimalistList(canvas, contentRect, renderItems, config, themeColors)
        }

        // Draw Footer
        drawFooter(canvas, footerRect, config, themeColors)

        return@withContext bitmap
    }

    private fun resolveThemeColors(
        context: Context,
        theme: SocialColorTheme,
        covers: List<Bitmap>
    ): ThemeColors {
        return when (theme) {
            SocialColorTheme.DYNAMIC_AMBIENT -> {
                var dominant = Color.parseColor("#1E1B4B")
                var vibrant = Color.parseColor("#6366F1")
                if (covers.isNotEmpty()) {
                    try {
                        val palette = Palette.from(covers.first()).generate()
                        dominant = palette.getDarkMutedColor(palette.getDominantColor(dominant))
                        vibrant = palette.getVibrantColor(palette.getLightVibrantColor(vibrant))
                    } catch (_: Exception) {}
                }
                ThemeColors(
                    bgPrimary = dominant,
                    bgSecondary = Color.parseColor("#0F172A"),
                    accent = vibrant,
                    cardBg = Color.argb(160, 255, 255, 255),
                    cardBorder = Color.argb(40, 255, 255, 255),
                    textPrimary = Color.WHITE,
                    textSecondary = Color.parseColor("#CBD5E1"),
                    isDark = true
                )
            }
            SocialColorTheme.APP_THEME -> {
                val pref = GeneralConsts.getSharedPreferences(context)
                val themeStr = pref.getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.name) ?: Themes.ORIGINAL.name
                val currentTheme = try {
                    Themes.valueOf(themeStr)
                } catch (_: Exception) {
                    Themes.ORIGINAL
                }
                val isDark = ThemeUtil.applyThemeMode(context)
                val accent = when (currentTheme) {
                    Themes.BLUE -> Color.parseColor("#2563EB")
                    Themes.OCEAN_BLUE -> Color.parseColor("#0284C7")
                    Themes.GREEN -> Color.parseColor("#16A34A")
                    Themes.FOREST_GREEN -> Color.parseColor("#059669")
                    Themes.PINK -> Color.parseColor("#DB2777")
                    Themes.RED -> Color.parseColor("#DC2626")
                    Themes.BLOOD_RED -> Color.parseColor("#991B1B")
                    else -> Color.parseColor("#6366F1")
                }
                if (isDark) {
                    ThemeColors(
                        bgPrimary = Color.parseColor("#0F172A"),
                        bgSecondary = Color.parseColor("#020617"),
                        accent = accent,
                        cardBg = Color.parseColor("#1E293B"),
                        cardBorder = Color.argb(50, 255, 255, 255),
                        textPrimary = Color.WHITE,
                        textSecondary = Color.parseColor("#94A3B8"),
                        isDark = true
                    )
                } else {
                    ThemeColors(
                        bgPrimary = Color.parseColor("#F8FAFC"),
                        bgSecondary = Color.parseColor("#E2E8F0"),
                        accent = accent,
                        cardBg = Color.WHITE,
                        cardBorder = Color.parseColor("#CBD5E1"),
                        textPrimary = Color.parseColor("#0F172A"),
                        textSecondary = Color.parseColor("#64748B"),
                        isDark = false
                    )
                }
            }
            SocialColorTheme.DARK_SLATE -> ThemeColors(
                bgPrimary = Color.parseColor("#0F172A"),
                bgSecondary = Color.parseColor("#1E293B"),
                accent = Color.parseColor("#38BDF8"),
                cardBg = Color.argb(180, 30, 41, 59),
                cardBorder = Color.argb(50, 255, 255, 255),
                textPrimary = Color.WHITE,
                textSecondary = Color.parseColor("#94A3B8"),
                isDark = true
            )
            SocialColorTheme.OLED_BLACK -> ThemeColors(
                bgPrimary = Color.BLACK,
                bgSecondary = Color.BLACK,
                accent = Color.parseColor("#E2E8F0"),
                cardBg = Color.parseColor("#121212"),
                cardBorder = Color.parseColor("#27272A"),
                textPrimary = Color.WHITE,
                textSecondary = Color.parseColor("#A1A1AA"),
                isDark = true
            )
            SocialColorTheme.WARM_PAPER -> ThemeColors(
                bgPrimary = Color.parseColor("#FBF8F1"),
                bgSecondary = Color.parseColor("#F2ECE1"),
                accent = Color.parseColor("#B45309"),
                cardBg = Color.parseColor("#FFFFFF"),
                cardBorder = Color.parseColor("#E5DDCB"),
                textPrimary = Color.parseColor("#27272A"),
                textSecondary = Color.parseColor("#71717A"),
                isDark = false
            )
            SocialColorTheme.MANGA_POP -> ThemeColors(
                bgPrimary = Color.parseColor("#18181B"),
                bgSecondary = Color.parseColor("#27272A"),
                accent = Color.parseColor("#F43F5E"),
                cardBg = Color.parseColor("#09090B"),
                cardBorder = Color.parseColor("#F43F5E"),
                textPrimary = Color.WHITE,
                textSecondary = Color.parseColor("#FDA4AF"),
                isDark = true
            )
        }
    }

    private fun drawBackground(
        canvas: Canvas,
        width: Float,
        height: Float,
        theme: SocialColorTheme,
        colors: ThemeColors
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (theme == SocialColorTheme.DYNAMIC_AMBIENT) {
            paint.shader = RadialGradient(
                width * 0.3f, height * 0.25f, width * 0.85f,
                intArrayOf(colors.accent, colors.bgPrimary, colors.bgSecondary),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
        } else {
            paint.shader = LinearGradient(
                0f, 0f, 0f, height,
                colors.bgPrimary, colors.bgSecondary,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width, height, paint)
    }

    private fun drawHeader(
        context: Context,
        canvas: Canvas,
        rect: RectF,
        config: SocialShareConfig,
        allStats: List<HistoryStatistics>,
        colors: ThemeColors
    ) {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.textPrimary
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = rect.height() * 0.32f
        }

        val titleText = if (config.title.isNotBlank()) config.title else context.getString(R.string.social_share_title)
        canvas.drawText(titleText, rect.left, rect.top + (rect.height() * 0.35f), textPaint)

        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.textSecondary
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = rect.height() * 0.18f
        }

        val totalPages = allStats.sumOf { it.pagesRead }
        val totalSeconds = allStats.sumOf { it.timeRead }
        val hours = totalSeconds / 3600
        val mins = (totalSeconds % 3600) / 60

        val statsSummary = "${allStats.size} ${context.getString(R.string.social_share_stat_works, allStats.size).substringAfter(" ")} • $totalPages ${context.getString(R.string.social_share_stat_pages, totalPages).substringAfter(" ")} • ${hours}h ${mins}m"
        val subtitleText = if (config.subtitle.isNotBlank()) "${config.subtitle} • $statsSummary" else statsSummary

        canvas.drawText(subtitleText, rect.left, rect.top + (rect.height() * 0.65f), subPaint)

        // Accent underline
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.accent
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(rect.left, rect.bottom, rect.left + (rect.width() * 0.25f), rect.bottom, linePaint)
    }

    private fun drawFooter(
        canvas: Canvas,
        rect: RectF,
        config: SocialShareConfig,
        colors: ThemeColors
    ) {
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.textSecondary
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = rect.height() * 0.3f
        }
        canvas.drawText("Bilingual Reader", rect.left, rect.centerY() + (brandPaint.textSize / 3), brandPaint)

        if (config.userHandle.isNotBlank()) {
            val handlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.accent
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = rect.height() * 0.3f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(config.userHandle, rect.right, rect.centerY() + (handlePaint.textSize / 3), handlePaint)
        }
    }

    private fun drawBentoGrid(
        canvas: Canvas,
        rect: RectF,
        items: List<ItemRenderData>,
        config: SocialShareConfig,
        colors: ThemeColors
    ) {
        if (items.isEmpty()) return

        val count = items.size
        val spacing = rect.width() * 0.025f

        when {
            count == 1 -> {
                drawCoverCard(canvas, rect, items[0], config, colors, isHero = true)
            }
            count in 2..3 -> {
                // Left hero, right vertical split
                val leftWidth = (rect.width() - spacing) * 0.55f
                val rightWidth = rect.width() - spacing - leftWidth
                val leftRect = RectF(rect.left, rect.top, rect.left + leftWidth, rect.bottom)
                drawCoverCard(canvas, leftRect, items[0], config, colors, isHero = true)

                val rightItems = items.drop(1)
                val rightItemHeight = (rect.height() - (spacing * (rightItems.size - 1))) / rightItems.size
                rightItems.forEachIndexed { idx, item ->
                    val top = rect.top + idx * (rightItemHeight + spacing)
                    val cardRect = RectF(leftRect.right + spacing, top, rect.right, top + rightItemHeight)
                    drawCoverCard(canvas, cardRect, item, config, colors, isHero = false)
                }
            }
            count in 4..6 -> {
                // Top hero (large) + bottom row or 2 columns
                val cols = 3
                val rows = 2
                val cardW = (rect.width() - (spacing * (cols - 1))) / cols
                val cardH = (rect.height() - (spacing * (rows - 1))) / rows

                items.forEachIndexed { idx, item ->
                    val c = idx % cols
                    val r = idx / cols
                    val left = rect.left + c * (cardW + spacing)
                    val top = rect.top + r * (cardH + spacing)
                    val cardRect = RectF(left, top, left + cardW, top + cardH)
                    drawCoverCard(canvas, cardRect, item, config, colors, isHero = false)
                }
            }
            else -> {
                // 3x3 or 4xN grid
                val cols = if (count > 9) 4 else 3
                val rows = (count + cols - 1) / cols
                val cardW = (rect.width() - (spacing * (cols - 1))) / cols
                val cardH = (rect.height() - (spacing * (rows - 1))) / rows

                items.forEachIndexed { idx, item ->
                    val c = idx % cols
                    val r = idx / cols
                    val left = rect.left + c * (cardW + spacing)
                    val top = rect.top + r * (cardH + spacing)
                    val cardRect = RectF(left, top, left + cardW, top + cardH)
                    drawCoverCard(canvas, cardRect, item, config, colors, isHero = false)
                }
            }
        }
    }

    private fun drawPerspectiveShelf(
        canvas: Canvas,
        rect: RectF,
        items: List<ItemRenderData>,
        config: SocialShareConfig,
        colors: ThemeColors
    ) {
        if (items.isEmpty()) return

        val count = items.size
        val shelfPadding = rect.width() * 0.05f
        val availableW = rect.width() - (shelfPadding * 2)

        val cardW = min(availableW * 0.35f, (availableW / max(1, count - 1)) * 1.5f)
        val cardH = rect.height() * 0.75f
        val stepX = if (count > 1) (availableW - cardW) / (count - 1) else 0f

        val centerY = rect.centerY()

        items.forEachIndexed { idx, item ->
            val left = rect.left + shelfPadding + (idx * stepX)
            val top = centerY - (cardH * 0.5f)
            val cardRect = RectF(left, top, left + cardW, top + cardH)

            canvas.save()
            // Subtle tilt for outer covers
            val tilt = when {
                idx == 0 && count > 1 -> -3f
                idx == count - 1 && count > 1 -> 3f
                else -> 0f
            }
            canvas.rotate(tilt, cardRect.centerX(), cardRect.centerY())
            drawCoverCard(canvas, cardRect, item, config, colors, isHero = false)
            canvas.restore()
        }
    }

    private fun drawWrappedStats(
        canvas: Canvas,
        rect: RectF,
        items: List<ItemRenderData>,
        config: SocialShareConfig,
        colors: ThemeColors,
        allStats: List<HistoryStatistics>
    ) {
        val statCardH = rect.height() * 0.22f
        val statCardRect = RectF(rect.left, rect.top, rect.right, rect.top + statCardH)

        // Draw big summary card
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (colors.isDark) Color.argb(120, 255, 255, 255) else Color.argb(120, 0, 0, 0)
        }
        val radius = 24f
        canvas.drawRoundRect(statCardRect, radius, radius, bgPaint)

        val colW = statCardRect.width() / 3f
        val numPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.accent
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = statCardH * 0.35f
            textAlign = Paint.Align.CENTER
        }
        val lblPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.textSecondary
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = statCardH * 0.16f
            textAlign = Paint.Align.CENTER
        }

        val totalPages = allStats.sumOf { it.pagesRead }
        val totalSecs = allStats.sumOf { it.timeRead }
        val hours = totalSecs / 3600

        // Col 1: Works
        canvas.drawText("${allStats.size}", statCardRect.left + (colW * 0.5f), statCardRect.top + (statCardH * 0.45f), numPaint)
        canvas.drawText("Obras Lidas", statCardRect.left + (colW * 0.5f), statCardRect.top + (statCardH * 0.72f), lblPaint)

        // Col 2: Pages
        canvas.drawText("$totalPages", statCardRect.left + (colW * 1.5f), statCardRect.top + (statCardH * 0.45f), numPaint)
        canvas.drawText("Páginas", statCardRect.left + (colW * 1.5f), statCardRect.top + (statCardH * 0.72f), lblPaint)

        // Col 3: Hours
        canvas.drawText("${hours}h", statCardRect.left + (colW * 2.5f), statCardRect.top + (statCardH * 0.45f), numPaint)
        canvas.drawText("Tempo Total", statCardRect.left + (colW * 2.5f), statCardRect.top + (statCardH * 0.72f), lblPaint)

        // Grid below
        val gridRect = RectF(rect.left, statCardRect.bottom + 20f, rect.right, rect.bottom)
        drawBentoGrid(canvas, gridRect, items, config, colors)
    }

    private fun drawMinimalistList(
        canvas: Canvas,
        rect: RectF,
        items: List<ItemRenderData>,
        config: SocialShareConfig,
        colors: ThemeColors
    ) {
        if (items.isEmpty()) return

        val count = min(items.size, 6)
        val spacing = rect.height() * 0.02f
        val itemH = (rect.height() - (spacing * (count - 1))) / count

        items.take(count).forEachIndexed { idx, item ->
            val top = rect.top + idx * (itemH + spacing)
            val rowRect = RectF(rect.left, top, rect.right, top + itemH)

            // Background row
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (colors.isDark) Color.argb(40, 255, 255, 255) else Color.argb(20, 0, 0, 0)
            }
            canvas.drawRoundRect(rowRect, 16f, 16f, bgPaint)

            // Cover on left
            val coverW = rowRect.height() * 0.7f
            val coverRect = RectF(rowRect.left + 12f, rowRect.top + 8f, rowRect.left + 12f + coverW, rowRect.bottom - 8f)
            drawCoverImageOnly(canvas, coverRect, item.cover, item.item.base.title)

            // Text on right
            val textLeft = coverRect.right + 20f
            val textRight = rowRect.right - 20f
            val base = item.item.base

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.textPrimary
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = rowRect.height() * 0.28f
            }
            val title = if (base.title.isNotBlank()) base.title else base.name
            canvas.drawText(title, textLeft, rowRect.top + (rowRect.height() * 0.42f), titlePaint)

            val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.textSecondary
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = rowRect.height() * 0.2f
            }
            val mins = (item.item.timeRead / 60)
            val meta = "${item.item.pagesRead} págs • ${mins} min"
            canvas.drawText(meta, textLeft, rowRect.top + (rowRect.height() * 0.75f), metaPaint)
        }
    }

    private fun drawCoverCard(
        canvas: Canvas,
        rect: RectF,
        item: ItemRenderData,
        config: SocialShareConfig,
        colors: ThemeColors,
        isHero: Boolean
    ) {
        val radius = 20f

        // Sombra / Card Container
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.cardBg
        }
        canvas.drawRoundRect(rect, radius, radius, cardPaint)

        // Draw Cover
        drawCoverImageOnly(canvas, rect, item.cover, item.item.base.title)

        // Inner border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = colors.cardBorder
            strokeWidth = 3f
        }
        canvas.drawRoundRect(rect, radius, radius, borderPaint)

        // Overlays: Progress / Completed Badge
        if (config.showProgressStatus) {
            val isCompleted = item.item.base.completed || (item.item.base.bookMark >= item.item.base.pages && item.item.base.pages > 0)
            if (isCompleted) {
                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#10B981")
                }
                val badgeRadius = rect.width() * 0.12f
                val badgeX = rect.right - badgeRadius - 12f
                val badgeY = rect.top + badgeRadius + 12f
                canvas.drawCircle(badgeX, badgeY, badgeRadius, badgePaint)

                val checkPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = badgeRadius * 1.2f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("✓", badgeX, badgeY + (checkPaint.textSize / 3), checkPaint)
            }
        }

        // Title and metrics on bottom if enabled
        if (config.showTitles || config.showPagesRead || config.showReadingTime) {
            val gradientH = rect.height() * 0.35f
            val gradRect = RectF(rect.left, rect.bottom - gradientH, rect.right, rect.bottom)
            val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, gradRect.top, 0f, gradRect.bottom, Color.TRANSPARENT, Color.argb(220, 0, 0, 0), Shader.TileMode.CLAMP)
            }

            val path = Path().apply {
                addRoundRect(rect, radius, radius, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(path)
            canvas.drawRect(gradRect, gradPaint)

            val base = item.item.base
            if (config.showTitles) {
                val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = rect.width() * 0.08f
                }
                val displayTitle = if (base.title.isNotBlank()) base.title else base.name
                canvas.drawText(displayTitle, rect.left + 12f, rect.bottom - (rect.width() * 0.12f), titlePaint)
            }

            if (config.showPagesRead || config.showReadingTime) {
                val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#E2E8F0")
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textSize = rect.width() * 0.065f
                }
                val mins = item.item.timeRead / 60
                val statsText = "${item.item.pagesRead}p • ${mins}m"
                canvas.drawText(statsText, rect.left + 12f, rect.bottom - 10f, subPaint)
            }
            canvas.restore()
        }
    }

    private fun drawCoverImageOnly(
        canvas: Canvas,
        rect: RectF,
        cover: Bitmap?,
        fallbackTitle: String
    ) {
        val path = Path().apply {
            addRoundRect(rect, 20f, 20f, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(path)

        if (cover != null && !cover.isRecycled) {
            // Draw center-crop
            val src = Rect(0, 0, cover.width, cover.height)
            val dst = RectF(rect)

            val srcAspect = cover.width.toFloat() / cover.height.toFloat()
            val dstAspect = rect.width() / rect.height()

            val scaledSrc = if (srcAspect > dstAspect) {
                val cropWidth = (cover.height * dstAspect).toInt()
                val left = (cover.width - cropWidth) / 2
                Rect(left, 0, left + cropWidth, cover.height)
            } else {
                val cropHeight = (cover.width / dstAspect).toInt()
                val top = (cover.height - cropHeight) / 2
                Rect(0, top, cover.width, top + cropHeight)
            }

            canvas.drawBitmap(cover, scaledSrc, dst, Paint(Paint.FILTER_BITMAP_FLAG))
        } else {
            // Fallback typographic cover
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#334155")
            }
            canvas.drawRect(rect, bgPaint)

            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = rect.width() * 0.12f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(fallbackTitle.take(12), rect.centerX(), rect.centerY(), textPaint)
        }
        canvas.restore()
    }

    suspend fun saveImageToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BilingualReader")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                }
                return@withContext uri
            }
        } catch (e: Exception) {
            mLOGGER.error("Error saving image to gallery", e)
        }
        return@withContext null
    }

    suspend fun shareImage(context: Context, bitmap: Bitmap, fileName: String) = withContext(Dispatchers.IO) {
        try {
            val cacheFolder = File(GeneralConsts.getCacheDir(context), "social_share").also { if (!it.exists()) it.mkdirs() }
            val file = File(cacheFolder, "$fileName.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = android.content.ClipData.newRawUri("SocialShare", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.social_share_btn_share)))
        } catch (e: Exception) {
            mLOGGER.error("Error sharing image", e)
        }
    }
}

