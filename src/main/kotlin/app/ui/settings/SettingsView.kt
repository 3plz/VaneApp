package app.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.i18n.AppLanguage
import app.i18n.strings
import app.theme.GoogleSansFontFamily
import app.theme.StudioTheme
import app.theme.ThemePreset
import app.theme.ThemePresets
import app.ui.oobe.components.ThemePreviewCard

enum class SettingsCategory(
    val icon: ImageVector,
    val titleEn: String,
    val titleRu: String
) {
    APPEARANCE(Icons.Default.Palette, "Appearance", "Внешний вид"),
    LANGUAGE(Icons.Default.Translate, "Language", "Язык"),
    JAVA_MEMORY(Icons.Default.Memory, "Java & Memory", "Java и память"),
    LAUNCHER(Icons.Default.Tune, "Launcher", "Лаунчер"),
    ABOUT(Icons.Default.Info, "About", "О программе");

    fun title(isRu: Boolean): String = if (isRu) titleRu else titleEn
}

/**
 * Full-featured Settings view containing Appearance (Themes), Language,
 * Java & Memory, Launcher preferences, and About section.
 */
@Composable
fun SettingsView(
    activePreset: ThemePreset,
    onThemeChanged: (ThemePreset) -> Unit,
    currentLanguage: AppLanguage,
    onLanguageChanged: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRu = currentLanguage == AppLanguage.RU
    var selectedCategory by remember { mutableStateOf(SettingsCategory.APPEARANCE) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Top navigation bar: Back button and "Settings" title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Back to Launcher button
                val backInteraction = remember { MutableInteractionSource() }
                val isBackHovered by backInteraction.collectIsHoveredAsState()
                val backBg by animateColorAsState(
                    targetValue = if (isBackHovered) StudioTheme.HoverDark else StudioTheme.SurfaceCardDark,
                    animationSpec = tween(140)
                )

                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(backBg)
                        .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(10.dp))
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(
                            interactionSource = backInteraction,
                            indication = ripple(color = StudioTheme.PrimaryIndigo.copy(alpha = 0.2f)),
                            onClick = onBack
                        )
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.backToLauncher,
                        tint = StudioTheme.TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = strings.backToLauncher,
                        fontFamily = GoogleSansFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = StudioTheme.TextPrimary
                    )
                }

                Text(
                    text = strings.settings,
                    fontFamily = GoogleSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = StudioTheme.TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Body: Left settings categories sidebar + Right category contents
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Settings Sidebar
            Column(
                modifier = Modifier
                    .width(210.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(StudioTheme.SurfaceCardDark)
                    .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(16.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SettingsCategory.entries.forEach { category ->
                    val isSelected = category == selectedCategory
                    val interactionSource = remember { MutableInteractionSource() }
                    val isHovered by interactionSource.collectIsHoveredAsState()

                    val itemBg by animateColorAsState(
                        targetValue = when {
                            isSelected -> StudioTheme.PrimaryIndigo.copy(alpha = 0.18f)
                            isHovered -> StudioTheme.HoverDark
                            else -> Color.Transparent
                        },
                        animationSpec = tween(140)
                    )

                    val contentColor by animateColorAsState(
                        targetValue = when {
                            isSelected -> StudioTheme.PrimaryIndigo
                            isHovered -> StudioTheme.TextPrimary
                            else -> StudioTheme.TextSecondary
                        },
                        animationSpec = tween(140)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(itemBg)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        1.dp,
                                        StudioTheme.PrimaryIndigo.copy(alpha = 0.45f),
                                        RoundedCornerShape(10.dp)
                                    )
                                } else Modifier
                            )
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = ripple(color = StudioTheme.PrimaryIndigo.copy(alpha = 0.2f)),
                                onClick = { selectedCategory = category }
                            )
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = category.title(isRu),
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = category.title(isRu),
                            fontFamily = GoogleSansFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = contentColor
                        )
                    }
                }
            }

            // Right Settings Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(StudioTheme.SurfaceCardDark)
                    .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                AnimatedContent(
                    targetState = selectedCategory,
                    transitionSpec = { fadeIn(tween(180)).togetherWith(fadeOut(tween(120))) }
                ) { category ->
                    when (category) {
                        SettingsCategory.APPEARANCE -> AppearanceSettings(
                            activePreset = activePreset,
                            onThemeChanged = onThemeChanged,
                            isRu = isRu
                        )
                        SettingsCategory.LANGUAGE -> LanguageSettings(
                            currentLanguage = currentLanguage,
                            onLanguageChanged = onLanguageChanged
                        )
                        SettingsCategory.JAVA_MEMORY -> JavaMemorySettings(isRu = isRu)
                        SettingsCategory.LAUNCHER -> LauncherPreferenceSettings(isRu = isRu)
                        SettingsCategory.ABOUT -> AboutSettings(isRu = isRu)
                    }
                }
            }
        }
    }
}

/**
 * 1. Appearance / Themes Settings with Dark / Light switcher and ThemePreviewCards.
 */
@Composable
private fun AppearanceSettings(
    activePreset: ThemePreset,
    onThemeChanged: (ThemePreset) -> Unit,
    isRu: Boolean
) {
    var isDarkTab by remember { mutableStateOf(activePreset.isDark) }
    val displayedPresets = remember(isDarkTab) {
        if (isDarkTab) ThemePresets.darkPresets else ThemePresets.lightPresets
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = strings.selectThemeTitle,
            fontFamily = GoogleSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = StudioTheme.TextPrimary
        )

        // Dark / Light Tab Switcher
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(StudioTheme.SurfaceContainerDark)
                .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ThemeModeTab(
                title = strings.darkThemeTab,
                icon = Icons.Default.DarkMode,
                isSelected = isDarkTab,
                onClick = { isDarkTab = true }
            )
            ThemeModeTab(
                title = strings.lightThemeTab,
                icon = Icons.Default.LightMode,
                isSelected = !isDarkTab,
                onClick = { isDarkTab = false }
            )
        }

        // 3 Theme cards row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            displayedPresets.forEach { preset ->
                ThemePreviewCard(
                    preset = preset,
                    isSelected = preset.id == activePreset.id,
                    isRu = isRu,
                    onClick = { onThemeChanged(preset) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ThemeModeTab(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bg by animateColorAsState(
        targetValue = when {
            isSelected -> StudioTheme.PrimaryIndigo
            isHovered -> StudioTheme.HoverDark
            else -> Color.Transparent
        },
        animationSpec = tween(150)
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color.White
            isHovered -> StudioTheme.TextPrimary
            else -> StudioTheme.TextSecondary
        },
        animationSpec = tween(150)
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            fontFamily = GoogleSansFontFamily,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp,
            color = contentColor
        )
    }
}

/**
 * 2. Language Settings with cards for English and Russian.
 */
@Composable
private fun LanguageSettings(
    currentLanguage: AppLanguage,
    onLanguageChanged: (AppLanguage) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = strings.selectLanguageTitle,
            fontFamily = GoogleSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = StudioTheme.TextPrimary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LanguageCard(
                title = strings.englishTitle,
                code = "EN",
                nativeName = "English (US)",
                isSelected = currentLanguage == AppLanguage.EN,
                onClick = { onLanguageChanged(AppLanguage.EN) },
                modifier = Modifier.weight(1f)
            )

            LanguageCard(
                title = strings.russianTitle,
                code = "RU",
                nativeName = "Русский",
                isSelected = currentLanguage == AppLanguage.RU,
                onClick = { onLanguageChanged(AppLanguage.RU) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LanguageCard(
    title: String,
    code: String,
    nativeName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val border by animateColorAsState(
        targetValue = when {
            isSelected -> StudioTheme.PrimaryIndigo
            isHovered -> StudioTheme.PrimaryIndigoLight.copy(alpha = 0.6f)
            else -> StudioTheme.BorderDark
        },
        animationSpec = tween(150)
    )

    val bg by animateColorAsState(
        targetValue = when {
            isSelected -> StudioTheme.PrimaryIndigo.copy(alpha = 0.12f)
            isHovered -> StudioTheme.HoverDark
            else -> StudioTheme.SurfaceContainerDark
        },
        animationSpec = tween(150)
    )

    Row(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(if (isSelected) 1.5.dp else 1.dp, border, RoundedCornerShape(12.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = StudioTheme.PrimaryIndigo.copy(alpha = 0.2f)),
                onClick = onClick
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) StudioTheme.PrimaryIndigo.copy(alpha = 0.2f)
                        else StudioTheme.HoverDark
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = code,
                    fontFamily = GoogleSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isSelected) StudioTheme.PrimaryIndigo else StudioTheme.TextSecondary
                )
            }

            Column {
                Text(
                    text = title,
                    fontFamily = GoogleSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = StudioTheme.TextPrimary
                )
                Text(
                    text = nativeName,
                    fontFamily = GoogleSansFontFamily,
                    fontSize = 12.sp,
                    color = StudioTheme.TextMuted
                )
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(StudioTheme.PrimaryIndigo),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * 3. Dummy tab: Java & Memory Allocation.
 */
@Composable
private fun JavaMemorySettings(isRu: Boolean) {
    var ramValue by remember { mutableStateOf(4096f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = if (isRu) "Настройки Java и оперативной памяти" else "Java & Memory Allocation",
            fontFamily = GoogleSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = StudioTheme.TextPrimary
        )

        // Java Path selector
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = strings.javaPathTitle,
                fontFamily = GoogleSansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = StudioTheme.TextSecondary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioTheme.SurfaceContainerDark)
                    .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Auto-detect (OpenJDK 21.0.3 x64 • System default)",
                    fontFamily = GoogleSansFontFamily,
                    fontSize = 13.sp,
                    color = StudioTheme.TextPrimary
                )

                Text(
                    text = if (isRu) "Обзор..." else "Browse...",
                    fontFamily = GoogleSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = StudioTheme.PrimaryIndigo
                )
            }
        }

        // RAM Allocation slider
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.allocatedRam,
                    fontFamily = GoogleSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = StudioTheme.TextSecondary
                )
                Text(
                    text = "${ramValue.toInt()} MB (${String.format("%.1f", ramValue / 1024f)} GB)",
                    fontFamily = GoogleSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = StudioTheme.PrimaryIndigo
                )
            }

            Slider(
                value = ramValue,
                onValueChange = { ramValue = it },
                valueRange = 2048f..16384f,
                steps = 13,
                colors = SliderDefaults.colors(
                    thumbColor = StudioTheme.PrimaryIndigo,
                    activeTrackColor = StudioTheme.PrimaryIndigo,
                    inactiveTrackColor = StudioTheme.SurfaceContainerDark
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("2 GB", fontSize = 11.sp, color = StudioTheme.TextMuted)
                Text("8 GB", fontSize = 11.sp, color = StudioTheme.TextMuted)
                Text("16 GB", fontSize = 11.sp, color = StudioTheme.TextMuted)
            }
        }

        // JVM Args dummy
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (isRu) "Дополнительные аргументы JVM" else "Custom JVM Arguments",
                fontFamily = GoogleSansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = StudioTheme.TextSecondary
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioTheme.SurfaceContainerDark)
                    .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200",
                    fontFamily = GoogleSansFontFamily,
                    fontSize = 12.sp,
                    color = StudioTheme.TextMuted
                )
            }
        }
    }
}

/**
 * 4. Dummy tab: Launcher & Game Preferences.
 */
@Composable
private fun LauncherPreferenceSettings(isRu: Boolean) {
    var closeOnLaunch by remember { mutableStateOf(false) }
    var discordRpc by remember { mutableStateOf(true) }
    var autoUpdates by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isRu) "Параметры запуска и интеграции" else "Launcher & Game Preferences",
            fontFamily = GoogleSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = StudioTheme.TextPrimary
        )

        SettingToggleRow(
            title = if (isRu) "Закрывать лаунчер при запуске игры" else "Close launcher when game starts",
            description = if (isRu) "Освобождает ресурсы системы во время игры" else "Frees system memory while playing",
            checked = closeOnLaunch,
            onCheckedChange = { closeOnLaunch = it }
        )

        SettingToggleRow(
            title = if (isRu) "Интеграция Discord Rich Presence" else "Discord Rich Presence",
            description = if (isRu) "Отображать статус и сборку в профиле Discord" else "Display current instance and game status in Discord",
            checked = discordRpc,
            onCheckedChange = { discordRpc = it }
        )

        SettingToggleRow(
            title = if (isRu) "Автоматическая проверка обновлений" else "Automatic Update Checks",
            description = if (isRu) "Уведомлять о новых релизах WickedApp" else "Notify about new WickedApp releases",
            checked = autoUpdates,
            onCheckedChange = { autoUpdates = it }
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioTheme.SurfaceContainerDark)
            .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = GoogleSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = StudioTheme.TextPrimary
            )
            Text(
                text = description,
                fontFamily = GoogleSansFontFamily,
                fontSize = 12.sp,
                color = StudioTheme.TextMuted
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = StudioTheme.PrimaryIndigo,
                uncheckedThumbColor = StudioTheme.TextMuted,
                uncheckedTrackColor = StudioTheme.SurfaceCardDark
            )
        )
    }
}

/**
 * 5. Dummy tab: About WickedApp.
 */
@Composable
private fun AboutSettings(isRu: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(StudioTheme.PrimaryIndigo.copy(alpha = 0.15f))
                .border(1.dp, StudioTheme.PrimaryIndigo.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.RocketLaunch,
                contentDescription = null,
                tint = StudioTheme.PrimaryIndigo,
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = "WickedApp",
            fontFamily = GoogleSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = StudioTheme.TextPrimary
        )

        Text(
            text = if (isRu) "Версия 1.0.0 • Автор 3plz" else "Version 1.0.0 • Author 3plz",
            fontFamily = GoogleSansFontFamily,
            fontSize = 13.sp,
            color = StudioTheme.TextSecondary
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(StudioTheme.SurfaceContainerDark)
                .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "License: GNU General Public License v2 (GPLv2)",
                fontFamily = GoogleSansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = StudioTheme.TextMuted
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TechBadge("Kotlin 2.1")
            TechBadge("Compose Multiplatform")
            TechBadge("Skiko")
            TechBadge("Material 3")
        }
    }
}

@Composable
private fun TechBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(StudioTheme.HoverDark)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontFamily = GoogleSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = StudioTheme.TextSecondary
        )
    }
}
