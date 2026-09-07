package app.ui.login

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import app.domain.model.AuthState
import app.i18n.AppLanguage
import app.theme.StudioTheme
import app.ui.components.CustomTitleBar
import java.awt.Dimension

@Composable
fun LoginWindow(
    authState: AuthState,
    onLoginClick: () -> Unit,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onCloseRequest: () -> Unit
) {
    val windowState = rememberWindowState(
        size = DpSize(475.dp, 300.dp),
        position = WindowPosition.Aligned(Alignment.Center)
    )

    Window(
        onCloseRequest = onCloseRequest,
        state = windowState,
        title = "",
        undecorated = true,
        resizable = false,
        transparent = false
    ) {
        LaunchedEffect(window) {
            window.size = Dimension(475, 300)
            window.minimumSize = Dimension(475, 300)
            window.maximumSize = Dimension(475, 300)
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, StudioTheme.BorderDark),
            color = StudioTheme.BackgroundDark
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                CustomTitleBar(
                    windowState = windowState,
                    onClose = onCloseRequest
                )

                LoginScreen(
                    authState = authState,
                    onLoginClick = onLoginClick,
                    currentLanguage = currentLanguage,
                    onLanguageSelected = onLanguageSelected,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
