package app.theme

import androidx.compose.ui.graphics.Color

data class ThemePreset(
    val id: String,
    val isDark: Boolean,
    val nameEn: String,
    val nameRu: String,
    val descriptionEn: String,
    val descriptionRu: String,
    val colors: StudioColors
) {
    fun getName(isRu: Boolean): String = if (isRu) nameRu else nameEn
    fun getDescription(isRu: Boolean): String = if (isRu) descriptionRu else descriptionEn
}

object ThemePresets {

    // Dark Preset 1: Standard / Default Dark
    val DarkDefault = ThemePreset(
        id = "dark_default",
        isDark = true,
        nameEn = "Default Dark",
        nameRu = "Стандартная",
        descriptionEn = "Modern deep slate with indigo accents",
        descriptionRu = "Классический глубокий сланцевый с индиго",
        colors = DarkStudioColors
    )

    // Dark Preset 2: Catppuccin Mocha
    val DarkCatppuccin = ThemePreset(
        id = "dark_catppuccin",
        isDark = true,
        nameEn = "Catppuccin",
        nameRu = "Catppuccin",
        descriptionEn = "Soothing warm pastel dark palette",
        descriptionRu = "Мягкая тёмная пастельная палитра",
        colors = StudioColors(
            isDark = true,
            background = Color(0xFF1E1E2E),       // Mocha Base
            surface = Color(0xFF181825),          // Mantle
            surfaceContainer = Color(0xFF11111B), // Crust
            surfaceCard = Color(0xFF313244),      // Surface0
            border = Color(0xFF45475A),           // Surface1
            borderSubtle = Color(0xFF313244),
            hover = Color(0xFF363A4F),
            textPrimary = Color(0xFFCDD6F4),      // Text
            textSecondary = Color(0xFFA6ADC8),    // Subtext0
            textMuted = Color(0xFF6C7086),        // Overlay0
            primaryIndigo = Color(0xFFCBA6F7),    // Mauve
            primaryIndigoLight = Color(0xFF89B4FA),// Sapphire
            accent = AppAccentColor.VIOLET
        )
    )

    // Dark Preset 3: Gruvbox Dark
    val DarkGruvbox = ThemePreset(
        id = "dark_gruvbox",
        isDark = true,
        nameEn = "Gruvbox",
        nameRu = "Gruvbox",
        descriptionEn = "Retro groove warm high-contrast theme",
        descriptionRu = "Ретро-палитра с тёплым оранжевым акцентом",
        colors = StudioColors(
            isDark = true,
            background = Color(0xFF282828),       // Dark0
            surface = Color(0xFF1D2021),          // Dark0_hard
            surfaceContainer = Color(0xFF32302F), // Dark1
            surfaceCard = Color(0xFF3C3836),      // Dark2
            border = Color(0xFF504945),           // Dark3
            borderSubtle = Color(0xFF3C3836),
            hover = Color(0xFF504945),
            textPrimary = Color(0xFFEBDBB2),      // Light1
            textSecondary = Color(0xFFA89984),    // Gray
            textMuted = Color(0xFF7C6F64),        // Dark4
            primaryIndigo = Color(0xFFFE8019),    // Bright Orange
            primaryIndigoLight = Color(0xFFFABD2F),// Bright Yellow
            accent = AppAccentColor.AMBER
        )
    )

    // Light Preset 1: Nordic Frost
    val LightNordic = ThemePreset(
        id = "light_nordic",
        isDark = false,
        nameEn = "Nordic Frost",
        nameRu = "Северный лед",
        descriptionEn = "Crisp, clean arctic minimalism",
        descriptionRu = "Чистый холодный минимализм с лазурным акцентом",
        colors = StudioColors(
            isDark = false,
            background = Color(0xFFF1F5F9),       // Slate 100
            surface = Color(0xFFFFFFFF),          // Pure White
            surfaceContainer = Color(0xFFE2E8F0), // Slate 200
            surfaceCard = Color(0xFFFFFFFF),
            border = Color(0xFFCBD5E1),           // Slate 300
            borderSubtle = Color(0xFFE2E8F0),
            hover = Color(0xFFE2E8F0),
            textPrimary = Color(0xFF0F172A),      // Slate 900
            textSecondary = Color(0xFF475569),    // Slate 600
            textMuted = Color(0xFF94A3B8),        // Slate 400
            primaryIndigo = Color(0xFF0284C7),    // Sky 600
            primaryIndigoLight = Color(0xFF38BDF8),// Sky 400
            accent = AppAccentColor.CYAN
        )
    )

    // Light Preset 2: Catppuccin Latte
    val LightCatppuccinLatte = ThemePreset(
        id = "light_catppuccin_latte",
        isDark = false,
        nameEn = "Catppuccin Latte",
        nameRu = "Catppuccin Latte",
        descriptionEn = "Warm creamy pastel paper theme",
        descriptionRu = "Нежная кремово-пастельная тема",
        colors = StudioColors(
            isDark = false,
            background = Color(0xFFEFF1F5),       // Latte Base
            surface = Color(0xFFE6E9EF),          // Mantle
            surfaceContainer = Color(0xFFDCE0E8), // Crust
            surfaceCard = Color(0xFFFFFFFF),
            border = Color(0xFFCCD0DA),           // Surface0
            borderSubtle = Color(0xFFDCE0E8),
            hover = Color(0xFFCCD0DA),
            textPrimary = Color(0xFF4C4F69),      // Text
            textSecondary = Color(0xFF6C6F85),    // Subtext0
            textMuted = Color(0xFF9CA0B0),        // Overlay0
            primaryIndigo = Color(0xFF8839EF),    // Mauve
            primaryIndigoLight = Color(0xFF1E66F5),// Blue
            accent = AppAccentColor.VIOLET
        )
    )

    // Light Preset 3: Solarized Sepia
    val LightSolarized = ThemePreset(
        id = "light_solarized",
        isDark = false,
        nameEn = "Solarized Light",
        nameRu = "Теплый папирус",
        descriptionEn = "Warm parchment with amber highlights",
        descriptionRu = "Тёплый винтажный пергамент с янтарным оттенком",
        colors = StudioColors(
            isDark = false,
            background = Color(0xFFFDF6E3),       // Base3
            surface = Color(0xFFEEE8D5),          // Base2
            surfaceContainer = Color(0xFFE4DCBE),
            surfaceCard = Color(0xFFFFFDF5),
            border = Color(0xFFD5CCB3),
            borderSubtle = Color(0xFFE5DECB),
            hover = Color(0xFFEAE2C9),
            textPrimary = Color(0xFF586E75),      // Base01
            textSecondary = Color(0xFF657B83),    // Base00
            textMuted = Color(0xFF93A1A1),        // Base1
            primaryIndigo = Color(0xFFB58900),    // Yellow/Amber
            primaryIndigoLight = Color(0xFFCB4B16),// Orange
            accent = AppAccentColor.AMBER
        )
    )

    val darkPresets = listOf(DarkDefault, DarkCatppuccin, DarkGruvbox)
    val lightPresets = listOf(LightNordic, LightCatppuccinLatte, LightSolarized)

    val allPresets = darkPresets + lightPresets

    fun findById(id: String): ThemePreset {
        return allPresets.firstOrNull { it.id == id } ?: DarkDefault
    }
}
