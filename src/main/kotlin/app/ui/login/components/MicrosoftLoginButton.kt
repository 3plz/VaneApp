package app.ui.login.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.i18n.strings
import app.theme.GoogleSansFontFamily
import app.theme.StudioTheme

@Suppress("DEPRECATION")
@Composable
fun MicrosoftLoginButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val btnInteraction = remember { MutableInteractionSource() }
    val isBtnHovered by btnInteraction.collectIsHoveredAsState()

    val btnBg by animateColorAsState(
        targetValue = if (isBtnHovered) Color(0xFF262B38) else Color(0xFF1E222E),
        animationSpec = tween(150)
    )
    val btnBorder by animateColorAsState(
        targetValue = if (isBtnHovered) Color(0xFF434A5E) else Color(0xFF2E3342),
        animationSpec = tween(150)
    )

    Surface(
        modifier = modifier
            .width(240.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = btnInteraction,
                indication = ripple(color = Color.White.copy(alpha = 0.2f)),
                enabled = !isLoading,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = btnBg,
        border = BorderStroke(1.dp, btnBorder)
    ) {
        if (isLoading) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color(0xFF60A5FA),
                    strokeWidth = 2.dp
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = strings.waitingInBrowser,
                    fontFamily = GoogleSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = Color(0xFF60A5FA),
                    letterSpacing = 0.2.sp
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource("Microsoft_icon.svg"),
                    contentDescription = "Microsoft",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = strings.loginWithMicrosoft,
                    fontFamily = GoogleSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.5.sp,
                    color = StudioTheme.TextPrimary,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}
