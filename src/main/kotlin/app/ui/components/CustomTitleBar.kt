package app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import app.theme.StudioTheme
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent

/**
 * Custom modern titlebar with draggable area, double-click to maximize/restore,
 * and adaptive Minimize / Maximize / Close window control buttons.
 *
 * For undecorated windows, standard desktop maximize bounds (excluding Windows Taskbar insets)
 * are used so the window does not cover the Windows taskbar as true fullscreen.
 */
@Composable
fun WindowScope.CustomTitleBar(
    windowState: WindowState,
    onClose: () -> Unit,
    canMaximize: Boolean = false,
    canMinimize: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isMaximized by remember { mutableStateOf(false) }
    var normalBounds by remember { mutableStateOf<Rectangle?>(null) }

    DisposableEffect(window, canMaximize) {
        if (!canMaximize) return@DisposableEffect onDispose {}

        val checkMaximizedState = {
            val gc = window.graphicsConfiguration
            val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)
            val screenBounds = gc.bounds
            val targetX = screenBounds.x + insets.left
            val targetY = screenBounds.y + insets.top
            val targetW = screenBounds.width - insets.left - insets.right
            val targetH = screenBounds.height - insets.top - insets.bottom

            val current = window.bounds
            val isNowMax = current.x == targetX &&
                    current.y == targetY &&
                    current.width == targetW &&
                    current.height == targetH

            isMaximized = isNowMax
            if (!isNowMax) {
                normalBounds = current
            }
        }

        val listener = object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                checkMaximizedState()
            }
            override fun componentMoved(e: ComponentEvent?) {
                checkMaximizedState()
            }
        }

        window.addComponentListener(listener)
        checkMaximizedState()

        onDispose {
            window.removeComponentListener(listener)
        }
    }

    val toggleMaximize: () -> Unit = {
        val gc = window.graphicsConfiguration
        val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)
        val screenBounds = gc.bounds
        val targetX = screenBounds.x + insets.left
        val targetY = screenBounds.y + insets.top
        val targetW = screenBounds.width - insets.left - insets.right
        val targetH = screenBounds.height - insets.top - insets.bottom

        if (isMaximized) {
            val prev = normalBounds
            if (prev != null) {
                window.bounds = prev
            } else {
                window.setBounds(targetX + 50, targetY + 50, 960, 600)
            }
            isMaximized = false
        } else {
            normalBounds = window.bounds
            window.setBounds(targetX, targetY, targetW, targetH)
            isMaximized = true
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(StudioTheme.BackgroundDark),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Draggable window header area (double-click to toggle maximize if supported)
        WindowDraggableArea(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(
                    if (canMaximize) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    toggleMaximize()
                                }
                            )
                        }
                    } else Modifier
                )
        )

        // Window control buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxHeight()
        ) {
            // Minimize button
            if (canMinimize) {
                TitleBarBtn(
                    onClick = { windowState.isMinimized = true }
                ) { tint ->
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawLine(
                            color = tint,
                            start = Offset(0f, size.height / 2f),
                            end = Offset(size.width, size.height / 2f),
                            strokeWidth = 1.2.dp.toPx()
                        )
                    }
                }
            }

            // Maximize / Restore button (Standard Desktop Work Area, keeping taskbar visible)
            if (canMaximize) {
                TitleBarBtn(
                    onClick = toggleMaximize
                ) { tint ->
                    Canvas(modifier = Modifier.size(10.dp)) {
                        val stroke = Stroke(width = 1.2.dp.toPx())
                        if (isMaximized) {
                            // Restore icon (overlapping squares)
                            val offset = 2.5.dp.toPx()
                            val sqSize = size.width - offset
                            // Back square
                            drawRect(
                                color = tint,
                                topLeft = Offset(offset, 0f),
                                size = Size(sqSize, sqSize),
                                style = stroke
                            )
                            // Front square
                            drawRect(
                                color = tint,
                                topLeft = Offset(0f, offset),
                                size = Size(sqSize, sqSize),
                                style = stroke
                            )
                        } else {
                            // Maximize icon (single square)
                            drawRect(
                                color = tint,
                                topLeft = Offset.Zero,
                                size = size,
                                style = stroke
                            )
                        }
                    }
                }
            }

            // Close button
            TitleBarBtn(
                isClose = true,
                onClick = onClose
            ) { tint ->
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = tint,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun TitleBarBtn(
    onClick: () -> Unit,
    isClose: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable (tint: Color) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val targetBg = when {
        isHovered && isClose -> Color(0xFFE81123)
        isHovered -> StudioTheme.HoverDark
        else -> Color.Transparent
    }

    val targetTint = when {
        isHovered && isClose -> Color.White
        isHovered -> StudioTheme.TextPrimary
        else -> StudioTheme.TextSecondary
    }

    val bgColor by animateColorAsState(targetBg, animationSpec = tween(100))
    val iconTint by animateColorAsState(targetTint, animationSpec = tween(100))

    Box(
        modifier = modifier
            .width(46.dp)
            .fillMaxHeight()
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content(iconTint)
    }
}
