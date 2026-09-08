package app.ui.oobe

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.i18n.AppLanguage
import app.i18n.strings
import app.theme.GoogleSansFontFamily
import app.theme.ThemePreset
import app.theme.ThemePresets
import app.ui.login.components.LanguageSelector
import app.ui.oobe.components.ThemePreviewCard

/**
 * First-time setup (OOBE) screen allowing user to choose their preferred color scheme and preset theme.
 */
@Suppress("DEPRECATION")
@Composable
fun OobeScreen(
    currentPreset: ThemePreset,
    onPresetChanged: (ThemePreset) -> Unit,
    onFinish: () -> Unit,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDarkTab by remember { mutableStateOf(currentPreset.isDark) }
    val isRu = currentLanguage == AppLanguage.RU
    val colors = currentPreset.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header: Title, Subtitle, and Dark/Light Mode Switcher
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = strings.oobeTitle,
                    fontFamily = GoogleSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = strings.oobeSubtitle,
                    fontFamily = GoogleSansFontFamily,
                    fontSize = 13.5.sp,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Mode Switcher Pill (Dark Themes / Light Themes)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surfaceContainer)
                        .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Dark Tab
                    val darkTabInteraction = remember { MutableInteractionSource() }
                    val darkBg by animateColorAsState(
                        targetValue = if (isDarkTab) colors.primaryIndigo else Color.Transparent,
                        animationSpec = tween(150)
                    )
                    val darkTextColor by animateColorAsState(
                        targetValue = if (isDarkTab) Color.White else colors.textSecondary,
                        animationSpec = tween(150)
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(darkBg)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable(
                                interactionSource = darkTabInteraction,
                                indication = ripple(color = Color.White.copy(alpha = 0.2f)),
                                onClick = {
                                    isDarkTab = true
                                    if (!currentPreset.isDark) {
                                        onPresetChanged(ThemePresets.DarkDefault)
                                    }
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = darkTextColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.darkThemeTab,
                            fontFamily = GoogleSansFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.5.sp,
                            color = darkTextColor
                        )
                    }

                    // Light Tab
                    val lightTabInteraction = remember { MutableInteractionSource() }
                    val lightBg by animateColorAsState(
                        targetValue = if (!isDarkTab) colors.primaryIndigo else Color.Transparent,
                        animationSpec = tween(150)
                    )
                    val lightTextColor by animateColorAsState(
                        targetValue = if (!isDarkTab) Color.White else colors.textSecondary,
                        animationSpec = tween(150)
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(lightBg)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable(
                                interactionSource = lightTabInteraction,
                                indication = ripple(color = Color.White.copy(alpha = 0.2f)),
                                onClick = {
                                    isDarkTab = false
                                    if (currentPreset.isDark) {
                                        onPresetChanged(ThemePresets.LightNordic)
                                    }
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LightMode,
                            contentDescription = null,
                            tint = lightTextColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.lightThemeTab,
                            fontFamily = GoogleSansFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.5.sp,
                            color = lightTextColor
                        )
                    }
                }
            }

            // Center Theme Cards Carousel
            AnimatedContent(
                targetState = isDarkTab,
                transitionSpec = {
                    fadeIn(tween(200, easing = FastOutSlowInEasing))
                        .togetherWith(fadeOut(tween(150, easing = FastOutSlowInEasing)))
                }
            ) { darkActive ->
                val presets = if (darkActive) ThemePresets.darkPresets else ThemePresets.lightPresets

                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    presets.forEach { preset ->
                        ThemePreviewCard(
                            preset = preset,
                            isSelected = currentPreset.id == preset.id,
                            isRu = isRu,
                            onClick = {
                                onPresetChanged(preset)
                            }
                        )
                    }
                }
            }

            // Bottom Actions: Language Selector & Get Started Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageSelector(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = onLanguageSelected
                )

                // Get Started action button dynamically themed
                val startInteraction = remember { MutableInteractionSource() }
                val isStartHovered by startInteraction.collectIsHoveredAsState()

                val buttonBg by animateColorAsState(
                    targetValue = if (isStartHovered) colors.primaryIndigoLight else colors.primaryIndigo,
                    animationSpec = tween(150)
                )

                Surface(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(
                            interactionSource = startInteraction,
                            indication = ripple(color = Color.White),
                            onClick = onFinish
                        ),
                    shape = RoundedCornerShape(12.dp),
                    color = buttonBg,
                    border = BorderStroke(1.dp, colors.primaryIndigoLight.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = strings.getStarted,
                            fontFamily = GoogleSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color.White,
                            letterSpacing = 0.2.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
