package app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import app.theme.StudioTheme

@Composable
fun WindowScope.CustomTitleBar(
    windowState: WindowState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(StudioTheme.BackgroundDark),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Draggable window header area
        WindowDraggableArea(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )

        // Window control buttons (Minimize and Close)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxHeight()
        ) {
            TitleBarBtn(
                icon = Icons.Default.Minimize,
                onClick = { windowState.isMinimized = true }
            )
            TitleBarBtn(
                icon = Icons.Default.Close,
                isClose = true,
                onClick = onClose
            )
        }
    }
}

@Composable
private fun TitleBarBtn(
    icon: ImageVector,
    onClick: () -> Unit,
    isClose: Boolean = false,
    modifier: Modifier = Modifier
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(if (icon == Icons.Default.Minimize) 14.dp else 16.dp)
        )
    }
}
