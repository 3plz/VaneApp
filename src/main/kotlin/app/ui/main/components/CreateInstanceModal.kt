package app.ui.main.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.domain.modpack.*
import app.i18n.strings
import app.theme.GoogleSansFontFamily
import app.theme.StudioTheme
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image as SkiaImage
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.nio.file.Path

/**
 * Modal dialog for creating or importing a Minecraft instance.
 * Allows choosing between "Import" (.zip, .mrpack) and "Create from scratch".
 * Supports inspecting .mrpack metadata, resolving modpack icons from Modrinth,
 * viewing raw modrinth.index.json, and installing the instance with real-time download progress.
 */
@Composable
fun CreateInstanceModal(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCreateFromScratchClick: () -> Unit = {},
    onInstanceCreated: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val installer = remember { InstanceInstaller() }

    var pickedFile by remember { mutableStateOf<File?>(null) }
    var inspectedPack by remember { mutableStateOf<MrpackInfo?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var showRawJson by remember { mutableStateOf(false) }
    var copyFeedback by remember { mutableStateOf(false) }

    // Installation flow state
    var isInstalling by remember { mutableStateOf(false) }
    var installProgress by remember { mutableStateOf<InstallProgress?>(null) }
    var installedPath by remember { mutableStateOf<Path?>(null) }
    var installError by remember { mutableStateOf<String?>(null) }

    // Reset internal state whenever modal is closed
    LaunchedEffect(visible) {
        if (!visible) {
            pickedFile = null
            inspectedPack = null
            parseError = null
            showRawJson = false
            copyFeedback = false
            isInstalling = false
            installProgress = null
            installedPath = null
            installError = null
        }
    }

    // Automatically resolve and download Modrinth icon if not bundled in the archive
    LaunchedEffect(inspectedPack?.name) {
        val pack = inspectedPack ?: return@LaunchedEffect
        if (pack.iconBytes == null) {
            val iconUrl = ModrinthIconService.resolveIconUrl(pack.versionId, pack.name)
            if (iconUrl != null) {
                val bytes = ModrinthIconService.downloadIconBytes(iconUrl)
                if (bytes != null) {
                    inspectedPack = pack.copy(iconBytes = bytes, iconUrl = iconUrl)
                }
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(140))
    ) {
        // Scrim overlay
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (!isInstalling) onDismiss()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Modal dialog card
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(initialScale = 0.94f, animationSpec = tween(180)) + fadeIn(tween(180)),
                exit = scaleOut(targetScale = 0.94f, animationSpec = tween(140)) + fadeOut(tween(140))
            ) {
                Box(
                    modifier = Modifier
                        .width(if (inspectedPack != null || installedPath != null) 640.dp else 520.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(StudioTheme.SurfaceCardDark)
                        .border(1.2.dp, StudioTheme.BorderDark, RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {} // Keep clicks inside dialog
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header bar: back button (if inspector is active and not installing), title, close button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if ((inspectedPack != null || parseError != null) && !isInstalling && installedPath == null) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(StudioTheme.SurfaceContainerDark)
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .clickable {
                                            pickedFile = null
                                            inspectedPack = null
                                            parseError = null
                                            showRawJson = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = strings.back,
                                        tint = StudioTheme.TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(32.dp))
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = when {
                                        installedPath != null -> strings.installationComplete
                                        isInstalling -> strings.installingInstance
                                        inspectedPack != null -> strings.modpackInspectorTitle
                                        else -> strings.newInstanceDialogTitle
                                    },
                                    fontFamily = GoogleSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = StudioTheme.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when {
                                        installedPath != null -> installedPath!!.fileName.toString()
                                        isInstalling -> installProgress?.stage ?: strings.installingInstance
                                        inspectedPack != null -> strings.modpackInspectorSubtitle
                                        else -> strings.newInstanceDialogSubtitle
                                    },
                                    fontFamily = GoogleSansFontFamily,
                                    fontSize = 12.sp,
                                    color = StudioTheme.TextMuted
                                )
                            }

                            // Close button
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(StudioTheme.SurfaceContainerDark)
                                    .pointerHoverIcon(if (isInstalling) PointerIcon.Default else PointerIcon.Hand)
                                    .clickable(enabled = !isInstalling) { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = strings.close,
                                    tint = if (isInstalling) StudioTheme.BorderDark else StudioTheme.TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Dynamic body switch:
                        when {
                            // 1. Installation Complete state
                            installedPath != null -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(StudioTheme.PrimaryIndigo.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = StudioTheme.PrimaryIndigo,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Text(
                                        text = strings.installationComplete,
                                        fontFamily = GoogleSansFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = StudioTheme.TextPrimary
                                    )

                                    Text(
                                        text = installedPath!!.toAbsolutePath().toString(),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = StudioTheme.TextMuted,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(StudioTheme.SurfaceContainerDark)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Open folder button
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(StudioTheme.SurfaceContainerDark)
                                                .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(10.dp))
                                                .pointerHoverIcon(PointerIcon.Hand)
                                                .clickable {
                                                    try {
                                                        Desktop.getDesktop().open(installedPath!!.toFile())
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.FolderOpen,
                                                    contentDescription = null,
                                                    tint = StudioTheme.TextSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = strings.openFolder,
                                                    fontFamily = GoogleSansFontFamily,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 13.sp,
                                                    color = StudioTheme.TextPrimary
                                                )
                                            }
                                        }

                                        // Done button
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(StudioTheme.PrimaryIndigo)
                                                .pointerHoverIcon(PointerIcon.Hand)
                                                .clickable {
                                                    onInstanceCreated()
                                                    onDismiss()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = strings.done,
                                                fontFamily = GoogleSansFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Installing state
                            isInstalling -> {
                                val progress = installProgress
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(StudioTheme.SurfaceContainerDark)
                                        .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(12.dp))
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = progress?.stage ?: strings.installingInstance,
                                            fontFamily = GoogleSansFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = StudioTheme.TextPrimary
                                        )
                                        Text(
                                            text = "${((progress?.percentage ?: 0f) * 100).toInt()}%",
                                            fontFamily = GoogleSansFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = StudioTheme.PrimaryIndigo
                                        )
                                    }

                                    LinearProgressIndicator(
                                        progress = { progress?.percentage ?: 0f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = StudioTheme.PrimaryIndigo,
                                        trackColor = StudioTheme.BackgroundDark
                                    )

                                    if (progress != null) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = progress.currentItem,
                                                fontFamily = GoogleSansFontFamily,
                                                fontSize = 12.sp,
                                                color = StudioTheme.TextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "${progress.completedItems} / ${progress.totalItems}",
                                                fontFamily = GoogleSansFontFamily,
                                                fontSize = 12.sp,
                                                color = StudioTheme.TextSecondary
                                            )
                                        }
                                    }

                                    if (installError != null) {
                                        Text(
                                            text = installError!!,
                                            fontFamily = GoogleSansFontFamily,
                                            fontSize = 12.sp,
                                            color = Color(0xFFFF6B6B)
                                        )
                                    }
                                }
                            }

                            inspectedPack != null -> {
                                val pack = inspectedPack!!

                                val packIconBitmap = remember(pack.iconBytes) {
                                    pack.iconBytes?.let { bytes ->
                                        try {
                                            SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
                                        } catch (_: Exception) {
                                            null
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Pack title, icon avatar and version
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // Modpack icon / avatar
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(StudioTheme.SurfaceContainerDark)
                                                .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (packIconBitmap != null) {
                                                Image(
                                                    bitmap = packIconBitmap,
                                                    contentDescription = pack.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                            Brush.linearGradient(
                                                                colors = listOf(
                                                                    StudioTheme.PrimaryIndigo,
                                                                    StudioTheme.PrimaryIndigoLight
                                                                )
                                                            )
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = pack.name.take(1).uppercase(),
                                                        fontFamily = GoogleSansFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 22.sp,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = pack.name,
                                                fontFamily = GoogleSansFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = StudioTheme.TextPrimary
                                            )
                                            if (!pack.versionId.isNullOrBlank()) {
                                                Text(
                                                    text = "v${pack.versionId}",
                                                    fontFamily = GoogleSansFontFamily,
                                                    fontSize = 12.sp,
                                                    color = StudioTheme.PrimaryIndigo
                                                )
                                            }
                                        }

                                        // Archive filename pill
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(StudioTheme.SurfaceContainerDark)
                                                .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = "${pack.fileName} (${pack.fileSizeFormatted})",
                                                fontFamily = GoogleSansFontFamily,
                                                fontSize = 11.sp,
                                                color = StudioTheme.TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Summary if present
                                    if (!pack.summary.isNullOrBlank()) {
                                        Text(
                                            text = pack.summary,
                                            fontFamily = GoogleSansFontFamily,
                                            fontSize = 13.sp,
                                            color = StudioTheme.TextSecondary,
                                            lineHeight = 18.sp
                                        )
                                    }

                                    // Metadata chips grid
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        MetadataChip(
                                            title = "Minecraft",
                                            value = pack.minecraftVersion,
                                            modifier = Modifier.weight(1f)
                                        )
                                        MetadataChip(
                                            title = "Mod Loader",
                                            value = if (pack.modLoaderVersion.isNotBlank()) "${pack.modLoader} (${pack.modLoaderVersion})" else pack.modLoader,
                                            modifier = Modifier.weight(1f)
                                        )
                                        MetadataChip(
                                            title = "Files",
                                            value = "${pack.totalFiles} mods (${pack.totalDownloadSizeFormatted})",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // Raw JSON toggle & copy bar
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { showRawJson = !showRawJson }
                                                .padding(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (showRawJson) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = StudioTheme.PrimaryIndigo,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = strings.rawJsonTitle,
                                                fontFamily = GoogleSansFontFamily,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp,
                                                color = StudioTheme.PrimaryIndigo
                                            )
                                        }

                                        if (showRawJson) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(StudioTheme.SurfaceContainerDark)
                                                    .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(6.dp))
                                                    .pointerHoverIcon(PointerIcon.Hand)
                                                    .clickable {
                                                        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                                                        clipboard.setContents(StringSelection(pack.rawJson), null)
                                                        copyFeedback = true
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (copyFeedback) strings.copiedToClipboard else strings.copyJson,
                                                    fontFamily = GoogleSansFontFamily,
                                                    fontSize = 11.sp,
                                                    color = if (copyFeedback) StudioTheme.PrimaryIndigo else StudioTheme.TextSecondary
                                                )
                                            }
                                        }
                                    }

                                    if (showRawJson) {
                                        SelectionContainer {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 160.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(StudioTheme.SurfaceContainerDark)
                                                    .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(8.dp))
                                                    .padding(10.dp)
                                                    .verticalScroll(rememberScrollState())
                                            ) {
                                                Text(
                                                    text = pack.rawJson,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    color = StudioTheme.TextSecondary,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Action buttons: "Choose another file" and "Install Instance"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Choose another file
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(StudioTheme.SurfaceContainerDark)
                                                .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(10.dp))
                                                .pointerHoverIcon(PointerIcon.Hand)
                                                .clickable {
                                                    openArchivePicker { file ->
                                                        pickedFile = file
                                                        val result = MrpackReader.parse(file)
                                                        if (result.isSuccess) {
                                                            inspectedPack = result.getOrNull()
                                                            parseError = null
                                                        } else {
                                                            inspectedPack = null
                                                            parseError = result.exceptionOrNull()?.message ?: "Unknown error"
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = strings.chooseAnotherFile,
                                                fontFamily = GoogleSansFontFamily,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = StudioTheme.TextSecondary
                                            )
                                        }

                                        // Install Instance button
                                        Box(
                                            modifier = Modifier
                                                .weight(1.3f)
                                                .height(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(StudioTheme.PrimaryIndigo)
                                                .pointerHoverIcon(PointerIcon.Hand)
                                                .clickable {
                                                    val file = pickedFile ?: return@clickable
                                                    isInstalling = true
                                                    installError = null
                                                    coroutineScope.launch {
                                                        val result = installer.installFromMrpack(
                                                            mrpackFile = file,
                                                            packInfo = pack,
                                                            onProgress = { progress ->
                                                                installProgress = progress
                                                            }
                                                        )
                                                        isInstalling = false
                                                        if (result.isSuccess) {
                                                            installedPath = result.getOrNull()
                                                            onInstanceCreated()
                                                        } else {
                                                            installError = result.exceptionOrNull()?.message ?: "Installation failed"
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Download,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = strings.installInstance,
                                                    fontFamily = GoogleSansFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            parseError != null -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(40.dp)
                                    )

                                    Text(
                                        text = strings.invalidFile,
                                        fontFamily = GoogleSansFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = StudioTheme.TextPrimary
                                    )

                                    Text(
                                        text = parseError!!,
                                        fontFamily = GoogleSansFontFamily,
                                        fontSize = 12.sp,
                                        color = StudioTheme.TextMuted,
                                        textAlign = TextAlign.Center
                                    )

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(StudioTheme.PrimaryIndigo)
                                            .pointerHoverIcon(PointerIcon.Hand)
                                            .clickable {
                                                openArchivePicker { file ->
                                                    pickedFile = file
                                                    val result = MrpackReader.parse(file)
                                                    if (result.isSuccess) {
                                                        inspectedPack = result.getOrNull()
                                                        parseError = null
                                                    } else {
                                                        inspectedPack = null
                                                        parseError = result.exceptionOrNull()?.message ?: "Unknown error"
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = strings.chooseAnotherFile,
                                            fontFamily = GoogleSansFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            else -> {
                                // Default Initial View: 2 Split Options
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // 1. Import Option Card
                                    CreateOptionCard(
                                        icon = Icons.Default.FileDownload,
                                        title = strings.importInstanceTitle,
                                        subtitle = strings.importInstanceSubtitle,
                                        onClick = {
                                            openArchivePicker { file ->
                                                pickedFile = file
                                                val result = MrpackReader.parse(file)
                                                if (result.isSuccess) {
                                                    inspectedPack = result.getOrNull()
                                                    parseError = null
                                                } else {
                                                    inspectedPack = null
                                                    parseError = result.exceptionOrNull()?.message ?: "Unknown error"
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                    // 2. Create from Scratch Option Card
                                    CreateOptionCard(
                                        icon = Icons.Default.BuildCircle,
                                        title = strings.createFromScratchTitle,
                                        subtitle = strings.createFromScratchSubtitle,
                                        onClick = onCreateFromScratchClick,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataChip(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(StudioTheme.SurfaceContainerDark)
            .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontFamily = GoogleSansFontFamily,
            fontSize = 10.sp,
            color = StudioTheme.TextMuted
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontFamily = GoogleSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = StudioTheme.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CreateOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val cardBg = if (isHovered) StudioTheme.HoverDark else StudioTheme.SurfaceContainerDark
    val borderCol = if (isHovered) StudioTheme.PrimaryIndigo else StudioTheme.BorderDark

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.2.dp, borderCol, RoundedCornerShape(16.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = StudioTheme.PrimaryIndigo.copy(alpha = 0.2f)),
                onClick = onClick
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (isHovered) StudioTheme.PrimaryIndigo else StudioTheme.PrimaryIndigo.copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isHovered) Color.White else StudioTheme.PrimaryIndigo,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = title,
            fontFamily = GoogleSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = StudioTheme.TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            fontFamily = GoogleSansFontFamily,
            fontSize = 12.sp,
            color = StudioTheme.TextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

/**
 * Native Windows FileDialog picker for .mrpack and .zip archives.
 */
private fun openArchivePicker(onFileSelected: (File) -> Unit) {
    val dialog = FileDialog(null as Frame?, "Select Modpack (.mrpack, .zip)", FileDialog.LOAD).apply {
        isMultipleMode = false
        setFilenameFilter { _, name ->
            name.endsWith(".mrpack", ignoreCase = true) || name.endsWith(".zip", ignoreCase = true)
        }
        isVisible = true
    }

    val dir = dialog.directory
    val file = dialog.file
    if (dir != null && file != null) {
        onFileSelected(File(dir, file))
    }
}
