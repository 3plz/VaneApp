package app.ui.main.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import app.theme.GoogleSansFontFamily
import app.theme.StudioTheme

/**
 * Placeholder tabs for the launcher sidebar navigation.
 */
enum class LauncherTab(
    val titleEn: String,
    val titleRu: String,
    val icon: ImageVector,
    val badge: String? = null
) {
    HOME("Overview", "Обзор", Icons.Default.Dashboard),
    INSTANCES("Instances", "Сборки", Icons.Default.Layers, "Beta"),
    MODS("Mods & Addons", "Моды и аддоны", Icons.Default.Extension),
    WORLDS("Worlds", "Миры", Icons.Default.Public),
    SCREENSHOTS("Screenshots", "Скриншоты", Icons.Default.PhotoLibrary),
    LOGS("Logs", "Журнал логов", Icons.Default.Terminal);

    fun title(language: AppLanguage): String = if (language == AppLanguage.RU) titleRu else titleEn
}

@Composable
fun SidebarTabs(
    selectedTab: LauncherTab,
    onTabSelected: (LauncherTab) -> Unit,
    currentLanguage: AppLanguage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(210.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(StudioTheme.SurfaceCardDark)
            .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LauncherTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()

            val tabBg by animateColorAsState(
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
                    .background(tabBg)
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
                        onClick = { onTabSelected(tab) }
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.title(currentLanguage),
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = tab.title(currentLanguage),
                    fontFamily = GoogleSansFontFamily,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = contentColor,
                    modifier = Modifier.weight(1f)
                )

                if (tab.badge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) StudioTheme.PrimaryIndigo.copy(alpha = 0.25f)
                                else StudioTheme.SurfaceContainerDark
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tab.badge,
                            fontFamily = GoogleSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (isSelected) StudioTheme.PrimaryIndigo else StudioTheme.TextMuted
                        )
                    }
                }
            }
        }
    }
}
