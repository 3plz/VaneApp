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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    userCode: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val btnInteraction = remember { MutableInteractionSource() }
    val isBtnHovered by btnInteraction.collectIsHoveredAsState()

    val btnBg by animateColorAsState(
        targetValue = if (isBtnHovered) StudioTheme.HoverDark else StudioTheme.SurfaceCardDark,
        animationSpec = tween(150)
    )
    val btnBorder by animateColorAsState(
        targetValue = if (isBtnHovered) StudioTheme.PrimaryIndigoLight else StudioTheme.BorderDark,
        animationSpec = tween(150)
    )

    Surface(
        modifier = modifier
            .width(240.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = btnInteraction,
                indication = ripple(color = StudioTheme.PrimaryIndigo.copy(alpha = 0.2f)),
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
                    color = StudioTheme.PrimaryIndigo,
                    strokeWidth = 2.dp
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = if (userCode != null) "Code: $userCode" else strings.waitingInBrowser,
                    fontFamily = GoogleSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = StudioTheme.PrimaryIndigo,
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
                    fontSize = 13.sp,
                    color = StudioTheme.TextPrimary,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}
