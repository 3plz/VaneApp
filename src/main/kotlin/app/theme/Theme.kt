package app.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle

// Google Sans Clean Studio Typography
val GoogleSansFontFamily = FontFamily(
    Font("fonts/GoogleSans-Regular.ttf", FontWeight.Normal),
    Font("fonts/GoogleSans-Medium.ttf", FontWeight.Medium),
    Font("fonts/GoogleSans-Bold.ttf", FontWeight.Bold)
)

val AppTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = (-0.5).sp
    ),
    titleMedium = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = (-0.2).sp
    ),
    titleSmall = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    ),
    bodySmall = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    labelMedium = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        letterSpacing = 0.6.sp
    )
)

enum class AppAccentColor(
    val id: String,
    val titleRu: String,
    val titleEn: String,
    val seedColor: Color,
    val primaryColor: Color,
    val primaryColorLight: Color
) {
    INDIGO(
        id = "indigo",
        titleRu = "Индиго",
        titleEn = "Indigo",
        seedColor = Color(0xFF6366F1),
        primaryColor = Color(0xFF6366F1),
        primaryColorLight = Color(0xFF818CF8)
    ),
    VIOLET(
        id = "violet",
        titleRu = "Фиолетовый",
        titleEn = "Violet",
        seedColor = Color(0xFF8B5CF6),
        primaryColor = Color(0xFF8B5CF6),
        primaryColorLight = Color(0xFFA78BFA)
    ),
    BLUE(
        id = "blue",
        titleRu = "Синий",
        titleEn = "Blue",
        seedColor = Color(0xFF3B82F6),
        primaryColor = Color(0xFF3B82F6),
        primaryColorLight = Color(0xFF60A5FA)
    ),
    CYAN(
        id = "cyan",
        titleRu = "Бирюзовый",
        titleEn = "Cyan",
        seedColor = Color(0xFF06B6D4),
        primaryColor = Color(0xFF06B6D4),
        primaryColorLight = Color(0xFF38BDF8)
    ),
    EMERALD(
        id = "emerald",
        titleRu = "Изумрудный",
        titleEn = "Emerald",
        seedColor = Color(0xFF10B981),
        primaryColor = Color(0xFF10B981),
        primaryColorLight = Color(0xFF34D399)
    ),
    AMBER(
        id = "amber",
        titleRu = "Янтарный",
        titleEn = "Amber",
        seedColor = Color(0xFFF59E0B),
        primaryColor = Color(0xFFF59E0B),
        primaryColorLight = Color(0xFFFBBF24)
    ),
    ROSE(
        id = "rose",
        titleRu = "Розовый",
        titleEn = "Rose",
        seedColor = Color(0xFFF43F5E),
        primaryColor = Color(0xFFF43F5E),
        primaryColorLight = Color(0xFFFB7185)
    ),
    CUSTOM(
        id = "custom",
        titleRu = "Своя палитра",
        titleEn = "Custom",
        seedColor = Color(0xFF6366F1),
        primaryColor = Color(0xFF6366F1),
        primaryColorLight = Color(0xFF818CF8)
    );

    fun getTitle(isRu: Boolean = true): String =
        if (isRu) titleRu else titleEn
}

fun lightenColor(color: Color, factor: Float = 0.25f): Color {
    return Color(
        red = (color.red + (1f - color.red) * factor).coerceIn(0f, 1f),
        green = (color.green + (1f - color.green) * factor).coerceIn(0f, 1f),
        blue = (color.blue + (1f - color.blue) * factor).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}

fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
    val h = (hue % 360f + 360f) % 360f
    val s = saturation.coerceIn(0f, 1f)
    val v = value.coerceIn(0f, 1f)

    val c = v * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = v - c

    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(r + m, g + m, b + m, 1f)
}

fun colorToHsv(color: Color): Triple<Float, Float, Float> {
    val r = color.red
    val g = color.green
    val b = color.blue

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    val h = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }

    val s = if (max == 0f) 0f else delta / max
    val v = max

    return Triple(h, s, v)
}

fun colorToHex(color: Color): String {
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(r, g, b)
}

fun hexToColorOrNull(hex: String): Color? {
    val clean = hex.trim().removePrefix("#")
    if (clean.length != 6 && clean.length != 8) return null
    return try {
        val num = clean.toLong(16)
        if (clean.length == 6) {
            Color(num or 0xFF000000)
        } else {
            Color(num)
        }
    } catch (_: Exception) {
        null
    }
}

// Studio Slate Palette
data class StudioColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceContainer: Color,
    val surfaceCard: Color,
    val border: Color,
    val borderSubtle: Color,
    val hover: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primaryIndigo: Color,
    val primaryIndigoLight: Color,
    val accent: AppAccentColor = AppAccentColor.INDIGO
)

val DarkStudioColors = StudioColors(
    isDark = true,
    background = Color(0xFF0B0D13),
    surface = Color(0xFF11141D),
    surfaceContainer = Color(0xFF161925),
    surfaceCard = Color(0xFF191D2B),
    border = Color(0xFF222738),
    borderSubtle = Color(0xFF1D2230),
    hover = Color(0xFF202638),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    primaryIndigo = Color(0xFF6366F1),
    primaryIndigoLight = Color(0xFF818CF8),
    accent = AppAccentColor.INDIGO
)

val LightStudioColors = StudioColors(
    isDark = false,
    background = Color(0xFFF8FAFC),       // Clean light paper-like slate canvas
    surface = Color(0xFFFFFFFF),          // Crisp white for titlebar and sidebars
    surfaceContainer = Color(0xFFF1F5F9), // Light slate container
    surfaceCard = Color(0xFFF1F5F9),      // Light cards and input surfaces
    border = Color(0xFFCBD5E1),           // Slate 300 crisp border
    borderSubtle = Color(0xFFE2E8F0),     // Slate 200 subtle divider
    hover = Color(0xFFE2E8F0),            // Hover state
    textPrimary = Color(0xFF0F172A),      // Slate 900 dark, crisp text
    textSecondary = Color(0xFF475569),    // Slate 600 medium dark text
    textMuted = Color(0xFF64748B),        // Slate 500 readable muted text
    primaryIndigo = Color(0xFF4F46E5),    // Rich indigo
    primaryIndigoLight = Color(0xFF6366F1),// Vibrant indigo
    accent = AppAccentColor.INDIGO
)

val LocalStudioColors = staticCompositionLocalOf { DarkStudioColors }

object StudioTheme {
    val isDark: Boolean
        @Composable get() = LocalStudioColors.current.isDark

    val BackgroundDark: Color
        @Composable get() = LocalStudioColors.current.background

    val SurfaceDark: Color
        @Composable get() = LocalStudioColors.current.surface

    val SurfaceContainerDark: Color
        @Composable get() = LocalStudioColors.current.surfaceContainer

    val SurfaceCardDark: Color
        @Composable get() = LocalStudioColors.current.surfaceCard

    val BorderDark: Color
        @Composable get() = LocalStudioColors.current.border

    val BorderSubtleDark: Color
        @Composable get() = LocalStudioColors.current.borderSubtle

    val HoverDark: Color
        @Composable get() = LocalStudioColors.current.hover

    val TextPrimary: Color
        @Composable get() = LocalStudioColors.current.textPrimary

    val TextSecondary: Color
        @Composable get() = LocalStudioColors.current.textSecondary

    val TextMuted: Color
        @Composable get() = LocalStudioColors.current.textMuted

    val PrimaryIndigo: Color
        @Composable get() = LocalStudioColors.current.primaryIndigo

    val PrimaryIndigoLight: Color
        @Composable get() = LocalStudioColors.current.primaryIndigoLight

    val AccentCyan = Color(0xFF06B6D4)
    val AccentEmerald = Color(0xFF10B981)
    val AccentAmber = Color(0xFFF59E0B)
    val AccentRose = Color(0xFFF43F5E)
}

val IdeSeedColor = Color(0xFF6366F1)

@Composable
fun WickedTheme(
    darkTheme: Boolean = true,
    accentColor: AppAccentColor = AppAccentColor.INDIGO,
    customAccentHex: String? = null,
    content: @Composable () -> Unit
) {
    val customColor = remember(customAccentHex) { customAccentHex?.let { hexToColorOrNull(it) } }

    val effectiveSeed = if (accentColor == AppAccentColor.CUSTOM && customColor != null) {
        customColor
    } else {
        accentColor.seedColor
    }

    val effectivePrimary = if (accentColor == AppAccentColor.CUSTOM && customColor != null) {
        customColor
    } else {
        if (darkTheme) accentColor.primaryColor else accentColor.seedColor
    }

    val effectivePrimaryLight = if (accentColor == AppAccentColor.CUSTOM && customColor != null) {
        lightenColor(customColor, 0.25f)
    } else {
        accentColor.primaryColorLight
    }

    val base = if (darkTheme) DarkStudioColors else LightStudioColors
    val studioColors = remember(darkTheme, accentColor, customAccentHex) {
        base.copy(
            primaryIndigo = effectivePrimary,
            primaryIndigoLight = effectivePrimaryLight,
            accent = accentColor
        )
    }

    CompositionLocalProvider(LocalStudioColors provides studioColors) {
        DynamicMaterialTheme(
            seedColor = effectiveSeed,
            useDarkTheme = darkTheme,
            style = PaletteStyle.Expressive,
            typography = AppTypography,
            content = content
        )
    }
}