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
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.palette.graphics.Palette
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.HistoryStatistics
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.SocialShareConfig
import br.com.fenix.bilingualreader.model.enums.SocialAspectRatio
import br.com.fenix.bilingualreader.model.enums.SocialBackgroundPattern
import br.com.fenix.bilingualreader.model.enums.SocialColorTheme
import br.com.fenix.bilingualreader.model.enums.SocialLayoutTemplate
import br.com.fenix.bilingualreader.model.enums.SocialTextColorMode
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil
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
    ): Bitmap {
        return generateImages(context, config, items, scale).first()
    }

    suspend fun generateImages(
        context: Context,
        config: SocialShareConfig,
        items: List<HistoryStatistics>,
        scale: Float = 1.0f
    ): List<Bitmap> = withContext(Dispatchers.Default) {
        val allRenderItems = withContext(Dispatchers.IO) {
            items.map { stat ->
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

        val themeColors = resolveThemeColors(context, config, allRenderItems.mapNotNull { it.cover })

        val pages: List<List<ItemRenderData>> = when {
            allRenderItems.isEmpty() -> listOf(emptyList())
            config.aspectRatio == SocialAspectRatio.SINGLE_FULL -> listOf(allRenderItems)
            else -> {
                val perPage = max(1, config.maxCovers)
                allRenderItems.chunked(perPage)
            }
        }

        val totalPages = pages.size

        pages.mapIndexed { pageIndex, pageItems ->
            renderSinglePage(
                context = context,
                config = config,
                pageItems = pageItems,
                allRenderItems = allRenderItems,
                allStats = items,
                themeColors = themeColors,
                pageIndex = pageIndex,
                totalPages = totalPages,
                scale = scale
            )
        }
    }

    private fun renderSinglePage(
        context: Context,
        config: SocialShareConfig,
        pageItems: List<ItemRenderData>,
        allRenderItems: List<ItemRenderData>,
        allStats: List<HistoryStatistics>,
        themeColors: ThemeColors,
        pageIndex: Int,
        totalPages: Int,
        scale: Float
    ): Bitmap {
        val baseWidth = config.aspectRatio.width
        val baseHeight = if (config.aspectRatio == SocialAspectRatio.SINGLE_FULL) {
            calculateSingleFullHeight(baseWidth, pageItems.size, config.template)
        } else {
            config.aspectRatio.height
        }

        val targetWidth = max(100, (baseWidth * scale).roundToInt())
        val targetHeight = max(100, (baseHeight * scale).roundToInt())

        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (scale != 1.0f) {
            canvas.scale(scale, scale)
        }

        val coversForMosaic = if (pageItems.isNotEmpty()) pageItems.mapNotNull { it.cover } else allRenderItems.mapNotNull { it.cover }
        drawBackground(canvas, baseWidth.toFloat(), baseHeight.toFloat(), config, themeColors, coversForMosaic)

        val refHeight = if (config.aspectRatio == SocialAspectRatio.SINGLE_FULL) 1350f else baseHeight.toFloat()
        val padding = baseWidth * 0.05f
        val headerHeight = refHeight * 0.14f
        val footerHeight = refHeight * 0.09f

        val headerRect = RectF(padding, padding, baseWidth - padding, padding + headerHeight)
        val footerRect = RectF(padding, baseHeight - padding - footerHeight, baseWidth - padding, baseHeight - padding)
        val contentRect = RectF(padding, headerRect.bottom + (padding * 0.45f), baseWidth - padding, footerRect.top - (padding * 0.45f))

        drawHeader(context, canvas, headerRect, config, allStats, themeColors)

        when (config.template) {
            SocialLayoutTemplate.BENTO_GRID -> drawBentoGrid(canvas, contentRect, pageItems, config, themeColors)
            SocialLayoutTemplate.PERSPECTIVE_SHELF -> drawPerspectiveShelf(canvas, contentRect, pageItems, config, themeColors)
            SocialLayoutTemplate.WRAPPED_STATS -> drawWrappedStats(canvas, contentRect, pageItems, config, themeColors, allStats)
            SocialLayoutTemplate.MINIMALIST_LIST -> drawMinimalistList(canvas, contentRect, pageItems, config, themeColors)
            SocialLayoutTemplate.MAGAZINE_HERO -> drawMagazineHero(canvas, contentRect, pageItems, config, themeColors)
            SocialLayoutTemplate.POLAROID_GALLERY -> drawPolaroidGallery(canvas, contentRect, pageItems, config, themeColors)
            SocialLayoutTemplate.PODIUM_RANKING -> drawPodiumRanking(canvas, contentRect, pageItems, config, themeColors)
            SocialLayoutTemplate.GLASS_CARDS -> drawGlassCards(canvas, contentRect, pageItems, config, themeColors)
        }

        drawFooter(canvas, footerRect, config, themeColors, pageIndex, totalPages)

        return bitmap
    }

    private fun calculateSingleFullHeight(baseWidth: Int, itemCount: Int, template: SocialLayoutTemplate): Int {
        if (itemCount <= 0) return 1350
        val padding = baseWidth * 0.05f
        val headerAndFooter = (1350f * 0.23f) + (padding * 3f)
        val contentWidth = baseWidth - (padding * 2f)

        val contentHeight = when (template) {
            SocialLayoutTemplate.MINIMALIST_LIST -> {
                val rowH = 140f
                val spacing = 16f
                itemCount * (rowH + spacing)
            }
            SocialLayoutTemplate.GLASS_CARDS -> {
                val rowH = 150f
                val spacing = 18f
                itemCount * (rowH + spacing)
            }
            SocialLayoutTemplate.PERSPECTIVE_SHELF -> {
                val perShelf = 5
                val shelves = ceil(itemCount / perShelf.toFloat()).toInt().coerceAtLeast(1)
                shelves * 420f
            }
            SocialLayoutTemplate.WRAPPED_STATS -> {
                val cols = if (itemCount > 12) 4 else 3
                val rows = ceil(itemCount / cols.toFloat()).toInt().coerceAtLeast(1)
                val spacing = contentWidth * 0.025f
                val cardW = (contentWidth - (spacing * (cols - 1))) / cols
                val cardH = cardW * 1.48f
                240f + (rows * (cardH + spacing))
            }
            SocialLayoutTemplate.BENTO_GRID -> {
                if (itemCount <= 3) {
                    880f
                } else {
                    val cols = if (itemCount > 12) 4 else 3
                    val rows = ceil(itemCount / cols.toFloat()).toInt().coerceAtLeast(1)
                    val spacing = contentWidth * 0.025f
                    val cardW = (contentWidth - (spacing * (cols - 1))) / cols
                    val cardH = cardW * 1.48f
                    rows * (cardH + spacing)
                }
            }
            SocialLayoutTemplate.MAGAZINE_HERO -> {
                if (itemCount <= 1) {
                    800f
                } else {
                    val restCount = itemCount - 1
                    val cols = if (restCount > 8) 4 else 3
                    val rows = ceil(restCount / cols.toFloat()).toInt().coerceAtLeast(1)
                    val spacing = contentWidth * 0.025f
                    val cardW = (contentWidth - (spacing * (cols - 1))) / cols
                    val cardH = cardW * 1.48f
                    520f + (rows * (cardH + spacing))
                }
            }
            SocialLayoutTemplate.POLAROID_GALLERY -> {
                val cols = if (itemCount <= 4) 2 else if (itemCount <= 9) 3 else 4
                val rows = ceil(itemCount / cols.toFloat()).toInt().coerceAtLeast(1)
                val spacing = contentWidth * 0.03f
                val cardW = (contentWidth - (spacing * (cols - 1))) / cols
                val cardH = cardW * 1.35f
                rows * (cardH + spacing)
            }
            SocialLayoutTemplate.PODIUM_RANKING -> {
                val podiumH = 460f
                val restCount = (itemCount - 3).coerceAtLeast(0)
                val restH = restCount * 120f
                podiumH + restH
            }
        }

        return (headerAndFooter + contentHeight).roundToInt().coerceIn(1350, 8000)
    }

    private fun resolveThemeColors(
        context: Context,
        config: SocialShareConfig,
        covers: List<Bitmap>
    ): ThemeColors {
        return when (config.colorTheme) {
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
            SocialColorTheme.COVER_BLUR_MOSAIC -> {
                var accent = Color.parseColor("#6366F1")
                if (covers.isNotEmpty()) {
                    try {
                        val palette = Palette.from(covers.first()).generate()
                        accent = palette.getVibrantColor(palette.getLightVibrantColor(accent))
                    } catch (_: Exception) {}
                }
                ThemeColors(
                    bgPrimary = Color.parseColor("#0B0F19"),
                    bgSecondary = Color.parseColor("#05070B"),
                    accent = accent,
                    cardBg = Color.argb(160, 20, 26, 40),
                    cardBorder = Color.argb(60, 255, 255, 255),
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
            SocialColorTheme.AURORA_BOREALIS -> ThemeColors(
                bgPrimary = Color.parseColor("#064E3B"),
                bgSecondary = Color.parseColor("#0F172A"),
                accent = Color.parseColor("#34D399"),
                cardBg = Color.argb(160, 6, 78, 59),
                cardBorder = Color.argb(60, 52, 211, 153),
                textPrimary = Color.WHITE,
                textSecondary = Color.parseColor("#A7F3D0"),
                isDark = true
            )
            SocialColorTheme.SUNSET_SYNTH -> ThemeColors(
                bgPrimary = Color.parseColor("#2E1065"),
                bgSecondary = Color.parseColor("#831843"),
                accent = Color.parseColor("#FB923C"),
                cardBg = Color.argb(160, 46, 16, 101),
                cardBorder = Color.argb(60, 251, 146, 60),
                textPrimary = Color.WHITE,
                textSecondary = Color.parseColor("#FED7AA"),
                isDark = true
            )
            SocialColorTheme.ROYAL_GOLD -> ThemeColors(
                bgPrimary = Color.parseColor("#090D16"),
                bgSecondary = Color.parseColor("#111827"),
                accent = Color.parseColor("#F59E0B"),
                cardBg = Color.argb(200, 17, 24, 39),
                cardBorder = Color.argb(80, 245, 158, 11),
                textPrimary = Color.WHITE,
                textSecondary = Color.parseColor("#FDE68A"),
                isDark = true
            )
            SocialColorTheme.MATCHA_BOTANICAL -> ThemeColors(
                bgPrimary = Color.parseColor("#14261C"),
                bgSecondary = Color.parseColor("#1E3528"),
                accent = Color.parseColor("#86EFAC"),
                cardBg = Color.argb(170, 20, 38, 28),
                cardBorder = Color.argb(60, 134, 239, 172),
                textPrimary = Color.WHITE,
                textSecondary = Color.parseColor("#D1FAE5"),
                isDark = true
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
            SocialColorTheme.SAKURA_DREAM -> ThemeColors(
                bgPrimary = Color.parseColor("#FFF1F2"),
                bgSecondary = Color.parseColor("#FFE4E6"),
                accent = Color.parseColor("#E11D48"),
                cardBg = Color.WHITE,
                cardBorder = Color.parseColor("#FECDD3"),
                textPrimary = Color.parseColor("#881337"),
                textSecondary = Color.parseColor("#9F1239"),
                isDark = false
            )
            SocialColorTheme.NORDIC_FROST -> ThemeColors(
                bgPrimary = Color.parseColor("#F0F9FF"),
                bgSecondary = Color.parseColor("#E0F2FE"),
                accent = Color.parseColor("#0284C7"),
                cardBg = Color.WHITE,
                cardBorder = Color.parseColor("#BAE6FD"),
                textPrimary = Color.parseColor("#0C4A6E"),
                textSecondary = Color.parseColor("#0369A1"),
                isDark = false
            )
            SocialColorTheme.CUSTOM -> {
                val isDark = when (config.customTextColorMode) {
                    SocialTextColorMode.LIGHT -> true
                    SocialTextColorMode.DARK -> false
                    SocialTextColorMode.AUTO -> {
                        val lum1 = calculateLuminance(config.customPrimaryColor)
                        val lum2 = calculateLuminance(config.customSecondaryColor)
                        ((lum1 + lum2) / 2.0) < 0.5
                    }
                }
                ThemeColors(
                    bgPrimary = config.customPrimaryColor,
                    bgSecondary = config.customSecondaryColor,
                    accent = config.customAccentColor,
                    cardBg = if (isDark) Color.argb(170, 26, 32, 44) else Color.argb(220, 255, 255, 255),
                    cardBorder = if (isDark) Color.argb(55, 255, 255, 255) else Color.argb(60, 0, 0, 0),
                    textPrimary = if (isDark) Color.WHITE else Color.parseColor("#0F172A"),
                    textSecondary = if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#475569"),
                    isDark = isDark
                )
            }
        }
    }

    private fun calculateLuminance(color: Int): Double {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun drawBackground(
        canvas: Canvas,
        width: Float,
        height: Float,
        config: SocialShareConfig,
        colors: ThemeColors,
        covers: List<Bitmap>
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        when (config.colorTheme) {
            SocialColorTheme.DYNAMIC_AMBIENT -> {
                paint.shader = RadialGradient(
                    width * 0.3f, height * 0.25f, max(width, height) * 0.85f,
                    intArrayOf(colors.accent, colors.bgPrimary, colors.bgSecondary),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width, height, paint)
            }
            SocialColorTheme.COVER_BLUR_MOSAIC -> {
                drawCoverBlurMosaicBackground(canvas, width, height, covers)
            }
            SocialColorTheme.AURORA_BOREALIS -> {
                paint.shader = LinearGradient(
                    0f, 0f, width * 0.7f, height,
                    intArrayOf(colors.bgPrimary, Color.parseColor("#042F2E"), colors.bgSecondary),
                    floatArrayOf(0f, 0.4f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width, height, paint)
            }
            SocialColorTheme.SUNSET_SYNTH -> {
                paint.shader = LinearGradient(
                    0f, 0f, 0f, height,
                    intArrayOf(colors.bgPrimary, Color.parseColor("#701A75"), colors.bgSecondary),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width, height, paint)
            }
            else -> {
                paint.shader = LinearGradient(
                    0f, 0f, 0f, height,
                    colors.bgPrimary, colors.bgSecondary,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width, height, paint)
            }
        }

        drawBackgroundPattern(canvas, width, height, config.backgroundPattern, colors)
    }

    private fun drawCoverBlurMosaicBackground(
        canvas: Canvas,
        width: Float,
        height: Float,
        covers: List<Bitmap>
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0B0F19") }
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        if (covers.isNotEmpty()) {
            val smallW = 120
            val smallH = (120f * (height / width)).roundToInt().coerceIn(120, 240)
            val smallBmp = Bitmap.createBitmap(smallW, smallH, Bitmap.Config.ARGB_8888)
            val smallCanvas = Canvas(smallBmp)

            val cols = if (covers.size >= 4) 2 else 1
            val rows = ceil(covers.size / cols.toFloat()).toInt().coerceAtLeast(1)
            val cellW = smallW.toFloat() / cols
            val cellH = smallH.toFloat() / rows

            covers.take(6).forEachIndexed { idx, cover ->
                val c = idx % cols
                val r = idx / cols
                val rect = RectF(c * cellW, r * cellH, (c + 1) * cellW, (r + 1) * cellH)
                drawCenterCropBitmap(smallCanvas, rect, cover)
            }

            fastBoxBlur(smallBmp, radius = 5, iterations = 3)

            val filterPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(smallBmp, null, RectF(0f, 0f, width, height), filterPaint)
            smallBmp.recycle()
        }

        val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width * 0.5f, height * 0.5f, max(width, height) * 0.8f,
                intArrayOf(Color.argb(120, 11, 15, 25), Color.argb(235, 5, 7, 11)),
                floatArrayOf(0.2f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width, height, overlayPaint)
    }

    private fun drawBackgroundPattern(
        canvas: Canvas,
        width: Float,
        height: Float,
        pattern: SocialBackgroundPattern,
        colors: ThemeColors
    ) {
        when (pattern) {
            SocialBackgroundPattern.NONE -> {}
            SocialBackgroundPattern.GRID -> {
                val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (colors.isDark) Color.argb(18, 255, 255, 255) else Color.argb(14, 0, 0, 0)
                    strokeWidth = 1.5f
                }
                val step = 64f
                var x = 0f
                while (x <= width) {
                    canvas.drawLine(x, 0f, x, height, gridPaint)
                    x += step
                }
                var y = 0f
                while (y <= height) {
                    canvas.drawLine(0f, y, width, y, gridPaint)
                    y += step
                }
            }
            SocialBackgroundPattern.DOTS -> {
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (colors.isDark) Color.argb(26, 255, 255, 255) else Color.argb(20, 0, 0, 0)
                }
                val step = 48f
                val radius = 2.5f
                var x = step / 2
                while (x <= width) {
                    var y = step / 2
                    while (y <= height) {
                        canvas.drawCircle(x, y, radius, dotPaint)
                        y += step
                    }
                    x += step
                }
            }
            SocialBackgroundPattern.ORBS -> {
                val orb1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        width * 0.85f, height * 0.15f, width * 0.5f,
                        intArrayOf(Color.argb(55, Color.red(colors.accent), Color.green(colors.accent), Color.blue(colors.accent)), Color.TRANSPARENT),
                        floatArrayOf(0f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawCircle(width * 0.85f, height * 0.15f, width * 0.5f, orb1Paint)

                val orb2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        width * 0.15f, height * 0.85f, width * 0.55f,
                        intArrayOf(Color.argb(45, Color.red(colors.bgSecondary), Color.green(colors.bgSecondary), Color.blue(colors.bgSecondary)), Color.TRANSPARENT),
                        floatArrayOf(0f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawCircle(width * 0.15f, height * 0.85f, width * 0.55f, orb2Paint)
            }
        }
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

        val titleText = if (config.title.isNotBlank()) {
            config.title
        } else {
            val months = context.resources.getStringArray(R.array.mouth_descriptions)
            val monthIndex = config.startDate.monthValue - 1
            val monthName = if (monthIndex in months.indices) months[monthIndex] else config.startDate.month.name
            context.getString(R.string.social_share_default_title_month, monthName)
        }
        val ellipsizedTitle = TextUtils.ellipsize(titleText, textPaint, rect.width(), TextUtils.TruncateAt.END).toString()
        canvas.drawText(ellipsizedTitle, rect.left, rect.top + (rect.height() * 0.35f), textPaint)

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
        val ellipsizedSub = TextUtils.ellipsize(subtitleText, subPaint, rect.width(), TextUtils.TruncateAt.END).toString()

        canvas.drawText(ellipsizedSub, rect.left, rect.top + (rect.height() * 0.65f), subPaint)

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
        colors: ThemeColors,
        pageIndex: Int,
        totalPages: Int
    ) {
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.textSecondary
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = rect.height() * 0.32f
        }
        val brandText = if (totalPages > 1) {
            "Bilingual Reader  •  ${pageIndex + 1}/$totalPages"
        } else {
            "Bilingual Reader"
        }
        canvas.drawText(brandText, rect.left, rect.centerY() + (brandPaint.textSize / 3), brandPaint)

        if (config.userHandle.isNotBlank()) {
            val handlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.accent
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = rect.height() * 0.32f
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
                drawCoverCard(canvas, rect, items[0], config, colors)
            }
            count in 2..3 && config.aspectRatio != SocialAspectRatio.SINGLE_FULL -> {
                val leftWidth = (rect.width() - spacing) * 0.55f
                val leftRect = RectF(rect.left, rect.top, rect.left + leftWidth, rect.bottom)
                drawCoverCard(canvas, leftRect, items[0], config, colors)

                val rightItems = items.drop(1)
                val rightItemHeight = (rect.height() - (spacing * (rightItems.size - 1))) / rightItems.size
                rightItems.forEachIndexed { idx, item ->
                    val top = rect.top + idx * (rightItemHeight + spacing)
                    val cardRect = RectF(leftRect.right + spacing, top, rect.right, top + rightItemHeight)
                    drawCoverCard(canvas, cardRect, item, config, colors)
                }
            }
            else -> {
                val cols = when {
                    count == 2 -> 2
                    count == 4 -> 2
                    count > 12 -> 4
                    else -> 3
                }
                val rows = (count + cols - 1) / cols
                val cardW = (rect.width() - (spacing * (cols - 1))) / cols
                val cardH = (rect.height() - (spacing * (rows - 1))) / rows

                items.forEachIndexed { idx, item ->
                    val c = idx % cols
                    val r = idx / cols
                    val left = rect.left + c * (cardW + spacing)
                    val top = rect.top + r * (cardH + spacing)
                    val cardRect = RectF(left, top, left + cardW, top + cardH)
                    drawCoverCard(canvas, cardRect, item, config, colors)
                }
            }
        }
    }

    private fun drawMagazineHero(
        canvas: Canvas,
        rect: RectF,
        items: List<ItemRenderData>,
        config: SocialShareConfig,
        colors: ThemeColors
    ) {
        if (items.isEmpty()) return
        val count = items.size
        val spacing = rect.width() * 0.025f

        if (count == 1) {
            drawCoverCard(canvas, rect, items[0], config, colors)
            drawHeroBadge(canvas, rect, colors)
            return
        }

        val heroWidth = (rect.width() - spacing) * 0.55f
        val heroRect = RectF(rect.left, rect.top, rect.left + heroWidth, rect.bottom)
        drawCoverCard(canvas, heroRect, items[0], config, colors)
        drawHeroBadge(canvas, heroRect, colors)

        val rest = items.drop(1)
        val rightRect = RectF(heroRect.right + spacing, rect.top, rect.right, rect.bottom)
        val cols = if (rest.size > 4) 2 else 1
        val rows = ceil(rest.size / cols.toFloat()).toInt().coerceAtLeast(1)
        val cardW = (rightRect.width() - (spacing * (cols - 1))) / cols
        val cardH = (rightRect.height() - (spacing * (rows - 1))) / rows

        rest.forEachIndexed { idx, item ->
            val c = idx % cols
            val r = idx / cols
            val left = rightRect.left + c * (cardW + spacing)
            val top = rightRect.top + r * (cardH + spacing)
            val cardRect = RectF(left, top, left + cardW, top + cardH)
            drawCoverCard(canvas, cardRect, item, config, colors)
        }
    }

    private fun drawHeroBadge(canvas: Canvas, cardRect: RectF, colors: ThemeColors) {
        val badgeW = (cardRect.width() * 0.44f).coerceIn(100f, 180f)
        val badgeH = (cardRect.height() * 0.065f).coerceIn(26f, 44f)
        val badgeRect = RectF(cardRect.left + 14f, cardRect.top + 14f, cardRect.left + 14f + badgeW, cardRect.top + 14f + badgeH)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.accent }
        canvas.drawRoundRect(badgeRect, badgeH * 0.5f, badgeH * 0.5f, bgPaint)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = badgeH * 0.48f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("★ #1 TOP", badgeRect.centerX(), badgeRect.centerY() + (textPaint.textSize * 0.35f), textPaint)
    }

    private fun drawPolaroidGallery(
        canvas: Canvas,
        rect: RectF,
        items: List<ItemRenderData>,
        config: SocialShareConfig,
        colors: ThemeColors
    ) {
        if (items.isEmpty()) return
        val count = items.size
        val spacing = rect.width() * 0.035f

        val cols = if (count <= 4) 2 else if (count <= 9) 3 else 4
        val rows = ceil(count / cols.toFloat()).toInt().coerceAtLeast(1)
        val cardW = (rect.width() - (spacing * (cols - 1))) / cols
        val cardH = (rect.height() - (spacing * (rows - 1))) / rows

        val tilts = floatArrayOf(-2.2f, 1.8f, -1.4f, 2.3f, -1.9f, 1.5f)

        items.forEachIndexed { idx, item ->
            val c = idx % cols
            val r = idx / cols
            val left = rect.left + c * (cardW + spacing)
            val top = rect.top + r * (cardH + spacing)
            val cardRect = RectF(left, top, left + cardW, top + cardH)

            val tilt = tilts[idx % tilts.size]
            canvas.save()
            canvas.rotate(tilt, cardRect.centerX(), cardRect.centerY())
            drawPolaroidCard(canvas, cardRect, item, config, colors)
            canvas.restore()
        }
    }

    private fun drawPolaroidCard(
        canvas: Canvas,
        rect: RectF,
        item: ItemRenderData,
        config: SocialShareConfig,
        colors: ThemeColors
    ) {
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 0, 0, 0)
        }
        val shadowRect = RectF(rect.left + 4f, rect.top + 6f, rect.right + 4f, rect.bottom + 6f)
        canvas.drawRoundRect(shadowRect, 10f, 10f, shadowPaint)

        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (colors.isDark) Color.parseColor("#F8FAFC") else Color.WHITE
        }
        canvas.drawRoundRect(rect, 10f, 10f, framePaint)

        val innerPadding = rect.width() * 0.06f
        val photoH = rect.height() * 0.72f
        val photoRect = RectF(rect.left + innerPadding, rect.top + innerPadding, rect.right - innerPadding, rect.top + innerPadding + photoH)

        drawCoverImageOnly(canvas, photoRect, item.cover, item.item.base.title)

        val legendTop = photoRect.bottom + (innerPadding * 0.5f)
        val legendW = max(30f, rect.width() - (innerPadding * 2f))

        val base = item.item.base
        val title = if (base.title.isNotBlank()) base.title else base.name
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = (rect.width() * 0.082f).coerceIn(12f, 26f)
        }
        val ellipTitle = TextUtils.ellipsize(title, titlePaint, legendW, TextUtils.TruncateAt.END).toString()
        canvas.drawText(ellipTitle, rect.left + innerPadding, legendTop + titlePaint.textSize, titlePaint)

        val metricsText = formatCardMetrics(item.item, config)
        if (metricsText.isNotBlank()) {
            val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = (rect.width() * 0.065f).coerceIn(10f, 20f)
            }
            val ellipMetrics = TextUtils.ellipsize(metricsText, subPaint, legendW, TextUtils.TruncateAt.END).toString()
            canvas.drawText(ellipMetrics, rect.left + innerPadding, rect.bottom - (innerPadding * 0.8f), subPaint)
        }
    }

    private fun drawPodiumRanking(
        canvas: Canvas,
        rect: RectF,
        items: List<ItemRenderData>,
        config: SocialShareConfig,
        colors: ThemeColors
    ) {
        if (items.isEmpty()) return
        val count = items.size

        val top3Count = min(3, count)
        val hasRest = count > 3

        val podiumHeight = if (hasRest) rect.height() * 0.54f else rect.height()
        val podiumRect = RectF(rect.left, rect.top, rect.right, rect.top + podiumHeight)

        val podW = (podiumRect.width() * 0.29f)
        val spacing = (podiumRect.width() - (podW * 3f)) / 4f

        val podiumOrder = if (top3Count == 3) listOf(1, 0, 2) else (0 until top3Count).toList()
        val baseH1 = podiumRect.height() * 0.90f
        val heights = mapOf(0 to baseH1, 1 to baseH1 * 0.82f, 2 to baseH1 * 0.72f)
        val medalColors = mapOf(0 to Color.parseColor("#FBBF24"), 1 to Color.parseColor("#94A3B8"), 2 to Color.parseColor("#D97706"))
        val medalLabels = mapOf(0 to "1", 1 to "2", 2 to "3")

        podiumOrder.forEachIndexed { colIdx, itemIdx ->
            val item = items[itemIdx]
            val cardH = heights[itemIdx] ?: baseH1
            val left = podiumRect.left + spacing + (colIdx * (podW + spacing))
            val bottom = podiumRect.bottom
            val top = bottom - cardH
            val cardRect = RectF(left, top, left + podW, bottom)

            drawCoverCard(canvas, cardRect, item, config, colors)

            val badgeColor = medalColors[itemIdx] ?: colors.accent
            val badgeLabel = medalLabels[itemIdx] ?: "${itemIdx + 1}"
            val badgeRadius = (podW * 0.16f).coerceIn(16f, 32f)
            val badgeX = cardRect.centerX()
            val badgeY = cardRect.top

            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badgeColor }
            canvas.drawCircle(badgeX, badgeY, badgeRadius, badgePaint)

            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = badgeRadius * 1.25f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(badgeLabel, badgeX, badgeY + (textPaint.textSize * 0.36f), textPaint)
        }

        if (hasRest) {
            val rest = items.drop(3)
            val restTop = podiumRect.bottom + 20f
            val restRect = RectF(rect.left, restTop, rect.right, rect.bottom)
            drawRankedCompactList(canvas, restRect, rest, 4, config, colors)
        }
    }

    private fun drawRankedCompactList(
        canvas: Canvas,
        rect: RectF,
        items: List<ItemRenderData>,
        startRank: Int,
        config: SocialShareConfig,
        colors: ThemeColors
    ) {
        val count = items.size
        val spacing = min(rect.height() * 0.02f, 16f)
        val itemH = (rect.height() - (spacing * (count - 1))) / count

        items.forEachIndexed { idx, item ->
            val top = rect.top + idx * (itemH + spacing)
            val rowRect = RectF(rect.left, top, rect.right, top + itemH)

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (colors.isDark) Color.argb(45, 255, 255, 255) else Color.argb(20, 0, 0, 0)
            }
            canvas.drawRoundRect(rowRect, 14f, 14f, bgPaint)

            val rankBadgeW = rowRect.height() * 0.7f
            val rankPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.accent
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = rowRect.height() * 0.38f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("#${startRank + idx}", rowRect.left + (rankBadgeW * 0.5f), rowRect.centerY() + (rankPaint.textSize * 0.35f), rankPaint)

            val coverW = rowRect.height() * 0.68f
            val coverRect = RectF(rowRect.left + rankBadgeW, rowRect.top + 6f, rowRect.left + rankBadgeW + coverW, rowRect.bottom - 6f)
            drawCoverImageOnly(canvas, coverRect, item.cover, item.item.base.title)

            val textLeft = coverRect.right + 16f
            val availTextW = max(30f, rowRect.right - textLeft - 16f)
            val base = item.item.base

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.textPrimary
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = min(rowRect.height() * 0.30f, 32f)
            }
            val title = TextUtils.ellipsize(if (base.title.isNotBlank()) base.title else base.name, titlePaint, availTextW, TextUtils.TruncateAt.END).toString()
            canvas.drawText(title, textLeft, rowRect.top + (rowRect.height() * 0.44f), titlePaint)

            val meta = formatCardMetrics(item.item, config)
            if (meta.isNotBlank()) {
                val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colors.textSecondary
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textSize = min(rowRect.height() * 0.22f, 22f)
                }
                canvas.drawText(meta, textLeft, rowRect.top + (rowRect.height() * 0.78f), metaPaint)
            }
        }
    }

    private fun drawGlassCards(
        canvas: Canvas,
        rect: RectF,
        items: List<ItemRenderData>,
        config: SocialShareConfig,
        colors: ThemeColors
    ) {
        if (items.isEmpty()) return
        val count = items.size
        val spacing = min(rect.height() * 0.025f, 20f)
        val itemH = (rect.height() - (spacing * (count - 1))) / count

        items.forEachIndexed { idx, item ->
            val top = rect.top + idx * (itemH + spacing)
            val cardRect = RectF(rect.left, top, rect.right, top + itemH)

            val cardPath = Path().apply { addRoundRect(cardRect, 18f, 18f, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(cardPath)

            if (item.cover != null) {
                drawBlurredCardFooter(canvas, cardRect, cardRect, item.cover)
            }
            val frostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (colors.isDark) Color.argb(140, 15, 23, 42) else Color.argb(190, 255, 255, 255)
            }
            canvas.drawRect(cardRect, frostPaint)

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = colors.cardBorder
                strokeWidth = 2f
            }
            canvas.drawRoundRect(cardRect, 18f, 18f, borderPaint)

            val coverW = cardRect.height() * 0.70f
            val coverRect = RectF(cardRect.left + 14f, cardRect.top + 10f, cardRect.left + 14f + coverW, cardRect.bottom - 10f)
            drawCoverImageOnly(canvas, coverRect, item.cover, item.item.base.title)

            val textLeft = coverRect.right + 18f
            val availW = max(40f, cardRect.right - textLeft - 18f)
            val base = item.item.base

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.textPrimary
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = min(cardRect.height() * 0.25f, 32f)
            }
            val rawTitle = if (base.title.isNotBlank()) base.title else base.name
            val title = TextUtils.ellipsize(rawTitle, titlePaint, availW, TextUtils.TruncateAt.END).toString()
            canvas.drawText(title, textLeft, cardRect.top + (cardRect.height() * 0.34f), titlePaint)

            val isCompleted = base.completed || (base.bookMark >= base.pages && base.pages > 0)
            val progressPercent = if (base.pages > 0) (base.bookMark.toFloat() / base.pages.toFloat()).coerceIn(0f, 1f) else 0f
            val barW = availW * 0.65f
            val barH = 8f
            val barTop = cardRect.top + (cardRect.height() * 0.50f)
            val barBgRect = RectF(textLeft, barTop, textLeft + barW, barTop + barH)

            val barBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (colors.isDark) Color.argb(60, 255, 255, 255) else Color.argb(40, 0, 0, 0)
            }
            canvas.drawRoundRect(barBgRect, 4f, 4f, barBgPaint)

            val barFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isCompleted) Color.parseColor("#10B981") else colors.accent
            }
            val fillW = if (isCompleted) barW else barW * progressPercent
            if (fillW > 0) {
                canvas.drawRoundRect(RectF(textLeft, barTop, textLeft + fillW, barTop + barH), 4f, 4f, barFillPaint)
            }

            val meta = formatCardMetrics(item.item, config)
            if (meta.isNotBlank()) {
                val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colors.textSecondary
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textSize = min(cardRect.height() * 0.18f, 22f)
                }
                canvas.drawText(meta, textLeft, cardRect.bottom - (cardRect.height() * 0.16f), metaPaint)
            }

            canvas.restore()
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

        val maxPerShelf = 6
        val shelfRows = items.chunked(maxPerShelf)
        val numShelves = shelfRows.size
        val shelfSpacing = rect.height() * 0.04f
        val singleShelfH = (rect.height() - (shelfSpacing * (numShelves - 1))) / numShelves

        shelfRows.forEachIndexed { shelfIdx, rowItems ->
            val shelfTop = rect.top + shelfIdx * (singleShelfH + shelfSpacing)
            val shelfRect = RectF(rect.left, shelfTop, rect.right, shelfTop + singleShelfH)

            val count = rowItems.size
            val shelfPadding = shelfRect.width() * 0.05f
            val availableW = shelfRect.width() - (shelfPadding * 2)

            val cardW = min(availableW * 0.35f, (availableW / max(1, count - 1)) * 1.45f)
            val cardH = shelfRect.height() * 0.82f
            val stepX = if (count > 1) (availableW - cardW) / (count - 1) else 0f
            val centerY = shelfRect.centerY()

            rowItems.forEachIndexed { idx, item ->
                val left = if (count == 1) {
                    shelfRect.centerX() - (cardW * 0.5f)
                } else {
                    shelfRect.left + shelfPadding + (idx * stepX)
                }
                val top = centerY - (cardH * 0.5f)
                val cardRect = RectF(left, top, left + cardW, top + cardH)

                canvas.save()
                val tilt = when {
                    idx == 0 && count > 1 -> -3f
                    idx == count - 1 && count > 1 -> 3f
                    else -> 0f
                }
                canvas.rotate(tilt, cardRect.centerX(), cardRect.centerY())
                drawCoverCard(canvas, cardRect, item, config, colors)
                canvas.restore()
            }
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
        val statCardH = min(rect.height() * 0.22f, 220f)
        val statCardRect = RectF(rect.left, rect.top, rect.right, rect.top + statCardH)

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

        canvas.drawText("${allStats.size}", statCardRect.left + (colW * 0.5f), statCardRect.top + (statCardH * 0.45f), numPaint)
        canvas.drawText("Obras Lidas", statCardRect.left + (colW * 0.5f), statCardRect.top + (statCardH * 0.72f), lblPaint)

        canvas.drawText("$totalPages", statCardRect.left + (colW * 1.5f), statCardRect.top + (statCardH * 0.45f), numPaint)
        canvas.drawText("Páginas", statCardRect.left + (colW * 1.5f), statCardRect.top + (statCardH * 0.72f), lblPaint)

        canvas.drawText("${hours}h", statCardRect.left + (colW * 2.5f), statCardRect.top + (statCardH * 0.45f), numPaint)
        canvas.drawText("Tempo Total", statCardRect.left + (colW * 2.5f), statCardRect.top + (statCardH * 0.72f), lblPaint)

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

        val count = items.size
        val spacing = min(rect.height() * 0.02f, 18f)
        val itemH = (rect.height() - (spacing * (count - 1))) / count

        items.forEachIndexed { idx, item ->
            val top = rect.top + idx * (itemH + spacing)
            val rowRect = RectF(rect.left, top, rect.right, top + itemH)

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (colors.isDark) Color.argb(40, 255, 255, 255) else Color.argb(20, 0, 0, 0)
            }
            canvas.drawRoundRect(rowRect, 16f, 16f, bgPaint)

            val coverW = rowRect.height() * 0.7f
            val coverRect = RectF(rowRect.left + 12f, rowRect.top + 8f, rowRect.left + 12f + coverW, rowRect.bottom - 8f)
            drawCoverImageOnly(canvas, coverRect, item.cover, item.item.base.title)

            val textLeft = coverRect.right + 20f
            val availableTextW = max(40f, rowRect.right - 20f - textLeft)
            val base = item.item.base

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors.textPrimary
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = min(rowRect.height() * 0.28f, 36f)
            }
            val rawTitle = if (base.title.isNotBlank()) base.title else base.name
            val title = TextUtils.ellipsize(rawTitle, titlePaint, availableTextW, TextUtils.TruncateAt.END).toString()
            canvas.drawText(title, textLeft, rowRect.top + (rowRect.height() * 0.42f), titlePaint)

            val meta = formatCardMetrics(item.item, config)
            if (meta.isNotBlank()) {
                val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colors.textSecondary
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textSize = min(rowRect.height() * 0.2f, 26f)
                }
                canvas.drawText(meta, textLeft, rowRect.top + (rowRect.height() * 0.75f), metaPaint)
            }
        }
    }

    private fun formatCardMetrics(stat: HistoryStatistics, config: SocialShareConfig): String {
        val parts = mutableListOf<String>()
        if (config.showPagesRead) {
            parts.add("${stat.pagesRead} págs")
        }
        if (config.showReadingTime) {
            val totalMins = (stat.timeRead / 60).coerceAtLeast(1)
            val hours = totalMins / 60
            val mins = totalMins % 60
            val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
            parts.add(timeStr)
        }
        return parts.joinToString(" • ")
    }

    private fun drawCoverCard(
        canvas: Canvas,
        rect: RectF,
        item: ItemRenderData,
        config: SocialShareConfig,
        colors: ThemeColors
    ) {
        val radius = 20f

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colors.cardBg
        }
        canvas.drawRoundRect(rect, radius, radius, cardPaint)

        drawCoverImageOnly(canvas, rect, item.cover, item.item.base.title)

        val hasTitle = config.showTitles
        val metricsText = formatCardMetrics(item.item, config)
        val hasMetrics = metricsText.isNotBlank()

        if (hasTitle || hasMetrics) {
            val padH = max(10f, rect.width() * 0.055f)
            val padBottom = max(10f, rect.height() * 0.035f)
            val availW = max(20f, rect.width() - (padH * 2f))

            val base = item.item.base
            val rawTitle = if (base.title.isNotBlank()) base.title else base.name

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = (rect.width() * 0.082f).coerceIn(13f, 34f)
                setShadowLayer(4f, 0f, 1.5f, Color.argb(180, 0, 0, 0))
            }

            val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2E8F0")
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = (rect.width() * 0.066f).coerceIn(11f, 26f)
                setShadowLayer(3f, 0f, 1f, Color.argb(180, 0, 0, 0))
            }

            val titleLayout = if (hasTitle) {
                StaticLayout.Builder.obtain(rawTitle, 0, rawTitle.length, titlePaint, availW.toInt())
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setMaxLines(2)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .setIncludePad(false)
                    .build()
            } else null

            val titleH = titleLayout?.height?.toFloat() ?: 0f
            val metricsH = if (hasMetrics) subPaint.textSize * 1.25f else 0f
            val gap = if (hasTitle && hasMetrics) 5f else 0f
            val totalTextH = titleH + gap + metricsH

            val clipPath = Path().apply {
                addRoundRect(rect, radius, radius, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clipPath)

            if (config.useCardTextBlur) {
                val boxTop = (rect.bottom - totalTextH - (padBottom * 2.2f)).coerceAtLeast(rect.top + rect.height() * 0.4f)
                val blurRect = RectF(rect.left, boxTop, rect.right, rect.bottom)

                drawBlurredCardFooter(canvas, rect, blurRect, item.cover)

                val frostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(115, 12, 16, 28)
                }
                canvas.drawRect(blurRect, frostPaint)

                val topLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(65, 255, 255, 255)
                    strokeWidth = 1.5f
                }
                canvas.drawLine(blurRect.left, blurRect.top, blurRect.right, blurRect.top, topLinePaint)
            } else {
                val fadeHeight = max(rect.height() * 0.42f, totalTextH + (padBottom * 3f))
                val gradRect = RectF(rect.left, (rect.bottom - fadeHeight).coerceAtLeast(rect.top), rect.right, rect.bottom)
                val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, gradRect.top, 0f, gradRect.bottom,
                        intArrayOf(Color.TRANSPARENT, Color.argb(145, 0, 0, 0), Color.argb(235, 0, 0, 0)),
                        floatArrayOf(0f, 0.45f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(gradRect, gradPaint)
            }

            var currentBottom = rect.bottom - padBottom
            if (hasMetrics) {
                val ellipsizedMetrics = TextUtils.ellipsize(metricsText, subPaint, availW, TextUtils.TruncateAt.END).toString()
                canvas.drawText(ellipsizedMetrics, rect.left + padH, currentBottom - subPaint.descent(), subPaint)
                currentBottom -= (metricsH + gap)
            }

            if (titleLayout != null) {
                canvas.save()
                canvas.translate(rect.left + padH, currentBottom - titleH)
                titleLayout.draw(canvas)
                canvas.restore()
            }

            canvas.restore()
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = colors.cardBorder
            strokeWidth = 3f
        }
        canvas.drawRoundRect(rect, radius, radius, borderPaint)

        if (config.showProgressStatus) {
            val isCompleted = item.item.base.completed || (item.item.base.bookMark >= item.item.base.pages && item.item.base.pages > 0)
            if (isCompleted) {
                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#10B981")
                }
                val badgeRadius = (rect.width() * 0.10f).coerceIn(14f, 28f)
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
    }

    private fun drawBlurredCardFooter(
        canvas: Canvas,
        cardRect: RectF,
        blurRect: RectF,
        cover: Bitmap?
    ) {
        if (cover == null || cover.isRecycled) return
        val smallW = max(16, (blurRect.width() / 6f).roundToInt())
        val smallH = max(10, (blurRect.height() / 6f).roundToInt())

        val smallBmp = Bitmap.createBitmap(smallW, smallH, Bitmap.Config.ARGB_8888)
        val smallCanvas = Canvas(smallBmp)

        val scaleX = smallW / blurRect.width()
        val scaleY = smallH / blurRect.height()
        smallCanvas.scale(scaleX, scaleY)
        smallCanvas.translate(-blurRect.left, -blurRect.top)

        drawCenterCropBitmap(smallCanvas, cardRect, cover)
        fastBoxBlur(smallBmp, radius = 3, iterations = 3)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(smallBmp, null, blurRect, paint)
        smallBmp.recycle()
    }

    private fun fastBoxBlur(bitmap: Bitmap, radius: Int, iterations: Int) {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= radius * 2 || h <= radius * 2) return

        val pixels = IntArray(w * h)
        val temp = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val div = 2 * radius + 1
        repeat(iterations) {
            for (y in 0 until h) {
                val rowOffset = y * w
                for (x in 0 until w) {
                    var rSum = 0
                    var gSum = 0
                    var bSum = 0
                    for (k in -radius..radius) {
                        val px = (x + k).coerceIn(0, w - 1)
                        val c = pixels[rowOffset + px]
                        rSum += (c shr 16) and 0xFF
                        gSum += (c shr 8) and 0xFF
                        bSum += c and 0xFF
                    }
                    temp[rowOffset + x] = (0xFF shl 24) or ((rSum / div) shl 16) or ((gSum / div) shl 8) or (bSum / div)
                }
            }
            for (x in 0 until w) {
                for (y in 0 until h) {
                    var rSum = 0
                    var gSum = 0
                    var bSum = 0
                    for (k in -radius..radius) {
                        val py = (y + k).coerceIn(0, h - 1)
                        val c = temp[py * w + x]
                        rSum += (c shr 16) and 0xFF
                        gSum += (c shr 8) and 0xFF
                        bSum += c and 0xFF
                    }
                    pixels[y * w + x] = (0xFF shl 24) or ((rSum / div) shl 16) or ((gSum / div) shl 8) or (bSum / div)
                }
            }
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun drawCenterCropBitmap(canvas: Canvas, rect: RectF, cover: Bitmap) {
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

        canvas.drawBitmap(cover, scaledSrc, rect, Paint(Paint.FILTER_BITMAP_FLAG))
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
            drawCenterCropBitmap(canvas, rect, cover)
        } else {
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

    suspend fun saveImagesToGallery(context: Context, bitmaps: List<Bitmap>, baseFileName: String): Int = withContext(Dispatchers.IO) {
        var savedCount = 0
        bitmaps.forEachIndexed { index, bmp ->
            val name = if (bitmaps.size > 1) "${baseFileName}_${index + 1}" else baseFileName
            val uri = saveImageToGallery(context, bmp, name)
            if (uri != null) savedCount++
        }
        savedCount
    }

    suspend fun shareImage(context: Context, bitmap: Bitmap, fileName: String) {
        shareImages(context, listOf(bitmap), fileName)
    }

    suspend fun shareImages(context: Context, bitmaps: List<Bitmap>, baseFileName: String) = withContext(Dispatchers.IO) {
        if (bitmaps.isEmpty()) return@withContext
        try {
            val cacheFolder = File(GeneralConsts.getCacheDir(context), "social_share").also { if (!it.exists()) it.mkdirs() }
            val uris = ArrayList<Uri>()

            bitmaps.forEachIndexed { index, bitmap ->
                val name = if (bitmaps.size > 1) "${baseFileName}_${index + 1}.png" else "$baseFileName.png"
                val file = File(cacheFolder, name)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                uris.add(uri)
            }

            val intent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                    clipData = android.content.ClipData.newRawUri("SocialShare", uris.first())
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "image/png"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    val clip = android.content.ClipData.newRawUri("SocialShare", uris.first())
                    for (i in 1 until uris.size) {
                        clip.addItem(android.content.ClipData.Item(uris[i]))
                    }
                    clipData = clip
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            withContext(Dispatchers.Main) {
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.social_share_btn_share)))
            }
        } catch (e: Exception) {
            mLOGGER.error("Error sharing images", e)
        }
    }
}
