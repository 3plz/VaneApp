package app.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.domain.model.AuthState
import app.i18n.AppLanguage
import app.i18n.strings
import app.theme.GoogleSansFontFamily
import app.ui.login.components.LanguageSelector
import app.ui.login.components.MicrosoftLoginButton

@Composable
fun LoginScreen(
    authState: AuthState,
    onLoginClick: () -> Unit,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoading = authState is AuthState.Authenticating

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MicrosoftLoginButton(
                isLoading = isLoading,
                onClick = onLoginClick
            )

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
                    // Idle state - no additional elements
                }
            }
        }

        LanguageSelector(
            currentLanguage = currentLanguage,
            onLanguageSelected = onLanguageSelected,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}
