package app.ui.login.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.i18n.strings
import app.theme.GoogleSansFontFamily
import app.theme.StudioTheme
import app.ui.components.PlayerAvatar

private val MINECRAFT_USERNAME_REGEX = Regex("^[a-zA-Z0-9_]{3,16}$")

/**
 * Modern card for creating/entering an offline Minecraft profile with live skin avatar head preview.
 */
@Composable
fun OfflineLoginCard(
    onConfirm: (username: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val isValid = remember(username) { MINECRAFT_USERNAME_REGEX.matches(username) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .width(320.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(StudioTheme.SurfaceCardDark)
            .border(1.dp, StudioTheme.BorderDark, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Card Header: back button & title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val backInteraction = remember { MutableInteractionSource() }
            val isBackHovered by backInteraction.collectIsHoveredAsState()

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isBackHovered) StudioTheme.HoverDark else Color.Transparent)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(
                        interactionSource = backInteraction,
                        indication = ripple(color = StudioTheme.TextPrimary),
                        onClick = onBack
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.back,
                    tint = StudioTheme.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = strings.createOfflineAccount,
                fontFamily = GoogleSansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.5.sp,
                color = StudioTheme.TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Custom stylized input field with live player head avatar
        val inputInteraction = remember { MutableInteractionSource() }
        val isFocused by inputInteraction.collectIsFocusedAsState()

        val inputBorder by animateColorAsState(
            targetValue = when {
                isFocused -> StudioTheme.PrimaryIndigo
                else -> StudioTheme.BorderDark
            },
            animationSpec = tween(150)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(12.dp),
            color = StudioTheme.BackgroundDark,
            border = BorderStroke(1.dp, inputBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 10.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live player head avatar from mc-heads.net API
                PlayerAvatar(
                    username = username,
                    size = 28.dp
                )

                Spacer(modifier = Modifier.width(10.dp))

                BasicTextField(
                    value = username,
                    onValueChange = { input ->
                        // Enforce Minecraft nickname constraints: only [a-zA-Z0-9_] and max 16 characters
                        val filtered = input.filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_' }.take(16)
                        username = filtered
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        fontFamily = GoogleSansFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.5.sp,
                        color = StudioTheme.TextPrimary
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(StudioTheme.PrimaryIndigo),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isValid) {
                                onConfirm(username)
                            }
                        }
                    ),
                    decorationBox = { innerTextField ->
                        if (username.isEmpty()) {
                            Text(
                                text = strings.offlineNicknamePlaceholder,
                                fontFamily = GoogleSansFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = StudioTheme.TextMuted
                            )
                        }
                        innerTextField()
                    }
                )

                // Length counter badge
                Text(
                    text = "${username.length}/16",
                    fontFamily = GoogleSansFontFamily,
                    fontSize = 11.sp,
                    color = if (isValid) Color(0xFF34D399) else StudioTheme.TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Continue / Confirm button dynamically themed
        val confirmInteraction = remember { MutableInteractionSource() }
        val isConfirmHovered by confirmInteraction.collectIsHoveredAsState()

        val buttonBg by animateColorAsState(
            targetValue = when {
                !isValid -> StudioTheme.SurfaceContainerDark.copy(alpha = 0.6f)
                isConfirmHovered -> StudioTheme.PrimaryIndigoLight
                else -> StudioTheme.PrimaryIndigo
            },
            animationSpec = tween(150)
        )

        val buttonTextColor by animateColorAsState(
            targetValue = when {
                !isValid -> StudioTheme.TextMuted
                else -> Color.White
            },
            animationSpec = tween(150)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .pointerHoverIcon(if (isValid) PointerIcon.Hand else PointerIcon.Default)
                .clickable(
                    enabled = isValid,
                    interactionSource = confirmInteraction,
                    indication = ripple(color = Color.White),
                    onClick = { onConfirm(username) }
                ),
            shape = RoundedCornerShape(12.dp),
            color = buttonBg,
            border = BorderStroke(
                1.dp,
                if (isValid) StudioTheme.PrimaryIndigoLight.copy(alpha = 0.5f) else StudioTheme.BorderDark
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.continueButton,
                    fontFamily = GoogleSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = buttonTextColor
                )
            }
        }
    }
}
