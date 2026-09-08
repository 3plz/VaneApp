package app.ui.login

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.domain.model.AuthState
import app.i18n.AppLanguage
import app.i18n.strings
import app.theme.GoogleSansFontFamily
import app.theme.StudioTheme
import app.ui.login.components.LanguageSelector
import app.ui.login.components.MicrosoftLoginButton
import app.ui.login.components.OfflineLoginCard

enum class AuthStep {
    MICROSOFT,
    OFFLINE
}

@Composable
fun LoginScreen(
    authState: AuthState,
    canCreateOffline: Boolean,
    canReturnToMain: Boolean,
    onReturnToMain: () -> Unit,
    onLoginClick: () -> Unit,
    onOfflineLoginClick: (username: String) -> Unit,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(AuthStep.MICROSOFT) }
    val isLoading = authState is AuthState.Authenticating
    val userCode = (authState as? AuthState.Authenticating)?.userCode

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Center area with horizontal sliding transition between Microsoft and Offline steps
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState == AuthStep.OFFLINE) {
                    // Slide to the left
                    (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { fullWidth -> fullWidth } + fadeIn(tween(250)))
                        .togetherWith(
                            slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { fullWidth -> -fullWidth } + fadeOut(tween(200))
                        )
                } else {
                    // Slide back to the right
                    (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { fullWidth -> -fullWidth } + fadeIn(tween(250)))
                        .togetherWith(
                            slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { fullWidth -> fullWidth } + fadeOut(tween(200))
                        )
                }
            },
            modifier = Modifier.align(Alignment.Center)
        ) { step ->
            when (step) {
                AuthStep.MICROSOFT -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MicrosoftLoginButton(
                            isLoading = isLoading,
                            userCode = userCode,
                            onClick = onLoginClick
                        )

                        if (userCode != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strings.codeCopiedHint,
                                fontFamily = GoogleSansFontFamily,
                                fontSize = 11.sp,
                                color = StudioTheme.TextMuted
                            )
                        }

                        // Only allow creating offline accounts if at least 1 Microsoft account is registered
                        if (canCreateOffline) {
                            Spacer(modifier = Modifier.height(14.dp))

                            val offlineInteractionSource = remember { MutableInteractionSource() }
                            val isOfflineHovered by offlineInteractionSource.collectIsHoveredAsState()

                            val offlineTextColor by animateColorAsState(
                                targetValue = if (isOfflineHovered) StudioTheme.PrimaryIndigo else StudioTheme.TextMuted,
                                animationSpec = tween(120)
                            )

                            Text(
                                text = strings.createOfflineAccount,
                                fontFamily = GoogleSansFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = offlineTextColor,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable(
                                        interactionSource = offlineInteractionSource,
                                        indication = null,
                                        onClick = { currentStep = AuthStep.OFFLINE }
                                    )
                            )
                        }

                        when (authState) {
                            is AuthState.Authenticating -> {
                                // Loading spinner and status are displayed inside MicrosoftLoginButton
                            }
                            is AuthState.Authenticated -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "✓ ${strings.loggedInAs}${authState.session.username}",
                                    color = Color(0xFF34D399),
                                    fontFamily = GoogleSansFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.5.sp
                                )
                            }
                            is AuthState.Error -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = authState.message,
                                    color = Color(0xFFEF4444),
                                    fontFamily = GoogleSansFontFamily,
                                    fontSize = 11.5.sp
                                )
                            }
                            AuthState.Unauthenticated -> {
                                // Idle state
                            }
                        }
                    }
                }
                AuthStep.OFFLINE -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OfflineLoginCard(
                            onConfirm = onOfflineLoginClick,
                            onBack = { currentStep = AuthStep.MICROSOFT }
                        )

                        when (authState) {
                            is AuthState.Authenticated -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "✓ ${strings.loggedInAs}${authState.session.username}",
                                    color = Color(0xFF34D399),
                                    fontFamily = GoogleSansFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.5.sp
                                )
                            }
                            is AuthState.Error -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = authState.message,
                                    color = Color(0xFFEF4444),
                                    fontFamily = GoogleSansFontFamily,
                                    fontSize = 11.5.sp
                                )
                            }
                            else -> {
                                // Idle state
                            }
                        }
                    }
                }
            }
        }

        // Bottom left: language selector
        LanguageSelector(
            currentLanguage = currentLanguage,
            onLanguageSelected = onLanguageSelected,
            modifier = Modifier.align(Alignment.BottomStart)
        )

        // Bottom right: themed button with right arrow returning to main screen if accounts exist
        if (canReturnToMain) {
            val returnInteraction = remember { MutableInteractionSource() }
            val isReturnHovered by returnInteraction.collectIsHoveredAsState()

            val returnBg by animateColorAsState(
                targetValue = if (isReturnHovered) StudioTheme.PrimaryIndigoLight else StudioTheme.PrimaryIndigo,
                animationSpec = tween(150)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(returnBg)
                    .border(1.dp, StudioTheme.PrimaryIndigoLight.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(
                        interactionSource = returnInteraction,
                        indication = ripple(color = Color.White),
                        onClick = onReturnToMain
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Return to Main Screen",
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}
