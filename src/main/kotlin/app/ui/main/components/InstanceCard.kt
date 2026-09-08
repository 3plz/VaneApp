package app.ui.main.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.domain.storage.InstalledInstance
import app.i18n.LocalAppLanguage
import app.i18n.formatPlayTime
import app.i18n.strings
import app.theme.AccentColorExtractor
import app.theme.GoogleSansFontFamily
import app.theme.StudioTheme
import org.jetbrains.skia.Image as SkiaImage
import java.nio.file.Files

/**
 * Compact horizontal rectangle card displaying a Minecraft instance.
 * Layout:
 * - Top: Dynamic accent-themed banner with loader/version in top-left, and 3-dots menu in top-right.
 * - Left: Logo icon slightly overlapping the banner boundary with clean cutout border.
 * - Right of avatar (below banner): Instance title & loader badge.
 * - Bottom: Description / summary text, playtime badge and quick launch action.
 */
@Composable
fun InstanceCard(
    instance: InstalledInstance,
    onClick: () -> Unit = {},
    onPlayClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    isLaunching: Boolean = false,
    isRunning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val currentLanguage = LocalAppLanguage.current
    val defaultPrimaryColor = StudioTheme.PrimaryIndigo
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    var isMenuExpanded by remember { mutableStateOf(false) }

    // Locate local icon file in instance directory
    val iconFile = remember(instance.directory, instance.manifest.iconPath) {
        val file = if (!instance.manifest.iconPath.isNullOrBlank()) {
            instance.directory.resolve(instance.manifest.iconPath).toFile()
        } else {
            instance.directory.resolve("icon.png").toFile()
        }
        if (file.exists() && file.isFile) file else null
    }

    // Extract dynamic accent color from icon, with fallback to theme primary color
    val accentColor = remember(iconFile, defaultPrimaryColor) {
        AccentColorExtractor.extractAccentColor(iconFile, fallback = defaultPrimaryColor)
    }

    val borderColor by animateColorAsState(
        targetValue = if (isHovered || isMenuExpanded) accentColor else StudioTheme.BorderDark,
        animationSpec = tween(150)
    )

    // Load icon bitmap for rendering
    val iconBitmap = remember(iconFile) {
        if (iconFile != null) {
            try {
                val bytes = Files.readAllBytes(iconFile.toPath())
                SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
            } catch (_: Exception) {
                null
            }
        } else null
    }

    Box(
        modifier = modifier
            .width(330.dp)
            .height(184.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(StudioTheme.SurfaceCardDark)
            .border(1.2.dp, borderColor, RoundedCornerShape(16.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = accentColor.copy(alpha = 0.15f)),
                onClick = onClick
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Banner (Gradient Header dynamically themed by the icon's accent color)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.60f),
                                accentColor.copy(alpha = 0.28f),
                                StudioTheme.SurfaceCardDark.copy(alpha = 0.45f)
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Top-Left: Loader & Version Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(6.dp))
                        .background(StudioTheme.SurfaceCardDark.copy(alpha = 0.88f))
                        .border(1.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = "${instance.manifest.modLoader} ${instance.manifest.minecraftVersion}",
                        fontFamily = GoogleSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = accentColor
                    )
                }

                // Top-Right: 3-Dots Menu Button
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    val menuInteraction = remember { MutableInteractionSource() }
                    val isMenuHovered by menuInteraction.collectIsHoveredAsState()

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isMenuHovered || isMenuExpanded) StudioTheme.SurfaceCardDark else Color.Transparent)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable(
                                interactionSource = menuInteraction,
                                indication = ripple(bounded = false, radius = 14.dp),
                                onClick = { isMenuExpanded = true }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = if (isMenuHovered || isMenuExpanded) StudioTheme.TextPrimary else StudioTheme.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        offset = DpOffset(x = 0.dp, y = 4.dp),
                        modifier = Modifier
                            .background(StudioTheme.SurfaceCardDark)
                            .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = strings.rename,
                                    fontFamily = GoogleSansFontFamily,
                                    fontSize = 13.sp,
                                    color = StudioTheme.TextPrimary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = strings.rename,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                isMenuExpanded = false
                                onRenameClick()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = strings.settings,
                                    fontFamily = GoogleSansFontFamily,
                                    fontSize = 13.sp,
                                    color = StudioTheme.TextSecondary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = strings.settings,
                                    tint = StudioTheme.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                isMenuExpanded = false
                                onSettingsClick()
                            }
                        )

                        HorizontalDivider(
                            color = StudioTheme.BorderDark,
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = strings.delete,
                                    fontFamily = GoogleSansFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = Color(0xFFFF5252)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = strings.delete,
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                isMenuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }

            // Body Area (under the banner)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 3. Right of avatar: Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 66.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = instance.manifest.name,
                        fontFamily = GoogleSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isHovered) accentColor else StudioTheme.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 4. Bottom: Description text
                val desc = instance.manifest.description?.takeIf { it.isNotBlank() }
                    ?: "Minecraft ${instance.manifest.minecraftVersion} • ${instance.manifest.modLoader} • ${instance.manifest.totalFiles} files"

                Text(
                    text = desc,
                    fontFamily = GoogleSansFontFamily,
                    fontSize = 12.sp,
                    color = StudioTheme.TextMuted,
                    maxLines = 2,
                    lineHeight = 16.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                // Bottom row: items count / playtime & Play button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val playtimeFormatted = if (instance.manifest.totalPlayTimeSeconds > 0) {
                        "${formatPlayTime(instance.manifest.totalPlayTimeSeconds, currentLanguage)} • "
                    } else ""

                    Text(
                        text = "$playtimeFormatted${instance.manifest.totalFiles} mods/files",
                        fontFamily = GoogleSansFontFamily,
                        fontSize = 11.sp,
                        color = StudioTheme.TextSecondary
                    )

                    // Quick launch play pill
                    when {
                        isRunning -> {
                            Row(
                                modifier = Modifier
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2E7D32))
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsEsports,
                                    contentDescription = strings.gameRunning,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = strings.gameRunning,
                                    fontFamily = GoogleSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }

                        isLaunching -> {
                            Row(
                                modifier = Modifier
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accentColor)
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.8.dp,
                                    color = Color.White
                                )
                                Text(
                                    text = strings.launching,
                                    fontFamily = GoogleSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }

                        else -> {
                            val playBg by animateColorAsState(
                                targetValue = if (isHovered) accentColor else StudioTheme.SurfaceContainerDark,
                                animationSpec = tween(140)
                            )

                            Row(
                                modifier = Modifier
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(playBg)
                                    .border(1.dp, if (isHovered) accentColor else StudioTheme.BorderDark, RoundedCornerShape(8.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(color = Color.White.copy(alpha = 0.2f)),
                                        onClick = onPlayClick
                                    )
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = strings.play,
                                    tint = if (isHovered) Color.White else accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = strings.play,
                                    fontFamily = GoogleSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isHovered) Color.White else StudioTheme.TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Avatar Icon: Overlapping the banner boundary on the left
        // Banner height is 68dp, avatar is 52dp tall, positioned at y = 42dp
        Box(
            modifier = Modifier
                .offset(x = 14.dp, y = 42.dp)
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(StudioTheme.SurfaceCardDark)
                .border(2.5.dp, StudioTheme.SurfaceCardDark, RoundedCornerShape(14.dp))
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = instance.manifest.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    accentColor,
                                    accentColor.copy(alpha = 0.7f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = instance.manifest.name.take(1).uppercase(),
                        fontFamily = GoogleSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Card button allowing the user to create/import a new instance.
 * Matches the dimensions of InstanceCard (330.dp x 184.dp).
 */
@Composable
fun AddInstanceCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val borderColor by animateColorAsState(
        targetValue = if (isHovered) StudioTheme.PrimaryIndigo else StudioTheme.BorderDark,
        animationSpec = tween(150)
    )

    val bg by animateColorAsState(
        targetValue = if (isHovered) StudioTheme.HoverDark else StudioTheme.SurfaceCardDark.copy(alpha = 0.5f),
        animationSpec = tween(150)
    )

    Box(
        modifier = modifier
            .width(330.dp)
            .height(184.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.2.dp, borderColor, RoundedCornerShape(16.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = StudioTheme.PrimaryIndigo.copy(alpha = 0.15f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isHovered) StudioTheme.PrimaryIndigo else StudioTheme.SurfaceContainerDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = strings.createInstance,
                    tint = if (isHovered) Color.White else StudioTheme.TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = strings.createInstance,
                fontFamily = GoogleSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (isHovered) StudioTheme.PrimaryIndigo else StudioTheme.TextPrimary
            )
        }
    }
}
