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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontFamily
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
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class InstanceDetailTab {
    MODS,
    OVERVIEW
}

data class ModFileInfo(
    val name: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val file: File
)

/**
 * Full-tab detailed view for a single Minecraft instance.
 * Features a dynamic banner themed to match the modpack logo's accent color.
 */
@Composable
fun InstanceDetailView(
    instance: InstalledInstance,
    onBack: () -> Unit,
    onPlay: () -> Unit = {},
    onRename: () -> Unit = {},
    onDelete: () -> Unit = {},
    onSettings: () -> Unit = {},
    isLaunching: Boolean = false,
    launchStage: String = "",
    launchProgress: Float = 0f,
    isRunning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val currentLanguage = LocalAppLanguage.current
    val defaultPrimaryColor = StudioTheme.PrimaryIndigo
    var activeSubTab by remember { mutableStateOf(InstanceDetailTab.MODS) }
    var searchQuery by remember { mutableStateOf("") }
    var isMenuExpanded by remember { mutableStateOf(false) }

    // Locate icon file
    val iconFile = remember(instance.directory, instance.manifest.iconPath) {
        val file = if (!instance.manifest.iconPath.isNullOrBlank()) {
            instance.directory.resolve(instance.manifest.iconPath).toFile()
        } else {
            instance.directory.resolve("icon.png").toFile()
        }
        if (file.exists() && file.isFile) file else null
    }

    // Extract dynamic accent color from icon
    val accentColor = remember(iconFile, defaultPrimaryColor) {
        AccentColorExtractor.extractAccentColor(iconFile, fallback = defaultPrimaryColor)
    }

    // Load instance icon bitmap
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

    // Discover installed mods in instance/mods
    val installedMods = remember(instance.directory) {
        val modsDir = instance.directory.resolve("mods").toFile()
        if (modsDir.exists() && modsDir.isDirectory) {
            modsDir.listFiles { file ->
                file.isFile && (file.name.endsWith(".jar", ignoreCase = true) || file.name.endsWith(".disabled", ignoreCase = true))
            }?.map { file ->
                ModFileInfo(
                    name = file.name,
                    sizeBytes = file.length(),
                    sizeFormatted = formatFileSize(file.length()),
                    file = file
                )
            }?.sortedBy { it.name.lowercase() } ?: emptyList()
        } else {
            emptyList()
        }
    }

    val filteredMods = remember(installedMods, searchQuery) {
        if (searchQuery.isBlank()) {
            installedMods
        } else {
            installedMods.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(StudioTheme.SurfaceCardDark)
            .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        // 1. Top Navigation Bar: Back button, Instance Title & Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back Button
            val backInteraction = remember { MutableInteractionSource() }
            val isBackHovered by backInteraction.collectIsHoveredAsState()
            val backBg by animateColorAsState(
                targetValue = if (isBackHovered) StudioTheme.HoverDark else StudioTheme.SurfaceContainerDark,
                animationSpec = tween(130)
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(backBg)
                    .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(10.dp))
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(
                        interactionSource = backInteraction,
                        indication = ripple(color = accentColor.copy(alpha = 0.2f)),
                        onClick = onBack
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.backToInstances,
                    tint = if (isBackHovered) accentColor else StudioTheme.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = strings.backToInstances,
                    fontFamily = GoogleSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = if (isBackHovered) accentColor else StudioTheme.TextPrimary
                )
            }

            // Right Action Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Open Folder Button
                val openFolderInteraction = remember { MutableInteractionSource() }
                val isOpenHovered by openFolderInteraction.collectIsHoveredAsState()
                val openBg by animateColorAsState(
                    targetValue = if (isOpenHovered) StudioTheme.HoverDark else StudioTheme.SurfaceContainerDark,
                    animationSpec = tween(130)
                )

                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(openBg)
                        .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(8.dp))
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(
                            interactionSource = openFolderInteraction,
                            indication = ripple(color = accentColor.copy(alpha = 0.2f)),
                            onClick = {
                                try {
                                    Desktop.getDesktop().open(instance.directory.toFile())
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        )
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = strings.openFolder,
                        tint = StudioTheme.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = strings.openFolder,
                        fontFamily = GoogleSansFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = StudioTheme.TextPrimary
                    )
                }

                // Play Button (State-Aware)
                when {
                    isRunning -> {
                        Row(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2E7D32))
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = strings.gameRunning,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = strings.gameRunning,
                                fontFamily = GoogleSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }

                    isLaunching -> {
                        Row(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor)
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Text(
                                text = if (launchStage.isNotBlank()) "$launchStage ${((launchProgress) * 100).toInt()}%" else strings.launching,
                                fontFamily = GoogleSansFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }

                    else -> {
                        Row(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = Color.White.copy(alpha = 0.25f)),
                                    onClick = onPlay
                                )
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = strings.play,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = strings.play,
                                fontFamily = GoogleSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // 3-dots Menu Button
                Box {
                    val menuBtnInteraction = remember { MutableInteractionSource() }
                    val isMenuHovered by menuBtnInteraction.collectIsHoveredAsState()
                    val menuBtnBg by animateColorAsState(
                        targetValue = if (isMenuHovered || isMenuExpanded) StudioTheme.HoverDark else StudioTheme.SurfaceContainerDark,
                        animationSpec = tween(130)
                    )

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(menuBtnBg)
                            .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = menuBtnInteraction,
                                indication = ripple(bounded = false, radius = 16.dp),
                                onClick = { isMenuExpanded = true }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = StudioTheme.TextSecondary,
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
                                onRename()
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
                                onSettings()
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
                                onDelete()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Hero Header Card: Icon, Name, Loader Tag, Description, Playtime, and Badges
        // Dynamically themed with the modpack logo's accent color
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.40f),
                            accentColor.copy(alpha = 0.16f),
                            StudioTheme.SurfaceContainerDark.copy(alpha = 0.85f)
                        )
                    )
                )
                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Instance Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(StudioTheme.SurfaceCardDark)
                        .border(2.dp, accentColor.copy(alpha = 0.60f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
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
                                fontSize = 26.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // Info block
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = instance.manifest.name,
                            fontFamily = GoogleSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = StudioTheme.TextPrimary
                        )

                        // Loader badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentColor.copy(alpha = 0.18f))
                                .border(1.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${instance.manifest.modLoader} ${instance.manifest.minecraftVersion}",
                                fontFamily = GoogleSansFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = accentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val desc = instance.manifest.description?.takeIf { it.isNotBlank() }
                        ?: "Minecraft ${instance.manifest.minecraftVersion} instance"

                    Text(
                        text = desc,
                        fontFamily = GoogleSansFontFamily,
                        fontSize = 13.sp,
                        color = StudioTheme.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Date, Playtime & Mod Count chips
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Playtime Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentColor.copy(alpha = 0.18f))
                                .border(1.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = strings.playtime,
                                    tint = accentColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${strings.playtime}: ${formatPlayTime(instance.manifest.totalPlayTimeSeconds, currentLanguage)}",
                                    fontFamily = GoogleSansFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp,
                                    color = accentColor
                                )
                            }
                        }

                        Text(
                            text = "${installedMods.size} mods installed",
                            fontFamily = GoogleSansFontFamily,
                            fontSize = 12.sp,
                            color = StudioTheme.TextMuted
                        )
                        Text(
                            text = "•",
                            fontFamily = GoogleSansFontFamily,
                            fontSize = 12.sp,
                            color = StudioTheme.TextMuted
                        )
                        Text(
                            text = "Created ${formatDate(instance.manifest.createdTimestamp)}",
                            fontFamily = GoogleSansFontFamily,
                            fontSize = 12.sp,
                            color = StudioTheme.TextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Sub-tabs Selector: Mods / Overview
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SubTabPill(
                    title = "${strings.modsTab} (${installedMods.size})",
                    isSelected = activeSubTab == InstanceDetailTab.MODS,
                    selectedColor = accentColor,
                    onClick = { activeSubTab = InstanceDetailTab.MODS }
                )
                SubTabPill(
                    title = strings.overviewTab,
                    isSelected = activeSubTab == InstanceDetailTab.OVERVIEW,
                    selectedColor = accentColor,
                    onClick = { activeSubTab = InstanceDetailTab.OVERVIEW }
                )
            }

            if (activeSubTab == InstanceDetailTab.MODS) {
                // Search box
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioTheme.SurfaceContainerDark)
                        .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = StudioTheme.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = strings.searchMods,
                                    fontFamily = GoogleSansFontFamily,
                                    fontSize = 12.sp,
                                    color = StudioTheme.TextMuted
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = StudioTheme.TextPrimary,
                                unfocusedTextColor = StudioTheme.TextPrimary
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = GoogleSansFontFamily,
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Content Area: Mods list or Overview details
        when (activeSubTab) {
            InstanceDetailTab.MODS -> {
                if (filteredMods.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioTheme.SurfaceContainerDark.copy(alpha = 0.4f))
                            .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Extension,
                                contentDescription = null,
                                tint = StudioTheme.TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No mods matching \"$searchQuery\"" else strings.noModsFound,
                                fontFamily = GoogleSansFontFamily,
                                fontSize = 13.sp,
                                color = StudioTheme.TextMuted
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioTheme.SurfaceContainerDark.copy(alpha = 0.4f))
                            .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredMods, key = { it.name }) { mod ->
                            ModItemRow(mod = mod, accentColor = accentColor)
                        }
                    }
                }
            }

            InstanceDetailTab.OVERVIEW -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioTheme.SurfaceContainerDark.copy(alpha = 0.4f))
                        .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Instance Details",
                        fontFamily = GoogleSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = StudioTheme.TextPrimary
                    )

                    HorizontalDivider(color = StudioTheme.BorderDark, thickness = 0.8.dp)

                    SelectionContainer {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            InfoRow(title = "Name", value = instance.manifest.name)
                            InfoRow(title = "Minecraft Version", value = instance.manifest.minecraftVersion)
                            InfoRow(title = "Mod Loader", value = "${instance.manifest.modLoader} ${instance.manifest.modLoaderVersion}")
                            InfoRow(title = "Total Files", value = "${instance.manifest.totalFiles}")
                            InfoRow(title = "Installed Mods", value = "${installedMods.size} active .jar files")
                            InfoRow(title = strings.playtime, value = formatPlayTime(instance.manifest.totalPlayTimeSeconds, currentLanguage))
                            if (instance.manifest.lastPlayedTimestamp > 0) {
                                InfoRow(title = strings.lastPlayed, value = formatDate(instance.manifest.lastPlayedTimestamp))
                            }
                            InfoRow(title = "Created", value = formatDate(instance.manifest.createdTimestamp))
                            InfoRow(title = "Directory", value = instance.directory.toAbsolutePath().toString())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubTabPill(
    title: String,
    isSelected: Boolean,
    selectedColor: Color = StudioTheme.PrimaryIndigo,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (isSelected) selectedColor else StudioTheme.SurfaceContainerDark,
        animationSpec = tween(120)
    )

    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, if (isSelected) selectedColor else StudioTheme.BorderDark, RoundedCornerShape(8.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontFamily = GoogleSansFontFamily,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp,
            color = if (isSelected) Color.White else StudioTheme.TextSecondary
        )
    }
}

@Composable
private fun ModItemRow(
    mod: ModFileInfo,
    accentColor: Color = StudioTheme.PrimaryIndigo
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bg by animateColorAsState(
        targetValue = if (isHovered) StudioTheme.HoverDark else StudioTheme.SurfaceCardDark,
        animationSpec = tween(100)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, if (isHovered) StudioTheme.BorderDark else Color.Transparent, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Extension,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = mod.name,
                fontFamily = GoogleSansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = StudioTheme.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = mod.sizeFormatted,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = StudioTheme.TextMuted
        )
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "$title:",
            fontFamily = GoogleSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = StudioTheme.TextSecondary,
            modifier = Modifier.width(130.dp)
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = StudioTheme.TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    val kilo = 1024.0
    val mega = kilo * 1024.0
    return when {
        bytes >= mega -> String.format(Locale.US, "%.2f MB", bytes / mega)
        bytes >= kilo -> String.format(Locale.US, "%.1f KB", bytes / kilo)
        else -> "$bytes B"
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
