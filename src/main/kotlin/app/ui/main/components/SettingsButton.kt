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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.i18n.strings
import app.theme.GoogleSansFontFamily
import app.theme.StudioTheme

/**
 * Settings pill button displaying the settings SVG gear icon and localized "Settings" / "Настройки" label.
 */
@Suppress("DEPRECATION")
@Composable
fun SettingsButton(
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bg by animateColorAsState(
        targetValue = if (isHovered) StudioTheme.HoverDark else StudioTheme.SurfaceCardDark,
        animationSpec = tween(150)
    )
    val border by animateColorAsState(
        targetValue = if (isHovered) StudioTheme.PrimaryIndigoLight else StudioTheme.BorderDark,
        animationSpec = tween(150)
    )
    val tint by animateColorAsState(
        targetValue = if (isHovered) StudioTheme.TextPrimary else StudioTheme.TextSecondary,
        animationSpec = tween(150)
    )

    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = StudioTheme.PrimaryIndigo.copy(alpha = 0.2f)),
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource("settings-icon.svg"),
            contentDescription = strings.settings,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = strings.settings,
            fontFamily = GoogleSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = tint
        )
    }
}
