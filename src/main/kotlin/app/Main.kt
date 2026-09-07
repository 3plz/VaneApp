package app

import androidx.compose.runtime.*
import androidx.compose.ui.window.application
import app.di.AppContainer
import app.i18n.AppLanguage
import app.i18n.LocalAppLanguage
import app.i18n.LocalStrings
import app.i18n.getStrings
import app.theme.WickedTheme
import app.ui.login.LoginWindow
import kotlinx.coroutines.launch

fun main() = application {
    val appContainer = remember { AppContainer() }
    val authService = appContainer.authService
    val authState by authService.authState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Active UI locale (English by default)
    var currentLanguage by remember { mutableStateOf(AppLanguage.EN) }

    CompositionLocalProvider(
        LocalAppLanguage provides currentLanguage,
        LocalStrings provides getStrings(currentLanguage)
    ) {
        WickedTheme(darkTheme = true) {
            LoginWindow(
                authState = authState,
                onLoginClick = {
                    coroutineScope.launch {
                        authService.login()
                    }
                },
                currentLanguage = currentLanguage,
                onLanguageSelected = { currentLanguage = it },
                onCloseRequest = ::exitApplication
            )
        }
    }
}
