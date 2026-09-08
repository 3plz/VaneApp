package app.ui.main.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.domain.launcher.LaunchStatus
import app.domain.storage.InstalledInstance
import app.i18n.LocalAppLanguage
import app.i18n.formatPlayTime
import app.i18n.strings
import app.theme.GoogleSansFontFamily
import app.theme.StudioTheme

/**
 * Instances tab content displaying either:
 * 1. Big centered plus button when no instances are installed.
 * 2. Visual grid of compact horizontal rectangular instance cards with playtime tracking.
 * 3. Full-tab detailed view when an instance is opened.
 * 4. Interactive Rename and Delete dialogs.
 * 5. Minecraft launch progress and status indicators.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InstancesTabContent(
    instances: List<InstalledInstance> = emptyList(),
    onCreateInstanceClick: () -> Unit = {},
    onInstanceClick: (InstalledInstance) -> Unit = {},
    onPlayInstance: (InstalledInstance) -> Unit = {},
    onRenameInstance: (InstalledInstance, String) -> Unit = { _, _ -> },
    onDeleteInstance: (InstalledInstance) -> Unit = {},
    launchStatus: LaunchStatus = LaunchStatus.Idle,
    launchingInstanceId: String? = null,
    runningInstanceId: String? = null,
    onDismissLaunchError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentLanguage = LocalAppLanguage.current
    val totalPlayTimeSeconds = remember(instances) { instances.sumOf { it.manifest.totalPlayTimeSeconds } }

    var selectedInstanceId by remember { mutableStateOf<String?>(null) }
    val selectedInstance = remember(instances, selectedInstanceId) {
        instances.firstOrNull { it.manifest.id == selectedInstanceId }
    }

    var instanceToRename by remember { mutableStateOf<InstalledInstance?>(null) }
    var renameInput by remember { mutableStateOf("") }

    var instanceToDelete by remember { mutableStateOf<InstalledInstance?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = selectedInstance,
            transitionSpec = {
                fadeIn(tween(160)).togetherWith(fadeOut(tween(130)))
            },
            modifier = Modifier.fillMaxSize()
        ) { currentInstance ->
            if (currentInstance != null) {
                val isInstanceLaunching = launchStatus is LaunchStatus.Preparing && launchingInstanceId == currentInstance.manifest.id
                val isInstanceRunning = runningInstanceId == currentInstance.manifest.id
                val stage = if (isInstanceLaunching) (launchStatus as LaunchStatus.Preparing).stage else ""
                val progress = if (isInstanceLaunching) (launchStatus as LaunchStatus.Preparing).percentage else 0f

                // Full-Tab Detailed View for the selected instance
                InstanceDetailView(
                    instance = currentInstance,
                    onBack = { selectedInstanceId = null },
                    onPlay = { onPlayInstance(currentInstance) },
                    onRename = {
                        instanceToRename = currentInstance
                        renameInput = currentInstance.manifest.name
                    },
                    onDelete = {
                        instanceToDelete = currentInstance
                    },
                    isLaunching = isInstanceLaunching,
                    launchStage = stage,
                    launchProgress = progress,
                    isRunning = isInstanceRunning,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (instances.isEmpty()) {
                // Empty State: Big plus button in the center
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(StudioTheme.SurfaceCardDark)
                        .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(16.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isHovered by interactionSource.collectIsHoveredAsState()

                    val scale by animateFloatAsState(
                        targetValue = if (isHovered) 1.06f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)
                    )

                    val circleBg by animateColorAsState(
                        targetValue = if (isHovered) {
                            StudioTheme.PrimaryIndigo
                        } else {
                            StudioTheme.PrimaryIndigo.copy(alpha = 0.14f)
                        },
                        animationSpec = tween(160)
                    )

                    val circleBorder by animateColorAsState(
                        targetValue = if (isHovered) {
                            StudioTheme.PrimaryIndigoLight
                        } else {
                            StudioTheme.PrimaryIndigo.copy(alpha = 0.35f)
                        },
                        animationSpec = tween(160)
                    )

                    val plusTint by animateColorAsState(
                        targetValue = if (isHovered) Color.White else StudioTheme.PrimaryIndigo,
                        animationSpec = tween(160)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = ripple(
                                    color = StudioTheme.PrimaryIndigo.copy(alpha = 0.25f),
                                    bounded = false,
                                    radius = 80.dp
                                ),
                                onClick = onCreateInstanceClick
                            )
                            .padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(circleBg)
                                .border(2.dp, circleBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = strings.createInstance,
                                tint = plusTint,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = strings.createInstance,
                            fontFamily = GoogleSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = if (isHovered) StudioTheme.PrimaryIndigo else StudioTheme.TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = strings.createInstanceHint,
                            fontFamily = GoogleSansFontFamily,
                            fontSize = 13.sp,
                            color = StudioTheme.TextMuted
                        )
                    }
                }
            } else {
                // Populated State: Grid of compact horizontal instance cards
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(StudioTheme.SurfaceCardDark)
                        .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    // Header: Title with counter, Total Playtime, and "+ New Instance" button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Instances",
                                fontFamily = GoogleSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = StudioTheme.TextPrimary
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StudioTheme.PrimaryIndigo.copy(alpha = 0.15f))
                                    .border(1.dp, StudioTheme.PrimaryIndigo.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${instances.size}",
                                    fontFamily = GoogleSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = StudioTheme.PrimaryIndigo
                                )
                            }

                            // Total Play Time Badge across all instances
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StudioTheme.SurfaceContainerDark)
                                    .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = strings.totalPlaytime,
                                        tint = StudioTheme.PrimaryIndigo,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${strings.totalPlaytime}: ${formatPlayTime(totalPlayTimeSeconds, currentLanguage)}",
                                        fontFamily = GoogleSansFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = StudioTheme.TextPrimary
                                    )
                                }
                            }
                        }

                        // Add button
                        val addInteraction = remember { MutableInteractionSource() }
                        val isAddHovered by addInteraction.collectIsHoveredAsState()
                        val addBg by animateColorAsState(
                            targetValue = if (isAddHovered) StudioTheme.PrimaryIndigo else StudioTheme.SurfaceContainerDark,
                            animationSpec = tween(140)
                        )

                        Row(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(addBg)
                                .border(1.dp, if (isAddHovered) StudioTheme.PrimaryIndigo else StudioTheme.BorderDark, RoundedCornerShape(8.dp))
                                .pointerHoverIcon(PointerIcon.Hand)
                                .clickable(
                                    interactionSource = addInteraction,
                                    indication = ripple(color = Color.White.copy(alpha = 0.2f)),
                                    onClick = onCreateInstanceClick
                                )
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = strings.createInstance,
                                tint = if (isAddHovered) Color.White else StudioTheme.PrimaryIndigo,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = strings.createInstance,
                                fontFamily = GoogleSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isAddHovered) Color.White else StudioTheme.TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Instances Flow Grid
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        instances.forEach { instance ->
                            val isInstanceLaunching = launchStatus is LaunchStatus.Preparing && launchingInstanceId == instance.manifest.id
                            val isInstanceRunning = runningInstanceId == instance.manifest.id

                            InstanceCard(
                                instance = instance,
                                onClick = {
                                    selectedInstanceId = instance.manifest.id
                                    onInstanceClick(instance)
                                },
                                onPlayClick = { onPlayInstance(instance) },
                                onRenameClick = {
                                    instanceToRename = instance
                                    renameInput = instance.manifest.name
                                },
                                onDeleteClick = {
                                    instanceToDelete = instance
                                },
                                isLaunching = isInstanceLaunching,
                                isRunning = isInstanceRunning
                            )
                        }

                        // Append AddInstanceCard at the end of the list
                        AddInstanceCard(
                            onClick = onCreateInstanceClick
                        )
                    }
                }
            }
        }

        // Floating Launch Error Notification
        if (launchStatus is LaunchStatus.Failed) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2C1515))
                    .border(1.dp, Color(0xFFE53935).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = strings.launchFailed,
                            fontFamily = GoogleSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            text = launchStatus.error,
                            fontFamily = GoogleSansFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFFFFCDD2),
                            maxLines = 2
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDismissLaunchError),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Modal Dialog: Rename Instance
        if (instanceToRename != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { instanceToRename = null }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(420.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(StudioTheme.SurfaceCardDark)
                        .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {} // Keep dialog open
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(StudioTheme.PrimaryIndigo.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = StudioTheme.PrimaryIndigo,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = strings.renameInstanceTitle,
                                    fontFamily = GoogleSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = StudioTheme.TextPrimary
                                )
                                Text(
                                    text = strings.renameInstancePrompt,
                                    fontFamily = GoogleSansFontFamily,
                                    fontSize = 12.sp,
                                    color = StudioTheme.TextMuted
                                )
                            }
                        }

                        OutlinedTextField(
                            value = renameInput,
                            onValueChange = { renameInput = it },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = StudioTheme.TextPrimary,
                                unfocusedTextColor = StudioTheme.TextPrimary,
                                focusedBorderColor = StudioTheme.PrimaryIndigo,
                                unfocusedBorderColor = StudioTheme.BorderDark,
                                focusedContainerColor = StudioTheme.SurfaceContainerDark,
                                unfocusedContainerColor = StudioTheme.SurfaceContainerDark
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Cancel
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StudioTheme.SurfaceContainerDark)
                                    .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(10.dp))
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable { instanceToRename = null },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strings.cancel,
                                    fontFamily = GoogleSansFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = StudioTheme.TextSecondary
                                )
                            }

                            // Save
                            val canSave = renameInput.isNotBlank() && renameInput.trim() != instanceToRename!!.manifest.name
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (canSave) StudioTheme.PrimaryIndigo else StudioTheme.PrimaryIndigo.copy(alpha = 0.4f))
                                    .pointerHoverIcon(if (canSave) PointerIcon.Hand else PointerIcon.Default)
                                    .clickable(enabled = canSave) {
                                        onRenameInstance(instanceToRename!!, renameInput.trim())
                                        instanceToRename = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strings.save,
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
        }

        // Modal Dialog: Delete Instance Confirmation
        if (instanceToDelete != null) {
            val inst = instanceToDelete!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { instanceToDelete = null }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(420.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(StudioTheme.SurfaceCardDark)
                        .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {} // Keep dialog open
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE53935).copy(alpha = 0.15f))
                                    .border(1.dp, Color(0xFFE53935).copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.deleteInstanceTitle,
                                    fontFamily = GoogleSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = StudioTheme.TextPrimary
                                )
                                Text(
                                    text = inst.manifest.name,
                                    fontFamily = GoogleSansFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFFF6B6B)
                                )
                            }
                        }

                        Text(
                            text = strings.deleteInstancePrompt,
                            fontFamily = GoogleSansFontFamily,
                            fontSize = 12.sp,
                            color = StudioTheme.TextSecondary,
                            lineHeight = 17.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Cancel
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StudioTheme.SurfaceContainerDark)
                                    .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(10.dp))
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable { instanceToDelete = null },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strings.cancel,
                                    fontFamily = GoogleSansFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = StudioTheme.TextSecondary
                                )
                            }

                            // Confirm Delete
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE53935))
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable {
                                        if (selectedInstanceId == inst.manifest.id) {
                                            selectedInstanceId = null
                                        }
                                        onDeleteInstance(inst)
                                        instanceToDelete = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strings.delete,
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
        }
    }
}
