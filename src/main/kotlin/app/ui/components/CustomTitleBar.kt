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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowState
import app.theme.StudioTheme

@Composable
fun FrameWindowScope.CustomTitleBar(
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
        // Перетаскивание окна за шапку
        WindowDraggableArea(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )

        // Кнопки управления окном (Свернуть и Закрыть)
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
    isClose: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bg by animateColorAsState(
        targetValue = when {
            isHovered && isClose -> Color(0xFFE81123)
            isHovered -> StudioTheme.HoverDark
            else -> Color.Transparent
        },
        animationSpec = tween(120)
    )

    val tint by animateColorAsState(
        targetValue = when {
            isHovered && isClose -> Color.White
            isHovered -> StudioTheme.TextPrimary
            else -> StudioTheme.TextSecondary
        },
        animationSpec = tween(120)
    )

    Box(
        modifier = Modifier
            .width(38.dp)
            .fillMaxHeight()
            .background(bg)
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
            tint = tint,
            modifier = Modifier.size(13.dp)
        )
    }
}
